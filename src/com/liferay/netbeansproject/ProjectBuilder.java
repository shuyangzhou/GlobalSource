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

import com.liferay.netbeansproject.container.JarDependency;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.ArgumentsUtil;
import com.liferay.netbeansproject.util.ModuleUtil;
import com.liferay.netbeansproject.util.PathUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;

import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class ProjectBuilder {

	public static void main(String[] args) throws Exception {
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		ProjectBuilder portalScanner = new ProjectBuilder();

		portalScanner.scanPortal(Paths.get(arguments.get("portal.dir")));
	}

	public void scanPortal(Path portalPath) throws Exception {
		Properties buildProperties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path portalNamePath = portalPath.getFileName();

		final Path projectPath = Paths.get(
			buildProperties.getProperty("project.dir"),
			portalNamePath.toString());

		PathUtil.delete(projectPath);

		final String ignoredDirs = buildProperties.getProperty("ignored.dirs");

		final Map<String, List<JarDependency>> jarDependenciesMap =
			ProcessGradle.processGradle(
				Boolean.valueOf(
					buildProperties.getProperty(
						"display.gradle.process.output")),
				portalPath, projectPath, portalPath.resolve("modules"));

		final Map<Path, Module> projectMap = new HashMap<>();

		Files.walkFileTree(
			portalPath, EnumSet.allOf(FileVisitOption.class), Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path fileNamePath = path.getFileName();

					String fileName = fileNamePath.toString();

					if (ignoredDirs.contains(fileName)) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (!Files.exists(path.resolve("src"))) {
						return FileVisitResult.CONTINUE;
					}

					Module module = Module.createModule(
						projectPath.resolve(
							Paths.get(
								"modules", ModuleUtil.getModuleName(path))),
						path, jarDependenciesMap.get(fileName));

					Path modulePath = module.getModulePath();

					projectMap.put(modulePath, module);

					return FileVisitResult.SKIP_SUBTREE;
				}

			});

		_generateModuleList(projectMap, projectPath.resolve("moduleList"));

		CreateModule.createModules(projectMap, portalPath, projectPath);

		CreateUmbrella.createUmbrella(projectMap, portalPath, buildProperties);
	}

	private void _generateModuleList(Map<Path, Module> moduleMap, Path filePath)
		throws IOException {

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				filePath, StandardOpenOption.CREATE)) {

			for (Path path : moduleMap.keySet()) {
				bufferedWriter.append(ModuleUtil.getModuleName(path));
				bufferedWriter.append(',');
			}
		}
	}

}