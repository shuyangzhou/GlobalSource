package com.liferay.netbeansproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
public class GradleParser {

	public static void main(String[] args) throws Exception {
		List<IvyDependency> ivyDependencyList = _parseBuildFile(args[0]);

		_createIvyFile(ivyDependencyList, args[1]);
	}

	private static void _appendIvy(
		boolean transitive, List<IvyDependency> ivyDependencyList,
		Map<String, String> variableList, String line, String type) {

		String[] ivyLine = StringUtils.substringsBetween(line, "\"", "\"");

		if (ivyLine.length < 3) {
			line = line.replaceAll("[\",]", "");

			String[] variableLine = line.split(" ");

			String name = "";
			String version = "";

			for (int i = 0; i < variableLine.length; i++) {
				if (variableLine[i].equals("name:")) {
					name = variableList.get(variableLine[i + 1]);
				}

				if (variableLine[i].equals("version:")) {
					version = variableList.get(variableLine[i + 1]);
				}
			}

			ivyDependencyList.add(
				new IvyDependency(name, ivyLine[0], version, transitive, type));
		}
		else {
			ivyDependencyList.add(
				new IvyDependency(
					ivyLine[1], ivyLine[0], ivyLine[2], transitive, type));
		}
	}

	private static void _createDependenciesElement(
		Element ivyModuleElement, List<IvyDependency> ivyDependencyList) {

		Element dependenciesElement = _document.createElement("dependencies");

		dependenciesElement.setAttribute("defaultconf", "default");

		ivyModuleElement.appendChild(dependenciesElement);

		for (IvyDependency ivyDependency : ivyDependencyList) {
			_createDependencyElement(dependenciesElement, ivyDependency);
		}
	}

	private static void _createDependencyElement(
		Element dependenciesElement, IvyDependency ivyDependency) {

		Element dependencyElement = _document.createElement("dependency");

		dependenciesElement.appendChild(dependencyElement);

		String dependencyType = ivyDependency.getType();

		if (!dependencyType.equals("default")) {
			dependencyElement.setAttribute("conf", dependencyType);
		}

		dependencyElement.setAttribute("name", ivyDependency.getName());
		dependencyElement.setAttribute("org", ivyDependency.getOrg());
		dependencyElement.setAttribute("rev", ivyDependency.getRev());

		if (!ivyDependency.isTransitive()) {
			dependencyElement.setAttribute("transitive", "false");
		}
	}

	private static void _createInfoElement(Element ivyModuleElement) {
		Element infoElement = _document.createElement("info");

		infoElement.setAttribute("module", "${plugin.name}");
		infoElement.setAttribute("organisation", "com.liferay");

		ivyModuleElement.appendChild(infoElement);

		Element extendsElement = _document.createElement("extends");

		extendsElement.setAttribute(
			"extendType", "configurations,description,info");
		extendsElement.setAttribute("location", "${sdk.dir}/ivy.xml");
		extendsElement.setAttribute("module", "com.liferay.sdk");
		extendsElement.setAttribute("organisation", "com.liferay");
		extendsElement.setAttribute("revision", "latest.integration");

		infoElement.appendChild(extendsElement);
	}

	private static void _createIvyFile(
		List<IvyDependency> ivyDependencyList, String projectName)
			throws Exception {

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		_document = documentBuilder.newDocument();

		_createIvyModuleElement(ivyDependencyList);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		DOMSource source = new DOMSource(_document);

		StreamResult streamResult = null;

		String fileName = "portal/modules/" + projectName + "/ivy.xml";

		streamResult = new StreamResult(new File(fileName));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static void _createIvyModuleElement(
		List<IvyDependency> ivyDependencyList) {

		Element ivyModuleElement = _document.createElement("ivy-module");

		ivyModuleElement.setAttribute("version", "2.0");
		ivyModuleElement.setAttribute(
			"xmlns:m2","http://ant.apache.org/ivy/maven");
		ivyModuleElement.setAttribute(
			"xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		ivyModuleElement.setAttribute(
				"xsi:noNamespaceSchemaLocation",
				"http://ant.apache.org/ivy/schemas/ivy.xsd");

		_document.appendChild(ivyModuleElement);

		_createInfoElement(ivyModuleElement);

		_createPublicationsElement(ivyModuleElement);

		_createDependenciesElement(ivyModuleElement, ivyDependencyList);
	}

	private static void _createPublicationsElement(Element ivyModuleElement) {
		Element publicationsElement = _document.createElement("publications");

		ivyModuleElement.appendChild(publicationsElement);

		Element artifactElement1 = _document.createElement("artifact");

		artifactElement1.setAttribute("type", "jar");

		publicationsElement.appendChild(artifactElement1);

		Element artifactElement2 = _document.createElement("artifact");

		artifactElement2.setAttribute("type", "pom");

		publicationsElement.appendChild(artifactElement2);

		Element artifactElement3 = _document.createElement("artifact");

		artifactElement3.setAttribute("m2:classifier", "sources");

		publicationsElement.appendChild(artifactElement3);
	}

	private static List<IvyDependency> _parseBuildFile(String modulePath)
		throws Exception {

		File gradleFile = new File(modulePath + "/build.gradle");

		List<IvyDependency> ivyDependencyList = new ArrayList();

		if (gradleFile.exists()) {
			try(BufferedReader br =
					new BufferedReader(new FileReader(gradleFile))) {

				String line = br.readLine();

				Map<String, String> variableList = new LinkedHashMap<>();

				while (line != null) {
					line = line.trim();

					_solveLine(ivyDependencyList, variableList, line);

					line = br.readLine();
				}
			}
		}

		return ivyDependencyList;
	}

	private static void _solveLine(
		List<IvyDependency> ivyDependencyList, Map<String, String> variableList,
		String line) {

		if (line.startsWith("String")) {
			String[] vars = line.split(" ");

			if (vars.length > 2) {
				if (vars[0].equals("String") && vars[2].equals("=")) {
					variableList.put(vars[1], vars[3].replace("\"", ""));
				}
			}
		}

		boolean transitive = true;

		if (line.contains("transitive: false")) {
			transitive = false;
		}

		if (line.startsWith("compile group")) {
			_appendIvy(
				transitive, ivyDependencyList, variableList, line, "default");
		}

		if (line.startsWith("provided group")) {
			_appendIvy(
				transitive, ivyDependencyList, variableList, line, "default");
		}

		if (line.startsWith("testCompile")) {
			_appendIvy(
				transitive, ivyDependencyList, variableList, line,
					"test->default");
		}

		// Special case for dynamic-data-mapping-form-values-query

		if (line.startsWith("antlr group")) {
			_appendIvy(
				transitive, ivyDependencyList, variableList, line, "default");
		}
	}

	private static Document _document;

	private static class IvyDependency {

		public String getName() {
			return _name;
		}

		public String getOrg() {
			return _org;
		}

		public String getRev() {
			return _rev;
		}

		public String getType() {
			return _type;
		}

		public boolean isTransitive() {
			return _transitive;
		}

		private IvyDependency(
			String name, String org, String rev, boolean transitive,
			String type) {

			_name = name;
			_org = org;
			_rev = rev;
			_transitive = transitive;
			_type = type;
		}

		private final String _name;
		private final String _org;
		private final String _rev;
		private final boolean _transitive;
		private final String _type;

	}

}