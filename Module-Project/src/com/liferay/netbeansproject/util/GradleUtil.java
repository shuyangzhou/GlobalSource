/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liferay.netbeansproject.util;

import com.liferay.netbeansproject.container.Module.ModuleDependency;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author tom
 */
public class GradleUtil {

	public static List<String> getJarDependencies(Path modulePath)
		throws Exception {

		String dependencies = _extractDependency(modulePath);

		Matcher projectMatcher = _projectPattern.matcher(dependencies);

		dependencies = projectMatcher.replaceAll("");

		Matcher unusedDependencyMatcher =
			_unusedDependencyPattern.matcher(dependencies);

		dependencies = unusedDependencyMatcher.replaceAll("");

		Matcher portalMatcher = _portalPattern.matcher(dependencies);

		dependencies = portalMatcher.replaceAll("");

		dependencies = _replaceKeywords(dependencies);

		return _formatDependency(dependencies);
	}

	public static List<ModuleDependency> getModuleDependencies(
			Path modulePath)
		throws Exception {

		Path gradleFilePath = modulePath.resolve("build.gradle");

		if (!Files.exists(gradleFilePath)) {
			return Collections.emptyList();
		}

		List<ModuleDependency> moduleInfos = new ArrayList<>();

		try(BufferedReader bufferedReader = Files.newBufferedReader(
			gradleFilePath, Charset.defaultCharset())) {

			String line = null;

			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();

				if (line.startsWith("compile project") ||
					line.startsWith("provided project") ||
					line.startsWith("frontendThemes project") ||
					line.startsWith("testCompile project") ||
					line.startsWith("testIntegrationCompile project")) {

					int index1 = line.indexOf('\"');

					if (index1 < 0) {
						throw new IllegalStateException(
							"Broken syntax in " + gradleFilePath);
					}

					int index2 = line.indexOf('\"', index1 + 1);

					if (index2 < 0) {
						throw new IllegalStateException(
							"Broken syntax in " + gradleFilePath);
					}

					String moduleLocation = line.substring(index1 + 1, index2);

					boolean test = false;

					if(line.startsWith("testCompile project") ||
						line.startsWith("testIntegrationCompile project")) {

						test = true;
					}

					moduleInfos.add(new ModuleDependency(moduleLocation, test));
				}
			}
		}

		return moduleInfos;
	}

	private static String _extractDependency(Path modulePath)
		throws IOException {

		Path buildGradlePath = modulePath.resolve("build.gradle");

		if (!Files.exists(buildGradlePath)) {
			return "";
		}

		String content = new String(Files.readAllBytes(buildGradlePath));

		Matcher jenkinsMatcher = _jenkinsPattern.matcher(content);

		StringBuilder sb = new StringBuilder();

		while(jenkinsMatcher.find()) {
			sb.append(jenkinsMatcher.group(0));
			sb.append('\n');
		}

		Matcher dependencyMatcher = _dependencyPattern.matcher(content);

		while(dependencyMatcher.find()) {
			sb.append(dependencyMatcher.group(0));
			sb.append('\n');
		}

		return sb.toString();
	}
	
	private static List<String> _formatDependency(String dependencies) {
		List<String> dependencyList = new ArrayList<>();

		for (String dependency : StringUtil.split(dependencies, '\n')) {
			if(!dependency.isEmpty()) {
				dependencyList.add(dependency.trim());
			}
		}
		return dependencyList;
	}

	private static String _replaceKeywords(String dependencies) {
		dependencies = StringUtil.replace(dependencies, "optional, ", "");
		dependencies = StringUtil.replace(
			dependencies, "antlr group", "compile group");
		dependencies = StringUtil.replace(
			dependencies, "jarjar group", "compile group");
		dependencies = StringUtil.replace(
			dependencies, "jruby group", "compile group");
		dependencies = StringUtil.replace(
			dependencies, "jnaerator classifier: \"shaded\",", "compile");
		dependencies = StringUtil.replace(dependencies, "provided", "compile");
		dependencies = StringUtil.replace(
			dependencies, "testIntegrationCompile", "testCompile");

		dependencies = StringUtil.replace(dependencies, "dependencies {", "");
		dependencies = StringUtil.replace(dependencies, "}", "");
		
		return StringUtil.replace(
			dependencies, "testCompile", "testConfiguration");
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

	private static final Pattern _unusedDependencyPattern =
		Pattern.compile("\tconfigAdmin\\s*group.*\\n");

}
