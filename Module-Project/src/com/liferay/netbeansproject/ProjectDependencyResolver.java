/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liferay.netbeansproject;

import com.liferay.netbeansproject.container.Module;

/**
 *
 * @author tom
 */
public interface ProjectDependencyResolver {
	public Module resolve(String modulePath);
}
