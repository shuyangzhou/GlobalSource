/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liferay.netbeansproject.container;

import java.nio.file.Path;
import java.util.List;

/**
 *
 * @author tom
 */
public class Module {
	public Path getModulePath() {
		return _modulePath;
	}

	public Path getSourcePath() {
		return _sourcePath;
	}

	public Path getSourceResourcePath() {
		return _sourceResourcePath;
	}

	public Path getTestUnitPath() {
		return _testUnitPath;
	}

	public Path getTestUnitResourcePath() {
		return _testUnitResourcePath;
	}

	public Path getTestIntegrationPath() {
		return _testIntegrationPath;
	}

	public Path getTestIntegrationResourcePath() {
		return _testIntegrationResourcePath;
	}

	public List<String> getJarDependencies() {
		return _jarDependencies;
	}

	public List<ProjectDependency> getProjectDependencies() {
		return _projectDependencies;
	}

	public Module(Path modulePath, Path sourcePath, Path sourceResourcePath,
		Path testUnitPath, Path testUnitResourcePath, Path testIntegrationPath,
		Path testIntegrationResourcePath, List<String> jarDependencies,
		List<ProjectDependency> projectDependencies) {

		_modulePath = modulePath;
		_sourcePath = sourcePath;
		_sourceResourcePath = sourceResourcePath;
		_testUnitPath = testUnitPath;
		_testUnitResourcePath = testUnitResourcePath;
		_testIntegrationPath = testIntegrationPath;
		_testIntegrationResourcePath = testIntegrationResourcePath;
		_jarDependencies = jarDependencies;
		_projectDependencies = projectDependencies;
	}

	private final Path _modulePath;
	private final Path _sourcePath;
	private final Path _sourceResourcePath;
	private final Path _testUnitPath;
	private final Path _testUnitResourcePath;
	private final Path _testIntegrationPath;
	private final Path _testIntegrationResourcePath;
	private final List<String> _jarDependencies;
	private final List<ProjectDependency> _projectDependencies;

	public static class ProjectDependency {

		public String[] getDependency() {
			return _dependency;
		}

		public boolean isTest() {
			return _test;
		}

		public ProjectDependency(String[] dependency, boolean test) {
			_dependency = dependency;
			_test = test;
		}

		private final String[] _dependency;
		private final boolean _test;

	}
}
