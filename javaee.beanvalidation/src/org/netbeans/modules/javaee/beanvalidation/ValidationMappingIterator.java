/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javaee.beanvalidation;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.netbeans.api.j2ee.core.Profile;
import org.netbeans.api.project.Project;
import org.netbeans.modules.j2ee.common.dd.DDHelper;
import org.netbeans.modules.javaee.beanvalidation.api.BeanValidationConfig;
import org.netbeans.modules.javaee.beanvalidation.spi.BeanValidationConfigProvider;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.TemplateWizard;

/**
 *
 * @author alexeybutenko
 */
public class ValidationMappingIterator extends AbstractIterator{
    private static final String defaultName = "constraint";   //NOI18N

    public Set<DataObject> instantiate(TemplateWizard wizard) throws IOException {
        String targetName = Templates.getTargetName(wizard);
        FileObject targetDir = Templates.getTargetFolder(wizard);

        FileObject fo = DDHelper.createConstraintXml(Profile.JAVA_EE_6_FULL, targetDir, targetName);
        if (fo != null) {
            Project project = Templates.getProject(wizard);
            registerConstraint(project, fo);
            return Collections.singleton(DataObject.find(fo));
        } else {
            return Collections.<DataObject>emptySet();
        }
    }

    @Override
    public String getDefaultName() {
        return defaultName;
    }

    /**
     * Register constraint in the validation.xml
     * @param fo
     */
    private void registerConstraint(Project project, FileObject fo) {
        BeanValidationConfigProvider provider = BeanValidationConfigProvider.getDefault();
        if (provider != null) {
            List<BeanValidationConfig> configList = provider.getConfigs(project);
            if (!configList.isEmpty()) {
                BeanValidationConfig config = configList.get(0);
                config.addConstraintMapping(fo);
            }
        }
    }

}
