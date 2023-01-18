/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.versioning.core.spi.testvcs;

import java.awt.event.ActionEvent;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;
import org.netbeans.modules.versioning.core.spi.VCSHistoryProvider;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;

/**
 *
 * @author tomas
 */
public class TestVCSHistoryProvider implements VCSHistoryProvider, VCSHistoryProvider.RevisionProvider {
    public static final String FILE_PROVIDES_REVISIONS_SUFFIX = "providesRevisions";
    public static TestVCSHistoryProvider instance;
    
    public boolean revisionProvided = false;
    public static HistoryEntry[] history;
    
    public TestVCSHistoryProvider() {
        instance = this;
    }
    

    public static void reset() {
        instance.history = null;
        instance.revisionProvided = false;
    }
    
    @Override
    public HistoryEntry[] getHistory(VCSFileProxy[] files, Date fromDate) {
        if(files[0].getName().endsWith(FILE_PROVIDES_REVISIONS_SUFFIX)) {
            return new VCSHistoryProvider.HistoryEntry[] {
                new VCSHistoryProvider.HistoryEntry(
                    files, 
                    new Date(System.currentTimeMillis()), 
                    "msg", 
                    "user", 
                    "username", 
                    "12345", 
                    "1234567890", 
                    new Action[] {new HistoryAwareAction()}, 
                    this)};
        }
        return history;
    }

    @Override
    public Action createShowHistoryAction(VCSFileProxy[] files) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }

    @Override
    public void addHistoryChangeListener(HistoryChangeListener l) {
        
    }

    @Override
    public void removeHistoryChangeListener(HistoryChangeListener l) {
        
    }

    @Override
    public void getRevisionFile(VCSFileProxy originalFile, VCSFileProxy revisionFile) {
        revisionProvided = true;
    }
    
    private class HistoryAwareAction extends AbstractAction implements ContextAwareAction {
        private Lookup context;
        @Override
        public void actionPerformed(ActionEvent e) {}
        @Override
        public Action createContextAwareInstance(Lookup actionContext) {
            this.context = actionContext;
            return this;
        }
    }
    
}
