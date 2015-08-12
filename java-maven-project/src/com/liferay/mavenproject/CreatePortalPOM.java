package com.liferay.mavenproject;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
public class CreatePortalPOM {

	public static void main(String[] args) throws Exception {
		if (args.length != 8) {
			throw new IllegalArgumentException("Incorrect Number of arguments");
		}

		ProjectInfo projectInfo = new ProjectInfo(
			args[0], args[1], args[2], args[3], args[4], args[5],
			_reorderModules(args[6]), args[7].split(File.pathSeparator));

		CreateSourceModulePOM.CreateSourceModulePOM(
			projectInfo.getGroupId(), "SourceModule", projectInfo.getVersion(),
			projectInfo.getPackaging(), "SourceModule",
			projectInfo.getModules());

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

		if (projectInfo.getArtifactId().equals("portal")) {
			streamResult = new StreamResult(
				new File(projectInfo.getArtifactId() + "/pom.xml"));
		}
		else {
			streamResult = new StreamResult(
				new File("portal/" + projectInfo.getArtifactId() + "/pom.xml"));
		}

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static void _createDependenciesElement(
		Element projectElement, ProjectInfo projectInfo) {

		Element dependenciesElement = _document.createElement("dependencies");

		projectElement.appendChild(dependenciesElement);

		for (String jar : projectInfo.getLib()) {
			_createDependencyElement(dependenciesElement, projectInfo, jar);
		}
	}

	private static void _createDependencyElement(
		Element dependenciesElement, ProjectInfo projectInfo, String jar) {

		String[] artifactIdToken = jar.split("/");

		Element dependencyElement = _document.createElement("dependency");

		Element dependencyGroupIdElement = _document.createElement("groupId");

		dependencyGroupIdElement.appendChild(
			_document.createTextNode(projectInfo.getGroupId()));

		dependencyElement.appendChild(dependencyGroupIdElement);

		Element dependencyArtifactIdElement = _document.createElement(
			"artifactId");

		dependencyArtifactIdElement.appendChild(
			_document.createTextNode(
				artifactIdToken[artifactIdToken.length - 1]));

		dependencyElement.appendChild(dependencyArtifactIdElement);

		Element dependencyVersionElement = _document.createElement("version");

		dependencyVersionElement.appendChild(
			_document.createTextNode(projectInfo.getVersion()));

		dependencyElement.appendChild(dependencyVersionElement);

		Element dependencyScopeElement = _document.createElement("scope");

		dependencyScopeElement.appendChild(_document.createTextNode("system"));

		dependencyElement.appendChild(dependencyScopeElement);

		Element dependencySystemPathElement = _document.createElement(
			"systemPath");

		dependencySystemPathElement.appendChild(_document.createTextNode(jar));

		dependencyElement.appendChild(dependencySystemPathElement);

		dependenciesElement.appendChild(dependencyElement);
	}

	private static void _createModulesElement(
		Element projectElement, ProjectInfo projectInfo) {

		Element modulesElement = _document.createElement("modules");

		projectElement.appendChild(modulesElement);

		for (String module : projectInfo.getModules()) {
			Element moduleElement = _document.createElement("module");

			moduleElement.appendChild(_document.createTextNode(module));

			modulesElement.appendChild(moduleElement);
		}
	}

	private static void _createPortalPOM(
			Element projectElement, ProjectInfo projectInfo)
		throws Exception {

		_createPropertiesElement(projectElement, projectInfo);

		_createModulesElement(projectElement, projectInfo);

		_createDependenciesElement(projectElement, projectInfo);

		_createRepositoriesElement(projectElement);
	}

	private static void _createProjectElement(ProjectInfo projectInfo)
		throws Exception {

		Element projectElement = CreatePOM.createProjectElement(
			_document, projectInfo);

		_createPortalPOM(projectElement, projectInfo);
	}

	private static void _createPropertiesElement(
		Element projectElement, ProjectInfo projectInfo) {

		Element propertiesElement = _document.createElement("properties");

		projectElement.appendChild(propertiesElement);

		Element portalSourceDirElement = _document.createElement(
			"sourceDirectory");

		portalSourceDirElement.appendChild(
			_document.createTextNode(projectInfo.getFullPath()));

		propertiesElement.appendChild(portalSourceDirElement);

		Element compilerSourceElement = _document.createElement(
			"maven.compiler.source");

		compilerSourceElement.appendChild(_document.createTextNode("1.7"));

		propertiesElement.appendChild(compilerSourceElement);

		Element compilerTargetElement = _document.createElement(
			"maven.compiler.target");

		compilerTargetElement.appendChild(_document.createTextNode("1.7"));

		propertiesElement.appendChild(compilerTargetElement);
	}

	private static void _createRepositoriesElement(Element projectElement)
		throws Exception {

		Element repositoriesElement = _document.createElement("repositories");

		projectElement.appendChild(repositoriesElement);

		_createRepositoryElement(
			repositoriesElement, "com.liferay.liferay-ce",
			"https://repository.liferay.com/nexus/content/groups/liferay-ce/");

		_createRepositoryElement(
			repositoriesElement, "public",
			"https://repository.liferay.com/nexus/content/groups/public/");

		_createRepositoryElement(
			repositoriesElement, "spring-releases",
			"http://repo.spring.io/libs-release-remote/");
	}

	private static void _createRepositoryElement(
			Element repositoriesElement, String repoId, String repoUrl)
		throws Exception {

		Element repositoryElement = _document.createElement("repository");

		repositoriesElement.appendChild(repositoryElement);

		Element repositoryIdElement = _document.createElement("id");

		repositoryElement.appendChild(repositoryIdElement);

		repositoryIdElement.appendChild(_document.createTextNode(repoId));

		Element repositoryURLElement = _document.createElement("url");

		repositoryElement.appendChild(repositoryURLElement);

		repositoryURLElement.appendChild(_document.createTextNode(repoUrl));
	}

	private static String[] _reorderModules(String originalOrder) {
		String[] modules = originalOrder.split(",");

		int i = 0;

		List<String> moduleSourceList = new ArrayList<>();
		List<String> portalSourceList = new ArrayList<>();

		for (String module : modules) {
			if (module.startsWith("module-")) {
				moduleSourceList.add(module);
			}
			else {
				portalSourceList.add(module);
			}
		}

		portalSourceList.addAll(moduleSourceList);

		return portalSourceList.toArray(new String[portalSourceList.size()]);
	}

	private static Document _document;

}