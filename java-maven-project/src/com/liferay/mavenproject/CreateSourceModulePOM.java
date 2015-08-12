package com.liferay.mavenproject;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
public class CreateSourceModulePOM {

	public static void CreateSourceModulePOM(
		String groupId, String artifactId, String version, String packaging,
		String name, String[] modules) throws Exception {

		ProjectInfo projectInfo = new ProjectInfo(
			groupId, artifactId, version, packaging, name, modules);

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		_document = documentBuilder.newDocument();

		_createProjectElement(projectInfo);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		DOMSource source = new DOMSource(_document);

		StreamResult streamResult = null;

		streamResult = new StreamResult(
			new File("portal/" + projectInfo.getArtifactId() + "/pom.xml"));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static void _createDependenciesElement(
		Element projectElement, ProjectInfo projectInfo) {

		Element dependenciesElement = _document.createElement("dependencies");

		projectElement.appendChild(dependenciesElement);

		for (String module : projectInfo.getModules()) {
			if (!module.equals(projectInfo.getArtifactId())) {
				_createDependencyElement(
					dependenciesElement, projectInfo, module);
			}
		}
	}

	private static void _createDependencyElement(
		Element dependenciesElement, ProjectInfo projectInfo, String module) {

		Element dependencyElement = _document.createElement("dependency");

		dependenciesElement.appendChild(dependencyElement);

		Element groupIdElement = _document.createElement("groupId");

		groupIdElement.appendChild(
			_document.createTextNode(projectInfo.getGroupId()));

		dependencyElement.appendChild(groupIdElement);

		Element artifactIdElement = _document.createElement("artifactId");

		artifactIdElement.appendChild(_document.createTextNode(module));

		dependencyElement.appendChild(artifactIdElement);

		Element versionElement = _document.createElement("version");

		versionElement.appendChild(
			_document.createTextNode(projectInfo.getVersion()));

		dependencyElement.appendChild(versionElement);
	}

	private static void _createParentElement(
		Element projectElement, ProjectInfo projectInfo) {

		Element parent = _document.createElement("parent");

		projectElement.appendChild(parent);

		Element groupIdElement = _document.createElement("groupId");

		groupIdElement.appendChild(
			_document.createTextNode(projectInfo.getGroupId()));

		parent.appendChild(groupIdElement);

		Element parentArtifactIdElement = _document.createElement("artifactId");

		parentArtifactIdElement.appendChild(_document.createTextNode("portal"));

		parent.appendChild(parentArtifactIdElement);

		Element parentVersionElement = _document.createElement("version");

		parentVersionElement.appendChild(
			_document.createTextNode(projectInfo.getVersion()));

		parent.appendChild(parentVersionElement);
	}

	private static void _createProjectElement(ProjectInfo projectInfo)
		throws Exception {

		Element projectElement = CreatePOM.createProjectElement(
			_document, projectInfo);

		_createParentElement(projectElement, projectInfo);

		_createDependenciesElement(projectElement, projectInfo);
	}

	private static Document _document;

}