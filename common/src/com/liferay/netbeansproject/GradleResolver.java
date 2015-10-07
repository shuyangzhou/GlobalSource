package com.liferay.netbeansproject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Properties;
public class GradleResolver {

	public static void main(String[] args) throws Exception {
		String dependencyString = _createDependencyString(args[0]);

		_createGradleFile(
			"../common/default.gradle", dependencyString, args[1]);
	}

	private static String _addOptionalRepository(
		Properties optionalRepositories, String filePath, String line) {

		Path path = Paths.get(filePath);

		Path fileName = path.getFileName();

		String moduleName = fileName.toString();

		if (optionalRepositories.containsKey(moduleName)) {
			line = line.replace(
					"optional-repository",
					optionalRepositories.getProperty(moduleName));
		}
		else {
			line = line.replace("optional-repository", "");
		}

		return line;
	}

	private static String _createDependencyString(String modulePath)
		throws Exception {

		File gradleFile = new File(modulePath + "/build.gradle");

		if (gradleFile.exists()) {
			try(BufferedReader br =
					new BufferedReader(new FileReader(gradleFile))) {

				String line = br.readLine();

				StringBuilder sb = new StringBuilder();

				while (line != null) {
					if (line.startsWith("dependencies {")) {
						while (!line.startsWith("}")) {
							if (!line.contains("project") &&
								!line.contains("group: \"com.liferay.portal\""))
							{
								line = line.replaceFirst("optional, ", "");
								line = line.replaceFirst(
									"antlr group", "compile group");
								line = line.replaceFirst(
									"jarjar group", "compile group");
								line = line.replaceFirst(
									"jruby group", "compile group");
								line = line.replaceFirst(
									"jnaerator classifier: \"shaded\",",
										"compile");
								line = line.replaceFirst("provided", "compile");
								line = line.replaceFirst(
									"testIntegrationCompile", "testCompile");
								sb.append(line);
								sb.append("\n");
							}

							line = br.readLine();
						}

						sb.append(line);
						sb.append("\n");
					}

					else if(line.startsWith("String jenkins")) {
						sb.append(line);
						sb.append("\n");

						line = br.readLine();
					}

					else {
						line = br.readLine();
					}
				}

				return sb.toString();
			}
		}

		return null;
	}

	private static void _createGradleFile(
			String defaultFilePath, String dependencyString, String filePath)
		throws Exception {

		File defaultFile = new File(defaultFilePath);

		try(PrintWriter pw = new PrintWriter(
			new BufferedWriter(new FileWriter(new File(
				filePath + "/build.gradle"))))) {

			try(BufferedReader br =
					new BufferedReader(new FileReader(defaultFile))) {

				Properties optionalRepositories =
					_propertyLoader.loadPropertyFile(
						"module-repository.properties");

				String line = br.readLine();

				while (!line.equals("*insert-dependencies*")) {
					if (line.contains("optional-repository")) {
						pw.println(
							_addOptionalRepository(
								optionalRepositories, filePath, line));
					}
					else {
						pw.println(line);
					}

					line = br.readLine();
				}

				pw.println(dependencyString);

				line = br.readLine();

				while (line != null) {
					pw.println(line);

					line = br.readLine();
				}
			}
		}
	}
	private static final PropertyLoader _propertyLoader = new PropertyLoader();
}