/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javaee.wildfly.nodes;

import java.awt.Image;
import javax.swing.Action;
import org.netbeans.api.db.explorer.node.BaseNode;
import org.netbeans.modules.j2ee.deployment.common.api.Datasource;
import org.netbeans.modules.javaee.wildfly.nodes.actions.UndeployModuleAction;
import org.netbeans.modules.javaee.wildfly.nodes.actions.UndeployModuleCookieImpl;
import org.openide.actions.PropertiesAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author ehugonnet
 */
public class WildflyDatasourceNode extends AbstractNode {

    public WildflyDatasourceNode(String name, Datasource ds, Lookup lookup) {
        super(Children.LEAF);
        getCookieSet().add(new UndeployModuleCookieImpl(ds.getDisplayName(), lookup));
        setDisplayName(ds.getJndiName());
        setName(name);
        setShortDescription(ds.getDisplayName());
        initProperties(ds);
    }

    protected void initProperties(Datasource ds) {
        addProperty("Driver", ds.getDriverClassName());
        addProperty("JndiName", ds.getJndiName());
        addProperty("Url", ds.getUrl());
        addProperty("Username", ds.getUsername());
        addProperty("Password", ds.getPassword());
    }

    private void addProperty(String name, String value) {
        String displayName = NbBundle.getMessage(WildflyDatasourceNode.class, "LBL_Resources_Datasources_Datasource_" + name);
        String description = NbBundle.getMessage(WildflyDatasourceNode.class, "DESC_Resources_Datasources_Datasource_" + name);
        PropertySupport ps = new SimplePropertySupport(name, value, displayName, description);
        getSheet().get(Sheet.PROPERTIES).put(ps);
    }
    
    @Override
    protected Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        setSheet(sheet);
        return sheet;
    }

    @Override
    public Action[] getActions(boolean context) {
        return new SystemAction[]{
            SystemAction.get(PropertiesAction.class),
            SystemAction.get(UndeployModuleAction.class)
        };
    }

    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage(Util.JDBC_RESOURCE_ICON);
    }

    @Override
    public Image getOpenedIcon(int type) {
        return ImageUtilities.loadImage(Util.JDBC_RESOURCE_ICON);
    }
}
