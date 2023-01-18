/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javaee.wildfly.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.javaee.wildfly.WildflyDeploymentManager;
import org.netbeans.modules.javaee.wildfly.nodes.actions.Refreshable;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 *
 * @author Emmanuel Hugonnet (ehsavoie) <ehsavoie@netbeans.org>
 */
public class WildflyEjbComponentsChildren extends WildflyAsyncChildren implements Refreshable {

    private static final Logger LOGGER = Logger.getLogger(WildflyEjbModulesChildren.class.getName());

    private final Lookup lookup;
    private final String deployment;
    private final List<WildflyEjbComponentNode> ejbsComponents;

    public WildflyEjbComponentsChildren(Lookup lookup, String deployment, List<WildflyEjbComponentNode> ejbs) {
        this.lookup = lookup;
        this.deployment = deployment;
        this.ejbsComponents = new ArrayList<WildflyEjbComponentNode>(ejbs.size());
        this.ejbsComponents.addAll(ejbs);
    }

    @Override
    public void updateKeys() {
        setKeys(new Object[]{Util.WAIT_NODE});
        getExecutorService().submit(new WildflyDestinationsNodeUpdater(), 0);

    }

    class WildflyDestinationsNodeUpdater implements Runnable {

        List keys = new ArrayList();

        @Override
        public void run() {
            try {
                WildflyDeploymentManager dm = lookup.lookup(WildflyDeploymentManager.class);
                keys.addAll(dm.getClient().listDestinationForDeployment(lookup, deployment));
                keys.addAll(ejbsComponents);
            } catch (Exception ex) {
                LOGGER.log(Level.INFO, null, ex);
            }

            setKeys(keys);
        }
    }

    @Override
    protected void addNotify() {
        updateKeys();
    }

    @Override
    protected void removeNotify() {
        setKeys(java.util.Collections.EMPTY_SET);
    }

    @Override
    protected org.openide.nodes.Node[] createNodes(Object key) {
        if (key instanceof WildflyDestinationNode) {
            return new Node[]{(WildflyDestinationNode) key};
        }
        if (key instanceof WildflyEjbComponentNode) {
            return new Node[]{(WildflyEjbComponentNode) key};
        }

        if (key instanceof String && key.equals(Util.WAIT_NODE)) {
            return new Node[]{Util.createWaitNode()};
        }
        return null;
    }

}
