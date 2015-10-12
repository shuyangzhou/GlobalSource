package com.liferay.netbeansproject;

import java.io.File;
import java.io.FileFilter;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import net.lingala.zip4j.core.ZipFile;
public class CreateProjectTree {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException("Incorrect Number of arguments");
		}

		Properties buildProperties =
			_propertyLoader.loadPropertyFile("build.properties");

		File portalDir = new File(args[0]);

		resolveIndividualModuleFolder(
			"modules/core/registry-api", args[1],
				args[0] + "/modules/core/registry-api", args[0]);

		String excludedModules = buildProperties.getProperty(
			"excluded.modules");

		List<String> excludedList =
			new ArrayList<>(Arrays.asList(excludedModules.split(",")));

		if (portalDir.exists()) {

			for (File folderFile : portalDir.listFiles(_directoryFileFilter)) {
				String folderFileName = folderFile.getName();

				if(new File(folderFile + "/src").exists()) {
					if (!excludedList.contains(folderFileName)) {
						resolveIndividualModuleFolder(
							folderFileName, args[1],
								folderFile.getAbsolutePath(), args[0]);
					}
				}

				if (folderFileName.equals("modules")) {
					_checkModuleDir(folderFile, args[1], "", args[0]);
				}
			}

			File moduleListFile = new File("portal/ModuleProjects");

			List<String> moduleList = new ArrayList<>();

			for (File module : moduleListFile.listFiles()) {
				moduleList.add(module.getName());
			}

			CreateProject.CreateProject(
				"portal", args[0], moduleList,
					buildProperties.getProperty("umbrella.source.list"));
		}
	}

	public static void resolveIndividualModuleFolder(
			String dependencyName, String moduleList, String moduleName,
			String portalDir)
		throws Exception {

		Path path = Paths.get(moduleName);

		Path fileName = path.getFileName();

		String name = fileName.toString();

		File localFolder = new File("portal/ModuleProjects/" + name);

		localFolder.mkdir();

		ZipFile zipFile = new ZipFile("portal/CleanModule.zip");

		zipFile.extractAll("portal/ModuleProjects/" + name);

		CreateModule.CreateModule(
			name, portalDir, moduleName, moduleList,
			_getProjectDependencies(dependencyName));
	}

	private static void _checkModuleDir(
			File folder, String moduleList, String moduleName, String portalDir)
		throws Exception {

		boolean isModule = false;

		String folderName = folder.getName();

		for (File folderContent : folder.listFiles(_directoryFileFilter)) {
			if(new File(folderContent + "/build.gradle").exists()) {
				isModule = true;
			}

			if (!moduleName.endsWith(folderName)) {
				moduleName = moduleName + "/" + folderName;
			}

			if(new File(portalDir + "/" + moduleName).exists()) {
				_checkModuleDir(
					folderContent, moduleList, moduleName, portalDir);
			}
		}

		if (isModule) {
			if (!folderName.startsWith("gradle-plugins")) {
				_resolveModuleLevelFolder(
					folderName, moduleList, moduleName, portalDir);
			}
		}
	}

	private static String _getProjectDependencies(String moduleName)
		throws Exception {

		Properties projectDependencyProperties =
			_propertyLoader.loadPropertyFile("project-dependency.properties");

		if (projectDependencyProperties.containsKey(moduleName)) {
			return projectDependencyProperties.getProperty(moduleName);
		}

		return "";
	}

	private static void _resolveModuleLevelFolder(
			String groupName, String moduleList, String moduleName,
			String portalDir)
		throws Exception {

		File groupFolder = new File("portal/ModuleProjects/" + groupName);

		groupFolder.mkdir();

		ZipFile zipFile = new ZipFile("portal/CleanModule.zip");

		zipFile.extractAll("portal/ModuleProjects/" + groupName);

		String modulePath = portalDir + moduleName;

		CreateModulesModule.CreateModule(
			groupName, portalDir, modulePath, moduleList,
			_getProjectDependencies("portal.module.dependencies"));
	}

	private static final FileFilter _directoryFileFilter =
		new DirectoryFileFilter();

	private static final PropertyLoader _propertyLoader = new PropertyLoader();

}