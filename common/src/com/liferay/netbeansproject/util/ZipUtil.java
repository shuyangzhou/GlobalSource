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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Tom Wang
 */
public class ZipUtil {

	public static void unZip(Path zipPath, final Path destinationPath)
		throws IOException {

		FileSystem fileSystem = FileSystems.newFileSystem(zipPath,null);

		for (final Path path : fileSystem.getRootDirectories()) {
			Files.walkFileTree(
				path,
				new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
					Path vistFilePath, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path relativeDir = path.relativize(vistFilePath);

					Path pastePath = destinationPath.resolve(
						relativeDir.toString());

					if (!Files.exists(pastePath)) {
						Files.createDirectories(pastePath);
					}

					Files.copy(
						vistFilePath, pastePath,
						StandardCopyOption.REPLACE_EXISTING);

					return FileVisitResult.CONTINUE;
				}

			});
		}
	}

}
