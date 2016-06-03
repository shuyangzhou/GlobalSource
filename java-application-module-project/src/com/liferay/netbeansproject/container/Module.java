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

package com.liferay.netbeansproject.container;

import com.liferay.netbeansproject.resolvers.ProjectDependencyResolver;
import com.liferay.netbeansproject.util.GradleUtil;
import com.liferay.netbeansproject.util.ModuleUtil;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tom Wang
 */
public class Module {

	public Module(List<JarDependency> jarDependencys, Path modulePath)
		throws Exception {

		if (jarDependencys == null) {
			jarDependencys = new ArrayList<>();
		}

		Path moduleLibPath = modulePath.resolve("lib");

		if (Files.exists(moduleLibPath)) {

			DirectoryStream<Path> moduleLibFiles = Files.newDirectoryStream(
				moduleLibPath);

			for (Path jar : moduleLibFiles) {
				jarDependencys.add(new JarDependency(jar, false));
			}
		}

		_modulePath = modulePath;
		_sourcePath =  _resolveSourcePath(modulePath);
		_sourceResourcePath = _resolveResourcePath(modulePath, "main");
		_testUnitPath = _resolveTestPath(modulePath, _TESTUNITPATH, "unit");
		_testUnitResourcePath = _resolveResourcePath(modulePath, "test");
		_testIntegrationPath = _resolveTestPath(
			modulePath, _TESTINTEGRATIONPATH, "integration");
		_testIntegrationResourcePath = _resolveResourcePath(
			modulePath, "integration");
		_moduleDependencies = GradleUtil.getModuleDependencies(modulePath);
		_moduleJarDependencies = jarDependencys;
	}

	public List<ModuleDependency> getModuleDependencies() {
		return _moduleDependencies;
	}

	public List<JarDependency> getModuleJarDependencies() {
		return _moduleJarDependencies;
	}

	public String getModuleName() {
		return ModuleUtil.getModuleName(_modulePath);
	}

	public Path getModulePath() {
		return _modulePath;
	}

	public Path getSourcePath() {
		return _sourcePath;
	}

	public Path getSourceResourcePath() {
		return _sourceResourcePath;
	}

	public Path getTestIntegrationPath() {
		return _testIntegrationPath;
	}

	public Path getTestIntegrationResourcePath() {
		return _testIntegrationResourcePath;
	}

	public Path getTestUnitPath() {
		return _testUnitPath;
	}

	public Path getTestUnitResourcePath() {
		return _testUnitResourcePath;
	}

	public static class ModuleDependency {

		public ModuleDependency(String modulePath, boolean test) {
			_modulePath = modulePath;
			_test = test;
		}

		public Module getModule(
			ProjectDependencyResolver projectDependencyResolver) {

			return projectDependencyResolver.resolve(_modulePath);
		}

		public boolean isTest() {
			return _test;
		}

		private final String _modulePath;
		private final boolean _test;

	}

	private static boolean _checkIfEmpty(Path path) throws IOException {
		DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);

		boolean isHidden = false;

		int count = 0;

		for (Path dirPath : directoryStream) {
			count++;

			if (Files.isHidden(dirPath)) {
				isHidden = true;
			}
		}

		if (count == 0) {
			return true;
		}
		else if (count < 2) {
			return isHidden;
		}

		return false;
	}

	private static Path _checkResourcePath(Path modulePath, String type)
		throws IOException {

		Path resourcePath = modulePath.resolve(
			Paths.get("src", type, "resources"));

		if (Files.exists(resourcePath)) {

			if(_checkIfEmpty(resourcePath)) {
				return null;
			}

			return resourcePath;
		}

		return null;
	}

	private static Path _resolvePath(Path modulePath, Path pathType) {
		modulePath = modulePath.resolve(pathType);

		if (Files.exists(modulePath)) {
			return modulePath;
		}

		modulePath = modulePath.getParent();

		if (Files.exists(modulePath)) {
			return null;
		}

		modulePath = modulePath.getParent();

		if (Files.exists(modulePath)) {
			return modulePath;
		}

		return null;
	}

	private static Path _resolveResourcePath(Path modulePath, String type)
		throws IOException {

		Path resourcePath = _checkResourcePath(modulePath, type);

		if (resourcePath != null) {
			return resourcePath;
		}
		else {
			resourcePath = modulePath.resolve(_DOCROOTPATH);

			return _checkResourcePath(resourcePath, type);
		}
	}

	private static Path _resolveSourcePath(Path modulePath) {
		Path sourcePath = _resolvePath(modulePath, _MAINJAVAPATH);

		if (sourcePath != null) {
			if (
				Files.exists(sourcePath.resolve("test")) ||
				Files.exists(sourcePath.resolve("testIntegration"))) {

				return null;
			}
			return sourcePath;
		}
		else {
			sourcePath = modulePath.resolve(_DOCROOTPATH);

			return _resolvePath(sourcePath, _MAINJAVAPATH);
		}
	}

	private static Path _resolveTestPath(
		Path modulePath, Path pathType, String type) {

		Path testPath = modulePath.resolve(pathType);

		if (Files.exists(testPath)) {
			return testPath;
		}

		testPath = modulePath.resolve("test");

		testPath = testPath.resolve(type);

		if (Files.exists(testPath)) {
			return testPath;
		}

		return null;
	}

	private static final Path _DOCROOTPATH = Paths.get("docroot/WEB-INF");
	private static final Path _MAINJAVAPATH = Paths.get("src/main/java");
	private static final Path _TESTINTEGRATIONPATH = Paths.get(
		"src/testIntegration/java");
	private static final Path _TESTUNITPATH = Paths.get("src/test/java");

	private final List<ModuleDependency> _moduleDependencies;
	private final List<JarDependency> _moduleJarDependencies;
	private final Path _modulePath;
	private final Path _sourcePath;
	private final Path _sourceResourcePath;
	private final Path _testIntegrationPath;
	private final Path _testIntegrationResourcePath;
	private final Path _testUnitPath;
	private final Path _testUnitResourcePath;

}