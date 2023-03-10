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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.api.java.source.gen;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;

import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;

import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;

import org.netbeans.junit.NbTestSuite;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.modules.java.source.indexing.TransactionContext;
import org.netbeans.modules.java.source.save.ElementOverlay;
import org.netbeans.modules.java.source.usages.ClasspathInfoAccessor;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

/**
 * Tests imports matching and its correct adding/removing. Just generator
 * test, does not do anything with import analysis.
 * 
 * @author Pavel Flaska
 */
public class ImportAnalysis2Test extends GeneratorTestMDRCompat {
    
    public ImportAnalysis2Test(String testName) {
        super(testName);
    }
    
    public static NbTestSuite suite() {
        NbTestSuite suite = new NbTestSuite();
        suite.addTestSuite(ImportAnalysis2Test.class);
//        suite.addTest(new ImportsTest("test166524c"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        FileUtil.createData(FileUtil.getConfigRoot(), "Templates/Classes/Empty.java");
    }

    public void testStringQualIdent() throws Exception {
        testFile = new File(getWorkDir(), "Test.java");
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "import java.util.List;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    List l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("java.util.List"), null);
                workingCopy.rewrite(clazz, make.addClassMember(clazz, vt));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testStringQualIdentNewlyCreated() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "import foo.A;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    A l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree nueClass = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "A", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Collections.<Tree>emptyList());
                CompilationUnitTree nueCUT = make.CompilationUnit(FileUtil.toFileObject(getWorkDir()), "foo/A.java", Collections.<ImportTree>emptyList(), Collections.singletonList(nueClass));
                workingCopy.rewrite(null, nueCUT);
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("foo.A"), null);
                workingCopy.rewrite(clazz, make.addClassMember(clazz, vt));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testStringQualIdentNewlyCreatedSamePackage() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    A l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree nueClass = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "A", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Collections.<Tree>emptyList());
                CompilationUnitTree nueCUT = make.CompilationUnit(FileUtil.toFileObject(getWorkDir()), "hierbas/del/litoral/A.java", Collections.<ImportTree>emptyList(), Collections.singletonList(nueClass));
                workingCopy.rewrite(null, nueCUT);
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("hierbas.del.litoral.A"), null);
                workingCopy.rewrite(clazz, make.addClassMember(clazz, vt));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testStringQualIdentNewlyCreatedNestedClasses() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "import foo.A.B;\n" +
            "import foo.A.C;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    B l;\n" +
            "    C k;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Preferences preferences = MimeLookup.getLookup(JavaTokenId.language().mimeType()).lookup(Preferences.class);
        preferences.putBoolean("importInnerClasses", true);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree B = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "B", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Collections.<Tree>emptyList());
                ClassTree C = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "C", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Collections.<Tree>emptyList());
                ClassTree nueClass = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "A", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Arrays.asList(B, C));
                CompilationUnitTree nueCUT = make.CompilationUnit(FileUtil.toFileObject(getWorkDir()), "foo/A.java", Collections.<ImportTree>emptyList(), Collections.singletonList(nueClass));
                workingCopy.rewrite(null, nueCUT);
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt1 = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("foo.A.B"), null);
                VariableTree vt2 = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "k", make.QualIdent("foo.A.C"), null);
                workingCopy.rewrite(clazz, make.addClassMember(make.addClassMember(clazz, vt1), vt2));
            }

        };
        src.runModificationTask(task).commit();
        preferences.remove("importInnerClasses");
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testStringQualIdentNewlyCreatedNestedClassesToCurrent() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    A l;\n\n" +
            "    class A {\n" +
            "    }\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree nueClass = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "A", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Collections.<Tree>emptyList());
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("hierbas.del.litoral.Test.A"), null);
                workingCopy.rewrite(clazz, make.addClassMember(make.addClassMember(clazz, vt), nueClass));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testStringQualIdentNewImplements() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "import java.util.Map;\n" +
            "\n" +
            "public class Test implements Map {\n" +
            "\n" +
            "    Entry l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("java.util.Map.Entry"), null);
                workingCopy.rewrite(clazz, make.addClassImplementsClause(make.addClassMember(clazz, vt), make.QualIdent("java.util.Map")));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testType1() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "import java.util.List;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    List<String> l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.Type("java.util.List<java.lang.String>"), null);
                workingCopy.rewrite(clazz, make.addClassMember(clazz, vt));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testType2() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    int[] l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.Type("int[]"), null);
                workingCopy.rewrite(clazz, make.addClassMember(clazz, vt));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testStringQualIdentNonExistent() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    does.not.Exist l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("does.not.Exist"), null);
                workingCopy.rewrite(clazz, make.addClassMember(clazz, vt));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testInternalChangesAreLightweight1() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        String code =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "    {\n" +
            "        new Runnable() {\n" +
            "            public void run() {\n" +
            "|            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";
        final int pos = code.indexOf("|");
        TestUtilities.copyStringToFile(testFile, code.replaceAll(Pattern.quote("|"), ""));
        String golden =
            "package hierbas.del.litoral;\n\n" +
            "import java.util.List;\n" +
            "\n" +
            "public class Test {\n" +
            "    {\n" +
            "        new Runnable() {\n" +
            "            public void run() {\n" +
            "                List l;\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        final AtomicReference<ElementOverlay> overlay = new AtomicReference<ElementOverlay>();
        Task<WorkingCopy> task = new Task<WorkingCopy>() {
            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                TreePath  tp = workingCopy.getTreeUtilities().pathFor(pos);
                BlockTree block = (BlockTree) tp.getLeaf();
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("java.util.List"), null);
                workingCopy.rewrite(block, make.addBlockStatement(block, vt));
                overlay.set(JavaSourceAccessor.getINSTANCE().getJavacTask(workingCopy).getContext().get(ElementOverlay.class));
            }
        };
        src.runModificationTask(task).commit();
        assertEquals(0, overlay.get().totalMapsSize());
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testInternalChangesAreLightweight2() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        String code =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "    {\n" +
            "        new Runnable() {\n" +
            "            public void run() {\n" +
            "                class Foo {{\n" +
            "|               }}\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";
        final int pos = code.indexOf("|");
        TestUtilities.copyStringToFile(testFile, code.replaceAll(Pattern.quote("|"), ""));
        String golden =
            "package hierbas.del.litoral;\n\n" +
            "import java.util.List;\n" +
            "\n" +
            "public class Test {\n" +
            "    {\n" +
            "        new Runnable() {\n" +
            "            public void run() {\n" +
            "                class Foo {{\n" +
            "                        List l;\n" +
            "               }}\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        final AtomicReference<ElementOverlay> overlay = new AtomicReference<ElementOverlay>();
        Task<WorkingCopy> task = new Task<WorkingCopy>() {
            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                TreePath  tp = workingCopy.getTreeUtilities().pathFor(pos);
                BlockTree block = (BlockTree) tp.getLeaf();
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("java.util.List"), null);
                workingCopy.rewrite(block, make.addBlockStatement(block, vt));
                overlay.set(JavaSourceAccessor.getINSTANCE().getJavacTask(workingCopy).getContext().get(ElementOverlay.class));
            }
        };
        src.runModificationTask(task).commit();
        assertEquals(0, overlay.get().totalMapsSize());
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void test192896() throws Exception {
        testFile = new File(getWorkDir(), "Test.java");
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test extends Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "import java.util.List;\n" +
            "\n" +
            "public class Test extends Test {\n" +
            "\n" +
            "    List l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("java.util.List"), null);
                workingCopy.rewrite(clazz, make.addClassMember(clazz, vt));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void test195882() throws Exception {
        clearWorkDir();
        beginTx();
        assertTrue(new File(getWorkDir(), "test").mkdirs());
        testFile = new File(getWorkDir(), "test/Test.java");
        TestUtilities.copyStringToFile(testFile,
            "package test;\n" +
            "\n" +
            "public class Test {\n" +
            "    List orig;\n" +
            "    void t() {\n" +
            "    }\n" +
            "}\n"
            );
        File listFile = new File(getWorkDir(), "test/List.java");
        TestUtilities.copyStringToFile(listFile,
            "package test;\n" +
            "\n" +
            "public class List {\n" +
            "}\n"
            );
        String golden =
            "package test;\n" +
            "\n" +
            "public class Test {\n" +
            "    List orig;\n" +
            "    void t() {\n" +
            "        java.util.List l;\n" +
            "    }\n" +
            "}\n";

        ClasspathInfo cpInfo = ClasspathInfoAccessor.getINSTANCE().create (ClassPathSupport.createClassPath(System.getProperty("sun.boot.class.path")), ClassPath.EMPTY, ClassPathSupport.createClassPath(getSourcePath()), null, true, false, false, true);
        JavaSource src = JavaSource.create(cpInfo, FileUtil.toFileObject(testFile));
        Task<WorkingCopy> task = new Task<WorkingCopy>() {
            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                //check that test.List exists:
                assertNotNull(workingCopy.getElements().getTypeElement("test.List"));
                TreeMaker make = workingCopy.getTreeMaker();
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                MethodTree method = (MethodTree) clazz.getMembers().get(2);
                BlockTree block = method.getBody();
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("java.util.List"), null);
                workingCopy.rewrite(block, make.addBlockStatement(block, vt));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testParameterizedType() throws Exception {
        clearWorkDir();
        beginTx();
        assertTrue(new File(getWorkDir(), "test").mkdirs());
        testFile = new File(getWorkDir(), "test/Test.java");
        TestUtilities.copyStringToFile(testFile,
            "package test;\n" +
            "\n" +
            "public abstract class Test {\n" +
            "}\n"
            );
        String golden =
            "package test;\n" +
            "\n" +
            "import java.util.Map;\n" +
            "\n" +
            "public abstract class Test implements Map<String, String> {\n" +
            "\n" +
            "    Entry e;\n" +
            "}\n";

        ClasspathInfo cpInfo = ClasspathInfoAccessor.getINSTANCE().create (ClassPathSupport.createClassPath(System.getProperty("sun.boot.class.path")), ClassPath.EMPTY, ClassPathSupport.createClassPath(getSourcePath()), null, true, false, false, true);
        JavaSource src = JavaSource.create(cpInfo, FileUtil.toFileObject(testFile));
        Task<WorkingCopy> task = new Task<WorkingCopy>() {
            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "e", make.QualIdent("java.util.Map.Entry"), null);
                workingCopy.rewrite(clazz, make.addClassMember(make.addClassImplementsClause(clazz, make.Type("java.util.Map<String, String>")), vt));
            }

        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testTooSoon206957a() throws Exception {
        clearWorkDir();
        assertTrue(new File(getWorkDir(), "test").mkdirs());
        testFile = new File(getWorkDir(), "test/Test.java");
        TestUtilities.copyStringToFile(testFile,
            "package test;\n" +
            "\n" +
            "import java.util.Map;\n" +
            "\n" +
            "public abstract class Test implements Map {\n" +
            "}\n"
            );
        String golden =
            "package test;\n" +
            "\n" +
            "import java.util.Map;\n" +
            "import java.util.Map.Entry;\n" +
            "\n" +
            "public abstract class Test implements Entry, Map {\n" +
            "}\n";
        final TransactionContext ctx = TransactionContext.beginStandardTransaction(Utilities.toURI(getWorkDir()).toURL(), true, true, false);
        try {
            ClasspathInfo cpInfo = ClasspathInfoAccessor.getINSTANCE().create (ClassPathSupport.createClassPath(System.getProperty("sun.boot.class.path")), ClassPath.EMPTY, ClassPathSupport.createClassPath(getSourcePath()), null, true, false, false, true);
            JavaSource src = JavaSource.create(cpInfo, FileUtil.toFileObject(testFile));
            Preferences preferences = MimeLookup.getLookup(JavaTokenId.language().mimeType()).lookup(Preferences.class);
            preferences.putBoolean("importInnerClasses", true);
            Task<WorkingCopy> task = new Task<WorkingCopy>() {
                public void run(WorkingCopy workingCopy) throws IOException {
                    workingCopy.toPhase(Phase.RESOLVED);
                    TreeMaker make = workingCopy.getTreeMaker();
                    CompilationUnitTree node = workingCopy.getCompilationUnit();
                    ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                    workingCopy.rewrite(clazz, make.insertClassImplementsClause(clazz, 0, make.QualIdent("java.util.Map.Entry")));
                }

            };
            src.runModificationTask(task).commit();
            preferences.remove("importInnerClasses");
            String res = TestUtilities.copyFileToString(testFile);
            System.err.println(res);
            assertEquals(golden, res);
        } finally {
            ctx.commit();
        }
    }

    public void testTooSoon206957b() throws Exception {
        clearWorkDir();
        assertTrue(new File(getWorkDir(), "test").mkdirs());
        testFile = new File(getWorkDir(), "test/Entry.java");
        TestUtilities.copyStringToFile(testFile,
            "package test;\n" +
            "\n" +
            "import java.util.Map;\n" +
            "\n" +
            "public abstract class Entry implements Map {\n" +
            "}\n"
            );
        String golden =
            "package test;\n" +
            "\n" +
            "import java.util.Map;\n" +
            "\n" +
            "public abstract class Entry implements Map.Entry, Map {\n" +
            "}\n";

        final TransactionContext ctx = TransactionContext.beginStandardTransaction(Utilities.toURI(getWorkDir()).toURL(), true, true, false);
        try {
            ClasspathInfo cpInfo = ClasspathInfoAccessor.getINSTANCE().create (ClassPathSupport.createClassPath(System.getProperty("sun.boot.class.path")), ClassPath.EMPTY, ClassPathSupport.createClassPath(getSourcePath()), null, true, false, false, true);
            JavaSource src = JavaSource.create(cpInfo, FileUtil.toFileObject(testFile));
            Task<WorkingCopy> task = new Task<WorkingCopy>() {
                public void run(WorkingCopy workingCopy) throws IOException {
                    workingCopy.toPhase(Phase.RESOLVED);
                    TreeMaker make = workingCopy.getTreeMaker();
                    CompilationUnitTree node = workingCopy.getCompilationUnit();
                    ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                    workingCopy.rewrite(clazz, make.insertClassImplementsClause(clazz, 0, make.QualIdent("java.util.Map.Entry")));
                }

            };
            src.runModificationTask(task).commit();
            String res = TestUtilities.copyFileToString(testFile);
            System.err.println(res);
            assertEquals(golden, res);
        } finally {
            ctx.commit();
        }
    }

    public void test208490() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "import foo.A;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    A l;\n" +
            "}\n";

        final JavaSource src = getJavaSource(testFile);
        src.runUserActionTask(new Task<CompilationController>() {
            @Override public void run(CompilationController parameter) throws Exception {
                parameter.toPhase(Phase.RESOLVED);
        src.runModificationTask(new Task<WorkingCopy>() {
            @Override public void run(WorkingCopy parameter) throws Exception {
                parameter.toPhase(Phase.RESOLVED);
                CompilationUnitTree nue = GeneratorUtilities.get(parameter).addImports(parameter.getCompilationUnit(), Collections.singleton(parameter.getElements().getTypeElement("java.util.Collection")));
                parameter.rewrite(parameter.getCompilationUnit(), nue);
            }
        });
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree nueClass = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "A", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Collections.<Tree>emptyList());
                CompilationUnitTree nueCUT = make.CompilationUnit(FileUtil.toFileObject(getWorkDir()), "foo/A.java", Collections.<ImportTree>emptyList(), Collections.singletonList(nueClass));
                workingCopy.rewrite(null, nueCUT);
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("foo.A"), null);
                workingCopy.rewrite(clazz, make.addClassMember(clazz, vt));
            }

        };
        src.runModificationTask(task).commit();
            }
        }, true);
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }

    public void testStringQualIdentClashWithRemovedClass1() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "class B {\n" +
            "}\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "import foo.A.B;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    B l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Preferences preferences = MimeLookup.getLookup(JavaTokenId.language().mimeType()).lookup(Preferences.class);
        preferences.putBoolean("importInnerClasses", true);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree B = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "B", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Collections.<Tree>emptyList());
                ClassTree nueClass = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "A", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Arrays.asList(B));
                CompilationUnitTree nueCUT = make.CompilationUnit(FileUtil.toFileObject(getWorkDir()), "foo/A.java", Collections.<ImportTree>emptyList(), Collections.singletonList(nueClass));
                workingCopy.rewrite(null, nueCUT);
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt1 = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("foo.A.B"), null);
                workingCopy.rewrite(clazz, make.addClassMember(make.removeClassMember(clazz, 1), vt1));
            }

        };
        src.runModificationTask(task).commit();
        preferences.remove("importInnerClasses");
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }
    
    public void testStringQualIdentClashWithRemovedClass2() throws Exception {
        clearWorkDir();
        testFile = new File(getWorkDir(), "hierbas/del/litoral/Test.java");
        assertTrue(testFile.getParentFile().mkdirs());
        TestUtilities.copyStringToFile(testFile,
            "package hierbas.del.litoral;\n" +
            "\n" +
            "public class Test {\n" +
            "}\n" +
            "class B {\n" +
            "}\n"
            );
        String golden =
            "package hierbas.del.litoral;\n" +
            "\n" +
            "import foo.A.B;\n" +
            "\n" +
            "public class Test {\n" +
            "\n" +
            "    B l;\n" +
            "}\n";

        JavaSource src = getJavaSource(testFile);
        Preferences preferences = MimeLookup.getLookup(JavaTokenId.language().mimeType()).lookup(Preferences.class);
        preferences.putBoolean("importInnerClasses", true);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree B = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "B", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Collections.<Tree>emptyList());
                ClassTree nueClass = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)), "A", Collections.<TypeParameterTree>emptyList(), null, Collections.<Tree>emptyList(), Arrays.asList(B));
                CompilationUnitTree nueCUT = make.CompilationUnit(FileUtil.toFileObject(getWorkDir()), "foo/A.java", Collections.<ImportTree>emptyList(), Collections.singletonList(nueClass));
                workingCopy.rewrite(null, nueCUT);
                CompilationUnitTree node = workingCopy.getCompilationUnit();
                ClassTree clazz = (ClassTree) node.getTypeDecls().get(0);
                VariableTree vt1 = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "l", make.QualIdent("foo.A.B"), null);
                workingCopy.rewrite(clazz, make.addClassMember(clazz, vt1));
                workingCopy.rewrite(node, make.removeCompUnitTypeDecl(node, 1));
            }

        };
        src.runModificationTask(task).commit();
        preferences.remove("importInnerClasses");
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertEquals(golden, res);
    }
 
    String getGoldenPckg() {
        return "";
    }

    String getSourcePckg() {
        return "";
    }

    FileObject[] getSourcePath() {
        try {
            return new FileObject[] {FileUtil.toFileObject(getDataDir()), FileUtil.toFileObject(getWorkDir())};
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
