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

import java.nio.file.Path;

/**
 * @author Tom Wang
 */
public class JarDependency {

	public JarDependency(Path jarPath, boolean test) {
		_jarPath = jarPath;
		_test = test;
	}

	public Path getJarPath() {
		return _jarPath;
	}

	public boolean isTest() {
		return _test;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("{jarPath=");
		sb.append(_jarPath);
		sb.append(", test=");
		sb.append(_test);
		sb.append("}");

		return sb.toString();
	}

	private final Path _jarPath;
	private final boolean _test;

}