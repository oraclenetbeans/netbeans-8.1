/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.editor.imports;

import java.io.IOException;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import static org.junit.Assert.*;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.api.lexer.Language;
import org.netbeans.junit.NbTestCase;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;

/**
 *
 * @author lahvac
 */
public class ClipboardHandlerTest extends NbTestCase {

    public ClipboardHandlerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        SourceUtilsTestUtil.prepareTest(new String[] {"META-INF/generated-layer.xml", "org/netbeans/modules/java/source/resources/layer.xml", "org/netbeans/modules/java/editor/resources/layer.xml"}, new Object[0]);
        ClipboardHandler.autoImport = true;
        super.setUp();
    }

    public void testSimple() throws Exception {
        copyAndPaste("package test;\nimport java.util.List;\npublic class Test { |List l;| }\n", "package test;\npublic Target {\n^\n}", "package test;\n\nimport java.util.List;\n\npublic Target {\nList l;\n}");
    }

    public void testFieldGroup1() throws Exception {
        copyAndPaste("package test;\nimport java.util.List;\npublic class Test { |List l1, l2;| }\n", "package test;\npublic Target {\n^\n}", "package test;\n\nimport java.util.List;\n\npublic Target {\nList l1, l2;\n}");
    }

    public void testFieldGroup2() throws Exception {
        copyAndPaste("package test;\nimport java.util.List;\npublic class Test { |@SuppressWarnings(\"deprecated\") List l1, l2;| }\n", "package test;\npublic Target {\n^\n}", "package test;\n\nimport java.util.List;\n\npublic Target {\n@SuppressWarnings(\"deprecated\") List l1, l2;\n}");
    }

    public void testCopyIntoComment() throws Exception {
        copyAndPaste("package test;\nimport java.util.List;\npublic class Test { |List l;| }\n", "package test;\npublic Target {\n/*^*/\n}", "package test;\npublic Target {\n/*List l;*/\n}");
    }
    
    public void testClassCopied() throws Exception {
        copyAndPaste("package test;\nimport java.util.List;\npublic class Test { |class Inner { } Inner i = new InnerSub(); class InnerSub extends Inner { }| }\n", "package test;\npublic Target {\n^\n}", "package test;\npublic Target {\nclass Inner { } Inner i = new InnerSub(); class InnerSub extends Inner { }\n}");
    }
    
    public void testClassNotCopied() throws Exception {
        copyAndPaste("package test;\nimport java.util.List;\npublic class Test { static class Inner { } |Inner i;| }\n", "package test;\npublic Target {\n^\n}", "package test;\npublic Target {\nTest.Inner i;\n}");
    }
    
    public void testMethodCopied() throws Exception {
        copyAndPaste("package test;\nimport java.util.List;\npublic class Test { |public static int m1() {return 0;} int one = m1(); int two = m2(); public static int m2() {return 0;}| }\n", "package test;\npublic Target {\n^\n}", "package test;\npublic Target {\npublic static int m1() {return 0;} int one = m1(); int two = m2(); public static int m2() {return 0;}\n}");
    }
    
    private void copyAndPaste(String from, final String to, String golden) throws Exception {
        final int pastePos = to.indexOf('^');

        assertTrue(pastePos >= 0);

        String[] split = from.split(Pattern.quote("|"));

        assertEquals(3, split.length);

        final String cleanFrom = split[0] + split[1] + split[2];

        final int start = split[0].length();
        final int end = start + split[1].length();

        FileObject wd = SourceUtilsTestUtil.makeScratchDir(this);
        final FileObject src = wd.createFolder("src");
        FileObject build = wd.createFolder("build");
        FileObject cache = wd.createFolder("cache");

        SourceUtilsTestUtil.prepareTest(src, build, cache);
        SourceUtilsTestUtil.compileRecursively(src);

        final JEditorPane[] target = new JEditorPane[1];
        final Exception[] fromAWT = new Exception[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                try {
                    JEditorPane source = paneFor(src, "test/Test.java", cleanFrom);

                    Thread.sleep(2000);//XXX

                    source.setSelectionStart(start);
                    source.setSelectionEnd(end);

                    source.copy();

                    target[0] = paneFor(src, "test/Target.java", to.replaceAll(Pattern.quote("^"), ""));

                    target[0].setCaretPosition(pastePos);

                    target[0].paste();
                } catch (Exception ex) {
                    fromAWT[0] = ex;
                }
            }
        });

        if (fromAWT[0] != null) throw fromAWT[0];

        final String[] actual = new String[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                actual[0] = target[0].getText();
            }
        });
        
        assertEquals(golden, actual[0]);
    }

    private JEditorPane paneFor(FileObject src, String fileName, String code) throws Exception, DataObjectNotFoundException, IOException {
        FileObject fromFO = FileUtil.createData(src, fileName);
        TestUtilities.copyStringToFile(fromFO, code);
        DataObject od = DataObject.find(fromFO);
        EditorCookie ec = od.getCookie(EditorCookie.class);

        ec.open();
        
        ec.getDocument().putProperty(Language.class, JavaTokenId.language());

        return ec.getOpenedPanes()[0];
    }

}
