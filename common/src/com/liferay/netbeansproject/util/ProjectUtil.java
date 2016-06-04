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

package com.liferay.netbeansproject.util;

import com.liferay.netbeansproject.container.Module;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tom Wang
 */
public class ProjectUtil {

	public static void linkModuletoMap(
		Map<Path, Map<String, Module>> projectMap, Module module) {

		Path modulePath = module.getModulePath();

		Path parentPath = modulePath.getParent();

		Map<String, Module> moduleMap = projectMap.get(parentPath);

		if (moduleMap == null) {
			moduleMap = new HashMap<>();
		}

		moduleMap.put(module.getModuleName(), module);

		projectMap.put(parentPath, moduleMap);
	}

}