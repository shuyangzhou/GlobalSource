package com.liferay.mavenproject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
public class CreatePOM {

	public static Element createProjectElement(
			Document document, ProjectInfo projectInfo)
		throws Exception {

		Element projectElement = document.createElement("project");

		document.appendChild(projectElement);

		projectElement.setAttribute(
			"xmlns", "http://maven.apache.org/POM/4.0.0");
		projectElement.setAttribute(
			"xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		projectElement.setAttribute(
			"xsi:schemaLocation",
			"http://maven.apache.org/POM/4.0.0 " +
			"http://maven.apache.org/maven-v4_0_0.xsd");

		createArtifactElements(document, projectElement, projectInfo);

		return projectElement;
	}

	private static void createArtifactElements(
			Document document, Element projectElement, ProjectInfo projectInfo)
		throws Exception {

		Element modelVersionElement = document.createElement("modelVersion");

		modelVersionElement.appendChild(document.createTextNode("4.0.0"));

		projectElement.appendChild(modelVersionElement);

		Element groupIdElement = document.createElement("groupId");

		groupIdElement.appendChild(
			document.createTextNode(projectInfo.getGroupId()));

		projectElement.appendChild(groupIdElement);

		Element artifactIdElement = document.createElement("artifactId");

		String artifactId = projectInfo.getArtifactId();

		if (artifactId.startsWith("module-")) {
			artifactId = artifactId.substring(7);
		}

		artifactIdElement.appendChild(document.createTextNode(artifactId));

		projectElement.appendChild(artifactIdElement);

		Element versionElement = document.createElement("version");

		versionElement.appendChild(
			document.createTextNode(projectInfo.getVersion()));

		projectElement.appendChild(versionElement);

		Element packagingElement = document.createElement("packaging");

		packagingElement.appendChild(
			document.createTextNode(projectInfo.getPackaging()));

		projectElement.appendChild(packagingElement);

		Element nameElement = document.createElement("name");

		nameElement.appendChild(document.createTextNode(projectInfo.getName()));

		projectElement.appendChild(nameElement);
	}

}