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


package org.netbeans.modules.derby;

import java.awt.Dialog;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.derby.ui.CreateDatabasePanel;
import org.netbeans.modules.derby.ui.CreateSampleDatabasePanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;


public class CreateSampleDBAction extends CallableSystemAction {

    private static final Logger LOG = Logger.getLogger(CreateSampleDBAction.class.getName());

    
    public CreateSampleDBAction() {
        putValue("noIconInMenu", Boolean.TRUE);
    }    
    
    public void performAction() {
        if (!Util.checkInstallLocation()) {
            return;
        }
        if (!Util.ensureSystemHome()) {
            return;
        }
        
        String derbySystemHome = DerbyOptions.getDefault().getSystemHome();
        CreateSampleDatabasePanel panel = new CreateSampleDatabasePanel(derbySystemHome);
        DialogDescriptor desc = new DialogDescriptor(panel, NbBundle.getMessage(CreateSampleDBAction.class, "LBL_CreateSampleDatabaseTitle"), true, null);
        desc.createNotificationLineSupport();
        panel.setDialogDescriptor(desc);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(desc);
        panel.setIntroduction();
        String acsd = NbBundle.getMessage(CreateSampleDBAction.class, "ACSD_CreateDatabaseAction");
        dialog.getAccessibleContext().setAccessibleDescription(acsd);
        dialog.setVisible(true);
        dialog.dispose();
        
        if (!DialogDescriptor.OK_OPTION.equals(desc.getValue())) {
            return;
        }
        
        String databaseName = panel.getDatabaseName();
        
        try {
            DerbyDatabasesImpl.getDefault().createSampleDatabase(databaseName, true);
        } catch (Exception e) {
            LOG.log(Level.INFO, null, e);
            LOG.log(Level.INFO, "", e);
            NotifyDescriptor nd = new NotifyDescriptor.Message(
                    "Failed to ceate sample database:\n"
                    + e.getLocalizedMessage(),
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notifyLater(nd);
        }
    }

    protected boolean asynchronous() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return Util.hasInstallLocation();
    }
    
    /** Gets localized name of action. Overrides superclass method. */
    public String getName() {
        return NbBundle.getBundle(CreateSampleDBAction.class).getString("LBL_CreateSampleDBAction");
    }

    /** Gets the action's help context. Implemenst superclass abstract method. */
    public HelpCtx getHelpCtx() {
        return new HelpCtx(CreateSampleDBAction.class);
    }

}
