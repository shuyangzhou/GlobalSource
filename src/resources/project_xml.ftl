<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<project xmlns="http://www.netbeans.org/ns/project/1">
	<type>org.netbeans.modules.java.j2seproject</type>
	<configuration>
		<data xmlns="http://www.netbeans.org/ns/j2se-project/3">
			<name>${moduleDisplayName}</name>
			<source-roots>
				<#if module.getSourcePath()??>
					<root id="src.${module.getModuleName()}.src.dir" name="src"/>
				</#if>
				<#if module.getSourceResourcePath()??>
					<root id="src.${module.getModuleName()}.resources.dir" name="resources"/>
				</#if>
				<#if module.getJspPath()??>
					<root id="src.${module.getModuleName()}.jsp.dir" name="jsp-src"/>
				</#if>
			</source-roots>

			<test-roots>
				<#if module.getTestUnitPath()??>
					<root id="test.${module.getModuleName()}.test-unit.dir" name="test-unit"/>
				</#if>
				<#if module.getTestUnitResourcePath()??>
					<root id="test.${module.getModuleName()}.test-unit-resources.dir" name="test-unit-resources"/>
				</#if>
				<#if module.getTestIntegrationPath()??>
					<root id="test.${module.getModuleName()}.test-integration.dir" name="test-integration"/>
				</#if>
				<#if module.getTestIntegrationResourcePath()??>
					<root id="test.${module.getModuleName()}.test-integration-resources.dir" name="test-integration-resources"/>
				</#if>
			</test-roots>
		</data>

		<references xmlns="http://www.netbeans.org/ns/ant-project-references/1">
			<#list module.getModuleDependencies() as dependency>
				<reference>
					<foreign-project>${dependency.getName()}</foreign-project>
					<artifact-type>jar</artifact-type>
					<script>build.xml</script>
					<target>jar</target>
					<clean-target>clean</clean-target>
					<id>jar</id>
				</reference>
			</#list>
			<#list module.getPortalModuleDependencies() as dependency>
				<reference>
					<foreign-project>${dependency}</foreign-project>
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