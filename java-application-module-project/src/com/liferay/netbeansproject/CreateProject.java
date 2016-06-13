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

import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
public class CreateProject {

	public static void createProject(
			Path projectPath, Set<String> umbrellaSources)
		throws Exception {

		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		ProjectInfo projectInfo = new ProjectInfo(
			properties.getProperty("project.name"),
			StringUtil.split(
				new String(
					Files.readAllBytes(projectPath.resolve("moduleList"))),
				','),
			umbrellaSources);

		String projectDir = projectPath.toString();

		_appendList(projectInfo, projectDir);

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		_document = documentBuilder.newDocument();

		_createProjectElement(projectInfo);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		StreamResult streamResult = new StreamResult(
			new File(projectDir, "nbproject/project.xml"));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(_document), streamResult);
	}

	private static void _appendList(ProjectInfo projectInfo, String projectDir)
		throws IOException {

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				Paths.get(projectDir, "nbproject", "project.properties"),
				Charset.defaultCharset(), StandardOpenOption.APPEND)) {

			StringBuilder sb = new StringBuilder("javac.classpath=\\\n");

			for (String modulePath : projectInfo.getModules()) {
				Path path = Paths.get(modulePath);

				Path moduleName = path.getFileName();

				sb.append("\t${reference.");
				sb.append(moduleName);
				sb.append(".jar}:\\\n");
			}

			sb.setLength(sb.length() - 3);

			bufferedWriter.append(sb);
			bufferedWriter.newLine();
		}
	}

	private static void _createConfiguration(
		Element projectElement, ProjectInfo projectInfo) {

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

		nameElement.appendChild(
			_document.createTextNode(projectInfo.getProjectName()));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = _document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		for (String module : projectInfo.getSources()) {
			_createRoots(sourceRootsElement, "src." + module + ".dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		dataElement.appendChild(testRootsElement);
	}

	private static void _createProjectElement(ProjectInfo projectInfo) {
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
		Element referencesElement, String module) {

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
		Element configurationElement, ProjectInfo projectInfo) {

		Element referencesElement = _document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		for (String module : projectInfo.getModules()) {
			Path path = Paths.get(module);

			Path moduleName = path.getFileName();

			_createReference(referencesElement, moduleName.toString());
		}
	}

	private static void _createRoots(
		Element sourceRootsElement, String module) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", module);

		sourceRootsElement.appendChild(rootElement);
	}

	private static Document _document;

	private static class ProjectInfo {

		public String[] getModules() {
			return _modules;
		}

		public String getProjectName() {
			return _projectName;
		}

		public Set<String> getSources() {
			return _sources;
		}

		private ProjectInfo(
			String projectName, String[] modules, Set<String> sources) {

			_projectName = projectName;

			_modules = modules;

			_sources = sources;
		}

		private final String[] _modules;
		private final String _projectName;
		private final Set<String> _sources;

	}

}