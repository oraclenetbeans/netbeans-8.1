/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints.jdk;

import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.hints.test.api.HintTest;

/**
 *
 * @author Jan Lahoda
 */
public class ConvertToStringSwitchTest extends NbTestCase {

    public ConvertToStringSwitchTest(String name) {
        super(name);
    }

    public void testSimple() throws Exception {
        HintTest
                .create()
                .input("package test;"
                + "public class Test {"
                + "     public void test() {"
                + "         String g = \"xxx\";"
                + "         if (g == \"j\") {"
                + "             System.err.println(1);"
                + "         } else if (g == \"k\") {"
                + "             System.err.println(2);"
                + "         } else if (g == \"l\") {"
                + "             System.err.println(3);"
                + "         }"
                + "     }"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .findWarning("0:92-0:94:verifier:Convert to switch")
                .applyFix("FIX_ConvertToStringSwitch")
                .assertCompilable()
                .assertOutput("package test;public class Test { public void test() { String g = \"xxx\"; switch (g) { case \"j\": System.err.println(1); break; case \"k\": System.err.println(2); break; case \"l\": System.err.println(3); break; default: break; } }}");
    }

    public void testSimpleFlow() throws Exception {
        HintTest
                .create()
                .input("package test;"
                + "public class Test {"
                + "     public int test(int r) throws Exception {"
                + "         String g = \"xxx\";\n"
                + "         if (g == \"j\") {"
                + "             System.err.println(1);"
                + "             return 1;"
                + "         } else if (g == \"k\") {"
                + "             System.err.println(2);"
                + "             if (r >= 0) {"
                + "                 return 2;"
                + "             } else {"
                + "                 return 3;"
                + "             }"
                + "         } else if (g == \"l\") {"
                + "             System.err.println(3);"
                + "         } else if (g == \"z\") {"
                + "             try {"
                + "                 throw new java.io.FileNotFoundException();"
                + "             } catch (java.io.IOException e) {}"
                + "         } else if (g == \"a\") {"
                + "             try {"
                + "                 throw new java.io.IOException();"
                + "             } catch (java.io.FileNotFoundException e) {}"
                + "         } else {\n"
                + "             throw new IllegalStateException();\n"
                + "         }\n"
                + "         return 11;\n"
                + "     }"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .findWarning("1:9-1:11:verifier:Convert to switch")
                .applyFix("FIX_ConvertToStringSwitch")
                .assertCompilable()
                .assertOutput("package test;"
                + "public class Test {"
                + "     public int test(int r) throws Exception {"
                + "         String g = \"xxx\";"
                + "         switch (g) {\n"
                + "             case \"j\":\n"
                + "                 System.err.println(1);"
                + "                 return 1;"
                + "             case \"k\":\n"
                + "                 System.err.println(2);"
                + "                 if (r >= 0) {"
                + "                     return 2;"
                + "                 } else {"
                + "                     return 3;"
                + "                 }\n"
                + "             case \"l\":\n"
                + "                 System.err.println(3);"
                + "                 break;"
                + "             case \"z\":\n"
                + "                 try {"
                + "                     throw new java.io.FileNotFoundException();"
                + "                 } catch (java.io.IOException e) {}"
                + "                 break;"
                + "             case \"a\":\n"
                + "                 try {"
                + "                     throw new java.io.IOException();"
                + "                 } catch (java.io.FileNotFoundException e) {}"
                + "             default:\n"
                + "                 throw new IllegalStateException();\n"
                + "         }\n"
                + "         return 11;\n"
                + "     }"
                + "}");
    }

    public void testOr() throws Exception {
        HintTest
                .create()
                .input("package test;\n"
                + "public class Test {\n"
                + "     public void test() {\n"
                + "         String g = null;\n"
                + "         if (g == \"j\" || g == \"m\") {\n"
                + "             System.err.println(1);\n"
                + "         } else if (g == \"k\") {\n"
                + "             System.err.println(2);\n"
                + "         } else if (g == \"l\" || g == \"n\") {\n"
                + "             System.err.println(3);\n"
                + "         } else {\n"
                + "             System.err.println(4);\n"
                + "             return;\n"
                + "         }\n"
                + "     }\n"
                + "}\n")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .findWarning("4:9-4:11:verifier:Convert to switch")
                .applyFix("FIX_ConvertToStringSwitch")
                .assertCompilable()
                .assertOutput("package test;\n"
                        + "public class Test {\n"
                        + "     public void test() {\n"
                        + "         String g = null;\n"
                        + "         if (null != g) switch (g) {\n"
                        + "             case \"j\":\n"
                        + "             case \"m\":\n"
                        + "                 System.err.println(1);\n"
                        + "                 break;\n"
                        + "             case \"k\":\n"
                        + "                 System.err.println(2);\n"
                        + "                 break;\n"
                        + "             case \"l\":\n"
                        + "             case \"n\":\n"
                        + "                 System.err.println(3);\n"
                        + "                 break;\n"
                        + "             default:\n"
                        + "                 System.err.println(4);\n"
                        + "             return;\n"
                        + "         }\n"
                        + "     }\n");
    }

    public void testStringEqualsObject() throws Exception {
        HintTest
                .create()
                .input("package test;"
                + "public class Test {"
                + "     public void test() throws Exception {"
                + "         Object g = null;\n"
                + "         if (\"j\".equals(g)) {"
                + "             System.err.println(1);"
                + "         } else if (\"k\".equals(g)) {"
                + "             System.err.println(2);"
                + "         } else {\n"
                + "             System.err.println(3);"
                + "         }\n"
                + "     }"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .assertWarnings();
    }

    public void testVariableDeclarations() throws Exception {
        HintTest
                .create()
                .input("package test;"
                + "public class Test {"
                + "     private int a, b;"
                + "     public void test() throws Exception {"
                + "         String g = \"xxx\";\n"
                + "         if (g == \"j\") {"
                + "             int i = 1;"
                + "             int z = 1;"
                + "             System.err.println(i + z);"
                + "         } else if (g == \"k\") {"
                + "             int i = 2;"
                + "             System.err.println(i);"
                + "         } else if (g == \"l\") {"
                + "             int j = 1;"
                + "             System.err.println(j);"
                + "         } else if (g == \"z\") {"
                + "             int z = 1;"
                + "             System.err.println(z);"
                + "         } else if (g == \"a\") {"
                + "             int a = 1;"
                + "             System.err.println(a);"
                + "         } else if (g == \"b\") {"
                + "             int b = 1;"
                + "             System.err.println(a + b);"
                + "         }\n"
                + "     }"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .findWarning("1:9-1:11:verifier:Convert to switch")
                .applyFix("FIX_ConvertToStringSwitch")
                .assertCompilable()
                .assertOutput("package test;"
                + "public class Test {"
                + "     private int a, b;"
                + "     public void test() throws Exception {"
                + "         String g = \"xxx\";"
                + "         switch (g) {\n"
                + "             case \"j\": {\n"
                + "                 int i = 1;"
                + "                 int z = 1;"
                + "                 System.err.println(i + z);"
                + "                 break;"
                + "             }"
                + "             case \"k\": {\n"
                + "                 int i = 2;"
                + "                 System.err.println(i);"
                + "                 break;"
                + "             }"
                + "             case \"l\":\n"
                + "                 int j = 1;"
                + "                 System.err.println(j);"
                + "                 break;"
                + "             case \"z\": {\n"
                + "                 int z = 1;"
                + "                 System.err.println(z);"
                + "                 break;"
                + "             }"
                + "             case \"a\": {\n"
                + "                 int a = 1;"
                + "                 System.err.println(a);"
                + "                 break;"
                + "             }"
                + "             case \"b\":\n"
                + "                 int b = 1;"
                + "                 System.err.println(a + b);"
                + "                 break;"
                + "             default:"
                + "                 break;"
                + "         }\n"
                + "     }"
                + "}");
    }

    public void testNonLocalBreak() throws Exception {
        HintTest
                .create()
                .input("package test;"
                + "public class Test {"
                + "     private int a, b;"
                + "     public void test() throws Exception {"
                + "         for (;;) {\n"
                + "             String g = \"xxx\";\n"
                + "             if (g == \"j\") {"
                + "                 System.err.println(1);"
                + "                 break;"
                + "             } else if (g == \"m\") {"
                + "                 System.err.println(3);"
                + "                 break;"
                + "             } else if (g == \"k\") {"
                + "                 System.err.println(2);"
                + "                 break;"
                + "             }\n"
                + "         }\n"
                + "     }"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .findWarning("2:13-2:15:verifier:Convert to switch")
                .applyFix("FIX_ConvertToStringSwitch")
                .assertCompilable()
                .assertOutput("package test;"
                + "public class Test {"
                + "     private int a, b;"
                + "     public void test() throws Exception {"
                + "         OUTER: for (;;) {\n"
                + "             String g = \"xxx\";\n"
                + "             switch (g) {"
                + "                 case \"j\":"
                + "                     System.err.println(1);"
                + "                     break OUTER;"
                + "                 case \"m\":"
                + "                     System.err.println(3);"
                + "                     break OUTER;"
                + "                 case \"k\":"
                + "                     System.err.println(2);"
                + "                     break OUTER;"
                + "                 default:"
                + "                     break;"
                + "             }\n"
                + "         }\n"
                + "     }"
                + "}");
    }

    public void testNonConstantString() throws Exception {
        HintTest
                .create()
                .input("package test;"
                + "public class Test {"
                + "     private static String nonConstant = \"a\";"
                + "     public void test() throws Exception {"
                + "         String g = null;\n"
                + "         if (\"j\".equals(g)) {"
                + "             System.err.println(1);"
                + "         } else if (nonConstant.equals(g)) {"
                + "             System.err.println(2);"
                + "         } else {\n"
                + "             System.err.println(3);"
                + "         }\n"
                + "     }"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .assertWarnings();
    }

    public void testComments1() throws Exception {
        HintTest
                .create()
                .input("package test;"
                + "public class Test {"
                + "     private int a, b;"
                + "     public void test() throws Exception {"
                + "         String g = \"xxx\";\n"
                + "         //comment\n"
                + "         if (g == \"j\") {//foo1\n"
                + "             System.err.println(1);\n"
                + "             //foo2\n"
                + "         } else if (g == \"k\") {"
                + "             System.err.println(2);"
                + "         } else if (g == \"l\") {"
                + "             System.err.println(3);"
                + "         }\n"
                + "     }"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .findWarning("2:9-2:11:verifier:Convert to switch")
                .applyFix("FIX_ConvertToStringSwitch")
                .assertCompilable()
                .assertOutput("package test;"
                + "public class Test {"
                + "     private int a, b;"
                + "     public void test() throws Exception {"
                + "         String g = \"xxx\";"
                + "         //comment\n"
                + "         switch (g) {\n"
                + "             case \"j\":\n"
                + "                 //foo1\n"
                + "                 System.err.println(1);"
                + "                 //foo2\n"
                + "                 break;"
                + "             case \"k\":\n"
                + "                 System.err.println(2);"
                + "                 break;"
                + "             case \"l\":\n"
                + "                 System.err.println(3);"
                + "                 break;"
                + "             default:"
                + "                 break;"
                + "         }\n"
                + "     }"
                + "}");
    }

    public void testNoEquals() throws Exception {
        HintTest
                .create()
                .input("package test;"
                + "public class Test {"
                + "     public void test() throws Exception {"
                + "         String g = null;\n"
                + "         if (\"j\" == g) {"
                + "             System.err.println(1);"
                + "         } else if (\"l\" == g) {"
                + "             System.err.println(2);"
                + "         } else {\n"
                + "             System.err.println(3);"
                + "         }\n"
                + "     }"
                + "}")
                .sourceLevel("1.7")
                .preference(ConvertToStringSwitch.KEY_ALSO_EQ, false)
                .run(ConvertToStringSwitch.class)
                .assertWarnings();
    }

    public void testSameLabels() throws Exception {
        HintTest
                .create()
                .input("package test;\n"
                + "final class Test {\n"
                + "    public void test(String val) {\n"
                + "        int res = 0;\n"
                + "        if (val.equals(\"a\" + \"b\")) {\n"
                + "            res = 1;\n"
                + "        } else if (val.equals(\"b\")) {\n"
                + "            res = 2;\n"
                + "        } else if (val.equals(\"ab\")) {\n"
                + "            res = 4;\n"
                + "        } else {\n"
                + "            res = 3;\n"
                + "        }\n"
                + "    }\n"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .assertWarnings("8:30-8:34:verifier:The string value `ab' used in String comparison appears earlier in the chained if-else-if statement. This condition never evaluates to true");
    }

    public void testNullableExpression() throws Exception {
        HintTest
                .create()
                .input("package test;\n"
                + "final class Test {\n"
                + "    public void test(String val) {\n"
                + "        int res = 0;\n"
                + "        if (\"a\".equals(val)) {\n"
                + "            res = 1;\n"
                + "        } else if (\"b\".equals(val)) {\n"
                + "            res = 2;\n"
                + "        } else {\n"
                + "            res = 3;\n"
                + "        }\n"
                + "    }\n"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .findWarning("4:8-4:10:verifier:Convert to switch")
                .applyFix("FIX_ConvertToStringSwitch")
                .assertCompilable()
                .assertOutput("package test;\n"
                + "final class Test {\n"
                + "    public void test(String val) {\n"
                + "        int res = 0;\n"
                + "        if (null != val) switch (val) {\n"
                + "            case \"a\":\n"
                + "                res = 1;\n"
                + "                break;\n"
                + "            case \"b\":\n"
                + "                res = 2;\n"
                + "                break;\n"
                + "            default:\n"
                + "                res = 3;\n"
                + "                break;\n"
                + "        }\n"
                + "    }\n"
                + "}");
    }

    public void testNullableDefinitelyAssigned() throws Exception {
        HintTest
                .create()
                .input("package test;\n"
                + "final class Test {\n"
                + "    public void test(String val, boolean f) {\n"
                + "        int res = 0;\n"
                + "        if (f) {\n"
                + "            val = \"a\";\n"
                + "        } else {\n"
                + "            val = \"b\";\n"
                + "        }\n"
                + "        if (\"a\".equals(val)) {\n"
                + "            res = 1;\n"
                + "        } else if (\"b\".equals(val)) {\n"
                + "            res = 2;\n"
                + "        } else {\n"
                + "            res = 3;\n"
                + "        }\n"
                + "    }\n"
                + "}")
                .sourceLevel("1.7")
                .run(ConvertToStringSwitch.class)
                .findWarning("9:8-9:10:verifier:Convert to switch")
                .applyFix("FIX_ConvertToStringSwitch")
                .assertCompilable()
                .assertOutput("package test;\n"
                + "final class Test {\n"
                + "    public void test(String val, boolean f) {\n"
                + "        int res = 0;\n"
                + "        if (f) {\n"
                + "            val = \"a\";\n"
                + "        } else {\n"
                + "            val = \"b\";\n"
                + "        }\n"
                + "        switch (val) {\n"
                + "            case \"a\":\n"
                + "                res = 1;\n"
                + "                break;\n"
                + "            case \"b\":\n"
                + "                res = 2;\n"
                + "                break;\n"
                + "            default:\n"
                + "                res = 3;\n"
                + "                break;\n"
                + "        }\n"
                + "    }\n"
                + "}");
    }
}