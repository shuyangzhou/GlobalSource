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

public class CreateProject {

	public static void main(String[] args) throws Exception {
		if(args.length != 5) {
			throw new IllegalArgumentException(
				"Incorrect Number of arguments");
		}

		ProjectInfo projectInfo =
			new ProjectInfo(
				args[0], args[1], _reorderModules(args[2],args[1]),
				_reorderModules(args[3],args[1]));

		String libList = _buildLibList(projectInfo, args[4]);

		AppendLibJars.appendJars(libList.split(File.pathSeparator));

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

		StreamResult streamResult;

		streamResult =
			new StreamResult(new File("portal/nbproject/project.xml"));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static String _buildLibList(
		ProjectInfo projectInfo, String portalJars) throws Exception {

		StringBuilder sb = new StringBuilder(portalJars + ":");

		for(String modulePath : projectInfo.getModules()) {
			sb.append(IvyReportParser.parseIvyReport(modulePath));
		}

		sb.setLength(sb.length() - 1);

		return sb.toString();
	}

	private static void _createConfiguration(
		Element projectElement, ProjectInfo projectInfo) {

		Element configurationElement = _document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(configurationElement, projectInfo);

		_createLibraries(configurationElement);
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

		String moduleName="";

		String relativePath = "";

		for (String module : projectInfo.getModules()) {
			if(module.startsWith(projectInfo.getPortalDir())) {
				relativePath =
					module.substring(projectInfo.getPortalDir().length()+1);
			}
			else {
				relativePath = module;
			}

			String[] moduleSplit = module.split("/");

			moduleName = moduleSplit[moduleSplit.length - 1];

			if(_verifySourceFolder(module)) {
				_createRoots(
					sourceRootsElement, "src." + moduleName + ".dir",
					relativePath);
			}
		}

		Element testRootsElement = _document.createElement("test-roots");

		dataElement.appendChild(testRootsElement);

		for (String test : projectInfo.getTests()) {
			if(test.startsWith(projectInfo.getPortalDir())) {
				relativePath =
					test.substring(projectInfo.getPortalDir().length() + 1);
			}
			else {
				relativePath = test;
			}

			String[] testSplit = test.split("/");

			String testName = testSplit[testSplit.length - 1];

			_createRoots(
				testRootsElement, "test." + testName + ".dir", relativePath);
		}
	}

	private static void _createLibraries(Element configurationElement) {
		Element librariesElement = _document.createElement("libraries");

		librariesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-libraries/1");

		configurationElement.appendChild(librariesElement);

		Element definitionsElement = _document.createElement("definitions");

		definitionsElement.appendChild(
			_document.createTextNode("./lib/nblibraries.properties"));

		librariesElement.appendChild(definitionsElement);
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

	private static void _createRoots(
		Element sourceRootsElement, String module, String moduleName) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", module);

		rootElement.setAttribute("name", moduleName);

		sourceRootsElement.appendChild(rootElement);
	}

	private static String[] _reorderModules(
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

	private static boolean _verifySourceFolder(String module) {
		File folder =
			new File(module + "/src");

		if(folder.exists()) {
			File[] listOfFiles = folder.listFiles();

			if(listOfFiles.length == 0) {
				return false;
			}

			if(listOfFiles.length == 1) {
				String fileName = listOfFiles[0].getName();

				if(fileName.startsWith(".")) {
					return false;
				}
			}
		}

		return true;
	}

	private static class ProjectInfo {
		private ProjectInfo(
			String projectName, String portalDir,String[] modules,
			String[] tests) {

			_projectName = projectName;

			_portalDir = portalDir;

			_modules = modules;

			_tests = tests;
		}

		public String[] getModules() {
			return _modules;
		}

		public String getPortalDir() {
			return _portalDir;
		}

		public String getProjectName() {
			return _projectName;
		}

		public String[] getTests() {
			return _tests;
		}

		private final String[] _modules;
		private final String _portalDir;
		private final String _projectName;
		private final String[] _tests;
	}

	private static Document _document;
}