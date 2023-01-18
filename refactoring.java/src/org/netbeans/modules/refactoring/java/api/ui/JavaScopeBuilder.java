/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.refactoring.java.api.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.modules.refactoring.api.Scope;
import org.netbeans.modules.refactoring.java.ui.scope.CustomScopePanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;

/**
 * Support for creating a new Scope. Opens a dialog to select
 * different parts of open projects to include in the new scope.
 * 
 * @author Ralph Ruijs
 * @since 1.27.0
 */
public final class JavaScopeBuilder {

    /**
     * Open a modal dialog to specify a new Scope.
     * 
     * @param title - the title of the dialog
     * @param scope - the scope to use to preselect parts
     * @return a new Scope or null if the dialog was canceled
     */
    public static Scope open(String title, final Scope scope) {
        final CustomScopePanel panel = new CustomScopePanel();
        final AtomicBoolean successful = new AtomicBoolean();

        DialogDescriptor dialogDescriptor = new DialogDescriptor(
                panel,
                title,
                true, // modal
                new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION},
                DialogDescriptor.CANCEL_OPTION,
                DialogDescriptor.DEFAULT_ALIGN,
                HelpCtx.DEFAULT_HELP,
                new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == DialogDescriptor.OK_OPTION) {
                    successful.set(true);
                }
            }
        });

        dialogDescriptor.setClosingOptions(new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION});

        new Thread(new Runnable() {

            @Override
            public void run() {
                panel.initialize(scope);
            }
        }).start();
        
        DialogDisplayer.getDefault().createDialog(dialogDescriptor).setVisible(true);
        
        if(successful.get()) {
            return panel.getCustomScope();
        }
        return null;
    }
}
