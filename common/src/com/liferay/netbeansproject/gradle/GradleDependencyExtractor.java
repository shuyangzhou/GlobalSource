package com.liferay.netbeansproject.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;

public class GradleDependencyExtractor {
	public static void main(String[] args) throws IOException {
		File gradleDependencyFile = new File("GradleDependency.properties");

		StringBuilder sb = new StringBuilder("compile=");

		sb.append(args[0]);
		sb.append("\n");
		sb.append("compileTest=");
		sb.append(args[1]);

		Files.write(
			gradleDependencyFile.toPath(), Arrays.asList(sb),
			Charset.defaultCharset());
	}
}
