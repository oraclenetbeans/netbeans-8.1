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
package org.netbeans.modules.javascript2.editor.parser;

import javax.swing.text.Document;
import org.netbeans.modules.javascript2.editor.JsTestBase;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.parser.SanitizingParser.Context;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;

/**
 *
 * @author Petr Hejl
 */
public class JsParserTest extends JsTestBase {

    public JsParserTest(String testName) {
        super(testName);
    }
    
    public void testSimpleCurly1() throws Exception {
        parse("var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "if (true) {\n"
            + "   var global3 = new org.foo.bar.Baz();\n"
            + "   gl\n"
            + "{\n"
            + "\n"
            + "DonaldDuck.Mickey.Baz.boo.boo = function(param) {\n"
            + "    return true;\n"
            + "}\n",
            "var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "if (true) {\n"
            + "   var global3 = new org.foo.bar.Baz();\n"
            + "   gl\n"
            + "{\n"
            + "\n"
            + "DonaldDuck.Mickey.Baz.boo.boo = function(param) {\n"
            + "    return true;\n"
            + "}\n}}",
            1,
            JsParser.Sanitize.MISSING_CURLY);
    }
    
    public void testSimpleCurly2() throws Exception {
        parse("var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "if (true) {\n"
            + "   var global3 = new org.foo.bar.Baz();\n"
            + "   gl\n"
            + "{\n"
            + "\n"
            + "DonaldDuck.Mickey.Baz.boo.boo = function(param) {\n"
            + "    return true;\n"
            + "}\n}}}",
            "var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "if (true) {\n"
            + "   var global3 = new org.foo.bar.Baz();\n"
            + "   gl\n"
            + "{\n"
            + "\n"
            + "DonaldDuck.Mickey.Baz.boo.boo = function(param) {\n"
            + "    return true;\n"
            + "}\n}} ",
            1,
            JsParser.Sanitize.MISSING_CURLY);
    }
    
    public void testSimpleSemicolon1() throws Exception {
        parse("\n"
            + "label:\n"
            + "\n",
            "\n"
            + "label:\n"
            + "\n;",
            1,
            JsParser.Sanitize.MISSING_SEMICOLON);
    }

    public void testSimpleCurrentError1() throws Exception {
        parse("var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "\n"
            + "a = 0x1G\n"
            + "\n"
            + "var global3 = 7\n",
            "var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "\n"
            + "a = 0x1 \n"
            + "\n"
            + "var global3 = 7\n",
            1,
            JsParser.Sanitize.SYNTAX_ERROR_CURRENT);
    }
    
    public void testSimplePreviousError1() throws Exception {
        parse("var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "if (true) {\n"
            + "   var global3 = new org.foo.bar.Baz();\n"
            + "   gl.\n"
            + "}\n"
            + "\n"
            + "DonaldDuck.Mickey.Baz.boo.boo = function(param) {\n"
            + "    return true;\n"
            + "}\n",
            "var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "if (true) {\n"
            + "   var global3 = new org.foo.bar.Baz();\n"
            + "   gl \n"
            + "}\n"
            + "\n"
            + "DonaldDuck.Mickey.Baz.boo.boo = function(param) {\n"
            + "    return true;\n"
            + "}\n",
            1,
            JsParser.Sanitize.SYNTAX_ERROR_PREVIOUS);
    }

    public void testSimplePreviousError2() throws Exception {
        parse("window.history.",
            "window.history ",
            1,
            JsParser.Sanitize.SYNTAX_ERROR_PREVIOUS);
    }

    public void testSimpleErrorDot1() throws Exception {
        parse("window.history.\n"
            + "function test(){"
            + "}",
            "window.history \n"
            + "function test(){"
            + "}",
            1,
            JsParser.Sanitize.ERROR_DOT);
    }

    public void testSimpleErrorLine1() throws Exception {
        parse("var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "if (true) {\n"
            + "   var global3 = new org.foo.bar.Baz();\n"
            + "   gl./d /\n"
            + "}\n"
            + "\n"
            + "DonaldDuck.Mickey.Baz.boo.boo = function(param) {\n"
            + "    return true;\n"
            + "}\n",
            "var global1 = new Foo.Bar();\n"
            + "var global2 = new Array();\n"
            + "if (true) {\n"
            + "   var global3 = new org.foo.bar.Baz();\n"
            + "          \n"
            + "}\n"
            + "\n"
            + "DonaldDuck.Mickey.Baz.boo.boo = function(param) {\n"
            + "    return true;\n"
            + "}\n",
            2,
            JsParser.Sanitize.ERROR_LINE);
    }
    
    public void testSimpleErrorLine2() throws Exception {
        parse("function A() {\n"
            + "}\n"
            + "A.prototype.say = function() {\n"
            + "    return \"ahoj\";\n"
            + "}\n"
            + "var a = new A();\n"
            + "function B() {\n"
            + "}\n" 
            + "B.prototype = new A();\n"
            + "var b = new B();\n"
            + "b.\n",
            "function A() {\n"
            + "}\n"
            + "A.prototype.say = function() {\n"
            + "    return \"ahoj\";\n"
            + "}\n"
            + "var a = new A();\n"
            + "function B() {\n"
            + "}\n" 
            + "B.prototype = new A();\n"
            + "var b = new B();\n"
            + "b \n",
            1,
            JsParser.Sanitize.SYNTAX_ERROR_PREVIOUS);
    }

    public void testSimpleParen1() throws Exception {
        parse("if (data != null) {\n"
            + "$.each(data, function(i,item) {\n"
            + "    text = \"test\";\n"
            + "    item = item.test\n"
            + "}\n"
            + "}",
            "if (data != null) {\n"
            + "$.each(data, function(i,item) {\n"
            + "    text = \"test\";\n"
            + "    item = item.test\n"
            + "})"
            + "}", 1, SanitizingParser.Sanitize.MISSING_PAREN);
    }

    public void testPreviousLines() throws Exception {
        parse("$('#selectorId').SomePlugin({ \n"
            + "    inline: true, \n"
            + "    calendars: 3,\n"
            + "    mode: 'range',\n"
            + "  _UNKNOWN_\n"
            + "    date: [c_from, c_to],\n"
            + "    current: new Date(c_to.getFullYear(), c_to.getMonth(), 1),\n"
            + "  _UNKNOWN_\n"
            + "    onChange: function(dates,el) {\n"
            + "        a\n"
            + "      }\n"
            + "});",
            "$('#selectorId').SomePlugin({ \n"
            + "    inline: true, \n"
            + "    calendars: 3,\n"
            + "    mode: 'range',\n"
            + "           \n"
            + "    date: [c_from, c_to],\n"
            + "    current: new Date(c_to.getFullYear(), c_to.getMonth(), 1),\n"
            + "           \n"
            + "    onChange: function(dates,el) {\n"
            + "         \n"
            + "      }\n"
            + "});",
            3,
            SanitizingParser.Sanitize.PREVIOUS_LINES);
    }

    public void testRegexp() throws Exception {
        parse("$?c.onreadystatechange=function(){/loaded|complete/.test(c.readyState)&&d()}:c.onload=c.onerror=d;\n",
                null, 0, null);
    }

    private void parse(String original, String expected, int errorCount,
            JsParser.Sanitize sanitization) throws Exception {

        JsParser parser = new JsParser();
        Document doc = getDocument(original);
        Snapshot snapshot = Source.create(doc).createSnapshot();
        Context context = new JsParser.Context("test.js", snapshot, -1);
        JsErrorManager manager = new JsErrorManager(snapshot, JsTokenId.javascriptLanguage());
        parser.parseContext(context, JsParser.Sanitize.NONE, manager);
        
        assertEquals(expected, context.getSanitizedSource());
        assertEquals(errorCount, manager.getErrors().size());
        assertEquals(sanitization, context.getSanitization());
    }
}
