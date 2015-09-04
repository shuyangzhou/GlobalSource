package com.liferay.netbeansproject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class CreateModule {

	public static void main(String[] args) throws Exception {
		if (args.length != 6) {
			throw new IllegalArgumentException("Incorrect Number of arguments");
		}

		ProjectInfo projectInfo = new ProjectInfo(
			args[0], args[1], args[2], _reorderModules(args[3], args[1]),
			_reorderModules(args[4], args[1]), args[5].split(","));

		_replaceProjectName(projectInfo);

		_appendProperties(projectInfo);

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

		String fileName =
			"portal/modules/" + projectInfo.getProjectName() +
				"/nbproject/project.xml";

		streamResult = new StreamResult(new File(fileName));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static void _appendImportSharedList(
			Set<String> importShared, ProjectInfo projectInfo, Set ivyJars,
			String fullPath)
		throws Exception {

		String importSharedList = ModuleBuildParser.parseBuildFile(fullPath);

		if (!importSharedList.isEmpty()) {
			for (String module : importSharedList.split(":")) {
				Map<String, String> moduleNameMap =
					projectInfo.getModuleNameMap();

				if (moduleNameMap.containsKey(module)) {
					importShared.add(module);

					_appendImportSharedList(
						importShared, projectInfo, ivyJars,
						moduleNameMap.get(module));

					String ivyListString = IvyReportParser.parseIvyReport(
						module);

					ivyJars.addAll(Arrays.asList(ivyListString.split(":")));
				}
			}
		}
	}

	private static void _appendJavacClasspath(
		File directory, StringBuilder sb) {

		for (File jar : directory.listFiles()) {
			sb.append("\t");
			sb.append(jar.getAbsolutePath());
			sb.append(":\\\n");
		}
	}

	private static void _appendProperties(ProjectInfo projectInfo)
		throws Exception {

		try (
			PrintWriter printWriter = new PrintWriter(
				new BufferedWriter(
					new FileWriter(
						"portal/modules/" + projectInfo.getProjectName() +
							"/nbproject/project.properties",
						true)))) {

			StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

			for (String module : projectInfo.getProjectLibs()) {
				if (!module.equals("")) {
					_appendReferenceProperties(printWriter, module, javacSB);
				}
			}

			Set<String> importShared = new HashSet<>();
			Set<String> ivyJars = new HashSet<>();

			_appendImportSharedList(
				importShared, projectInfo, ivyJars, projectInfo.getFullPath());

			projectInfo.setImportShared(importShared);

			for (String module : importShared) {
				if (!module.equals("")) {
					_appendReferenceProperties(printWriter, module, javacSB);
				}
			}

			String ivyListString = IvyReportParser.parseIvyReport(
				projectInfo.getProjectName());

			ivyJars.addAll(Arrays.asList(ivyListString.split(":")));

			for (String jar : ivyJars) {
				if (!jar.equals("")) {
					javacSB.append("\t");
					javacSB.append(jar);
					javacSB.append(":\\\n");
				}
			}

			_appendJavacClasspath(
				new File(projectInfo.getPortalDir() + "/lib/development"),
				javacSB);
			_appendJavacClasspath(
				new File(projectInfo.getPortalDir() + "/lib/global"), javacSB);
			_appendJavacClasspath(
				new File(projectInfo.getPortalDir() + "/lib/portal"), javacSB);

			javacSB.setLength(javacSB.length() - 3);

			if (projectInfo.getProjectName().equals("portal-impl")) {
				javacSB.append("\nfile.reference.portal-test-internal-src=");
				javacSB.append(projectInfo.getPortalDir());
				javacSB.append("/portal-test-internal/src\n");
				javacSB.append(
					"src.test.dir=${file.reference.portal-test-internal-src}");
			}

			if (projectInfo.getProjectName().equals("portal-service")) {
				javacSB.append("\nfile.reference.portal-test-src=");
				javacSB.append(projectInfo.getPortalDir());
				javacSB.append("/portal-test/src\n");
				javacSB.append(
					"src.test.dir=${file.reference.portal-test-src}");
			}

			printWriter.println(javacSB.toString());
		}
	}

	private static void _appendReferenceProperties(
		PrintWriter printWriter, String module, StringBuilder javacSB) {

		StringBuilder sb = new StringBuilder("project.");

		sb.append(module);
		sb.append("=../");
		sb.append(module);
		sb.append("\n");
		sb.append("reference.");
		sb.append(module);
		sb.append(".jar=${project.");
		sb.append(module);
		sb.append("}/dist/");
		sb.append(module);
		sb.append(".jar");

		printWriter.println(sb.toString());

		javacSB.append("\t${reference.");
		javacSB.append(module);
		javacSB.append(".jar}:\\\n");
	}

	private static void _createConfiguration(
			Element projectElement, ProjectInfo projectInfo)
		throws IOException {

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
			_document.createTextNode(projectInfo.getFullPath()));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = _document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		_createRoots(sourceRootsElement, projectInfo.getFullPath(), "src.dir");

		if (
				projectInfo.getProjectName().equals("portal-impl") ||
			projectInfo.getProjectName().equals("portal-service")) {

			_createRoots(sourceRootsElement, "src.test.dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		if (new File(projectInfo.getFullPath() + "/test/unit").exists()) {
			_createRoots(testRootsElement, "test.unit.dir");
		}

		if (new File(
				projectInfo.getFullPath() + "/test/integration").exists()) {

			_createRoots(testRootsElement, "test.integration.dir");
		}

		dataElement.appendChild(testRootsElement);
	}

	private static void _createProjectElement(ProjectInfo projectInfo)
		throws IOException {

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
			Element referencesElement, String module)
		throws IOException {

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
			Element configurationElement, ProjectInfo projectInfo)
		throws IOException {

		Element referencesElement = _document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		for (String module : projectInfo.getImportShared()) {
			if (!module.equals("")) {
				_createReference(referencesElement, module);
			}
		}

		for (String module : projectInfo.getProjectLibs()) {
			if (!module.equals("")) {
				_createReference(referencesElement, module);
			}
		}
	}

	private static void _createRoots(
		Element sourceRootsElement, String rootId) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", rootId);

		sourceRootsElement.appendChild(rootElement);
	}

	private static void _createRoots(
		Element sourceRootsElement, String label, String rootId) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", rootId);

		rootElement.setAttribute("name", label);

		sourceRootsElement.appendChild(rootElement);
	}

	private static String[] _reorderModules(
		String originalOrder, String portalDir) {

		String[] modules = originalOrder.split(",");

		int i = 0;

		List<String> moduleSourceList = new ArrayList<>();

		while (modules[i].startsWith(portalDir + "/modules")) {
			moduleSourceList.add(modules[i]);

			i++;
		}

		List<String> portalSourceList = new ArrayList<>();

		while (i < modules.length) {
			portalSourceList.add(modules[i]);

			i++;
		}

		Collections.sort(portalSourceList);

		Collections.sort(moduleSourceList);

		portalSourceList.addAll(moduleSourceList);

		return portalSourceList.toArray(new String[portalSourceList.size()]);
	}

	private static void _replaceProjectName(ProjectInfo projectInfo)
		throws IOException {

		File file =
			new File(
				"portal/modules/" + projectInfo.getProjectName() +
					"/build.xml");

		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line = "";
		String originalFileContent = "";

		while ((line = reader.readLine()) != null) {
			originalFileContent += line + "\r\n";
		}

		reader.close();

		String newFileContent = originalFileContent.replaceAll(
			"%placeholder%", projectInfo.getProjectName());

		FileWriter writer = new FileWriter(file);

		writer.write(newFileContent);

		writer.close();
	}

	private static Document _document;

	private static class ProjectInfo {

		public String getFullPath() {
			return _fullPath;
		}

		public Set<String> getImportShared() {
			return _importShared;
		}

		public String[] getJarLibs() {
			return _jarLib;
		}

		public String[] getModuleList() {
			return _moduleList;
		}

		public Map getModuleNameMap() {
			return _moduleNameMap;
		}

		public String getPortalDir() {
			return _portalDir;
		}

		public String[] getProjectLibs() {
			return _projectLib;
		}

		public String getProjectName() {
			return _projectName;
		}

		public void setImportShared(Set<String> importShared) {
			_importShared = importShared;
		}

		private ProjectInfo(
			String projectName, String portalDir, String fullPath,
			String[] projectLibs, String[] jarLibs, String[] moduleList) {

			_projectName = projectName;

			_portalDir = portalDir;

			_fullPath = fullPath;

			_projectLib = projectLibs;

			_jarLib = jarLibs;

			_moduleList = moduleList;

			_moduleNameMap = new HashMap();

			for (String module : moduleList) {
				Path path = Paths.get(module);

				Path namePath = path.getFileName();

				String nameString = namePath.toString();

				_moduleNameMap.put(nameString, module);
			}
		}

		private final String _fullPath;
		private Set<String> _importShared;
		private final String[] _jarLib;
		private final String[] _moduleList;
		private final Map _moduleNameMap;
		private final String _portalDir;
		private final String[] _projectLib;
		private final String _projectName;

	}

}