/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javafx2.platform.api;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.modules.javafx2.platform.Utils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Parameters;

/**
 * Type of JavaFX runtime in {@link JavaPlatform}.
 * @author Tomas Zezula
 * @author Petr Somol
 * @since 1.13
 */
public class JavaFxRuntimeInclusion {

    public enum Support {
    /**
     * No JavaFX runtime.
     */
        MISSING, 
    /**
     * JavaFX is a part of {@link JavaPlatform} but it's not
     * on classpath.
     */
        PRESENT, 
    /**
     * JavaFX is a part of {@link JavaPlatform} and it's a part
     * of platform classpath.
     */
        INCLUDED
    };
    
    private static final String PROP_PLATFORM_ANT_NAME = "platform.ant.name";   //NOI18N
    private static final String PROP_JAVA_HOME = "java.home";    //NOI18N
    private static final String SPEC_J2SE = "j2se"; //NOI18N

    private final Support support;
    private final List<String> artifacts;

    private JavaFxRuntimeInclusion(
            final Support supported,
            final List<String> artifacts
            ) {
        this.support = supported;
        this.artifacts = artifacts;
    }

    /**
     * Returns true if the JavaFX is supported.
     * @return true if there is jfxrt.jar installed
     */
    public boolean isSupported() {
        return support == Support.INCLUDED || support == Support.PRESENT;
    }

    /**
     * Returns true if the JavaFX runtime is on boot classpath.
     * @return true if jfxrt.jar is on boot classpath
     */
    public boolean isIncludedOnClassPath() {
        return support == Support.INCLUDED;
    }
    
    /**
     * Returns list of relative paths to artifacts that are needed
     * by FX Projects but that are not on boot classpath and need
     * to be added (jfxrt.jar in JDK7, javaws.jar..)
     * @return list of relative paths
     */
    public List<String> getExtensionArtifactPaths() {
        return artifacts;
    }

    /**
     * Returns {@link JavaFxRuntimeInclusion} for given {@link JavaPlatform}.
     * @param javaPlatform the {@link JavaPlatform} to return the {@link JavaFxRuntimeInclusion} for
     * @return the {@link JavaFxRuntimeInclusion}
     */
    @NonNull
    public static JavaFxRuntimeInclusion forPlatform(@NonNull final JavaPlatform javaPlatform) {
        Parameters.notNull("javaPlatform", javaPlatform);   //NOI18N
        boolean isDefault = JavaPlatform.getDefault().equals(javaPlatform);
        List<String> paths = new ArrayList<String>();
        Support runtimeSupport = Support.MISSING;
        String runtimePath = null;
        for(String runtimeLocation : Utils.getJavaFxRuntimeLocations()) {
            runtimePath = runtimeLocation + Utils.getJavaFxRuntimeArchiveName();
            runtimeSupport = forRuntime(javaPlatform, Utils.getJavaFxRuntimeSubDir() + runtimePath);
            if(runtimeSupport != Support.MISSING) {
                break;
            }
        }
        if(runtimeSupport != Support.MISSING && runtimePath != null) {
            if(runtimeSupport == Support.PRESENT) {
                paths.add((isDefault ? "" : Utils.getJavaFxRuntimeSubDir()) + runtimePath);
            }
            for(String optionalName : Utils.getJavaFxRuntimeOptionalNames()) {
                Support optionalSupport = Support.MISSING;
                String optionalPath = null;
                for(String optionalLocation : Utils.getJavaFxRuntimeLocations()) {
                    optionalPath = optionalLocation + optionalName;
                    optionalSupport = forRuntime(javaPlatform, Utils.getJavaFxRuntimeSubDir() + optionalPath);
                    if(optionalSupport == Support.PRESENT) {
                        break;
                    }
                }
                if(optionalSupport == Support.PRESENT && optionalPath != null) {
                    paths.add((isDefault ? "" : Utils.getJavaFxRuntimeSubDir()) + optionalPath);
                }
            }
        }
        return new JavaFxRuntimeInclusion(runtimeSupport, paths);
    }

    /**
     * Returns status of the artifact at relative path runtimePath in platform javaPlatform
     * @param javaPlatform the {@link JavaPlatform} where the artifact is to be searched for
     * @param runtimePath relative path to artifact
     * @return status of artifact presence/inclusion in platform
     */
    @NonNull
    private static Support forRuntime(@NonNull final JavaPlatform javaPlatform, @NonNull final String runtimePath) {
        Parameters.notNull("javaPlatform", javaPlatform);   //NOI18N
        Parameters.notNull("rtPath", runtimePath);   //NOI18N
        for (FileObject installFolder : javaPlatform.getInstallFolders()) {
            final FileObject jfxrtJar = installFolder.getFileObject(runtimePath);
            if (jfxrtJar != null  && jfxrtJar.isData()) {
                final URL jfxrtRoot = FileUtil.getArchiveRoot(jfxrtJar.toURL());
                for (ClassPath.Entry e : javaPlatform.getBootstrapLibraries().entries()) {
                    if (jfxrtRoot.equals(e.getURL())) {
                        return Support.INCLUDED;
                    }
                }
                return Support.PRESENT;
            }
        }
        return Support.MISSING;
    }
    
    /**
     * Returns the classpath entries which should be included into project's classpath
     * to include JavaFX on given platform.
     * @param javaPlatform for which the classpath entries should be created
     * @return the classpath entries separated by {@link java.io.File#pathSeparatorChar} to include
     * to project classpath or an empty string if JavaFX is already a part of the {@link JavaPlatform}s
     * classpath.
     * @throws IllegalArgumentException if given {@link JavaPlatform} does not support JavaFX or
     * the platform is not a valid J2SE platform.
     *
     * <p class="nonnormative">
     * Typical usage of this method is:
     * <pre>
     * {@code
     * if (JavaFxRuntimeInclusion.forPlatform(javaPlatform).isSupported()) {
     *      Set&lt;String&gt; cpEntries = JavaFxRuntimeInclusion.getProjectClassPathExtension(javaPlatform);
     *      if (cpEntries.length > 0) {
     *          appendToProjectClasspath(cpEntries);
     *      }
     * }
     * </pre>
     * </p>
     *
     */
    public static Set<String> getProjectClassPathExtension(@NonNull final JavaPlatform javaPlatform) {
        Parameters.notNull("javaPlatform", javaPlatform);   //NOI18N
        if (!SPEC_J2SE.equals(javaPlatform.getSpecification().getName())) {
            final Collection<? extends FileObject> installFolders = javaPlatform.getInstallFolders();
            throw new IllegalArgumentException(
                String.format(
                    "Java platform %s (%s) installed in %s is not a valid J2SE platform.",    //NOI18N
                    javaPlatform.getDisplayName(),
                    javaPlatform.getSpecification(),
                    installFolders.isEmpty() ?
                        "???" : //NOI18N
                        FileUtil.getFileDisplayName(installFolders.iterator().next())));
        }
        final JavaFxRuntimeInclusion inclusion = forPlatform(javaPlatform);
        if (!inclusion.isSupported()) {
            return new LinkedHashSet<String>();
        }
        List<String> artifacts = inclusion.getExtensionArtifactPaths();
        if(!artifacts.isEmpty()) {
            Set<String> extensionProp = new LinkedHashSet<String>();
            Iterator<String> i = artifacts.iterator();
            while(i.hasNext()) {
                String artifact = i.next();
                extensionProp.add(
                        String.format(
                            "${%s}/%s",  //NOI18N
                            getPlatformHomeProperty(javaPlatform),
                            artifact));
            }
            return extensionProp; //.toArray(new String[0]);
        }
        return new LinkedHashSet<String>();
    }

    /**
     * Returns name of property that should contain valid path to platform install folder
     * @param javaPlatform the {@link JavaPlatform} whose location the property contains
     * @return property name
     */
    @NonNull
    public static String getPlatformHomeProperty(@NonNull final JavaPlatform javaPlatform) {
        Parameters.notNull("javaPlatform", javaPlatform);   //NOI18N
        return javaPlatform.equals(JavaPlatformManager.getDefault().getDefaultPlatform()) ?
            PROP_JAVA_HOME :
            String.format(
                "platforms.%s.home",   //NOI18N
                javaPlatform.getProperties().get(PROP_PLATFORM_ANT_NAME));
    }

}
