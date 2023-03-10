/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints.jdk;

import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.hints.spiimpl.TestCompilerSettings;
import org.netbeans.modules.java.hints.test.api.HintTest;

/**
 *
 * @author lahvac
 */
public class ConvertToDiamondBulkHintTest extends NbTestCase {

    public ConvertToDiamondBulkHintTest(String name) {
        super(name);
    }

    private String allBut(String key) {
        return ("," + ConvertToDiamondBulkHint.ALL + ",")
                .replace("," + key + ",", ",");
    }

    public void testSimple1() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private java.util.LinkedList<String> l = new java.util.LinkedList<String>();\n" +
                       "}\n")
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .findWarning("2:49-2:77:verifier:redundant type arguments in new expression (use diamond operator instead).")
                .applyFix("FIX_ConvertToDiamond")
                .assertCompilable()
                .assertOutput("package test;\n" +
                              "public class Test {\n" +
                              "    private java.util.LinkedList<String> l = new java.util.LinkedList<>();\n" +
                              "}\n");
    }

    public void testSimple2() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "import java.util.LinkedList;\n" +
                       "public class Test {\n" +
                       "    private LinkedList<String> l = new LinkedList<String>();\n" +
                       "}\n")
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .findWarning("3:39-3:57:verifier:redundant type arguments in new expression (use diamond operator instead).")
                .applyFix("FIX_ConvertToDiamond")
                .assertCompilable()
                .assertOutput("package test;\n" +
                              "import java.util.LinkedList;\n" +
                              "public class Test {\n" +
                              "    private LinkedList<String> l = new LinkedList<>();\n" +
                              "}\n");
    }

    public void testConfiguration1() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private java.util.LinkedList<String> l = new java.util.LinkedList<String>();\n" +
                       "}\n")
                .sourceLevel("1.7")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("initializer"))
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }

    public void testConfiguration2() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    { java.util.LinkedList<String> l = new java.util.LinkedList<String>(); }\n" +
                       "}\n")
                .sourceLevel("1.7")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("initializer"))
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }

    public void testConfiguration2a() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    { java.util.LinkedList<String> l = new java.util.LinkedList<String>(); }\n" +
                       "}\n")
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings("2:43-2:71:verifier:redundant type arguments in new expression (use diamond operator instead).");
    }

    public void testConfiguration2b() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    { java.util.LinkedList<String> l = new java.util.LinkedList<String>(); }\n" +
                       "}\n")
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings("2:43-2:71:verifier:redundant type arguments in new expression (use diamond operator instead).");
    }

    public void testConfiguration2c() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    { java.util.LinkedList<java.util.LinkedList<?>> l = new java.util.LinkedList<java.util.LinkedList<?>>(); }\n" +
                       "}\n")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("initializer"))
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }

    public void testConfiguration3() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    { final java.util.LinkedList<String> l = new java.util.LinkedList<String>(); }\n" +
                       "}\n")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("initializer"))
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }

    public void testConfiguration4() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private java.util.LinkedList<String> l() { return new java.util.LinkedList<String>(); }\n" +
                       "}\n")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("initializer"))
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings("2:58-2:86:verifier:redundant type arguments in new expression (use diamond operator instead).");
    }

    public void testConfiguration5() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    private java.util.LinkedList<String> l() { return new java.util.LinkedList<String>(); }\n" +
                       "}\n")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("return"))
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }

    public void testConfiguration6a() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test<T extends CharSequence> extends java.util.LinkedList<T> {\n" +
                       "    private void l(java.util.LinkedList<? extends CharSequence> a) { l(new Test<CharSequence>()); }\n" +
                       "}\n")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("argument"))
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }

    public void testConfiguration6b() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test<T extends CharSequence> extends java.util.LinkedList<T> {\n" +
                       "    private void l(java.util.LinkedList<? extends CharSequence> a) { this.l(new Test<CharSequence>()); }\n" +
                       "}\n")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("argument"))
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }

    public void testConfiguration6c() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test<T extends CharSequence> extends java.util.LinkedList<T> {\n" +
                       "    public Test(java.util.LinkedList<? extends CharSequence> a) { new Test(new Test<CharSequence>(null)); }\n" +
                       "}\n")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("argument"))
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }

    public void testConfiguration6d() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test<T extends CharSequence> extends java.util.LinkedList<T> {\n" +
                       "    public Test(java.util.LinkedList<? extends CharSequence> a) { new Test<String>(new Test<CharSequence>(null)); }\n" +
                       "}\n")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("argument"))
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }

    public void testConfiguration7a() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test<T extends CharSequence> extends java.util.LinkedList<T> {\n" +
                       "    private void l(java.util.LinkedList<? extends CharSequence> a) { l(new Test<CharSequence>()); }\n" +
                       "}\n")
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings("2:75-2:93:verifier:redundant type arguments in new expression (use diamond operator instead).");
    }

    public void testConfiguration7b() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test<T extends CharSequence> extends java.util.LinkedList<T> {\n" +
                       "    private void l(java.util.LinkedList<? extends CharSequence> a) { this.l(new Test<CharSequence>()); }\n" +
                       "}\n")
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings("2:80-2:98:verifier:redundant type arguments in new expression (use diamond operator instead).");
    }

    public void testConfiguration7c() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test<T extends CharSequence> extends java.util.LinkedList<T> {\n" +
                       "    public Test(java.util.LinkedList<? extends CharSequence> a) { new Test(new Test<CharSequence>(null)); }\n" +
                       "}\n")
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings("2:79-2:97:verifier:redundant type arguments in new expression (use diamond operator instead).");
    }

    public void testConfiguration7d() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test<T extends CharSequence> extends java.util.LinkedList<T> {\n" +
                       "    public Test(java.util.LinkedList<? extends CharSequence> a) { new Test<String>(new Test<CharSequence>(null)); }\n" +
                       "}\n")
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings("2:87-2:105:verifier:redundant type arguments in new expression (use diamond operator instead).");
    }

    public void testConfiguration8() throws Exception {
        HintTest
                .create()
                .input("package test;\n" +
                       "public class Test {\n" +
                       "    { java.util.LinkedList<String> l; l = new java.util.LinkedList<String>(); }\n" +
                       "}\n")
                .preference(ConvertToDiamondBulkHint.KEY, allBut("assignment"))
                .sourceLevel("1.7")
                .run(ConvertToDiamondBulkHint.class)
                .assertWarnings();
    }
    
    static {
        TestCompilerSettings.commandLine = "-XDfindDiamond";
    }
}