/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liferay.netbeansproject;

import static com.liferay.netbeansproject.ModuleProject.linkModuletoMap;
import com.liferay.netbeansproject.container.Module;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tom
 */
public class PortalFileVisitor<T> extends SimpleFileVisitor<T> {
	protected PortalFileVisitor() {
	}

	@Override
	public FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) {
		Path dirPath = Paths.get(dir.toString());

		if (dirPath.endsWith(".gradle")) {
			return FileVisitResult.SKIP_SUBTREE;
		}

		else if (dirPath.endsWith("sample")) {
			return FileVisitResult.SKIP_SUBTREE;
		}

		else if (dirPath.endsWith("src")) {
			Path parentPath = dirPath.getParent();

			try {
				Module module = ModuleProject.createModule(parentPath);

				linkModuletoMap(module, parentPath.getParent());
			}
			catch (Exception ex) {
				Logger.getLogger(PortalFileVisitor.class.getName()).log(
					Level.SEVERE, null, ex);
			}
			return FileVisitResult.SKIP_SUBTREE;
		}
		return FileVisitResult.CONTINUE;
	}
}
