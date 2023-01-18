/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Scope;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.util.regex.Pattern;
import org.netbeans.modules.java.hints.spiimpl.TestBase;

/**
 *
 * @author lahvac
 */
public class ArithmeticUtilitiesTest extends TestBase {

    public ArithmeticUtilitiesTest(String name) {
        super(name);
    }

    public void test186347() throws Exception {
        performTest("package test; public class Test { private static final short A = 0, B = 1; | }", "A + B", 1);
    }

    public void test185010() throws Exception {
        performTest("package test; public class Test { private static final byte[] a; | }", "a[0]", null);
    }
    
    public void testIntegerOperations() throws Exception {
        performExpressionTest("1 + 1", Integer.valueOf(2));
        performExpressionTest("3 * 5", Integer.valueOf(15));
        performExpressionTest("17 / 5", Integer.valueOf(3));
        performExpressionTest("12 % 5", Integer.valueOf(2));
        performExpressionTest("1 == 1", Boolean.valueOf(true));
        performExpressionTest("2 != 1", Boolean.valueOf(true));
        performExpressionTest("5 > 3", Boolean.valueOf(true));
        performExpressionTest("3 <= 9", Boolean.valueOf(true));
        performExpressionTest("-3", Integer.valueOf(-3));
        performExpressionTest("+3", Integer.valueOf(3));
    }
    
    public void testLongOperations() throws Exception {
        performExpressionTest("1l + 1l", Long.valueOf(2));
        performExpressionTest("3l * 5l", Long.valueOf(15));
        performExpressionTest("17l / 5l", Long.valueOf(3));
        performExpressionTest("12l % 5l", Long.valueOf(2));
        performExpressionTest("1l == 1l", Boolean.valueOf(true));
        performExpressionTest("2l != 1l", Boolean.valueOf(true));
        performExpressionTest("5l > 3l", Boolean.valueOf(true));
        performExpressionTest("3l <= 9l", Boolean.valueOf(true));
        performExpressionTest("-3l", Long.valueOf(-3));
        performExpressionTest("+3l", Long.valueOf(3));
    }
    
    /**
     * Tests correct promotion to long during evaluation
     * @throws Exception 
     */
    public void testLongPromotion() throws Exception {
        // int
        performExpressionTest("1 + 1l", Long.valueOf(2));
        // short
        performExpressionTest("((short)3) * 5l", Long.valueOf(15));
        // char
        performExpressionTest("((char)17) / 5l", Long.valueOf(3));

        performExpressionTest("((byte)17) % 5l", Long.valueOf(2));
    }

    public void testFloatOperations() throws Exception {
        performExpressionTest("1f + 1f", Float.valueOf(2));
        performExpressionTest("3f * 5f", Float.valueOf(15));
        performExpressionTest("17f / 5f", Float.valueOf(17f / 5f));
        performExpressionTest("12f % 5f", Float.valueOf(12f % 5f));
        performExpressionTest("1f == 1f", Boolean.valueOf(true));
        performExpressionTest("2f != 1f", Boolean.valueOf(true));
        performExpressionTest("5f > 3f", Boolean.valueOf(true));
        performExpressionTest("3f <= 9f", Boolean.valueOf(true));
        performExpressionTest("-3f", Float.valueOf(-3));
        performExpressionTest("+3f", Float.valueOf(3));
    }

    public void testFloatPromotions() throws Exception {
        performExpressionTest("1f + 1", Float.valueOf(2));
        performExpressionTest("3 * 5f", Float.valueOf(15));
        performExpressionTest("17l / 5f", Float.valueOf(17f / 5f));
        performExpressionTest("12 % 5f", Float.valueOf(12f % 5f));
        performExpressionTest("((char)1) == 1f", Boolean.valueOf(true));
        performExpressionTest("2f != ((char)1)", Boolean.valueOf(true));
        performExpressionTest("5f > 3f", Boolean.valueOf(true));
        performExpressionTest("3f <= 9f", Boolean.valueOf(true));
        performExpressionTest("-3f", Float.valueOf(-3));
        performExpressionTest("+3f", Float.valueOf(3));
    }
    
    public void testDoubleOperations() throws Exception {
        performExpressionTest("1d + 1d", Double.valueOf(2));
        performExpressionTest("3d * 5d", Double.valueOf(15));
        performExpressionTest("17d / 5d", Double.valueOf(17d / 5));
        performExpressionTest("12d % 5d", Double.valueOf(2));
        performExpressionTest("1d == 1d", Boolean.valueOf(true));
        performExpressionTest("2d != 1d", Boolean.valueOf(true));
        performExpressionTest("5d > 3d", Boolean.valueOf(true));
        performExpressionTest("3d <= 9d", Boolean.valueOf(true));
        performExpressionTest("-3d", Double.valueOf(-3));
        performExpressionTest("+3d", Double.valueOf(3));
    }
    
    public void testBitwiseOperations() throws Exception {
        performExpressionTest("1 << 2", Integer.valueOf(4));
        performExpressionTest("2 >> 1", Integer.valueOf(1));
        performExpressionTest("1 | 2", Integer.valueOf(3));
        performExpressionTest("7 & 4", Integer.valueOf(4));
        performExpressionTest("7 ^ 4", Integer.valueOf(3));
    }
    
    public void testBooleanBitwiseOperation() throws Exception {
        performExpressionTest("false ^ true", Boolean.TRUE);    
        performExpressionTest("false & true", Boolean.FALSE);    
        performExpressionTest("false | true", Boolean.TRUE);    
    }
    
    public void testBooleanLogicalOps() throws Exception {
        performExpressionTest("false && true", Boolean.FALSE);
        performExpressionTest("false || true", Boolean.TRUE);
    }
    
    public void testResolveFieldsJLS() throws Exception {
        performWithFields("intVar + 5", Integer.valueOf(8), "static final int intVar = 3");
        // final field initialized with an expression is OK
        performWithFields("intVar + 5", Integer.valueOf(26), "static final int intVar = 3 * 7");
        // non-final field is NOT ok, should fail
        performWithFields("intVar + 5", null, "static int intVar = 3 * 7");
        // check concatenation with String constant
        performWithFields("var + \"bak\"", "bubak", "static final String var = \"bu\"");
        
        // check that const null field provides null (= unknown), JLS does not specify null literal as const value
        performWithFields("var", null, "static final String var = null");
    }
    
    public void testResolveVariablesJLS() throws Exception {
        // final field is recognized
        // should work also for variables
        performWithFieldsOrVars("intVar + 5", 26, "final int intVar = 3 * 7;");
    }
    
    
    /**
     * Checks that null constant value is used in enhanced mode
     * @throws Exception 
     */
    public void testUseNullValue() throws Exception {
        enhancedProcessing = true;
        performWithFields("var", ArithmeticUtilities.NULL, "static final String var = null");
        performWithFields("var == null", Boolean.TRUE, "static final String var = null");
    }
    
    /**
     * Checks that enhanced processing does not affect results in JLS strict mode
     * @throws Exception 
     */
    public void testResolveNonJLSDoesNotAffectJLS() throws Exception {
    }
    
    public void testInitializedToNonNullJLS() throws Exception {
        performWithFields("r != null", null, "final Runnable r = new Runnable() { public void run() {}}");
    }

    public void testInitializedToNonNull() throws Exception {
        enhancedProcessing = true;
        performWithFields("r != null", true, "final Runnable r = new Runnable() { public void run() {}}");
    }
    
    /**
     * Checks that shorts are promoted to ints in arithmetic
     * @throws Exception 
     */
    public void testShortOperations() throws Exception {
        performWithFields("var1 + var2", 14, "static final short var1 = 3", "static final short var2 = 11");
        performWithFields("var1 - var2", -8, "static final short var1 = 3", "static final short var2 = 11");
        performWithFields("var1 * var2", 33, "static final short var1 = 3", "static final short var2 = 11");
        performWithFields("var2 / var1", 3, "static final short var1 = 3", "static final short var2 = 11");
        performWithFields("var2 % var1", 2, "static final short var1 = 3", "static final short var2 = 11");
    }
    
    private boolean enhancedProcessing = false;

    private void performTest(String context, String expression, Object golden) throws Exception {
        int pos = context.indexOf('|');
        prepareTest("test/Test.java", context.replaceAll(Pattern.quote("|"), ""));

        ExpressionTree expr = info.getTreeUtilities().parseExpression(expression, new SourcePositions[1]);
        TreePath tp = info.getTreeUtilities().pathFor(pos);
        Scope scope = info.getTrees().getScope(tp);

        info.getTreeUtilities().attributeTree(expr, scope);

        Object real = ArithmeticUtilities.compute(info, new TreePath(tp, expr), true, enhancedProcessing);

        assertEquals(golden, real);
    }
    
    private void performExpressionTest(String expression, Object value) throws Exception {
        performTest("package test; public class Test { private static void method() { Object o = |null; }}", expression, value);
    }
    
    private void performWithFieldsOrVars(String expression, Object value, String prolog, String... fieldDecls) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String s : fieldDecls) {
            sb.append(s).append(";");
        }
        if (prolog == null) prolog = "";
        performTest("package test; public class Test { " + sb.toString() + " private static void method() { " + prolog + " Object o = |null; }}", expression, value);
    }

    private void performWithFields(String expression, Object value, String... fieldDecls) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String s : fieldDecls) {
            sb.append(s).append(";");
        }
        performTest("package test; public class Test { " + sb.toString() + " private static void method() { Object o = |null; }}", expression, value);
    }

}