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
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Tom Wang
 */
public class ZipUtil {

	public static void unZip(final Path destinationPath)
		throws IOException {

		Files.createDirectories(destinationPath);

		FileSystem fileSystem = FileSystems.newFileSystem(
			Paths.get("CleanProject.zip"), null);

		for (final Path path : fileSystem.getRootDirectories()) {
			Files.walkFileTree(
				path,
				new SimpleFileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(
							Path dirPath,
							BasicFileAttributes basicFileAttributes)
						throws IOException {

						Path relativePath = path.relativize(dirPath);

						dirPath = destinationPath.resolve(
							relativePath.toString());

						if (Files.notExists(dirPath)) {
							Files.createDirectory(dirPath);
						}

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(
							Path filePath,
							BasicFileAttributes basicFileAttributes)
						throws IOException {

						Path relativePath = path.relativize(filePath);

						Files.copy(
							filePath,
							destinationPath.resolve(relativePath.toString()),
							StandardCopyOption.REPLACE_EXISTING);

						return FileVisitResult.CONTINUE;
					}

				});
		}
	}

}
