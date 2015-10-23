package com.liferay.netbeansproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.commons.lang3.StringUtils;
public class ModuleBuildParser {

	public static String parseBuildFile(String modulePath) throws Exception {
		File gradleFile = new File(modulePath + "/build.gradle");

		StringBuilder sb = new StringBuilder();

		if (gradleFile.exists()) {
			try(BufferedReader bufferedReader =
				new BufferedReader(new FileReader(gradleFile))) {

				String line = null;

				while ((line = bufferedReader.readLine()) != null) {
					line = line.trim();

					if (line.startsWith("compile project") ||
						line.startsWith("provided project") ||
						line.startsWith("frontendThemes project") ||
						line.startsWith("testCompile project") ||
						line.startsWith("testIntegrationCompile project")) {

						String[] importSharedProject =
							StringUtils.substringsBetween(line, "\"", "\"");

						String[] split = importSharedProject[0].split(":");

						String importSharedProjectName = split[split.length-1];

						sb.append(importSharedProjectName);

						if(line.startsWith("testCompile project") ||
							line.startsWith("testIntegrationCompile project")) {

							sb.append("-testCompile");

						}
						sb.append(":");
					}
				}
			}
		}

		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}

		return sb.toString();
	}

}