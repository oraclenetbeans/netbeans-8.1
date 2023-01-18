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
package org.netbeans.modules.selenium2.webclient.ui.customizer;

import java.beans.PropertyChangeSupport;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
//import org.netbeans.modules.selenium2.webclient.mocha.preferences.Selenium2Preferences;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Theofanis Oikonomou
 */
public final class Selenium2ProjectSupport {

    static final Logger LOGGER = Logger.getLogger(Selenium2ProjectSupport.class.getName());

    static final RequestProcessor RP = new RequestProcessor(Selenium2ProjectSupport.class);

    final Project project;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
//    final Selenium2Preferences preferences;
    
    private Selenium2ProjectSupport(Project project) {
        assert project != null;
        this.project = project;
//        preferences = new Selenium2Preferences(project);
    }

    @ProjectServiceProvider(service = Selenium2ProjectSupport.class, projectType = "org-netbeans-modules-web-clientproject") // NOI18N
    public static Selenium2ProjectSupport create(Project project) {
        Selenium2ProjectSupport support = new Selenium2ProjectSupport(project);
        return support;
    }

    public static Selenium2ProjectSupport forProject(Project project) {
        Selenium2ProjectSupport support = project.getLookup().lookup(Selenium2ProjectSupport.class);
        assert support != null : "Selenium2ProjectSupport should be found in project " + project.getClass().getName() + " (lookup: " + project.getLookup() + ")";
        return support;
    }

//    public Selenium2Preferences getPreferences() {
//        return preferences;
//    }
    
}
