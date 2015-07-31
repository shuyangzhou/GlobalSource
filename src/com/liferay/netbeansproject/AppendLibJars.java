package com.liferay.netbeansproject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppendLibJars {

	public static void appendJars(String jarList) throws IOException {
		String[] jars = jarList.split(File.pathSeparator);

		if (jars.length == 0) {
			return;
		}

		try (
			PrintWriter printWriter = new PrintWriter(
				new BufferedWriter(
					new FileWriter(
						"portal/nbproject/project.properties", true)))) {

			StringBuilder sb = new StringBuilder("javac.classpath=\\\n");

			for (String jarPath : jars) {
				Path path = Paths.get(jarPath);

				Path fileNamePath = path.getFileName();

				printWriter.println(
					"file.reference." + fileNamePath + "=" + jarPath);

				sb.append("\t${file.reference.");
				sb.append(fileNamePath);
				sb.append("}:\\\n");
			}

			sb.setLength(sb.length() - 3);

			printWriter.println(sb.toString());
		}
	}

}