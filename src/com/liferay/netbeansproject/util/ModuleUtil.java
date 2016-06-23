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

import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.EnumSet;

/**
 * @author Tom Wang
 */
public class ModuleUtil {

	public static String getPortalLibJars(Path portalPath) throws IOException {
		final StringBuilder sb = new StringBuilder();

		Files.walkFileTree(
			portalPath.resolve("lib"), EnumSet.allOf(FileVisitOption.class),
			Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
						Path filePath, BasicFileAttributes attrs)
					throws IOException {

					String filePathString = filePath.toString();

					if (filePathString.endsWith(".jar")) {
						sb.append('\t');
						sb.append(filePathString);
						sb.append(":\\\n");
					}

					return FileVisitResult.CONTINUE;
				}

			});

		return sb.toString();
	}

}