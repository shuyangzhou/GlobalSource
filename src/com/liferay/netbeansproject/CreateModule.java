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
import java.nio.file.Paths;

import java.util.Collections;
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
			module, portalPath, portalLibJars,
			projectModulePath.resolve("nbproject/project.properties"));

		_createProjectXML(
			module, portalPath.getParent(),
			projectModulePath.resolve("nbproject/project.xml"));
	}

	private static void _appendProperties(
			Module module, Path portalPath, Set<Dependency> portalLibJars,
			Path projectPropertiesPath)
		throws Exception {

		Map<String, Object> data = new HashMap<>();

		data.put("module", module);
		data.put("portalPath", portalPath);
		data.put("portalLibJars", portalLibJars);

		try (Writer writer = Files.newBufferedWriter(projectPropertiesPath)) {
			FreeMarkerUtil.process(
				"resources/project_properties.ftl", data, writer);
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

		try (Writer writer = Files.newBufferedWriter(projectXMLPath)) {
			FreeMarkerUtil.process("resources/project_xml.ftl", data, writer);
		}
	}

	private static void _generateBuildXML(Module module, Path buildXMLPath)
		throws Exception {

		try (Writer writer = Files.newBufferedWriter(buildXMLPath)) {
			FreeMarkerUtil.process(
				"resources/build_xml.ftl",
				Collections.singletonMap("projectName", module.getModuleName()),
				writer);
		}
	}

}