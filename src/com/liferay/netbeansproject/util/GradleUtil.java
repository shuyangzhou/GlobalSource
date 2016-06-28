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
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class GradleUtil {

	public static Map<String, List<Dependency>> getJarDependencies(
			Path portalDirPath, Path workDirPath,
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

		Map<String, List<Dependency>> dependenciesMap = new HashMap<>();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				dependenciesDirPath)) {

			for (Path dependencyPath : directoryStream) {
				List<Dependency> jarDependencies = new ArrayList<>();

				Properties dependencies = PropertiesUtil.loadProperties(
					dependencyPath);

				for (String jar :
						StringUtil.split(
							dependencies.getProperty("compile"), ':')) {

					jarDependencies.add(new Dependency(Paths.get(jar), false));
				}

				for (String jar :
						StringUtil.split(
							dependencies.getProperty("compileTest"), ':')) {

					Dependency dependency = new Dependency(
						Paths.get(jar), true);

					if (!jarDependencies.contains(dependency)) {
						jarDependencies.add(dependency);
					}
				}

				Path moduleName = dependencyPath.getFileName();

				dependenciesMap.put(moduleName.toString(), jarDependencies);
			}
		}

		FileUtil.delete(dependenciesDirPath);

		return dependenciesMap;
	}

	public static List<Dependency> getModuleDependencies(Path modulePath)
		throws IOException {

		Path buildGradlePath = modulePath.resolve("build.gradle");

		if (!Files.exists(buildGradlePath)) {
			return Collections.emptyList();
		}

		List<Dependency> moduleDependencies = new ArrayList<>();

		for (String line : Files.readAllLines(buildGradlePath)) {
			if (!line.contains(" project(")) {
				continue;
			}

			line = line.trim();

			int index1 = line.indexOf('\"');

			if (index1 < 0) {
				throw new IllegalStateException(
					"Broken syntax in " + buildGradlePath);
			}

			int index2 = line.indexOf('\"', index1 + 1);

			if (index2 < 0) {
				throw new IllegalStateException(
					"Broken syntax in " + buildGradlePath);
			}

			String moduleLocation = line.substring(index1 + 1, index2);

			moduleDependencies.add(
				new Dependency(
					Paths.get("modules", StringUtil.split(moduleLocation, ':')),
					line.startsWith("test")));
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

}