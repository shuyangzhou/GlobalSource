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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tom Wang
 */
public class CreateGroupModule {

	public static void createModule(
			Path projectPath, Path portalPath, Path groupPath,
			List<Module> moduleList, Set<Dependency> portalLibJars)
		throws Exception {

		String projectName = _createProjectName(portalPath, groupPath);

		projectPath = projectPath.resolve(projectName);

		Files.createDirectories(projectPath.resolve("nbproject"));

		_generateBuildXML(projectName, projectPath.resolve("build.xml"));

		Set<Dependency> jarDependencies = _filterJarDependencies(moduleList);

		Set<Dependency> moduleDependencies = _filterModuleDependencies(
			moduleList, portalPath.relativize(groupPath));

		_appendProperties(
			projectName, moduleList, jarDependencies, moduleDependencies,
			portalLibJars, projectPath.resolve("nbproject/project.properties"));

		_createProjectXML(
			projectName, moduleList, moduleDependencies,
			projectPath.resolve("nbproject/project.xml"));
	}

	private static void _appendProperties(
			String projectName, List<Module> moduleList,
			Set<Dependency> jarDependencies, Set<Dependency> moduleDependencies,
			Set<Dependency> portalLibJars, Path projectPropertiesPath)
		throws Exception {

		Map<String, Object> data = new HashMap<>();

		data.put("projectName", projectName);
		data.put("moduleList", moduleList);
		data.put("jarDependencies", jarDependencies);
		data.put("moduleDependencies", moduleDependencies);
		data.put("portalLibJars", portalLibJars);

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

		data.put("projectName", projectName);
		data.put("moduleList", moduleList);
		data.put("moduleDependencies", moduleDependencies);

		try (Writer writer = Files.newBufferedWriter(projectXMLPath)) {
			FreeMarkerUtil.process(
				"resources/group_project_xml.ftl", data, writer);
		}
	}

	private static Set<Dependency> _filterJarDependencies(
		List<Module> moduleList) {

		Map<Dependency, Dependency> jarSet = new HashMap<>();

		for (Module module : moduleList) {
			for (Dependency dependency : module.getJarDependencies()) {
				if (!jarSet.containsKey(dependency)) {
					jarSet.put(dependency, dependency);
				}
				else {
					Dependency current = jarSet.get(dependency);

					if (current.isTest() && !dependency.isTest()) {
						jarSet.remove(dependency);
						jarSet.put(dependency, dependency);
					}
				}
			}
		}

		return jarSet.keySet();
	}

	private static Set<Dependency> _filterModuleDependencies(
		List<Module> moduleList, Path groupPath) {

		Set<Dependency> moduleSet = new HashSet<>();

		for (Module module : moduleList) {
			for (Dependency dependency : module.getModuleDependencies()) {
				Path dependencyPath = dependency.getPath();

				if (!dependencyPath.startsWith(groupPath)) {
					moduleSet.add(dependency);
				}
			}
		}

		return moduleSet;
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