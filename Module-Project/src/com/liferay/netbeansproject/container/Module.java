/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liferay.netbeansproject.container;

import com.liferay.netbeansproject.GradleDependencyResolver;
import java.nio.file.Path;
import java.util.ArrayList;
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

	public List<Path> getJarDependenciesPaths(
		GradleDependencyResolver gradleDependencyResolver) {

		List<Path> jarDependenciesPaths = new ArrayList<>(
			_jarDependencies.size());

		for (String jarDependency : _jarDependencies) {
			jarDependenciesPaths.add(
				gradleDependencyResolver.resolve(jarDependency));
		}

		return jarDependenciesPaths;
	}

	public List<ModuleDependency> getModuleDependencies() {
		return _moduleDependencies;
	}

	public Module(Path modulePath, Path sourcePath, Path sourceResourcePath,
		Path testUnitPath, Path testUnitResourcePath, Path testIntegrationPath,
		Path testIntegrationResourcePath, List<String> jarDependencies,
		List<ModuleDependency> moduleDependencies) {

		_modulePath = modulePath;
		_sourcePath = sourcePath;
		_sourceResourcePath = sourceResourcePath;
		_testUnitPath = testUnitPath;
		_testUnitResourcePath = testUnitResourcePath;
		_testIntegrationPath = testIntegrationPath;
		_testIntegrationResourcePath = testIntegrationResourcePath;
		_jarDependencies = jarDependencies;
		_moduleDependencies = moduleDependencies;
	}

	private final Path _modulePath;
	private final Path _sourcePath;
	private final Path _sourceResourcePath;
	private final Path _testUnitPath;
	private final Path _testUnitResourcePath;
	private final Path _testIntegrationPath;
	private final Path _testIntegrationResourcePath;
	private final List<String> _jarDependencies;
	private final List<ModuleDependency> _moduleDependencies;

	public static class ModuleDependency {

		public ModuleDependency(Module module, boolean test) {
			_module = module;
			_test = test;
		}

		public Module getModule() {
			return _module;
		}

		public boolean isTest() {
			return _test;
		}

		private final String _modulePath;
		private final boolean _test;

	}
}
