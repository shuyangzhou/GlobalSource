<?xml version="1.0" encoding="UTF-8"?>
<project name="${projectName}" default="default" basedir=".">
	<description></description>
	<import file="nbproject/build-impl.xml"/>
	<taskdef classpath="../../../ant-contrib.jar" resource="net/sf/antcontrib/antlib.xml" />
	<target name="-pre-jar">
		<antcall target="compile-test" />
	</target>
	<target name="-post-jar">
		<if>
			<available file="build/test/classes" type="dir" />
			<then>
				<jar jarfile="${"$"}{dist.jar}" update="true">
					<zipfileset dir="build/test/classes" />
				</jar>
			</then>
		</if>
	</target>
</project>