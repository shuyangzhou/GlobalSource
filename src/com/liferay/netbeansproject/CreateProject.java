package com.liferay.netbeansproject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

import com.liferay.netbeansproject.AppendLibJars;

public class CreateProject {

	private static void createConfiguration(
		Element projectElement, ProjectInfo projectInfo) {

		Element configurationElement = document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		createData(configurationElement, projectInfo);

		createLibraries(configurationElement);
	}

	private static void createData(
		Element configurationElement, ProjectInfo projectInfo) {

		Element dataElement = document.createElement("data");

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		configurationElement.appendChild(dataElement);

		Element nameElement = document.createElement("name");

		nameElement.appendChild(
			document.createTextNode(projectInfo.projectName));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		String moduleName="";

		String relativePath = "";

		for (String module : projectInfo.modules) {
			if(module.startsWith(projectInfo.portalDir)) {
				relativePath =
					module.substring(projectInfo.portalDir.length()+1);
			}
			else {
				relativePath = module;
			}

			String[] moduleSplit = module.split("/");

			moduleName = moduleSplit[moduleSplit.length - 1];

			if(verifySourceFolder(projectInfo, moduleName)) {
				createRoots(
					sourceRootsElement, "src." + moduleName + ".dir",
					relativePath);
			}
		}

		Element testRootsElement = document.createElement("test-roots");

		dataElement.appendChild(testRootsElement);

		for (String test : projectInfo.tests) {
			if(test.startsWith(projectInfo.portalDir)) {
				relativePath =
					test.substring(projectInfo.portalDir.length() + 1);
			}
			else {
				relativePath = test;
			}

			String[] testSplit = test.split("/");

			String testName = testSplit[testSplit.length - 1];

			createRoots(
				testRootsElement, "test." + testName + ".dir", relativePath);
		}
	}

	private static void createLibraries(Element configurationElement) {
		Element librariesElement = document.createElement("libraries");

		librariesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-libraries/1");

		configurationElement.appendChild(librariesElement);

		Element definitionsElement = document.createElement("definitions");

		definitionsElement.appendChild(
			document.createTextNode("./lib/nblibraries.properties"));

		librariesElement.appendChild(definitionsElement);
	}

	private static void createProjectElement(ProjectInfo projectInfo) {
		Element projectElement = document.createElement("project");

		projectElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/project/1");

		document.appendChild(projectElement);

		Element typeElement = document.createElement("type");

		typeElement.appendChild(
			document.createTextNode("org.netbeans.modules.java.j2seproject"));

		projectElement.appendChild(typeElement);

		createConfiguration(projectElement, projectInfo);
	}

	private static void createRoots(
		Element sourceRootsElement, String module, String moduleName) {

		Element rootElement = document.createElement("root");

		rootElement.setAttribute("id", module);

		rootElement.setAttribute("name", moduleName);

		sourceRootsElement.appendChild(rootElement);
	}

	public static void main(String[] args) throws Exception {
		AppendLibJars.AppendJars(args[4]);

		ProjectInfo projectInfo = parseArgument(args);

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		document = documentBuilder.newDocument();

		createProjectElement(projectInfo);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		DOMSource source = new DOMSource(document);

		StreamResult streamResult;

		streamResult =
			new StreamResult(new File("portal/nbproject/project.xml"));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static ProjectInfo parseArgument(String[] args) {
		ProjectInfo projectInfo = new ProjectInfo();

		projectInfo.projectName = args[0];

		projectInfo.portalDir = args[1];

		projectInfo.modules = reorderModules(args[2], projectInfo.portalDir);

		projectInfo.tests = reorderModules(args[3], projectInfo.portalDir);

		return projectInfo;
	}

	private static String[] reorderModules(
		String originalOrder, String portalDir) {

		String[] modules = originalOrder.split(",");

		int i = 0;

		List<String> moduleSourceList = new ArrayList<>();

		while(modules[i].startsWith(portalDir + "/modules")) {
			moduleSourceList.add(modules[i]);

			i++;
		}

		List<String> portalSourceList = new ArrayList<>();

		while(i < modules.length) {
			portalSourceList.add(modules[i]);

			i++;
		}

		Collections.sort(portalSourceList);

		Collections.sort(moduleSourceList);

		portalSourceList.addAll(moduleSourceList);

		return portalSourceList.toArray(new String[portalSourceList.size()]);
	}

	private static boolean verifySourceFolder(
		ProjectInfo projectInfo, String moduleName) {

		File folder =
			new File(projectInfo.portalDir + "/" + moduleName + "/src");

		if(folder.exists()) {
			File[] listOfFiles = folder.listFiles();

			if(listOfFiles.length == 1) {
				String fileName = listOfFiles[0].getName();

				if(fileName.startsWith(".")) {
					return false;
				}
			}
		}

		return true;
	}

	static class ProjectInfo {
		public String[] modules;
		public String portalDir;
		public String projectName;
		public String[] tests;
	}

	private static Document document;

}