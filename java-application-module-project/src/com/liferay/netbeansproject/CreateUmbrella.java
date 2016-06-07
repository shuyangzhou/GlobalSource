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

import com.liferay.netbeansproject.util.ArgumentsUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.ZipUtil;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
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
	public static void main(String[] args) throws Exception {
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		CreateUmbrella createUmbrella = new CreateUmbrella();

		createUmbrella.createUmbrella(Paths.get(arguments.get("portal.dir")));
	}

	public void createUmbrella(Path portalPath) throws Exception {
		Properties buildProperties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path portalNamePath = portalPath.getFileName();

		Path projectPath =
			Paths.get(
				buildProperties.getProperty("project.dir"),
				portalNamePath.toString());

		ZipUtil.unZip(projectPath);

		_appendProjectProperties(
			portalPath, projectPath.resolve("nbproject/project.properties"),
			buildProperties);
	}

	private void _appendProjectProperties(
			Path portalPath, Path propertiesFilePath,
			Properties buildProperties)
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

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
			propertiesFilePath, Charset.defaultCharset(),
			StandardOpenOption.APPEND)) {

			bufferedWriter.append(sb);
			bufferedWriter.newLine();
		}
	}
}
