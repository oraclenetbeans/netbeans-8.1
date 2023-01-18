/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript2.editor;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.swing.text.Document;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import org.netbeans.modules.csl.api.ColoringAttributes;
import org.netbeans.modules.csl.api.DeclarationFinder;
import org.netbeans.modules.csl.api.OccurrencesFinder;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.SemanticAnalyzer;
import static org.netbeans.modules.csl.api.test.CslTestBase.getCaretOffset;
import org.netbeans.modules.csl.spi.GsfUtilities;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.Parser;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Petr Pisl
 */
public class JsWithBase extends JsCodeCompletionBase{

    public JsWithBase(String testName) {
        super(testName);
    }
    
    @Override
    protected DeclarationFinder.DeclarationLocation findDeclaration(String relFilePath, final String caretLine) throws Exception {
        Source testSource = getTestSource(getTestFile(relFilePath));

        final int caretOffset = getCaretOffset(testSource.createSnapshot().getText().toString(), caretLine);
        enforceCaretOffset(testSource, caretOffset);

        final DeclarationFinder.DeclarationLocation [] location = new DeclarationFinder.DeclarationLocation[] { null };
        ParserManager.parse(Collections.singleton(testSource), new UserTask() {
            public @Override void run(ResultIterator resultIterator) throws Exception {
                Parser.Result r = resultIterator.getParserResult();
                assertTrue(r instanceof JsParserResult);
                JsParserResult pr = (JsParserResult) r;
                pr.getModel().getGlobalObject();
                DeclarationFinder finder = getFinder();
                location[0] = finder.findDeclaration(pr, caretOffset);
            }
        });

        return location[0];
    }
    
    @Override
    protected void assertDescriptionMatches(FileObject fileObject,
            String description, boolean includeTestName, String ext, boolean goldenFileInTestFileDir) throws IOException {
        super.assertDescriptionMatches(fileObject, description, includeTestName, ext, true);
    }
    
    @Override
    protected void checkOccurrences(String relFilePath, String caretLine, final boolean symmetric) throws Exception {
        Source testSource = getTestSource(getTestFile(relFilePath));

        Document doc = testSource.getDocument(true);
        final int caretOffset = getCaretOffset(doc.getText(0, doc.getLength()), caretLine);

        final OccurrencesFinder finder = getOccurrencesFinder();
        assertNotNull("getOccurrencesFinder must be implemented", finder);
        finder.setCaretPosition(caretOffset);

        ParserManager.parse(Collections.singleton(testSource), new UserTask() {
            public @Override void run(ResultIterator resultIterator) throws Exception {
                Parser.Result r = resultIterator.getParserResult(caretOffset);
                if (r instanceof JsParserResult) {
                    ((JsParserResult)r).getModel().getGlobalObject();
                    finder.run((ParserResult) r, null);
                    Map<OffsetRange, ColoringAttributes> occurrences = finder.getOccurrences();
                    if (occurrences == null) {
                        occurrences = Collections.emptyMap();
                    }

                    String annotatedSource = annotateFinderResult(resultIterator.getSnapshot(), occurrences, caretOffset);
                    assertDescriptionMatches(resultIterator.getSnapshot().getSource().getFileObject(), annotatedSource, true, ".occurrences");

                    if (symmetric) {
                        // Extra check: Ensure that occurrences are symmetric: Placing the caret on ANY of the occurrences
                        // should produce the same set!!
                        for (OffsetRange range : occurrences.keySet()) {
                            int midPoint = range.getStart() + range.getLength() / 2;
                            finder.setCaretPosition(midPoint);
                            finder.run((ParserResult) r, null);
                            Map<OffsetRange, ColoringAttributes> alternates = finder.getOccurrences();
                            assertEquals("Marks differ between caret positions - failed at " + midPoint, occurrences, alternates);
                        }
                    }
                }
            }
        });
    }
    
    @Override
    protected void checkSemantic(final String relFilePath, final String caretLine) throws Exception {
        Source testSource = getTestSource(getTestFile(relFilePath));

        if (caretLine != null) {
            int caretOffset = getCaretOffset(testSource.createSnapshot().getText().toString(), caretLine);
            enforceCaretOffset(testSource, caretOffset);
        }

        ParserManager.parse(Collections.singleton(testSource), new UserTask() {
            public @Override void run(ResultIterator resultIterator) throws Exception {
                Parser.Result r = resultIterator.getParserResult();
                assertTrue(r instanceof ParserResult);
                JsParserResult pr = (JsParserResult) r;
                
                pr.getModel().getGlobalObject();
                
                SemanticAnalyzer analyzer = getSemanticAnalyzer();
                assertNotNull("getSemanticAnalyzer must be implemented", analyzer);

                analyzer.run(pr, null);
                Map<OffsetRange, Set<ColoringAttributes>> highlights = analyzer.getHighlights();

                if (highlights == null) {
                    highlights = Collections.emptyMap();
                }

                Document doc = GsfUtilities.getDocument(pr.getSnapshot().getSource().getFileObject(), true);
                checkNoOverlaps(highlights.keySet(), doc);

                String annotatedSource = annotateSemanticResults(doc, highlights);
                assertDescriptionMatches(relFilePath, annotatedSource, false, ".semantic");
            }
        });
    }
}
