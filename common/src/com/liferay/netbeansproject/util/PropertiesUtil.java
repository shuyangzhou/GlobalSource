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

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class PropertiesUtil {

	public static Map<String, String> getProperties(
		Properties properties, String prefix) {

		Map<String, String> map = new HashMap<>();

		for (String curName : properties.stringPropertyNames()) {
			if ((curName.length() >= (prefix.length() + 2)) &&
				curName.startsWith(prefix) &&
				(curName.charAt(prefix.length()) == '[') &&
				(curName.charAt(curName.length() - 1) == ']')) {

				map.put(
					curName.substring(
						prefix.length() + 1, curName.length() - 1),
					properties.getProperty(curName));
			}
		}

		return map;
	}

	public static Properties loadProperties(Path filePath) throws IOException {
		Properties properties = new Properties();

		try (InputStream in = Files.newInputStream(filePath)) {
			properties.load(in);
		}

		Path fileNamePath = filePath.getFileName();

		String fileName = fileNamePath.toString();

		int index = fileName.indexOf('.');

		if (index < 0) {
			return properties;
		}

		String extFileName =
			fileName.substring(0, index) + "-ext" + fileName.substring(index);

		Path extFilePath = filePath.resolveSibling(extFileName);

		if (Files.exists(extFilePath)) {
			try (InputStream in = Files.newInputStream(extFilePath)) {
				properties.load(in);
			}
		}

		return properties;
	}

}