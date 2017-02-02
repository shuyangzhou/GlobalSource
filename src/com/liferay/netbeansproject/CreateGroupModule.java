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
import com.liferay.netbeansproject.template.FreeMarkerUtil;

import java.io.Writer;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Tom Wang
 */
public class CreateGroupModule {

	public static void createModule(
			Path projectPath, Path portalPath, Path groupPath,
			List<Module> moduleList, Set<Dependency> portalLibJars)
		throws Exception {

		Collections.sort(moduleList);

		String projectName = _createProjectName(portalPath, groupPath);

		projectPath = projectPath.resolve(projectName);

		Files.createDirectories(projectPath.resolve("nbproject"));

		_generateBuildXML(projectName, projectPath.resolve("build.xml"));

		List<Dependency> jarDependencies =
			moduleList.stream()
				.flatMap(module -> module.getJarDependencies().stream())
				.sorted()
				.distinct()
				.collect(Collectors.toList());

		Set<Dependency> moduleDependencies =
			moduleList.stream()
				.flatMap(module -> module.getModuleDependencies().stream())
				.filter(
					dependency -> !dependency.getPath().startsWith(groupPath))
				.collect(Collectors.toSet());

		_appendProperties(
			projectName, moduleList, jarDependencies, moduleDependencies,
			portalLibJars, projectPath.resolve("nbproject/project.properties"));

		_createProjectXML(
			projectName, moduleList, moduleDependencies,
			projectPath.resolve("nbproject/project.xml"));
	}

	private static void _appendProperties(
			String projectName, List<Module> moduleList,
			List<Dependency> jarDependencies,
			Set<Dependency> moduleDependencies, Set<Dependency> portalLibJars,
			Path projectPropertiesPath)
		throws Exception {

		Map<String, Object> data = new HashMap<>();

		data.put("jarDependencies", jarDependencies);
		data.put("moduleDependencies", moduleDependencies);
		data.put("moduleList", moduleList);
		data.put("portalLibJars", portalLibJars);
		data.put("projectName", projectName);

		try (Writer writer = Files.newBufferedWriter(projectPropertiesPath)) {
			FreeMarkerUtil.process(
				"resources/group_project_properties.ftl", data, writer);
		}
	}

	private static String _createProjectName(Path portalPath, Path groupPath) {
		if (portalPath.equals(groupPath)) {
			return "portal";
		}

		groupPath = portalPath.relativize(groupPath);

		String projectName = groupPath.toString();

		return projectName.replace('/', '_');
	}

	private static void _createProjectXML(
			String projectName, List<Module> moduleList,
			Set<Dependency> moduleDependencies, Path projectXMLPath)
		throws Exception {

		Map<String, Object> data = new HashMap<>();

		data.put("moduleDependencies", moduleDependencies);
		data.put("moduleList", moduleList);
		data.put("projectName", projectName);

		try (Writer writer = Files.newBufferedWriter(projectXMLPath)) {
			FreeMarkerUtil.process(
				"resources/group_project_xml.ftl", data, writer);
		}
	}

	private static void _generateBuildXML(String projectName, Path buildXMLPath)
		throws Exception {

		try (Writer writer = Files.newBufferedWriter(buildXMLPath)) {
			FreeMarkerUtil.process(
				"resources/build_xml.ftl",
				Collections.singletonMap("projectName", projectName), writer);
		}
	}

}