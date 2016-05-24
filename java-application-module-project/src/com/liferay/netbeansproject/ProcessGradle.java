package com.liferay.netbeansproject;

import com.liferay.netbeansproject.container.Module.JarDependency;
import com.liferay.netbeansproject.util.ArgumentsUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author tom
 */
public class ProcessGradle {

	public static void main(String[] args) throws Exception {
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		processGradle(
			Paths.get(arguments.get("portal.dir")),
			Paths.get(arguments.get("project.dir")),
			Paths.get(arguments.get("work.dir")));
	}

	public static Map<String, List<JarDependency>> processGradle(
			Path portalDirPath, Path projectDirPath, Path workDirPath)
		throws Exception {

		Path gradlewPath = portalDirPath.resolve("gradlew");

		List<String> gradleTask = new ArrayList<>();

		gradleTask.add(gradlewPath.toString());
		gradleTask.add("--parallel");
		gradleTask.add("--init-script=dependency.gradle");
		gradleTask.add("-p");

		Path modulesPath = portalDirPath.resolve("modules");

		gradleTask.add(modulesPath.toString());

		Path relativeWorkPath = modulesPath.relativize(workDirPath);

		String relativeWorkString = relativeWorkPath.toString();

		if (!relativeWorkString.isEmpty()) {
			relativeWorkString = relativeWorkString.replace('/', ':');

			relativeWorkString += ":";
		}

		gradleTask.add(relativeWorkString + "printDependencies");

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

		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		if (Boolean.valueOf(
				properties.getProperty("display.gradle.process.output"))) {

			String line;

			try(BufferedReader br = new BufferedReader(
				new InputStreamReader(process.getInputStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}

			try(BufferedReader br = new BufferedReader(
				new InputStreamReader(process.getErrorStream()))) {

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

		final Map<String, List<JarDependency>> dependenciesMap =
			new HashMap<>();

		Files.walkFileTree(dependenciesDirPath, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					List<JarDependency> jarDependencies = new ArrayList<>();

					Properties dependencies = PropertiesUtil.loadProperties(
						path);

					String[] compileProperties = StringUtil.split(
						dependencies.getProperty("compile"), ':');

					for (String jar : compileProperties) {
						jarDependencies.add(
							new JarDependency(Paths.get(jar), false));
					}

					String[] compileTestProperties = StringUtil.split(
						dependencies.getProperty("compileTest"), ':');

					for (String jar : compileTestProperties) {
						jarDependencies.add(
							new JarDependency(Paths.get(jar), true));
					}

					Path moduleName = path.getFileName();

					dependenciesMap.put(moduleName.toString(), jarDependencies);

					return FileVisitResult.CONTINUE;
				}
			});

		return dependenciesMap;
	}

}