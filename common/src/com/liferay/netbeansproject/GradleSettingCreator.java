package com.liferay.netbeansproject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.nio.file.Path;
import java.nio.file.Paths;
public class GradleSettingCreator {

	public static void main(String[] args) throws Exception {
		String[] modules = args[0].split(",");

		File settingsFile = new File("settings.gradle");

		try(PrintWriter pw = new PrintWriter(
			new BufferedWriter(new FileWriter(settingsFile, true)))) {

			for (String modulePath : modules) {
				Path path = Paths.get(modulePath);

				pw.println("include \"portal/modules/" + path.getFileName() + "\"");
			}
		}
	}

}