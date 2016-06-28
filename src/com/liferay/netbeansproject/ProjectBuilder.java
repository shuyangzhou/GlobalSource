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

import com.liferay.netbeansproject.container.Dependency;
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

import java.util.ArrayList;
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

		int groupDepth = Integer.valueOf(
			PropertiesUtil.getRequiredProperty(buildProperties, "group.depth"));

		List<String> groupStopWords = Arrays.asList(
			StringUtil.split(
				PropertiesUtil.getRequiredProperty(
					buildProperties, "group.stop.words"),
				','));

		ProjectBuilder projectBuilder = new ProjectBuilder();

		for (String portalDir : portalDirs) {
			Path portalDirPath = Paths.get(portalDir);

			List<String> currentGroupStopWords = new ArrayList<>(
				groupStopWords);

			currentGroupStopWords.add(
				String.valueOf(
					portalDirPath.getName(portalDirPath.getNameCount() - 2)));

			projectBuilder.scanPortal(
				rebuild, projectDirPath.resolve(portalDirPath.getFileName()),
				portalDirPath, displayGradleProcessOutput, ignoredDirs,
				projectName, excludeTypes, umbrellaSourceListMap, groupDepth,
				currentGroupStopWords);
		}
	}

	public void scanPortal(
			boolean rebuild, final Path projectPath, Path portalPath,
			final boolean displayGradleProcessOutput, String ignoredDirs,
			String projectName, String excludedTypes,
			Map<String, String> umbrellaSourceList, int groupDepth,
			List<String> groupStopWords)
		throws Exception {

		final Map<Path, Module> oldModulePaths = new HashMap<>();

		if (!rebuild) {
			_loadExistingProjects(
				projectPath.resolve("modules"), oldModulePaths);

			if (oldModulePaths.isEmpty()) {
				rebuild = true;
			}
		}

		final Set<String> ignoredDirSet = new HashSet<>(
			Arrays.asList(StringUtil.split(ignoredDirs, ',')));

		final Properties portalModuleDependencyProperties =
			PropertiesUtil.loadProperties(
				Paths.get("portal-module-dependency.properties"));

		final Set<String> moduleNames = new HashSet<>();

		final Set<Path> newModulePaths = new HashSet<>();

		final List<Module> modules = new ArrayList<>();

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

					if (path.endsWith("WEB-INF")) {
						path = path.getParent();
						path = path.getParent();
					}

					moduleNames.add(String.valueOf(path.getFileName()));

					Module module = oldModulePaths.remove(path);

					if ((module == null) ||
						!module.equals(
							Module.createModule(
								null, path, null,
								portalModuleDependencyProperties))) {

						newModulePaths.add(path);
					}
					else {
						modules.add(module);
					}

					return FileVisitResult.SKIP_SUBTREE;
				}

			});

		Map<String, Set<Dependency>> jarDependenciesMap = new HashMap<>();

		for (Path oldModulePath : oldModulePaths.keySet()) {
			Path oldModulePathName = oldModulePath.getFileName();

			FileUtil.delete(
				projectPath.resolve(
					Paths.get("modules", oldModulePathName.toString())));
		}

		if (rebuild) {
			FileUtil.delete(projectPath);

			jarDependenciesMap = GradleUtil.getJarDependencies(
				portalPath, portalPath.resolve("modules"),
				displayGradleProcessOutput, false);
		}
		else {
			for (Path newModulePath : newModulePaths) {
				Path newModulePathName = newModulePath.getFileName();

				FileUtil.delete(
					projectPath.resolve(
						Paths.get("modules", newModulePathName.toString())));

				if (Files.exists(newModulePath.resolve("build.gradle"))) {
					jarDependenciesMap.putAll(
						GradleUtil.getJarDependencies(
							portalPath, newModulePath,
							displayGradleProcessOutput, true));
				}
			}

			GradleUtil.stopGradleDaemon(portalPath, displayGradleProcessOutput);
		}

		Set<Dependency> portalLibJars = ModuleUtil.getPortalLibJars(portalPath);

		for (Path newModulePath : newModulePaths) {
			Module module = Module.createModule(
				projectPath.resolve("modules"), newModulePath,
				jarDependenciesMap.get(
					String.valueOf(newModulePath.getFileName())),
				portalModuleDependencyProperties);

			modules.add(module);

			CreateModule.createModule(
				module, projectPath, portalLibJars, portalPath);
		}

		CreateUmbrella.createUmbrella(
			portalPath, projectName, umbrellaSourceList, excludedTypes,
			moduleNames, projectPath.resolve("umbrella"));

		Map<Path, List<Module>> moduleGroups = _createModuleGroups(
			modules, groupDepth, groupStopWords);

		Path groupProjectPath = projectPath.resolve("group-modules");

		FileUtil.delete(groupProjectPath);

		for (Map.Entry<Path, List<Module>> entry : moduleGroups.entrySet()) {
			CreateGroupModule.createModule(
				groupProjectPath, portalPath, entry.getKey(), entry.getValue());
		}
	}

	private Map<Path, List<Module>> _createModuleGroups(
		List<Module> modules, int groupDepth, List<String> groupStopWords) {

		Map<Path, List<Module>> moduleGroups = new HashMap<>();

		for (Module module : modules) {
			Path groupPath = module.getModulePath();

			for (int i = 1; i < groupDepth; i++) {
				if (groupStopWords.contains(
						String.valueOf(
							groupPath.getName(groupPath.getNameCount() - 2)))) {

					break;
				}

				groupPath = groupPath.getParent();
			}

			List<Module> moduleGroup = moduleGroups.get(groupPath);

			if (moduleGroup == null) {
				moduleGroup = new ArrayList<>();

				moduleGroups.put(groupPath, moduleGroup);
			}

			moduleGroup.add(module);
		}

		return moduleGroups;
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