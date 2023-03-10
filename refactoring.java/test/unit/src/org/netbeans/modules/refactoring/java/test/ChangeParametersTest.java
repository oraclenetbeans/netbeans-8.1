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
package org.netbeans.modules.refactoring.java.test;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import java.util.*;
import javax.lang.model.element.Modifier;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.java.source.parsing.JavacParser;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RefactoringSession;
import org.netbeans.modules.refactoring.java.api.ChangeParametersRefactoring;
import org.netbeans.modules.refactoring.java.api.ChangeParametersRefactoring.ParameterInfo;
import org.netbeans.modules.refactoring.java.ui.ChangeParametersPanel.Javadoc;

/**
 *
 * @author Ralph Ruijs
 */
public class ChangeParametersTest extends RefactoringTestBase {

    public ChangeParametersTest(String name) {
        super(name, "1.8");
    }
    
    static {
        JavacParser.DISABLE_SOURCE_LEVEL_DOWNGRADE = true;
    }
    
    public void test255269() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t;\n"
                        + "public interface A {\n"
                        + "    boolean test(int a);\n"
                        + "\n"
                        + "    public static void m() {\n"
                        + "        A f2 = (int c) -> true;\n"
                        + "    }\n"
                        + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "a", "Object", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 0, false, new Problem(false, "WRN_isNotAssignable"));
        verifyContent(src,
                new File("t/A.java", "package t;\n"
                        + "public interface A {\n"
                        + "    boolean test(Object a);\n"
                        + "\n"
                        + "    public static void m() {\n"
                        + "        A f2 = (Object c) -> true;\n"
                        + "    }\n"
                        + "}\n"));
    }
    
    public void test250047() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                        + "    /**\n"
                        + "     * \n"
                        + "     * @param x the value of x\n"
                        + "     */\n"
                        + "    public void testMethod(int a, int x) {\n"
                        + "         System.out.println(x);\n"
                        + "    }\n"
                        + "\n"
                        + "    public static void main(string[] args) {\n"
                        + "        testMethod(2, 1);\n"
                        + "    }\n"
                        + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                        + "    public void testMethod(int a, int b) {\n"
                        + "         int x = a;\n"
                        + "    }\n"
                        + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[]{new ParameterInfo(0, "b", "int", null), new ParameterInfo(1, "x", "int", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                        + "    /**\n"
                        + "     * \n"
                        + "     * @param x the value of x\n"
                        + "     */\n"
                        + "    public void testMethod(int b, int x) {\n"
                        + "         System.out.println(x);\n"
                        + "    }\n"
                        + "\n"
                        + "    public static void main(string[] args) {\n"
                        + "        testMethod(2, 1);\n"
                        + "    }\n"
                        + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                        + "    public void testMethod(int a, int b) {\n"
                        + "         int x = a;\n"
                        + "    }\n"
                        + "}\n"));
    }
    
    public void test239300() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param x the value of x\n"
                + "     */\n"
                + "    public void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, 1);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    public void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(1, "y", "int", null), new ParameterInfo(0, "x", "int", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.UPDATE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param y the value of y\n"
                + "     * @param x the value of x\n"
                + "     */\n"
                + "    public void testMethod(int y, int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(1, 2);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    public void testMethod(int y, int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void test242827() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t;\n"
                        + "public interface A {\n"
                        + "    boolean test(int a);\n"
                        + "\n"
                        + "    public static void m() {\n"
                        + "        A f2 = c -> true;\n"
                        + "    }\n"
                        + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "a", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 0, false);
        verifyContent(src,
                new File("t/A.java", "package t;\n"
                        + "public interface A {\n"
                        + "    boolean test(int a, int y);\n"
                        + "\n"
                        + "    public static void m() {\n"
                        + "        A f2 = (c, y) -> true;\n"
                        + "    }\n"
                        + "}\n"));
    }
    
    public void test241499() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t;\n"
                        + "public interface A {\n"
                        + "    boolean test(int a);\n"
                        + "\n"
                        + "    public static void m() {\n"
                        + "        A f2 = (c) -> true;\n"
                        + "    }\n"
                        + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "a", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 0, false);
        verifyContent(src,
                new File("t/A.java", "package t;\n"
                        + "public interface A {\n"
                        + "    boolean test(int a, int y);\n"
                        + "\n"
                        + "    public static void m() {\n"
                        + "        A f2 = (c, y) -> true;\n"
                        + "    }\n"
                        + "}\n"));
    }
    
    public void test238836() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t;\n"
                        + "public interface A {\n"
                        + "    boolean test(int a);\n"
                        + "\n"
                        + "    public static void m() {\n"
                        + "        A f2 = (int c) -> true;\n"
                        + "    }\n"
                        + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "a", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 0, false);
        verifyContent(src,
                new File("t/A.java", "package t;\n"
                        + "public interface A {\n"
                        + "    boolean test(int a, int y);\n"
                        + "\n"
                        + "    public static void m() {\n"
                        + "        A f2 = (int c, int y) -> true;\n"
                        + "    }\n"
                        + "}\n"));
    }
    
    public void test221730() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t;\n"
                + "import java.util.Map;\n"
                + "\n"
                + "public class A {\n"
                + "    public static void testMethod(Map<String, Map<Boolean, Long>> x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(null);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "Map<String, Map<Boolean, Long>>", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java","package t;\n"
                + "import java.util.Map;\n"
                + "\n"
                + "public class A {\n"
                + "    public static void testMethod(Map<String, Map<Boolean, Long>> x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(null, 1);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void test215256() throws Exception { // #215256 - Refactoring "change method parameters" confuses parameters of newly created delegate
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t;\n"
                + "\n"
                + "import java.io.File;\n"
                + "import java.util.Scanner;\n"
                + "\n"
                + "public class A\n"
                + "{\n"
                + "    public static void main(String[] args) throws Exception\n"
                + "    {\n"
                + "        File file = new File(\"example\");\n"
                + "        analyzeFile(file);\n"
                + "    }\n"
                + "\n"
                + "    private static void analyzeFile(final File file) throws\n"
                + "java.io.FileNotFoundException\n"
                + "    {\n"
                + "        Scanner scanner = new Scanner(file);\n"
                + "\n"
                + "        while(scanner.hasNext())\n"
                + "        {\n"
                + "            String nextLine = scanner.nextLine();\n"
                + "            System.out.println(nextLine);\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "}"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(0, "is", "java.io.InputStream", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 2, true, new Problem(false, "WRN_isNotAssignable"));
        verifyContent(src,
                new File("t/A.java", "package t;\n"
                + "\n"
                + "import java.io.File;\n"
                + "import java.util.Scanner;\n"
                + "\n"
                + "public class A\n"
                + "{\n"
                + "    public static void main(String[] args) throws Exception\n"
                + "    {\n"
                + "        File file = new File(\"example\");\n"
                + "        analyzeFile(file);\n"
                + "    }\n"
                + "\n"
                + "    private static void analyzeFile(final File file) throws\n"
                + "java.io.FileNotFoundException\n"
                + "    {\n"
                + "        analyzeFile(file);\n"
                + "    }\n"
                + "\n"
                + "    private static void analyzeFile(final java.io.InputStream is) throws\n"
                + "java.io.FileNotFoundException\n"
                + "    {\n"
                + "        Scanner scanner = new Scanner(is);\n"
                + "\n"
                + "        while(scanner.hasNext())\n"
                + "        {\n"
                + "            String nextLine = scanner.nextLine();\n"
                + "            System.out.println(nextLine);\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "}"));
    }
    
    public void test218053() throws Exception { // #218053 - IllegalArgumentException after ChangeMethodParameters refactoring
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param x the value of x\n"
                + "     * @param y the value of y\n"
                + "     */\n"
                + "    public void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, 1);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    public void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(1, "z", "int", null), new ParameterInfo(0, "w", "int", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.UPDATE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param z the value of y\n"
                + "     * @param w the value of x\n"
                + "     */\n"
                + "    public void testMethod(int z, int w) {\n"
                + "         System.out.println(w);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(1, 2);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    public void testMethod(int z, int w) {\n"
                + "         System.out.println(w);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void test208495() throws Exception { //[Bug 208495] [Change Method Parameter] Method is not renamed when generating javadoc and adding a parameter
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, "renamed", null, paramTable, Javadoc.GENERATE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /** * * @param x the value of x * @param y the value of y */\n"
                + "    public static void renamed(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        renamed(2, 1);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void test199738() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param x the value of x\n"
                + "     * @param y the value of y\n"
                + "     */\n"
                + "    public void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, 1);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    public void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(1, "y", "int", null), new ParameterInfo(0, "x", "int", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.UPDATE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param y the value of y\n"
                + "     * @param x the value of x\n"
                + "     */\n"
                + "    public void testMethod(int y, int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(1, 2);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    public void testMethod(int y, int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void testAddParameter() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, 1);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void testCompatible() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, true);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         testMethod(x, 1);\n"
                + "    }\n"
                + "\n"
                + "    public static void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void testConstructorJavaDoc() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public A(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        A a = new A(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.GENERATE, 0, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /** * * @param x the value of x * @param y the value of y */\n"
                + "    public A(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        A a = new A(2, 1);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void testSynthConstructorJavaDoc() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void main(string[] args) {\n"
                + "        A a = new A();\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.GENERATE, 0, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /** * * @param y the value of y */\n"
                + "    public A(int y) {\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        A a = new A(1);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void testConstructor() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public A(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        A a = new A(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 0, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public A(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        A a = new A(2, 1);\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void testConstructor2() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public A(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        A a = new A(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(0, "x", "String", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 0, false, new Problem(false, "WRN_isNotAssignable"));
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public A(String x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        A a = new A(2);\n"
                + "    }\n"
                + "}\n"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public A(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public A(int x, int z) {\n"
                + "         System.out.println(x + z);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(0, "x", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 0, false, new Problem(false, "ERR_existingConstructor"));
    }
    
    public void test208694() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public String testMethod() {\n"
                + "         System.out.println(1);\n"
                + "         return \"\";\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    public String testMethod() {\n"
                + "         System.out.println(2);\n"
                + "         return null;\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {};
        performChangeParameters(EnumSet.of(Modifier.PROTECTED), null, null, paramTable, Javadoc.NONE, 1, false);
    }
    
    public void test201140() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod() {\n"
                + "         System.out.println(1);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    public void testMethod(short x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(-1, "y", "short", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "WRN_MethodIsOverridden"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod() {\n"
                + "         System.out.println(1);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    protected void testMethod(short x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(-1, "y", "short", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(true, "ERR_WeakerAccess"), new Problem(false, "WRN_MethodIsOverridden"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod() {\n"
                + "         System.out.println(1);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    public int testMethod(short x) {\n"
                + "         System.out.println(x);\n"
                + "         return 0;\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(-1, "y", "short", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(true, "ERR_existingReturnType"), new Problem(false, "WRN_MethodIsOverridden"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    private void testMethod() {\n"
                + "         System.out.println(1);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B extends A {\n"
                + "    protected void testMethod() {\n"
                + "         System.out.println(2);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {};
        performChangeParameters(EnumSet.of(Modifier.PUBLIC), null, null, paramTable, Javadoc.NONE, 1, false, new Problem(true, "ERR_WeakerAccess"), new Problem(false, "WRN_MethodIsOverridden"));
    }
    
    public void test201161() throws Exception { // [Change Method Parameter] Change Parameters - changes method invocation because of widening conversions (behavioral change)
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod() {\n"
                + "         System.out.println(1);\n"
                + "    }\n"
                + "\n"
                + "    public void testMethod(long x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "WRN_wideningConversion"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod() {\n"
                + "         System.out.println(1);\n"
                + "    }\n"
                + "\n"
                + "    public void testMethod(short x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A extends B {\n"
                + "    public void testMethod() {\n"
                + "         System.out.println(1);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B {\n"
                + "    public void testMethod(short x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(-1, "y", "short", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "ERR_existingMethod"));
    }
    
    public void test114328() throws Exception { // [Change parameters] Check if method with the same signature already exists
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void testMethod(int x, int z) {\n"
                + "         System.out.println(x + z);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "ERR_existingMethod"));
    }
    
    public void test114328_2() throws Exception { // [Change parameters] Check if method with the same signature already exists
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A extends B{\n"
                + "    public void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B {\n"
                + "    public void testMethod(int x, int z) {\n"
                + "         System.out.println(x + z);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "ERR_existingMethod"));
    }
    
    public void test114321() throws Exception { // [Change parameters] Check if method is accessible when modifier is changed
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "int", null)};
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B {\n"
                + "    public void testMethod(int x, int z) {\n"
                + "         A a = new A();\n"
                + "         a.testMethod(x + z);\n"
                + "    }\n"
                + "}\n"));
        performChangeParameters(EnumSet.of(Modifier.PRIVATE), null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "ERR_StrongAccMod"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "    public void secondMethod(int x, int z) {\n"
                + "         A a = new A();\n"
                + "         a.testMethod(x + z);\n"
                + "    }\n"
                + "}\n"));
        performChangeParameters(EnumSet.of(Modifier.PRIVATE), null, null, paramTable, Javadoc.NONE, 1, false);

        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"),
                new File("y/B.java", "package y; public class B {\n"
                + "    public void testMethod(int x, int z) {\n"
                + "         t.A a = new t.A();\n"
                + "         a.testMethod(x + z);\n"
                + "    }\n"
                + "}\n"));
        performChangeParameters(EnumSet.noneOf(Modifier.class), null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "ERR_StrongAccMod"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B {\n"
                + "    public void testMethod(int x, int z) {\n"
                + "         t.A a = new t.A();\n"
                + "         a.testMethod(x + z);\n"
                + "    }\n"
                + "}\n"));
        performChangeParameters(EnumSet.noneOf(Modifier.class), null, null, paramTable, Javadoc.NONE, 1, false);
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"),
                new File("y/B.java", "package y; public class B {\n"
                + "    public void testMethod(int x, int z) {\n"
                + "         t.A a = new t.A();\n"
                + "         a.testMethod(x + z);\n"
                + "         a.testMethod(x + z);\n"
                + "    }\n"
                + "}\n"),
                new File("y/C.java", "package y; public class C {\n"
                + "    public void testMethod(int x, int z) {\n"
                + "         t.A a = new t.A();\n"
                + "         a.testMethod(x + z);\n"
                + "         a.testMethod(x + z);\n"
                + "    }\n"
                + "}\n"));
        performChangeParameters(EnumSet.of(Modifier.PROTECTED), null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "ERR_StrongAccMod"), new Problem(false, "ERR_StrongAccMod"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "}\n"),
                new File("y/B.java", "package y; public class B extends t.A {\n"
                + "    public void testMethod(int x, int z) {\n"
                + "         t.A a = new t.A();\n"
                + "         a.testMethod(x + z);\n"
                + "         a.testMethod(x + z);\n"
                + "    }\n"
                + "}\n"));
        performChangeParameters(EnumSet.of(Modifier.PROTECTED), null, null, paramTable, Javadoc.NONE, 1, false);
    }

    public void test194592() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /** */\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(-1, "x", "String", "\"\"")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /** */\n"
                + "    public static void testMethod(String x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(\"\");\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void test56114() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(-1, "x", "String", "\"\"")};
        performChangeParameters(null, "changedMethod", "void", paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void changedMethod(String x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        changedMethod(\"\");\n"
                + "    }\n"
                + "}\n"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(-1, "x", "String", "\"\"")};
        performChangeParameters(null, "testMethod", "String", paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static String testMethod(String x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(\"\");\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void test245211a() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(-1, "x", "String", "\"\"")};
        performChangeParameters(null, "testMethod", "String", paramTable, Javadoc.GENERATE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param x the value of x\n"
                + "     * @return the String\n"
                + "     */\n"
                + "    public static String testMethod(String x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(\"\");\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void test245211b() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param x the value of x\n"
                + "     */\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(-1, "x", "String", "\"\"")};
        performChangeParameters(null, "testMethod", "String", paramTable, Javadoc.UPDATE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param x the value of x\n"
                + "     * @return the String\n"
                + "     */\n"
                + "    public static String testMethod(String x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(\"\");\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void test54688() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "y", "int", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int y) {\n"
                + "         System.out.println(y);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(0, "34s", "int", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(true, "ERR_InvalidIdentifier"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "         System.out.println(y);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, 3);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(0, "y", "int", null), new ParameterInfo(1, "x", "int", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(true, "ERR_NameAlreadyUsed"), new Problem(true, "ERR_NameAlreadyUsed"));
    }
    
    public void test53147() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "int", null), new ParameterInfo(-1, "y", "int", "1")};
        performChangeParameters(null, null, null, paramTable, Javadoc.GENERATE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param x the value of x\n"
                + "     * @param y the value of y\n"
                + "     */\n"
                + "    public static void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, 1);\n"
                + "    }\n"
                + "}\n"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param x the value of x\n"
                + "     * @param y the value of y\n"
                + "     */\n"
                + "    public static void testMethod(int x, int y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, 1);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(1, "y", "int", null), new ParameterInfo(0, "x", "int", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.UPDATE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    /**\n"
                + "     * \n"
                + "     * @param y the value of y\n"
                + "     * @param x the value of x\n"
                + "     */\n"
                + "    public static void testMethod(int y, int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(1, 2);\n"
                + "    }\n"
                + "}\n"));
    }

    public void test202336() throws Exception { // [71cat] ArrayIndexOutOfBoundsException: -1
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x, String y) {\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, \"ddd\");\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[0];
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod() {\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod();\n"
                + "    }\n"
                + "}\n"));
    }

    public void test202156() throws Exception { // [Change Method Parameters] Re-order parameters and change to varargs creates wrong method call
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x, String y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, \"ddd\");\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(1, "y", "String", null), new ParameterInfo(0, "x", "int...", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(String y, int... x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(\"ddd\", 2);\n"
                + "    }\n"
                + "}\n"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x, String... y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2, \"ddd\", \"eee\");\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(-1, "z", "int", "42"), new ParameterInfo(0, "x", "int", null), new ParameterInfo(1, "y", "String...", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int z, int x, String... y) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(42, 2, \"ddd\", \"eee\");\n"
                + "    }\n"
                + "}\n"));
    }
    
    public void test83483() throws Exception {
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = {new ParameterInfo(0, "x", "String", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "WRN_isNotAssignable"));
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(String x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(int x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
        paramTable = new ParameterInfo[] {new ParameterInfo(0, "x", "Strings", null)};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false, new Problem(false, "WRN_canNotResolve"));
        verifyContent(src,
                new File("t/A.java", "package t; public class A {\n"
                + "    public static void testMethod(Strings x) {\n"
                + "         System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public static void main(string[] args) {\n"
                + "        testMethod(2);\n"
                + "    }\n"
                + "}\n"));
    }
        
    public void test208022() throws Exception { // Refactoring Change Method Parameters - adding varargs doesn't work correctly
        writeFilesAndWaitForScan(src,
                new File("t/A.java", "package t; public interface A {\n"
                + "    void foo();"
                + "    int testMethod(int x, String y);\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B implements A {\n"
                + "    public int testMethod(int x, String y) {\n"
                + "         return x;\n"
                + "    }\n"
                + "\n"
                + "    public void foo() {\n"
                + "        testMethod(2, \"ddd\");\n"
                + "    }\n"
                + "}\n"));
        ParameterInfo[] paramTable = new ParameterInfo[] {new ParameterInfo(0, "x", "int", null), new ParameterInfo(1, "y", "String", null), new ParameterInfo(-1, "i", "int...", "testMethod(1,\"\",2,3), 4")};
        performChangeParameters(null, null, null, paramTable, Javadoc.NONE, 1, false);
        verifyContent(src,
                new File("t/A.java", "package t; public interface A {\n"
                + "    void foo();"
                + "    int testMethod(int x, String y, int... i);\n"
                + "}\n"),
                new File("t/B.java", "package t; public class B implements A {\n"
                + "    public int testMethod(int x, String y, int... i) {\n"
                + "         return x;\n"
                + "    }\n"
                + "\n"
                + "    public void foo() {\n"
                + "        testMethod(2, \"ddd\", testMethod(1, \"\", 2, 3), 4);\n"
                + "    }\n"
                + "}\n"));
    }

    private void performChangeParameters(final Set<Modifier> modifiers, String methodName, String returnType, ParameterInfo[] paramTable, final Javadoc javadoc, final int position, final boolean compatible, Problem... expectedProblems) throws Exception {
        final ChangeParametersRefactoring[] r = new ChangeParametersRefactoring[1];

        JavaSource.forFileObject(src.getFileObject("t/A.java")).runUserActionTask(new Task<CompilationController>() {

            @Override
            public void run(CompilationController javac) throws Exception {
                javac.toPhase(JavaSource.Phase.RESOLVED);
                CompilationUnitTree cut = javac.getCompilationUnit();

                MethodTree testMethod = (MethodTree) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(position);
                TreePath tp = TreePath.getPath(cut, testMethod);
                r[0] = new ChangeParametersRefactoring(TreePathHandle.create(tp, javac));

                Set<Modifier> modifierSet = modifiers;
//                if(modifiers == null) {
//                    modifierSet = new HashSet<Modifier>(testMethod.getModifiers().getFlags());
//                }
                r[0].setModifiers(modifierSet);
                r[0].setOverloadMethod(compatible);
                r[0].getContext().add(javadoc);
            }
        }, true);

        r[0].setParameterInfo(paramTable);
        r[0].setMethodName(methodName);
        r[0].setReturnType(returnType);

        RefactoringSession rs = RefactoringSession.create("Session");
        List<Problem> problems = new LinkedList<Problem>();

        addAllProblems(problems, r[0].preCheck());
        if (!problemIsFatal(problems)) {
            addAllProblems(problems, r[0].prepare(rs));
        }
        if (!problemIsFatal(problems)) {
            addAllProblems(problems, rs.doRefactoring(true));
        }

        assertProblems(Arrays.asList(expectedProblems), problems);
    }
}
