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
import com.liferay.netbeansproject.container.Module.JarDependency;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Tom Wang
 */
public class GradleDependencyResolverImpl implements GradleDependencyResolver {

	public GradleDependencyResolverImpl(
		Map<Module, List<JarDependency>> dependenciesMap) {

		_dependenciesMap = dependenciesMap;
	}

	@Override
	public List<JarDependency> resolve(Module module) {
		return _dependenciesMap.get(module);
	}

	private static Map<Module, List<JarDependency>> _dependenciesMap;

}