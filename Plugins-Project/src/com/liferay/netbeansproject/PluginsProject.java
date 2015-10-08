package com.liferay.netbeansproject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.lingala.zip4j.core.ZipFile;

public class PluginsProject {

	public static void main(String[] args) throws Exception {
		Properties buildProperties = _propertyLoader.loadPropertyFile(
			"build.properties");

		File pluginsFile = new File(buildProperties.getProperty("plugins.dir"));

		List<File> docrootSourceList = new ArrayList<>();

		List<File> sourceList = new ArrayList<>();

		if (pluginsFile.exists()) {
			for (File firstLayerFile :
					pluginsFile.listFiles(_directoryFileFilter)) {

				for (File secondLayerFile :
						firstLayerFile.listFiles(_directoryFileFilter)) {

					if (new File(
							secondLayerFile + "/docroot/WEB-INF/src").exists()){

						docrootSourceList.add(secondLayerFile);
					}

					if (new File(secondLayerFile + "/src").exists()) {
						sourceList.add(secondLayerFile);
					}
				}
			}
		}

		_appendProperties(docrootSourceList, sourceList, buildProperties);

		sourceList.addAll(docrootSourceList);

		CreateProject.CreateProject(sourceList);
	}

	private static void _appendProperties(
			List<File> docrootSourceList, List<File>sourceList,
				Properties buildProperties)
		throws Exception {

		ZipFile zipFile = new ZipFile("CleanProject.zip");

		zipFile.extractAll("plugins");

		StringBuilder sb = new StringBuilder();

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

		String projectDependencies = buildProperties.getProperty(
			"project.dependencies");

		for (String project : projectDependencies.split(",")) {
			sb.append("project.");
			sb.append(project);
			sb.append("=");
			sb.append(buildProperties.getProperty("portal.modules.dir"));
			sb.append("/");
			sb.append(project);
			sb.append("\nreference.");
			sb.append(project);
			sb.append(".jar=${project.");
			sb.append(project);
			sb.append("}/dist/");
			sb.append(project);
			sb.append(".jar\n");

			javacSB.append("\t${reference.");
			javacSB.append(project);
			javacSB.append(".jar}:\\\n");
		}

		appendSource(sourceList, javacSB, sb, "");

		appendSource(docrootSourceList, javacSB, sb, "/docroot/WEB-INF");

		appendLib(buildProperties.getProperty("plugins.dir") + "/lib", javacSB);

		File pluginDependencies = new File(
			buildProperties.getProperty("plugins.dir") + "/dependencies");

		if (pluginDependencies.exists()) {
			for (File dependencies :
					pluginDependencies.listFiles(_directoryFileFilter)) {

				appendLib(dependencies + "/lib", javacSB);
			}
		}

		File portalLib = new File(
			buildProperties.getProperty("portal.dir") + "/lib");

		if (portalLib.exists()) {
			for (File dependencies :
					portalLib.listFiles(_directoryFileFilter)) {

				appendLib(dependencies.getAbsolutePath(), javacSB);
			}
		}

		try (
			PrintWriter printWriter =
				new PrintWriter(new BufferedWriter(new FileWriter(
					"plugins/nbproject/project.properties", true)))) {

			printWriter.append(sb.toString());

			javacSB.setLength(javacSB.length() - 3);

			printWriter.append(javacSB.toString());
		}
	}

	private static void appendJar(File jar, StringBuilder javacSB) {
		javacSB.append("\t");
		javacSB.append(jar);
		javacSB.append(":\\\n");
	}

	private static void appendLib(String fileName, StringBuilder javacSB) {
		File pluginLib = new File(fileName);

		if (pluginLib.exists()) {
			for (File jar : pluginLib.listFiles(_jarFileFilter)) {
				appendJar(jar, javacSB);
			}
		}
	}

	private static void appendSource(
		List<File> sourceList, StringBuilder javacSB, StringBuilder sb,
			String path) {

		for (File source : sourceList) {
			String sourceName = source.getName();

			sb.append("src.");
			sb.append(sourceName);
			sb.append(".dir=");
			sb.append(source);
			sb.append(path);
			sb.append("/src\n");

			File libFolder = new File(source + path + "/lib");

			if (libFolder.exists()) {
				File[] libJars = libFolder.listFiles(_jarFileFilter);

				for (File jar : libJars) {
					appendJar(jar, javacSB);
				}
			}
		}
	}

	private static final FileFilter _directoryFileFilter =
		new DirectoryFileFilter();
	private static final FileFilter _jarFileFilter = new JarFileFilter();
	private static final PropertyLoader _propertyLoader = new PropertyLoader();

}