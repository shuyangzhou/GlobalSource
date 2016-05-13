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
		Path workingModulePath = Paths.get(arguments.get("process.module"));

		Path gradlewPath = portalDirPath.resolve("gradlew");

		List<String> gradleTask = new ArrayList<>();

		gradleTask.add(gradlewPath.toString());
		gradleTask.add("--parallel");
		gradleTask.add("--init-script=dependency.gradle");
		gradleTask.add("printDependencies");
		gradleTask.add("-p");

		Path moduleDirPath = workingModulePath;

		gradleTask.add(moduleDirPath.toString());

		Path dependenciesDirPath = projectDirPath.resolve("dependencies");

		Files.createDirectories(dependenciesDirPath);

		gradleTask.add("-PdependencyDirectory=" + dependenciesDirPath);

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Map<String, String> env = processBuilder.environment();

		String javaVersion = System.getProperty("java.version");

		if (javaVersion.startsWith("1.7.")) {
			env.put("GRADLE_OPTS", "-Xmx2g -XX:MaxPermSize=256m");
		}
		else {
			env.put("GRADLE_OPTS", "-Xmx2g");
		}

		Process process = processBuilder.start();

		if (Boolean.valueOf(arguments.get("display.gradle"))) {
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
