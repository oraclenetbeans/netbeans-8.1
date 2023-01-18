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
package org.netbeans.modules.diff.options;

import org.netbeans.modules.diff.*;
import org.netbeans.modules.diff.builtin.provider.BuiltInDiffProvider;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.Lookup;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * Diff module's Options Controller.
 * 
 * @author Maros Sandor
 */
@OptionsPanelController.SubRegistration(
    id=DiffOptionsController.OPTIONS_SUBPATH,
    displayName="#LBL_DiffOptions",
    keywords="#KW_DiffOptions",
    keywordsCategory="Advanced/Diff"
//    toolTip="#TT_DiffOptions"
)
public class DiffOptionsController extends OptionsPanelController {

    public static final String OPTIONS_SUBPATH = "Diff";

    private DiffOptionsPanel panel;
    
    @Override
    public void update() {
        panel.getInternalDiff().setSelected(DiffModuleConfig.getDefault().isUseInteralDiff());
        panel.getExternalDiff().setSelected(!DiffModuleConfig.getDefault().isUseInteralDiff());
        panel.getIgnoreWhitespace().setSelected(DiffModuleConfig.getDefault().getOptions().ignoreLeadingAndtrailingWhitespace);
        panel.getIgnoreInnerWhitespace().setSelected(DiffModuleConfig.getDefault().getOptions().ignoreInnerWhitespace);
        panel.getIgnoreCase().setSelected(DiffModuleConfig.getDefault().getOptions().ignoreCase);
        panel.getExternalCommand().setText(DiffModuleConfig.getDefault().getPreferences().get(DiffModuleConfig.PREF_EXTERNAL_DIFF_COMMAND, "diff {0} {1}")); // NOI18N
        panel.refreshComponents();
        panel.setChanged(false);
    }

    @Override
    public void applyChanges() {
        panel.checkExternalCommand();
        DiffModuleConfig.getDefault().setUseInteralDiff(panel.getInternalDiff().isSelected());
        BuiltInDiffProvider.Options options = new BuiltInDiffProvider.Options();
        options.ignoreLeadingAndtrailingWhitespace = panel.getIgnoreWhitespace().isSelected();
        options.ignoreInnerWhitespace = panel.getIgnoreInnerWhitespace().isSelected();
        options.ignoreCase = panel.getIgnoreCase().isSelected();
        DiffModuleConfig.getDefault().setOptions(options);
        DiffModuleConfig.getDefault().getPreferences().put(DiffModuleConfig.PREF_EXTERNAL_DIFF_COMMAND, panel.getExternalCommand().getText());
        panel.setChanged(false);
    }

    @Override
    public void cancel() {
        // nothing to do
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isChanged() {
        return panel.isChanged();
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        if (panel == null) {
            panel = new DiffOptionsPanel(); 
        }
        return panel;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("org.netbeans.modules.diff.options.DiffOptionsController"); //NOI18N
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }
}
