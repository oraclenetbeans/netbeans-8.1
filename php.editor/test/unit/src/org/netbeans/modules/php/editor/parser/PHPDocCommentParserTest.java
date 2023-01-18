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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.parser;

import java.io.File;
import java.util.List;
import org.netbeans.modules.php.editor.PHPTestBase;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocBlock;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocTag;
import org.netbeans.modules.php.spi.annotation.AnnotationParsedLine;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author PetrPisl
 */
public class PHPDocCommentParserTest extends PHPTestBase {

    public PHPDocCommentParserTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testEmpty() throws Exception {
        String comment = " ";

        PHPDocCommentParser parser = new PHPDocCommentParser();
        PHPDocBlock block = parser.parse(100, 150, comment);
        assertNotNull(block);
        assertEquals("", block.getDescription());
    }

    public void testEmpty2() throws Exception {
        String comment = " *     ";

        PHPDocCommentParser parser = new PHPDocCommentParser();
        PHPDocBlock block = parser.parse(100, 150, comment);
        assertNotNull(block);
        assertEquals("", block.getDescription());
    }

    public void testDescriptionSimple() throws Exception {
        String comment = " simple";

        PHPDocCommentParser parser = new PHPDocCommentParser();
        PHPDocBlock block = parser.parse(100, 150, comment);
        assertNotNull(block);
        assertEquals("simple", block.getDescription());
    }

    public void testDescriptionOnly() throws Exception {
        String comment = " * hello this is a * very simple comment \n * and seccond line";

        PHPDocCommentParser parser = new PHPDocCommentParser();
        PHPDocBlock block = parser.parse(100, 150, comment);
        assertNotNull(block);
        assertEquals("hello this is a * very simple comment\nand seccond line", block.getDescription());
    }

    public void testNoDescriptionOneTag() throws Exception {
        String comment = " * @custom Petr";

        PHPDocCommentParser parser = new PHPDocCommentParser();
        PHPDocBlock block = parser.parse(0, comment.length(), comment);
        List<PHPDocTag> tags = block.getTags();
        assertNotNull(block);
        assertEquals("", block.getDescription());
        assertEquals("Nunber of tags", 1, tags.size());
        AnnotationParsedLine kind = tags.get(0).getKind();
        assertTrue(kind instanceof UnknownAnnotationLine);
        assertEquals("custom", kind.getName());
        assertEquals("Petr", tags.get(0).getValue());
        assertEquals(comment.indexOf("@custom"), tags.get(0).getStartOffset());
        assertEquals(comment.indexOf("@custom Petr") + "@custom Petr".length(), tags.get(0).getEndOffset() - 3);
    }

    public void testNoDescriptionTwoTags() throws Exception {
        String comment = " * @custom Petr  \n * @custom 1.5";

        PHPDocCommentParser parser = new PHPDocCommentParser();
        PHPDocBlock block = parser.parse(0, comment.length(), comment);
        List<PHPDocTag> tags = block.getTags();
        assertNotNull(block);
        assertEquals("", block.getDescription());
        assertEquals("Nunber of tags", 2, tags.size());
        AnnotationParsedLine kind = tags.get(0).getKind();
        assertTrue(kind instanceof UnknownAnnotationLine);
        assertEquals("custom", kind.getName());
        assertEquals(" Petr", tags.get(0).getValue());
        assertEquals(comment.indexOf("@custom"), tags.get(0).getStartOffset());
        assertEquals(comment.indexOf("@custom Petr  ") + "@custom Petr  ".length(), tags.get(0).getEndOffset() - 3);
        AnnotationParsedLine kind1 = tags.get(1).getKind();
        assertTrue(kind1 instanceof UnknownAnnotationLine);
        assertEquals("custom", kind1.getName());
        assertEquals("1.5", tags.get(1).getValue());
        assertEquals(comment.indexOf("@custom 1.5") + 3 , tags.get(1).getStartOffset());
        assertEquals(comment.indexOf("@custom 1.5") + "@custom 1.5".length(), tags.get(1).getEndOffset() - 3);
    }

    public void testNoDescriptionThreeTags() throws Exception {
        String comment = " * @custom Petr  \n *    @custom 1.5  \n *      @custom mine";

        PHPDocCommentParser parser = new PHPDocCommentParser();
        PHPDocBlock block = parser.parse(0, comment.length(), comment);
        List<PHPDocTag> tags = block.getTags();
        assertNotNull(block);
        assertEquals("", block.getDescription());
        assertEquals("Nunber of tags", 3, tags.size());
        AnnotationParsedLine kind = tags.get(0).getKind();
        assertTrue(kind instanceof UnknownAnnotationLine);
        assertEquals("custom", kind.getName());
        assertEquals(" Petr", tags.get(0).getValue());
        assertEquals(comment.indexOf("@custom"), tags.get(0).getStartOffset());
        assertEquals(comment.indexOf("@custom Petr  ") + "@custom Petr  ".length(), tags.get(0).getEndOffset() - 3);
        AnnotationParsedLine kind1 = tags.get(1).getKind();
        assertTrue(kind1 instanceof UnknownAnnotationLine);
        assertEquals("custom", kind1.getName());
        assertEquals(" 1.5", tags.get(1).getValue());
        assertEquals(comment.indexOf("@custom 1.5") + 3 , tags.get(1).getStartOffset());
        assertEquals(comment.indexOf("@custom 1.5") + "@custom 1.5  ".length(), tags.get(1).getEndOffset() - 3);
        AnnotationParsedLine kind2 = tags.get(2).getKind();
        assertTrue(kind2 instanceof UnknownAnnotationLine);
        assertEquals("custom", kind2.getName());
        assertEquals("mine", tags.get(2).getValue());
        assertEquals(comment.indexOf("@custom mine") + 3 , tags.get(2).getStartOffset());
        assertEquals(comment.indexOf("@custom mine") + "@custom mine".length(), tags.get(2).getEndOffset() - 3);
    }

    public void testDescriptionTags() throws Exception {
        String comment = " * hello this is a * very simple comment \n * and seccond line \n  * \n * last line of description\n * @custom   http://www.seznam.cz   \n * @custom1";

        PHPDocCommentParser parser = new PHPDocCommentParser();

        PHPDocBlock block = parser.parse(0, comment.length(), comment);
        assertNotNull(block);
        assertEquals("hello this is a * very simple comment\nand seccond line\n\nlast line of description", block.getDescription().trim());
        List<PHPDocTag> tags = block.getTags();
        assertEquals("Nunber of tags", 2, tags.size());
        AnnotationParsedLine kind = tags.get(0).getKind();
        assertTrue(kind instanceof UnknownAnnotationLine);
        assertEquals("custom", kind.getName());
        assertEquals("   http://www.seznam.cz", tags.get(0).getValue());
        assertEquals(comment.indexOf("@custom") + 3, tags.get(0).getStartOffset());
        assertEquals(comment.indexOf("@custom   http://www.seznam.cz   ") + "@custom   http://www.seznam.cz   ".length(), tags.get(0).getEndOffset() - 3);
        AnnotationParsedLine kind1 = tags.get(1).getKind();
        assertTrue(kind1 instanceof UnknownAnnotationLine);
        assertEquals("custom1", kind1.getName());
        assertEquals("", tags.get(1).getValue());
        assertEquals(comment.indexOf("@custom1") + 3 , tags.get(1).getStartOffset());
        assertEquals(comment.indexOf("@custom1") + "@custom1".length(), tags.get(1).getEndOffset() - 3);
    }

    public void testIssue222836() throws Exception {
        String comment = " * @todo Visible but nothing after white space is not\n * @copyright (c) year, John Doe";
        PHPDocCommentParser parser = new PHPDocCommentParser();
        PHPDocBlock block = parser.parse(0, comment.length(), comment);
        assertNotNull(block);
        List<PHPDocTag> tags = block.getTags();
        assertEquals(2, tags.size());
        AnnotationParsedLine first = tags.get(0).getKind();
        assertTrue(first instanceof UnknownAnnotationLine);
        assertEquals("todo", first.getName());
        assertEquals("Visible but nothing after white space is not", first.getDescription());
        AnnotationParsedLine second = tags.get(1).getKind();
        assertTrue(second instanceof UnknownAnnotationLine);
        assertEquals("copyright", second.getName());
        assertEquals("(c) year, John Doe", second.getDescription());
    }

    public void testDescriptionWithHtml() throws Exception {
        String comment = "*   <dd> \"*word\"  => ENDS_WITH(word)\n *   <dd> \"/^word.* /\" => REGEX(^word.*)\n *   <dd> \"word*word\" => REGEX(word.*word)";
        PHPDocCommentParser parser = new PHPDocCommentParser();

        PHPDocBlock block = parser.parse(100, 150, comment);
        assertNotNull(block);
    }

    public void testProperty01() throws Exception {
        String comment = " * PHP Template.\n" +
                " * @property string $name\n" +
                " * @property-read int $ahoj\n" +
                " * @property-write int $death";
        perform(comment, "Property01");
    }

    public void testProperty02() throws Exception {
        String comment = " * PHP Template.\n" +
                " * @property";
        perform(comment, "Property02");
    }

    public void testProperty03() throws Exception {
        String comment = " * PHP Template.\n" +
                " * @property string \n" +
                " * @property-read int $ahoj\n" +
                " * @property-write int $death";
        perform(comment, "Property03");
    }

    public void testProperty04() throws Exception {
        String comment = " * PHP Template.\n" +
                " * @property-read int $ahoj there is some doc\n" +
                " * @property-write int $death";
        perform(comment, "Property04");
    }

    public void testReturnType01() throws Exception {
        String comment = " * Function XYZ.\n" +
                " * @return string\n";
        perform(comment, "ReturnType01");
    }

    public void testReturnType02() throws Exception {
        String comment = " * Function XYZ.\n" +
                " * @return string|int test documentation\n";
        perform(comment, "ReturnType02");
    }

    public void testReturnType03() throws Exception {
        String comment = " * Function XYZ.\n" +
                " * @return TClass::CONSTANT test documentation\n";
        perform(comment, "ReturnType03");
    }

    public void testParamReturn01() throws Exception {
        String comment =
                "   * Retrieves the entry at a specific index.\n" +
                "   *\n" +
                "   * @param int $index An entry index\n" +
                "   *\n" +
                "   * @return sfActionStackEntry An action stack entry implementation.\n";
        perform(comment, "ParamReturn01");
    }

    public void testExample01() throws Exception {
        String comment =
         "* Dispatches to the action defined by the 'action' parameter of the sfRequest object.\n" +
         "   *\n" +
         "   * This method try to execute the executeXXX() method of the current object where XXX is the\n" +
         "   * defined action name.\n" +
         "   *\n" +
         "   * @param  sfRequest $request The current sfRequest object\n" +
         "   *\n" +
         "   * @return string    A string containing the view name associated with this action\n" +
         "   *\n" +
         "   * @throws sfInitializationException\n" +
         "   *\n" +
         "   * @see sfAction";
        perform(comment, "Example01");
    }

    public void testArrayParam01() throws Exception {
        String comment = " * Function XYZ.\n" +
                " * @param Car[] $test\n";
        perform(comment, "ArrayParam01");
    }

    public void testArrayParam02() throws Exception {
        String comment = " * Function XYZ.\n" +
                " * @param Car[] $test\n";
        perform(comment, "ArrayParam02");
    }

    public void testHTMLWrapper() throws Exception {
        String comment =
                "*\n" +
                "* @throws <b>sfInitializationException</b> If an error occurs while initializing this sfCache instance.";
        perform(comment, "HTMLWrapper");
    }

    public void testIssue197946() throws Exception {
        String comment =
                "/**\n" +
                " * This is the model class for table \"cliente\".\n" +
                " *\n" +
                " * The followings are the available columns in table 'cliente':\n" +
                " * @property string $id_cliente\n" +
                " * ....\n" +
                " * ....\n" +
                " * @property integer $id_profissao\n" +
                " * @property string $renda_mensal\n" +
                " *\n" +
                " * The followings are the available model relations:\n" +
                " * ....\n" +
                " * ....\n" +
                " */\n";
        perform(comment, "Issue197946");
    }

    public void perform(String comment, String filename) throws Exception {
        PHPDocCommentParser parser = new PHPDocCommentParser();
        PHPDocBlock block = parser.parse(0, comment.length(), comment);
        PrintASTVisitor visitor = new PrintASTVisitor();
        String result = visitor.printTree(block);

        // try to find golden file
        String fullClassName = this.getClass().getName();
        String goldenFileDir = fullClassName.replace('.', '/');
        String goldenFolder = getDataSourceDir().getAbsolutePath() + "/goldenfiles/" + goldenFileDir + "/";
        File goldenFile = new File(goldenFolder + filename + ".pass");
        if (!goldenFile.exists()) {
            // if doesn't exist, create it
            FileObject goldenFO = touch(goldenFolder, filename + ".pass");
            copyStringToFileObject(goldenFO, result);
        }
        else {
            // if exist, compare it.
            goldenFile = getGoldenFile(filename + ".pass");
            FileObject resultFO = touch(getWorkDir(), filename + ".result");
            copyStringToFileObject(resultFO, result);
            assertFile(FileUtil.toFile(resultFO), goldenFile, getWorkDir());
        }
    }
}
