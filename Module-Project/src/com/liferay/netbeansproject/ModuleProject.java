/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.netbeansproject;

import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.container.Module.JarDependency;
import com.liferay.netbeansproject.util.GradleUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tom Wang
 */
public class ModuleProject {

	public static void main(String[] args) throws Exception, IOException {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		final Set<String> blackListDirs = new HashSet<>();

		blackListDirs.addAll(
			Arrays.asList(
				StringUtil.split(
					properties.getProperty("blackListDirs"), ',')));

		final StringBuilder settingsSB = new StringBuilder();

		final String projectDir = properties.getProperty("project.dir");

		final Map<Path, Map<String, Module>> projectMap = new HashMap<>();

		_clean(projectDir);

		Files.walkFileTree(
			Paths.get(properties.getProperty("portal.dir")),
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path dir, BasicFileAttributes attrs)
					throws IOException {

					Path dirFileName = dir.getFileName();

					if (blackListDirs.contains(dirFileName.toString())) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (Files.exists(dir.resolve("src"))) {
						try {
							Module module = ModuleProject._createModule(
								dir, projectDir, settingsSB);

							_linkModuletoMap(
								projectMap, module, dir.getParent());
						}
						catch (Exception ex) {
							Logger.getLogger(
								ModuleProject.class.getName()).log(
									Level.SEVERE, null, ex);
						}

						return FileVisitResult.SKIP_SUBTREE;
					}

					return FileVisitResult.CONTINUE;
				}

			});

		_createSettingsGradleFile(settingsSB, projectDir);

		_processGradle(properties);

		final Map<Module, List<JarDependency>> dependenciesMap =
			_createDependenciesMap(projectMap, projectDir);
	}

	private static void _clean(String projectDir) throws IOException {
		Files.walkFileTree(
			Paths.get(projectDir), new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(
				Path file, BasicFileAttributes attrs) throws IOException {

				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {

				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static void _createBuildGradleFile(
			Path modulePathName, String jarDependencyList, String projectDir)
		throws IOException {

		String fileContent = new String(
			Files.readAllBytes(Paths.get("../common/default.gradle")));

		Path buildGradlePath = Paths.get(
			projectDir, "modules", modulePathName.toString());

		Files.createDirectories(buildGradlePath);

		Path dependenciesProperties = buildGradlePath.resolve(
			"dependencies.properties");

		fileContent = StringUtil.replace(
			fileContent, "*insert-filepath*",
			dependenciesProperties.toString());

		Files.write(
			buildGradlePath.resolve("build.gradle"),
			Arrays.asList(
				StringUtil.replace(
					fileContent, "*insert-dependencies*", jarDependencyList)),
			Charset.defaultCharset());
	}

	private static Map<Module, List<JarDependency>> _createDependenciesMap(
			Map<Path, Map<String, Module>> projectMap, String projectDir)
		throws IOException {

		Map<Module, List<JarDependency>> dependenciesMap = new HashMap<>();

		for (Map<String, Module> projects : projectMap.values()) {
			for (Module module : projects.values()) {
				Properties properties = PropertiesUtil.loadProperties(
					Paths.get(projectDir, "modules", module.getModuleName(),
					"dependencies.properties"));

				List<JarDependency> jarDependencys = new ArrayList<>();

				jarDependencys.addAll(
					_getJarDependency(false, properties, "compile"));

				jarDependencys.addAll(
					_getJarDependency(true, properties, "compileTest"));

				dependenciesMap.put(module, jarDependencys);
			}
		}

		return dependenciesMap;
	}

	private static Module _createModule(
			Path modulePath, String projectDir, StringBuilder settingsSB)
		throws Exception {

		Path moduleFileName = modulePath.getFileName();

		if (moduleFileName.endsWith("WEB-INF")) {
			modulePath = modulePath.getParent();
			modulePath = modulePath.getParent();

			moduleFileName = modulePath.getFileName();
		}

		String jarDependencyList = GradleUtil.getJarDependencies(modulePath);

		_createBuildGradleFile(moduleFileName, jarDependencyList, projectDir);

		settingsSB.append("include \"");
		settingsSB.append(moduleFileName);
		settingsSB.append("\"\n");

		return new Module(
			modulePath, _resolveSourcePath(modulePath),
			_resolveResourcePath(modulePath, "main"),
			_resolveTestPath(modulePath, "unit"),
			_resolveResourcePath(modulePath, "test"),
			_resolveTestPath(modulePath, "integration"),
			_resolveResourcePath(modulePath, "integration"), jarDependencyList,
			GradleUtil.getModuleDependencies(modulePath),
			moduleFileName.toString());
	}

	private static void _createSettingsGradleFile(
			StringBuilder sb, String projectDir)
		throws IOException {

		Path moduleProjectsDirPath = Paths.get(projectDir, "modules");

		Files.write(
			moduleProjectsDirPath.resolve("settings.gradle"), Arrays.asList(sb),
			Charset.defaultCharset());
	}

	private static List<JarDependency> _getJarDependency(
		boolean isTest, Properties dependenciesProperties, String configuration)
			{

		List<JarDependency> jarDependencys = new ArrayList<>();

		String compile = dependenciesProperties.getProperty(configuration);

		if (compile != null) {
			for (
				String jarPath :
				StringUtil.split(compile, File.pathSeparatorChar)) {

				JarDependency jarDependency = new JarDependency(
					Paths.get(jarPath), isTest);

				jarDependencys.add(jarDependency);
			}
		}

		return jarDependencys;
	}

	private static void _linkModuletoMap(
		Map<Path, Map<String, Module>> projectMap, Module module,
		Path parentPath) {

		if (parentPath.endsWith("docroot")) {
			Path modulePath = parentPath.getParent();

			parentPath = modulePath.getParent();
		}

		Map<String, Module> moduleMap = projectMap.get(parentPath);

		if (moduleMap == null) {
			moduleMap = new HashMap<>();
		}

		moduleMap.put(module.getModuleName(), module);

		projectMap.put(parentPath, moduleMap);
	}

	private static void _processGradle(Properties properties)
		throws IOException {

		List<String> gradleTask = new ArrayList<>();

		Path gradlewPath = Paths.get(
			properties.getProperty("portal.dir"), "gradlew");

		gradleTask.add(gradlewPath.toString());
		gradleTask.add("createJarDependencies");
		gradleTask.add("-p");

		Path modulesDirPath = Paths.get(
			properties.getProperty("project.dir"), "modules");

		gradleTask.add(modulesDirPath.toString());
		gradleTask.add("--info");

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Process process = processBuilder.start();

		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

		String line;
		while ((line = br.readLine()) != null)
			 System.out.println(line);
	}

	private static Path _resolveResourcePath(Path modulePath, String type) {
		Path resourcePath = modulePath.resolve(
			Paths.get("src", type, "resources"));

		if (Files.exists(resourcePath)) {
			return resourcePath;
		}

		resourcePath = modulePath.resolve(_docrootPath);
		resourcePath = resourcePath.resolve(
			Paths.get("src", type, "resources"));

		if (Files.exists(resourcePath)) {
			return resourcePath;
		}

		return null;
	}

	private static Path _resolveSourcePath(Path modulePath) {
		Path mainJavaPath = modulePath.resolve(_mainJavaPath);

		if (Files.exists(mainJavaPath)) {
			return mainJavaPath;
		}

		if (Files.exists(modulePath.resolve(Paths.get("src", "main")))) {
			return null;
		}

		Path mainJavaDocrootPath = modulePath.resolve(_docrootPath);

		mainJavaPath = mainJavaDocrootPath.resolve(_mainJavaPath);

		if (Files.exists(mainJavaPath)) {
			return mainJavaPath;
		}

		if (Files.exists(mainJavaPath.getParent())) {
			return null;
		}

		if (Files.exists(mainJavaDocrootPath.resolve("src"))) {
			return mainJavaDocrootPath.resolve("src");
		}

		return modulePath.resolve("src");
	}

	private static Path _resolveTestPath(Path modulePath, String type) {
		Path testUnitPath = modulePath.resolve(_testUnitPath);

		if (Files.exists(testUnitPath) && type.equals("unit")) {
			return testUnitPath;
		}

		testUnitPath = modulePath.resolve(_docrootPath);
		testUnitPath = testUnitPath.resolve(_testUnitPath);

		if (Files.exists(testUnitPath)) {
			return testUnitPath;
		}

		Path testIntegrationPath = modulePath.resolve(_testIntegrationPath);

		if (Files.exists(testIntegrationPath) && type.equals("integration")) {
			return testIntegrationPath;
		}

		testIntegrationPath = modulePath.resolve(_docrootPath);
		testIntegrationPath = testIntegrationPath.resolve(_testIntegrationPath);

		if (Files.exists(testIntegrationPath)) {
			return testIntegrationPath;
		}

		Path testPath = modulePath.resolve(Paths.get("test", type));

		if (Files.exists(testPath)) {
			return testPath;
		}

		testPath = modulePath.resolve(_docrootPath);
		testPath = testPath.resolve(Paths.get("test", type));

		if (Files.exists(testPath)) {
			return testPath;
		}

		return null;
	}

	private static final Path _docrootPath = Paths.get("docroot", "WEB-INF");
	private static final Path _mainJavaPath = Paths.get("src", "main", "java");
	private static final Path _testIntegrationPath = Paths.get(
		"src", "testIntegration", "java");
	private static final Path _testUnitPath = Paths.get("src", "test", "java");
}