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

import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.container.ModuleDependency;
import com.liferay.netbeansproject.resolvers.ProjectDependencyResolver;
import com.liferay.netbeansproject.util.FileUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
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
 * @author Tom Wang
 */
public class CreateModule {

	public static void createModule(
			Module module, Path portalPath, String excludedTypes,
			ProjectDependencyResolver projectDependencyResolver,
			Path projectPath)
		throws Exception {

		Path projectModulePath = projectPath.resolve(
			Paths.get("modules", module.getModuleName()));

		FileUtil.unZip(projectModulePath);

		_replaceProjectName(module, projectModulePath);

		_appendProperties(
			module, excludedTypes,
			projectModulePath.resolve("nbproject/project.properties"),
			projectDependencyResolver, portalPath, projectPath);

		_createProjectXML(module, portalPath.getParent(), projectModulePath);
	}

	private static Set<Path> _addDependenciesToSet(String[] dependencies) {
		Set<Path> set = new LinkedHashSet<>();

		for (String dependency : dependencies) {
			set.add(Paths.get(dependency));
		}

		return set;
	}

	private static void _appendLibJars(
		Path portalPath, Set<Path> dependencies, StringBuilder classpathSB,
		StringBuilder projectSB) {

		Path sdkPath = portalPath.resolve("tools/sdk");

		for (Path jar : dependencies) {
			if (jar.startsWith(sdkPath)) {
				continue;
			}

			projectSB.append("file.reference.");
			projectSB.append(jar.getFileName());
			projectSB.append('=');
			projectSB.append(jar);
			projectSB.append('\n');

			classpathSB.append('\t');
			classpathSB.append("${file.reference.");
			classpathSB.append(jar.getFileName());
			classpathSB.append("}:\\\n");
		}
	}

	private static void _appendProperties(
			Module module, String excludeTypes, Path projectPropertiesPath,
			ProjectDependencyResolver projectDependencyResolver,
			Path portalPath, Path projectPath)
		throws Exception {

		String projectName = module.getModuleName();

		StringBuilder projectSB = new StringBuilder();

		projectSB.append("excludes=");
		projectSB.append(excludeTypes);
		projectSB.append('\n');

		projectSB.append("application.title=");
		projectSB.append(module.getModulePath());
		projectSB.append('\n');

		projectSB.append("dist.jar=${dist.dir}/");
		projectSB.append(projectName);
		projectSB.append(".jar\n");

		_appendSourcePaths(module, projectSB);

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

		StringBuilder testSB = new StringBuilder("javac.test.classpath=\\\n");

		testSB.append("\t${build.classes.dir}:\\\n");
		testSB.append("\t${javac.classpath}:\\\n");

		Path dependenciesDirPath = projectPath.resolve("dependencies");

		if (!Files.exists(dependenciesDirPath)) {
			Files.createDirectory(dependenciesDirPath);
		}

		Path dependenciesPath = dependenciesDirPath.resolve(projectName);

		if (!Files.exists(dependenciesPath)) {
			Files.write(
				dependenciesPath, Arrays.asList("compile:\ncompileTest:"));
		}

		Properties dependencyProperties = PropertiesUtil.loadProperties(
			dependenciesPath);

		String compileDependencies = dependencyProperties.getProperty(
			"compile");

		Set<Path> compileSet = new LinkedHashSet<>();

		compileSet.addAll(
			_addDependenciesToSet(
				StringUtil.split(compileDependencies, File.pathSeparatorChar)));

		String compileTestDependencies = dependencyProperties.getProperty(
			"compileTest");

		if (compileTestDependencies == null) {
			compileTestDependencies = "";
		}

		Set<Path> compileTestSet = new LinkedHashSet<>();

		compileTestSet.addAll(
			_addDependenciesToSet(
				StringUtil.split(
					compileTestDependencies, File.pathSeparatorChar)));

		_appendLibJars(portalPath, compileSet, javacSB, projectSB);
		_appendLibJars(portalPath, compileTestSet, testSB, projectSB);

		for (ModuleDependency moduleDependency :
				module.getModuleDependencies()) {

			Module dependencyModule = projectDependencyResolver.resolve(
				moduleDependency.getModuleLocation());

			if (moduleDependency.isTest()) {
				_appendReferenceProperties(
					dependencyModule.getModuleName(), projectSB, testSB);
			}
			else {
				_appendReferenceProperties(
					dependencyModule.getModuleName(), projectSB, javacSB);
			}
		}

		for (String moduleName : module.getPortalLevelModuleDependencies()) {
			if (!moduleName.isEmpty()) {
				_appendReferenceProperties(moduleName, projectSB, javacSB);
			}
		}

		Path libDevelopmentPath = portalPath.resolve("lib/development");

		_appendLibJars(
			portalPath, _getDependencySet(libDevelopmentPath), javacSB,
			projectSB);

		Path libGlobalPath = portalPath.resolve("lib/global");

		_appendLibJars(
			portalPath, _getDependencySet(libGlobalPath), javacSB, projectSB);

		Path libPortalPath = portalPath.resolve("lib/portal");

		_appendLibJars(
			portalPath, _getDependencySet(libPortalPath), javacSB, projectSB);

		if (projectName.equals("portal-impl")) {
			projectSB.append("\nfile.reference.portal-test-integration-src=");
			projectSB.append(portalPath);
			projectSB.append("/portal-test-integration/src\n");
			projectSB.append("src.test.dir=");
			projectSB.append("${file.reference.portal-test-integration-src}");
		}

		if (projectName.equals("portal-kernel")) {
			projectSB.append("\nfile.reference.portal-test-src=");
			projectSB.append(portalPath);
			projectSB.append("/portal-test/src\n");
			projectSB.append("src.test.dir=${file.reference.portal-test-src}");
		}

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

	private static void _appendReferenceProperties(
			String moduleName, StringBuilder projectSB, StringBuilder javacSB)
		throws IOException {

		projectSB.append("project.");
		projectSB.append(moduleName);
		projectSB.append("=..");
		projectSB.append(File.separatorChar);
		projectSB.append(moduleName);
		projectSB.append('\n');
		projectSB.append("reference.");
		projectSB.append(moduleName);
		projectSB.append(".jar=${project.");
		projectSB.append(moduleName);
		projectSB.append('}');
		projectSB.append(File.separatorChar);
		projectSB.append("dist");
		projectSB.append(File.separatorChar);
		projectSB.append(moduleName);
		projectSB.append(".jar\n");

		javacSB.append("\t${reference.");
		javacSB.append(moduleName);
		javacSB.append(".jar}:\\\n");
	}

	private static void _appendSourcePathIndividual(
		Path path, String prefix, String name, String subfix,
		StringBuilder sb) {

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
		Module module, StringBuilder projectSB) {

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

	private static void _createData(
		Document document, Element configurationElement, Module module,
		Path portalPath) {

		Element dataElement = document.createElement("data");

		configurationElement.appendChild(dataElement);

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		Element nameElement = document.createElement("name");

		dataElement.appendChild(nameElement);

		Path moduleRelativePath = portalPath.relativize(module.getModulePath());

		nameElement.appendChild(
			document.createTextNode(moduleRelativePath.toString()));

		Element sourceRootsElement = document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		String moduleName = module.getModuleName();

		if (module.getSourcePath() != null) {
			_createRoots(
				document, sourceRootsElement, "src",
				"src." + moduleName + ".src.dir");
		}

		if (module.getSourceResourcePath() != null) {
			_createRoots(
				document, sourceRootsElement, "resources",
				"src." + moduleName + ".resources.dir");
		}

		Element testRootsElement = document.createElement("test-roots");

		dataElement.appendChild(testRootsElement);

		if (module.getTestUnitPath() != null) {
			_createRoots(
				document, testRootsElement, "test-unit",
				"test." + moduleName + ".test-unit.dir");
		}

		if (module.getTestUnitResourcePath() != null) {
			_createRoots(
				document, testRootsElement, "test-unit-resources",
				"test." + moduleName + ".test-unit-resources.dir");
		}

		if (module.getTestIntegrationPath() != null) {
			_createRoots(
				document, testRootsElement, "test-integration",
				"test." + moduleName + ".test-integration.dir");
		}

		if (module.getTestIntegrationResourcePath() != null) {
			_createRoots(
				document, testRootsElement, "test-integration-resources",
				"test." + moduleName + ".test-integration-resources.dir");
		}

		if (moduleName.equals("portal-impl")) {
			_createRoots(
				document, sourceRootsElement, "portal-test-integration",
				"src.test.dir");
		}

		if (moduleName.equals("portal-kernel")) {
			_createRoots(
				document, sourceRootsElement, "portal-test", "src.test.dir");
		}
	}

	private static void _createProjectElement(
			Document document, Module module, Path portalPath)
		throws IOException {

		Element projectElement = document.createElement("project");

		document.appendChild(projectElement);

		projectElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/project/1");

		Element typeElement = document.createElement("type");

		projectElement.appendChild(typeElement);

		typeElement.appendChild(
			document.createTextNode("org.netbeans.modules.java.j2seproject"));

		Element configurationElement = document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(document, configurationElement, module, portalPath);

		_createReferences(document, configurationElement, module);
	}

	private static void _createProjectXML(
			Module module, Path portalPath, Path projectModulePath)
		throws Exception {

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.newDocument();

		_createProjectElement(document, module, portalPath);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");

		try (Writer writer = Files.newBufferedWriter(
				projectModulePath.resolve("nbproject/project.xml"))) {

			transformer.transform(
				new DOMSource(document), new StreamResult(writer));
		}
	}

	private static void _createReference(
			Document document, Element referencesElement, String module)
		throws IOException {

		Element referenceElement = document.createElement("reference");

		referencesElement.appendChild(referenceElement);

		Element foreignProjectElement = document.createElement(
			"foreign-project");

		referenceElement.appendChild(foreignProjectElement);

		foreignProjectElement.appendChild(document.createTextNode(module));

		Element artifactTypeElement = document.createElement("artifact-type");

		referenceElement.appendChild(artifactTypeElement);

		artifactTypeElement.appendChild(document.createTextNode("jar"));

		Element scriptElement = document.createElement("script");

		referenceElement.appendChild(scriptElement);

		scriptElement.appendChild(document.createTextNode("build.xml"));

		Element targetElement = document.createElement("target");

		referenceElement.appendChild(targetElement);

		targetElement.appendChild(document.createTextNode("jar"));

		Element cleanTargetElement = document.createElement("clean-target");

		referenceElement.appendChild(cleanTargetElement);

		cleanTargetElement.appendChild(document.createTextNode("clean"));

		Element idElement = document.createElement("id");

		referenceElement.appendChild(idElement);

		idElement.appendChild(document.createTextNode("jar"));
	}

	private static void _createReferences(
			Document document, Element configurationElement, Module module)
		throws IOException {

		Element referencesElement = document.createElement("references");

		configurationElement.appendChild(referencesElement);

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		for (ModuleDependency moduleDependency :
				module.getModuleDependencies()) {

			String moduleLocation = moduleDependency.getModuleLocation();

			String[] moduleLocationSplit = StringUtil.split(
				moduleLocation, ':');

			_createReference(
				document, referencesElement,
				moduleLocationSplit[moduleLocationSplit.length - 1]);
		}

		for (String dependency : module.getPortalLevelModuleDependencies()) {
			_createReference(document, referencesElement, dependency);
		}
	}

	private static void _createRoots(
		Document document, Element sourceRootsElement, String label,
		String rootId) {

		Element rootElement = document.createElement("root");

		sourceRootsElement.appendChild(rootElement);

		rootElement.setAttribute("id", rootId);

		rootElement.setAttribute("name", label);
	}

	private static Set<Path> _getDependencySet(Path directory)
		throws IOException {

		DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
			directory);

		List<Path> jarList = new ArrayList<>();

		for (Path jarPath : directoryStream) {
			jarList.add(jarPath);
		}

		Collections.sort(jarList);

		directoryStream.close();

		return new HashSet<>(jarList);
	}

	private static void _replaceProjectName(
			Module module, Path projectModulePath)
		throws IOException {

		Path buildXMLPath = projectModulePath.resolve("build.xml");

		String content = StringUtil.replace(
			new String(Files.readAllBytes(buildXMLPath)), "%placeholder%",
			module.getModuleName());

		Files.write(buildXMLPath, Arrays.asList(content));
	}

}