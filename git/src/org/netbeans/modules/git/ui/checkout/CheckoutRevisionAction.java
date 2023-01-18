/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.git.ui.checkout;

import java.io.File;
import org.netbeans.libs.git.GitBranch;
import org.netbeans.modules.git.ui.repository.RepositoryInfo;
import org.netbeans.modules.git.ui.repository.RevisionDialogController;
import org.netbeans.modules.git.utils.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author ondra
 */
@ActionID(id = "org.netbeans.modules.git.ui.checkout.CheckoutRevisionAction", category = "Git")
@ActionRegistration(displayName = "#LBL_CheckoutRevisionAction_Name")
public class CheckoutRevisionAction extends AbstractCheckoutAction {

    private static final String ICON_RESOURCE = "org/netbeans/modules/git/resources/icons/checkout.png"; //NOI18N
    
    public CheckoutRevisionAction () {
        super(ICON_RESOURCE);
    }

    @Override
    protected String iconResource () {
        return ICON_RESOURCE;
    }
    
    @Override
    protected void performAction (File repository, File[] roots, VCSContext context) {
        RepositoryInfo info = RepositoryInfo.getInstance(repository);
        if (canCheckout(info)) {
            checkoutRevision(repository, info.getActiveBranch().getName().equals(GitBranch.NO_BRANCH) ? GitUtils.HEAD : info.getActiveBranch().getName());
        }
    }

    public void checkoutRevision (final File repository, String preselectedRevision) {
        checkoutRevision(repository, new CheckoutRevision(repository, RepositoryInfo.getInstance(repository), preselectedRevision), "LBL_CheckoutRevisionAction.progressName", //NOI18N
                new HelpCtx(CheckoutRevisionAction.class));
    }

    
    private static class CheckoutRevision extends AbstractCheckoutRevision {

        public CheckoutRevision (File repository, RepositoryInfo info, String initialRevision) {
            super(info, new RevisionDialogController(repository, new File[] { repository }, initialRevision));
            panel.jLabel1.setText(NbBundle.getMessage(CheckoutRevisionAction.class, "CheckoutRevision.jLabel1.text")); //NOI18N
        }

        @Override
        protected String getOkButtonLabel () {
            return NbBundle.getMessage(CheckoutRevisionAction.class, "LBL_CheckoutRevision.OKButton.text"); //NOI18N
        }

        @Override
        protected String getDialogTitle () {
            return NbBundle.getMessage(CheckoutRevisionAction.class, "LBL_CheckoutRevision.title"); //NOI18N
        }
    }
}
