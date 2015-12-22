package com.liferay.netbeansproject;

import com.liferay.netbeansproject.util.StringUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author tom
 */
public class ProcessGradle {
	public static void main(String[] args) throws IOException {
		Path portalDirPath = Paths.get(args[0]);
		Path projectFilePath = Paths.get(args[1]);

		Path gradlewPath = portalDirPath.resolve("gradlew");

		List<String> gradleTask = new ArrayList<>();

		gradleTask.add(gradlewPath.toString());
		gradleTask.add("--init-script=dependency.gradle");
		gradleTask.add("printDependencies");
		gradleTask.add("-p");

		Path moduleDirPath = portalDirPath.resolve("modules");

		gradleTask.add(moduleDirPath.toString());

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Process process = processBuilder.start();

		BufferedReader br =
			new BufferedReader(new InputStreamReader(process.getInputStream()));

		Path moduleFolderPath = projectFilePath.resolve("modules");

		Path dependencyFile = moduleFolderPath;

		StringBuilder sb = new StringBuilder();

		String line;

		while ((line = br.readLine()) != null) {
			if (line.startsWith("configuration")) {
				if (!dependencyFile.equals(moduleFolderPath)) {
					if(sb.length() > 8) {
						sb.setLength(sb.length() - 1);
					}

					Files.write(
						dependencyFile, Arrays.asList(sb.toString()),
						StandardOpenOption.APPEND);

					sb.setLength(0);
				}

				line = StringUtil.extract(line, '\'');

				String[] dependencyConfigSplit = StringUtil.split(line, ':');

				int splitLength = dependencyConfigSplit.length - 1;

				Path modulePath = moduleFolderPath.resolve(
					dependencyConfigSplit[splitLength - 1]);

				dependencyFile = modulePath.resolve("dependency.properties");

				if (Files.notExists(modulePath)) {
					Files.createDirectories(modulePath);
				}

				if (Files.notExists(dependencyFile)) {
					Files.createFile(dependencyFile);
				}

				if (
					dependencyConfigSplit[splitLength].equals(
						"testIntegrationRuntime")) {

					sb.append("compileTest:");
				}
				else {
					sb.append("compile:");
				}
			}
			else if (line.endsWith("jar")) {
				sb.append(line);
				sb.append(":");
			}
			else {
				System.out.println(line);
			}
		}
		BufferedReader errorBR =
			new BufferedReader(new InputStreamReader(process.getErrorStream()));

		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
	}
}
