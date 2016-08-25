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

package com.liferay.netbeansproject.util;

import com.liferay.netbeansproject.container.Dependency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Tom Wang
 */
public class GradleUtil {

	public static Map<String, Set<Dependency>> getJarDependencies(
			Path portalDirPath, Path workDirPath, Set<String> symbolicNameSet,
			boolean displayGradleProcessOutput, boolean daemon)
		throws Exception {

		Path dependenciesDirPath = Files.createTempDirectory(null);

		FileUtil.delete(dependenciesDirPath);

		Files.createDirectories(dependenciesDirPath);

		List<String> gradleTask = new ArrayList<>();

		gradleTask.add(String.valueOf(portalDirPath.resolve("gradlew")));

		if (daemon) {
			gradleTask.add("--daemon");
		}

		gradleTask.add("--parallel");
		gradleTask.add("--init-script=dependency.gradle");
		gradleTask.add("-p");
		gradleTask.add(String.valueOf(portalDirPath.resolve("modules")));
		gradleTask.add(_getTaskName(portalDirPath, workDirPath));
		gradleTask.add(
			"-PdependencyDirectory=".concat(dependenciesDirPath.toString()));
		gradleTask.add("-g");

		Path gradleCachePath = Paths.get(".gradle");

		FileUtil.copy(
			portalDirPath.resolve(".gradle/caches/modules-2/files-2.1"),
			gradleCachePath.resolve("caches/modules-2/files-2.1"));

		gradleTask.add(String.valueOf(gradleCachePath));

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Map<String, String> env = processBuilder.environment();

		env.put("GRADLE_OPTS", "-Xmx2g");

		Process process = processBuilder.start();

		if (displayGradleProcessOutput) {
			String line = null;

			try(
				BufferedReader br = new BufferedReader(
					new InputStreamReader(process.getInputStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}

			try(
				BufferedReader br = new BufferedReader(
					new InputStreamReader(process.getErrorStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}
		}

		int exitCode = process.waitFor();

		if (exitCode != 0) {
			throw new IOException(
				"Process " + processBuilder.command() + " failed with " +
					exitCode);
		}

		Map<String, Set<Dependency>> dependenciesMap = new HashMap<>();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				dependenciesDirPath)) {

			String portalToolsPath = String.valueOf(
				portalDirPath.resolve("tools/sdk"));

			for (Path dependencyPath : directoryStream) {
				Set<Dependency> jarDependencies = new HashSet<>();

				jarDependencies.addAll(
					_getConfigurationDependencies(
						dependencyPath, "compile", "compileSources", false,
						portalToolsPath, symbolicNameSet));

				jarDependencies.addAll(
					_getConfigurationDependencies(
						dependencyPath, "compileTest",
						"testIntegrationRuntimeSources", true, portalToolsPath,
						symbolicNameSet));

				dependenciesMap.put(
					String.valueOf(dependencyPath.getFileName()),
					jarDependencies);
			}
		}

		FileUtil.delete(dependenciesDirPath);

		return dependenciesMap;
	}

	public static Set<Dependency> getModuleDependencies(
			Path modulePath, Map<String, Path> moduleProjectPaths)
		throws IOException {

		Path buildGradlePath = modulePath.resolve("build.gradle");

		if (!Files.exists(buildGradlePath)) {
			return Collections.emptySet();
		}

		Set<Dependency> moduleDependencies = new HashSet<>();

		for (String line : Files.readAllLines(buildGradlePath)) {
			Path moduleProjectPath = null;

			if (line.contains(" project(") || line.contains(" project (")) {
				moduleProjectPath = Paths.get(
					"modules",
					StringUtil.split(
						StringUtil.extractQuotedText(line.trim()), ':'));
			}
			else if (line.contains("name: \"com.liferay")) {
				String[] split = StringUtil.split(line.trim(), ',');

				String moduleSymbolicName = StringUtil.extractQuotedText(
					split[1]);

				moduleProjectPath = moduleProjectPaths.get(moduleSymbolicName);

				if (moduleProjectPath == null) {
					continue;
				}
			}
			else {
				continue;
			}

			moduleDependencies.add(
				new Dependency(
					moduleProjectPath, null, line.startsWith("test")));
		}

		return moduleDependencies;
	}

	public static void stopGradleDaemon(
			Path portalDirPath, boolean displayGradleProcessOutput)
		throws Exception {

		List<String> gradleTask = new ArrayList<>();

		gradleTask.add(String.valueOf(portalDirPath.resolve("gradlew")));

		gradleTask.add("--stop");

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Process process = processBuilder.start();

		if (displayGradleProcessOutput) {
			String line = null;

			try(
				BufferedReader br = new BufferedReader(
					new InputStreamReader(process.getInputStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}

			try(
				BufferedReader br = new BufferedReader(
					new InputStreamReader(process.getErrorStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}
		}

		int exitCode = process.waitFor();

		if (exitCode != 0) {
			throw new IOException(
				"Process " + processBuilder.command() + " failed with " +
					exitCode);
		}
	}

	private static Set<Dependency> _getConfigurationDependencies(
			Path dependencyPath, String configurationName, String sourceName,
			boolean isTest, String portalToolsPath, Set<String> symbolicNameSet)
		throws IOException {

		Properties dependencies = PropertiesUtil.loadProperties(dependencyPath);

		Map<String, Path> sourceJarPaths = _loadSourceJarPaths(
			dependencies.getProperty(sourceName));

		Set<Dependency> jarDependencies = new HashSet<>();

		for (String jar :
				StringUtil.split(
					dependencies.getProperty(configurationName), ':')) {

			if (jar.startsWith(portalToolsPath)) {
				continue;
			}

			Path jarPath = Paths.get(jar);

			String jarName = String.valueOf(jarPath.getFileName());

			if (jarName.startsWith("com.liferay")) {
				String[] jarPathSplit = StringUtil.split(jarName, '-');

				if (symbolicNameSet.contains(jarPathSplit[0])) {
					continue;
				}
			}

			jarDependencies.add(
				new Dependency(
					jarPath, sourceJarPaths.get(
						String.valueOf(jarPath.getFileName())),
					isTest));
		}

		return jarDependencies;
	}

	private static String _getTaskName(Path portalDirPath, Path workDirPath) {
		Path modulesPath = portalDirPath.resolve("modules");

		Path relativeWorkPath = modulesPath.relativize(workDirPath);

		String relativeWorkPathString = relativeWorkPath.toString();

		if (relativeWorkPathString.isEmpty()) {
			return "printDependencies";
		}

		relativeWorkPathString = relativeWorkPathString.replace('/', ':');

		return relativeWorkPathString.concat(":").concat("printDependencies");
	}

	private static Map<String, Path> _loadSourceJarPaths(String sources) {
		Map<String, Path> sourceJarPaths = new HashMap<>();

		for (String sourceJarLocation : StringUtil.split(sources, ':')) {
			Path path = Paths.get(sourceJarLocation);

			sourceJarPaths.put(
				StringUtil.replace(
					String.valueOf(path.getFileName()), "-sources.jar", ".jar"),
				path);
		}

		return sourceJarPaths;
	}

}