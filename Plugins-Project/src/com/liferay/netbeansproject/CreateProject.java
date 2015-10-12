package com.liferay.netbeansproject;

import java.io.File;

import java.util.List;
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

public class CreateProject {

	public static void CreateProject(List<File> sourceList) throws Exception {
		_buildProperties = _propertyLoader.loadPropertyFile("build.properties");

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.newDocument();

		_createProjectElement(document, sourceList);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		DOMSource source = new DOMSource(document);

		StreamResult streamResult = null;

		streamResult = new StreamResult(
			new File("plugins/nbproject/project.xml"));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static void _createConfigurationElement(
			Document document, Element projectElement, List<File> sourceList)
		throws Exception {

		Element configurationElement = document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createDataElement(document, configurationElement, sourceList);

		_createReferencesElement(document, configurationElement);
	}

	private static void _createDataElement(
			Document document, Element configurationElement,
				List<File> sourceList)
		throws Exception {

		Element dataElement = document.createElement("data");

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		configurationElement.appendChild(dataElement);

		Element nameElement = document.createElement("name");

		nameElement.appendChild(document.createTextNode("Plugins"));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		String pluginsDir = _buildProperties.getProperty("plugins.dir");

		for (File source : sourceList) {
			String id = "src." + source.getName() + ".dir";

			String fullPath = source.getAbsolutePath();

			String label = fullPath.replace(pluginsDir, "");

			_createRootElement(document, sourceRootsElement, id, label);
		}

		Element testRootsElement = document.createElement("test-roots");

		_createRootElement(document, testRootsElement, "test.src.dir", "");

		dataElement.appendChild(testRootsElement);
	}

	private static void _createProjectElement(
			Document document, List<File> sourceList)
		throws Exception {

		Element projectElement = document.createElement("project");

		projectElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/project/1");

		document.appendChild(projectElement);

		Element typeElement = document.createElement("type");

		typeElement.appendChild(
			document.createTextNode("org.netbeans.modules.java.j2seproject"));

		projectElement.appendChild(typeElement);

		_createConfigurationElement(document, projectElement, sourceList);
	}

	private static void _createReferenceElement(
		Document document, Element referencesElement, String name) {

		Element referenceElement = document.createElement("reference");

		referencesElement.appendChild(referenceElement);

		Element foreignProjectElement = document.createElement(
			"foreign-project");

		foreignProjectElement.appendChild(document.createTextNode(name));

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

	private static void _createReferencesElement(
		Document document, Element configurationElement) {

		Element referencesElement = document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		String projectDependencies = _buildProperties.getProperty(
			"project.dependencies");

		for (String name : projectDependencies.split(",")) {
			_createReferenceElement(document, referencesElement, name);
		}
	}

	private static void _createRootElement(
		Document document, Element sourceRootsElement, String id,
			String label) {

		Element rootElement = document.createElement("root");

		rootElement.setAttribute("id", id);

		if (!label.equals("")) {
			rootElement.setAttribute("name", label);
		}

		sourceRootsElement.appendChild(rootElement);
	}

	private static Properties _buildProperties;
	private static final PropertyLoader _propertyLoader = new PropertyLoader();

}