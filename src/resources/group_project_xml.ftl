<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<project xmlns="http://www.netbeans.org/ns/project/1">
	<type>org.netbeans.modules.java.j2seproject</type>
	<configuration>
		<data xmlns="http://www.netbeans.org/ns/j2se-project/3">
			<name>${projectName}</name>
			<source-roots>
				<#list moduleList as module>
				<#if module.getSourcePath()??>
					<root id="src.${module.getModuleName()}.src.dir" name="${module.getModuleName()}.src"/>
				</#if>
				<#if module.getSourceResourcePath()??>
					<root id="src.${module.getModuleName()}.resources.dir" name="${module.getModuleName()}.resources"/>
				</#if>
				</#list>
			</source-roots>

			<test-roots>
				<#list moduleList as module>
				<#if module.getTestUnitPath()??>
					<root id="test.${module.getModuleName()}.test-unit.dir" name="${module.getModuleName()}.test-unit"/>
				</#if>
				<#if module.getTestUnitResourcePath()??>
					<root id="test.${module.getModuleName()}.test-unit-resources.dir" name="${module.getModuleName()}.test-unit-resources"/>
				</#if>
				<#if module.getTestIntegrationPath()??>
					<root id="test.${module.getModuleName()}.test-integration.dir" name="${module.getModuleName()}.test-integration"/>
				</#if>
				<#if module.getTestIntegrationResourcePath()??>
					<root id="test.${module.getModuleName()}.test-integration-resources.dir" name="${module.getModuleName()}.test-integration-resources"/>
				</#if>
				</#list>
			</test-roots>
		</data>

		<references xmlns="http://www.netbeans.org/ns/ant-project-references/1">
			<#list moduleDependencies as dependency>
				<reference>
					<foreign-project>${dependency.getName()}</foreign-project>
					<artifact-type>jar</artifact-type>
					<script>build.xml</script>
					<target>jar</target>
					<clean-target>clean</clean-target>
					<id>jar</id>
				</reference>
			</#list>
				<reference>
					<foreign-project>registry-api</foreign-project>
					<artifact-type>jar</artifact-type>
					<script>build.xml</script>
					<target>jar</target>
					<clean-target>clean</clean-target>
					<id>jar</id>
				</reference>
			<#if projectName != "portal">
				<reference>
					<foreign-project>portal</foreign-project>
					<artifact-type>jar</artifact-type>
					<script>build.xml</script>
					<target>jar</target>
					<clean-target>clean</clean-target>
					<id>jar</id>
				</reference>
			</#if>
		</references>
	</configuration>
</project>