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

import java.io.IOException;

import java.nio.file.Path;

import java.util.List;

/**
 * @author Tom Wang
 */
public class CreateGroupModule {

	public static void createModule(
			Path projectPath, Path portalPath, Path groupPath,
			List<Module> moduleList)
		throws IOException {

		String projectName = _createProjectName(portalPath, groupPath);

		projectPath = projectPath.resolve(projectName);

		FileUtil.unZip(projectPath);
	}

	private static String _createProjectName(Path portalPath, Path groupPath) {
		if (portalPath.equals(groupPath)) {
			return "portal";
		}

		groupPath = portalPath.relativize(groupPath);

		String projectName = groupPath.toString();

		return projectName.replace('/', ':');
	}

}