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

import com.liferay.netbeansproject.template.FreeMarkerUtil;
import com.liferay.netbeansproject.util.FileUtil;

import java.io.Writer;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Tom Wang
 */
public class CreateUmbrella {

	public static void createUmbrella(
			Path portalPath, Set<String> moduleNames, Path projectPath)
		throws Exception {

		FileUtil.delete(projectPath);

		Files.createDirectories(projectPath.resolve("nbproject"));

		try (Writer writer = Files.newBufferedWriter(
				projectPath.resolve("build.xml"))) {

			FreeMarkerUtil.process(
				"resources/build_xml.ftl",
				Collections.singletonMap(
					"projectName", "IndividualModuleUmbrella"),
				writer);
		}

		Map<String, Object> data = new HashMap<>();

		data.put("portalPath", portalPath);

		Path projectParentPath = projectPath.getParent();

		data.put("projectModulesPath", projectParentPath.resolve("modules"));
		data.put("moduleNames", moduleNames);

		try (Writer writer = Files.newBufferedWriter(
				projectPath.resolve("nbproject/project.properties"))) {

			FreeMarkerUtil.process(
				"resources/umbrella_project_properties.ftl", data, writer);
		}

		data.put("portalName", portalPath.getFileName());

		try (Writer writer = Files.newBufferedWriter(
				projectPath.resolve("nbproject/project.xml"))) {

			FreeMarkerUtil.process(
				"resources/umbrella_project_xml.ftl", data, writer);
		}
	}

}