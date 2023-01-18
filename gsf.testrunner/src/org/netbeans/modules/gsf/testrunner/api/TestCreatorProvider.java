/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.gsf.testrunner.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.gsf.testrunner.plugin.RootsProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 *
 * @author Theofanis Oikonomou
 */
public abstract class TestCreatorProvider {
    
    public static final String FRAMEWORK_JUNIT = "JUnit";
    public static final String FRAMEWORK_TESTNG = "TestNG";
    public static final String FRAMEWORK_SELENIUM = "Selenium";
    /**
     * @since 2.4
     */
    public static final String FRAMEWORK_PHP = "PHP";
    
    /**
     * @since 2.6
     */
    public static final String IDENTIFIER_JUNIT = "junit";
    public static final String IDENTIFIER_TESTNG = "testng";
    public static final String IDENTIFIER_SELENIUM = "selenium";
    public static final String IDENTIFIER_PHP = "php";
    
    /** suffix of test classes */
    public static final String TEST_CLASS_SUFFIX = "Test"; //NOI18N
    /** suffix of integration test classes */
    public static final String INTEGRATION_TEST_CLASS_SUFFIX = "IT"; //NOI18N
    /** suffix of TestNG classes */
    public static final String TESTNG_TEST_CLASS_SUFFIX = "NG"; //NOI18N
    
    /**
     * Registers a {@link TestCreatorProvider}.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Registration {

        /**
         * Identifier of the TestCreatorProvider. 
         * This should never change to facilitate e.g. usage statistics.
         * @return 
         * @since 2.6
         */
        String identifier();

        /**
         * Display name of the TestCreatorProvider.
         * @return 
         */
        String displayName();
    }
    
    /**
     *
     * @param activatedFOs list of {@link FileObject}s that triggered the test creation
     * @return {@code true} if test creation can be enabled in this context, {@code false} otherwise
     */
    public abstract boolean enable(FileObject[] activatedFOs);
    
    /**
     * Start creating the tests based on this Context
     * @param context contains needed information for creating the tests
     */
    public abstract void createTests(Context context);

    /**
     * Get the {@link SourceGroup} that contains the file.
     * @param file the file to search for
     * @param prj the project to get the list of {@link Sources} in which to search for the file
     * @return the {@link SourceGroup} that contains the file or {@code null}
     */
    public static SourceGroup getSourceGroup(FileObject file, Project prj) {
        Sources src = ProjectUtils.getSources(prj);
        String type = "";
        Collection<? extends RootsProvider> providers = Lookup.getDefault().lookupAll(RootsProvider.class);
        for (RootsProvider provider : providers) {
            type = provider.getSourceRootType();
            break;
        }
        SourceGroup[] srcGrps = src.getSourceGroups(type);
        for (SourceGroup srcGrp : srcGrps) {
            FileObject rootFolder = srcGrp.getRootFolder();
            if (((file == rootFolder) || FileUtil.isParentOf(rootFolder, file)) 
                    && srcGrp.contains(file)) {
                return srcGrp;
            }
        }
        return null;
    }

    /**
     * Holds needed information for creating the tests
     */
    public static final class Context {

        private boolean singleClass;
        private String testClassName;
        private FileObject targetFolder;
        private final FileObject[] activatedFOs;
        private boolean integrationTests;
        private Map<String, Object> configurationPanelProperties;
        
        /**
         *
         * @param activatedNodes list of {@link FileObject}s that triggered the test creation
         */
        public Context(FileObject[] activatedNodes) {
            this.activatedFOs = activatedNodes;
        }

        /**
         *
         * @return the list of {@link FileObject}s that triggered the test creation
         */
        public FileObject[] getActivatedFOs() {
            return activatedFOs;
        }

        /**
         * 
         * @return {@code true} if a test for a single class is to be created, {@code false} otherwise
         */
        public boolean isSingleClass() {
            return singleClass;
        }

        /**
         * 
         * @param singleClass {@code true} if a test for a single class is to be created, {@code false} otherwise
         */
        public void setSingleClass(boolean singleClass) {
            this.singleClass = singleClass;
        }

        /**
         *
         * @return the folder where the tests are to be created
         */
        public FileObject getTargetFolder() {
            return targetFolder;
        }

        /**
         *
         * @param targetFolder the folder where the tests are to be created
         */
        public void setTargetFolder(FileObject targetFolder) {
            this.targetFolder = targetFolder;
        }

        /**
         *
         * @return class name entered in the form, or null if the form did not contain the field for entering class name
         */
        public String getTestClassName() {
            return testClassName;
        }

        /**
         *
         * @param testClassName class name entered in the form, or null if the form did not contain the field for entering class name
         */
        public void setTestClassName(String testClassName) {
            this.testClassName = testClassName;
        }

        /**
         *
         * @return {@code true} if an integration test is to be created, {@code false} otherwise
         */
        public boolean isIntegrationTests() {
            return integrationTests;
        }

        /**
         *
         * @param integrationTests {@code true} if an integration test is to be created, {@code false} otherwise
         */
        public void setIntegrationTests(boolean integrationTests) {
            this.integrationTests = integrationTests;
        }
        
        /**
         * Set properties from configuration panel inside "Create Tests" dialog. 
         * These properties will be available whenever 
         * {@link TestCreatorProvider#createTests(org.netbeans.modules.gsf.testrunner.api.TestCreatorProvider.Context)}
         * is called.
         *
         * @param configurationPanelProperties map of properties from configuration panel inside "Create Tests" dialog
         */
        public void setConfigurationPanelProperties(Map<String, Object> configurationPanelProperties) {
            this.configurationPanelProperties = configurationPanelProperties;
        }
    
        /**
         * Get properties from configuration panel inside "Create Tests" dialog.
         *
         * @return map of properties from configuration panel inside "Create Tests" dialog
         */
        public Map<String, Object> getConfigurationPanelProperties() {
            return Collections.unmodifiableMap(configurationPanelProperties);
        }
        
    }
    
}
