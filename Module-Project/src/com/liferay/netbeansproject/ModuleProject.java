/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liferay.netbeansproject;

import com.liferay.netbeansproject.util.GradleUtil;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.container.Module.ModuleDependency;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import java.io.IOException;
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
 * @author tom
 */
public class ModuleProject {
	public static void main(String[] args) throws IOException {

		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		final Set<String> blackListDirs = new HashSet<>();

		blackListDirs.addAll(Arrays.asList(
			StringUtil.split(properties.getProperty("blackListDirs"), ',')));

		final Set<String> dependenciesSet = new HashSet<>();

		final Map<Path, List<Module>> projectMap = new HashMap<>();

		// TODO 1) populate the blackListPaths from properties

		Files.walkFileTree(
			Paths.get(properties.getProperty("portal.dir")),
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
					Path dir, BasicFileAttributes attrs) throws IOException {

					Path dirFileName = dir.getFileName();

					if (blackListDirs.contains(dirFileName.toString())) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (Files.exists(dir.resolve("src"))) {
						try {
							Module module = ModuleProject._createModule(
								dir, dependenciesSet);

							_linkModuletoMap(
								projectMap, module, dir.getParent());
						}
						catch (Exception ex) {
							Logger.getLogger(ModuleProject.class.getName()).
								log(Level.SEVERE, null, ex);
						}

						return FileVisitResult.SKIP_SUBTREE;
					}

					return FileVisitResult.CONTINUE;
				}

			});

		_createBuildGradleFile(
			dependenciesSet, properties.getProperty("project.dir"));
	}

	private static Module _createModule(
		Path modulePath, Set<String> dependenciesSet) throws Exception {

		List<String> jarDependencyList =
			GradleUtil.getJarDependencies(modulePath);

		dependenciesSet.addAll(jarDependencyList);

		return new Module(
			modulePath, _resolveSourcePath(modulePath),
			_resolveResourcePath(modulePath, "main"),
			_resolveTestPath(modulePath, "unit"),
			_resolveResourcePath(modulePath, "test"),
			_resolveTestPath(modulePath, "integration"),
			_resolveResourcePath(modulePath, "integration"), jarDependencyList,
			GradleUtil.getModuleDependencies(modulePath));
	}

	private static void _createBuildGradleFile(
			Set<String> dependenciesSet, String projectDir)
		throws IOException {

		final StringBuilder dependenciesSB =
			new StringBuilder("dependencies {");

		for (String dependency : dependenciesSet) {
			dependenciesSB.append("\t");
			dependenciesSB.append(dependency);
			dependenciesSB.append("\n");
		}

		dependenciesSB.append("}");

		String defaultGradleContent = new String(
			Files.readAllBytes(Paths.get("..", "common", "default.gradle")));

		String gradleContent =
			StringUtil.replace(
				defaultGradleContent, "*insert-dependencies*",
				dependenciesSB.toString());

		Path modulesDir = Paths.get(projectDir, "modules");

		gradleContent =
			StringUtil.replace(
				gradleContent, "*insert-filepath*", modulesDir.toString());

		Files.createDirectories(modulesDir);

		Files.write(
			modulesDir.resolve("build.gradle"), Arrays.asList(gradleContent),
			Charset.defaultCharset());
	}

	private static void _linkModuletoMap(
		Map<Path, List<Module>> projectMap, Module module, Path path) {

		List<Module> moduleList = projectMap.get(path);

		if(moduleList == null) {
			moduleList = new ArrayList<>();
		}

		moduleList.add(module);

		projectMap.put(path, moduleList);
	}

	private static Path _resolveResourcePath(Path modulePath, String type) {

		Path resourcePath =modulePath.resolve(
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

		return modulePath.resolve("src");
	}

	private static Path _resolveTestPath(Path modulePath, String type) {

		Path testUnitPath = modulePath.resolve(_testUnitPath);

		if (Files.exists(testUnitPath) && type.equals("unit")) {

			return testUnitPath;
		}

		Path testIntegrationPath = modulePath.resolve(_testIntegrationPath);

		if (Files.exists(testIntegrationPath) && type.equals("integration")) {

			return testIntegrationPath;
		}

		Path testPath = modulePath.resolve(Paths.get("test", type));

		if (Files.exists(testPath)) {

			return testPath;
		}

		return null;
	}

	private static final Path _mainJavaPath = Paths.get("src", "main", "java");

	private static final Path _testUnitPath = Paths.get("src", "test", "java");

	private static final Path _testIntegrationPath =
		Paths.get("src", "testIntegration", "java");
}
