package com.liferay.netbeansproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
public class ModuleBuildParser {

	public static String modulePathFinder(String module, String portalDir) {
		File moduleFile = new File(portalDir);

		File[] moduleFileContents = moduleFile.listFiles(_directoryFileFilter);
		String fileName = "";

		for (File moduleFileContent : moduleFileContents) {
			String moduleFileContentName = moduleFileContent.getName();

			if (moduleFileContentName.equals(module)) {
				return moduleFileContent.toString();
			}
			else if(moduleFileContentName.equals("src")) {
				break;
			}
			else {
				fileName = modulePathFinder(
					module, moduleFileContent.toString());
			}

			if (!fileName.equals("")) {
				break;
			}
		}

		return fileName;
	}

	public static String parseIndividualBuildFile(String modulePath)
		throws Exception {

		File moduleFolder = new File(modulePath);

		StringBuilder sb = new StringBuilder();

		Properties buildProperties = new Properties();

		PropertyLoader propertyLoader = new PropertyLoader();

		buildProperties = propertyLoader.loadPropertyFile(
			buildProperties, "build.properties");

		String singleModuleProperty = buildProperties.getProperty(
			"single.module.list");

		List<String> singleModuleList = new ArrayList<>();

		singleModuleList.addAll(Arrays.asList(singleModuleProperty.split(",")));

		File gradleFile = new File(moduleFolder + "/build.gradle");

		if (gradleFile.exists()) {
			try(BufferedReader br =
				new BufferedReader(new FileReader(gradleFile))) {

				String line = br.readLine();

				while (line != null) {
					line = line.trim();

					if (line.startsWith("compile project")) {
						String[] importSharedProject =
							StringUtils.substringsBetween(line, "\"", "\"");

						String[] split = importSharedProject[0].split(":");
						String importSharedProjectName = null;

						if (singleModuleList.contains(split[split.length-1])) {
							importSharedProjectName = split[split.length-1];
						}
						else {
							importSharedProjectName = split[split.length-2];
						}

						sb.append(importSharedProjectName);
						sb.append(":");
					}

					line = br.readLine();
				}
			}
		}

		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}

		return sb.toString();
	}

	public static String parseModuleBuildFile(String modulePath)
		throws Exception {

		File moduleFolder = new File(modulePath);

		File[] subModules = moduleFolder.listFiles(_directoryFileFilter);

		StringBuilder sb = new StringBuilder();

		Properties buildProperties = new Properties();

		PropertyLoader propertyLoader = new PropertyLoader();

		buildProperties = propertyLoader.loadPropertyFile(
			buildProperties, "build.properties");

		String singleModuleProperty = buildProperties.getProperty(
			"single.module.list");

		List<String> singleModuleList = new ArrayList<>();

		singleModuleList.addAll(Arrays.asList(singleModuleProperty.split(",")));

		for (File subModule : subModules) {
			File gradleFile = new File(subModule + "/build.gradle");

			if (gradleFile.exists()) {
				try(BufferedReader br =
					new BufferedReader(new FileReader(gradleFile))) {

					String line = br.readLine();

					while (line != null) {
						line = line.trim();

						if (line.startsWith("compile project")) {
							String[] importSharedProject =
								StringUtils.substringsBetween(line, "\"", "\"");

							String[] split = importSharedProject[0].split(":");
							String importSharedProjectName = null;

							if (singleModuleList.contains(
									split[split.length-1])) {

								importSharedProjectName = split[split.length-1];
							}
							else {
								importSharedProjectName = split[split.length-2];
							}

							sb.append(importSharedProjectName);
							sb.append(":");
						}

						line = br.readLine();
					}
				}
			}
		}

		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}

		return sb.toString();
	}

	private static final FileFilter _directoryFileFilter =
		new DirectoryFileFilter();

}