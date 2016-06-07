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

import com.liferay.netbeansproject.container.ModuleDependency;

import java.io.BufferedReader;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tom Wang
 */
public class GradleUtil {

	public static List<ModuleDependency> getModuleDependencies(Path modulePath)
		throws IOException {

		Path buildGradlePath = modulePath.resolve("build.gradle");

		if (!Files.exists(buildGradlePath)) {
			return Collections.emptyList();
		}

		List<ModuleDependency> moduleDependencies = new ArrayList<>();

		try(
			BufferedReader bufferedReader = Files.newBufferedReader(
				buildGradlePath, StandardCharsets.UTF_8)) {

			String line = null;

			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();

				if (!line.contains(" project(")) {
					continue;
				}

				int index1 = line.indexOf('\"');

				if (index1 < 0) {
					throw new IllegalStateException(
						"Broken syntax in " + buildGradlePath);
				}

				int index2 = line.indexOf('\"', index1 + 1);

				if (index2 < 0) {
					throw new IllegalStateException(
						"Broken syntax in " + buildGradlePath);
				}

				moduleDependencies.add(
					new ModuleDependency(line.substring(index1 + 1, index2),
					line.startsWith("test")));
			}
		}

		return moduleDependencies;
	}

}