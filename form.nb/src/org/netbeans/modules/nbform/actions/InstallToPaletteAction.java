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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.nbform.actions;

import java.util.Arrays;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.nbform.palette.BeanInstaller;
import org.netbeans.spi.project.ui.RecommendedTemplates;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**
 * InstallToPalette action - installs selected classes as beans to palette.
 *
 * @author   Ian Formanek
 */

public class InstallToPaletteAction extends NodeAction {

    private static String name;

    public InstallToPaletteAction () {
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
    }
    
    @Override
    public String getName() {
        if (name == null)
            name = org.openide.util.NbBundle.getBundle(InstallToPaletteAction.class)
                     .getString("ACT_InstallToPalette"); // NOI18N
        return name;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("beans.adding"); // NOI18N
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        BeanInstaller.installBeans(activatedNodes);
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        for (Node n: activatedNodes) {
            FileObject fobj = n.getLookup().lookup(FileObject.class);
            if (fobj == null || JavaSource.forFileObject(fobj) == null) {
                return false;
            }
            // Issue 73641
            Project project = FileOwnerQuery.getOwner(fobj);
            if (project != null) {
                RecommendedTemplates info = project.getLookup().lookup(RecommendedTemplates.class);
                if ((info != null) && !Arrays.asList(info.getRecommendedTypes()).contains("java-forms")) { // NOI18N
                    return false;
                }
            }
        }
        
        return true;
    }

}
