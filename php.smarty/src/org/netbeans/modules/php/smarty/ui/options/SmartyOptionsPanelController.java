/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.php.smarty.ui.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.modules.php.smarty.editor.utlis.LexerUtils;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 * @author Martin Fousek
 */
@UiUtils.PhpOptionsPanelRegistration(
    id=SmartyOptionsPanelController.OPTIONS_ID,
    displayName="#LBL_PHPSmartyOptionsName",
    position=400
)
public class SmartyOptionsPanelController extends OptionsPanelController implements ChangeListener {
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public static final String OPTIONS_ID = "Smarty"; //NOI18N
    public static final String OPTIONS_SUB_PATH = UiUtils.FRAMEWORKS_AND_TOOLS_SUB_PATH + "/" + OPTIONS_ID; //NOI18N

    private SmartyOptionsPanel smartyOptionsPanel;
    private volatile boolean changed = false;

    @Override
    public void update() {
        getPanel().update();
        changed = false;
    }

    @Override
    public void applyChanges() {
        getPanel().applyChanges();
        changed = false;

        // accomplish manual relexing
        LexerUtils.relexerOpenedTpls();
    }

    @Override
    public void cancel() {
        getPanel().cancel();
    }

    @Override
    public boolean isValid() {
        return getPanel().valid();
    }

    @Override
    public boolean isChanged() {
        return getPanel().changed();
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    private synchronized SmartyOptionsPanel getPanel() {
        if (smartyOptionsPanel == null) {
            smartyOptionsPanel = new SmartyOptionsPanel();
            smartyOptionsPanel.addChangeListener(this);
        }
        return smartyOptionsPanel;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(SmartyOptions.class);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public void stateChanged(ChangeEvent e) {
        if (!changed) {
            changed = true;
            propertyChangeSupport.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        propertyChangeSupport.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }
}
