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

import com.liferay.netbeansproject.resolvers.GradleDependencyResolver;
import com.liferay.netbeansproject.resolvers.ProjectDependencyResolver;

import java.nio.file.Path;

import java.util.List;

/**
 *
 * @author Tom Wang
 */
public class Module {

	public Module(
		Path modulePath, Path sourcePath, Path sourceResourcePath,
		Path testUnitPath, Path testUnitResourcePath, Path testIntegrationPath,
		Path testIntegrationResourcePath, String jarDependencies,
		List<ModuleDependency> moduleDependencies, String moduleName) {

		_modulePath = modulePath;
		_sourcePath = sourcePath;
		_sourceResourcePath = sourceResourcePath;
		_testUnitPath = testUnitPath;
		_testUnitResourcePath = testUnitResourcePath;
		_testIntegrationPath = testIntegrationPath;
		_testIntegrationResourcePath = testIntegrationResourcePath;
		_moduleDependencies = moduleDependencies;
		_moduleName = moduleName;
	}

	public List<JarDependency> getJarDependencies(
		GradleDependencyResolver gradleDependencyResolver) {

		return gradleDependencyResolver.resolve(this);
	}

	public List<ModuleDependency> getModuleDependencies() {
		return _moduleDependencies;
	}

	public String getModuleName() {
		return _moduleName;
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

	public static class JarDependency {

		public JarDependency(Path jarPath, boolean test) {
			_jarPath = jarPath;
			_test = test;
		}

		public Path getJarPath() {
			return _jarPath;
		}

		public boolean isTest() {
			return _test;
		}

		private final Path _jarPath;
		private final boolean _test;

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

	private final List<ModuleDependency> _moduleDependencies;
	private final String _moduleName;
	private final Path _modulePath;
	private final Path _sourcePath;
	private final Path _sourceResourcePath;
	private final Path _testIntegrationPath;
	private final Path _testIntegrationResourcePath;
	private final Path _testUnitPath;
	private final Path _testUnitResourcePath;

}