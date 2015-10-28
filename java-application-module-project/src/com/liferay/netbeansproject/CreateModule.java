package com.liferay.netbeansproject;

import com.liferay.netbeansproject.ModuleBuildParser.ModuleInfo;
import com.liferay.netbeansproject.util.ArgumentsUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import java.util.Queue;
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
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		String portalDir = properties.getProperty("portal.dir");

		ProjectInfo projectInfo = new ProjectInfo(
			arguments.get("src.dir.name"), portalDir, arguments.get("src.dir"),
			StringUtil.split(arguments.get("project.dependencies"), ','),
			StringUtil.split(arguments.get("module.list"), ','));

		String moduleDir = properties.getProperty("project.dir") + "/modules";

		_replaceProjectName(projectInfo, moduleDir);

		_appendProperties(projectInfo, properties, moduleDir);

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
			moduleDir + "/" + projectInfo.getProjectName() +
				"/nbproject/project.xml";

		streamResult = new StreamResult(new File(fileName));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	private static Map<String, ModuleInfo> _parseModuleDependencies(
			ProjectInfo projectInfo, Path modulePath)
		throws Exception {

		Map<String, ModuleInfo> dependenciesModuleMap = new HashMap<>();

		Map<String, Path> moduleMap = projectInfo.getModuleMap();

		Queue<ModuleInfo> moduleInfoQueue = new LinkedList<>();

		moduleInfoQueue.addAll(ModuleBuildParser.parseBuildFile(modulePath));

		ModuleInfo moduleInfo = null;

		while ((moduleInfo = moduleInfoQueue.poll()) != null) {
			String moduleName = moduleInfo.getModuleName();

			if (!moduleMap.containsKey(moduleName)) {
				continue;
			}

			if (dependenciesModuleMap.put(moduleName, moduleInfo) == null) {
				moduleInfoQueue.addAll(
					ModuleBuildParser.parseBuildFile(
						moduleMap.get(moduleName)));
			}
		}

		return dependenciesModuleMap;
	}

	private static void _appendJavacClasspath(
		File directory, StringBuilder sb) {

		for (File jar : directory.listFiles()) {
			sb.append("\t");
			sb.append(jar.getAbsolutePath());
			sb.append(":\\\n");
		}
	}

	private static void _appendLibFolders(
		File libFolder, StringBuilder javacSB, StringBuilder testSB) {

		for (File jar : libFolder.listFiles()) {
			String jarName = jar.getName();

			if (jarName.endsWith(".jar")) {
				javacSB.append("\t");
				javacSB.append(jar.getAbsolutePath());
				javacSB.append(":\\\n");
			}

			if (jarName.equals("test")) {
				for (File testJar : jar.listFiles()) {
					testSB.append("\t");
					testSB.append(testJar.getAbsolutePath());
					testSB.append(":\\\n");
				}
			}
		}
	}

	private static void _appendLibJars(
		Set<String> dependencies, StringBuilder sb) {

		for (String jar : dependencies) {
			if (!jar.isEmpty()) {
				sb.append("\t");
				sb.append(jar);
				sb.append(":\\\n");
			}
		}
	}

	private static void _appendProperties(
		ProjectInfo projectInfo, Properties properties, String moduleDir)
		throws Exception {

		try (
			PrintWriter printWriter = new PrintWriter(
				new BufferedWriter(
					new FileWriter(
						moduleDir + "/" + projectInfo.getProjectName() +
							"/nbproject/project.properties",
						true)))) {

			StringBuilder projectSB = new StringBuilder();

			projectSB.append("excludes=");
			projectSB.append(properties.getProperty("exclude.types"));
			projectSB.append("\n");

			projectSB.append("application.title=");
			projectSB.append(projectInfo.getFullPath());
			projectSB.append("\n");

			projectSB.append("dist.jar=${dist.dir}/");
			projectSB.append(projectInfo.getProjectName());
			projectSB.append(".jar\n");

			_appendSourcePath(projectInfo.getProjectName(),
				projectInfo.getFullPath(), projectSB);

			projectSB.append("javac.classpath=\\\n");

			for (String module : projectInfo.getProjectLibs()) {
				if (!module.equals("")) {
					_appendReferenceProperties(printWriter, module, projectSB);
				}
			}

			File libFolder = new File(
				moduleDir + "/" + projectInfo.getProjectName() + "/lib");

			Properties dependencyProperties =
				PropertiesUtil.loadProperties(
					Paths.get(
						moduleDir + "/" + projectInfo.getProjectName() +
							"/GradleDependency.properties"));

			StringBuilder testSB = new StringBuilder(
				"javac.test.classpath=\\\n");

			String compileDependencies =
				dependencyProperties.getProperty("compile");

			Set<String> compileSet = new HashSet<>();

			compileSet.addAll(
				Arrays.asList(compileDependencies.split(File.pathSeparator)));

			String compileTestDependencies =
				dependencyProperties.getProperty("compileTest");

			Set<String> compileTestSet = new HashSet<>();

			compileTestSet.addAll(Arrays.asList(
				compileTestDependencies.split(File.pathSeparator)));

			if (libFolder.exists()) {
				_appendLibFolders(libFolder, projectSB, testSB);
			}

			Map<String, ModuleInfo> dependenciesModuleMap =
				_parseModuleDependencies(
					projectInfo, Paths.get(projectInfo.getFullPath()));

			for (ModuleInfo moduleInfo : dependenciesModuleMap.values()) {
				String moduleName = moduleInfo.getModuleName();

				if(moduleInfo.isTest()) {
					_appendReferenceProperties(
						printWriter, moduleName, testSB);
				}
				else {
					_appendReferenceProperties(
						printWriter, moduleName, projectSB);
				}

				Properties moduleDependencyProperties =
					PropertiesUtil.loadProperties(
						Paths.get(
							moduleDir + "/" + moduleName +
								"/GradleDependency.properties"));

				compileDependencies =
					moduleDependencyProperties.getProperty("compile");

				compileSet.addAll(Arrays.asList(
					compileDependencies.split(File.pathSeparator)));

				compileTestDependencies =
					moduleDependencyProperties.getProperty("compileTest");

				compileTestSet.addAll(Arrays.asList(
					compileTestDependencies.split(File.pathSeparator)));
			}

			_appendLibJars(compileSet, projectSB);
			_appendLibJars(compileTestSet, testSB);

			projectInfo.setDependenciesModuleMap(dependenciesModuleMap);

			_appendJavacClasspath(
				new File(projectInfo.getPortalDir() + "/lib/development"),
				projectSB);
			_appendJavacClasspath(
				new File(projectInfo.getPortalDir() + "/lib/global"),
				projectSB);
			_appendJavacClasspath(
				new File(projectInfo.getPortalDir() + "/lib/portal"),
				projectSB);

			projectSB.setLength(projectSB.length() - 3);

			if (projectInfo.getProjectName().equals("portal-impl")) {
				projectSB.append("\nfile.reference.portal-test-internal-src=");
				projectSB.append(projectInfo.getPortalDir());
				projectSB.append("/portal-test-internal/src\n");
				projectSB.append(
					"src.test.dir=${file.reference.portal-test-internal-src}");
			}

			if (projectInfo.getProjectName().equals("portal-service")) {
				projectSB.append("\nfile.reference.portal-test-src=");
				projectSB.append(projectInfo.getPortalDir());
				projectSB.append("/portal-test/src\n");
				projectSB.append(
					"src.test.dir=${file.reference.portal-test-src}");
			}

			printWriter.println(projectSB.toString());

			testSB.append("\t${build.classes.dir}:\\\n");
			testSB.append("\t${javac.classpath}");

			printWriter.println(testSB.toString());
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

	private static void _appendSourcePath(String moduleName, String modulePath,
		StringBuilder projectSB) {

		if (new File(modulePath + "/docroot").exists()) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-src=");
			projectSB.append(modulePath);
			projectSB.append("/docroot/WEB-INF/src\n");
		}
		else if (new File(modulePath + "/src").exists()) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-src=");
			projectSB.append(modulePath);
			projectSB.append("/src");

			if(new File(modulePath + "/src/main").exists()) {
				projectSB.append("/main/java\n");
			}
			else {
				projectSB.append("\n");
			}
		}

		projectSB.append("src.");
		projectSB.append(moduleName);
		projectSB.append(".dir=${file.reference.");
		projectSB.append(moduleName);
		projectSB.append("-src}\n");

		if (new File(modulePath + "/src/main/resources").exists()) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-resources=");
			projectSB.append(modulePath);
			projectSB.append("/src/main/resources\n");
			projectSB.append("src.");
			projectSB.append(moduleName);
			projectSB.append(".resources.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-resources}\n");
		}

		if (new File(modulePath + "/test/unit").exists()) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit=");
			projectSB.append(modulePath);
			projectSB.append("/test/unit\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".unit.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit}\n");
		}
		else if(new File(modulePath + "/src/test/java").exists()) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit=");
			projectSB.append(modulePath);
			projectSB.append("/src/test/java\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".unit.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit}\n");
		}

		if(new File(modulePath + "/src/test/resources").exists()) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit-resources=");
			projectSB.append(modulePath);
			projectSB.append("/src/test/resources\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".unit.resources.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit-resources}\n");
		}

		if (new File(modulePath + "/test/integration").exists()) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration=");
			projectSB.append(modulePath);
			projectSB.append("/test/integration\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".integration.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration}\n");
		}
		else if(new File(modulePath + "/src/testIntegration/java").exists()) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration=");
			projectSB.append(modulePath);
			projectSB.append("/src/testIntegration/java\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".integration.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration}\n");
		}

		if(new File(modulePath + "/src/testIntegration/resources").exists()) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration-resources=");
			projectSB.append(modulePath);
			projectSB.append("/src/testIntegration/resources\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".integration.resources.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration-resources}\n");
		}
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

		String projectPath = projectInfo.getFullPath();

		if (!(new File(projectPath + "/src/main").exists()) ||
			new File(projectPath + "/src/main/java").exists()) {

			_createRoots(sourceRootsElement, projectPath + "/java",
				"src." + projectInfo.getProjectName() + ".dir");
		}

		if (new File(projectPath + "/src/main/resources").exists()) {
			_createRoots(sourceRootsElement, projectPath + "/resources",
				"src." + projectInfo.getProjectName() + ".resources.dir");
		}

		if (projectInfo.getProjectName().equals("portal-impl") ||
			projectInfo.getProjectName().equals("portal-service")) {

			_createRoots(sourceRootsElement, "src.test.dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		if (new File(projectPath + "/test/unit").exists() ||
			new File(projectPath + "/src/test").exists()) {
			_createRoots(
				testRootsElement, projectPath + "/unit/test",
				"test." + projectInfo.getProjectName() + ".unit.dir");
		}

		if (new File(projectPath + "/src/test/resources").exists()) {
			_createRoots(sourceRootsElement, projectPath + "/unit/resources",
				"test." + projectInfo.getProjectName() + ".unit.resources.dir");
		}

		if (new File(projectPath + "/test/integration").exists() ||
			new File(projectPath + "/src/testIntegration").exists()) {

			_createRoots(
				testRootsElement, projectPath + "/integration/test",
				"test." + projectInfo.getProjectName() + ".integration.dir");
		}

		if (new File(projectPath + "/src/testIntegration/resources").exists()) {
			_createRoots(sourceRootsElement,
				projectPath + "/integration/resources",
				"test." + projectInfo.getProjectName() +
					".integration.resources.dir");
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

		Map<String, ModuleInfo> dependenciesModuleMap =
			projectInfo.getDependenciesModuleMap();

		for (String moduleName : dependenciesModuleMap.keySet()) {
			_createReference(referencesElement, moduleName);
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

	private static void _replaceProjectName(
		ProjectInfo projectInfo, String moduleDir)
		throws IOException {

		File file =
			new File(
				moduleDir + "/" + projectInfo.getProjectName() +
					"/build.xml");

		String content = new String(Files.readAllBytes(file.toPath()));

		content = StringUtil.replace(
			content, "%placeholder%",projectInfo.getProjectName());

		Files.write(file.toPath(), content.getBytes());
	}

	private static Document _document;

	private static class ProjectInfo {

		public String getFullPath() {
			return _fullPath;
		}

		public Map<String, ModuleInfo> getDependenciesModuleMap() {
			return _dependenciesModuleMap;
		}

		public Map<String, Path> getModuleMap() {
			return _moduleMap;
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

		public void setDependenciesModuleMap(
			Map<String, ModuleInfo> dependenciesModuleMap) {

			_dependenciesModuleMap = dependenciesModuleMap;
		}

		private ProjectInfo(
			String projectName, String portalDir, String fullPath,
			String[] projectLibs, String[] moduleList) {

			_projectName = projectName;

			_portalDir = portalDir;

			_fullPath = fullPath;

			_projectLib = projectLibs;

			_moduleMap = new HashMap<>();

			for (String module : moduleList) {
				Path modulePath = Paths.get(module);

				Path namePath = modulePath.getFileName();

				_moduleMap.put(namePath.toString(), modulePath);
			}
		}

		private final String _fullPath;
		private Map<String, ModuleInfo> _dependenciesModuleMap;
		private final Map<String, Path> _moduleMap;
		private final String _portalDir;
		private final String[] _projectLib;
		private final String _projectName;

	}

}