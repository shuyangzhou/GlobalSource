package com.liferay.netbeansproject;

import com.liferay.netbeansproject.util.ArgumentsUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author tom
 */
public class ProcessGradle {
	public static void main(String[] args) throws Exception {
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		Path portalDirPath = Paths.get(arguments.get("portal.dir"));
		Path projectDirPath = Paths.get(arguments.get("project.dir"));

		Boolean displayGradleOutput = Boolean.valueOf(
			arguments.get("display.gradle"));

		Path gradlewPath = portalDirPath.resolve("gradlew");

		List<String> gradleTask = new ArrayList<>();

		gradleTask.add(gradlewPath.toString());
		gradleTask.add("--parallel");
		gradleTask.add("--init-script=dependency.gradle");
		gradleTask.add("printDependencies");

		if (!Boolean.valueOf(arguments.get("prebuilt.portal"))) {
			gradleTask.add("copyLibs");
		}

		gradleTask.add("-p");

		Path moduleDirPath = portalDirPath.resolve("modules");

		gradleTask.add(moduleDirPath.toString());

		Path dependenciesDirPath = projectDirPath.resolve("dependencies");

		Files.createDirectories(dependenciesDirPath);

		gradleTask.add("-PdependencyDirectory=" + dependenciesDirPath);

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Map<String, String> env = processBuilder.environment();

		env.put("GRADLE_OPTS", "-Xmx2g");

		Process process = processBuilder.start();

		if (displayGradleOutput) {
			String line;

			try(BufferedReader br = new BufferedReader(new InputStreamReader(
				process.getInputStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}

			try(BufferedReader br = new BufferedReader(new InputStreamReader(
				process.getErrorStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}
		}
		else {
			int exitCode = process.waitFor();

			if (exitCode != 0) {
				throw new IOException(
					"Process " + processBuilder.command() + " failed with " +
						exitCode);
			}
		}
	}
}
