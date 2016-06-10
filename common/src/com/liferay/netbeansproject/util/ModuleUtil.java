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

import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.security.MessageDigest;

/**
 * @author Tom Wang
 */
public class ModuleUtil {

	public static void appendPathToStringBuilder(Path path, StringBuilder sb) {
		if (path == null) {
			sb.append('\n');
		}
		else {
			sb.append(path);
			sb.append('\n');
		}
	}

	public static void createModuleInfo(Module module, Path projectPath)
		throws IOException {

		StringBuilder sb = new StringBuilder();

		sb.append("ModuleName=");
		sb.append(module.getModuleName());
		sb.append('\n');
		sb.append("ModulePath=");

		Path modulePath = module.getModulePath();

		sb.append(modulePath);
		sb.append('\n');
		sb.append("SourcePath=");

		appendPathToStringBuilder(module.getSourcePath(), sb);

		sb.append("SourceResourcePath=");

		appendPathToStringBuilder(module.getSourceResourcePath(), sb);

		sb.append("TestUnitPath=");

		appendPathToStringBuilder(module.getTestUnitPath(), sb);

		sb.append("TestUnitResourcePath=");

		appendPathToStringBuilder(module.getTestUnitResourcePath(), sb);

		sb.append("TestIntegrationPath=");

		appendPathToStringBuilder(module.getTestIntegrationPath(), sb);

		sb.append("TestIntegrationResourcePath=");

		appendPathToStringBuilder(module.getTestIntegrationResourcePath(), sb);

		sb.append("checksum=");

		Path gradleFilePath = modulePath.resolve("build.gradle");

		try {
			if (Files.exists(gradleFilePath)) {
				MessageDigest messageDigest = MessageDigest.getInstance("MD5");

				byte[] hash = messageDigest.digest(
					Files.readAllBytes(gradleFilePath));

				sb.append(StringUtil.bytesToHexString(hash));
			}
		}
		catch (Exception e) {
			throw new IOException(e);
		}

		Files.createDirectories(projectPath);

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				projectPath.resolve("ModuleInfo.properties"),
				Charset.defaultCharset(), StandardOpenOption.CREATE)) {

			bufferedWriter.append(sb);
		}
	}

	public static String getModuleName(Path modulePath) {
		Path moduleName = modulePath.getFileName();

		if ("WEB-INF".equals(moduleName.toString())) {
			moduleName = modulePath.getName(modulePath.getNameCount() - 3);
		}

		return moduleName.toString();
	}

}