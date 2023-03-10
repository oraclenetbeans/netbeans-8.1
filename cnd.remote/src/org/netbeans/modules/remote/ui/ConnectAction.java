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
package org.netbeans.modules.remote.ui;

import java.io.IOException;
import org.netbeans.modules.cnd.remote.support.RemoteUtil;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager.CancellationException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Vladimir Kvashin
 */
@ActionID(id = "org.netbeans.modules.remote.ui.ConnectAction", category = "NativeRemote")
@ActionRegistration(displayName = "ConnectMenuItem")
@ActionReference(path = "Remote/Host/Actions", name = "ConnectAction", position = 100)
public class ConnectAction extends SingleHostAction {
    
    private static final RequestProcessor RP = new RequestProcessor("ConnectAction", 1); // NOI18N

    @Override
    public String getName() {
        return NbBundle.getMessage(HostListRootNode.class, "ConnectMenuItem");
    }

    @Override
    protected boolean enable(ExecutionEnvironment env) {
        return !ConnectionManager.getInstance().isConnectedTo(env)
                || !HostNode.isOnline(env);
    }

    @Override
    public boolean isVisible(Node node) {
        ExecutionEnvironment env = node.getLookup().lookup(ExecutionEnvironment.class);        
        return env != null && env.isRemote();
    }


    @Override
    protected void performAction(final ExecutionEnvironment env, Node node) {
        RP.post(new Runnable() {
            @Override
            public void run() {
                connect(env);
            }
        });
    }

    private void connect(ExecutionEnvironment env) {
        try {
            ConnectionManager.getInstance().connectTo(env);
            RemoteUtil.checkSetupAfterConnection(env);
        } catch (IOException ex) {
            conectionFailed(env, ex);
        } catch (CancellationException ex) {
            conectionFailed(env, ex);
        }
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    private void conectionFailed(ExecutionEnvironment env, Exception e) {
        StatusDisplayer.getDefault().setStatusText(
                NbBundle.getMessage(ConnectAction.class, "UnableToConnectMessage", RemoteUtil.getDisplayName(env), e.getMessage()));

    }
}
