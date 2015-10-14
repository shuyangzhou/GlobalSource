package com.liferay.netbeansproject;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class GradleResolver {

	public static void main(String[] args) throws Exception {
		String dependencyString = _extractDependencyString(args[0]);

		_createGradleFile(
			"../common/default.gradle", dependencyString, args[1]);
	}

	private static String _extractDependencyString(String modulePath)
		throws Exception {

		File gradleFile = new File(modulePath, "build.gradle");

		if(!gradleFile.exists()) {
			return "";
		}

		String content = new String(
			Files.readAllBytes(Paths.get(gradleFile.getAbsolutePath())));

		Pattern pattern =
			Pattern.compile("dependencies.?[{][^}]*}|String jenkins.*");

		Matcher matcher = pattern.matcher(content);

		StringBuilder sb = new StringBuilder();

		while(matcher.find()) {
			sb.append(matcher.group(0));
			sb.append("\n");
		}

		return sb.toString();
	}

	private static void _createGradleFile(
			String defaultFilePath, String dependencyString, String filePath)
		throws Exception {

		String content = new String(
			Files.readAllBytes(Paths.get(defaultFilePath)));

		dependencyString = dependencyString.replaceAll(
				".*project\\(\".*?[)]\\n|.*?com\\.liferay\\.portal\".*?\\n",
					"");

		dependencyString = _replaceKeywords(dependencyString);

		content = _replaceOptionalRepository(filePath, content);

		content = content.replace("*insert-dependencies*", dependencyString);

		Files.write(Paths.get(filePath + "/build.gradle"), content.getBytes());
	}

	private static String _replaceKeywords(String line) {
		line = line.replace("optional, ", "");
		line = line.replace("antlr group", "compile group");
		line = line.replace("jarjar group", "compile group");
		line = line.replace("jruby group", "compile group");
		line = line.replace("jnaerator classifier: \"shaded\",", "compile");
		line = line.replace("provided", "compile");
		line = line.replace("testIntegrationCompile", "testCompile");

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