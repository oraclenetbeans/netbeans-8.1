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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven.navigator;

import java.util.Collection;
import javax.swing.JComponent;
import org.netbeans.modules.maven.api.Constants;
import static org.netbeans.modules.maven.navigator.Bundle.*;
import org.netbeans.spi.navigator.NavigatorPanel;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author mkleint
 */
@NavigatorPanel.Registration(mimeType=Constants.POM_MIME_TYPE, position=100, displayName="#POM_MODEL_NAME")
public class POMModelNavigator implements NavigatorPanel {
    private POMModelPanel component;
    
    protected Lookup.Result<DataObject> selection;

    protected final LookupListener selectionListener = new LookupListener() {
        @Override
        public void resultChanged(LookupEvent ev) {
            if(selection == null)
                return;
            navigate(selection.allInstances());
        }
    };
    

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(POMModelNavigator.class, "POM_MODEL_NAME");
    }

    @Override
    @Messages("POM_MODEL_HINT=View what values are inherited from parents or overriding parent values.")
    public String getDisplayHint() {
        return POM_MODEL_HINT();
    }

    @Override
    public JComponent getComponent() {
        return getNavigatorUI();
    }
    
    private POMModelPanel getNavigatorUI() {
        if (component == null) {
            component = new POMModelPanel();
        }
        return component;
    }

    @Override
    public void panelActivated(Lookup context) {
        getNavigatorUI().showWaitNode();
        selection = context.lookupResult(DataObject.class);
        selection.addLookupListener(selectionListener);
        selectionListener.resultChanged(null);
    }
    
    @Override
    public void panelDeactivated() {
        getNavigatorUI().showWaitNode();
        if(selection != null) {
            selection.removeLookupListener(selectionListener);
            selection = null;
        }
        getNavigatorUI().release();
    }

    @Override
    public Lookup getLookup() {
        return Lookup.EMPTY;
    }
    
    /**
     * 
     * @param selectedFiles 
     */

    public void navigate(Collection<? extends DataObject> selectedFiles) {
        if(selectedFiles.size() == 1) {
            DataObject d = (DataObject) selectedFiles.iterator().next();
            getNavigatorUI().navigate(d);           
        } else {
            getNavigatorUI().cleanup();
        }
    }
    

}
