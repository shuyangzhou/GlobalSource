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

import com.liferay.netbeansproject.container.Dependency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Tom Wang
 */
public class GradleUtil {

	public static Set<Dependency> getModuleDependencies(
			Path modulePath, Map<String, Path> moduleProjectPaths)
		throws IOException {

		Path buildGradlePath = modulePath.resolve("build.gradle");

		if (!Files.exists(buildGradlePath)) {
			return new HashSet<>();
		}

		Set<Dependency> moduleDependencies = new HashSet<>();

		for (String line : Files.readAllLines(buildGradlePath)) {
			Path moduleProjectPath = null;

			if (line.contains(" project(") || line.contains(" project (") ||
				line.contains(" (project(") || line.contains(" (project (")) {

				moduleProjectPath = Paths.get(
					"modules",
					StringUtil.split(
						StringUtil.extractQuotedText(line.trim()), ':'));
			}
			else if (line.contains("name: \"com.liferay")) {
				String[] split = StringUtil.split(line.trim(), ',');

				String moduleSymbolicName = StringUtil.extractQuotedText(
					split[1]);

				moduleProjectPath = moduleProjectPaths.get(moduleSymbolicName);

				if (moduleProjectPath == null) {
					continue;
				}
			}
			else {
				continue;
			}

			moduleDependencies.add(
				new Dependency(
					moduleProjectPath, null, line.startsWith("test")));
		}

		return moduleDependencies;
	}

	private static boolean _checkPortalSnapshotsVersions(Path portalDirPath)
		throws IOException {

		Files.walkFileTree(
			portalDirPath, EnumSet.allOf(FileVisitOption.class),
			Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path bndPath = path.resolve("bnd.bnd");

					if (Files.exists(bndPath)) {
						String dirName = String.valueOf(path.getFileName());

						dirName = dirName.replace("-", ".");

						Path metadataPath = portalDirPath.resolve(
							".m2/com/liferay/portal/com.liferay." + dirName +
								"/maven-metadata-local.xml");

						try {
							if (!_getMetadataVersion(
									metadataPath).startsWith(
										_getBundleVersion(bndPath))) {

								_installPortalSnapshot(path);
							}
						}
						catch (Exception e) {
							throw new IOException(e);
						}

						return FileVisitResult.SKIP_SUBTREE;
					}

					if ((path != portalDirPath) &&
						(path.getParent() != portalDirPath)) {

						return FileVisitResult.SKIP_SUBTREE;
					}

					return FileVisitResult.CONTINUE;
				}

			});

		return true;
	}

	private static String _getBundleVersion(Path bndPath) throws IOException {
		List<String> content = Files.readAllLines(bndPath);

		for (String line : content) {
			if (line.startsWith("Bundle-Version:")) {
				String[] split = StringUtil.split(line, ":");

				return split[1].trim();
			}
		}

		return null;
	}

	private static Set<Dependency> _getConfigurationDependencies(
			Path dependencyPath, String configurationName, String sourceName,
			boolean test, String portalToolsPath, Set<String> symbolicNameSet)
		throws IOException {

		Properties dependencies = PropertiesUtil.loadProperties(dependencyPath);

		Map<String, Path> sourceJarPaths = _loadSourceJarPaths(
			dependencies.getProperty(sourceName));

		Set<Dependency> jarDependencies = new HashSet<>();

		for (String jar :
				StringUtil.split(
					dependencies.getProperty(configurationName), ':')) {

			Path jarPath = Paths.get(jar);

			String jarName = String.valueOf(jarPath.getFileName());

			if (!jar.contains("com.liferay.osgi.felix.util")) {
				if (jar.startsWith(portalToolsPath)) {
					continue;
				}

				if (jarName.startsWith("com.liferay")) {
					String[] jarPathSplit = StringUtil.split(jarName, '-');

					if (symbolicNameSet.contains(jarPathSplit[0])) {
						continue;
					}
				}
			}

			jarDependencies.add(
				new Dependency(
					jarPath,
					sourceJarPaths.get(String.valueOf(jarPath.getFileName())),
					test));
		}

		return jarDependencies;
	}

	private static String _getMetadataVersion(Path metadataPath)
		throws Exception {

		DocumentBuilderFactory documentBulderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBulderFactory.newDocumentBuilder();

		Document document = documentBuilder.parse(metadataPath.toFile());

		NodeList nodeList = document.getElementsByTagName("version");

		Node node = nodeList.item(0);

		return node.getTextContent();
	}

	private static String _getTaskName(Path portalDirPath, Path workDirPath) {
		Path modulesPath = portalDirPath.resolve("modules");

		Path relativeWorkPath = modulesPath.relativize(workDirPath);

		String relativeWorkPathString = relativeWorkPath.toString();

		if (relativeWorkPathString.isEmpty()) {
			return "printDependencies";
		}

		relativeWorkPathString = relativeWorkPathString.replace('/', ':');

		return relativeWorkPathString.concat(
			":"
		).concat(
			"printDependencies"
		);
	}

	private static void _installPortalSnapshot(Path path) throws IOException {
		List<String> antTask = new ArrayList<>();

		antTask.add("ant");

		antTask.add("install-portal-snapshot");

		ProcessBuilder processBuilder = new ProcessBuilder(antTask);

		processBuilder.directory(path.toFile());

		Process process = processBuilder.start();

		String line = null;

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(process.getInputStream()))) {

			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		}

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(process.getErrorStream()))) {

			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		}
	}

	private static Map<String, Path> _loadSourceJarPaths(String sources) {
		Map<String, Path> sourceJarPaths = new HashMap<>();

		for (String sourceJarLocation : StringUtil.split(sources, ':')) {
			Path path = Paths.get(sourceJarLocation);

			String fileName = String.valueOf(path.getFileName());

			if (fileName.startsWith("javax.portlet") ||
				fileName.startsWith("javax.servlet")) {

				if (!(sourceJarLocation.contains("org.glassfish.web") &&
					  !sourceJarLocation.contains("LIFERAY-PATCHED"))) {

					continue;
				}
			}

			sourceJarPaths.put(
				StringUtil.replace(fileName, "-sources.jar", ".jar"), path);
		}

		return sourceJarPaths;
	}

}