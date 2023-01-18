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
 * Software is Sun Microsystems, Inc. Portions Copyright 2006 Sun
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

package org.openide.awt;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Jaroslav Tulach
 */
public class CallbackActionTest extends NbTestCase {
    private FileObject folder;
    
    public CallbackActionTest(String testName) {
        super(testName);
    }
    
    @Override
    protected boolean runInEQ() {
        return true;
    }
    
    @Override
    protected void setUp() throws Exception {
        folder = FileUtil.getConfigFile("actions/support/test");
        assertNotNull("testing layer is loaded: ", folder);
    }
    
    public void testKeyMustBeProvided() {
        String key = null;
        Action defaultDelegate = null;
        Lookup context = Lookup.EMPTY;
        
        ContextAwareAction expResult = null;
        try {
            ContextAwareAction result = Actions.callback(key, defaultDelegate, false,
                    "displayName", "org/openide/awt/TestIcon.png", false);
            fail("Shall fail as key is null");
        } catch (NullPointerException ex) {
            // ok
        }
    }
    
    public void testCallback() throws Exception {
        FileObject fo = folder.getFileObject("testCallback.instance");
        
        Object obj = fo.getAttribute("instanceCreate");
        if (!(obj instanceof Action)) {
            fail("Shall create an action: " + obj);
        }
    }

    public void testCopyLikeProblem() throws Exception {
        FileObject fo = folder.getFileObject("testCopyLike.instance");

        Object obj = fo.getAttribute("instanceCreate");
        if (!(obj instanceof Action)) {
            fail("Shall create an action: " + obj);
        }

        InstanceContent ic = new InstanceContent();
        AbstractLookup l = new AbstractLookup(ic);
        ActionMap map = new ActionMap();
        map.put("copy-to-clipboard", new MyAction());
        ic.add(map);

        CntListener list = new CntListener();
        Action clone = ((ContextAwareAction)obj).createContextAwareInstance(l);
        clone.addPropertyChangeListener(list);
        assertTrue("Enabled", clone.isEnabled());

        ic.remove(map);
        assertFalse("Disabled", clone.isEnabled());
        list.assertCnt("one change", 1);
    }
    
    
    public void testWithFallback() throws Exception {
        MyAction myAction = new MyAction();
        MyAction fallAction = new MyAction();
        
        ActionMap other = new ActionMap();
        ActionMap tc = new ActionMap();
        tc.put("somekey", myAction);
        
        InstanceContent ic = new InstanceContent();
        AbstractLookup al = new AbstractLookup(ic);
        ic.add(tc);
        
        ContextAwareAction a = callback("somekey", fallAction, al, false);
        CntListener l = new CntListener();
        a.addPropertyChangeListener(l);

        assertTrue("My action is on", myAction.isEnabled());
        assertTrue("Callback is on", a.isEnabled());
        
        l.assertCnt("No change yet", 0);
        
        ic.remove(tc);
        assertTrue("fall is on", fallAction.isEnabled());
        assertTrue("My is on as well", a.isEnabled());

        l.assertCnt("Still enabled, so no change", 0);
        
        fallAction.setEnabled(false);
        
        l.assertCnt("Now there was one change", 1);
        
        assertFalse("fall is off", fallAction.isEnabled());
        assertFalse("My is off as well", a.isEnabled());
        
        
        Action a2 = a.createContextAwareInstance(Lookup.EMPTY);
        assertEquals("Both actions are equal", a, a2);
        assertEquals("and have the same hash", a.hashCode(), a2.hashCode());
    }
    
    static ContextAwareAction callback(String key, AbstractAction fallAction, Lookup al, boolean b) {
        return GeneralAction.callback(key, fallAction, al, b, false);
    }
    
    private static final class CntListener extends Object
            implements PropertyChangeListener {
        private int cnt;
        
        public void propertyChange(PropertyChangeEvent evt) {
            cnt++;
        }
        
        public void assertCnt(String msg, int count) {
            assertEquals(msg, count, this.cnt);
            this.cnt = 0;
        }
    } // end of CntListener

    class MyAction extends AbstractAction {

        public int cntEnabled;
        public int cntPerformed;

        @Override
        public boolean isEnabled() {
            cntEnabled++;
            return super.isEnabled();
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            cntPerformed++;
        }
    } // end of MyAction
    
}
