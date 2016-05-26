package com.liferay.netbeansproject.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesUtil {

	public static Properties loadProperties(Path filePath)
		throws IOException {

		Properties properties = new Properties();

		try (InputStream in = Files.newInputStream(filePath)) {
			properties.load(in);
		}

		Path fileNamePath = filePath.getFileName();

		String fileName = fileNamePath.toString();

		int index = fileName.indexOf('.');

		if (index < 0) {
			return properties;
		}

		String extFileName =
			fileName.substring(0,index) + "-ext" + fileName.substring(index);

		Path extFilePath = filePath.resolveSibling(extFileName);

		if(Files.exists(extFilePath)) {
			try (InputStream in = Files.newInputStream(extFilePath)) {
				properties.load(in);
			}
		}

		return properties;
	}

	public static Map<String, String> getSubProperties(
		Properties properties, String propertyName) {

		Map<String, String> map = new HashMap<>();

		Enumeration e = properties.propertyNames();

		while (e.hasMoreElements()) {
			String property = (String) e.nextElement();

			if (property.contains(propertyName)) {
				map.put(
					StringUtil.extract(property, '[', ']'),
					properties.getProperty(property));
			}
		}

		return map;
	}

}