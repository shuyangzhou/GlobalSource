/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liferay.netbeansproject;

import com.liferay.netbeansproject.util.GradleUtil;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.container.Module.ProjectDependency;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author tom
 */
public class ModuleProject {
	public static void main(String[] args) throws IOException {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path portalDirPath = Paths.get(properties.getProperty("portal.dir"));

		FileVisitor test = new PortalFileVisitor();

		Files.walkFileTree(portalDirPath, test);

		System.out.println(_dependenciesSB);
	}

	public static Module createModule(Path modulePath) throws Exception {
		Path sourcePath = _resolveSourcePath(modulePath);

		Path testUnitPath = _resolveTestPath(modulePath, "unit");

		Path testIntegrationPath = _resolveTestPath(modulePath, "integration");

		Path sourceResourcePath = _resolveResourcePath(modulePath, "main");

		Path testUnitResourcePath = _resolveResourcePath(modulePath, "test");

		Path testIntegrationResourcePath =
			_resolveResourcePath(modulePath, "integration");

		List<ProjectDependency> projectDependencyList =
			GradleUtil.getProjectDependencies(modulePath);

		String dependencies = GradleUtil.getJarDependencies(modulePath);

		_dependenciesSB.append(dependencies);

		List<String> jarDependencyList = _formatDependency(dependencies);

		return new Module(
			modulePath, sourcePath, sourceResourcePath, testUnitPath,
			testUnitResourcePath, testIntegrationPath,
			testIntegrationResourcePath, jarDependencyList,
			projectDependencyList);
	}

	public static void linkModuletoMap(Module module, Path path) {
		List<Module> moduleList = _projectMap.get(path);

		if(moduleList == null) {
			moduleList = new ArrayList<>();
		}

		moduleList.add(module);

		_projectMap.put(path, moduleList);
	}

	private static List<String> _formatDependency(String dependencies) {
		List<String> dependencyList = new ArrayList<>();

		for (String dependency : StringUtil.split(dependencies, '\n')) {
			if(!dependency.isEmpty()) {
				dependencyList.add(dependency.trim());
			}
		}
		return dependencyList;
	}

	private static Path _resolveResourcePath(Path modulePath, String type) {
		if (Files.exists(modulePath.resolve(
			"src" + File.separator + type + File.separator + "resources"))) {

			return modulePath.resolve(
				"src" + File.separator + type + File.separator + "resources");
		}

		return null;
	}
	private static Path _resolveSourcePath(Path modulePath) {
		if (Files.exists(modulePath.resolve(
			"src" + File.separator + "main" + File.separator + "java"))) {

			return modulePath.resolve(
				"src" + File.separator + "main" + File.separator + "java");
		}
		else if (Files.exists(modulePath.resolve(
			"src" + File.separator + "main"))) {

			return null;
		}

		return modulePath.resolve("src");
	}

	private static Path _resolveTestPath(Path modulePath, String type) {
		if (Files.exists(modulePath.resolve(
			"src" + File.separator + "test" + File.separator + "java")) &&
			type.equals("unit")) {

			return modulePath.resolve("src" + File.separator + "test");
		}
		else if (Files.exists(modulePath.resolve(
			"src" + File.separator + "testIntegration" + File.separator +
				"java")) && type.equals("integration")) {

			return modulePath.resolve("src" + File.separator + "testIntegration");
		}
		else if (Files.exists(modulePath.resolve(
			"test" + File.separator + type))) {

			return modulePath.resolve("test" + File.separator + type);
		}

		return null;
	}

	private static final Map<Path, List<Module>> _projectMap = new HashMap<>();

	private static final StringBuilder _dependenciesSB = new StringBuilder();
}
