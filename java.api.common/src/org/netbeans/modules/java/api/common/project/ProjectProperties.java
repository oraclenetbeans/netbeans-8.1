/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.java.api.common.project;

import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;


/** Helper class. Defines constants for properties. Knows the proper
 *  place where to store the properties.
 * 
 * @author Petr Hrebejk, Radko Najman, David Konecny
 * @since org.netbeans.modules.java.api.common/1 1.5
 */
public final class ProjectProperties {

    /**
     * @since org.netbeans.modules.java.api.common/0 1.14
     */
    public static final String ANNOTATION_PROCESSING_ENABLED = "annotation.processing.enabled"; //NOI18N
    /**
     * @since org.netbeans.modules.java.api.common/0 1.14
     */
    public static final String ANNOTATION_PROCESSING_ENABLED_IN_EDITOR = "annotation.processing.enabled.in.editor"; //NOI18N
    /**
     * @since org.netbeans.modules.java.api.common/0 1.14
     */
    public static final String ANNOTATION_PROCESSING_RUN_ALL_PROCESSORS = "annotation.processing.run.all.processors"; //NOI18N
    /**
     * @since org.netbeans.modules.java.api.common/0 1.14
     */
    public static final String ANNOTATION_PROCESSING_PROCESSORS_LIST = "annotation.processing.processors.list"; //NOI18N
    /**
     * @since org.netbeans.modules.java.api.common/0 1.14
     */
    public static final String ANNOTATION_PROCESSING_SOURCE_OUTPUT = "annotation.processing.source.output"; //NOI18N
    /**
     * @since org.netbeans.modules.java.api.common/0 1.15
     */
    public static final String ANNOTATION_PROCESSING_PROCESSOR_OPTIONS = "annotation.processing.processor.options"; //NOI18N
    public static final String JAVAC_CLASSPATH = "javac.classpath"; //NOI18N
    /**
     * @since org.netbeans.modules.java.api.common/0 1.14
     */
    public static final String JAVAC_PROCESSORPATH = "javac.processorpath"; //NOI18N
    public static final String JAVAC_TEST_CLASSPATH = "javac.test.classpath"; // NOI18N
    public static final String RUN_CLASSPATH = "run.classpath"; // NOI18N
    public static final String RUN_TEST_CLASSPATH = "run.test.classpath"; // NOI18N
    public static final String BUILD_CLASSES_DIR = "build.classes.dir"; //NOI18N
    public static final String BUILD_TEST_CLASSES_DIR = "build.test.classes.dir"; // NOI18N
    public static final String ENDORSED_CLASSPATH = "endorsed.classpath"; // NOI18N

    public static final String[] WELL_KNOWN_PATHS = new String[] {
        "${" + JAVAC_CLASSPATH + "}", // NOI18N
        "${" + JAVAC_PROCESSORPATH + "}", // NOI18N
        "${" + JAVAC_TEST_CLASSPATH + "}", // NOI18N
        "${" + RUN_CLASSPATH + "}", // NOI18N
        "${" + RUN_TEST_CLASSPATH + "}", // NOI18N
        "${" + BUILD_CLASSES_DIR + "}", // NOI18N
        "${" + ENDORSED_CLASSPATH + "}", // NOI18N
        "${" + BUILD_TEST_CLASSES_DIR + "}" // NOI18N
    };    
   
    // Prefixes and suffixes of classpath
    public static final String ANT_ARTIFACT_PREFIX = "${reference."; // NOI18N

    private static String RESOURCE_ICON_JAR = "org/netbeans/modules/java/api/common/project/ui/resources/jar.gif"; //NOI18N
    private static String RESOURCE_ICON_LIBRARY = "org/netbeans/modules/java/api/common/project/ui/resources/libraries.gif"; //NOI18N
    private static String RESOURCE_ICON_ARTIFACT = "org/netbeans/modules/java/api/common/project/ui/resources/projectDependencies.gif"; //NOI18N
    private static String RESOURCE_ICON_BROKEN_BADGE = "org/netbeans/modules/java/api/common/project/ui/resources/brokenProjectBadge.gif"; //NOI18N
    private static String RESOURCE_ICON_SOURCE_BADGE = "org/netbeans/modules/java/api/common/project/ui/resources/jarSourceBadge.png"; //NOI18N
    private static String RESOURCE_ICON_JAVADOC_BADGE = "org/netbeans/modules/java/api/common/project/ui/resources/jarJavadocBadge.png"; //NOI18N
    private static String RESOURCE_ICON_CLASSPATH = "org/netbeans/modules/java/api/common/project/ui/resources/referencedClasspath.gif"; //NOI18N
        
        
    public static ImageIcon ICON_JAR = ImageUtilities.loadImageIcon(RESOURCE_ICON_JAR, false);
    public static ImageIcon ICON_LIBRARY = ImageUtilities.loadImageIcon(RESOURCE_ICON_LIBRARY, false);
    public static ImageIcon ICON_ARTIFACT  = ImageUtilities.loadImageIcon(RESOURCE_ICON_ARTIFACT, false);
    public static ImageIcon ICON_BROKEN_BADGE  = ImageUtilities.loadImageIcon(RESOURCE_ICON_BROKEN_BADGE, false);
    public static ImageIcon ICON_JAVADOC_BADGE  = ImageUtilities.loadImageIcon(RESOURCE_ICON_JAVADOC_BADGE, false);
    public static ImageIcon ICON_SOURCE_BADGE  = ImageUtilities.loadImageIcon(RESOURCE_ICON_SOURCE_BADGE, false);
    public static ImageIcon ICON_CLASSPATH  = ImageUtilities.loadImageIcon(RESOURCE_ICON_CLASSPATH, false);

    public static final String INCLUDES = "includes"; // NOI18N
    public static final String EXCLUDES = "excludes"; // NOI18N

    //General
    /**
     * Name of the property holding the project sources encoding.
     * @since 1.60
     */
    public static final String SOURCE_ENCODING="source.encoding"; // NOI18N
    /**
     * Name of the property holding the active project platform.
     * @since 1.60
     */
    public static final String PLATFORM_ACTIVE = "platform.active"; //NOI18N
    /**
     * Name of the property holding the project main build script reference.
     * @since 1.60
     */
    public static final String BUILD_SCRIPT ="buildfile";      //NOI18N
    /**
     * Name of the property holding the project license.
     * @since 1.60
     */
    public static final String LICENSE_NAME = "project.license";
    /**
     * Name of the property holding the path to project license.
     * @since 1.60
     */
    public static final String LICENSE_PATH = "project.licensePath";

    //Build & Run
    /**
     * Name of the property disabling dependency tracking.
     * @since 1.60
     */
    public static final String NO_DEPENDENCIES="no.dependencies"; // NOI18N
    /**
     * Name of the property holding the debug project classpath.
     * @since 1.60
     */
    public static final String DEBUG_CLASSPATH = "debug.classpath"; //NOI18N
    /**
     * Name of the property holding the debug test classpath.
     * @since 1.60
     */
    public static final String DEBUG_TEST_CLASSPATH = "debug.test.classpath"; // NOI18N
    /**
     * Name of the property holding the reference to folder where test results should be generated.
     * @since 1.60
     */
    public static final String BUILD_TEST_RESULTS_DIR = "build.test.results.dir"; // NOI18N
    /**
     * Name of the property holding the reference to build generated sources.
     * @since 1.60
     */
    public static final String BUILD_GENERATED_SOURCES_DIR = "build.generated.sources.dir"; //NOI18N
    /**
     * Name of the property holding the build excludes.
     * @since 1.60
     */
    public static final String BUILD_CLASSES_EXCLUDES = "build.classes.excludes"; //NOI18N
    public static final String RUN_JVM_ARGS = "run.jvmargs"; // NOI18N
    static final String RUN_JVM_ARGS_IDE = "run.jvmargs.ide"; // NOI18N
    public static final String RUNTIME_ENCODING="runtime.encoding"; //NOI18N
    public static final String BUILD_DIR = "build.dir"; // NOI18N
    public static final String MAIN_CLASS = "main.class"; // NOI18N
    public static final String APPLICATION_ARGS = "application.args"; // NOI18N
    public static final String RUN_WORK_DIR = "work.dir"; // NOI18N

    public static final String SYSTEM_PROPERTIES_RUN_PREFIX = "run-sys-prop."; // NOI18N
    public static final String SYSTEM_PROPERTIES_TEST_PREFIX = "test-sys-prop."; // NOI18N

    public static final String PROP_PROJECT_CONFIGURATION_CONFIG = "config"; // NOI18N

    //Javac
    /**
     * Name of the property holding the javac extra args.
     * @since 1.60
     */
    public static final String JAVAC_COMPILERARGS = "javac.compilerargs"; //NOI18N
    /**
     * Name of the property holding the javac source.
     * @since 1.60
     */
    public static final String JAVAC_SOURCE = "javac.source"; //NOI18N
    /**
     * Name of the property holding the javac target.
     * @since 1.60
     */
    public static final String JAVAC_TARGET = "javac.target"; //NOI18N
    /**
     * Name of the property enabling javac deprecation.
     * @since 1.60
     */
    public static final String JAVAC_DEPRECATION = "javac.deprecation"; //NOI18N
    /**
     * Name of the property holding the javac profile.
     * @since 1.60
     */
    public static final String JAVAC_PROFILE = "javac.profile"; // NOI18N
    /**
     * Name of the property turning on javac debug info generation.
     * @since 1.60
     */
    public static final String JAVAC_DEBUG = "javac.debug"; // NOI18N

    //Jar
    /**
     * Name of the property holding the reference to built jar file.
     * @since 1.60
     */
    public static final String DIST_JAR ="dist.jar";    //NOI18N
    /**
     * Name of the property holding the reference to distribution directory.
     * @since 1.60
     */
    public static final String DIST_DIR ="dist.dir";    //NOI18N
    /**
     * Name of the property enabling jar compression.
     * @since 1.60
     */
    public static final String JAR_COMPRESS = "jar.compress";   //NOI18N
    /**
     * Name of the property holding files excluded from jar file.
     * @since 1.60
     */
    public static final String DIST_ARCHIVE_EXCLUDES = "dist.archive.excludes";   //NOI18N

    //Javadoc
    /**
     * Name of property holding reference to folder where JavaDoc is genered.
     * @since 1.60
     */
    public static final String DIST_JAVADOC_DIR = "dist.javadoc.dir"; // NOI18N
    /**
     * Name of property enabling JavaDoc for non public classes.
     * @since 1.60
     */
    public static final String JAVADOC_PRIVATE="javadoc.private"; // NOI18N
    /**
     * Name of property disabling javadoc class hierarchy generation.
     * @since 1.60
     */
    public static final String JAVADOC_NO_TREE="javadoc.notree"; // NOI18N
    /**
     * Name of property enabling creation of javadoc class and package usage pages.
     * @since 1.60
     */
    public static final String JAVADOC_USE="javadoc.use"; // NOI18N
    /**
     * Name of property disabling creation of javadoc navigation bar.
     * @since 1.60
     */
    public static final String JAVADOC_NO_NAVBAR="javadoc.nonavbar"; // NOI18N
    /**
     * Name of property disabling creation of javadoc index.
     * @since 1.60
     */
    public static final String JAVADOC_NO_INDEX="javadoc.noindex"; // NOI18N
    /**
     * Name of property enabling of javadoc split index.
     * @since 1.60
     */
    public static final String JAVADOC_SPLIT_INDEX="javadoc.splitindex"; // NOI18N
    /**
     * Name of property holding the javadoc author.
     * @since 1.60
     */
    public static final String JAVADOC_AUTHOR="javadoc.author"; // NOI18N
    /**
     * Name of property holding the javadoc version.
     * @since 1.60
     */
    public static final String JAVADOC_VERSION="javadoc.version"; // NOI18N
    /**
     * Name of property holding the javadoc window title.
     * @since 1.60
     */
    public static final String JAVADOC_WINDOW_TITLE="javadoc.windowtitle"; // NOI18N
    /**
     * Name of property holding the javadoc encoding.
     * @since 1.60
     */
    public static final String JAVADOC_ENCODING="javadoc.encoding"; // NOI18N
    /**
     * Name of property holding the javadoc additional parameters.
     * @since 1.60
     */
    public static final String JAVADOC_ADDITIONALPARAM="javadoc.additionalparam"; // NOI18N
    //Javadoc stored in the PRIVATE.PROPERTIES
    /**
     * Name of property enabling javadoc preview.
     * @since 1.60
     */
    public static final String JAVADOC_PREVIEW="javadoc.preview"; // NOI18N

    /** @since org.netbeans.modules.java.j2seproject/1 1.12 */
    public static final String DO_DEPEND = "do.depend"; // NOI18N
    /** @since org.netbeans.modules.java.j2seproject/1 1.12 */
    public static final String DO_JAR = "do.jar"; // NOI18N
    /** @since org.netbeans.modules.java.j2seproject/1 1.21 */
    public static final String COMPILE_ON_SAVE = "compile.on.save"; // NOI18N
    /** @since org.netbeans.modules.java.j2seproject/1 1.19 */
    public static final String COMPILE_ON_SAVE_UNSUPPORTED_PREFIX = "compile.on.save.unsupported"; // NOI18N

    //NB 6.1 tracking of files modifications
    public static final String TRACK_FILE_CHANGES="track.file.changes"; //NOI18N
}
