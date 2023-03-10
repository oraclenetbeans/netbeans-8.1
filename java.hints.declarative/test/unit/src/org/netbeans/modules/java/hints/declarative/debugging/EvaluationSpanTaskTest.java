/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints.declarative.debugging;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.Document;
import org.netbeans.api.java.lexer.JavaTokenId;
import static org.junit.Assert.*;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.api.lexer.Language;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.source.TreeLoader;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/**
 *
 * @author Jan Lahoda
 */
public class EvaluationSpanTaskTest extends NbTestCase {

    public EvaluationSpanTaskTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SourceUtilsTestUtil.prepareTest(new String[] {"org/netbeans/modules/java/editor/resources/layer.xml"}, new Object[0]);
        TreeLoader.DISABLE_CONFINEMENT_TEST = true;
    }

    public void testHintConditions() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    private final Object oo;\n" +
                    "    public void test() {|/*bar*/\n" +
                    "        synchronized(oo) {\n" +
                    "            System.err.println(1);\n" +
                    "        }//foo\n" +
                    "    |}\n" +
                    "}\n",
                    "|synchronized ($var) {\n" +
                    "     $stmts$;\n" +
                    "}|\n :: ^!hasModifier($var, Modifier.FINAL)^ && |elementKindMatches($var, ElementKind.FIELD)|;;");
    }

    public void testNegative() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    private final Object oo;\n" +
                    "    public void test() {\n" +
                    "        synchronized(oo) {\n" +
                    "            |System.err.println(1);|\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n",
                    "^synchronized ($var) {\n" +
                    "     $stmts$;\n" +
                    "}^\n :: !hasModifier($var, Modifier.FINAL) && elementKindMatches($var, ElementKind.FIELD);;");
    }

    public void testFixConditions() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    private final Object oo;\n" +
                    "    public void test() {\n" +
                    "        |synchronized(oo) {\n" +
                    "            System.err.println(1);\n" +
                    "        }|\n" +
                    "    }\n" +
                    "}\n",
                    "|synchronized ($var) {\n" +
                    "     $stmts$;\n" +
                    "}|\n :: ^!hasModifier($var, Modifier.FINAL)^ && |elementKindMatches($var, ElementKind.FIELD)|\n" +
                    "=>1 :: ^hasModifier($var, Modifier.STATIC)^\n" +
                    "=>2 :: ^elementKindMatches($var, ElementKind.METHOD)^;;");
    }

    private static final Pattern SPLIT = Pattern.compile("(\\|)|(\\^)");
    
    private void performTest(String code, String hintSpec) throws Exception {
        StringBuilder sb = new StringBuilder();
        Matcher m = SPLIT.matcher(hintSpec);
        int lastStart = 0;
        int[] span = new int[] {-1, -1};
        List<int[]> passedGolden = new LinkedList<int[]>();
        List<int[]> failedGolden = new LinkedList<int[]>();

        while (m.find()) {
            sb.append(hintSpec.substring(lastStart, m.start()));

            if (span[0] == (-1)) {
                span[0] = sb.length();
            } else {
                span[1] = sb.length();
                if (m.group(1) != null) {
                    passedGolden.add(span);
                } else {
                    failedGolden.add(span);
                }
                span = new int[] {-1, -1};
            }
            lastStart = m.end();
        }

        sb.append(hintSpec.substring(lastStart, hintSpec.length()));

        String[] codeSplit = code.split("\\|");
        
        prepareTest("test/Test.java", codeSplit[0] + codeSplit[1] + codeSplit[2]);
        
        Collection<? extends HintWrapper> w = HintWrapper.parse(info.getFileObject(), sb.toString());

        List<int[]> passedActual = new LinkedList<int[]>();
        List<int[]> failedActual = new LinkedList<int[]>();

        EvaluationSpanTask.computeHighlights(info,
                                             codeSplit[0].length(),
                                             codeSplit[0].length() + codeSplit[1].length(),
                                             w,
                                             passedActual,
                                             failedActual);

        assertEquals(passedGolden, passedActual);
        assertEquals(failedGolden, failedActual);
    }

    private static void assertEquals(List<int[]> golden, List<int[]> actual) {
        assertEquals(golden.size(), actual.size());

        Iterator<int[]> g = golden.iterator();
        Iterator<int[]> a = actual.iterator();

        while (g.hasNext() && a.hasNext()) {
            int[] gSpan = g.next();
            int[] aSpan = a.next();

            assertTrue("Expected: " + Arrays.toString(gSpan) + ", but was: " + Arrays.toString(aSpan),
                       Arrays.equals(gSpan, aSpan));
        }
    }

    private void prepareTest(String fileName, String code) throws Exception {
        clearWorkDir();
        File wdFile = getWorkDir();
        FileUtil.refreshFor(wdFile);

        FileObject wd = FileUtil.toFileObject(wdFile);
        assertNotNull(wd);
        sourceRoot = FileUtil.createFolder(wd, "src");
        FileObject buildRoot = FileUtil.createFolder(wd, "build");
        FileObject cache = FileUtil.createFolder(wd, "cache");

        FileObject data = FileUtil.createData(sourceRoot, fileName);
        File dataFile = FileUtil.toFile(data);

        assertNotNull(dataFile);

        TestUtilities.copyStringToFile(dataFile, code);

        SourceUtilsTestUtil.prepareTest(sourceRoot, buildRoot, cache);

        DataObject od = DataObject.find(data);
        EditorCookie ec = od.getLookup().lookup(EditorCookie.class);

        assertNotNull(ec);

        doc = ec.openDocument();
        doc.putProperty(Language.class, JavaTokenId.language());
        doc.putProperty("mimeType", "text/x-java");

        JavaSource js = JavaSource.forFileObject(data);

        assertNotNull(js);

        info = SourceUtilsTestUtil.getCompilationInfo(js, Phase.RESOLVED);

        assertNotNull(info);
    }

    private FileObject sourceRoot;
    private CompilationInfo info;
    private Document doc;

}