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
			try(BufferedReader br =
				new BufferedReader(new FileReader(gradleFile))) {

				String line = br.readLine();

				while (line != null) {
					line = line.trim();

					if (line.startsWith("compile project")) {
						String[] importSharedProject =
							StringUtils.substringsBetween(line, "\"", "\"");

						String[] split = importSharedProject[0].split(":");

						String importSharedProjectName = split[split.length-1];

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

}