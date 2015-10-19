package com.liferay.netbeansproject;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class GradleResolver {

	public static void main(String[] args) throws Exception {
		StringBuilder gradleSettingSB = new StringBuilder();

		for(String pathToGradle : args[0].split(",")) {
			String dependencyString = _extractDependencyString(pathToGradle);

			Path path = Paths.get(pathToGradle);

			Path fileName = path.getFileName();

			_createGradleFile(dependencyString, "portal/modules/" + fileName);

			gradleSettingSB.append("include \"portal/modules/");
			gradleSettingSB.append(fileName);
			gradleSettingSB.append("\"\n");
		}

		String gradleSettings = gradleSettingSB.toString();

		Files.write(Paths.get("settings.gradle"), gradleSettings.getBytes());
	}

	private static String _extractDependencyString(String modulePath)
		throws Exception {

		File gradleFile = new File(modulePath, "build.gradle");

		if(!gradleFile.exists()) {
			return "";
		}

		String content = new String(Files.readAllBytes(gradleFile.toPath()));

		Matcher jenkinsMatcher = _jenkinsPattern.matcher(content);

		StringBuilder sb = new StringBuilder();

		while(jenkinsMatcher.find()) {
			sb.append(jenkinsMatcher.group(0));
			sb.append("\n");
		}

		Matcher dependencyMatcher = _dependencyPattern.matcher(content);

		while(dependencyMatcher.find()) {
			sb.append(dependencyMatcher.group(0));
			sb.append("\n");
		}

		return sb.toString();
	}

	private static void _createGradleFile(String dependency, String filePath)
		throws Exception {

		Matcher projectMatcher = _projectPattern.matcher(dependency);

		dependency = projectMatcher.replaceAll("");

		Matcher portalMatcher = _portalPattern.matcher(dependency);

		dependency = portalMatcher.replaceAll("");

		dependency = _replaceKeywords(dependency);

		String content = new String(
			Files.readAllBytes(Paths.get("../common/default.gradle")));

		content = content.replace("*insert-dependencies*", dependency);

		Files.write(Paths.get(filePath + "/build.gradle"), content.getBytes());
	}

	private static String _replaceKeywords(String dependency) {
		dependency = dependency.replace("optional, ", "");
		dependency = dependency.replace("antlr group", "compile group");
		dependency = dependency.replace("jarjar group", "compile group");
		dependency = dependency.replace("jruby group", "compile group");
		dependency = dependency.replace(
			"jnaerator classifier: \"shaded\",", "compile");
		dependency = dependency.replace("provided", "compile");
		dependency = dependency.replace(
			"testIntegrationCompile", "testCompile");

		return dependency;
	}

	private static final Pattern _dependencyPattern =
		Pattern.compile("dependencies(\\s*)\\{[^}]*}");

	private static final Pattern _jenkinsPattern =
		Pattern.compile("String jenkins.*");

	private static final Pattern _portalPattern =
		Pattern.compile(
			"\t(compile|provided|testCompile|testIntegrationCompile)\\s*group:"
				+ "\\s\"com\\.liferay\\.portal\".*\\n");

	private static final Pattern _projectPattern =
		Pattern.compile(
			"\t(compile|provided|testCompile|testIntegrationCompile|"
				+ "frontendThemes)\\s*project.*\\n");
}