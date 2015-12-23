package com.liferay.netbeansproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

		Path dependenciesDirPath = projectFilePath.resolve("dependencies");

		Files.createDirectories(dependenciesDirPath);

		gradleTask.add("-PdependencyDirectory=" + dependenciesDirPath);

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Process process = processBuilder.start();

		BufferedReader br =
			new BufferedReader(new InputStreamReader(process.getInputStream()));

		String line;

		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}

		BufferedReader errBr =
			new BufferedReader(new InputStreamReader(process.getErrorStream()));

		while ((line = errBr.readLine()) != null) {
			System.out.println(line);
		}
	}
}
