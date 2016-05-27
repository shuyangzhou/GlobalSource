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
import com.liferay.netbeansproject.container.Module.JarDependency;
import com.liferay.netbeansproject.creators.ModuleCreator;
import com.liferay.netbeansproject.util.ArgumentsUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class PortalScanner {
	public static void main(String[] args) throws Exception {
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		PortalScanner portalScanner = new PortalScanner();

		portalScanner.scanPortal(Paths.get(arguments.get("portal.dir")));
	}

	public void scanPortal(Path portalPath) throws Exception {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path portalNamePath = portalPath.getFileName();

		Path projectPath = Paths.get(
			properties.getProperty("project.dir"),portalNamePath.toString());

		final String ignoredDirs = properties.getProperty("ignored.dirs");

		final Map<String, List<JarDependency>> jarDependenciesMap =
				ProcessGradle.processGradle(
					portalPath, projectPath, portalPath.resolve("modules"));

		Files.walkFileTree(
			portalPath,
			EnumSet.allOf(FileVisitOption.class),
			Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(
					Path path, BasicFileAttributes basicFileAttributes)
				throws IOException {

				Path fileNamePath = path.getFileName();

				String fileName = fileNamePath.toString();

				if (ignoredDirs.contains(fileName)) {
					return FileVisitResult.SKIP_SUBTREE;
				}

				if (Files.exists(path.resolve("src"))) {
					try {
						Module module = ModuleCreator.createModule(
							jarDependenciesMap.get(fileName), path);
					}
					catch (Exception e) {
						throw new IOException(e);
					}

					return FileVisitResult.SKIP_SUBTREE;
				}

				return FileVisitResult.CONTINUE;
			}

		});

		CreateUmbrella c = new CreateUmbrella();

		c.createUmbrella(portalPath);
	}

}
