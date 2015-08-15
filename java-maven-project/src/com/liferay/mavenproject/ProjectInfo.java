package com.liferay.mavenproject;

public class ProjectInfo {

	public ProjectInfo(
		String groupId, String artifactId, String version, String packaging,
		String name, String fullPath, String portalPath, String ivyFile,
		String buildFile, String[] modules, String[] moduleList) {

		_groupId = groupId;

		_artifactId = artifactId;

		_version = version;

		_packaging = packaging;

		_name = name.substring(portalPath.length() + 1);

		_fullPath = fullPath;

		_portalPath = portalPath;

		_ivyFile = ivyFile;

		_buildFile = buildFile;

		_modules = modules;

		_lib = moduleList;
	}

	public ProjectInfo(
		String groupId, String artifactId, String version, String packaging,
		String name, String fullPath, String[] modules, String[] lib) {

		_groupId = groupId;

		_artifactId = artifactId;

		_version = version;

		_packaging = packaging;

		_name = name;

		_fullPath = fullPath;

		_modules = modules;

		_lib = lib;

		_buildFile = null;

		_ivyFile = null;

		_portalPath = null;
	}

	public ProjectInfo(
		String groupId, String artifactId, String version, String packaging,
		String name, String[] modules) {

		_groupId = groupId;

		_artifactId = artifactId;

		_version = version;

		_packaging = packaging;

		_name = name;

		_modules = modules;

		_buildFile = null;

		_fullPath = null;

		_ivyFile = null;

		_lib = null;

		_portalPath = null;
	}

	public String getArtifactId() {
		return _artifactId;
	}

	public String getBuildFile() {
		return _buildFile;
	}

	public String getFullPath() {
		return _fullPath;
	}

	public String getGroupId() {
		return _groupId;
	}

	public String getIvyFile() {
		return _ivyFile;
	}

	public String[] getLib() {
		return _lib;
	}

	public String[] getModules() {
		return _modules;
	}

	public String getName() {
		return _name;
	}

	public String getPackaging() {
		return _packaging;
	}

	public String getPortalPath() {
		return _portalPath;
	}

	public String getVersion() {
		return _version;
	}

	private final String _artifactId;
	private final String _buildFile;
	private final String _fullPath;
	private final String _groupId;
	private final String _ivyFile;
	private final String[] _lib;
	private final String[] _modules;
	private final String _name;
	private final String _packaging;
	private final String _portalPath;
	private final String _version;

}