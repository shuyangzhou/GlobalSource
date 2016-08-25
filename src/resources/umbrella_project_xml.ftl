<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<project xmlns="http://www.netbeans.org/ns/project/1">
	<type>org.netbeans.modules.java.j2seproject</type>
	<configuration>
		<data xmlns="http://www.netbeans.org/ns/j2se-project/3">
			<name>${portalName}-umbrella</name>
			<source-roots>
				<root id="src.portal-web.dir"/>
				<root id="src.portal-web-functional.dir"/>
			</source-roots>
			<test-roots/>
		</data>

		<references xmlns="http://www.netbeans.org/ns/ant-project-references/1">
			<#list moduleNames as moduleName>
			<reference>
				<foreign-project>${moduleName}</foreign-project>
				<artifact-type>jar</artifact-type>
				<script>build.xml</script>
				<target>jar</target>
				<clean-target>clean</clean-target>
				<id>jar</id>
			</reference>
			</#list>
		</references>
	</configuration>
</project>