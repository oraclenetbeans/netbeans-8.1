/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.clientproject.ui.customizer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import org.netbeans.api.project.Project;
import org.netbeans.modules.web.common.api.WebUtils;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

public final class JsLibsCompositeCategoryProvider implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String CATEGORY_IDENT = "JsLibs"; // NOI18N
    private static final JComponent COMPONENT = new JLabel();

    private final boolean checkWebRoot;


    public JsLibsCompositeCategoryProvider() {
        this(false);
    }

    public JsLibsCompositeCategoryProvider(boolean checkWebRoot) {
        this.checkWebRoot = checkWebRoot;
    }

    @Override
    public ProjectCustomizer.Category createCategory(Lookup context) {
        // XXX
        assert false : "Unfortunately, this method gets never called...";
        if (checkWebRoot
                && !WebUtils.hasWebRoot(context.lookup(Project.class))) {
            return null;
        }
        return ProjectCustomizer.Category.create(
                CATEGORY_IDENT, NbBundle.getMessage(JsLibsCompositeCategoryProvider.class, "JsLibs.name"), null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
        return COMPONENT;
    }

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org.netbeans.modules.web.clientproject", // NOI18N
            category = CATEGORY_IDENT,
            categoryLabel = "#JsLibs.name", // NOI18N
            position = 200)
    public static ProjectCustomizer.CompositeCategoryProvider forHtml5Project() {
        return new JsLibsCompositeCategoryProvider();
    }

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-php-project", // NOI18N
            category = CATEGORY_IDENT,
            categoryLabel = "#JsLibs.name", // NOI18N
            position = 190)
    public static ProjectCustomizer.CompositeCategoryProvider forPhpProject() {
        return new JsLibsCompositeCategoryProvider();
    }

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-web-project", // NOI18N
            category = CATEGORY_IDENT,
            categoryLabel = "#JsLibs.name", // NOI18N
            position = 350)
    public static ProjectCustomizer.CompositeCategoryProvider forWebProject() {
        return new JsLibsCompositeCategoryProvider();
    }

    // XXX does not work, createCategory() is not called
    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-maven", // NOI18N
            category = CATEGORY_IDENT,
            categoryLabel = "#JsLibs.name", // NOI18N
            position = 258)
    public static ProjectCustomizer.CompositeCategoryProvider forMavenProject() {
        return new JsLibsCompositeCategoryProvider(true);
    }

}
