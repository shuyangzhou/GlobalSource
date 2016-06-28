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

import com.liferay.netbeansproject.util.HashUtil;

import java.nio.file.Path;

import java.util.Objects;

/**
 * @author Tom Wang
 */
public class Dependency {

	public Dependency(Path path, boolean test) {
		_path = path;
		_test = test;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Dependency)) {
			return false;
		}

		Dependency dependency = (Dependency)obj;

		if (Objects.equals(_path, dependency._path)) {
			return true;
		}

		return false;
	}

	public Path getName() {
		return _path.getFileName();
	}

	public Path getPath() {
		return _path;
	}

	@Override
	public int hashCode() {
		int hashCode = HashUtil.hash(0, _path);

		hashCode = HashUtil.hash(hashCode, _test);

		return hashCode;
	}

	public boolean isTest() {
		return _test;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("{Path=");
		sb.append(_path);
		sb.append(", test=");
		sb.append(_test);
		sb.append("}");

		return sb.toString();
	}

	private final Path _path;
	private final boolean _test;

}