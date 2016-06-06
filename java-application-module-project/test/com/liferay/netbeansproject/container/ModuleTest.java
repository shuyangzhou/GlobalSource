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

import com.liferay.netbeansproject.util.ProjectUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Tom Wang
 */
public class ModuleTest {

	@BeforeClass
	public static void setUpTest() throws Exception {
		_testRootPath = Paths.get("Unit-Test-Model");
	}

	@Test
	public void testClassicModel() throws Exception {
		Path classicModelSrcPath = _testRootPath.resolve("src");

		Files.createDirectories(classicModelSrcPath);

		Path classicUnitPath = _testRootPath.resolve(Paths.get("test", "unit"));

		Files.createDirectories(classicUnitPath);

		Path classicIntegrationPath = _testRootPath.resolve(
			Paths.get("test", "integration"));

		Files.createDirectories(classicIntegrationPath);

		Module module = new Module(
			new ArrayList<JarDependency>(), _testRootPath);

		Assert.assertEquals(classicModelSrcPath, module.getSourcePath());

		Assert.assertEquals(classicUnitPath, module.getTestUnitPath());

		Assert.assertEquals(
			classicIntegrationPath, module.getTestIntegrationPath());
	}

	@Test
	public void testMavenModel() throws Exception {
		Path mavenModelSrcPath = _testRootPath.resolve(
			Paths.get("src", "main", "java"));

		Files.createDirectories(mavenModelSrcPath);

		Path mavenModelUnitPath = _testRootPath.resolve(
			Paths.get("src", "test", "java"));

		Files.createDirectories(mavenModelUnitPath);

		Path mavenModelIntegrationPath = _testRootPath.resolve(
			Paths.get("src", "testIntegration", "java"));

		Files.createDirectories(mavenModelIntegrationPath);

		Module module = new Module(
			new ArrayList<JarDependency>(), _testRootPath);

		Assert.assertEquals(mavenModelSrcPath, module.getSourcePath());

		Assert.assertEquals(mavenModelUnitPath, module.getTestUnitPath());

		Assert.assertEquals(
			mavenModelIntegrationPath, module.getTestIntegrationPath());
	}

	@Test
	public void testNoSourceModel() throws Exception {
		Path srcTestPath = _testRootPath.resolve(
			Paths.get("src", "test", "java"));

		Files.createDirectories(srcTestPath);

		Module module = new Module(
			new ArrayList<JarDependency>(), _testRootPath);

		Assert.assertNull(module.getSourcePath());

		Assert.assertEquals(srcTestPath, module.getTestUnitPath());
	}

	@Test
	public void testPortletModel() throws Exception {
		Path portletModelSrcPath = _testRootPath.resolve(
			Paths.get("docroot", "WEB-INF", "src"));

		Files.createDirectories(portletModelSrcPath);

		Module module = new Module(
			new ArrayList<JarDependency>(), _testRootPath);

		Assert.assertEquals(portletModelSrcPath, module.getSourcePath());
	}

	@Test
	public void testResource() throws Exception {
		Path srcResourcesPath = _testRootPath.resolve(
			Paths.get("src", "main", "resources"));

		Files.createDirectories(srcResourcesPath);

		Path testUnitResourcesPath = _testRootPath.resolve(
			Paths.get("src", "test", "resources"));

		Files.createDirectories(testUnitResourcesPath);

		Path testIntegrationResourcesPath = _testRootPath.resolve(
			Paths.get("src", "testIntegration", "resources"));

		Files.createDirectories(testIntegrationResourcesPath);

		Module module = new Module(
			new ArrayList<JarDependency>(), _testRootPath);

		Assert.assertEquals(
			srcResourcesPath, module.getSourceResourcePath());

		Assert.assertEquals(
			testUnitResourcesPath, module.getTestUnitResourcePath());

		Assert.assertEquals(
			testIntegrationResourcesPath,
			module.getTestIntegrationResourcePath());
	}

	@After
	public void cleanup() throws IOException {
		ProjectUtil.clean(_testRootPath);
	}

	private static Path _testRootPath;
}
