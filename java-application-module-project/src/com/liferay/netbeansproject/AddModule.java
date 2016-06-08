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
import com.liferay.netbeansproject.util.ArgumentsUtil;
import com.liferay.netbeansproject.util.ModuleUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.ZipUtil;

import java.io.IOException;

import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class AddModule {

	public static void main(String[] args) throws IOException {
		AddModule addModule = new AddModule();

		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		addModule.addModule(Paths.get(arguments.get("portal.dir")));
	}

	public void addModule(final Path portalPath) throws IOException {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path portalName = portalPath.getFileName();

		final Path projectRootPath = Paths.get(
			properties.getProperty("project.dir"), portalName.toString());

		final Map<String, Module> currentProjectMap = _getExistingProjects(
			projectRootPath);

		final String ignoredDirs = properties.getProperty("ignored.dirs");

		Files.walkFileTree(
			portalPath, EnumSet.allOf(FileVisitOption.class), Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path pathFileName = path.getFileName();

					if (ignoredDirs.contains(pathFileName.toString())) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (!Files.exists(path.resolve("src"))) {
						return FileVisitResult.CONTINUE;
					}

					String moduleName = ModuleUtil.getModuleName(path);

					if (!currentProjectMap.containsKey(moduleName)) {
						try {
							ProcessGradle.processGradle(
								portalPath, projectRootPath, path);

							ZipUtil.unZip(
								projectRootPath.resolve(
									Paths.get("modules", moduleName)));

							CreateModule.createModule(
								projectRootPath, path, portalPath,
								new ArrayList<>(currentProjectMap.keySet()));
						}
						catch (IOException ioe) {
							throw ioe;
						}
						catch (Exception e) {
							throw new IOException(e);
						}
					}

					return FileVisitResult.SKIP_SUBTREE;
				}

			});
	}

	private Map<String, Module> _getExistingProjects(Path projectRootPath)
		throws IOException {

		Map<String, Module> map = new HashMap<>();

		for (Path path :
				Files.newDirectoryStream(projectRootPath.resolve("modules"))) {

			Properties properties = PropertiesUtil.loadProperties(
				path.resolve("ModuleInfo.properties"));

			String moduleName = properties.getProperty("ModuleName");

			map.put(
				moduleName,
				new Module(
					properties.getProperty("checksum"),
					Paths.get(properties.getProperty("ModulePath")),
					Paths.get(properties.getProperty("SourcePath")),
					Paths.get(properties.getProperty("SourceResourcePath")),
					Paths.get(properties.getProperty("TestIntegrationPath")),
					Paths.get(
						properties.getProperty("TestIntegrationResourcePath")),
					Paths.get(properties.getProperty("TestUnitPath")),
					Paths.get(properties.getProperty("TestUnitResourcePath"))));
		}

		return map;
	}

}