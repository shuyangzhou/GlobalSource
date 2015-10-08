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

		try(
			PrintWriter printWriter =
				new PrintWriter(new BufferedWriter(
					new FileWriter(new File("settings.gradle"), true)))) {

			for (String modulePath : modules) {
				Path path = Paths.get(modulePath);

				printWriter.println(
					"include \"portal/modules/" + path.getFileName() + "\"");
			}
		}
	}

}