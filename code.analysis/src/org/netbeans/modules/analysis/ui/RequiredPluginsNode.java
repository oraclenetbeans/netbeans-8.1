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
package org.netbeans.modules.analysis.ui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.analysis.DescriptionReader;
import org.netbeans.modules.analysis.SPIAccessor;
import org.netbeans.modules.analysis.Utils;
import org.netbeans.modules.analysis.spi.Analyzer.MissingPlugin;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author lahvac
 */
public class RequiredPluginsNode extends AbstractNode {

    private final Collection<? extends MissingPlugin> requiredPlugins;
    
    @Messages({"DN_MissingPlugins=Required Plugins Missing",
               "#{0}: the list of missing plugins",
               "DESC_MissingPlugins=<html>The following plugins that are required to fully analyze the selected scope were missing:<ul>{0}</ul>"})
    public RequiredPluginsNode(final Collection<? extends MissingPlugin> requiredPlugins) {
        super(Children.LEAF, Lookups.fixed(new DescriptionReader() {
            @Override public CharSequence getDescription() {
                StringBuilder missingPlugins = new StringBuilder();
                
                for (MissingPlugin p : requiredPlugins) {
                    missingPlugins.append("<li>").append(SPIAccessor.ACCESSOR.getDisplayName(p)).append("</li>");
                }
                
                return Bundle.DESC_MissingPlugins(missingPlugins.toString());
            }
        }));
        this.requiredPlugins = new ArrayList<MissingPlugin>(requiredPlugins);
        setDisplayName(Bundle.DN_MissingPlugins());
        setIconBaseWithExtension("org/netbeans/modules/analysis/ui/resources/warning.gif");
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            new ActionImpl(requiredPlugins)
        };
    }
    
    private static final class ActionImpl extends AbstractAction {

        private Collection<? extends MissingPlugin> requiredPlugins;
        @Messages("LBL_InstallPlugins=Install Missing Plugins...")
        public ActionImpl(Collection<? extends MissingPlugin> requiredPlugins) {
            this.putValue(NAME, Bundle.LBL_InstallPlugins());
            this.requiredPlugins = requiredPlugins;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Utils.installMissingPlugins(requiredPlugins);
        }
        
    }
    
}
