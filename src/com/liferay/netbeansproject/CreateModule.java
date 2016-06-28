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
import com.liferay.netbeansproject.util.FreeMarkerUtil;

import java.io.FileWriter;
import java.io.Writer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Tom Wang
 */
public class CreateModule {

	public static void createModule(
			Module module, Path projectPath, Set<Dependency> portalLibJars,
			Path portalPath)
		throws Exception {

		Path projectModulePath = projectPath.resolve(
			Paths.get("modules", module.getModuleName()));

		Files.createDirectories(projectModulePath.resolve("nbproject"));

		_generateBuildXML(module, projectModulePath.resolve("build.xml"));

		_appendProperties(
			module, portalLibJars,
			projectModulePath.resolve("nbproject/project.properties"));

		_createProjectXML(
			module, portalPath.getParent(),
			projectModulePath.resolve("nbproject/project.xml"));
	}

	private static void _appendProperties(
			Module module, Set<Dependency> portalLibJars,
			Path projectPropertiesPath)
		throws Exception {

		Map<String, Object> data = new HashMap<>();

		data.put("module", module);
		data.put("portalLibJars", portalLibJars);

		try (Writer writer = new FileWriter(projectPropertiesPath.toFile())) {
			FreeMarkerUtil.process(
				"resources/projectProperties.ftl", data, writer);
		}
	}

	private static void _createProjectXML(
			Module module, Path portalParentPath, Path projectXMLPath)
		throws Exception {

		Map<String, Object> data = new HashMap<>();

		data.put("module", module);
		data.put(
			"moduleDisplayName",
			portalParentPath.relativize(module.getModulePath()));

		try (Writer writer = new FileWriter(projectXMLPath.toFile())) {
			FreeMarkerUtil.process("resources/projectXML.ftl", data, writer);
		}
	}

	private static void _generateBuildXML(Module module, Path buildXMLPath)
		throws Exception {

		Map<String, String> data = new HashMap<>();

		data.put("projectName", module.getModuleName());

		try (Writer writer = new FileWriter(buildXMLPath.toFile())) {
			FreeMarkerUtil.process("resources/buildXML.ftl", data, writer);
		}
	}

}