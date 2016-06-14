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
import com.liferay.netbeansproject.util.ModuleUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.ZipUtil;

import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

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
public class CreateUmbrella {

	public static void createUmbrella(
			Map<Path, Module> projectMap, Path portalPath,
			Properties buildProperties)
		throws Exception {

		Path portalNamePath = portalPath.getFileName();

		Path projectPath = Paths.get(
			buildProperties.getProperty("project.dir"),
			portalNamePath.toString());

		ZipUtil.unZip(projectPath);

		Map<String, String> umbrellaSourceMap = PropertiesUtil.getProperties(
			buildProperties, "umbrella.source.list");

		_appendProjectProperties(
			projectMap, umbrellaSourceMap, portalPath, projectPath,
			buildProperties.getProperty("exclude.types"));

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.newDocument();

		String projectName = buildProperties.getProperty("project.name");

		_createProjectElement(
			document, projectMap, umbrellaSourceMap, projectName);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");

		Path projectXMLPath = projectPath.resolve("nbproject/project.xml");

		transformer.transform(
			new DOMSource(document), new StreamResult(projectXMLPath.toFile()));
	}

	private static void _appendProjectProperties(
			Map<Path, Module> projectMap, Map<String, String> umbrellaSourceMap,
			Path portalPath, Path projectPath, String excludeTypes)
		throws IOException {

		StringBuilder sb = new StringBuilder();

		sb.append("excludes=");
		sb.append(excludeTypes);
		sb.append('\n');

		for (Entry<String, String> source : umbrellaSourceMap.entrySet()) {
			String key = source.getKey();

			sb.append("file.reference.");
			sb.append(key);
			sb.append(".src=");
			sb.append(portalPath.resolve(source.getValue()));
			sb.append('\n');
			sb.append("src.");
			sb.append(key);
			sb.append(".dir=${file.reference.");
			sb.append(key);
			sb.append(".src}");
			sb.append('\n');
		}

		Path projectModulesPath = projectPath.resolve("modules");

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

		for (Path modulePath : projectMap.keySet()) {
			String name = ModuleUtil.getModuleName(modulePath);

			sb.append("project.");
			sb.append(name);
			sb.append('=');
			sb.append(projectModulesPath.resolve(name));
			sb.append('\n');
			sb.append("reference.");
			sb.append(name);
			sb.append(".jar=${project.");
			sb.append(name);
			sb.append("}/dist/");
			sb.append(name);
			sb.append(".jar\n");

			javacSB.append("\t${reference.");
			javacSB.append(name);
			javacSB.append(".jar}:\\\n");
		}

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				projectPath.resolve("nbproject/project.properties"),
				StandardOpenOption.APPEND)) {

			bufferedWriter.append(sb);
			bufferedWriter.newLine();

			javacSB.setLength(javacSB.length() - 3);

			bufferedWriter.append(javacSB);
			bufferedWriter.newLine();
		}
	}

	private static void _createConfiguration(
		Document document, Element projectElement, Map<Path, Module> projectMap,
		Map<String, String> umbrellaSourceMap, String projectName) {

		Element configurationElement = document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(
			document, configurationElement, umbrellaSourceMap, projectName);

		_createReferences(document, configurationElement, projectMap);
	}

	private static void _createData(
		Document document, Element configurationElement,
		Map<String, String> umbrellaSourceMap, String projectName) {

		Element dataElement = document.createElement("data");

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		configurationElement.appendChild(dataElement);

		Element nameElement = document.createElement("name");

		nameElement.appendChild(document.createTextNode(projectName));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		for (String module : umbrellaSourceMap.keySet()) {
			_createRoots(
				document, sourceRootsElement, "src." + module + ".dir");
		}

		Element testRootsElement = document.createElement("test-roots");

		dataElement.appendChild(testRootsElement);
	}

	private static void _createProjectElement(
		Document document, Map<Path, Module> projectMap,
		Map<String, String> umbrellaSourceMap, String projectName) {

		Element projectElement = document.createElement("project");

		projectElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/project/1");

		document.appendChild(projectElement);

		Element typeElement = document.createElement("type");

		typeElement.appendChild(
			document.createTextNode("org.netbeans.modules.java.j2seproject"));

		projectElement.appendChild(typeElement);

		_createConfiguration(
			document, projectElement, projectMap, umbrellaSourceMap,
			projectName);
	}

	private static void _createReference(
		Document document, Element referencesElement, String module) {

		Element referenceElement = document.createElement("reference");

		referencesElement.appendChild(referenceElement);

		Element foreignProjectElement = document.createElement(
			"foreign-project");

		foreignProjectElement.appendChild(document.createTextNode(module));

		referenceElement.appendChild(foreignProjectElement);

		Element artifactTypeElement = document.createElement("artifact-type");

		artifactTypeElement.appendChild(document.createTextNode("jar"));

		referenceElement.appendChild(artifactTypeElement);

		Element scriptElement = document.createElement("script");

		scriptElement.appendChild(document.createTextNode("build.xml"));

		referenceElement.appendChild(scriptElement);

		Element targetElement = document.createElement("target");

		targetElement.appendChild(document.createTextNode("jar"));

		referenceElement.appendChild(targetElement);

		Element cleanTargetElement = document.createElement("clean-target");

		cleanTargetElement.appendChild(document.createTextNode("clean"));

		referenceElement.appendChild(cleanTargetElement);

		Element idElement = document.createElement("id");

		idElement.appendChild(document.createTextNode("jar"));

		referenceElement.appendChild(idElement);
	}

	private static void _createReferences(
		Document document, Element configurationElement,
		Map<Path, Module> projectMap) {

		Element referencesElement = document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		for (Path modulePath : projectMap.keySet()) {
			_createReference(
				document, referencesElement,
				ModuleUtil.getModuleName(modulePath));
		}
	}

	private static void _createRoots(
		Document document, Element sourceRootsElement, String module) {

		Element rootElement = document.createElement("root");

		rootElement.setAttribute("id", module);

		sourceRootsElement.appendChild(rootElement);
	}

}