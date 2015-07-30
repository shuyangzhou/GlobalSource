package com.liferay.netbeansproject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class AppendLibJars {

	public static void AppendJars(String jarList) {
		String[] jars = jarList.split(File.pathSeparator);

		File propertyFile = new File("portal/nbproject/project.properties");

		if (propertyFile.exists()) {
			try (
				PrintWriter printWriter = new PrintWriter(
					new BufferedWriter(new FileWriter(propertyFile, true)))) {

				for (String jarPath : jars) {
					String[] jarPathSplit = jarPath.split("/");

					String jar = jarPathSplit[jarPathSplit.length - 1];

					printWriter.println(
						"file.reference." + jar + "=" + jarPath);
				}

				printWriter.println("javac.classpath=\\");

				for (String jarPath : jars) {
					String[] jarPathSplit = jarPath.split("/");

					String jar = jarPathSplit[jarPathSplit.length - 1];

					printWriter.println("${file.reference." + jar + "}:\\");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("file not found");
		}
	}
}