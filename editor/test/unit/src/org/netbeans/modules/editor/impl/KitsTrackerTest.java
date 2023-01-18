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
package org.netbeans.modules.editor.impl;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import javax.swing.text.EditorKit;
import org.netbeans.editor.BaseKit;
import org.netbeans.editor.ext.ExtKit;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.editor.EditorTestLookup;
import org.netbeans.modules.editor.NbEditorKit;
import org.netbeans.modules.editor.lib.KitsTracker;
import org.openide.filesystems.FileObject;

/**
 *
 * @author vita
 */
public class KitsTrackerTest extends NbTestCase {
    
    /** Creates a new instance of KitsTrackerTest */
    public KitsTrackerTest(String name) {
        super(name);
    }
    
    protected @Override void setUp() throws Exception {
        super.setUp();

        clearWorkDir();

        EditorTestLookup.setLookup(
            new URL[] {
                KitsTrackerTest.class.getResource("/org/netbeans/modules/editor/impl/KitsTracker-test-layer.xml"),
            },
            new Object[] {},
            getClass().getClassLoader()
        );
    }
    
    public void testFindKit() {
        String mimeType = KitsTrackerImpl.getInstance().findMimeType(TestAKit.class);
        assertEquals("Wrong mime type", "text/x-type-A", mimeType);
    }
    
    public void testFindKitFromSuper() {
        String mimeType = KitsTrackerImpl.getInstance().findMimeType(TestAChildKit.class);
        assertEquals("Wrong mime type", "text/x-type-A", mimeType);
    }

    public void testFindSharedKit() {
        String mimeType = KitsTrackerImpl.getInstance().findMimeType(SharedKit.class);
        assertNull("Should not get any mimetype", mimeType);
        
        List<String> mimeTypes = KitsTrackerImpl.getInstance().getMimeTypesForKitClass(SharedKit.class);
        assertEquals("Wrong number of mime types", 2, mimeTypes.size());
        assertTrue("Should be registered for text/x-type-B-1", mimeTypes.contains("text/x-type-B-1"));
        assertTrue("Should be registered for text/x-type-B-2", mimeTypes.contains("text/x-type-B-2"));
    }

    public void testFindKitForKnownSupers() {
        Class [] kitClasses = new Class [] {
            BaseKit.class, ExtKit.class, NbEditorKit.class
        };
        
        for(Class c : kitClasses) {
            String mimeType = KitsTrackerImpl.getInstance().findMimeType(c);
            assertNull("Shouldn't have mimetype for " + c, mimeType);
            
            List<String> mimeTypes = KitsTrackerImpl.getInstance().getMimeTypesForKitClass(c);
            assertEquals("Wrong number of mime types for " + c, 0, mimeTypes.size());
        }
    }
    
    public void testFindKitForNull() {
        String mimeType = KitsTrackerImpl.getInstance().findMimeType(null);
        assertEquals("Wrong mimetype for null", "", mimeType);

        List<String> mimeTypes = KitsTrackerImpl.getInstance().getMimeTypesForKitClass(null);
        assertEquals("Wrong number of mime types for null", 1, mimeTypes.size());
        assertEquals("Wrong mime type for null", "", mimeTypes.get(0));
    }
    
    // o.n.editor.BaseKit uses similar code
    public void testKitsTrackerCallable() throws Exception {
        Class clazz = getClass().getClassLoader().loadClass("org.netbeans.modules.editor.lib.KitsTracker"); //NOI18N
        Method getInstanceMethod = clazz.getDeclaredMethod("getInstance"); //NOI18N
        Method findMimeTypeMethod = clazz.getDeclaredMethod("findMimeType", Class.class); //NOI18N
        Object kitsTracker = getInstanceMethod.invoke(null);
        String mimeType = (String) findMimeTypeMethod.invoke(kitsTracker, EditorKit.class);
        assertNull("EditorKit.class should not have a mime type", mimeType);
    }

    public void testKitsTrackerImplInstalled() throws Exception {
        KitsTracker tracker = KitsTracker.getInstance();
        assertEquals("Wrong KitsTracker implementation installed", KitsTrackerImpl.class, tracker.getClass());
    }
    
    public static class TestAKit extends NbEditorKit {

        @Override
        public String getContentType() {
            return "text/x-type-A";
        }
        
    } // End of TestAKit class

    public static class TestAChildKit extends TestAKit {

    } // End of TestAChildKit class
    
    public static SharedKit sharedKit(FileObject f) {
        String mimeType = f.getParent().getPath().substring(8); //'Editors/'
        return new SharedKit(mimeType);
    }
    
    public static class SharedKit extends NbEditorKit {
        private final String mimeType;
        
        public SharedKit(String mimeType) {
            this.mimeType = mimeType;
        }
        
        @Override
        public String getContentType() {
            return mimeType;
        }
        
    } // End of SharedKit class

}
