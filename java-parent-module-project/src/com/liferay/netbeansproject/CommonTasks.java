package com.liferay.netbeansproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
public class CommonTasks {

	public static void appendJavacClasspath(File directory, StringBuilder sb) {
		for (File jar : directory.listFiles()) {
			sb.append("\t");
			sb.append(jar.getAbsolutePath());
			sb.append(":\\\n");
		}
	}

	public static void appendLibList(
		String moduleFileName, Set<String> libJarSet, StringBuilder javacSB,
		StringBuilder testSB) {

		File libFolder = new File("portal/modules/" + moduleFileName + "/lib");

		if (libFolder.exists()) {

			for (File jar : libFolder.listFiles()) {
				String jarName = jar.getName();

				if (jarName.endsWith(".jar")) {
					if (!libJarSet.contains(jarName)) {
						javacSB.append("\t../../modules/");
						javacSB.append(moduleFileName);
						javacSB.append("/lib/");
						javacSB.append(jarName);
						javacSB.append(":\\\n");
						libJarSet.add(jarName);
					}
				}

				if (jarName.equals("test")) {
					for (File testJar : jar.listFiles()) {
						testSB.append("\t../../modules/");
						testSB.append(moduleFileName);
						testSB.append("/lib/test/");
						testSB.append(testJar.getName());
						testSB.append(":\\\n");
					}
				}
			}
		}
	}

	public static void appendReferenceProperties(
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

	public static void createReferences(
		Document _document, Element configurationElement,
			ProjectInfo projectInfo)
		throws IOException {

		Element referencesElement = _document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		for (String module : projectInfo.getImportShared()) {
			if (!module.equals("")) {
				_createReference(_document, referencesElement, module);
			}
		}

		for (String module : projectInfo.getProjectLibs()) {
			if (!module.equals("")) {
				_createReference(_document, referencesElement, module);
			}
		}
	}

	public static void createRoots(
		Document _document, Element sourceRootsElement, String rootId) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", rootId);

		sourceRootsElement.appendChild(rootElement);
	}

	public static void createRoots(
		Document _document, Element sourceRootsElement, String label,
			String rootId) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", rootId);

		rootElement.setAttribute("name", label);

		sourceRootsElement.appendChild(rootElement);
	}

	public static void replaceProjectName(ProjectInfo projectInfo)
		throws IOException {

		File file =
			new File(
				"portal/ModuleProjects/" + projectInfo.getProjectName() +
					"/build.xml");

		String originalFileContent = "";

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line = "";

			while ((line = reader.readLine()) != null) {
				originalFileContent += line + "\r\n";
			}
		}

		String newFileContent = originalFileContent.replaceAll(
			"%placeholder%", projectInfo.getProjectName());

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(newFileContent);
		}
	}

	public static class ProjectInfo {

		public ProjectInfo(
			String projectName, String portalDir, String fullPath,
			String[] projectLibs, String[] moduleList) {

			_projectName = projectName;

			_portalDir = portalDir;

			_fullPath = fullPath;

			_projectLib = projectLibs;

			_moduleList = moduleList;
		}

		public String getFullPath() {
			return _fullPath;
		}

		public Set<String> getImportShared() {
			return _importShared;
		}

		public String[] getModuleList() {
			return _moduleList;
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

		private final String _fullPath;
		private Set<String> _importShared;
		private final String[] _moduleList;
		private final String _portalDir;
		private final String[] _projectLib;
		private final String _projectName;

	}

	private static void _createReference(
		Document _document, Element referencesElement, String module)
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

	public static File modulePathFinder(String currentPath, String module) {
		File currentFile = new File(currentPath);

		for (File childFile : currentFile.listFiles(_directoryFileFilter)) {
			String childFileName = childFile.getName();

			if (childFileName.equals(module)) {
				return childFile;
			}

			List<String> keywords =
				Arrays.asList(
					"build", "classes", "lib", "src", "tmp", "test",
						"test-classes");

			if(!keywords.contains(childFileName)) {
				File returnFile = modulePathFinder(
					childFile.getAbsolutePath(), module);

				if (returnFile != null) {
					return returnFile;
				}
			}
		}

		return null;
	}

private static final FileFilter _directoryFileFilter =
		new DirectoryFileFilter();
}