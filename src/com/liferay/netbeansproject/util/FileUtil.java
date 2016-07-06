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

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Tom Wang
 */
public class FileUtil {

	public static void copy(Path sourcePath, Path targetPath)
		throws IOException {

		if (!Files.exists(sourcePath)) {
			throw new IOException(sourcePath + " does not exist");
		}

		Files.walkFileTree(
			sourcePath,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Files.createDirectories(
						targetPath.resolve(sourcePath.relativize(path)));

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path targetFilePath = targetPath.resolve(
						sourcePath.relativize(path));

					if (!Files.exists(targetFilePath)) {
						Files.copy(path, targetFilePath);
					}

					return FileVisitResult.CONTINUE;
				}

			});
	}

	public static void delete(Path projectDirPath) throws IOException {
		if (!Files.exists(projectDirPath)) {
			return;
		}

		Files.walkFileTree(
			projectDirPath,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult postVisitDirectory(
						Path path, IOException ioe)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}

			});
	}

}