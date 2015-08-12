package com.liferay.mavenproject.module;

import com.liferay.mavenproject.CreatePOM;
import com.liferay.mavenproject.ProjectInfo;

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
import org.w3c.dom.NodeList;
public class CreateModulePOM {

	public static void main(String[] args) throws Exception {
		if (args.length != 10) {
			throw new IllegalArgumentException("Incorrect Number of arguments");
		}

		ProjectInfo projectInfo = new ProjectInfo(
			args[0], args[1], args[2], args[3], args[4], args[5], args[6],
			args[7], args[8], args[9].split(","));

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		_documentBuilder = documentBuilderFactory.newDocumentBuilder();

		_document = _documentBuilder.newDocument();

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

	private static void _createBuildElement(
		Element portalSourceDirElement, Element projectElement,
		ProjectInfo projectInfo) {

		Element buildElement = _document.createElement("build");

		projectElement.appendChild(buildElement);

		buildElement.appendChild(portalSourceDirElement);

		String fullSourcePath = projectInfo.getFullPath();

		String portalPath = projectInfo.getPortalPath();

		String relativePath = fullSourcePath.substring(portalPath.length());

		String testPath =
			relativePath.substring(0, relativePath.length() - 3) + "test";

		if (new File(portalPath + testPath).exists()) {
			Element testSourceDirElement = _document.createElement(
				"testSourceDirectory");

			testSourceDirElement.appendChild(
				_document.createTextNode("${sourceDirectory}" + testPath));

			buildElement.appendChild(testSourceDirElement);
		}
	}

	private static void _createDependenciesElement(
			Element projectElement, ProjectInfo projectInfo)
		throws Exception {

		Element dependenciesElement = _document.createElement("dependencies");

		projectElement.appendChild(dependenciesElement);

		_parseModuleBuildFile(dependenciesElement, projectInfo);

		_parseIvyDependencies(dependenciesElement, projectInfo);

		for (String module : projectInfo.getModules()) {
			if (!module.isEmpty()) {
				_createDependencyElement(
					dependenciesElement, projectInfo, module);
			}
		}
	}

	private static void _createDependencyElement(
		Element dependenciesElement, ProjectInfo projectInfo, String module) {

		String[] dependencyTokens = module.split(":");

		String[] artifactIdToken =
			dependencyTokens[dependencyTokens.length - 1].split("/");

		Element dependencyElement = _document.createElement("dependency");

		Element dependencyGroupIdElement = _document.createElement("groupId");

		if (artifactIdToken[0].equals("")) {
			dependencyGroupIdElement.appendChild(
				_document.createTextNode(projectInfo.getGroupId()));
		}
		else {
			dependencyGroupIdElement.appendChild(
				_document.createTextNode(dependencyTokens[0]));
		}

		dependencyElement.appendChild(dependencyGroupIdElement);

		Element dependencyArtifactIdElement = _document.createElement(
			"artifactId");

		dependencyArtifactIdElement.appendChild(
			_document.createTextNode(
				artifactIdToken[artifactIdToken.length - 1]));

		dependencyElement.appendChild(dependencyArtifactIdElement);

		Element dependencyVersionElement = _document.createElement("version");

		if (artifactIdToken[0].equals("")) {
			dependencyVersionElement.appendChild(
				_document.createTextNode(projectInfo.getVersion()));
		}
		else {
			dependencyVersionElement.appendChild(
				_document.createTextNode(dependencyTokens[1]));
		}

		dependencyElement.appendChild(dependencyVersionElement);

		if (dependencyTokens.length > 3) {
			Element dependencyScopeElement = _document.createElement("scope");

			if (dependencyTokens[2].equals("master")) {
				dependencyScopeElement.appendChild(
					_document.createTextNode("compile"));

				dependencyElement.appendChild(dependencyScopeElement);

				Element exclusionsElement = _document.createElement(
					"exclusions");

				dependencyElement.appendChild(exclusionsElement);

				Element exclusionElement = _document.createElement("exclusion");

				exclusionsElement.appendChild(exclusionElement);

				Element exclusionGroupIdElement = _document.createElement(
					"groupId");

				exclusionElement.appendChild(exclusionGroupIdElement);

				exclusionGroupIdElement.appendChild(
					_document.createTextNode("*"));

				Element exclusionArtifactIdElement = _document.createElement(
					"artifactId");

				exclusionElement.appendChild(exclusionArtifactIdElement);

				exclusionArtifactIdElement.appendChild(
					_document.createTextNode("*"));
			}
			else {
				dependencyScopeElement.appendChild(
					_document.createTextNode(dependencyTokens[2]));

				dependencyElement.appendChild(dependencyScopeElement);
			}
		}

		if (artifactIdToken[artifactIdToken.length - 1].endsWith(".jar")) {
			Element dependencyScopeElement = _document.createElement("scope");

			dependencyScopeElement.appendChild(
				_document.createTextNode("system"));

			dependencyElement.appendChild(dependencyScopeElement);

			Element dependencySystemPathElement = _document.createElement(
				"systemPath");

			dependencySystemPathElement.appendChild(
				_document.createTextNode(
					dependencyTokens[dependencyTokens.length - 1]));

			dependencyElement.appendChild(dependencySystemPathElement);
		}

		dependenciesElement.appendChild(dependencyElement);
	}

	private static void _createModulePOM(
			Element projectElement, Element portalSourceDirElement,
			ProjectInfo projectInfo)
		throws Exception {

		_createBuildElement(
			portalSourceDirElement, projectElement, projectInfo);

		_createDependenciesElement(projectElement, projectInfo);
	}

	private static void _createParentElement(
		Element projectElement, Element portalSourceDirElement,
		ProjectInfo projectInfo) {

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

		String fullPath = projectInfo.getFullPath();

		String portalPath = projectInfo.getPortalPath();

		String path = fullPath.substring(portalPath.length());

		portalSourceDirElement.appendChild(
			_document.createTextNode("${sourceDirectory}" + path));
	}

	private static void _createProjectElement(ProjectInfo projectInfo)
		throws Exception {

		Element projectElement = CreatePOM.createProjectElement(
			_document, projectInfo);

		Element portalSourceDirElement = _document.createElement(
			"sourceDirectory");

		_createParentElement(
			projectElement, portalSourceDirElement, projectInfo);

		_createModulePOM(projectElement, portalSourceDirElement, projectInfo);
	}

	private static void _parseIvyDependencies(
			Element dependenciesElement, ProjectInfo projectInfo)
		throws Exception {

		if (!projectInfo.getIvyFile().startsWith("$")) {
			File ivyFile = new File(projectInfo.getIvyFile());

			Document ivyDocument = _documentBuilder.parse(ivyFile);

			ivyDocument.getDocumentElement().normalize();

			NodeList ivyDependencyList = ivyDocument.getElementsByTagName(
				"dependency");

			for (int i = 0; i < ivyDependencyList.getLength(); i++) {
				Element ivyDependencyElement = (Element)ivyDependencyList.item(
					i);

				String ivyDependency = null;

				if (ivyDependencyElement.getAttribute("conf").isEmpty()) {
					ivyDependency =
						ivyDependencyElement.getAttribute("org") + ":" +
						ivyDependencyElement.getAttribute("rev") + ":" +
						ivyDependencyElement.getAttribute("name");
				}
				else {
					String ivyConf = ivyDependencyElement.getAttribute("conf");

					if (ivyConf.endsWith("master") &&
						!ivyConf.startsWith("internal")) {

						ivyConf = "master";
					}
					else {
						ivyConf = "compile";
					}

					ivyDependency =
						ivyDependencyElement.getAttribute("org") + ":" +
						ivyDependencyElement.getAttribute("rev") + ":" +
						ivyConf + ":" +
						ivyDependencyElement.getAttribute("name");
				}

				_createDependencyElement(
					dependenciesElement, projectInfo, ivyDependency);
			}
		}
	}

	private static void _parseModuleBuildFile(
			Element dependenciesElement, ProjectInfo projectInfo)
		throws Exception {

		if (!projectInfo.getBuildFile().startsWith("$")) {
			File moduleBuildFile = new File(projectInfo.getBuildFile());

			Document moduleBuildFileDocument = _documentBuilder.parse(
				moduleBuildFile);

			Element moduleBuildFileElement =
				moduleBuildFileDocument.getDocumentElement();

			moduleBuildFileElement.normalize();

			NodeList modulePropertyList =
				moduleBuildFileDocument.getElementsByTagName("property");

			for (int i = 0; i < modulePropertyList.getLength(); i++) {
				Element modulePropertyElement =
					(Element)modulePropertyList.item(i);

				String modulePropertyElementName =
					modulePropertyElement.getAttribute("name");

				if (modulePropertyElementName.equals("import.shared")) {
					String moduleDependencyString =
						modulePropertyElement.getAttribute("value");

					String[] moduleDependencyList =
						moduleDependencyString.split(",");

					for (String moduleDependency : moduleDependencyList) {
						String[] moduleDependencySplit = moduleDependency.split(
							"/");

						String module =
							projectInfo.getGroupId() + ":" +
							projectInfo.getVersion() + ":" +
							moduleDependencySplit[
							moduleDependencySplit.length - 1];

						_createDependencyElement(
							dependenciesElement, projectInfo, module);
					}
				}
			}

			NodeList webLibPathNodes =
				moduleBuildFileDocument.getElementsByTagName("path");

			for (int i = 0; i < webLibPathNodes.getLength(); i++) {
				Element webLibPathElement = (Element)webLibPathNodes.item(i);

				String webLibPathElementId = webLibPathElement.getAttribute(
					"id");

				if (webLibPathElementId.equals("web-lib.classpath")) {
					NodeList filesetNodeList =
						webLibPathElement.getElementsByTagName("fileset");

					Element filesetElement = (Element)filesetNodeList.item(0);

					String libDependencyString = filesetElement.getAttribute(
						"includes");

					String[] libDependencyList = libDependencyString.split(",");

					String libDependencyPath = filesetElement.getAttribute(
						"dir");

					String parsedPath = libDependencyPath.replace(
						"${project.dir}", projectInfo.getPortalPath());

					for (String libDependency : libDependencyList) {
						libDependencyPath = parsedPath + "/" + libDependency;

						File dependencyFile = new File(libDependencyPath);

						if (!dependencyFile.exists()) {
							System.out.println(
								"Lib path error at: " + parsedPath +
									" for module: " +
										projectInfo.getArtifactId());
						}
						else {
							_createDependencyElement(
								dependenciesElement, projectInfo,
								libDependencyPath);
						}
					}
				}
			}
		}
	}

	private static Document _document;
	private static DocumentBuilder _documentBuilder;

}