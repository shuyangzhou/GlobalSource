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

package com.liferay.netbeansproject.container;

/**
 * @author Tom Wang
 */
public class ModuleDependency {

	public ModuleDependency(String moduleLocation, boolean test) {
		_moduleLocation = moduleLocation;
		_test = test;
	}

	public String getModuleLocation() {
		return _moduleLocation;
	}

	public boolean isTest() {
		return _test;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("{moduleLocation=");
		sb.append(_moduleLocation);
		sb.append(", test=");
		sb.append(_test);

		return sb.toString();
	}

	private final String _moduleLocation;
	private final boolean _test;

}