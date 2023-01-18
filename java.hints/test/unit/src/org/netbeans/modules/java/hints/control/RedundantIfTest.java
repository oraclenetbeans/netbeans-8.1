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
package org.netbeans.modules.java.hints.control;

import org.junit.Test;
import org.netbeans.modules.java.hints.test.api.HintTest;

/**
 *
 * @author lahvac
 */
public class RedundantIfTest {
    
    @Test
    public void testSimpleRedundantIf() throws Exception {
        HintTest.create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private boolean t(int i) {\n" +
                       "        if (i == 0) {\n" +
                       "            return true;\n" +
                       "        } else {\n" +
                       "            return false;\n" +
                       "        }\n" +
                       "    }\n" +
                       "}\n")
                .run(RedundantIf.class)
                .findWarning("3:8-3:10:verifier:" + Bundle.ERR_redundantIf())
                .applyFix()
                .assertCompilable()
                .assertOutput("package test;\n" +
                              "public class Test {\n" +
                              "    private boolean t(int i) {\n" +
                              "        return i == 0;\n" +
                              "    }\n" +
                              "}\n");
    }
    
    @Test
    public void testRedundantIfNeg() throws Exception {
        HintTest.create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private boolean t(int i) {\n" +
                       "        if (i == 0) {\n" +
                       "            return false;\n" +
                       "        } else {\n" +
                       "            return true;\n" +
                       "        }\n" +
                       "    }\n" +
                       "}\n")
                .run(RedundantIf.class)
                .findWarning("3:8-3:10:verifier:" + Bundle.ERR_redundantIf())
                .applyFix()
                .assertCompilable()
                .assertOutput("package test;\n" +
                              "public class Test {\n" +
                              "    private boolean t(int i) {\n" +
                              "        return i != 0;\n" +
                              "    }\n" +
                              "}\n");
    }

    /**
     * Checks that De Morgan rules will not apply when negating an if-expression.
     * @throws Exception 
     */
    @Test
    public void testRedundantNoDeMorgan() throws Exception {
        HintTest.create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private boolean t(int i) {\n" +
                       "        if (i < 5 || i > 7) {\n" +
                       "            return false;\n" +
                       "        } else {\n" +
                       "            return true;\n" +
                       "        }\n" +
                       "    }\n" +
                       "}\n")
                .run(RedundantIf.class)
                .findWarning("3:8-3:10:verifier:" + Bundle.ERR_redundantIf())
                .applyFix()
                .assertCompilable()
                .assertOutput("package test;\n" +
                              "public class Test {\n" +
                              "    private boolean t(int i) {\n" +
                              "        return !(i < 5 || i > 7);\n" +
                              "    }\n" +
                              "}\n");
    }

    @Test
    public void testSimpleRedundantIfVar() throws Exception {
        HintTest.create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private boolean t(int i) {\n" +
                       "        boolean r;\n" +
                       "        if (i == 0) {\n" +
                       "            r = true;\n" +
                       "        } else {\n" +
                       "            r = false;\n" +
                       "        }\n" +
                       "        return r;\n" +
                       "    }\n" +
                       "}\n")
                .run(RedundantIf.class)
                .findWarning("4:8-4:10:verifier:" + Bundle.ERR_redundantIf())
                .applyFix()
                .assertCompilable()
                .assertOutput("package test;\n" +
                              "public class Test {\n" +
                              "    private boolean t(int i) {\n" +
                              "        boolean r;\n" +
                              "        r = i == 0;\n" +
                              "        return r;\n" +
                              "    }\n" +
                              "}\n");
    }
    
    @Test
    public void testSimpleRedundantIfVarNeg() throws Exception {
        HintTest.create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private boolean t(int i) {\n" +
                       "        boolean r;\n" +
                       "        if (i == 0) {\n" +
                       "            r = false;\n" +
                       "        } else {\n" +
                       "            r = true;\n" +
                       "        }\n" +
                       "        return r;\n" +
                       "    }\n" +
                       "}\n")
                .run(RedundantIf.class)
                .findWarning("4:8-4:10:verifier:" + Bundle.ERR_redundantIf())
                .applyFix()
                .assertCompilable()
                .assertOutput("package test;\n" +
                              "public class Test {\n" +
                              "    private boolean t(int i) {\n" +
                              "        boolean r;\n" +
                              "        r = i != 0;\n" +
                              "        return r;\n" +
                              "    }\n" +
                              "}\n");
    }

    @Test
    public void testRedundantIfImplicit() throws Exception {
        HintTest.create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private boolean t(int i) {\n" +
                       "        System.err.println(i);\n" +
                       "        if (i == 0) {\n" +
                       "            return true;\n" +
                       "        }\n" +
                       "        return false;\n" +
                       "    }\n" +
                       "}\n")
                .run(RedundantIf.class)
                .findWarning("4:8-4:10:verifier:" + Bundle.ERR_redundantIf())
                .applyFix()
                .assertCompilable()
                .assertOutput("package test;\n" +
                              "public class Test {\n" +
                              "    private boolean t(int i) {\n" +
                              "        System.err.println(i);\n" +
                              "        return i == 0;\n" +
                              "    }\n" +
                              "}\n");
    }
    
    @Test
    public void testRedundantIfImplicitNeg() throws Exception {
        HintTest.create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private boolean t(int i) {\n" +
                       "        System.err.println(i);\n" +
                       "        if (i == 0) {\n" +
                       "            return false;\n" +
                       "        }\n" +
                       "        return true;\n" +
                       "    }\n" +
                       "}\n")
                .run(RedundantIf.class)
                .findWarning("4:8-4:10:verifier:" + Bundle.ERR_redundantIf())
                .applyFix()
                .assertCompilable()
                .assertOutput("package test;\n" +
                              "public class Test {\n" +
                              "    private boolean t(int i) {\n" +
                              "        System.err.println(i);\n" +
                              "        return i != 0;\n" +
                              "    }\n" +
                              "}\n");
    }
}
