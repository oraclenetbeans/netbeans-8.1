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
package org.netbeans.modules.cnd.makeproject.ui;

import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.modules.cnd.makeproject.api.MakeCustomizerProvider;
import org.netbeans.modules.cnd.makeproject.api.configurations.Folder;
import org.netbeans.modules.cnd.makeproject.api.configurations.Item;
import org.netbeans.modules.cnd.makeproject.ui.customizer.MakeContext;
import org.netbeans.modules.cnd.utils.FSPath;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

public class PropertiesItemAction extends NodeAction {

    @Override
    protected boolean enable(Node[] activatedNodes) {
        if (activatedNodes.length == 0) {
            return false;
        }
        Project golden = (Project) activatedNodes[0].getValue("Project");// NOI18N
        if (golden == null) {
            return false;
        }
        for (int i = 1; i < activatedNodes.length; i++) {
            if (!golden.equals((Project) activatedNodes[i].getValue("Project"))) {// NOI18N
                return false;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(getClass(), "CTL_PropertiesItemActionName"); // NOI18N
    }

    @Override
    public void performAction(Node[] activatedNodes) {
        List<Item> list = new ArrayList<>();
        MakeCustomizerProvider best = null;
        for (int i = 0; i < activatedNodes.length; i++) {
            Node n = activatedNodes[i];
            Folder folder = (Folder) n.getValue("Folder"); // NOI18N
            Item item = (Item) n.getValue("Item"); // NOI18N
            Project project = (Project) n.getValue("Project"); // NOI18N
            if (project == null) {
                continue; // FIXUP
            }
            MakeCustomizerProvider cp = project.getLookup().lookup(MakeCustomizerProvider.class);
            if (cp == null) {
                continue; // FIXUP
            }
            if (best == null) {
                best = cp;
            }
            list.add(item);
            //dumpNativeFileInfo(item);
        }
        if (best != null) {
            best.showCustomizer(best.getLastCurrentNodeName(MakeContext.Kind.Item), list, null);
        }
    }

    private void dumpNativeFileInfo(Item item) {
        System.out.println("---------------------------------------------------------- " + item.getPath()); // NOI18N
        dumpPathsList("SystemIncludePaths", item.getSystemIncludePaths()); // NOI18N
        dumpPathsList("UserIncludePaths", item.getUserIncludePaths()); // NOI18N
        dumpList("SystemMacroDefinitions", item.getSystemMacroDefinitions()); // NOI18N
        dumpList("UserMacroDefinitions", item.getUserMacroDefinitions()); // NOI18N
    }

    public void dumpList(String txt, List<String> list) {
        for (String s : list) {
            System.out.println(txt + ":" + s); // NOI18N
        }
    }

    private void dumpPathsList(String txt, List<FSPath> list) {
        for (FSPath s : list) {
            System.out.println(txt + ":" + s.getURL()); // NOI18N
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}
