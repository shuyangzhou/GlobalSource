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

import com.liferay.netbeansproject.util.GradleUtil;
import com.liferay.netbeansproject.util.HashUtil;
import com.liferay.netbeansproject.util.ModuleUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.IOException;
import java.io.Writer;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class Module {

	public static Module createModule(
			Path projectPath, Path modulePath,
			List<JarDependency> jarDependencies,
			Properties projectDependencyProperties)
		throws IOException {

		if (jarDependencies == null) {
			jarDependencies = new ArrayList<>();
		}

		Path moduleLibPath = modulePath.resolve("lib");

		if (Files.exists(moduleLibPath)) {
			try (DirectoryStream<Path> directoryStream =
					Files.newDirectoryStream(moduleLibPath, "*.jar")) {

				for (Path jarPath : directoryStream) {
					jarDependencies.add(new JarDependency(jarPath, false));
				}
			}
		}

		String checksum = null;

		Path gradleFilePath = modulePath.resolve("build.gradle");

		if (Files.exists(gradleFilePath)) {
			try {
				MessageDigest messageDigest = MessageDigest.getInstance("MD5");

				byte[] hash = messageDigest.digest(
					Files.readAllBytes(gradleFilePath));

				checksum = StringUtil.bytesToHexString(hash);
			}
			catch (NoSuchAlgorithmException nsae) {
				throw new Error(nsae);
			}
		}

		if (modulePath.endsWith("WEB-INF")) {
			modulePath = modulePath.getParent();
			modulePath = modulePath.getParent();
		}

		Module module = new Module(
			projectPath, modulePath, _resolveSourcePath(modulePath),
			_resolveResourcePath(modulePath, "main"),
			_resolveTestPath(modulePath, true),
			_resolveResourcePath(modulePath, "test"),
			_resolveTestPath(modulePath, false),
			_resolveResourcePath(modulePath, "testIntegration"),
			GradleUtil.getModuleDependencies(modulePath), jarDependencies,
			_resolvePortalLevelDependencies(
				projectDependencyProperties,
				ModuleUtil.getModuleName(modulePath)),
			checksum);

		if (projectPath != null) {
			module._save();
		}

		return module;
	}

	public static Module load(Path projectPath) throws IOException {
		Path moduleInfoPath = projectPath.resolve("module-info.properties");

		if (Files.notExists(moduleInfoPath)) {
			return null;
		}

		Properties properties = PropertiesUtil.loadProperties(moduleInfoPath);

		return new Module(
			projectPath, Paths.get(properties.getProperty("module.path")),
			_getPath(properties, "source.path"),
			_getPath(properties, "source.resource.path"),
			_getPath(properties, "test.unit.path"),
			_getPath(properties, "test.unit.resource.path"),
			_getPath(properties, "test.integration.path"),
			_getPath(properties, "test.integration.resource.path"), null, null,
			null, properties.getProperty("checksum"));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Module)) {
			return false;
		}

		Module module = (Module)obj;

		if (Objects.equals(_modulePath, module._modulePath) &&
			Objects.equals(_sourcePath, module._sourcePath) &&
			Objects.equals(_sourceResourcePath, module._sourceResourcePath) &&
			Objects.equals(_testUnitPath, module._testUnitPath) &&
			Objects.equals(
				_testUnitResourcePath, module._testUnitResourcePath) &&
			Objects.equals(_testIntegrationPath, module._testIntegrationPath) &&
			Objects.equals(
				_testIntegrationResourcePath,
				module._testIntegrationResourcePath) &&
			Objects.equals(_checksum, module._checksum)) {

			return true;
		}

		return false;
	}

	public String getChecksum() {
		return _checksum;
	}

	public List<JarDependency> getJarDependencies() {
		return _jarDependencies;
	}

	public List<ModuleDependency> getModuleDependencies() {
		return _moduleDependencies;
	}

	public String getModuleName() {
		return ModuleUtil.getModuleName(_modulePath);
	}

	public Path getModulePath() {
		return _modulePath;
	}

	public List<String> getPortalLevelModuleDependencies() {
		return _portalLevelModuleDependencies;
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

	@Override
	public int hashCode() {
		int hashCode = HashUtil.hash(0, _modulePath);

		hashCode = HashUtil.hash(hashCode, _sourcePath);
		hashCode = HashUtil.hash(hashCode, _sourceResourcePath);
		hashCode = HashUtil.hash(hashCode, _testUnitPath);
		hashCode = HashUtil.hash(hashCode, _testUnitResourcePath);
		hashCode = HashUtil.hash(hashCode, _testIntegrationPath);
		hashCode = HashUtil.hash(hashCode, _testIntegrationResourcePath);
		hashCode = HashUtil.hash(hashCode, _checksum);

		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("{projectPath=");
		sb.append(_projectPath);
		sb.append(", modulePath=");
		sb.append(_modulePath);
		sb.append(", sourcePath=");
		sb.append(_sourcePath);
		sb.append(", sourceResourcePath=");
		sb.append(_sourceResourcePath);
		sb.append(", testUnitPath=");
		sb.append(_testUnitPath);
		sb.append(", testUnitResourcePath=");
		sb.append(_testUnitResourcePath);
		sb.append(", testIntegrationPath=");
		sb.append(_testIntegrationPath);
		sb.append(", testIntegrationResourcePath=");
		sb.append(_testIntegrationResourcePath);
		sb.append(", moduleDependencies=");
		sb.append(_moduleDependencies);
		sb.append(", jarDependencies=");
		sb.append(_jarDependencies);
		sb.append(", checksum=");
		sb.append(_checksum);
		sb.append("}");

		return sb.toString();
	}

	private static Path _getPath(Properties properties, String key) {
		String value = properties.getProperty(key);

		if (value == null) {
			return null;
		}

		return Paths.get(value);
	}

	private static void _putProperty(
		Properties properties, String name, Object value) {

		if (value != null) {
			properties.put(name, String.valueOf(value));
		}
	}

	private static List<String> _resolvePortalLevelDependencies(
			Properties properties, String moduleName)
		throws IOException {

		String dependencies = properties.getProperty(moduleName);

		if (dependencies == null) {
			dependencies = PropertiesUtil.getRequiredProperty(
				properties, "portal.module.dependencies");
		}

		return Arrays.asList(StringUtil.split(dependencies, ','));
	}

	private static Path _resolveResourcePath(Path modulePath, String type) {
		Path resolvedResourcePath = modulePath.resolve(
			Paths.get("src", type, "resources"));

		if (Files.exists(resolvedResourcePath)) {
			return resolvedResourcePath;
		}

		return null;
	}

	private static Path _resolveSourcePath(Path modulePath) {
		Path sourcePath = modulePath.resolve(
			Paths.get("docroot", "WEB-INF", "src"));

		if (Files.exists(sourcePath)) {
			return sourcePath;
		}

		sourcePath = modulePath.resolve(Paths.get("src", "main", "java"));

		if (Files.exists(sourcePath)) {
			return sourcePath;
		}

		sourcePath = modulePath.resolve("src");

		if (Files.exists(sourcePath.resolve("main")) ||
			Files.exists(sourcePath.resolve("test")) ||
			Files.exists(sourcePath.resolve("testIntegration"))) {

			return null;
		}

		return sourcePath;
	}

	private static Path _resolveTestPath(Path modulePath, boolean unit) {
		Path testPath = null;

		if (unit) {
			testPath = modulePath.resolve(Paths.get("src", "test", "java"));
		}
		else {
			testPath = modulePath.resolve(
				Paths.get("src", "testIntegration", "java"));
		}

		if (Files.exists(testPath)) {
			return testPath;
		}

		if (unit) {
			testPath = modulePath.resolve(Paths.get("test", "unit"));
		}
		else {
			testPath = modulePath.resolve(Paths.get("test", "integration"));
		}

		if (Files.exists(testPath)) {
			return testPath;
		}

		return null;
	}

	private Module(
		Path projectPath, Path modulePath, Path sourcePath,
		Path sourceResourcePath, Path testUnitPath, Path testUnitResourcePath,
		Path testIntegrationPath, Path testIntegrationResourcePath,
		List<ModuleDependency> moduleDependencies,
		List<JarDependency> jarDependencies,
		List<String> portalLevelModuleDependencies, String checksum) {

		_projectPath = projectPath;
		_modulePath = modulePath;
		_sourcePath = sourcePath;
		_sourceResourcePath = sourceResourcePath;
		_testUnitPath = testUnitPath;
		_testUnitResourcePath = testUnitResourcePath;
		_testIntegrationPath = testIntegrationPath;
		_testIntegrationResourcePath = testIntegrationResourcePath;
		_moduleDependencies = moduleDependencies;
		_jarDependencies = jarDependencies;
		_portalLevelModuleDependencies = portalLevelModuleDependencies;
		_checksum = checksum;
	}

	private void _save() throws IOException {
		Properties properties = new Properties();

		_putProperty(properties, "module.path", _modulePath);
		_putProperty(properties, "source.path", _sourcePath);
		_putProperty(properties, "source.resource.path", _sourceResourcePath);
		_putProperty(properties, "test.unit.path", _testUnitPath);
		_putProperty(
			properties, "test.unit.resource.path", _testUnitResourcePath);
		_putProperty(properties, "test.integration.path", _testIntegrationPath);
		_putProperty(
			properties, "test.integration.resource.path",
			_testIntegrationResourcePath);

		Path gradleFilePath = _modulePath.resolve("build.gradle");

		try {
			if (Files.exists(gradleFilePath)) {
				MessageDigest messageDigest = MessageDigest.getInstance("MD5");

				byte[] hash = messageDigest.digest(
					Files.readAllBytes(gradleFilePath));

				properties.put("checksum", StringUtil.bytesToHexString(hash));
			}
		}
		catch (NoSuchAlgorithmException nsae) {
			throw new Error(nsae);
		}

		Files.createDirectories(_projectPath);

		try (Writer writer = Files.newBufferedWriter(
				_projectPath.resolve("module-info.properties"))) {

			properties.store(writer, null);
		}
	}

	private final String _checksum;
	private final List<JarDependency> _jarDependencies;
	private final List<ModuleDependency> _moduleDependencies;
	private final Path _modulePath;
	private final List<String> _portalLevelModuleDependencies;
	private final Path _projectPath;
	private final Path _sourcePath;
	private final Path _sourceResourcePath;
	private final Path _testIntegrationPath;
	private final Path _testIntegrationResourcePath;
	private final Path _testUnitPath;
	private final Path _testUnitResourcePath;

}