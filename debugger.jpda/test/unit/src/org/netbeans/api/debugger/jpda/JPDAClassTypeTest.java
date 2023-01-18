/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */

package org.netbeans.api.debugger.jpda;

import java.util.Arrays;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingConstants;
import junit.framework.Test;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.debugger.jpda.models.CallStackFrameImpl;

/**
 * A test of JPDAClassType class.
 *
 * @author Martin Entlicher
 */
public class JPDAClassTypeTest extends NbTestCase {

    private static final String APP_SRC_NAME = "org/netbeans/api/debugger/jpda/testapps/JPDAClassTypeTestApp.java"; // NOI18N
    private static final String APP_CLASS_NAME = "org.netbeans.api.debugger.jpda.testapps.JPDAClassTypeTestApp";    // NOI18N

    private JPDASupport     support;
    private JPDAClassType   testAppClass;
    private JPDAClassType   multiImplClass;

    public JPDAClassTypeTest(String s) {
        super(s);
    }

    public static Test suite() {
        return JPDASupport.createTestSuite(JPDAClassTypeTest.class);
    }

    @Override
    protected void setUp () throws Exception {
        super.setUp ();
        JPDASupport.removeAllBreakpoints ();
        Utils.BreakPositions bp = Utils.getBreakPositions(System.getProperty("test.dir.src")+
                                                          APP_SRC_NAME);
        LineBreakpoint lb = bp.getLineBreakpoints().get(0);
        DebuggerManager.getDebuggerManager().addBreakpoint(lb);
        support = JPDASupport.attach(APP_CLASS_NAME);
        support.waitState(JPDADebugger.STATE_STOPPED);
        CallStackFrame csf = support.getDebugger().getCurrentCallStackFrame();
        LocalVariable[] methodArguments = ((CallStackFrameImpl) csf).getMethodArguments();
        testAppClass = ((ObjectVariable) methodArguments[0]).getClassType();
        multiImplClass = ((ObjectVariable) methodArguments[1]).getClassType();
    }

    public void testJPDAClassTypeMethods() throws Exception {
        checkName();
        checkSourceName();
        checkClassObject();
        checkClassLoader();
        checkSuperClass();
        checkSubClasses();
        checkAllInterfaces();
        checkDirectInterfaces();
        checkInstanceOf();
        checkStaticFields();
        checkInvokeMethod();
        checkInstanceCount();
        checkInstances();
    }

    private void checkName() throws Exception {
        String tasn = testAppClass.getName();
        String misn = multiImplClass.getName();
        assertEquals("Wrong Name", APP_CLASS_NAME, tasn);
        assertEquals("Wrong Name", APP_CLASS_NAME+"$MultiImpl", misn);
    }

    private void checkSourceName() throws Exception {
        String tasn = testAppClass.getSourceName();
        String misn = multiImplClass.getSourceName();
        String expectedSrcName = APP_SRC_NAME.substring(APP_SRC_NAME.lastIndexOf('/')+1);
        assertEquals("Wrong Source Name", expectedSrcName, tasn);
        assertEquals("Wrong Source Name", expectedSrcName, misn);
    }

    private void checkClassObject() throws Exception {
        ClassVariable taClassObject = testAppClass.classObject();
        ClassVariable miClassObject = multiImplClass.classObject();
        assertEquals("Wrong Class Object Type", "java.lang.Class", taClassObject.getType());
        assertEquals("Wrong Class Object Type", "java.lang.Class", miClassObject.getType());
        assertEquals("Wrong Class Object", "class "+APP_CLASS_NAME, taClassObject.getToStringValue());
        assertEquals("Wrong Class Object", "class "+APP_CLASS_NAME+"$MultiImpl", miClassObject.getToStringValue());
    }

    private void checkClassLoader() throws Exception {
        ObjectVariable taClassLoader = testAppClass.getClassLoader();
        String taClassLoaderStr = taClassLoader.getToStringValue();
        assertTrue("Wrong Class Loader: "+taClassLoaderStr, taClassLoaderStr.startsWith("sun.misc.Launcher$AppClassLoader@"));
    }

    private void checkSuperClass() throws Exception {
        Super taSuperClass = testAppClass.getSuperClass();
        Super miSuperClass = multiImplClass.getSuperClass();
        assertEquals("Wrong Super Class", "java.lang.Object", taSuperClass.getType());
        Super ss = taSuperClass.getSuper();
        assertNull("Non null Object super", ss);
        assertEquals("Wrong Super Class", APP_CLASS_NAME+"$SuperImpl", miSuperClass.getType());
    }

    private void checkSubClasses() throws Exception {
        List<JPDAClassType> subClasses = testAppClass.getSubClasses();
        assertTrue("Subclasses should be empty", subClasses.isEmpty());
        subClasses = multiImplClass.getSubClasses();
        assertEquals("Wrong number of subclasses", 2, subClasses.size());
        Set<String> subClassesNames = new HashSet<>(Arrays.asList(new String[] {
            APP_CLASS_NAME+"$MultiImplSubClass1",
            APP_CLASS_NAME+"$MultiImplSubClass2",
        }));
        for (JPDAClassType sc : subClasses) {
            boolean removed = subClassesNames.remove(sc.getName());
            assertTrue("Wrong subclass '"+sc.getName()+"'", removed);
        }
        assertTrue("Not found subclasses: "+subClassesNames.toString(), subClassesNames.isEmpty());
    }

    private void checkAllInterfaces() throws Exception {
        Set<String> taAllInterfaces = new HashSet<>(Arrays.asList(new String[] {
            EventListener.class.getName(),
            SwingConstants.class.getName(),
        }));
        Set<String> miAllInterfaces = new HashSet<>(Arrays.asList(new String[] {
            Runnable.class.getName(),
            APP_CLASS_NAME+"$Intrfc1",
            APP_CLASS_NAME+"$Intrfc2",
            APP_CLASS_NAME+"$Intrfc3",
            APP_CLASS_NAME+"$Intrfc4",
        }));
        Set<String> mifAllInterfaces = new HashSet<>(Arrays.asList(new String[] {
            Runnable.class.getName(),
            APP_CLASS_NAME+"$Intrfc1",
            APP_CLASS_NAME+"$Intrfc2",
            APP_CLASS_NAME+"$Intrfc3",
            APP_CLASS_NAME+"$Intrfc4",
            APP_CLASS_NAME+"$Intrfc5",
        }));

        List<JPDAClassType> allInterfaces = testAppClass.getAllInterfaces();
        assertEquals("Wrong number of all interfaces", taAllInterfaces.size(), allInterfaces.size());
        for (JPDAClassType sc : allInterfaces) {
            boolean removed = taAllInterfaces.remove(sc.getName());
            assertTrue("Wrong all interface '"+sc.getName()+"'", removed);
        }
        assertTrue("Not found all interfaces: "+taAllInterfaces.toString(), taAllInterfaces.isEmpty());

        allInterfaces = multiImplClass.getAllInterfaces();
        assertEquals("Wrong number of all interfaces", miAllInterfaces.size(), allInterfaces.size());
        for (JPDAClassType sc : allInterfaces) {
            boolean removed = miAllInterfaces.remove(sc.getName());
            assertTrue("Wrong all interface '"+sc.getName()+"'", removed);
        }
        assertTrue("Not found all interfaces: "+miAllInterfaces.toString(), miAllInterfaces.isEmpty());

        JPDAClassType multiIntrfcClass = null;
        for (Field sf : multiImplClass.staticFields()) {
            if (sf.getName().equals("multiIntrfc")) {
                multiIntrfcClass = ((ClassVariable) sf).getReflectedType();
                break;
            }
        }
        assertNotNull("Did not find the multiIntrfc field", multiIntrfcClass);

        allInterfaces = multiIntrfcClass.getAllInterfaces();
        assertEquals("Wrong number of all interfaces", mifAllInterfaces.size(), allInterfaces.size());
        for (JPDAClassType sc : allInterfaces) {
            boolean removed = mifAllInterfaces.remove(sc.getName());
            assertTrue("Wrong all interface '"+sc.getName()+"'", removed);
        }
        assertTrue("Not found all interfaces: "+mifAllInterfaces.toString(), mifAllInterfaces.isEmpty());
    }

    private void checkDirectInterfaces() throws Exception {
        Set<String> taDirectInterfaces = new HashSet<>(Arrays.asList(new String[] {
            EventListener.class.getName(),
            SwingConstants.class.getName(),
        }));
        Set<String> miDirectInterfaces = new HashSet<>(Arrays.asList(new String[] {
            Runnable.class.getName(),
            APP_CLASS_NAME+"$Intrfc1",
            APP_CLASS_NAME+"$Intrfc2",
        }));
        Set<String> mifDirectInterfaces = new HashSet<>(Arrays.asList(new String[] {
            Runnable.class.getName(),
            APP_CLASS_NAME+"$Intrfc1",
            APP_CLASS_NAME+"$Intrfc4",
            APP_CLASS_NAME+"$Intrfc5",
        }));

        List<JPDAClassType> directInterfaces = testAppClass.getDirectInterfaces();
        assertEquals("Wrong number of direct interfaces", taDirectInterfaces.size(), directInterfaces.size());
        for (JPDAClassType sc : directInterfaces) {
            boolean removed = taDirectInterfaces.remove(sc.getName());
            assertTrue("Wrong direct interface '"+sc.getName()+"'", removed);
        }
        assertTrue("Not found direct interfaces: "+taDirectInterfaces.toString(), taDirectInterfaces.isEmpty());

        directInterfaces = multiImplClass.getDirectInterfaces();
        assertEquals("Wrong number of direct interfaces", miDirectInterfaces.size(), directInterfaces.size());
        for (JPDAClassType sc : directInterfaces) {
            boolean removed = miDirectInterfaces.remove(sc.getName());
            assertTrue("Wrong direct interface '"+sc.getName()+"'", removed);
        }
        assertTrue("Not found direct interfaces: "+miDirectInterfaces.toString(), miDirectInterfaces.isEmpty());

        JPDAClassType multiIntrfcClass = null;
        for (Field sf : multiImplClass.staticFields()) {
            if (sf.getName().equals("multiIntrfc")) {
                multiIntrfcClass = ((ClassVariable) sf).getReflectedType();
            }
        }
        assertNotNull("Did not find the multiIntrfc field", multiIntrfcClass);

        directInterfaces = multiIntrfcClass.getDirectInterfaces();
        assertEquals("Wrong number of direct interfaces", mifDirectInterfaces.size(), directInterfaces.size());
        for (JPDAClassType sc : directInterfaces) {
            boolean removed = mifDirectInterfaces.remove(sc.getName());
            assertTrue("Wrong direct interface '"+sc.getName()+"'", removed);
        }
        assertTrue("Not found direct interfaces: "+mifDirectInterfaces.toString(), mifDirectInterfaces.isEmpty());
    }

    private void checkInstanceOf() throws Exception {
        boolean is = testAppClass.isInstanceOf(APP_SRC_NAME);
        assertFalse(is);
        is = testAppClass.isInstanceOf(APP_CLASS_NAME);
        assertTrue("Instance of "+APP_CLASS_NAME, is);
        is = testAppClass.isInstanceOf(EventListener.class.getName());
        assertTrue("Instance of "+EventListener.class.getName(), is);

        is = multiImplClass.isInstanceOf("huuuhuuu");
        assertFalse(is);
        is = multiImplClass.isInstanceOf(APP_CLASS_NAME+"$MultiImpl");
        assertTrue("Instance of "+APP_CLASS_NAME+"$MultiImpl", is);
        is = multiImplClass.isInstanceOf(Runnable.class.getName());
        assertTrue("Instance of "+Runnable.class.getName(), is);
        is = multiImplClass.isInstanceOf(APP_CLASS_NAME+"$SuperImpl");
        assertTrue("Instance of "+APP_CLASS_NAME+"$SuperImpl", is);
        is = multiImplClass.isInstanceOf(APP_CLASS_NAME+"$Intrfc4");
        assertTrue("Instance of "+APP_CLASS_NAME+"$Intrfc4", is);
    }

    private void checkStaticFields() throws Exception {
        List<Field> staticFields = testAppClass.staticFields();
        // Contains all SwingConstants
        assertFalse(staticFields.isEmpty());

        staticFields = multiImplClass.staticFields();
        assertEquals("Static fields", 1, staticFields.size());
    }

    private void checkInvokeMethod() throws Exception {
        Variable pi = multiImplClass.invokeMethod("getPreparedInterface", "()Ljava/lang/Class;", new Variable[]{});
        assertNotNull(pi);
        assertEquals("getPreparedInterface() method invoked", "java.lang.Class", pi.getType());
    }

    private void checkInstanceCount() throws Exception {
        long ic = testAppClass.getInstanceCount();
        assertEquals("Number of instances", 1, ic);
        ic = multiImplClass.getInstanceCount();
        assertEquals("Number of instances", 4, ic);
    }

    private void checkInstances() throws Exception {
        List<ObjectVariable> instances = testAppClass.getInstances(100);
        assertEquals(1, instances.size());
        instances = multiImplClass.getInstances(100);
        assertEquals(4, instances.size());
        instances = multiImplClass.getInstances(2);
        assertEquals(2, instances.size());
    }

}
