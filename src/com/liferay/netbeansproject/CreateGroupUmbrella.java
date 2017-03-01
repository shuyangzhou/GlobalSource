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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Tom Wang
 */
public class CreateGroupUmbrella {

	public static void createUmbrella(
			Path portalPath, Set<Path> groupPathSet, Path groupUmbrellaPath,
			Path trunkPath, String tomcatVersion)
		throws Exception {

		FileUtil.delete(groupUmbrellaPath);

		Files.createDirectories(groupUmbrellaPath.resolve("nbproject"));

		try (Writer writer = Files.newBufferedWriter(
				groupUmbrellaPath.resolve("build.xml"))) {

			FreeMarkerUtil.process(
				"resources/build_xml.ftl",
				Collections.singletonMap("projectName", "GroupModuleUmbrella"),
				writer);
		}

		Set<String> moduleNames = new HashSet<>();

		for (Path groupPath : groupPathSet) {
			moduleNames.add(_createProjectName(portalPath, groupPath));
		}

		Map<String, Object> data = new HashMap<>();

		data.put("portalPath", portalPath);

		Path projectParentPath = groupUmbrellaPath.getParent();

		data.put(
			"projectModulesPath", projectParentPath.resolve("group-modules"));

		data.put("moduleNames", moduleNames);

		data.put("trunkPath", trunkPath);

		data.put("tomcatVersion", tomcatVersion);

		try (Writer writer = Files.newBufferedWriter(
				groupUmbrellaPath.resolve("nbproject/project.properties"))) {

			FreeMarkerUtil.process(
				"resources/umbrella_project_properties.ftl", data, writer);
		}

		data.put("portalName", portalPath.getFileName());

		try (Writer writer = Files.newBufferedWriter(
				groupUmbrellaPath.resolve("nbproject/project.xml"))) {

			FreeMarkerUtil.process(
				"resources/umbrella_project_xml.ftl", data, writer);
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

}