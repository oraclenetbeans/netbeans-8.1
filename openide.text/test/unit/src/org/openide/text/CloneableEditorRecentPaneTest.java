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

package org.openide.text;

import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.text.EditorKit;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.netbeans.junit.NbTestCase;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.windows.CloneableOpenSupport;
import org.openide.windows.CloneableTopComponent;

/**
 * Test that CloneableEditorSupport.getRecentPane returns correct pane for cloned editors.
 *
 * @author Marek Slama
 */
public class CloneableEditorRecentPaneTest extends NbTestCase
implements CloneableEditorSupport.Env {

    public static Test suite() {
        return GraphicsEnvironment.isHeadless() ? new TestSuite() : new TestSuite(CloneableEditorRecentPaneTest.class);
    }

    static {
        System.setProperty("org.openide.windows.DummyWindowManager.VISIBLE", "false");
    }
    /** the support to work with */
    private transient CES support;

    // Env variables
    private transient String content = "";
    private transient boolean valid = true;
    private transient boolean modified = false;
    /** if not null contains message why this document cannot be modified */
    private transient String cannotBeModified;
    private transient Date date = new Date ();
    private transient List/*<PropertyChangeListener>*/ propL = new ArrayList ();
    private transient VetoableChangeListener vetoL;
    
    private static CloneableEditorRecentPaneTest RUNNING;
    
    public CloneableEditorRecentPaneTest(String s) {
        super(s);
    }
    
    protected void setUp () {
        support = new CES (this, Lookup.EMPTY);
        RUNNING = this;
    }
    
    protected boolean runInEQ() {
        return true;
    }
    
    private Object writeReplace () {
        return new Replace ();
    }
    
    /**
     * Test that CloneableEditorSupport.getRecentPane returns correct pane for cloned editors.
     *
     * @throws Exception
     */
    public void testEditorClones () throws Exception {
        support.open ();
        JEditorPane[] panes = support.getOpenedPanes();
        assertNotNull(panes);
        assertEquals(panes.length, 1);
        assertNotNull(panes[0]);
        CloneableEditor ed1 = (CloneableEditor) support.getRef().getArbitraryComponent();

        CloneableEditor ed2 = (CloneableEditor) ed1.clone();
        ed2.open();

        panes = support.getOpenedPanes();
        assertNotNull(panes);
        assertEquals(panes.length, 2);
        assertNotNull(panes[0]);
        assertNotNull(panes[1]);

        CloneableEditor ed3 = (CloneableEditor) ed1.clone();
        ed3.open();

        panes = support.getOpenedPanes();
        assertNotNull(panes);
        assertEquals(panes.length, 3);
        assertNotNull(panes[0]);
        assertNotNull(panes[1]);
        assertNotNull(panes[2]);

        JEditorPane pane1, pane2;
        ed1.requestActive();
        pane1 = ed1.getEditorPane();
        pane2 = support.getRecentPane();
        assertEquals(pane1, pane2);

        ed2.requestActive();
        pane1 = ed2.getEditorPane();
        pane2 = support.getRecentPane();
        assertEquals(pane1, pane2);

        ed3.requestActive();
        pane1 = ed3.getEditorPane();
        pane2 = support.getRecentPane();
        assertEquals(pane1, pane2);
    }
    
    //
    // Implementation of the CloneableEditorSupport.Env
    //
    
    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        propL.add (l);
    }    
    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        propL.remove (l);
    }
    
    public synchronized void addVetoableChangeListener(VetoableChangeListener l) {
        assertNull ("This is the first veto listener", vetoL);
        vetoL = l;
    }
    public void removeVetoableChangeListener(VetoableChangeListener l) {
        assertEquals ("Removing the right veto one", vetoL, l);
        vetoL = null;
    }
    
    public CloneableOpenSupport findCloneableOpenSupport() {
        return RUNNING.support;
    }
    
    public String getMimeType() {
        return "text/plain";
    }
    
    public Date getTime() {
        return date;
    }
    
    public InputStream inputStream() throws IOException {
        return new ByteArrayInputStream (content.getBytes ());
    }
    public OutputStream outputStream() throws IOException {
        class ContentStream extends ByteArrayOutputStream {
            public void close () throws IOException {
                super.close ();
                content = new String (toByteArray ());
            }
        }
        
        return new ContentStream ();
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public boolean isModified() {
        return modified;
    }

    public void markModified() throws IOException {
        if (cannotBeModified != null) {
            final String notify = cannotBeModified;
            IOException e = new IOException () {
                public String getLocalizedMessage () {
                    return notify;
                }
            };
            Exceptions.attachLocalizedMessage(e, cannotBeModified);
            throw e;
        }
        
        modified = true;
    }
    
    public void unmarkModified() {
        modified = false;
    }

    /** Implementation of the CES */
    private final class CES extends CloneableEditorSupport {
        public CES (Env env, Lookup l) {
            super (env, l);
        }
        
        public CloneableTopComponent.Ref getRef () {
            return allEditors;
        }
        
        protected String messageName() {
            return "Name";
        }
        
        protected String messageOpened() {
            return "Opened";
        }
        
        protected String messageOpening() {
            return "Opening";
        }
        
        protected String messageSave() {
            return "Save";
        }
        
        protected String messageToolTip() {
            return "ToolTip";
        }

        @Override
        protected EditorKit createEditorKit () {
            return new NbLikeEditorKit () {
                @Override
                public Void call() throws Exception {
                    super.call();
                    return null;
                }

            };
        }
        
    }

    private static final class Replace implements Serializable {
        public Object readResolve () {
            return RUNNING;
        }
    }
}
