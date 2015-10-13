package com.liferay.netbeansproject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Properties;
public class GradleResolver {

	public static void main(String[] args) throws Exception {
		String dependencyString = _createDependencyString(args[0]);

		_createGradleFile(
			"../common/default.gradle", dependencyString, args[1]);
	}

	private static String _createDependencyString(String modulePath)
		throws Exception {

		File gradleFile = new File(modulePath, "build.gradle");

		try(BufferedReader bufferedReader =
				new BufferedReader(new FileReader(gradleFile))) {

			StringBuilder sb = new StringBuilder();

			String line = null;

			while ((line = bufferedReader.readLine()) != null) {
				if (line.startsWith("dependencies {")) {
					sb.append(line);
					sb.append("\n");

					_extractDependencies(bufferedReader, line, sb);
				}

				else if(line.startsWith("String jenkins")) {
					sb.append(line);
					sb.append("\n");
				}
			}

			return sb.toString();
		}
		catch (NullPointerException | FileNotFoundException e) {
			return "";
		}
	}

	private static void _createGradleFile(
			String defaultFilePath, String dependencyString, String filePath)
		throws Exception {

		String content = new String(
			Files.readAllBytes(Paths.get(defaultFilePath)));

		content = _replaceOptionalRepository(filePath, content);

		content = content.replace("*insert-dependencies*", dependencyString);

		try(
			PrintWriter printWriter =
				new PrintWriter(new BufferedWriter(
					new FileWriter(new File(filePath, "build.gradle"))))) {

			printWriter.println(content);
		}
	}

	private static void _extractDependencies(
			BufferedReader bufferedReader, String line, StringBuilder sb)
		throws Exception {

		while (!(line = bufferedReader.readLine()).startsWith("}")) {
			if (!line.contains("project") &&
				!line.contains("group: \"com.liferay.portal\"")) {

				line = replaceKeywords(line);

				sb.append(line);
				sb.append("\n");
			}
		}

		sb.append(line);
		sb.append("\n");
	}

	private static String replaceKeywords(String line) {
		line = line.replaceFirst("optional, ", "");
		line = line.replaceFirst("antlr group", "compile group");
		line = line.replaceFirst("jarjar group", "compile group");
		line = line.replaceFirst("jruby group", "compile group");
		line = line.replaceFirst(
			"jnaerator classifier: \"shaded\",", "compile");
		line = line.replaceFirst("provided", "compile");
		line = line.replaceFirst("testIntegrationCompile", "testCompile");

		return line;
	}

	private static String _replaceOptionalRepository(
			String filePath, String line)
		throws Exception {

		Properties optionalRepositories = _propertyLoader.loadPropertyFile(
					"module-repository.properties");

		Path path = Paths.get(filePath);

		Path fileName = path.getFileName();

		String moduleName = fileName.toString();

		if (optionalRepositories.containsKey(moduleName)) {
			return line.replace(
				"*optional-repository*",
					optionalRepositories.getProperty(moduleName));
		}
		else {
			return line.replace("*optional-repository*", "");
		}
	}

	private static final PropertyLoader _propertyLoader = new PropertyLoader();

}