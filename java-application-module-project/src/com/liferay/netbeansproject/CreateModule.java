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

import com.liferay.netbeansproject.ModuleBuildParser.ModuleInfo;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import com.liferay.netbeansproject.util.ZipUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
 * @author Tom Wang
 */
public class CreateModule {

	public static void createModule(
			Path projectPath, Path modulePath, Path portalPath)
		throws Exception {

		List<String> moduleList = new ArrayList<>(Arrays.asList(
				StringUtil.split(
					new String(
						Files.readAllBytes(projectPath.resolve("moduleList"))),
					',')));

		Path moduleProjectPath = projectPath.resolve("modules");

		ZipUtil.unZip(moduleProjectPath.resolve(modulePath.getFileName()));

		Properties projectDependencyProperties = PropertiesUtil.loadProperties(
			Paths.get("project-dependency.properties"));

		Path moduleNamePath = modulePath.getFileName();

		String moduleName = moduleNamePath.toString();

		String projectDependencies = projectDependencyProperties.getProperty(
			moduleName);

		if (projectDependencies == null) {
			projectDependencies = projectDependencyProperties.getProperty(
				"portal.module.dependencies");
		}

		ProjectInfo projectInfo = new ProjectInfo(
			moduleName, portalPath, modulePath,
			StringUtil.split(projectDependencies, ','), moduleList);

		Path moduleDir = projectPath.resolve("modules");

		_replaceProjectName(projectInfo, moduleDir);

		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		_appendProperties(
			projectInfo, properties.getProperty("exclude.types"), moduleDir,
			projectPath);

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		_document = documentBuilder.newDocument();

		_createProjectElement(projectInfo);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		Path fileNamePath = Paths.get(
			moduleDir.toString(), projectInfo.getProjectName(), "nbproject",
			"project.xml");

		StreamResult streamResult = new StreamResult(fileNamePath.toFile());

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(_document), streamResult);
	}

	public static void createModules(
		Map<Path, Map<String, Module>> projectMap, Path portalPath,
		Path projectPath) throws Exception {

		for (Map<String, Module> map : projectMap.values()) {
			for (Module module : map.values()) {
				CreateModule.createModule(
					projectPath, module.getModulePath(), portalPath);
			}
		}
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
			ProjectInfo projectInfo, String excludeTypes, Path modulePath,
			Path projectPath)
		throws Exception {

		String projectName = projectInfo.getProjectName();

		Path projectPropertiesPath = Paths.get(
			modulePath.toString(), projectName, "nbproject",
			"project.properties");

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				projectPropertiesPath, Charset.defaultCharset(),
				StandardOpenOption.APPEND)) {

			StringBuilder projectSB = new StringBuilder();

			projectSB.append("excludes=");
			projectSB.append(excludeTypes);
			projectSB.append('\n');

			projectSB.append("application.title=");
			projectSB.append(projectInfo.getFullPath());
			projectSB.append('\n');

			projectSB.append("dist.jar=${dist.dir}/");
			projectSB.append(projectName);
			projectSB.append(".jar\n");

			_appendSourcePath(
				projectName, projectInfo.getFullPath(), projectSB);

			StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

			for (String module : projectInfo.getProjectLibs()) {
				if (!module.isEmpty()) {
					_appendReferenceProperties(bufferedWriter, module, javacSB);
				}
			}

			Path dependenciesDirPath = projectPath.resolve("dependencies");

			if (!Files.exists(dependenciesDirPath)) {
				Files.createDirectory(dependenciesDirPath);
			}

			Path dependenciesPath = dependenciesDirPath.resolve(projectName);

			if (!Files.exists(dependenciesPath)) {
				Files.write(
					dependenciesPath, Arrays.asList("compile:\ncompileTest:"),
					Charset.defaultCharset());
			}

			Properties dependencyProperties = PropertiesUtil.loadProperties(
				dependenciesPath);

			StringBuilder testSB = new StringBuilder(
				"javac.test.classpath=\\\n");

			testSB.append("\t${build.classes.dir}:\\\n");
			testSB.append("\t${javac.classpath}:\\\n");

			String compileDependencies = dependencyProperties.getProperty(
				"compile");

			Set<Path> compileSet = new LinkedHashSet<>();

			compileSet.addAll(
				_addDependenciesToSet(
					StringUtil.split(
						compileDependencies, File.pathSeparatorChar)));

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

			Map<String, ModuleInfo> dependenciesModuleMap =
				_parseModuleDependencies(
					projectInfo, projectInfo.getFullPath());

			for (ModuleInfo moduleInfo : dependenciesModuleMap.values()) {
				String moduleName = moduleInfo.getModuleName();

				if (moduleInfo.isTest()) {
					_appendReferenceProperties(
						bufferedWriter, moduleName, testSB);
				}
				else {
					_appendReferenceProperties(
						bufferedWriter, moduleName, javacSB);
				}
			}

			projectInfo.setDependenciesModuleMap(dependenciesModuleMap);

			Path portalPath = projectInfo.getPortalPath();

			_appendLibJars(portalPath, compileSet, javacSB, projectSB);
			_appendLibJars(portalPath, compileTestSet, testSB, projectSB);

			Path libDevelopmentPath = portalPath.resolve("lib/development");

			_appendLibJars(
				portalPath, _getDependencySet(libDevelopmentPath), javacSB,
				projectSB);

			Path libGlobalPath = portalPath.resolve("lib/global");

			_appendLibJars(
				portalPath, _getDependencySet(libGlobalPath), javacSB,
				projectSB);

			Path libPortalPath = portalPath.resolve("lib/portal");

			_appendLibJars(
				portalPath, _getDependencySet(libPortalPath), javacSB,
				projectSB);

			if (projectName.equals("portal-impl")) {
				projectSB.append(
					"\nfile.reference.portal-test-integration-src=");
				projectSB.append(projectInfo.getPortalPath());
				projectSB.append("/portal-test-integration/src\n");
				projectSB.append("src.test.dir=");
				projectSB.append(
					"${file.reference.portal-test-integration-src}");
			}

			if (projectName.equals("portal-kernel")) {
				projectSB.append("\nfile.reference.portal-test-src=");
				projectSB.append(projectInfo.getPortalPath());
				projectSB.append("/portal-test/src\n");
				projectSB.append(
					"src.test.dir=${file.reference.portal-test-src}");
			}

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
			BufferedWriter bufferedWriter, String module, StringBuilder javacSB)
		throws IOException {

		StringBuilder sb = new StringBuilder("project.");

		sb.append(module);
		sb.append("=..");
		sb.append(File.separatorChar);
		sb.append(module);
		sb.append('\n');
		sb.append("reference.");
		sb.append(module);
		sb.append(".jar=${project.");
		sb.append(module);
		sb.append('}');
		sb.append(File.separatorChar);
		sb.append("dist");
		sb.append(File.separatorChar);
		sb.append(module);
		sb.append(".jar");

		bufferedWriter.append(sb);
		bufferedWriter.newLine();

		javacSB.append("\t${reference.");
		javacSB.append(module);
		javacSB.append(".jar}:\\\n");
	}

	private static void _appendSourcePath(
		String moduleName, Path modulePath, StringBuilder projectSB) {

		Path moduleSrcPath = modulePath.resolve("src");

		if (Files.exists(modulePath.resolve("docroot"))) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-src=");
			projectSB.append(modulePath);
			projectSB.append(File.separatorChar);
			projectSB.append("docroot");
			projectSB.append(File.separatorChar);
			projectSB.append("WEB-INF");
			projectSB.append(File.separatorChar);
			projectSB.append("src\n");
		}
		else if (Files.exists(moduleSrcPath.resolve("com")) ||
				 Files.exists(moduleSrcPath.resolve("main"))) {

			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-src=");
			projectSB.append(modulePath);
			projectSB.append(File.separatorChar);
			projectSB.append("src");

			if (Files.exists(moduleSrcPath.resolve("main"))) {
				projectSB.append(File.separatorChar);
				projectSB.append("main");
				projectSB.append(File.separatorChar);
				projectSB.append("java\n");
			}
			else {
				projectSB.append('\n');
			}

			projectSB.append("src.");
			projectSB.append(moduleName);
			projectSB.append(".dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-src}\n");
		}

		Path mainResourcesPath = Paths.get(
			moduleSrcPath.toString(), "main", "resources");

		if (Files.exists(mainResourcesPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-resources=");
			projectSB.append(mainResourcesPath);
			projectSB.append('\n');
			projectSB.append("src.");
			projectSB.append(moduleName);
			projectSB.append(".resources.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-resources}\n");
		}

		Path testPath = modulePath.resolve("test");

		Path testUnitPath = testPath.resolve("unit");
		Path srcTestPath = moduleSrcPath.resolve("test");

		Path testJavaPath = srcTestPath.resolve("java");

		if (Files.exists(testUnitPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit=");
			projectSB.append(testUnitPath);
			projectSB.append('\n');
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".unit.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit}\n");
		}
		else if(Files.exists(testJavaPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit=");
			projectSB.append(testJavaPath);
			projectSB.append('\n');
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".unit.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit}\n");
		}

		Path testResourcesPath = Paths.get(
			moduleSrcPath.toString(), "test", "Resources");

		if (Files.exists(testResourcesPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit-resources=");
			projectSB.append(testResourcesPath);
			projectSB.append('\n');
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".unit.resources.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit-resources}\n");
		}

		Path testIntegrationPath = testPath.resolve("integration");
		Path srcTestIntegrationPath = moduleSrcPath.resolve("testIntegration");

		Path testIntegrationJavaPath = srcTestIntegrationPath.resolve("java");

		if (Files.exists(testIntegrationPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration=");
			projectSB.append(testIntegrationPath);
			projectSB.append('\n');
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".integration.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration}\n");
		}
		else if(Files.exists(testIntegrationJavaPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration=");
			projectSB.append(testIntegrationJavaPath);
			projectSB.append('\n');
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".integration.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration}\n");
		}

		Path testIntegrationResourcesPath = srcTestIntegrationPath.resolve(
			"resources");

		if (Files.exists(testIntegrationResourcesPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration-resources=");
			projectSB.append(testIntegrationResourcesPath);
			projectSB.append('\n');
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".integration.resources.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration-resources}\n");
		}
	}

	private static void _createConfiguration(
			Element projectElement, ProjectInfo projectInfo)
		throws IOException {

		Element configurationElement = _document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(configurationElement, projectInfo);

		_createReferences(configurationElement, projectInfo);
	}

	private static void _createData(
		Element configurationElement, ProjectInfo projectInfo) {

		Element dataElement = _document.createElement("data");

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		configurationElement.appendChild(dataElement);

		Element nameElement = _document.createElement("name");

		Path projectPath = projectInfo.getFullPath();

		Path portalPath = projectInfo.getPortalPath();

		Path portalParentPath = portalPath.getParent();

		Path projectNamePath = portalParentPath.relativize(projectPath);

		nameElement.appendChild(
			_document.createTextNode(projectNamePath.toString()));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = _document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		String projectName = projectInfo.getProjectName();

		Path srcPath = projectPath.resolve("src");

		Path mainPath = srcPath.resolve("main");

		Path mainJavaPath = mainPath.resolve("java");

		if (!Files.exists(mainPath) || Files.exists(mainJavaPath)) {
			_createRoots(
				sourceRootsElement, "src", "src." + projectName + ".dir");
		}

		Path mainResourcesPath = mainPath.resolve("resources");

		if (Files.exists(mainResourcesPath)) {
			_createRoots(
				sourceRootsElement, "resources",
				"src." + projectName + ".resources.dir");
		}

		if (projectName.equals("portal-impl")) {
			_createRoots(
				sourceRootsElement, "portal-test-integration", "src.test.dir");
		}
		else if (projectName.equals("portal-kernel")) {
			_createRoots(sourceRootsElement, "portal-test", "src.test.dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		Path testPath = projectPath.resolve("test");

		Path testUnitPath = testPath.resolve("unit");
		Path srcTestPath = srcPath.resolve("test");

		if (Files.exists(testUnitPath) || Files.exists(srcTestPath)) {
			_createRoots(
				testRootsElement, "unit" + File.separator + "test",
				"test." + projectName + ".unit.dir");
		}

		Path testResourcesPath = testPath.resolve("resources");

		if (Files.exists(testResourcesPath)) {
			_createRoots(
				sourceRootsElement, "unit" + File.separator + "resources",
				"test." + projectName + ".unit.resources.dir");
		}

		Path testIntegrationPath = testPath.resolve("integration");
		Path srcTestIntegrationPath = srcPath.resolve("testIntegration");

		if (Files.exists(testIntegrationPath) ||
			Files.exists(srcTestIntegrationPath)) {

			_createRoots(
				testRootsElement, "integration" + File.separator + "test",
				"test." + projectName + ".integration.dir");
		}

		Path testIntegrationResources = srcTestIntegrationPath.resolve(
			"resources");

		if (Files.exists(testIntegrationResources)) {
			_createRoots(
				sourceRootsElement,
				"integration" + File.separator + "resources",
				"test." + projectName + ".integration.resources.dir");
		}

		dataElement.appendChild(testRootsElement);
	}

	private static void _createProjectElement(ProjectInfo projectInfo)
		throws IOException {

		Element projectElement = _document.createElement("project");

		projectElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/project/1");

		_document.appendChild(projectElement);

		Element typeElement = _document.createElement("type");

		typeElement.appendChild(
			_document.createTextNode("org.netbeans.modules.java.j2seproject"));

		projectElement.appendChild(typeElement);

		_createConfiguration(projectElement, projectInfo);
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
			Element configurationElement, ProjectInfo projectInfo)
		throws IOException {

		Element referencesElement = _document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		Map<String, ModuleInfo> dependenciesModuleMap =
			projectInfo.getDependenciesModuleMap();

		for (String moduleName : dependenciesModuleMap.keySet()) {
			_createReference(referencesElement, moduleName);
		}

		for (String module : projectInfo.getProjectLibs()) {
			if (!module.equals("")) {
				_createReference(referencesElement, module);
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

	private static Map<String, ModuleInfo> _parseModuleDependencies(
			ProjectInfo projectInfo, Path modulePath)
		throws Exception {

		Map<String, ModuleInfo> dependenciesModuleMap = new HashMap<>();

		Map<String, Path> moduleMap = projectInfo.getModuleMap();

		Queue<ModuleInfo> moduleInfoQueue = new LinkedList<>();

		moduleInfoQueue.addAll(ModuleBuildParser.parseBuildFile(modulePath));

		ModuleInfo moduleInfo = null;

		while ((moduleInfo = moduleInfoQueue.poll()) != null) {
			String moduleName = moduleInfo.getModuleName();

			if (!moduleMap.containsKey(moduleName)) {
				continue;
			}

			if (dependenciesModuleMap.put(moduleName, moduleInfo) == null) {
				moduleInfoQueue.addAll(
					ModuleBuildParser.parseBuildFile(
						moduleMap.get(moduleName)));
			}
		}

		return dependenciesModuleMap;
	}

	private static void _replaceProjectName(
			ProjectInfo projectInfo, Path moduleDir)
		throws IOException {

		String projectName = projectInfo.getProjectName();

		Path projectpath = moduleDir.resolve(projectName);

		Path buildXMLPath = projectpath.resolve("build.xml");

		String content = new String(Files.readAllBytes(buildXMLPath));

		content = StringUtil.replace(content, "%placeholder%", projectName);

		Files.write(
			buildXMLPath, Arrays.asList(content), Charset.defaultCharset());
	}

	private static Document _document;

	private static class ProjectInfo {

		public Map<String, ModuleInfo> getDependenciesModuleMap() {
			return _dependenciesModuleMap;
		}

		public Path getFullPath() {
			return _fullPath;
		}

		public Map<String, Path> getModuleMap() {
			return _moduleMap;
		}

		public Path getPortalPath() {
			return _portalPath;
		}

		public String[] getProjectLibs() {
			return _projectLib;
		}

		public String getProjectName() {
			return _projectName;
		}

		public void setDependenciesModuleMap(
			Map<String, ModuleInfo> dependenciesModuleMap) {

			_dependenciesModuleMap = dependenciesModuleMap;
		}

		private ProjectInfo(
			String projectName, Path portalPath, Path fullPath,
			String[] projectLibs, List<String> moduleList) {

			_projectName = projectName;

			_portalPath = portalPath;

			_fullPath = fullPath;

			_projectLib = projectLibs;

			_moduleMap = new HashMap<>();

			for (String module : moduleList) {
				Path modulePath = Paths.get(module);

				Path namePath = modulePath.getFileName();

				_moduleMap.put(namePath.toString(), modulePath);
			}
		}

		private Map<String, ModuleInfo> _dependenciesModuleMap;
		private final Path _fullPath;
		private final Map<String, Path> _moduleMap;
		private final Path _portalPath;
		private final String[] _projectLib;
		private final String _projectName;

	}

}