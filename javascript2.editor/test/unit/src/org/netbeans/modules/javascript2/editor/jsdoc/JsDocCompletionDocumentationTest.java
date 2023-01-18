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
package org.netbeans.modules.javascript2.editor.jsdoc;

import java.util.Collections;
import org.netbeans.modules.javascript2.editor.doc.JsDocumentationTestBase;
import org.netbeans.modules.javascript2.editor.doc.spi.JsDocumentationHolder;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;

/**
 *
 * @author Martin Fousek <marfous@netbeans.org>
 */
public class JsDocCompletionDocumentationTest extends JsDocumentationTestBase {

    private static final String BASE_PATH = "testfiles/jsdoc/completionDocumentation/";

    private JsDocumentationHolder documentationHolder;
    private JsParserResult parserResult;

    public JsDocCompletionDocumentationTest(String testName) {
        super(testName);
    }

    private void initializeDocumentationHolder(Source source) throws ParseException {
        ParserManager.parse(Collections.singleton(source), new UserTask() {
            public @Override void run(ResultIterator resultIterator) throws Exception {
                Parser.Result result = resultIterator.getParserResult();
                assertTrue(result instanceof JsParserResult);
                parserResult = (JsParserResult) result;
                documentationHolder = getDocumentationHolder(parserResult, new JsDocDocumentationProvider());
            }
        });
    }

    private void checkCompletionDocumentation(String relPath, String caretSeeker) throws Exception {
        Source testSource = getTestSource(getTestFile(relPath));
        final int caretOffset = getCaretOffset(testSource, caretSeeker);
        initializeDocumentationHolder(testSource);
        assertDescriptionMatches(relPath, documentationHolder.getDocumentation(getNodeForOffset(parserResult, caretOffset)).getContent(), true, "completionDoc.html");
    }

    public void testCompletionDocumentation01() throws Exception {
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "function Shape()^{");
    }

    public void testCompletionDocumentation02() throws Exception {
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "function addReference()^{");
    }

    public void testCompletionDocumentation03() throws Exception {
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "function Hexagon(sideLength) ^{");
    }

    public void testCompletionDocumentation04() throws Exception {
        // check throws
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "Shape.prototype.setColor = function(color)^{");
    }

    public void testCompletionDocumentation05() throws Exception {
        // check throws
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "Shape.prototype.clone3 = function()^{");
    }

    public void testCompletionDocumentation06() throws Exception {
        // check deprecation
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "Circle.P^I = 3.14;");
    }

    public void testCompletionDocumentation07() throws Exception {
        // check deprecation
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "function Add(One, Two)^{");
    }

    public void testCompletionDocumentation08() throws Exception {
        // check extends
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "function Circle(radius)^{");
    }

    public void testCompletionDocumentation09() throws Exception {
        // check see
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "Coordinate.prototype.x^ = 0;");
    }

    public void testCompletionDocumentation10() throws Exception {
        // check see
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "Circle.prototype.getRadius = function()^{");
    }

    public void testCompletionDocumentation11() throws Exception {
        // check since
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "function addReference()^{");
    }

    public void testCompletionDocumentation12() throws Exception {
        // check example
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "function Hexagon(sideLength) ^{");
    }

    public void testCompletionDocumentation13() throws Exception {
        // check example
        checkCompletionDocumentation(BASE_PATH + "completionDocumentation01.js", "Circle.createCircle = function(radius)^{");
    }

}
