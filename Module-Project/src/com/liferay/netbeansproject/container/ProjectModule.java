/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liferay.netbeansproject.container;

import java.util.List;

/**
 *
 * @author tom
 */
public class ProjectModule {
	public String getProjectModuleName() {
		return _projectModuleName;
	}

	public List<Module> getModuleList() {
		return _moduleList;
	}

	public ProjectModule(String projectModuleName, List<Module> moduleList) {
		_projectModuleName = projectModuleName;
		_moduleList = moduleList;
	}

	private final String _projectModuleName;
	private final List<Module> _moduleList;
}
