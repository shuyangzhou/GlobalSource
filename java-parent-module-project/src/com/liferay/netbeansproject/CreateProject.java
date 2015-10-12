package com.liferay.netbeansproject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.file.Path;
import java.nio.file.Paths;

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

	public static void CreateProject(
			String projectName, String portalDir, List<String> modules,
			String sources)
		throws Exception {

		ProjectInfo projectInfo = new ProjectInfo(
			projectName, portalDir, modules, sources.split(","));

		_appendList(projectInfo);

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
			new File("portal/nbproject/project.xml"));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static void _appendList(ProjectInfo projectInfo)
		throws IOException {

		try (
			PrintWriter printWriter = new PrintWriter(
				new BufferedWriter(
					new FileWriter(
						"portal/nbproject/project.properties", true)))) {

			StringBuilder sb = new StringBuilder();

			for (String module : projectInfo.getModules()) {
				sb.append("project.");
				sb.append(module);
				sb.append("=ModuleProjects/");
				sb.append(module);
				sb.append("\nreference.");
				sb.append(module);
				sb.append(".jar=${project.");
				sb.append(module);
				sb.append("}/dist/");
				sb.append(module);
				sb.append(".jar\n");
			}

			sb.append("\njavac.classpath=\\\n");

			for (String modulePath : projectInfo.getModules()) {
				sb.append("\t${reference.");
				sb.append(modulePath);
				sb.append(".jar}:\\\n");
			}

			sb.setLength(sb.length() - 3);

			printWriter.println(sb.toString());
		}
	}

	private static void _createConfiguration(
		Element projectElement, ProjectInfo projectInfo) {

		Element configurationElement = _document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(configurationElement, projectInfo);

		_createReferences(configurationElement, projectInfo);
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

		for (String module : projectInfo.getSources()) {
			_createRoots(sourceRootsElement, "src." + module + ".dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		dataElement.appendChild(testRootsElement);
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

	private static void _createReference(
		Element referencesElement, String module) {

		Element referenceElement = _document.createElement("reference");

		referencesElement.appendChild(referenceElement);

		Element foreignProjectElement = _document.createElement(
			"foreign-project");

		foreignProjectElement.appendChild(_document.createTextNode(module));

		referenceElement.appendChild(foreignProjectElement);

		Element artifactTypeElement = _document.createElement("artifact-type");

		artifactTypeElement.appendChild(_document.createTextNode("jar"));

		referenceElement.appendChild(artifactTypeElement);

		Element scriptElement = _document.createElement("script");

		scriptElement.appendChild(_document.createTextNode("build.xml"));

		referenceElement.appendChild(scriptElement);

		Element targetElement = _document.createElement("target");

		targetElement.appendChild(_document.createTextNode("jar"));

		referenceElement.appendChild(targetElement);

		Element cleanTargetElement = _document.createElement("clean-target");

		cleanTargetElement.appendChild(_document.createTextNode("clean"));

		referenceElement.appendChild(cleanTargetElement);

		Element idElement = _document.createElement("id");

		idElement.appendChild(_document.createTextNode("jar"));

		referenceElement.appendChild(idElement);
	}

	private static void _createReferences(
		Element configurationElement, ProjectInfo projectInfo) {

		Element referencesElement = _document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		for (String module : projectInfo.getModules()) {
			Path path = Paths.get(module);

			Path moduleName = path.getFileName();

			_createReference(referencesElement, moduleName.toString());
		}
	}

	private static void _createRoots(
		Element sourceRootsElement, String module) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", module);

		sourceRootsElement.appendChild(rootElement);
	}

	private static Document _document;

	private static class ProjectInfo {

		public List<String> getModules() {
			return _modules;
		}

		public String getPortalDir() {
			return _portalDir;
		}

		public String getProjectName() {
			return _projectName;
		}

		public String[] getSources() {
			return _sources;
		}

		private ProjectInfo(
			String projectName, String portalDir, List<String> modules,
			String[] sources) {

			_projectName = projectName;

			_portalDir = portalDir;

			_modules = modules;

			_sources = sources;
		}

		private final List<String> _modules;
		private final String _portalDir;
		private final String _projectName;
		private final String[] _sources;

	}

}