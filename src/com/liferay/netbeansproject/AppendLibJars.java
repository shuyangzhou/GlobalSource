package com.liferay.netbeansproject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class AppendLibJars {

	public static void appendJars(String jarList) throws FileNotFoundException {
		String[] jars = jarList.split(File.pathSeparator);

		File propertyFile = new File("portal/nbproject/project.properties");

		if (!propertyFile.exists()) {
			throw new FileNotFoundException("Project.properties not found");
		}
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
		catch (IOException e) {
			System.out.println(e.getClass());
		}
	}

}