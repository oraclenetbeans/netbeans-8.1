<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:xalan="http://xml.apache.org/xslt"
                exclude-result-prefixes="xalan">
    <xsl:output method="xml" indent="yes" encoding="UTF-8" xalan:indent-amount="4"/>
    <xsl:template match="/">
        <project name="platform" default="download" basedir="..">
            <condition property="download.required">
                <and>
                    <not>
                        <available file="${{harness.dir}}/suite.xml"/>
                    </not>
                    <isset property="bootstrap.url"/>
                    <isset property="autoupdate.catalog.url"/>
                </and>
            </condition>
            <target name="download" if="download.required">
                <mkdir dir="${{harness.dir}}"/>
                <pathconvert pathsep="|" property="download.clusters">
                    <mapper type="flatten"/>
                    <path path="${{cluster.path}}"/>
                </pathconvert>
                <property name="disabled.modules" value=""/>
                <pathconvert property="module.includes" pathsep="">
                    <mapper type="glob" from="${{basedir}}${{file.separator}}*" to="(?!^\Q*\E$)"/>
                    <path>
                        <filelist files="${{disabled.modules}}" dir="."/>
                    </path>
                </pathconvert>
                <echo message="Downloading clusters ${{download.clusters}}"/>
                <property name="tasks.jar" location="${{java.io.tmpdir}}/tasks.jar"/>
                <get src="${{bootstrap.url}}" dest="${{tasks.jar}}" usetimestamp="true" verbose="true"/>
                <taskdef name="autoupdate" classname="org.netbeans.nbbuild.AutoUpdate" classpath="${{tasks.jar}}"/>
                <autoupdate installdir="${{nbplatform.active.dir}}" updatecenter="${{autoupdate.catalog.url}}">
                    <modules includes="${{module.includes}}.*" clusters="${{download.clusters}}"/>
                    <modules includes="org[.]netbeans[.]modules[.]apisupport[.]harness" clusters="harness"/>
                </autoupdate>
            </target>
        </project>
    </xsl:template>
</xsl:stylesheet>
