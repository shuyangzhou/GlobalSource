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

import com.liferay.netbeansproject.util.FileUtil;
import com.liferay.netbeansproject.util.ModuleUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.Map;
import java.util.Map.Entry;
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
public class CreateUmbrella {

	public static void createUmbrella(
			Path portalPath, String projectName,
			Map<String, String> umbrellaSourceMap, String excludeTypes,
			Set<Path> modulePaths, Path projectPath)
		throws Exception {

		FileUtil.delete(projectPath);

		FileUtil.unZip(projectPath);

		_appendProjectProperties(
			portalPath, excludeTypes, umbrellaSourceMap, modulePaths,
			projectPath);

		_createProjectXML(
			projectName, umbrellaSourceMap, modulePaths, projectPath);
	}

	private static void _appendProjectProperties(
			Path portalPath, String excludeTypes,
			Map<String, String> umbrellaSourceMap, Set<Path> modulePaths,
			Path projectPath)
		throws IOException {

		StringBuilder sb = new StringBuilder();

		sb.append("excludes=");

		if (excludeTypes != null) {
			sb.append(excludeTypes);
		}

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

		Path projectRootPath = projectPath.getParent();

		Path projectModulesPath = projectRootPath.resolve("modules");

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

		for (Path modulePath : modulePaths) {
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

			if (!modulePaths.isEmpty()) {
				javacSB.setLength(javacSB.length() - 3);
			}

			bufferedWriter.append(javacSB);
			bufferedWriter.newLine();
		}
	}

	private static void _createData(
		Document document, Element configurationElement,
		Map<String, String> umbrellaSourceMap, String projectName) {

		Element dataElement = document.createElement("data");

		configurationElement.appendChild(dataElement);

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		Element nameElement = document.createElement("name");

		dataElement.appendChild(nameElement);

		nameElement.appendChild(document.createTextNode(projectName));

		Element sourceRootsElement = document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		for (String module : umbrellaSourceMap.keySet()) {
			_createRoot(document, sourceRootsElement, "src." + module + ".dir");
		}

		dataElement.appendChild(document.createElement("test-roots"));
	}

	private static void _createProjectElement(
		Document document, String projectName,
		Map<String, String> umbrellaSourceMap, Set<Path> modulePaths) {

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

		_createData(
			document, configurationElement, umbrellaSourceMap, projectName);

		_createReferences(document, configurationElement, modulePaths);
	}

	private static void _createProjectXML(
			String projectName, Map<String, String> umbrellaSourceMap,
			Set<Path> modulePaths, Path projectPath)
		throws Exception {

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.newDocument();

		_createProjectElement(
			document, projectName, umbrellaSourceMap, modulePaths);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");

		try (Writer writer = Files.newBufferedWriter(
				projectPath.resolve("nbproject/project.xml"))) {

			transformer.transform(
				new DOMSource(document), new StreamResult(writer));
		}
	}

	private static void _createReference(
		Document document, Element referencesElement, String module) {

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
		Document document, Element configurationElement,
		Set<Path> modulePaths) {

		Element referencesElement = document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		for (Path modulePath : modulePaths) {
			_createReference(
				document, referencesElement,
				ModuleUtil.getModuleName(modulePath));
		}
	}

	private static void _createRoot(
		Document document, Element sourceRootsElement, String module) {

		Element rootElement = document.createElement("root");

		sourceRootsElement.appendChild(rootElement);

		rootElement.setAttribute("id", module);
	}

}