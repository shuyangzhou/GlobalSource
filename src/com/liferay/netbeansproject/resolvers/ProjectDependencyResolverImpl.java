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

package com.liferay.netbeansproject.resolvers;

import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.StringUtil;

import java.nio.file.Path;

import java.util.Map;

/**
 * @author Tom Wang
 */
public class ProjectDependencyResolverImpl
	implements ProjectDependencyResolver {

	public ProjectDependencyResolverImpl(
		Map<Path, Module> projectMap, Path portalDir) {

		_projectMap = projectMap;

		_portalDir = portalDir;
	}

	@Override
	public Module resolve(String moduleLocation) {
		String[] modulePathSplit = StringUtil.split(moduleLocation, ':');

		int splitLength = modulePathSplit.length - 1;

		Path moduleGroupPath = _portalDir.resolve("modules");

		for (int i = 0; i <= splitLength; i++) {
			moduleGroupPath = moduleGroupPath.resolve(modulePathSplit[i]);
		}

		return _projectMap.get(moduleGroupPath);
	}

	private static Path _portalDir;
	private static Map<Path, Module> _projectMap;

}