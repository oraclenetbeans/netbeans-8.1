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
package org.netbeans.modules.html.editor.embedding;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.html.lexer.HtmlLexerPlugin;
import org.netbeans.modules.csl.api.test.CslTestBase;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.web.common.api.WebUtils;

/**
 *
 * @author marekfukala
 */
public class JsEmbeddingProviderTest extends CslTestBase {

    public JsEmbeddingProviderTest(String name) {
        super(name);
    }

    public void testScriptTag() throws ParseException {
        assertEmbedding("<script>alert();</script>", "alert();\n");

        assertEmbedding("<script>\n"
                + "function hello() {\n"
                + "    alert('hello!');\n"
                + "}\n"
                + "</script>",
                "\n"
                + "function hello() {\n"
                + "    alert('hello!');\n"
                + "}\n"
                + "\n"
                + "");
    }

    public void testOnClick() throws ParseException {
        assertEmbedding("<div onclick='alert()'/>",
                "(function(){\n"
                + "alert();\n"
                + "});\n"
                + "");
    }

    public void testCustomJSEmbeddingOnAttributeValue() {
        assertEmbedding("<div controller='MyController'/>",
                "(function(){\n"
                + "MyController;\n"
                + "});\n"
                + "");
    }

    public void testCustomEL() {
        //the default JsEmbeddingProvider creates no virtual js embedding
        //at the place of the expression language. The js source is provided
        //by the frameworks plugins (KO, Angular).
        assertEmbedding("<div>{{hello}}</div>",
                null);
    }

    public void testIssue231633() {
        assertEmbedding(
                "<script type=\"text/javascript\">\n"
                + "   <!--   \n"
                + "   window.alert(\"Hello World!\");\n"
                + "   -->\n"
                + " </script>",
                "\n"
                + "      window.alert(\"Hello World!\");\n"
                + "   \n");

        assertEmbedding(
                "<script type=\"text/javascript\">\n"
                + "   <!--//-->   \n"
                + "   window.alert(\"Hello World!\");\n"
                + "   <!--//-->\n"
                + " </script>",
                "\n"
                + "      window.alert(\"Hello World!\");\n"
                + "   \n"
                + " \n");
    }

    /*
     * Tests conversion of the generic templating mark "@@@"
     * to its JS counterpart.
     */
    public void testConvertGenericMarksToJSMark() {
        assertEmbedding("<script>hello @@@ word</script>",
                "hello __UNKNOWN__ word\n");

        //this needs some more "fine tuning" :-)
        //so far works only if the pattern is surrounded by sg.

//        assertEmbedding("<script>x@@@</script>",
//                "x__UNKNOWN__\n");
//        
//        assertEmbedding("<script>@@@x</script>",
//                "__UNKNOWN__x\n");
//        
//        assertEmbedding("<script>@@@</script>",
//                "__UNKNOWN__\n");

//        assertEmbedding("<div onclick=\"@@@\">",
//                "");

        assertEmbedding("<div onclick=\"a@@@b\">",
                "(function(){\n"
                + "a__UNKNOWN__b;\n"
                + "});\n"
                + "");

    }

    @MimeRegistration(mimeType = "text/html", service = HtmlLexerPlugin.class)
    public static class TestHtmlLexerPlugin extends HtmlLexerPlugin {

        @Override
        public String getOpenDelimiter() {
            return "{{";
        }

        @Override
        public String getCloseDelimiter() {
            return "}}";
        }

        @Override
        public String getContentMimeType() {
            return "text/javascript";
        }

        @Override
        public String createAttributeEmbedding(String elementName, String attributeName) {
            if ("controller".equals(attributeName)) {
                return "text/javascript";
            }
            return null;
        }
    }

    private void assertEmbedding(String code, String expectedJsVirtualSource) {
        assertEmbedding(getDocument(code, "text/html"), expectedJsVirtualSource);
    }

    public static void assertEmbedding(Document doc, String expectedJsVirtualSource) {
        try {
            Source source = Source.create(doc);
            final AtomicReference<String> jsCodeRef = new AtomicReference<>();
            ParserManager.parse(Collections.singleton(source), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    ResultIterator jsRi = WebUtils.getResultIterator(resultIterator, "text/javascript");
                    if (jsRi != null) {
                        jsCodeRef.set(jsRi.getSnapshot().getText().toString());
                    } else {
                        //no js embedded code
                    }
                }
            });
            String jsCode = jsCodeRef.get();
            if (expectedJsVirtualSource != null) {
                assertNotNull(jsCode);
                assertEquals(expectedJsVirtualSource, jsCode);
            } else {
                //expected no embedded js code
                assertNull(jsCode);
            }
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
}