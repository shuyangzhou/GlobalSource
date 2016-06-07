package com.liferay.netbeansproject;

import com.liferay.netbeansproject.util.StringUtil;

import java.io.BufferedReader;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class ModuleBuildParser {

	public static List<ModuleInfo> parseBuildFile(Path modulePath)
		throws Exception {

		Path gradleFilePath = modulePath.resolve("build.gradle");

		if (!Files.exists(gradleFilePath)) {
			return Collections.emptyList();
		}

		List<ModuleInfo> moduleInfos = new ArrayList<>();

		try(BufferedReader bufferedReader = Files.newBufferedReader(
			gradleFilePath, Charset.defaultCharset())) {

			String line = null;

			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();

				if (!line.contains(" project(")) {
					continue;
				}

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

				String[] parts = StringUtil.split(moduleLocation, ':');

				if (parts.length == 0) {
					throw new IllegalStateException(
						"Broken syntax in " + gradleFilePath);
				}

				moduleInfos.add(
					new ModuleInfo(
						parts[parts.length - 1], line.startsWith("test")));
			}
		}

		return moduleInfos;
	}

	public static class ModuleInfo {

		public String getModuleName() {
			return _moduleName;
		}

		public boolean isTest() {
			return _test;
		}

		private ModuleInfo(String moduleName, boolean test) {
			_moduleName = moduleName;
			_test = test;
		}

		private final String _moduleName;
		private final boolean _test;

	}

}