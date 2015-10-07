package com.liferay.netbeansproject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
public class PropertyLoader {
	public Properties loadPropertyFile(String propertyFileName) throws Exception {
		Properties buildProperties = new Properties();

		if(new File(propertyFileName).exists()) {
			try (InputStream in =
				new FileInputStream(propertyFileName)) {

				buildProperties.load(in);
			}
		}

		int index = propertyFileName.indexOf(".");

		String extFile = propertyFileName.substring(0,index) + "-ext" + propertyFileName.substring(index);

		if(new File(extFile).exists()) {
			try (InputStream in =
				new FileInputStream(extFile)) {

				buildProperties.load(in);
			}
		}
		return buildProperties;
	}
}
