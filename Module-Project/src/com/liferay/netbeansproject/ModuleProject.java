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

import com.liferay.netbeansproject.container.Module.JarDependency;
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

/**
 * @author Tom Wang
 */
public class ModuleProject {

	public static void main(String[] args) throws IOException {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path projectDirPath = Paths.get(properties.getProperty("project.dir"));

		_clean(projectDirPath);

		Path portalDirPath = Paths.get(properties.getProperty("portal.dir"));

		Map<String, List<JarDependency>> jarDependenciesMap =
			_processGradle(portalDirPath);
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
}