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
package org.netbeans.modules.javafx2.editor.css;

import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.css.editor.module.spi.*;
import org.netbeans.modules.css.lib.api.CssModule;
import org.netbeans.modules.css.lib.api.properties.PropertyCategory;
import org.netbeans.modules.css.lib.api.properties.PropertyDefinition;
import org.netbeans.modules.javafx2.project.api.JavaFXProjectUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Java FX CSS editor
 *
 * @author Anton Chechel, Marek Fukala, Petr Somol
 * @version 2.0
 */
@NbBundle.Messages({
    "JavaFXCSSModule.displayName=JavaFX"
})
@ServiceProvider(service = CssEditorModule.class)
public class JavaFXCSSModule extends CssEditorModule implements CssModule {

    static ElementKind JFX_CSS_ELEMENT_KIND = ElementKind.GLOBAL;
    private static final String PROPERTIES_DEFINITION_PATH = "org/netbeans/modules/javafx2/editor/css/javafx2"; // NOI18N
    private static Map<String, PropertyDefinition> propertyDescriptors;
    private static SoftReference<Map<String, Boolean>> fileTypeCache;
    private static final String PSEUDO_CLASSES_PROPERTY = "@pseudo-classes"; // NOI18N
    private static Collection<String> pseudoClasses;
    private static Browser FX_BROWSER = new FxBrowser();
    
    @Override
    public Collection<String> getPseudoClasses(EditorFeatureContext context) {
        if(pseudoClasses == null) {
            pseudoClasses = new ArrayList<String>();
            PropertyDefinition prop = getJavaFXProperties().get(PSEUDO_CLASSES_PROPERTY);
            if(prop != null) {
                String grammar = prop.getGrammar();
                StringTokenizer tokenizer = new StringTokenizer(grammar, "| "); //NOI18N
                while(tokenizer.hasMoreTokens()) {
                    pseudoClasses.add(tokenizer.nextToken());
                }
            }
        }
        return pseudoClasses;
    }

    @Override
    public Collection<Browser> getExtraBrowsers(FileObject file) {
        return isJavaFXContext(file) ? Collections.singleton(FX_BROWSER) : null;
    }

    @Override
    public Collection<String> getPropertyNames(FileObject file) {
        return isJavaFXContext(file) ? getJavaFXProperties().keySet() : Collections.<String>emptyList();
    }

    @Override
    public PropertyDefinition getPropertyDefinition(String propertyName) {
        return getJavaFXProperties().get(propertyName);
    }

    private synchronized Map<String, PropertyDefinition> getJavaFXProperties() {
        if (propertyDescriptors == null) {
            propertyDescriptors = Utilities.parsePropertyDefinitionFile(PROPERTIES_DEFINITION_PATH, this);
        }
        return propertyDescriptors;
    }

    /**
     * Checks whether the file is standard CSS or FX CSS. Unfortunately this
     * can not be easily determined by file extension nor file contents as FX CSS
     * is a superset of CSS; a valid FX CSS file can be also a valid standard CSS file.
     * Here we decide based on file location - CSS files within a FX project are considered
     * to be FX CSS, all others are considered to be standard CSS.
     * 
     * @param file file context - may be null!
     * @return
     */
    private boolean isJavaFXContext(FileObject file) {
        if(file != null) {
            Map<String, Boolean> m;
            if(fileTypeCache == null) {
                m = new HashMap<String, Boolean>();
                fileTypeCache = new SoftReference<Map<String, Boolean>>( m );
            } else {
                m = fileTypeCache.get();
            }
            if(m != null) {
                Boolean b = m.get(file.getPath());
                if(b != null) {
                    return b.booleanValue();
                } else {
                    Project p = FileOwnerQuery.getOwner(file);
                    if(p != null) {
                        boolean isFX = JavaFXProjectUtils.isJavaFxEnabled(p) || JavaFXProjectUtils.isMavenFxProject(p);
                        m.put(file.getPath(), isFX);
                        return isFX;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "javafx2_css"; //NOI18N
    }

    @Override
    public String getDisplayName() {
        return Bundle.JavaFXCSSModule_displayName();
        
    }

    @Override
    public String getSpecificationURL() {
        return "http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html"; // NOI18N
    }
    
    private static class FxBrowser extends Browser {
        
        private static final String VENDOR = "Oracle"; // NOI18N
        private static final String NAME = "JavaFX"; // NOI18N
        private static final String RENDERING_ENGINE = "javafx"; // NOI18N
        private static final String PREFIX = "fx"; // NOI18N
        
        private static final String ICONS_LOCATION = "/org/netbeans/modules/javafx2/editor/resources/"; //NOI18N
        private static final String iconBase = "javafxicon"; // NOI18N
        private URL active, inactive;
      
        @Override
        public PropertyCategory getPropertyCategory() {
            return PropertyCategory.UNKNOWN;
        }

        @Override
        public String getVendor() {
            return VENDOR;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String getDescription() {
            return new StringBuilder().append(getVendor()).append(' ').append(getName()).toString(); // NOI18N
        }

        @Override
        public String getRenderingEngineId() {
            return RENDERING_ENGINE;
        }

        @Override
        public String getVendorSpecificPropertyId() {
            return PREFIX;
        }

        //why icon by an URL??? - its put to the generated html source this way:
        //         sb.append("<img src=\""); //NOI18N
        //         sb.append(browserIcon.toExternalForm());
        //         sb.append("\">"); // NOI18N

        @Override
        public synchronized URL getActiveIcon() {
            if(active == null) {
                active = FxBrowser.class.getResource(
                    ICONS_LOCATION + iconBase + ".png"); //NOI18N
            }
            return active;
        }

        @Override
        public synchronized URL getInactiveIcon() {
            if(inactive == null) {
                inactive = FxBrowser.class.getResource(
                    ICONS_LOCATION + iconBase + "-disabled.png"); //NOI18N
            }
            return inactive;
        }
    
    }
}
