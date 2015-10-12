package com.liferay.netbeansproject;

import java.io.File;
import java.io.FileFilter;

public class JarFileFilter implements FileFilter {
	@Override
		public boolean accept(File file) {
			String fileName = file.getName();

			return fileName.endsWith(".jar");
		}
}
