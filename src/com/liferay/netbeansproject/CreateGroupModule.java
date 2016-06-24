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

import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.FileUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.List;

/**
 * @author Tom Wang
 */
public class CreateGroupModule {

	public static void createModule(
			Path projectPath, Path groupPath, List<Module> moduleList)
		throws IOException {

		if (groupPath.equals(Paths.get(""))) {
			groupPath = groupPath.resolve("portal");
		}

		String groupPathString = groupPath.toString();

		projectPath = projectPath.resolve(groupPathString.replace('/', '-'));

		FileUtil.unZip(projectPath);

		_replaceProjectName(groupPathString, projectPath);
	}

	private static void _replaceProjectName(
			String projectName, Path projectModulePath)
		throws IOException {

		Path buildXMLPath = projectModulePath.resolve("build.xml");

		String content = StringUtil.replace(
			new String(Files.readAllBytes(buildXMLPath)), "%placeholder%",
			projectName);

		Files.write(buildXMLPath, Arrays.asList(content));
	}

}