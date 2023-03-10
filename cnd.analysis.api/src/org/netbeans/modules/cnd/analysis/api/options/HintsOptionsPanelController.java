/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
package org.netbeans.modules.cnd.analysis.api.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.modules.cnd.api.model.syntaxerr.CodeAuditProvider;
import org.netbeans.modules.cnd.utils.MIMENames;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

public class HintsOptionsPanelController extends OptionsPanelController {
    
    private HintsPanel panel;
    
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;
                    
    @Override
    public void update() {
        if (panel != null) {
            panel.update();
        }
    }
    
    @Override
    public void applyChanges() {
        if ( isChanged() ) {
            panel.applyChanges();
        }
    }
    
    @Override
    public void cancel() {
        if (panel != null) {
            panel.cancel();
        }
    }
    
    @Override
    public boolean isValid() {
        return true; 
    }
    
    @Override
    public boolean isChanged() {
        return panel == null ? false : panel.isChanged();
    }
    
    @Override
    public HelpCtx getHelpCtx() {
	return new HelpCtx("netbeans.optionsDialog.java.hints"); //NOI18N
    }
    
    protected HintsPanel createPanel(Lookup masterLookup, CodeAuditProvider selection) {
        return new HintsPanel(masterLookup, selection, selection == null ? MIMENames.SOURCES_MIME_TYPE : selection.getMimeType());
    }
    
    @Override
    protected void setCurrentSubcategory(String subpath) {
        if (panel != null) {
            panel.selectPath(subpath);
        }
    }
    
    @Override
    public synchronized JComponent getComponent(Lookup masterLookup) {
        if ( panel == null ) {
            panel = createPanel(masterLookup, null);
        }
        return panel;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
	pcs.addPropertyChangeListener(l);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
	pcs.removePropertyChangeListener(l);
    }
        
    void changed() {
	if (!changed) {
	    changed = true;
	    pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
	}
	pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }
    
}
