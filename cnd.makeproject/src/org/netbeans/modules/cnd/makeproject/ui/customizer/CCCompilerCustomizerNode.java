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
package org.netbeans.modules.cnd.makeproject.ui.customizer;

import java.util.ArrayList;
import java.util.List;
import org.netbeans.modules.cnd.makeproject.api.configurations.Configuration;
import org.netbeans.modules.cnd.makeproject.api.configurations.Folder;
import org.netbeans.modules.cnd.makeproject.api.configurations.Item;
import org.netbeans.modules.cnd.makeproject.api.configurations.ItemConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.ui.CustomizerNode;
import org.openide.nodes.Sheet;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

class CCCompilerCustomizerNode extends CustomizerNode {

    public CCCompilerCustomizerNode(String name, String displayName, CustomizerNode[] children, Lookup lookup) {
        super(name, displayName, children, lookup);
    }

    @Override
    public Sheet[] getSheets(Configuration configuration) {
        switch (getContext().getKind()) {
            case Item:
                SharedItemConfiguration[] configurations = getContext().getItems();
                List<Sheet> itemSheets = new ArrayList<>();
                for (SharedItemConfiguration cfg : configurations) {
                    ItemConfiguration itemConfiguration = cfg.getItemConfiguration(configuration);
                    if (itemConfiguration != null) {
                        Item item = cfg.getItem();
                        itemSheets.add(itemConfiguration.getCCCompilerConfiguration().getSheet((MakeConfiguration) configuration, null, item));
                    }
                }
                return itemSheets.toArray(new Sheet[itemSheets.size()]);
            case Folder:
                Folder[] folders = getContext().getFolders();
                List<Sheet> folderSheets = new ArrayList<>();
                for (Folder folder: folders) {
                    Sheet sheet = folder.getFolderConfiguration(configuration).getCCCompilerConfiguration().getSheet((MakeConfiguration) configuration, folder, null);
                    folderSheets.add(sheet);
                }
                return folderSheets.toArray(new Sheet[folderSheets.size()]);
            case Project:
                return new Sheet[]{((MakeConfiguration) configuration).getCCCompilerConfiguration().getSheet((MakeConfiguration) configuration, null, null)};
        }
        return null;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(getContext().isCompilerConfiguration() ? "ProjectPropsCompiling" : "ProjectPropsParser"); // NOI18N
    }
}
