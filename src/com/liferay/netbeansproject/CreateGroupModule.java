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

import com.liferay.netbeansproject.container.Dependency;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.FileUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Tom Wang
 */
public class CreateGroupModule {

	public static void createModule(
			Path projectPath, String portalName, Path groupPath,
			List<Module> moduleList, String excludedTypes, String portalLibJars)
		throws IOException {

		if (groupPath.equals(Paths.get(""))) {
			groupPath = groupPath.resolve("portal");
		}

		String groupPathString = groupPath.toString();

		groupPathString = groupPathString.replace('/', '-');

		projectPath = projectPath.resolve(groupPathString);

		FileUtil.unZip(projectPath);

		_replaceProjectName(groupPathString, projectPath);

		_appendProperties(
			groupPathString, portalName, moduleList, excludedTypes,
			portalLibJars, projectPath.resolve("nbproject/project.properties"));
	}

	private static void _appendJars(Set<Path> jarSet, StringBuilder sb) {
		for (Path jarPath : jarSet) {
			sb.append('\t');
			sb.append(jarPath);
			sb.append(":\\\n");
		}
	}

	private static void _appendProjectDependencies(
		String moduleName, String portalName, StringBuilder projectSB,
		StringBuilder javacSB) {

		projectSB.append("project.");
		projectSB.append(moduleName);
		projectSB.append("=../../");
		projectSB.append(portalName);
		projectSB.append('/');
		projectSB.append("modules");
		projectSB.append('/');
		projectSB.append(moduleName);
		projectSB.append('\n');
		projectSB.append("reference.");
		projectSB.append(moduleName);
		projectSB.append(".jar=${project.");
		projectSB.append(moduleName);
		projectSB.append("}/dist/");
		projectSB.append(moduleName);
		projectSB.append(".jar\n");

		javacSB.append("\t${reference.");
		javacSB.append(moduleName);
		javacSB.append(".jar}:\\\n");
	}

	private static void _appendProperties(
			String groupPathString, String portalName, List<Module> moduleList,
			String excludeTypes, String portalLibJars,
			Path projectPropertiesPath)
		throws IOException {

		StringBuilder projectSB = new StringBuilder();

		projectSB.append("excludes=");
		projectSB.append(excludeTypes);
		projectSB.append('\n');

		projectSB.append("application.title=");
		projectSB.append(groupPathString);
		projectSB.append('\n');

		projectSB.append("dist.jar=${dist.dir}/");
		projectSB.append(groupPathString);
		projectSB.append(".jar\n");

		_appendSourcePaths(moduleList, projectSB);

		Set<Path> javacJars = new LinkedHashSet<>();
		Set<Path> testJars = new LinkedHashSet<>();

		_resolveJarDependencySet(moduleList, javacJars, testJars);

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

		StringBuilder testSB = new StringBuilder("javac.test.classpath=\\\n");

		testSB.append("\t${build.classes.dir}:\\\n");
		testSB.append("\t${javac.classpath}:\\\n");

		_appendJars(javacJars, javacSB);
		_appendJars(testJars, testSB);

		_resolveProjectDependencySet(
			moduleList, portalName, projectSB, javacSB, testSB);

		javacSB.append(portalLibJars);

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				projectPropertiesPath, StandardOpenOption.APPEND)) {

			bufferedWriter.append(projectSB);
			bufferedWriter.newLine();

			javacSB.setLength(javacSB.length() - 3);

			bufferedWriter.append(javacSB);
			bufferedWriter.newLine();

			testSB.setLength(testSB.length() - 3);

			bufferedWriter.append(testSB);
			bufferedWriter.newLine();
		}
	}

	private static void _appendSourcePathIndividual(
		Path path, String prefix, String name, String subfix,
		StringBuilder sb) {

		if (path == null) {
			return;
		}

		sb.append("file.reference.");
		sb.append(name);
		sb.append('-');
		sb.append(subfix);
		sb.append('=');
		sb.append(path);
		sb.append('\n');
		sb.append(prefix);
		sb.append('.');
		sb.append(name);
		sb.append('.');
		sb.append(subfix);
		sb.append(".dir=${file.reference.");
		sb.append(name);
		sb.append('-');
		sb.append(subfix);
		sb.append("}\n");
	}

	private static void _appendSourcePaths(
		List<Module> moduleList, StringBuilder projectSB) {

		for (Module module : moduleList) {
			String moduleName = module.getModuleName();

			_appendSourcePathIndividual(
				module.getSourcePath(), "src", moduleName, "src", projectSB);
			_appendSourcePathIndividual(
				module.getSourceResourcePath(), "src", moduleName, "resources",
				projectSB);
			_appendSourcePathIndividual(
				module.getTestUnitPath(), "test", moduleName, "test-unit",
				projectSB);
			_appendSourcePathIndividual(
				module.getTestUnitResourcePath(), "test", moduleName,
				"test-unit-resources", projectSB);
			_appendSourcePathIndividual(
				module.getTestIntegrationPath(), "test", moduleName,
				"test-integration", projectSB);
			_appendSourcePathIndividual(
				module.getTestIntegrationPath(), "test", moduleName,
				"test-integration-resources", projectSB);
		}
	}

	private static void _replaceProjectName(
			String projectName, Path projectModulePath)
		throws IOException {

		Path buildXMLPath = projectModulePath.resolve("build.xml");

		String content = StringUtil.replace(
			new String(Files.readAllBytes(buildXMLPath)), "%placeholder%",
			projectName);

		Files.write(buildXMLPath, Arrays.asList(content));
	}

	private static void _resolveJarDependencySet(
		List<Module> moduleList, Set<Path> javacJars, Set<Path> testJars) {

		for (Module module : moduleList) {
			for (Dependency jarDependency : module.getJarDependencies()) {
				Path jarPath = jarDependency.getPath();

				if (jarDependency.isTest()) {
					testJars.add(jarPath);
				}
				else {
					javacJars.add(jarPath);
				}
			}
		}
	}

	private static void _resolveProjectDependencySet(
		List<Module> moduleList, String portalName, StringBuilder projectSB,
		StringBuilder javacSB, StringBuilder testSB) {

		Set<String> resolvedSet = new HashSet<>();

		for (Module module : moduleList) {
			for (Dependency moduleDependency : module.getModuleDependencies()) {
				Path dependencyModulePath = moduleDependency.getPath();

				if (!resolvedSet.contains(
						String.valueOf(dependencyModulePath.getFileName()))) {

					if (moduleDependency.isTest()) {
						_appendProjectDependencies(
							String.valueOf(dependencyModulePath.getFileName()),
							portalName, projectSB, testSB);
					}
					else {
						_appendProjectDependencies(
							String.valueOf(dependencyModulePath.getFileName()),
							portalName, projectSB, javacSB);
					}

					resolvedSet.add(
						String.valueOf(dependencyModulePath.getFileName()));
				}
			}

			for (String moduleName : module.getPortalModuleDependencies()) {
				if (!resolvedSet.contains(moduleName)) {
					if (!moduleName.isEmpty()) {
						_appendProjectDependencies(
							moduleName, portalName, projectSB, javacSB);

						resolvedSet.add(moduleName);
					}
				}
			}
		}
	}

}