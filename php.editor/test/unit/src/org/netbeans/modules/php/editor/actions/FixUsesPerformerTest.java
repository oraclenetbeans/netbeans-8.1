/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.text.Document;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.php.editor.PHPTestBase;
import org.netbeans.modules.php.editor.actions.FixUsesAction.Options;
import org.netbeans.modules.php.editor.actions.ImportData.ItemVariant;
import org.netbeans.modules.php.editor.api.ElementQueryFactory;
import org.netbeans.modules.php.editor.api.QuerySupportFactory;
import org.netbeans.modules.php.editor.indent.CodeStyle;
import org.netbeans.modules.php.editor.model.FileScope;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.NamespaceScope;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.project.api.PhpSourcePath;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class FixUsesPerformerTest extends PHPTestBase {
    private static final String CAN_NOT_BE_RESOLVED = "<CAN-NOT-BE-RRESOLVED>"; //NOI18N

    public FixUsesPerformerTest(String testName) {
        super(testName);
    }

    public void testIssue210093_01() throws Exception {
        String[] selections = new String[] {"\\Issue\\Martin\\Pondeli"};
        Options options = new Options(false, false, true, false, false);
        performTest("function testFail(\\Issue\\Martin\\Pond^eli $param) {}", createSelections(selections, ItemVariant.Type.CLASS), true, options);
    }

    public void testIssue210093_02() throws Exception {
        String[] selections = new String[] {"\\Issue\\Martin\\Pondeli"};
        Options options = new Options(false, false, false, false, false);
        performTest("function testFail(\\Issue\\Martin\\Pond^eli $param) {}", createSelections(selections, ItemVariant.Type.CLASS), true, options);
    }

    public void testIssue211566_01() throws Exception {
        String[] selections = new String[] {"\\Foo\\Bar\\Baz"};
        Options options = new Options(false, false, false, false, false);
        performTest("new \\Foo\\Bar\\B^az(); //HERE", createSelections(selections, ItemVariant.Type.CLASS), true, options);
    }

    public void testIssue211566_02() throws Exception {
        String[] selections = new String[] {"\\Foo\\Bar\\Baz"};
        Options options = new Options(false, false, true, false, false);
        performTest("new \\Foo\\Bar\\B^az(); //HERE", createSelections(selections, ItemVariant.Type.CLASS), true, options);
    }

    public void testIssue214699() throws Exception {
        String[] selections = new String[] {"\\Foo\\Bar\\ClassName", "\\Baz\\Bat\\ClassName", "\\Fom\\Bom\\ClassName"};
        performTest("$a = new ClassName();^//HERE", createSelections(selections, ItemVariant.Type.CLASS));
    }

    public void testIssue211585_01() throws Exception {
        String[] selections = new String[] {"\\Foo\\Bar\\ClassName", "\\Baz\\Bat\\ClassName", "\\Fom\\Bom\\ClassName"};
        Options options = new Options(false, false, true, true, false);
        performTest("$a = new ClassName();^//HERE", createSelections(selections, ItemVariant.Type.CLASS), false, options);
    }

    public void testIssue211585_02() throws Exception {
        String[] selections = new String[] {"\\Fom\\Bom\\ClassName", "\\Foo\\Bar\\ClassName", "\\Baz\\Bat\\ClassName"};
        Options options = new Options(false, false, true, true, false);
        performTest("$a = new ClassName();^//HERE", createSelections(selections, ItemVariant.Type.CLASS), false, options);
    }

    public void testIssue233527() throws Exception {
        String[] selections = new String[] {"\\NS1\\NS2\\SomeClass", "\\NS1\\NS2\\SomeClass"};
        Options options = new Options(false, false, true, true, false);
        performTest("public function test(SomeClass $a) {^", createSelections(selections, ItemVariant.Type.CLASS), false, options);
    }

    public void testIssue222595_01() throws Exception {
        String[] selections = new String[] {"\\pl\\dagguh\\people\\Person"};
        Options options = new Options(false, false, false, true, false);
        performTest("function assignRoom(Room $room, Person $roomOwner);^", createSelections(selections, ItemVariant.Type.INTERFACE), false, options);
    }

    public void testIssue222595_02() throws Exception {
        String[] selections = new String[] {"\\pl\\dagguh\\people\\Person"};
        Options options = new Options(true, false, true, true, false);
        performTest("function assignRoom(Room $room, Person $roomOwner);^", createSelections(selections, ItemVariant.Type.INTERFACE), false, options);
    }

    public void testIssue222595_03() throws Exception {
        String[] selections = new String[] {};
        Options options = new Options(false, false, true, true, false);
        performTest("function addRoom(\\pl\\dagguh\\buildings\\Room $room);^", createSelections(selections, ItemVariant.Type.NONE), false, options);
    }

    public void testIssue222595_04() throws Exception {
        String[] selections = new String[] {};
        Options options = new Options(true, false, true, true, false);
        performTest("function addRoom(\\pl\\dagguh\\buildings\\Room $room);^", createSelections(selections, ItemVariant.Type.NONE), false, options);
    }

    public void testIssue238828() throws Exception {
        String[] selections = new String[] {"\\First\\Second\\Util", CAN_NOT_BE_RESOLVED, CAN_NOT_BE_RESOLVED, CAN_NOT_BE_RESOLVED};
        Options options = new Options(true, false, true, true, false);
        performTest("function functionName3($param) {}^", createSelections(selections, ItemVariant.Type.CLASS), false, options);
    }

    public void testUseFuncAndConst_01() throws Exception {
        List<Selection> selections = new ArrayList<>();
        selections.add(new Selection("Name\\Space\\Bar2", ItemVariant.Type.CLASS));
        selections.add(new Selection("Name\\Space\\FOO", ItemVariant.Type.CONST));
        selections.add(new Selection("Name\\Space\\Bar", ItemVariant.Type.CLASS));
        selections.add(new Selection("Name\\Space\\FOO2", ItemVariant.Type.CONST));
        selections.add(new Selection("Name\\Space\\fnc2", ItemVariant.Type.FUNCTION));
        selections.add(new Selection("Name\\Space\\fnc", ItemVariant.Type.FUNCTION));
        Options options = new Options(false, false, false, false, true);
        performTest("Name\\Space\\fnc2();^", selections, false, options);
    }

    public void testUseFuncAndConst_02() throws Exception {
        List<Selection> selections = new ArrayList<>();
        selections.add(new Selection("Name\\Space\\Bar2", ItemVariant.Type.CLASS));
        selections.add(new Selection("Name\\Space\\FOO", ItemVariant.Type.CONST));
        selections.add(new Selection("Name\\Space\\Bar", ItemVariant.Type.CLASS));
        selections.add(new Selection("Name\\Space\\FOO2", ItemVariant.Type.CONST));
        selections.add(new Selection("Name\\Space\\fnc2", ItemVariant.Type.FUNCTION));
        selections.add(new Selection("Name\\Space\\fnc", ItemVariant.Type.FUNCTION));
        Options options = new Options(false, true, false, false, true);
        performTest("Name\\Space\\fnc2();^", selections, false, options);
    }

    public void testUseFuncAndConst_03() throws Exception {
        List<Selection> selections = new ArrayList<>();
        selections.add(new Selection("Name\\Space\\Bar2", ItemVariant.Type.CLASS));
        selections.add(new Selection("Name\\Space\\Bar", ItemVariant.Type.CLASS));
        selections.add(new Selection("Name\\Space\\fnc2", ItemVariant.Type.FUNCTION));
        selections.add(new Selection("Name\\Space\\fnc", ItemVariant.Type.FUNCTION));
        Options options = new Options(false, true, false, false, true);
        performTest("function __construct() {^", selections, false, options);
    }

    public void testUseFuncAndConst_04() throws Exception {
        List<Selection> selections = new ArrayList<>();
        selections.add(new Selection("Name\\Space\\Bar2", ItemVariant.Type.CLASS));
        selections.add(new Selection("Name\\Space\\FOO", ItemVariant.Type.CONST));
        selections.add(new Selection("Name\\Space\\Bar", ItemVariant.Type.CLASS));
        selections.add(new Selection("Name\\Space\\FOO2", ItemVariant.Type.CONST));
        Options options = new Options(false, true, false, false, true);
        performTest("function __construct() {^", selections, false, options);
    }

    public void testUseFuncAndConst_05() throws Exception {
        List<Selection> selections = new ArrayList<>();
        selections.add(new Selection("Name\\Space\\FOO", ItemVariant.Type.CONST));
        selections.add(new Selection("Name\\Space\\FOO2", ItemVariant.Type.CONST));
        selections.add(new Selection("Name\\Space\\fnc2", ItemVariant.Type.FUNCTION));
        selections.add(new Selection("Name\\Space\\fnc", ItemVariant.Type.FUNCTION));
        Options options = new Options(false, true, false, false, true);
        performTest("function __construct() {^", selections, false, options);
    }

    public void testIssue243271_01() throws Exception {
        List<Selection> selections = new ArrayList<>();
        selections.add(new Selection("SomeClassAlias", ItemVariant.Type.CLASS, true));
        Options options = new Options(false, false, false, false, false);
        performTest("public function getSomething(SomeClassAlias $someClass) {}^", selections, false, options);
    }

    public void testIssue243271_02() throws Exception {
        List<Selection> selections = new ArrayList<>();
        Options options = new Options(false, false, false, false, false);
        performTest("public function getSomething(SomeClassAlias $someClass) {}^", selections, false, options);
    }

    private String getTestResult(final String fileName, final String caretLine, final List<Selection> selections, final boolean removeUnusedUses, final Options options) throws Exception {
        FileObject testFile = getTestFile(fileName);

        Source testSource = getTestSource(testFile);

        final int caretOffset;
        if (caretLine != null) {
            caretOffset = getCaretOffset(testSource.createSnapshot().getText().toString(), caretLine);
            enforceCaretOffset(testSource, caretOffset);
        } else {
            caretOffset = -1;
        }
        final String[] result = new String[1];
        ParserManager.parse(Collections.singleton(testSource), new UserTask() {

            @Override
            public void run(final ResultIterator resultIterator) throws Exception {
                Parser.Result r = caretOffset == -1 ? resultIterator.getParserResult() : resultIterator.getParserResult(caretOffset);
                if (r != null) {
                    assertTrue(r instanceof ParserResult);
                    PHPParseResult phpResult = (PHPParseResult)r;
                    Map<String, List<UsedNamespaceName>> usedNames = new UsedNamesCollector(phpResult, caretOffset).collectNames();
                    FileScope fileScope = phpResult.getModel().getFileScope();
                    NamespaceScope namespaceScope = ModelUtils.getNamespaceScope(fileScope, caretOffset);
                    Options currentOptions = options;
                    Document document = phpResult.getSnapshot().getSource().getDocument(false);
                    if (currentOptions == null) {
                        CodeStyle codeStyle = CodeStyle.get(document);
                        currentOptions = new FixUsesAction.Options(codeStyle, fileScope.getFileObject());
                    }
                    ImportData importData = new ImportDataCreator(
                            usedNames,
                            ElementQueryFactory.createIndexQuery(QuerySupportFactory.get(phpResult)),
                            namespaceScope.getNamespaceName(),
                            currentOptions).create();
                    final List<ItemVariant> properSelections = new ArrayList<>();
                    for (Selection selection : selections) {
                        properSelections.add(new ItemVariant(
                                selection.getSelection(),
                                CAN_NOT_BE_RESOLVED.equals(selection.getSelection()) ? ItemVariant.UsagePolicy.CAN_NOT_BE_USED : ItemVariant.UsagePolicy.CAN_BE_USED,
                                selection.getType(),
                                selection.isAlias()));
                    }
                    importData.caretPosition = caretOffset;
                    FixUsesPerformer fixUsesPerformer = new FixUsesPerformer(phpResult, importData, properSelections, removeUnusedUses, currentOptions);
                    fixUsesPerformer.perform();
                    result[0] = document.getText(0, document.getLength());
                }
            }
        });
        return result[0];
    }

    private void performTest(final String caretLine, final List<Selection> selections) throws Exception {
        performTest(caretLine, selections, true, null);
    }

    private void performTest(final String caretLine, final List<Selection> selections, final boolean removeUnusedUses) throws Exception {
        performTest(caretLine, selections, removeUnusedUses, null);
    }

    private void performTest(final String caretLine, final List<Selection> selections, final boolean removeUnusedUses, final Options options) throws Exception {
        String exactFileName = getTestPath();
        String result = getTestResult(exactFileName, caretLine, selections, removeUnusedUses, options);
        assertDescriptionMatches(exactFileName, result, false, ".fixUses");
    }

    protected FileObject[] createSourceClassPathsForTest() {
        final File folder = new File(getDataDir(), getTestFolderPath());
        return new FileObject[]{FileUtil.toFileObject(folder)};
    }

    @Override
    protected Map<String, ClassPath> createClassPathsForTest() {
        return Collections.singletonMap(
            PhpSourcePath.SOURCE_CP,
            ClassPathSupport.createClassPath(new FileObject[] {
                FileUtil.toFileObject(new File(getDataDir(), getTestFolderPath()))
            })
        );
    }

    private String getTestFolderPath() {
        return "testfiles/actions/" + transformTestMethodNameToDirectory();//NOI18N
    }

    private String getTestPath() {
        return getTestFolderPath() + "/" + getName() + ".php";//NOI18N
    }

    private String transformTestMethodNameToDirectory() {
        return getName().replace('_', '/');
    }

    private static final class Selection {
        private final String selection;
        private final ItemVariant.Type type;
        private final boolean isAlias;

        public Selection(String selection, ItemVariant.Type type) {
            this(selection, type, false);
        }

        public Selection(String selection, ItemVariant.Type type, boolean isAlias) {
            this.selection = selection;
            this.type = type;
            this.isAlias = isAlias;
        }

        public String getSelection() {
            return selection;
        }

        public ItemVariant.Type getType() {
            return type;
        }

        public boolean isAlias() {
            return isAlias;
        }

    }

    private static List<Selection> createSelections(String[] selections, ItemVariant.Type type) {
        List<Selection> result = new ArrayList<>();
        for (String selection : selections) {
            result.add(new Selection(selection, type));
        }
        return result;
    }

}
