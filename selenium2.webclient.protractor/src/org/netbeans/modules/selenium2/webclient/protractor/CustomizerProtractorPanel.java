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
package org.netbeans.modules.selenium2.webclient.protractor;

import java.awt.EventQueue;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.modules.selenium2.webclient.protractor.preferences.ProtractorPreferences;
import org.netbeans.modules.web.clientproject.spi.CustomizerPanelImplementation;
import org.openide.util.NbBundle;

/**
 *
 * @author Theofanis Oikonomou
 */
@NbBundle.Messages("CustomizerProtractorPanel.displayName=Protractor")
public class CustomizerProtractorPanel implements CustomizerPanelImplementation {
    
    public static final String IDENTIFIER = "Protractor"; // NOI18N

    private final Project project;

    // creation @GuardedBy("this")
    private volatile CustomizerProtractor customizerProtractor;


    public CustomizerProtractorPanel(Project project) {
        assert project != null;
        this.project = project;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getDisplayName() {
        return Bundle.CustomizerProtractorPanel_displayName();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        getComponent().addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        getComponent().removeChangeListener(listener);
    }

    @Override
    public synchronized CustomizerProtractor getComponent() {
        if (customizerProtractor == null) {
            customizerProtractor = new CustomizerProtractor(project);
        }
        return customizerProtractor;
    }

    @Override
    public boolean isValid() {
        return getErrorMessage() == null;
    }

    @Override
    public String getErrorMessage() {
        return getComponent().getErrorMessage();
    }

    @Override
    public String getWarningMessage() {
        return getComponent().getWarningMessage();
    }

    @Override
    public void save() {
        assert !EventQueue.isDispatchThread();
        assert customizerProtractor != null;
        ProtractorPreferences.setProtractor(project, customizerProtractor.getProtractor());
        ProtractorPreferences.setUserConfigurationFile(project, customizerProtractor.getUserConfigurationFile());
    }

}

