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
import com.liferay.netbeansproject.util.FileUtil;
import com.liferay.netbeansproject.util.GradleUtil;
import com.liferay.netbeansproject.util.ModuleUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.IOException;

import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Tom Wang
 */
public class ProjectBuilder {

	public static void main(String[] args) throws Exception {
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		Properties buildProperties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path projectDirPath = Paths.get(
			PropertiesUtil.getRequiredProperty(buildProperties, "project.dir"));

		ProjectBuilder projectBuilder = new ProjectBuilder();

		for (String portalDir : StringUtil.split(
				PropertiesUtil.getRequiredProperty(
					buildProperties, "portal.dirs"),
				',')) {

			Path portalDirPath = Paths.get(portalDir);

			projectBuilder.scanPortal(
				Boolean.valueOf(arguments.get("rebuild")),
				projectDirPath.resolve(portalDirPath.getFileName()),
				portalDirPath,
				Boolean.valueOf(
					buildProperties.getProperty(
						"display.gradle.process.output")),
				PropertiesUtil.getRequiredProperty(
					buildProperties, "ignored.dirs"),
				PropertiesUtil.getRequiredProperty(
					buildProperties, "project.name"),
				buildProperties.getProperty("exclude.types"),
				PropertiesUtil.getProperties(
					buildProperties, "umbrella.source.list"));
		}
	}

	public void scanPortal(
			boolean rebuild, final Path projectPath, Path portalPath,
			final boolean displayGradleProcessOutput, String ignoredDirs,
			String projectName, String excludedTypes,
			Map<String, String> umbrellaSourceList)
		throws Exception {

		final Map<Path, Module> existingProjectMap = _getExistingProjects(
			rebuild, projectPath.resolve("modules"));

		if (existingProjectMap.isEmpty()) {
			rebuild = true;

			FileUtil.delete(projectPath);
		}

		final Set<String> ignoredDirSet = new HashSet<>(
			Arrays.asList(StringUtil.split(ignoredDirs, ',')));

		final Properties projectDependencyProperties =
			PropertiesUtil.loadProperties(
				Paths.get("project-dependency.properties"));

		final Set<Path> changedModules = new HashSet<>();

		Files.walkFileTree(
			portalPath, EnumSet.allOf(FileVisitOption.class), Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path fileNamePath = path.getFileName();

					String fileName = fileNamePath.toString();

					if (ignoredDirSet.contains(fileName)) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (!Files.exists(path.resolve("src"))) {
						return FileVisitResult.CONTINUE;
					}

					Module existModule = existingProjectMap.get(path);

					if ((existModule != null) &&
						existModule.equals(
							Module.createModule(
								null, path, null,
								projectDependencyProperties))) {

						return FileVisitResult.SKIP_SUBTREE;
					}

					changedModules.add(path);

					return FileVisitResult.SKIP_SUBTREE;
				}

			});

		Map<Path, Module> moduleMap = new HashMap<>();

		String portalLibJars = ModuleUtil.getPortalLibJars(portalPath);

		Map<String, List<JarDependency>> jarDependenciesMap = new HashMap<>();

		if (rebuild) {
			jarDependenciesMap = GradleUtil.getJarDependencies(
				portalPath, portalPath.resolve("modules"),
				displayGradleProcessOutput);

			CreateUmbrella.createUmbrella(
				portalPath, projectName, umbrellaSourceList, excludedTypes,
				changedModules, projectPath);
		}

		for (Path path : changedModules) {
			if (!rebuild) {
				String moduleName = ModuleUtil.getModuleName(path);

				Path moduleProjectPath = projectPath.resolve(
					Paths.get("modules", moduleName));

				FileUtil.delete(moduleProjectPath);

				jarDependenciesMap = new HashMap<>();

				if (Files.exists(path.resolve("build.gradle"))) {
					jarDependenciesMap = GradleUtil.getJarDependencies(
						portalPath, path, displayGradleProcessOutput);
				}
			}

			Module module = Module.createModule(
				projectPath.resolve(
					Paths.get("modules", ModuleUtil.getModuleName(path))),
				path,
				jarDependenciesMap.get(String.valueOf(path.getFileName())),
				projectDependencyProperties);

			moduleMap.put(module.getModulePath(), module);

			CreateModule.createModule(
				module, projectPath, excludedTypes, portalLibJars, portalPath);
		}
	}

	private Map<Path, Module> _getExistingProjects(
			boolean rebuild, Path projectModulesPath)
		throws IOException {

		Map<Path, Module> map = new HashMap<>();

		if (rebuild) {
			return map;
		}

		if (Files.exists(projectModulesPath)) {
			for (Path path : Files.newDirectoryStream(projectModulesPath)) {
				Module module = Module.load(path);

				if (module != null) {
					map.put(module.getModulePath(), module);
				}
			}
		}

		return map;
	}

}