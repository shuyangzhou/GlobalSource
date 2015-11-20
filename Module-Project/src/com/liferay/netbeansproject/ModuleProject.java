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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tom Wang
 */
public class ModuleProject {

	public static void main(String[] args) throws IOException {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		final Path projectDirPath = Paths.get(
			properties.getProperty("project.dir"));

		_clean(projectDirPath);

		Path portalDirPath = Paths.get(properties.getProperty("portal.dir"));

		final Map<String, List<JarDependency>> jarDependenciesMap =
			_processGradle(portalDirPath);

		final String blackListDirs = properties.getProperty("blackListDirs");

		Files.walkFileTree(portalDirPath, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path dirFileName = path.getFileName();

					if (blackListDirs.contains(dirFileName.toString())) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (Files.exists(path.resolve("src"))) {
						try {
							Module module = ModuleProject._createModule(
								jarDependenciesMap, path);

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
	}

	private static void _clean(Path projectDirPath) throws IOException {
		if (Files.exists(projectDirPath)) {
			Files.walkFileTree(
				projectDirPath,
				new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(
						Path path, IOException ioe)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}
			});

		}
	}

	private static Module _createModule(
			Map<String, List<JarDependency>> jarDependenciesMap,
			Path modulePath)
		throws Exception {

		Path moduleFileName = modulePath.getFileName();

		if (moduleFileName.endsWith("WEB-INF")) {
			modulePath = modulePath.getParent();
			modulePath = modulePath.getParent();

			moduleFileName = modulePath.getFileName();
		}

		return new Module(
			modulePath, _resolveSourcePath(modulePath),
			_resolveResourcePath(modulePath, "main"),
			_resolveTestPath(modulePath, _testUnitPath, "unit"),
			_resolveResourcePath(modulePath, "test"),
			_resolveTestPath(modulePath, _testIntegrationPath, "integration"),
			_resolveResourcePath(modulePath, "integration"),
			GradleUtil.getModuleDependencies(modulePath),
			jarDependenciesMap.get(moduleFileName.toString()));
	}

	private static Map<String, List<JarDependency>> _processGradle(
			Path portalDirPath)
		throws IOException {

		List<String> gradleTask = new ArrayList<>();

		Path gradlewPath = portalDirPath.resolve("gradlew");

		gradleTask.add(gradlewPath.toString());
		gradleTask.add("--init-script=dependency.gradle");
		gradleTask.add("printDependencies");
		gradleTask.add("-p");

		Path modulesDirPath = portalDirPath.resolve("modules");

		gradleTask.add(modulesDirPath.toString());

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Process process = processBuilder.start();

		BufferedReader br = new BufferedReader(
			new InputStreamReader(process.getInputStream()));

		String line;

		String moduleName = "";

		Map<String, List<JarDependency>> dependenciesMap = new HashMap<>();

		List<JarDependency> jarDependencies = new ArrayList<>();

		boolean isTest = false;

		while ((line = br.readLine()) != null) {

			if (line.startsWith("configuration")) {
				if (!moduleName.isEmpty()) {
					dependenciesMap.put(moduleName, jarDependencies);
				}

				line = StringUtil.extract(line, '\'');

				String[] dependencyConfigSplit = StringUtil.split(line, ':');

				int splitLength = dependencyConfigSplit.length - 1;

				moduleName = dependencyConfigSplit[splitLength - 1];

				isTest = dependencyConfigSplit[splitLength].equals(
					"testIntegrationRuntime");

				jarDependencies = dependenciesMap.get(moduleName);

				if (jarDependencies == null) {
					jarDependencies = new ArrayList<>();
				}
			}
			else if (line.endsWith("jar")) {
				jarDependencies.add(new JarDependency(Paths.get(line), isTest));
			}
			else {
				System.out.println(line);
			}
		}

		BufferedReader errorBR = new BufferedReader(
			new InputStreamReader(process.getErrorStream()));

		String errorLine;

		while ((errorLine = errorBR.readLine()) != null) {
			System.out.println(errorLine);
		}

		return dependenciesMap;
	}

	private static Path _resolvePath(Path modulePath, Path pathType) {
		modulePath = modulePath.resolve(pathType);

		if (Files.exists(modulePath)) {
			return modulePath;
		}

		modulePath = modulePath.getParent();

		if (Files.exists(modulePath)) {
			return null;
		}

		modulePath = modulePath.getParent();

		if (Files.exists(modulePath)) {
			return modulePath;
		}

		return null;
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
		Path sourcePath = _resolvePath(modulePath, _mainJavaPath);

		if (sourcePath != null) {
			return sourcePath;
		}

		else {
			sourcePath = modulePath.resolve(_docrootPath);

			return _resolvePath(sourcePath, _mainJavaPath);
		}
	}

	private static Path _resolveTestPath(
		Path modulePath, Path pathType, String type) {

		Path testPath = modulePath.resolve(pathType);

		if (Files.exists(testPath)) {
			return testPath;
		}

		testPath = modulePath.resolve("test");

		testPath = testPath.resolve(type);

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