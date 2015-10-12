package com.liferay.netbeansproject;

import com.liferay.netbeansproject.CommonTasks.ProjectInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
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

	public static void CreateModule(
			String moduleName, String portalDir, String modulePath,
			String moduleList, String projectLibs)
		throws Exception {

		ProjectInfo projectInfo = new ProjectInfo(
			moduleName, portalDir, modulePath, projectLibs.split(","),
			moduleList.split(","));

		CommonTasks.replaceProjectName(projectInfo);

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
			"portal/ModuleProjects/" + projectInfo.getProjectName() +
				"/nbproject/project.xml";

		streamResult = new StreamResult(new File(fileName));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static void _appendImportSharedList(
			Set<String> importSharedSet, ProjectInfo projectInfo,
			String fullPath)
		throws Exception {

		String importShared = ModuleBuildParser.parseIndividualBuildFile(
			fullPath);

		if (!importShared.isEmpty()) {
			Set<String> importSharedList = new HashSet<>();

			importSharedList.addAll(Arrays.asList(importShared.split(":")));

			for (String module : importSharedList) {
				if (!importSharedSet.contains(module)) {
					importSharedSet.add(module);

					String path = ModuleBuildParser.modulePathFinder(
						module, projectInfo.getPortalDir() + "/modules");

					_appendImportSharedList(importSharedSet, projectInfo, path);
				}
			}
		}
	}

	private static void _appendProperties(ProjectInfo projectInfo)
		throws Exception {

		String projectName = projectInfo.getProjectName();
		String fullPath = projectInfo.getFullPath();

		try (
			PrintWriter printWriter =
				new PrintWriter(new BufferedWriter(new FileWriter(
					"portal/ModuleProjects/" + projectName +
						"/nbproject/project.properties", true)))) {

			PropertyLoader propertyLoader = new PropertyLoader();

			Properties buildProperties = propertyLoader.loadPropertyFile(
				"build.properties");

			StringBuilder projectSB = new StringBuilder();

			projectSB.append("excludes=");
			projectSB.append(buildProperties.getProperty("exclude.types"));
			projectSB.append("\n");

			projectSB.append("application.title=");
			projectSB.append(fullPath);
			projectSB.append("\n");

			projectSB.append("dist.jar=${dist.dir}/");
			projectSB.append(projectName);
			projectSB.append(".jar\n");

			projectSB.append("file.reference.");
			projectSB.append(projectName);
			projectSB.append("-src=");
			projectSB.append(fullPath);
			projectSB.append("/src\n");

			projectSB.append("\n");

			projectSB.append("src.dir=${file.reference.");
			projectSB.append(projectName);
			projectSB.append("-src}\n");

			projectSB.append("file.reference.test-unit=");
			projectSB.append(fullPath);
			projectSB.append("/test/unit\n");

			if (new File(fullPath + "/test/unit").exists()) {
				projectSB.append("test.unit.dir=${file.reference.test-unit}\n");
			}
			else {
				projectSB.append("test.unit.dir=\n");
			}

			projectSB.append("file.reference.test-integration=");
			projectSB.append(fullPath);
			projectSB.append("/test/integration\n");

			if (new File(fullPath + "/test/integration").exists()) {
				projectSB.append(
					"test.integration.dir=${file.reference.test-integration}" +
						"\n");
			}
			else {
				projectSB.append("test.integration.dir=\n");
			}

			printWriter.println(projectSB.toString());

			StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");
			StringBuilder testSB = new StringBuilder(
				"javac.test.classpath=\\\n");

			for (String module : projectInfo.getProjectLibs()) {
				if (!module.equals("")) {
					CommonTasks.appendReferenceProperties(
						printWriter, module, javacSB);
				}
			}

			Set<String> importShared = new HashSet<>();
			Set<String> libJarSet = new HashSet<>();

			CommonTasks.appendLibList(
				projectInfo.getProjectName(), libJarSet, javacSB, testSB);

			_appendImportSharedList(
				importShared, projectInfo, projectInfo.getFullPath());

			projectInfo.setImportShared(importShared);

			for (String module : importShared) {
				if (!module.equals("")) {
					CommonTasks.appendReferenceProperties(
						printWriter, module, javacSB);

					File importModule = CommonTasks.modulePathFinder(
						projectInfo.getPortalDir() + "/modules", module);

					for (File importModuleFile : importModule.listFiles()) {

						CommonTasks.appendLibList(
							importModuleFile.getName(), libJarSet, javacSB,
								testSB);
					}
				}
			}

			CommonTasks.appendJavacClasspath(
				new File(projectInfo.getPortalDir() + "/lib/development"),
				javacSB);
			CommonTasks.appendJavacClasspath(
				new File(projectInfo.getPortalDir() + "/lib/global"), javacSB);
			CommonTasks.appendJavacClasspath(
				new File(projectInfo.getPortalDir() + "/lib/portal"), javacSB);

			javacSB.setLength(javacSB.length() - 3);

			if (projectName.equals("portal-impl")) {
				javacSB.append("\nfile.reference.portal-test-internal-src=");
				javacSB.append(projectInfo.getPortalDir());
				javacSB.append("/portal-test-internal/src\n");
				javacSB.append(
					"src.test.dir=${file.reference.portal-test-internal-src}");
			}

			if (projectName.equals("portal-service")) {
				javacSB.append("\nfile.reference.portal-test-src=");
				javacSB.append(projectInfo.getPortalDir());
				javacSB.append("/portal-test/src\n");
				javacSB.append(
					"src.test.dir=${file.reference.portal-test-src}");
			}

			printWriter.println(javacSB.toString());

			testSB.append("\t${build.classes.dir}:\\\n");
			testSB.append("\t${javac.classpath}");

			printWriter.println(testSB.toString());
		}
	}

	private static void _createConfiguration(
			Element projectElement, ProjectInfo projectInfo)
		throws IOException {

		Element configurationElement = _document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(configurationElement, projectInfo);

		CommonTasks.createReferences(
			_document, configurationElement, projectInfo);
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

		CommonTasks.createRoots(
			_document, sourceRootsElement, projectInfo.getFullPath(),
			"src.dir");

		if (projectInfo.getProjectName().equals("portal-impl") ||
			projectInfo.getProjectName().equals("portal-service")) {

			CommonTasks.createRoots(
				_document, sourceRootsElement, "src.test.dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		if (new File(projectInfo.getFullPath() + "/test/unit").exists()) {
			CommonTasks.createRoots(
				_document, testRootsElement, "test.unit.dir");
		}

		if (new File(
				projectInfo.getFullPath() + "/test/integration").exists()) {

			CommonTasks.createRoots(
				_document, testRootsElement, "test.integration.dir");
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

	private static Document _document;

}