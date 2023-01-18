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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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

package org.netbeans.modules.debugger.jpda.jdi;

// DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
// Generated by org.netbeans.modules.debugger.jpda.jdi.Generate class located in 'gensrc' folder,
// perform the desired modifications there and re-generate by "ant generate".

/**
 * Wrapper for Field JDI class.
 * Use methods of this class instead of direct calls on JDI objects.
 * These methods assure that exceptions thrown from JDI calls are handled appropriately.
 *
 * @author Martin Entlicher
 */
public final class FieldWrapper {

    private FieldWrapper() {}

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static boolean equals0(com.sun.jdi.Field a, java.lang.Object b) {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "equals",
                    "JDI CALL: com.sun.jdi.Field({0}).equals({1})",
                    new Object[] {a, b});
        }
        Object retValue = null;
        try {
            boolean ret;
            ret = a.equals(b);
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            return false;
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            return false;
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "equals",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static boolean equals(com.sun.jdi.Field a, java.lang.Object b) throws org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper, org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "equals",
                    "JDI CALL: com.sun.jdi.Field({0}).equals({1})",
                    new Object[] {a, b});
        }
        Object retValue = null;
        try {
            boolean ret;
            ret = a.equals(b);
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            throw new org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper(ex);
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            throw new org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper(ex);
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "equals",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static int hashCode0(com.sun.jdi.Field a) {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "hashCode",
                    "JDI CALL: com.sun.jdi.Field({0}).hashCode()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            int ret;
            ret = a.hashCode();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            return 0;
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            return 0;
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "hashCode",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static int hashCode(com.sun.jdi.Field a) throws org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper, org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "hashCode",
                    "JDI CALL: com.sun.jdi.Field({0}).hashCode()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            int ret;
            ret = a.hashCode();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            throw new org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper(ex);
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            throw new org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper(ex);
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "hashCode",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static boolean isEnumConstant0(com.sun.jdi.Field a) {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "isEnumConstant",
                    "JDI CALL: com.sun.jdi.Field({0}).isEnumConstant()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            boolean ret;
            ret = a.isEnumConstant();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            return false;
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            return false;
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "isEnumConstant",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static boolean isEnumConstant(com.sun.jdi.Field a) throws org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper, org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "isEnumConstant",
                    "JDI CALL: com.sun.jdi.Field({0}).isEnumConstant()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            boolean ret;
            ret = a.isEnumConstant();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            throw new org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper(ex);
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            throw new org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper(ex);
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "isEnumConstant",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static boolean isTransient0(com.sun.jdi.Field a) {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "isTransient",
                    "JDI CALL: com.sun.jdi.Field({0}).isTransient()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            boolean ret;
            ret = a.isTransient();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            return false;
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            return false;
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "isTransient",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static boolean isTransient(com.sun.jdi.Field a) throws org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper, org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "isTransient",
                    "JDI CALL: com.sun.jdi.Field({0}).isTransient()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            boolean ret;
            ret = a.isTransient();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            throw new org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper(ex);
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            throw new org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper(ex);
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "isTransient",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static boolean isVolatile0(com.sun.jdi.Field a) {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "isVolatile",
                    "JDI CALL: com.sun.jdi.Field({0}).isVolatile()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            boolean ret;
            ret = a.isVolatile();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            return false;
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            return false;
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "isVolatile",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static boolean isVolatile(com.sun.jdi.Field a) throws org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper, org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "isVolatile",
                    "JDI CALL: com.sun.jdi.Field({0}).isVolatile()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            boolean ret;
            ret = a.isVolatile();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            throw new org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper(ex);
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            throw new org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper(ex);
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "isVolatile",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static com.sun.jdi.Type type(com.sun.jdi.Field a) throws com.sun.jdi.ClassNotLoadedException, org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper, org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper, org.netbeans.modules.debugger.jpda.jdi.ObjectCollectedExceptionWrapper {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "type",
                    "JDI CALL: com.sun.jdi.Field({0}).type()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            com.sun.jdi.Type ret;
            ret = a.type();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            throw new org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper(ex);
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            throw new org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper(ex);
        } catch (com.sun.jdi.ObjectCollectedException ex) {
            retValue = ex;
            throw new org.netbeans.modules.debugger.jpda.jdi.ObjectCollectedExceptionWrapper(ex);
        } catch (com.sun.jdi.ClassNotLoadedException ex) {
            retValue = ex;
            throw ex;
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "type",
                        retValue);
            }
        }
    }

    // DO NOT MODIFY THIS CODE, GENERATED AUTOMATICALLY
    public static java.lang.String typeName(com.sun.jdi.Field a) throws org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper, org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper {
        if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallStart(
                    "com.sun.jdi.Field",
                    "typeName",
                    "JDI CALL: com.sun.jdi.Field({0}).typeName()",
                    new Object[] {a});
        }
        Object retValue = null;
        try {
            java.lang.String ret;
            ret = a.typeName();
            retValue = ret;
            return ret;
        } catch (com.sun.jdi.InternalException ex) {
            retValue = ex;
            org.netbeans.modules.debugger.jpda.JDIExceptionReporter.report(ex);
            throw new org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper(ex);
        } catch (com.sun.jdi.VMDisconnectedException ex) {
            retValue = ex;
            if (a instanceof com.sun.jdi.Mirror) {
                com.sun.jdi.VirtualMachine vm = ((com.sun.jdi.Mirror) a).virtualMachine();
                try {
                    vm.dispose();
                } catch (com.sun.jdi.VMDisconnectedException vmdex) {}
            }
            throw new org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper(ex);
        } catch (Error err) {
            retValue = err;
            throw err;
        } catch (RuntimeException rex) {
            retValue = rex;
            throw rex;
        } finally {
            if (org.netbeans.modules.debugger.jpda.JDIExceptionReporter.isLoggable()) {
                org.netbeans.modules.debugger.jpda.JDIExceptionReporter.logCallEnd(
                        "com.sun.jdi.Field",
                        "typeName",
                        retValue);
            }
        }
    }

}
