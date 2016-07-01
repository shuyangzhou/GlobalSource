annotation.processing.enabled=true
annotation.processing.enabled.in.editor=false
annotation.processing.processors.list=
annotation.processing.run.all.processors=true
annotation.processing.source.output=${r"${build.generated.sources.dir}"}/ap-source-output
build.classes.dir=${"$"}{build.dir}/classes
build.classes.excludes=**/*.java,**/*.form
# This directory is removed when the project is cleaned:
build.dir=build
build.generated.dir=${"$"}{build.dir}/generated
build.generated.sources.dir=${"$"}{build.dir}/generated-sources
# Only compile against the classpath explicitly listed here:
build.sysclasspath=ignore
build.test.classes.dir=${"$"}{build.dir}/test/classes
build.test.results.dir=${"$"}{build.dir}/test/results
# Uncomment to specify the preferred debugger connection transport:
#debug.transport=dt_socket
debug.classpath=\
	${"$"}{run.classpath}
debug.test.classpath=\
	${"$"}{run.test.classpath}
# Files in build.classes.dir which should be excluded from distribution jar
dist.archive.excludes=
# This directory is removed when the project is cleaned:
dist.dir=dist
dist.javadoc.dir=${"$"}{dist.dir}/javadoc
endorsed.classpath=
excludes=
includes=**
jar.compress=false
# Space-separated list of extra javac options
javac.compilerargs=
javac.deprecation=false
javac.processorpath=\
	${"$"}{javac.classpath}
javac.source=1.8
javac.target=1.8
javac.test.classpath=\
	${"$"}{build.classes.dir}:\
	${"$"}{javac.classpath}
javac.test.processorpath=\
	${"$"}{javac.test.classpath}
javadoc.additionalparam=
javadoc.author=false
javadoc.encoding=${"$"}{source.encoding}
javadoc.noindex=false
javadoc.nonavbar=false
javadoc.notree=false
javadoc.private=false
javadoc.splitindex=true
javadoc.use=true
javadoc.version=false
javadoc.windowtitle=
main.class=
manifest.file=manifest.mf
meta.inf.dir=${"$"}{src.dir}/META-INF
mkdist.disabled=false
platform.active=default_platform

run.classpath=\
	${"$"}{javac.classpath}:\
	${"$"}{build.classes.dir}
# Space-separated list of JVM arguments used when running the project.
# You may also define separate properties like run-sys-prop.name=value instead of -Dname=value.
# To set system properties for unit tests define test-sys-prop.name=value:
run.jvmargs=
run.test.classpath=\
	${"$"}{javac.test.classpath}:\
	${"$"}{build.test.classes.dir}
source.encoding=UTF-8
excludes=**/*.css,**/*.js,**/*.json,**/*.sass,**/*.scss
application.title=${projectName}
dist.jar=${r"${dist.dir}"}/${projectName}.jar

<#list moduleList as module>
<#if module.getSourcePath()??>
file.reference.${module.getModuleName()}-src=${module.getSourcePath()}
src.${module.getModuleName()}.src.dir=${"$"}{file.reference.${module.getModuleName()}-src}
</#if>
<#if module.getSourceResourcePath()??>
file.reference.${module.getModuleName()}-resources=${module.getSourceResourcePath()}
src.${module.getModuleName()}.resources.dir=${"$"}{file.reference.${module.getModuleName()}-resources}
</#if>
<#if module.getTestUnitPath()??>
file.reference.${module.getModuleName()}-test-unit=${module.getTestUnitPath()}
test.${module.getModuleName()}.test-unit.dir=${"$"}{file.reference.${module.getModuleName()}-test-unit}
</#if>
<#if module.getTestIntegrationPath()??>
file.reference.${module.getModuleName()}-test-integration=${module.getTestIntegrationPath()}
test.${module.getModuleName()}.test-integration.dir=${"$"}{file.reference.${module.getModuleName()}-test-integration}
</#if>
<#if module.getTestUnitResourcePath()??>
file.reference.${module.getModuleName()}-test-unit-resources=${module.getTestUnitResourcePath()}
test.${module.getModuleName()}.test-unit-resources.dir=${"$"}{file.reference.${module.getModuleName()}-test-unit-resources}
</#if>
<#if module.getTestIntegrationResourcePath()??>
file.reference.${module.getModuleName()}-test-integration-resources=${module.getTestIntegrationResourcePath()}
test.${module.getModuleName()}.test-integration-resources.dir=${"$"}{file.reference.${module.getModuleName()}-test-integration-resources}
</#if>
</#list>
<#list jarDependencies as jarDependency>
file.reference.${jarDependency.getName()}=${jarDependency.getPath()}
<#if jarDependency.getSourcePath()??>
source.reference.${jarDependency.getName()}=${jarDependency.getSourcePath()}
</#if>
</#list>
<#list moduleDependencies as dependency>
project.${dependency.getName()}=../../modules/${dependency.getName()}
reference.${dependency.getName()}.jar=${"$"}{project.${dependency.getName()}}/dist/${dependency.getName()}.jar
</#list>
<#if projectName != "portal">
project.portal=../portal
reference.portal.jar=${"$"}{project.portal}/dist/portal.jar
</#if>
project.registry-api=../../modules/registry-api
reference.registry-api.jar=${"$"}{project.registry-api}/dist/registry-api.jar
<#list portalLibJars as jarDependency>
file.reference.${jarDependency.getName()}=${jarDependency.getPath()}
</#list>

javac.classpath=\
	<#list jarDependencies as jarDependency>
		<#if !jarDependency.isTest()>
	${"$"}{file.reference.${jarDependency.getName()}}:\
		</#if>
	</#list>
	<#list moduleDependencies as dependency>
		<#if !dependency.isTest()>
	${"$"}{reference.${dependency.getName()}.jar}:\
		</#if>
	</#list>
	<#if projectName != "portal">
	${"$"}{reference.portal.jar}:\
	</#if>
	${"$"}{reference.registry-api.jar}:\
	<#list portalLibJars as jarDependency>
	${"$"}{file.reference.${jarDependency.getName()}}:\
	</#list>

javac.test.classpath=\
	<#list jarDependencies as jarDependency>
		<#if jarDependency.isTest()>
	${"$"}{file.reference.${jarDependency.getName()}}:\
		</#if>
	</#list>
	<#list moduleDependencies as dependency>
		<#if dependency.isTest()>
	${"$"}{reference.${dependency.getName()}.jar}:\
		</#if>
	</#list>
	${"$"}{build.classes.dir}:\
	${"$"}{javac.classpath}