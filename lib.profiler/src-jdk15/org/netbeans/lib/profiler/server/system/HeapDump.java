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

package org.netbeans.lib.profiler.server.system;

import java.lang.management.ManagementFactory;
import javax.management.InstanceNotFoundException;
import javax.management.JMRuntimeException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;


/**
 *
 * @author Tomas Hurka
 */
public class HeapDump {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static MBeanServer mserver;
    private static ObjectName hotspotDiag;
    private static boolean initialized;
    private static boolean runningOnJdk15;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private HeapDump() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void initialize(boolean jdk15) {
        runningOnJdk15 = jdk15;
        if (runningOnJdk15) {
            initialize15();
        } else {
            initialize16();
        }
    }

    public static String takeHeapDump(String outputFile) {
        if (runningOnJdk15) {
            return takeHeapDump15(outputFile);
        }

        return takeHeapDump16(outputFile);
    }

    private static native void initialize15();

    private static void initialize16() {
        if (initialized) {
            return;
        }

        initialized = true;

        try {
            mserver = ManagementFactory.getPlatformMBeanServer();
        } catch (JMRuntimeException ex) {
            // Glassfish: if ManagementFactory.getPlatformMBeanServer() is called too early it will throw JMRuntimeException
            // in such case initialization will be rerun later as part of takeHeapDump()
            System.err.println(ex.getLocalizedMessage());
            initialized = false;

            return;
        }

        try {
            hotspotDiag = new ObjectName("com.sun.management:type=HotSpotDiagnostic");   // NOI18N
            mserver.getObjectInstance(hotspotDiag);
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
        } catch (InstanceNotFoundException ex) {
            System.err.println("Heap Dump is not available"); // NOI18N
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    private static String takeHeapDump15(String outputFile) {
        int error = -1;

        try {
            error = takeHeapDump15Native(outputFile);
        } catch (Exception ex) {
            return ex.getLocalizedMessage();
        }

        if (error == -1) {
            return "Take heap dump is not available."; // NOI18N
        }

        return null;
    }

    private static native int takeHeapDump15Native(String outputFile);

    private static String takeHeapDump16(String outputFile) {
        String error = null;
        initialize16();

        if ((mserver == null) || (hotspotDiag == null)) {
            return "Take heap dump is not available."; // NOI18N
        }

        try {
            mserver.invoke(hotspotDiag, "dumpHeap", new Object[] {outputFile, true}, new String[] {String.class.getName(), boolean.class.getName()} );  // NOI18N
        } catch (IllegalArgumentException ex) {
            error = ex.getLocalizedMessage();
        } catch (InstanceNotFoundException ex) {
            error = ex.getLocalizedMessage();
        } catch (MBeanException ex) {
            error = ex.getLocalizedMessage();
        } catch (ReflectionException ex) {
            error = ex.getLocalizedMessage();
        }

        return error;
    }
}
