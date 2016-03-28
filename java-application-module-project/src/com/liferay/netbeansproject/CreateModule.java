package com.liferay.netbeansproject;

import com.liferay.netbeansproject.ModuleBuildParser.ModuleInfo;
import com.liferay.netbeansproject.util.ArgumentsUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

		ProjectInfo projectInfo = new ProjectInfo(
			arguments.get("src.dir.name"), arguments.get("portal.dir"),
			Paths.get(arguments.get("src.dir")),
			StringUtil.split(arguments.get("project.dependencies"), ','),
			StringUtil.split(arguments.get("module.list"), ','));

		Path projectPath = Paths.get(arguments.get("project.dir"));

		Path moduleDir = projectPath.resolve("modules");

		_replaceProjectName(projectInfo, moduleDir);

		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		_appendProperties(
			projectInfo, properties.getProperty("exclude.types"), moduleDir,
			projectPath);

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		_document = documentBuilder.newDocument();

		_createProjectElement(projectInfo, properties);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		Path fileNamePath =
			Paths.get(
				moduleDir.toString(), projectInfo.getProjectName(), "nbproject",
				"project.xml");

		StreamResult streamResult = new StreamResult(fileNamePath.toFile());

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(_document), streamResult);
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
			ProjectInfo projectInfo, String excludeTypes, Path modulePath,
			Path projectPath)
		throws Exception {

		String projectName = projectInfo.getProjectName();

		Path projectPropertiesPath =
			Paths.get(
				modulePath.toString(), projectName,
				"nbproject", "project.properties");
		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				projectPropertiesPath, Charset.defaultCharset(),
				StandardOpenOption.APPEND)) {

			StringBuilder projectSB = new StringBuilder();

			projectSB.append("excludes=");
			projectSB.append(excludeTypes);
			projectSB.append("\n");

			projectSB.append("application.title=");
			projectSB.append(projectInfo.getFullPath());
			projectSB.append("\n");

			projectSB.append("dist.jar=${dist.dir}/");
			projectSB.append(projectName);
			projectSB.append(".jar\n");

			_appendSourcePath(
				projectName, projectInfo.getFullPath(), projectSB);

			projectSB.append("javac.classpath=\\\n");

			for (String module : projectInfo.getProjectLibs()) {
				if (!module.isEmpty()) {
					_appendReferenceProperties(
						bufferedWriter, module, projectSB);
				}
			}

			Path dependenciesDirPath = projectPath.resolve("dependencies");

			Path dependenciesPath = dependenciesDirPath.resolve(projectName);

			if (!Files.exists(dependenciesPath)) {
				Files.write(
					dependenciesPath, Arrays.asList("compile:\ncompileTest:"),
					Charset.defaultCharset());
			}

			Properties dependencyProperties =
				PropertiesUtil.loadProperties(dependenciesPath);

			StringBuilder testSB = new StringBuilder(
				"javac.test.classpath=\\\n");

			testSB.append("\t${build.classes.dir}:\\\n");
			testSB.append("\t${javac.classpath}:\\\n");

			String compileDependencies =
				dependencyProperties.getProperty("compile");

			Set<String> compileSet = new LinkedHashSet<>();

			compileSet.addAll(
				Arrays.asList(
					StringUtil.split(
						compileDependencies, File.pathSeparatorChar)));

			String compileTestDependencies =
				dependencyProperties.getProperty("compileTest");

			if (compileTestDependencies == null) {
				compileTestDependencies = "";
			}

			Set<String> compileTestSet = new LinkedHashSet<>();

			compileTestSet.addAll(
				Arrays.asList(
					StringUtil.split(
						compileTestDependencies, File.pathSeparatorChar)));

			Map<String, ModuleInfo> dependenciesModuleMap =
				_parseModuleDependencies(
					projectInfo, projectInfo.getFullPath());

			for (ModuleInfo moduleInfo : dependenciesModuleMap.values()) {
				String moduleName = moduleInfo.getModuleName();

				if(moduleInfo.isTest()) {
					_appendReferenceProperties(
						bufferedWriter, moduleName, testSB);
				}
				else {
					_appendReferenceProperties(
						bufferedWriter, moduleName, projectSB);
				}

				Path inheritedDependenciesPath = dependenciesDirPath.resolve(
					moduleName);

				Properties moduleDependencyProperties =
					PropertiesUtil.loadProperties(inheritedDependenciesPath);

				compileDependencies =
					moduleDependencyProperties.getProperty("compile");

				compileSet.addAll(
					Arrays.asList(
						StringUtil.split(
							compileDependencies, File.pathSeparatorChar)));

				compileTestDependencies =
					moduleDependencyProperties.getProperty("compileTest");

				compileTestSet.addAll(Arrays.asList(
					StringUtil.split(
						compileTestDependencies, File.pathSeparatorChar)));
			}

			_appendLibJars(compileSet, projectSB);
			_appendLibJars(compileTestSet, testSB);

			projectInfo.setDependenciesModuleMap(dependenciesModuleMap);

			Path developmentPath = Paths.get(
				projectInfo.getPortalDir(),"lib", "development");

			_appendJavacClasspath(developmentPath.toFile(), projectSB);

			Path globalPath = Paths.get(
				projectInfo.getPortalDir(),"lib", "global");

			_appendJavacClasspath(globalPath.toFile(), projectSB);

			Path portalPath = Paths.get(
				projectInfo.getPortalDir(),"lib", "portal");

			_appendJavacClasspath(portalPath.toFile(), projectSB);

			projectSB.setLength(projectSB.length() - 3);

			if (projectName.equals("portal-impl")) {
				projectSB.append("\nfile.reference.portal-test-integration-src=");
				projectSB.append(projectInfo.getPortalDir());
				projectSB.append("/portal-test-integration/src\n");
				projectSB.append(
					"src.test.dir=${file.reference.portal-test-integration-src}");
			}

			if (projectName.equals("portal-kernel")) {
				projectSB.append("\nfile.reference.portal-test-src=");
				projectSB.append(projectInfo.getPortalDir());
				projectSB.append("/portal-test/src\n");
				projectSB.append(
					"src.test.dir=${file.reference.portal-test-src}");
			}

			bufferedWriter.append(projectSB);
			bufferedWriter.newLine();

			testSB.setLength(testSB.length() - 3);

			bufferedWriter.append(testSB);
			bufferedWriter.newLine();
		}
	}

	private static void _appendReferenceProperties(
			BufferedWriter bufferedWriter, String module, StringBuilder javacSB)
		throws IOException {

		StringBuilder sb = new StringBuilder("project.");

		sb.append(module);
		sb.append("=..");
		sb.append(File.separatorChar);
		sb.append(module);
		sb.append("\n");
		sb.append("reference.");
		sb.append(module);
		sb.append(".jar=${project.");
		sb.append(module);
		sb.append("}");
		sb.append(File.separatorChar);
		sb.append("dist");
		sb.append(File.separatorChar);
		sb.append(module);
		sb.append(".jar");

		bufferedWriter.append(sb);
		bufferedWriter.newLine();

		javacSB.append("\t${reference.");
		javacSB.append(module);
		javacSB.append(".jar}:\\\n");
	}

	private static void _appendSourcePath(String moduleName, Path modulePath,
		StringBuilder projectSB) {

		Path moduleSrcPath = modulePath.resolve("src");

		if (Files.exists(modulePath.resolve("docroot"))) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-src=");
			projectSB.append(modulePath);
			projectSB.append(File.separatorChar);
			projectSB.append("docroot");
			projectSB.append(File.separatorChar);
			projectSB.append("WEB-INF");
			projectSB.append(File.separatorChar);
			projectSB.append("src\n");
		}
		else if (Files.exists(moduleSrcPath.resolve("com")) ||
				Files.exists(moduleSrcPath.resolve("main"))) {

			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-src=");
			projectSB.append(modulePath);
			projectSB.append(File.separatorChar);
			projectSB.append("src");


			if(Files.exists(moduleSrcPath.resolve("main"))) {
				projectSB.append(File.separatorChar);
				projectSB.append("main");
				projectSB.append(File.separatorChar);
				projectSB.append("java\n");
			}
			else {
				projectSB.append("\n");
			}

			projectSB.append("src.");
			projectSB.append(moduleName);
			projectSB.append(".dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-src}\n");
		}

		Path mainResourcesPath = Paths.get(
			moduleSrcPath.toString(), "main", "resources");

		if (Files.exists(mainResourcesPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-resources=");
			projectSB.append(mainResourcesPath);
			projectSB.append("\n");
			projectSB.append("src.");
			projectSB.append(moduleName);
			projectSB.append(".resources.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-resources}\n");
		}

		Path testPath = modulePath.resolve("test");
		Path testUnitPath = testPath.resolve("unit");
		Path srcTestPath = moduleSrcPath.resolve("test");
		Path testJavaPath = srcTestPath.resolve("java");

		if (Files.exists(testUnitPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit=");
			projectSB.append(testUnitPath);
			projectSB.append("\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".unit.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit}\n");
		}
		else if(Files.exists(testJavaPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit=");
			projectSB.append(testJavaPath);
			projectSB.append("\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".unit.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit}\n");
		}

		Path testResourcesPath = Paths.get(
			moduleSrcPath.toString(), "test", "Resources");

		if(Files.exists(testResourcesPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit-resources=");
			projectSB.append(testResourcesPath);
			projectSB.append("\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".unit.resources.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-unit-resources}\n");
		}

		Path testIntegrationPath = testPath.resolve("integration");
		Path srcTestIntegrationPath = moduleSrcPath.resolve("testIntegration");
		Path testIntegrationJavaPath = srcTestIntegrationPath.resolve("java");

		if (Files.exists(testIntegrationPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration=");
			projectSB.append(testIntegrationPath);
			projectSB.append("\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".integration.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration}\n");
		}
		else if(Files.exists(testIntegrationJavaPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration=");
			projectSB.append(testIntegrationJavaPath);
			projectSB.append("\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".integration.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration}\n");
		}

		Path testIntegrationResourcesPath = srcTestIntegrationPath.resolve(
			"resources");

		if(Files.exists(testIntegrationResourcesPath)) {
			projectSB.append("file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration-resources=");
			projectSB.append(testIntegrationResourcesPath);
			projectSB.append("\n");
			projectSB.append("test.");
			projectSB.append(moduleName);
			projectSB.append(".integration.resources.dir=${file.reference.");
			projectSB.append(moduleName);
			projectSB.append("-test-integration-resources}\n");
		}
	}

	private static void _createConfiguration(
			Element projectElement, ProjectInfo projectInfo,
			Properties properties)
		throws IOException {

		Element configurationElement = _document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(configurationElement, projectInfo, properties);

		_createReferences(configurationElement, projectInfo);
	}

	private static void _createData(
		Element configurationElement, ProjectInfo projectInfo,
		Properties properties) {

		Element dataElement = _document.createElement("data");

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		configurationElement.appendChild(dataElement);

		Element nameElement = _document.createElement("name");

		Path projectPath = projectInfo.getFullPath();

		Path portalPath = Paths.get(properties.getProperty("portal.dir"));

		Path portalParentPath = portalPath.getParent();

		Path projectNamePath = portalParentPath.relativize(projectPath);

		nameElement.appendChild(
			_document.createTextNode(projectNamePath.toString()));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = _document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		String projectName = projectInfo.getProjectName();

		Path srcPath = projectPath.resolve("src");
		Path mainPath = srcPath.resolve("main");
		Path mainJavaPath = mainPath.resolve("java");

		if (!Files.exists(mainPath) || Files.exists(mainJavaPath)) {

			_createRoots(
				sourceRootsElement, "src", "src." + projectName + ".dir");
		}

		Path mainResourcesPath = mainPath.resolve("resources");

		if (Files.exists(mainResourcesPath)) {
			_createRoots(
				sourceRootsElement, "resources",
				"src." + projectName + ".resources.dir");
		}

		if (projectName.equals("portal-impl")) {
			_createRoots(
				sourceRootsElement,"portal-test-integration","src.test.dir");
		}
		else if (projectName.equals("portal-kernel")) {
			_createRoots(sourceRootsElement,"portal-test", "src.test.dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		Path testPath = projectPath.resolve("test");
		Path testUnitPath = testPath.resolve("unit");
		Path srcTestPath = srcPath.resolve("test");

		if (Files.exists(testUnitPath) || Files.exists(srcTestPath)) {
			_createRoots(
				testRootsElement, "unit" + File.separator + "test",
				"test." + projectName + ".unit.dir");
		}

		Path testResourcesPath = testPath.resolve("resources");

		if (Files.exists(testResourcesPath)) {
			_createRoots(
				sourceRootsElement, "unit" + File.separator + "resources",
				"test." + projectName + ".unit.resources.dir");
		}

		Path testIntegrationPath = testPath.resolve("integration");
		Path srcTestIntegrationPath = srcPath.resolve("testIntegration");

		if (Files.exists(testIntegrationPath) ||
			Files.exists(srcTestIntegrationPath)) {

			_createRoots(
				testRootsElement, "integration" + File.separator + "test",
				"test." + projectName + ".integration.dir");
		}

		Path testIntegrationResources = srcTestIntegrationPath.resolve(
			"resources");

		if (Files.exists(testIntegrationResources)) {
			_createRoots(
				sourceRootsElement,
				"integration" + File.separator + "resources",
				"test." + projectName + ".integration.resources.dir");
		}

		dataElement.appendChild(testRootsElement);
	}

	private static void _createProjectElement(
			ProjectInfo projectInfo, Properties properties)
		throws IOException {

		Element projectElement = _document.createElement("project");

		projectElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/project/1");

		_document.appendChild(projectElement);

		Element typeElement = _document.createElement("type");

		typeElement.appendChild(
			_document.createTextNode("org.netbeans.modules.java.j2seproject"));

		projectElement.appendChild(typeElement);

		_createConfiguration(projectElement, projectInfo, properties);
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
		ProjectInfo projectInfo, Path moduleDir)
		throws IOException {

		String projectName = projectInfo.getProjectName();

		Path projectpath = moduleDir.resolve(projectName);

		Path buildXMLPath = projectpath.resolve("build.xml");

		String content = new String(Files.readAllBytes(buildXMLPath));

		content = StringUtil.replace(content, "%placeholder%",projectName);

		Files.write(
			buildXMLPath, Arrays.asList(content), Charset.defaultCharset());
	}

	private static Document _document;

	private static class ProjectInfo {

		public Path getFullPath() {
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
			String projectName, String portalDir, Path fullPath,
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

		private final Path _fullPath;
		private Map<String, ModuleInfo> _dependenciesModuleMap;
		private final Map<String, Path> _moduleMap;
		private final String _portalDir;
		private final String[] _projectLib;
		private final String _projectName;

	}

}