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

		boolean rebuild = Boolean.valueOf(arguments.get("rebuild"));

		Properties buildProperties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		String[] portalDirs = StringUtil.split(
			PropertiesUtil.getRequiredProperty(buildProperties, "portal.dirs"),
			',');

		Path projectDirPath = Paths.get(
			PropertiesUtil.getRequiredProperty(buildProperties, "project.dir"));

		boolean displayGradleProcessOutput = Boolean.valueOf(
			buildProperties.getProperty("display.gradle.process.output"));

		String ignoredDirs = PropertiesUtil.getRequiredProperty(
			buildProperties, "ignored.dirs");

		String projectName = PropertiesUtil.getRequiredProperty(
			buildProperties, "project.name");

		String excludeTypes = buildProperties.getProperty("exclude.types");

		Map<String, String> umbrellaSourceListMap =
			PropertiesUtil.getProperties(
				buildProperties, "umbrella.source.list");

		ProjectBuilder projectBuilder = new ProjectBuilder();

		for (String portalDir : portalDirs) {
			Path portalDirPath = Paths.get(portalDir);

			projectBuilder.scanPortal(
				rebuild, projectDirPath.resolve(portalDirPath.getFileName()),
				portalDirPath, displayGradleProcessOutput, ignoredDirs,
				projectName, excludeTypes, umbrellaSourceListMap);
		}
	}

	public void scanPortal(
			boolean rebuild, final Path projectPath, Path portalPath,
			final boolean displayGradleProcessOutput, String ignoredDirs,
			String projectName, String excludedTypes,
			Map<String, String> umbrellaSourceList)
		throws Exception {

		final Map<Path, Module> oldModules = new HashMap<>();

		if (!rebuild) {
			_loadExistingProjects(projectPath.resolve("modules"), oldModules);

			if (oldModules.isEmpty()) {
				rebuild = true;
			}
		}

		final Set<String> ignoredDirSet = new HashSet<>(
			Arrays.asList(StringUtil.split(ignoredDirs, ',')));

		final Properties projectDependencyProperties =
			PropertiesUtil.loadProperties(
				Paths.get("project-dependency.properties"));

		final Set<Path> modulePaths = new HashSet<>();

		final Set<Path> newModules = new HashSet<>();

		Files.walkFileTree(
			portalPath, EnumSet.allOf(FileVisitOption.class), Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					if (ignoredDirSet.contains(
							String.valueOf(path.getFileName()))) {

						return FileVisitResult.SKIP_SUBTREE;
					}

					if (!Files.exists(path.resolve("src"))) {
						return FileVisitResult.CONTINUE;
					}

					modulePaths.add(path);

					Module module = oldModules.remove(path);

					if ((module == null) ||
						!module.equals(
							Module.createModule(
								null, path, null,
								projectDependencyProperties))) {

						newModules.add(path);
					}

					return FileVisitResult.SKIP_SUBTREE;
				}

			});

		String portalLibJars = ModuleUtil.getPortalLibJars(portalPath);

		Map<String, List<JarDependency>> jarDependenciesMap = new HashMap<>();

		for (Path modulePath : oldModules.keySet()) {
			FileUtil.delete(
				projectPath.resolve(
					Paths.get(
						"modules", ModuleUtil.getModuleName(modulePath))));
		}

		if (rebuild) {
			FileUtil.delete(projectPath);

			jarDependenciesMap = GradleUtil.getJarDependencies(
				portalPath, portalPath.resolve("modules"),
				displayGradleProcessOutput);
		}
		else {
			for (Path newModule : newModules) {
				FileUtil.delete(
					projectPath.resolve(
						Paths.get(
							"modules", ModuleUtil.getModuleName(newModule))));

				if (Files.exists(newModule.resolve("build.gradle"))) {
					jarDependenciesMap.putAll(
						GradleUtil.getJarDependencies(
							portalPath, newModule, displayGradleProcessOutput));
				}
			}
		}

		for (Path modulePath : modulePaths) {
			Module module = Module.createModule(
				projectPath.resolve(
					Paths.get("modules", ModuleUtil.getModuleName(modulePath))),
				modulePath,
				jarDependenciesMap.get(
					String.valueOf(modulePath.getFileName())),
				projectDependencyProperties);

			CreateModule.createModule(
				module, projectPath, excludedTypes, portalLibJars, portalPath);
		}

		CreateUmbrella.createUmbrella(
			portalPath, projectName, umbrellaSourceList, excludedTypes,
			modulePaths, projectPath);
	}

	private void _loadExistingProjects(
			Path projectModulesPath, Map<Path, Module> modules)
		throws IOException {

		if (Files.exists(projectModulesPath)) {
			for (Path path : Files.newDirectoryStream(projectModulesPath)) {
				Module module = Module.load(path);

				if (module != null) {
					modules.put(module.getModulePath(), module);
				}
			}
		}
	}

}