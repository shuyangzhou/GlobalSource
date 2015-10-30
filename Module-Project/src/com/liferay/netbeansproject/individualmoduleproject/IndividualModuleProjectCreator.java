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

package com.liferay.netbeansproject.individualmoduleproject;

import static com.liferay.netbeansproject.individualmoduleproject.CreateUmbrellaProject.createUmbrellaProject;

import com.liferay.netbeansproject.resolvers.GradleDependencyResolver;
import com.liferay.netbeansproject.resolvers.GradleDependencyResolverImpl;
import com.liferay.netbeansproject.resolvers.ProjectDependencyResolver;
import com.liferay.netbeansproject.resolvers.ProjectDependencyResolverImpl;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.container.Module.JarDependency;
import com.liferay.netbeansproject.container.Module.ModuleDependency;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import com.liferay.netbeansproject.util.ZipUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Tom Wang
 */
public class IndividualModuleProjectCreator {

	public static void createIndividualModuleProject(
			Map<Module, List<JarDependency>> dependenciesMap,
			Map<Path, Map<String, Module>> projectMap, Properties properties)
		throws Exception {

		Path currentRelativePath = Paths.get("");

		Path netbeansProjectPath = currentRelativePath.toAbsolutePath();

		GradleDependencyResolver gradleDependencyResolver =
			new GradleDependencyResolverImpl(dependenciesMap);

		ProjectDependencyResolver projectDependencyResolver =
			new ProjectDependencyResolverImpl(
				projectMap, Paths.get(properties.getProperty("portal.dir")));

		for (Map<String, Module> moduleMap : projectMap.values()) {
			for (Module module : moduleMap.values()) {
				_createModuleProject(
					gradleDependencyResolver, projectMap, module,
					netbeansProjectPath, projectDependencyResolver, properties,
					_resolvePortalLibJars(
						properties.getProperty("portal.dir")));
			}
		}

		createUmbrellaProject(projectMap, netbeansProjectPath, properties);
	}

	private static void _appendDependencyJar(Path jarPath, StringBuilder sb) {
		sb.append("\t");
		sb.append(jarPath);
		sb.append(":\\\n");
	}

	private static void _appendProjectDependencies(
		String moduleName, StringBuilder sb, StringBuilder javacSB) {

		sb.append("project.");
		sb.append(moduleName);
		sb.append("=");

		Path path = Paths.get("..", moduleName);

		sb.append(path);
		sb.append("\n");
		sb.append("reference.");
		sb.append(moduleName);
		sb.append(".jar=${project.");
		sb.append(moduleName);

		path = Paths.get("}", "dist", moduleName + ".jar");

		sb.append(path);
		sb.append("\n");

		javacSB.append("\t${reference.");
		javacSB.append(moduleName);
		javacSB.append(".jar}:\\\n");
	}

	private static void _appendSourcePath(
		Module module, StringBuilder projectSB) {

		String moduleName = module.getModuleName();

		if (module.getSourcePath() != null) {
			projectSB.append(
				_appendSourcePathIndividual(
					module.getSourcePath(), "src", moduleName, "src"));
		}

		if (module.getSourceResourcePath() != null) {
			projectSB.append(
				_appendSourcePathIndividual(
					module.getSourceResourcePath(), "src", moduleName,
					"resources"));
		}

		if (module.getTestUnitPath() != null) {
			projectSB.append(
				_appendSourcePathIndividual(
					module.getTestUnitPath(), "test", moduleName, "test-unit"));
		}

		if (module.getTestUnitResourcePath() != null) {
			projectSB.append(
				_appendSourcePathIndividual(
					module.getTestUnitResourcePath(), "test", moduleName,
					"test-unit-resources"));
		}

		if (module.getTestIntegrationPath() != null) {
			projectSB.append(
				_appendSourcePathIndividual(
					module.getTestIntegrationPath(), "test", moduleName,
					"test-integration"));
		}

		if (module.getTestIntegrationResourcePath() != null) {
			projectSB.append(
				_appendSourcePathIndividual(
					module.getTestIntegrationResourcePath(), "test", moduleName,
					"test-integration-resources"));
		}
	}

	private static String _appendSourcePathIndividual(
		Path path, String prefix, String name, String subfix) {

		StringBuilder sb = new StringBuilder();

		sb.append("file.reference.");
		sb.append(name);
		sb.append("-");
		sb.append(subfix);
		sb.append("=");
		sb.append(path);
		sb.append("\n");
		sb.append(prefix);
		sb.append(".");
		sb.append(name);
		sb.append(".");
		sb.append(subfix);
		sb.append(".dir=${file.reference.");
		sb.append(name);
		sb.append("-");
		sb.append(subfix);
		sb.append("}\n");

		return sb.toString();
	}

	private static boolean _checkSetContains(
		JarDependency jarDependency, Set<JarDependency> solvedJars) {

		for (JarDependency jar : solvedJars) {
			Path jarPath = jar.getJarPath();

			boolean isTest = jar.isTest();

			if (jarPath.equals(jarDependency.getJarPath()) &&
				isTest == jarDependency.isTest()) {

				return true;
			}
		}

		return false;
	}

	private static void _createConfiguration(
			Element projectElement, Module module,
			Properties projectDependenciesProperties, Set<Module> solvedSet)
		throws IOException {

		Element configurationElement = _document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(configurationElement, module);

		_createReferences(
			configurationElement, module, projectDependenciesProperties,
			solvedSet);
	}

	private static void _createData(
		Element configurationElement, Module module) {

		Element dataElement = _document.createElement("data");

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		configurationElement.appendChild(dataElement);

		Element nameElement = _document.createElement("name");

		Path modulePath = module.getModulePath();

		nameElement.appendChild(
			_document.createTextNode(modulePath.toString()));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = _document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		String moduleName = module.getModuleName();

		if (module.getSourcePath() != null) {
			_createRoots(
				sourceRootsElement, moduleName + "/src",
				"src." + moduleName + ".src.dir");
		}

		if (module.getSourceResourcePath() != null) {
			_createRoots(
				sourceRootsElement, moduleName + "/resources",
				"src." + moduleName + ".resources.dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		if (module.getTestUnitPath() != null) {
			_createRoots(
				testRootsElement, moduleName + "/unit",
				"test." + moduleName + ".test-unit.dir");
		}

		if (module.getTestUnitResourcePath() != null) {
			_createRoots(
				testRootsElement, moduleName + "/unit-resources",
				"test." + moduleName + ".test-unit-resources.dir");
		}

		if (module.getTestIntegrationPath() != null) {
			_createRoots(
				testRootsElement, moduleName + "/integration",
				"test." + moduleName + ".test-integration.dir");
		}

		if (module.getTestIntegrationResourcePath() != null) {
			_createRoots(
				testRootsElement, moduleName + "/integration-resources",
				"test." + moduleName + ".test-integration-resources.dir");
		}

		if (moduleName.equals("portal-impl")) {
			_createRoots(
				sourceRootsElement, "portal-test-internal",
				"src.portal-test-internal.src.dir");
		}

		if (moduleName.equals("portal-service")) {
			_createRoots(
				sourceRootsElement, "portal-test", "src.portal-test.src.dir");
		}

		dataElement.appendChild(testRootsElement);
	}

	private static void _createModuleProject(
			GradleDependencyResolver gradleDependencyResolver,
			Map<Path, Map<String, Module>> projectMap, Module module,
			Path netbeansProjectPath,
			ProjectDependencyResolver projectDependencyResolver,
			Properties properties, String portalLibJars)
		throws Exception {

		Path projectDir = Paths.get(properties.getProperty("project.dir"));

		Path modulesDir = projectDir.resolve("modules");

		String moduleName = module.getModuleName();

		ZipUtil.unZip(
			netbeansProjectPath.resolve("CleanModule.zip"),
			modulesDir.resolve(moduleName));

		_replaceProjectName(module, modulesDir);

		Set<Module> solvedSet = new HashSet<>();

		Properties projectDependenciesProperties =
				PropertiesUtil.loadProperties(
					netbeansProjectPath.resolve(
						"project-dependency.properties"));

		_prepareProjectPropertyFile(
			gradleDependencyResolver, projectMap, module,
			projectDependencyResolver, properties,
			projectDependenciesProperties, modulesDir, solvedSet,
			portalLibJars);

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		_document = documentBuilder.newDocument();

		_createProjectElement(module, projectDependenciesProperties, solvedSet);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		Path filePath = Paths.get(
			properties.getProperty("project.dir"), "modules", moduleName,
			"nbproject", "project.xml");

		Files.createDirectories(filePath.getParent());

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(
			new DOMSource(_document), new StreamResult(filePath.toFile()));
	}

	private static void _createProjectElement(
			Module module, Properties projectDependenciesProperties,
			Set<Module> solvedSet)
		throws IOException {

		Element projectElement = _document.createElement("project");

		projectElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/project/1");

		_document.appendChild(projectElement);

		Element typeElement = _document.createElement("type");

		typeElement.appendChild(
			_document.createTextNode("org.netbeans.modules.java.j2seproject"));

		projectElement.appendChild(typeElement);

		_createConfiguration(
			projectElement, module, projectDependenciesProperties, solvedSet);
	}

	private static void _createReference(
			Element referencesElement, String module)
		throws IOException {

		Element referenceElement = _document.createElement("reference");

		referencesElement.appendChild(referenceElement);

		Element foreignProjectElement = _document.createElement(
			"foreign-project");

		foreignProjectElement.appendChild(_document.createTextNode(module));

		referenceElement.appendChild(foreignProjectElement);

		Element artifactTypeElement = _document.createElement("artifact-type");

		artifactTypeElement.appendChild(_document.createTextNode("jar"));

		referenceElement.appendChild(artifactTypeElement);

		Element scriptElement = _document.createElement("script");

		scriptElement.appendChild(_document.createTextNode("build.xml"));

		referenceElement.appendChild(scriptElement);

		Element targetElement = _document.createElement("target");

		targetElement.appendChild(_document.createTextNode("jar"));

		referenceElement.appendChild(targetElement);

		Element cleanTargetElement = _document.createElement("clean-target");

		cleanTargetElement.appendChild(_document.createTextNode("clean"));

		referenceElement.appendChild(cleanTargetElement);

		Element idElement = _document.createElement("id");

		idElement.appendChild(_document.createTextNode("jar"));

		referenceElement.appendChild(idElement);
	}

	private static void _createReferences(
			Element configurationElement, Module module,
			Properties projectDependenciesProperties, Set<Module> solvedSet)
		throws IOException {

		Element referencesElement = _document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		for (Module setModule : solvedSet) {
			_createReference(referencesElement, setModule.getModuleName());
		}

		String dependencies = projectDependenciesProperties.getProperty(
			module.getModuleName());

		if (dependencies == null) {
			dependencies = projectDependenciesProperties.getProperty(
				"project.module.dependencies");
		}

		Queue<String> dependencyQueue = new LinkedList<>();

		dependencyQueue.addAll(
			Arrays.asList(StringUtil.split(dependencies, ',')));

		String dependency = null;

		while ((dependency = dependencyQueue.poll()) != null) {
			if (dependency.startsWith("${")) {
				dependencyQueue.addAll(
					Arrays.asList(
						StringUtil.split(
							projectDependenciesProperties.getProperty(
								dependency.substring(
									2, dependency.length() - 1)),
							',')));
			}
			else {
				_createReference(referencesElement, dependency);
			}
		}
	}

	private static void _createRoots(
		Element sourceRootsElement, String label, String rootId) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", rootId);

		rootElement.setAttribute("name", label);

		sourceRootsElement.appendChild(rootElement);
	}

	private static void _prepareProjectPropertyFile(
			GradleDependencyResolver gradleDependencyResolver,
			Map<Path, Map<String, Module>> projectMap, Module module,
			ProjectDependencyResolver projectDependencyResolver,
			Properties properties, Properties projectDependenciesProperties,
			Path moduleDir, Set<Module> solvedSet, String portalLibJars)
		throws Exception {

		String moduleName = module.getModuleName();

		Path projectPropertiesPath = Paths.get(
			moduleDir.toString(), moduleName, "nbproject",
			"project.properties");

		try (
			PrintWriter printWriter = new PrintWriter(
				new BufferedWriter(
					new FileWriter(projectPropertiesPath.toFile(), true)))) {

			StringBuilder projectSB = new StringBuilder();

			projectSB.append("excludes=");
			projectSB.append(properties.getProperty("exclude.types"));
			projectSB.append("\n");

			projectSB.append("application.title=");
			projectSB.append(module.getModulePath());
			projectSB.append("\n");

			projectSB.append("dist.jar=${dist.dir}/");
			projectSB.append(moduleName);
			projectSB.append(".jar\n");

			_appendSourcePath(module, projectSB);

			StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");
			StringBuilder testSB = new StringBuilder(
				"javac.test.classpath=\\\n");

			testSB.append("\t${build.classes.dir}:\\\n");
			testSB.append("\t${javac.classpath}:\\\n");

			Set<JarDependency> solvedJars = new HashSet<>();

			_resolveDependencyJarSet(
				gradleDependencyResolver, module, solvedJars, javacSB, testSB);

			Queue<ModuleDependency> projectDependencyQueue = new LinkedList<>();

			projectDependencyQueue.addAll(module.getModuleDependencies());

			ModuleDependency moduleDependency = null;

			while ((moduleDependency = projectDependencyQueue.poll()) != null) {
				Module dependencyModule = moduleDependency.getModule(
					projectDependencyResolver);

				if (!solvedSet.contains(dependencyModule)) {
					projectDependencyQueue.addAll(
						dependencyModule.getModuleDependencies());

					_resolveDependencyJarSet(
						gradleDependencyResolver, dependencyModule, solvedJars,
						javacSB, testSB);

					if (moduleDependency.isTest()) {
						_appendProjectDependencies(
							dependencyModule.getModuleName(), projectSB,
							testSB);
					}
					else {
						_appendProjectDependencies(
							dependencyModule.getModuleName(), projectSB,
							javacSB);
					}
				}

				solvedSet.add(
					moduleDependency.getModule(projectDependencyResolver));
			}

			_resolvePortalProjectDependencies(
				module, projectDependenciesProperties, projectSB, javacSB);

			javacSB.append(portalLibJars);

			Map<String, Module> portalLevelMap = projectMap.get(
				Paths.get(properties.getProperty("portal.dir")));

			if (moduleName.equals("portal-impl")) {
				Module portalTestInternalModule = portalLevelMap.get(
					"portal-test-internal");

				projectSB.append(
					_appendSourcePathIndividual(
						portalTestInternalModule.getSourcePath(), "src",
						portalTestInternalModule.getModuleName(), "src"));
			}

			if (moduleName.equals("portal-service")) {
				Module portalTestInternalModule = portalLevelMap.get(
					"portal-test");

				projectSB.append(
					_appendSourcePathIndividual(
						portalTestInternalModule.getSourcePath(), "src",
						portalTestInternalModule.getModuleName(), "src"));
			}

			printWriter.println(projectSB.toString());

			javacSB.setLength(javacSB.length() - 3);

			printWriter.println(javacSB.toString());

			testSB.setLength(testSB.length() - 3);

			printWriter.println(testSB.toString());
		}
	}

	private static void _replaceProjectName(Module module, Path modulesDir)
		throws IOException {

		String moduleName = module.getModuleName();

		Path modulePath = modulesDir.resolve(moduleName);

		Path buildXmlPath = modulePath.resolve("build.xml");

		String content = new String(Files.readAllBytes(buildXmlPath));

		content = StringUtil.replace(content, "%placeholder%", moduleName);

		Files.write(buildXmlPath, content.getBytes());
	}

	private static void _resolveDependencyJarSet(
		GradleDependencyResolver gradleDependencyResolver, Module module,
		Set<JarDependency> solvedJars, StringBuilder projectSB,
		StringBuilder testSB) {

		for (
			JarDependency jarDependency :
			module.getJarDependencies(gradleDependencyResolver)) {

			if (!_checkSetContains(jarDependency, solvedJars)) {

				if (jarDependency.isTest()) {
					_appendDependencyJar(jarDependency.getJarPath(), testSB);
				}
				else {
					_appendDependencyJar(jarDependency.getJarPath(), projectSB);
				}

				solvedJars.add(jarDependency);
			}
		}
	}

	private static String _resolvePortalLibJars(String portalDir)
		throws IOException {

		Path portalLibPath = Paths.get(portalDir, "lib");

		final StringBuilder sb = new StringBuilder();

		Files.walkFileTree(portalLibPath, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
						Path dir, BasicFileAttributes attrs)
					throws IOException {

					String fileName = dir.toString();

					if (fileName.endsWith(".jar")) {
						sb.append("\t");
						sb.append(dir);
						sb.append(":\\\n");
					}

					return FileVisitResult.CONTINUE;
				}
			});

		return sb.toString();
	}

	private static void _resolvePortalProjectDependencies(
		Module module, Properties properties, StringBuilder sb,
		StringBuilder javacSB) {

		String dependencies = properties.getProperty(module.getModuleName());

		if (dependencies == null) {
			dependencies = properties.getProperty(
				"project.module.dependencies");
		}

		Queue<String> dependencyQueue = new LinkedList<>();

		dependencyQueue.addAll(
			Arrays.asList(StringUtil.split(dependencies, ',')));

		String dependency = null;

		while ((dependency = dependencyQueue.poll()) != null) {
			if (dependency.startsWith("${")) {
				dependencyQueue.addAll(
					Arrays.asList(
						StringUtil.split(
							properties.getProperty(
								dependency.substring(
									2, dependency.length() - 1)),
							',')));
			}
			else {
				_appendProjectDependencies(dependency, sb, javacSB);
			}
		}
	}

	private static Document _document;

}