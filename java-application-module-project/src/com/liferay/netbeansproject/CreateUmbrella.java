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
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.ZipUtil;

import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class CreateUmbrella {

	public static void createUmbrella(
			Map<Path, Map<String, Module>> projectMap, Path portalPath,
			Properties buildProperties)
		throws IOException {

		Path portalNamePath = portalPath.getFileName();

		Path projectPath = Paths.get(
			buildProperties.getProperty("project.dir"),
			portalNamePath.toString());

		ZipUtil.unZip(projectPath);

		_appendProjectProperties(
			projectMap, portalPath, projectPath, buildProperties);
	}

	private static void _appendProjectProperties(
			Map<Path, Map<String, Module>> projectMap, Path portalPath,
			Path projectPath, Properties buildProperties)
		throws IOException {

		StringBuilder sb = new StringBuilder();

		sb.append("excludes=");
		sb.append(buildProperties.getProperty("exclude.types"));
		sb.append('\n');

		Map<String, String> umbrellaSourceMap = PropertiesUtil.getProperties(
			buildProperties, "umbrella.source.list");

		for (Entry<String, String> source : umbrellaSourceMap.entrySet()) {
			String s = source.getKey();

			sb.append("file.reference.");
			sb.append(s);
			sb.append(".src=");
			sb.append(portalPath.resolve(source.getValue()));
			sb.append('\n');
			sb.append("src.");
			sb.append(s);
			sb.append(".dir=${file.reference.");
			sb.append(s);
			sb.append(".src}");
			sb.append('\n');
		}

		Path projectModulesPath = projectPath.resolve("modules");

		for (Map<String, Module> map : projectMap.values()) {
			for (String name : map.keySet()) {
				sb.append("project.");
				sb.append(name);
				sb.append('=');
				sb.append(projectModulesPath.resolve(name));
				sb.append('\n');
				sb.append("reference.");
				sb.append(name);
				sb.append(".jar=${project.");
				sb.append(name);
				sb.append("}/dist/");
				sb.append(name);
				sb.append(".jar\n");
			}
		}

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				projectPath.resolve("nbproject/project.properties"),
				StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {

			bufferedWriter.append(sb);
			bufferedWriter.newLine();
		}
	}

}