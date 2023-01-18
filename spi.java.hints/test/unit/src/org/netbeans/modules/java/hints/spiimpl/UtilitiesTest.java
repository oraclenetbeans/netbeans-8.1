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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints.spiimpl;

import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.junit.RandomlyFails;
import org.netbeans.modules.java.source.pretty.VeryPretty;
import org.netbeans.modules.java.source.save.DiffContext;

/**
 *
 * @author lahvac
 */
public class UtilitiesTest extends TestBase {

    public UtilitiesTest(String name) {
        super(name);
    }

    public void testParseAndAttributeExpressionStatement() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.singletonMap("$1", info.getTreeUtilities().parseType("int", info.getTopLevelElements().get(0))));
        Tree result = Utilities.parseAndAttribute(info, "$1 = 1;", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.EXPRESSION_STATEMENT);
    }

    public void testParseAndAttributeVariable() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.singletonMap("$1", info.getTreeUtilities().parseType("int", info.getTopLevelElements().get(0))));
        Tree result = Utilities.parseAndAttribute(info, "int $2 = $1;", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.VARIABLE);
    }

    public void testParseAndAttributeMultipleStatements() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "String $2 = $1; int $l = $2.length(); System.err.println($l);", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.BLOCK);

        String golden = "{\n" +
                        "    $$1$;\n" +
                        "    String $2 = $1;\n" +
                        "    int $l = $2.length();\n" +
                        "    System.err.println($l);\n" +
                        "    $$2$;\n" +
                        "}";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }

    public void testParseAndAttributeMethod() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.singletonMap("$1", info.getTreeUtilities().parseType("int", info.getTopLevelElements().get(0))));
        String methodCode = "private int test(int i) { return i; }";
        Tree result = Utilities.parseAndAttribute(info, methodCode, s);

        assertEquals(Kind.METHOD, result.getKind());
        assertEquals(methodCode.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " ").trim());
    }

    public void testParseAndAttributeMultipleClassMembers() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.singletonMap("$1", info.getTreeUtilities().parseType("int", info.getTopLevelElements().get(0))));
        String code = "private int i; private int getI() { return i; } private void setI(int i) { this.i = i; }";
        Tree result = Utilities.parseAndAttribute(info, code, s);

        String golden = "class $ {\n" +
                        "    $$1$;\n" +
                        "    private int i;\n" +
                        "    private int getI() {\n" +
                        "        return i;\n" +
                        "    }\n" +
                        "    private void setI(int i) {\n" +
                        "        this.i = i;\n" +
                        "    }\n" +
                        "}";

        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " ").trim());
    }

    public void testParseAndAttributeFieldModifiersVariable() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        String code = "$mods$ java.lang.String $name;";
        Tree result = Utilities.parseAndAttribute(info, code, s);

        String golden = "$mods$ java.lang.String $name";

        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " ").trim());
    }

    public void testParseAndAttributeIfWithParenthetised() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        String code = "if ($c) { $1$; System.err.println('a'); $2$; }";
        Tree result = Utilities.parseAndAttribute(info, code, s);

        IfTree it = (IfTree) result;

        assertEquals(Kind.PARENTHESIZED, it.getCondition().getKind());

        String golden = "if ($c) { $1$; System.err.println('a'); $2$; }";

        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " ").trim());
    }

    public void testParseAndAttributeMultipleStatements2() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "$type $name = $map.get($key); if ($name == null) { $map.put($key, $name = $init); }", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.BLOCK);

        String golden = "{" +
                        "    $$1$;" +
                        "    $type $name = $map.get($key);" +
                        "    if ($name == null) {" +
                        "        $map.put($key, $name = $init);" +
                        "    }" +
                        "    $$2$;\n" +
                        "}";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }

    public void testParseAndAttributeMethodDeclarationWithMultiparameters() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "public void t($params$) {}", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.METHOD);

        String golden = " public void t($params$) { }";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }

    public void testParseAndAttributeMethodModifiersVariable() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        String code = "$mods$ $type $name() { $r$; }";
        Tree result = Utilities.parseAndAttribute(info, code, s);

        String golden = "$mods$ $type $name() { $r$; }";

        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " ").trim());
    }

    public void testSimpleExpression() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "$1.isDirectory()", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.METHOD_INVOCATION);

        String golden = "$1.isDirectory()";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }
    
    public void testARMResourceVariable1() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "try ($t$) { $stmts$; } catch $catches$", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.TRY);

        String golden = "try ($t$) { $stmts$; }$catches$";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }
    
    public void testARMResourceVariable2() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "try ($t$; $type $name = $init) { $stmts$; } catch $catches$", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.TRY);

        String golden = "try ($t$ final $type $name = $init;) { $stmts$; }$catches$";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }
    
    public void testARMResourceNotVariable() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "try ($t $n = $init$) { $stmts$; } catch $catches$", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.TRY);

        String golden = "try (final $t $n = $init$;) { $stmts$; }$catches$";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }

    public void testParseAndAttributeType() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "\njava. util \n.List \n", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.MEMBER_SELECT);

        assertEquals(ElementKind.INTERFACE, info.getTrees().getElement(new TreePath(new TreePath(info.getCompilationUnit()), result)).getKind());
        assertEquals(info.getElements().getTypeElement("java.util.List"), info.getTrees().getElement(new TreePath(new TreePath(info.getCompilationUnit()), result)));
    }
    
    public void testCatchMultiparam() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "try {\n }\n catch $catch$ finally {\n }\n", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.TRY);

        String golden = "try {\n }$catch$ finally {\n }";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }

    public void testCaseMultiparam() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "switch ($v) {case $c1$ case 1: ; case $c2$; case 3: ;}", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.SWITCH);

        String golden = "switch ($v) { $c1$ case 1: ; $c2$ case 3: ; }";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }
    
    public void testOrdinaryCatch() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "try {\n }\n catch (NullPointerException ex) { } finally {\n }\n", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.TRY);

        String golden = "try {\n } catch (NullPointerException ex) { } finally {\n }";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }
    
    public void testClassPattern() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "$mods$ class $name extends java.util.LinkedList { $methods$; }\n", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.CLASS);

        String golden = " $mods$ class $name extends java.util.LinkedList { $name() { super(); } $methods$ }";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }

    public void testErrorsForPatterns1() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        SourcePositions[] positions = new SourcePositions[1];
        Collection<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();
        String code = "foo bar";
        Tree result = Utilities.parseAndAttribute(info, code, null, positions, errors);

        assertDiagnostics(errors, "7-7:compiler.err.expected");
        assertPositions(result, positions[0], code, "foo", "foo bar");
    }

    @RandomlyFails
    public void testErrorsForPatterns2() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        SourcePositions[] positions = new SourcePositions[1];
        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Collection<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();
        String code = "$1.isDirectory()";
        Tree result = Utilities.parseAndAttribute(info, code, s, positions, errors);

        assertDiagnostics(errors, "0-0:compiler.err.cant.resolve.location");
        assertPositions(result, positions[0], code, "$1", "$1.isDirectory", "$1.isDirectory()");
    }

    public void testErrorsForPatterns3() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        SourcePositions[] positions = new SourcePositions[1];
        Collection<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();
        String code = "if ($cond) { foo() } else $else;";
        Tree result = Utilities.parseAndAttribute(info, code, null, positions, errors);

        assertDiagnostics(errors, "18-18:compiler.err.expected");
        assertPositions(result, positions[0], code, "$cond", "$else", "$else;", "($cond)", "foo", "foo()", "foo() ", "if ($cond) { foo() } else $else;", "{ foo() }");
    }

    public void testPositionsForCorrectStatement() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        SourcePositions[] positions = new SourcePositions[1];
        Collection<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();
        String code = "assert true;";
        Tree result = Utilities.parseAndAttribute(info, code, null, positions, errors);

        assertTrue(errors.isEmpty());
        assertPositions(result, positions[0], code, "assert true;", "true");
    }

    public void testCasePattern() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        SourcePositions[] positions = new SourcePositions[1];
        Collection<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();
        String code = "case $expr: foo bar $stmts$;\n";
        Tree result = Utilities.parseAndAttribute(info, code, null, positions, errors);

        assertTrue(result.getKind().name(), result.getKind() == Kind.CASE);

        String golden = "case $expr: foo bar; $stmts$; ";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
        assertDiagnostics(errors, "19-19:compiler.err.expected");
        assertPositions(result, positions[0], code, "$expr", "$stmts$", "$stmts$;", "case $expr: foo bar $stmts$;", "foo", "foo bar ");
    }

    public void testLambdaPattern() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "new $type() {\n $mods$ $resultType $methodName($args$) {\n $statements$;\n }\n }\n", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.NEW_CLASS);

        String golden = "new $type(){ $mods$ $resultType $methodName($args$) { $statements$; } }";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));

        Collection<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();

        result = Utilities.parseAndAttribute(info, "new $type() {\n $mods$ $resultType $methodName($args$) {\n $statements$;\n }\n }\n", null, errors);
        assertTrue(result.getKind().name(), result.getKind() == Kind.NEW_CLASS);

        golden = "new $type(){ $mods$ $resultType $methodName($args$) { $statements$; } }";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
        assertTrue(errors.toString(), errors.isEmpty());
    }
    
    public void testPackagePattern() throws Exception {
        prepareTest("test/a/Test.java", "package test.a; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "test.$1", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.MEMBER_SELECT);

        String golden = "test.$1";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
        
        Element total = info.getTrees().getElement(new TreePath(new TreePath(info.getCompilationUnit()), result));
        
        assertTrue(Utilities.isError(total));
        
        Element testPack = info.getTrees().getElement(new TreePath(new TreePath(info.getCompilationUnit()), ((MemberSelectTree) result).getExpression()));

        assertFalse(Utilities.isError(testPack));
        assertEquals(info.getElements().getPackageElement("test"), testPack);
    }

    public void DtestMultiStatementVarWithModifiers() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "$mods$ $type $name; $name = $init;", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.BLOCK);

        String golden = "{ $$1$; $mods$$type $name; $name = $init; $$2$; }";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }
    
    public void testAttributionErrors233526() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        SourcePositions[] positions = new SourcePositions[1];
        String code = "{\n   $4\n};;";
        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Collection<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<>();
        Tree result = Utilities.parseAndAttribute(info, code, s, positions, errors);

        assertDiagnostics(errors, "7-7:compiler.err.expected", "5-7:compiler.err.cant.resolve");
    }
    
    public void testAnnotation() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "@$annotation($args$)", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.ANNOTATION);

        String golden = "@$annotation(value = $args$)";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }

    public void testParseAndAttributeMultipleStatementsWithBefore() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "$before$; String $2 = $1; int $l = $2.length(); System.err.println($l);", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.BLOCK);

        String golden = "{\n" +
                        "    $before$;\n" +
                        "    String $2 = $1;\n" +
                        "    int $l = $2.length();\n" +
                        "    System.err.println($l);\n" +
                        "    $$2$;\n" +
                        "}";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }
    
    public void testParseAndAttributeMultipleStatementsWithAfter() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "String $2 = $1; int $l = $2.length(); System.err.println($l); $after$;", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.BLOCK);

        String golden = "{\n" +
                        "    $$1$;\n" +
                        "    String $2 = $1;\n" +
                        "    int $l = $2.length();\n" +
                        "    System.err.println($l);\n" +
                        "    $after$;\n" +
                        "}";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }
    
    public void testMethodFormalParams() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "$mods$ $ret $name($pref$, $type $name, $suff$) throws $throws$ { $body$; }", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.METHOD);

        String golden = " $mods$ $ret $name($pref$, $type $name, $suff$) throws $throws$ { $body$; }";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }
    
    public void testPartialModifiers() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "$mods$ @Deprecated public $type $name;", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.VARIABLE);

        ModifiersTree mods = ((VariableTree) result).getModifiers();
        String golden = "$mods$,@Deprecated(), [public]";
        
        assertEquals(golden.replaceAll("[ \n\r]+", " "), mods.getAnnotations().toString() + ", " + mods.getFlags().toString());
    }
    
    public void testBrokenPlatform226678() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        JavaSource.create(ClasspathInfo.create(ClassPath.EMPTY, ClassPath.EMPTY, ClassPath.EMPTY), info.getFileObject()).runUserActionTask(new Task<CompilationController>() {
            @Override public void run(CompilationController parameter) throws Exception {
                parameter.toPhase(Phase.RESOLVED);
                info = parameter;
            }
        }, true);
        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        String methodCode = "private int test(int i) { return i; }";
        Tree result = Utilities.parseAndAttribute(info, methodCode, s);

        assertEquals(Kind.METHOD, result.getKind());
        assertEquals(methodCode.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " ").trim());
    }
    
    public void testLambdaExpression1() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "($args$) -> $expression", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.LAMBDA_EXPRESSION);

        LambdaExpressionTree let = (LambdaExpressionTree) result;
        
        assertEquals(Kind.IDENTIFIER, let.getParameters().get(0).getKind());
        String golden = "($args$)->$expression";
        
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString());
    }
    
    public void testMemberReference() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "$expression::$name", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.MEMBER_REFERENCE);

        MemberReferenceTree mrt = (MemberReferenceTree) result;
        
        assertEquals(Kind.IDENTIFIER, mrt.getQualifierExpression().getKind());
        String golden = "$expression::$name";
        
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString());
    }
    
    public void testToHumanReadableTime() {
        long time = 202;
        assertEquals(    "5s", Utilities.toHumanReadableTime(time +=           5 * 1000));
        assertEquals(  "3m5s", Utilities.toHumanReadableTime(time +=      3 * 60 * 1000));
        assertEquals("7h3m5s", Utilities.toHumanReadableTime(time += 7 * 60 * 60 * 1000));
    }

    public void testGeneralization() throws Exception {
        performGeneralizationTest("package test;\n" +
                                  "public class Test {\n" +
                                  "    class Inner {\n" +
                                  "        Inner(int i) {}\n" +
                                  "    }\n" +
                                  "    public static void main(String[] args) {\n" +
                                  "        int i = 1;\n" +
                                  "        Test c = null;\n" +
                                  "        c.new Inner(i++) {};\n" +
                                  "    }\n" +
                                  "}\n",
                                  "package test;\n" +
                                  "public class Test {\n" +
                                  "    class Inner {\n" +
                                  "        Inner(int $0) { super(); }\n" +
                                  "    }\n" +
                                  "    public static void main(String[] $1) {\n" +
                                  "        int $2 = 1;\n" +
                                  "        Test $3 = null;\n" +
                                  "        $4;\n" + //XXX
                                  "    }\n" +
                                  "}\n");
    }
    private void performGeneralizationTest(String code, String generalized) throws Exception {
        prepareTest("test/Test.java", code);

        Tree generalizedTree = Utilities.generalizePattern(info, new TreePath(info.getCompilationUnit()));
        VeryPretty vp = new VeryPretty(new DiffContext(info));

        vp.print((JCTree) generalizedTree);

        String repr = vp.toString();

        assertEquals(generalized.replaceAll("[ \n\t]+", " "),
                     repr.replaceAll("[ \n\t]+", " "));
    }

    private void assertDiagnostics(Collection<Diagnostic<? extends JavaFileObject>> errors, String... golden) {
        List<String> actual = new ArrayList<String>(errors.size());

        for (Diagnostic<? extends JavaFileObject> d : errors) {
            actual.add(d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }

        assertEquals(Arrays.asList(golden), actual);
    }

    private void assertPositions(Tree t, final SourcePositions sp, final String code, String... golden) {
        final List<String> actual = new ArrayList<String>(golden.length);

        new TreeScanner<Void, Void>() {
            @Override
            public Void scan(Tree node, Void p) {
                if (node != null) {
                    int start = (int) sp.getStartPosition(null, node);
                    int end = (int) sp.getEndPosition(null, node);

                    if (start >= 0 && end >= 0) {
                        actual.add(code.substring(start, end));
                    }
                }
                return super.scan(node, p);
            }
        }.scan(t, null);

        Collections.sort(actual);

        List<String> goldenList = new ArrayList<String>(Arrays.asList(golden));

        Collections.sort(goldenList);

        assertEquals(goldenList, actual);
    }

}
