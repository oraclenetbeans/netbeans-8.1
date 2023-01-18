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

package org.netbeans.modules.javascript2.editor;

import java.util.prefs.Preferences;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.modules.csl.api.Formatter;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.options.OptionsUtils;

/**
 * @todo Try typing in whole source files and other than tracking missing end and } closure
 *   statements the buffer should be identical - both in terms of quotes to the rhs not having
 *   accumulated as well as indentation being correct.
 * @todo
 *   // automatic reindentation of "end", "else" etc.
 *
 *
 *
 * @author Tor Norbye
 */
public class JsTypedTextInterceptorTest extends JsTestBase {

    public JsTypedTextInterceptorTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MimeLookup.getLookup(JsTokenId.JAVASCRIPT_MIME_TYPE).lookup(Preferences.class).clear();
    }


    @Override
    protected Formatter getFormatter(IndentPrefs preferences) {
        return null;
    }

    private void insertChar(String original, char insertText, String expected) throws Exception {
        insertChar(original, insertText, expected, null);
    }

    private void insertChar(String original, char insertText, String expected, String selection) throws Exception {
        insertChar(original, insertText, expected, selection, false);
    }

    public void testInsertX() throws Exception {
        insertChar("c^ass", 'l', "cl^ass");
    }

    public void testInsertX2() throws Exception {
        insertChar("clas^", 's', "class^");
    }

    public void testNoMatchInComments() throws Exception {
        insertChar("// Hello^", '\'', "// Hello'^");
        insertChar("// Hello^", '"', "// Hello\"^");
        insertChar("// Hello^", '[', "// Hello[^");
        insertChar("// Hello^", '(', "// Hello(^");
        insertChar("/* Hello^*/", '\'', "/* Hello'^*/");
        insertChar("/* Hello^*/", '"', "/* Hello\"^*/");
        insertChar("/* Hello^*/", '[', "/* Hello[^*/");
        insertChar("/* Hello^*/", '(', "/* Hello(^*/");
    }

    public void testNoMatchInStrings() throws Exception {
        insertChar("x = \"^\"", '\'', "x = \"'^\"");
        insertChar("x = \"^\"", '[', "x = \"[^\"");
        insertChar("x = \"^\"", '(', "x = \"(^\"");
        insertChar("x = \"^)\"", ')', "x = \")^)\"");
        insertChar("x = '^'", '"', "x = '\"^'");
    }

    public void testSingleQuotes1() throws Exception {
        insertChar("x = ^", '\'', "x = '^'");
    }

    public void testSingleQuotes2() throws Exception {
        insertChar("x = '^'", '\'', "x = ''^");
    }

    public void testSingleQuotes3() throws Exception {
        insertChar("x = '^'", 'a', "x = 'a^'");
    }

    public void testSingleQuotes4() throws Exception {
        insertChar("x = '\\^'", '\'', "x = '\\'^'");
    }

    public void testInsertBrokenQuote() throws Exception {
        insertChar("System.out.prinlnt(\"pavel^)", '"',
                "System.out.prinlnt(\"pavel\"^)");
    }

    public void testInsertBrokenQuote2() throws Exception {
        insertChar("System.out.prinlnt(\"pavel^\n", '"',
                "System.out.prinlnt(\"pavel\"^\n");
    }

    public void testInsertBrokenQuote3() throws Exception {
        insertChar("System.out.prinlnt(\"^\n", '"',
                "System.out.prinlnt(\"\"^\n");
    }

    public void testInsertBrokenQuote4() throws Exception {
        insertChar("System.out.prinlnt(\"pavel^", '"',
                "System.out.prinlnt(\"pavel\"^");
    }

    public void testDoubleQuotes1() throws Exception {
        insertChar("x = ^", '"', "x = \"^\"");
    }

    public void testDoubleQuotes2() throws Exception {
        insertChar("x = \"^\"", '"', "x = \"\"^");
    }

    public void testDoubleQuotes3() throws Exception {
        insertChar("x = \"^\"", 'a', "x = \"a^\"");
    }

    public void testDobuleQuotes4() throws Exception {
        insertChar("x = \"\\^\"", '"', "x = \"\\\"^\"");
    }

    public void testBrackets1() throws Exception {
        insertChar("x = ^", '[', "x = [^]");
    }

    public void testBrackets2() throws Exception {
        insertChar("x = [^]", ']', "x = []^");
    }

    public void testBrackets3() throws Exception {
        insertChar("x = [^]", 'a', "x = [a^]");
    }

    public void testBrackets4() throws Exception {
        insertChar("x = [^]", '[', "x = [[^]]");
    }

    public void testBrackets5() throws Exception {
        insertChar("x = [[^]]", ']', "x = [[]^]");
    }

    public void testBrackets6() throws Exception {
        insertChar("x = [[]^]", ']', "x = [[]]^");
    }

    public void testBrace1() throws Exception {
        insertChar("function test(){^}", '}', "function test(){}^");
    }

    public void testParens1() throws Exception {
        insertChar("x = ^", '(', "x = (^)");
    }

    public void testParens2() throws Exception {
        insertChar("x = (^)", ')', "x = ()^");
    }

    public void testParens3() throws Exception {
        insertChar("x = (^)", 'a', "x = (a^)");
    }

    public void testParens4() throws Exception {
        insertChar("x = (^)", '(', "x = ((^))");
    }

    public void testParens5() throws Exception {
        insertChar("x = ((^))", ')', "x = (()^)");
    }

    public void testParens6() throws Exception {
        insertChar("x = (()^)", ')', "x = (())^");
    }

    public void testParens7() throws Exception {
        insertChar("x = ((^)", ')', "x = (()^)");
    }

    public void testRegexp3() throws Exception {
        insertChar("x = /^/", 'a', "x = /a^/");
    }

    public void testRegexp4() throws Exception {
        insertChar("x = /\\^/", '/', "x = /\\/^/");
    }

    public void testRegexp5() throws Exception {
        insertChar("    regexp = /fofo^\n      // Subsequently, you can make calls to it by name with <tt>yield</tt> in", '/',
                "    regexp = /fofo/^\n      // Subsequently, you can make calls to it by name with <tt>yield</tt> in");
    }

    public void testRegexp6() throws Exception {
        insertChar("    regexp = /fofo^\n", '/',
                "    regexp = /fofo/^\n");
    }

    public void testRegexp9() throws Exception {
        insertChar("x = /^/\n", 'a', "x = /a^/\n");
    }

    public void testRegexp10() throws Exception {
        insertChar("x = /\\^/\n", '/', "x = /\\/^/\n");
    }

    public void testRegexp11() throws Exception {
        insertChar("/foo^", '/',
                "/foo/^");
    }
    public void testNotRegexp1() throws Exception {
        insertChar("x = 10 ^", '/', "x = 10 /^");
    }

    public void testNotRegexp2() throws Exception {
        insertChar("x = 3.14 ^", '/', "x = 3.14 /^");
    }

    public void testNotRegexp4() throws Exception {
        insertChar("x = y^", '/', "x = y/^");
    }

    public void testNotRegexp5() throws Exception {
        insertChar("/^", '/', "//^");
    }

    public void testNoInsertPercentElsewhere() throws Exception {
        insertChar("x = ^", '#', "x = #^");
    }

    public void testReplaceSelection1() throws Exception {
        insertChar("x = foo^", 'y', "x = y^", "foo");
    }

    public void testReplaceSelection2() throws Exception {
        insertChar("x = foo^", '"', "x = \"foo\"^", "foo");
    }

    public void testReplaceSelection4() throws Exception {
        insertChar("x = 'foo^bar'", '#', "x = '#^bar'", "foo");
    }

    public void testReplaceSelection5() throws Exception {
        insertChar("'(^position:absolute;'", '{', "'{^position:absolute;'", "(");
    }

    public void testReplaceSelection6() throws Exception {
        insertChar("'position^:absolute;'", '{', "'pos{^:absolute;'", "ition");
    }

    public void testReplaceSelectionChangeType1() throws Exception {
        insertChar("x = \"foo\"^", '\'', "x = 'foo'^", "\"foo\"");
    }

    public void testReplaceSelectionChangeType2() throws Exception {
        insertChar("x = \"foo\"^", '{', "x = {foo}^", "\"foo\"");
    }

    public void testReplaceSelectionNotInTemplateMode1() throws Exception {
        insertChar("x = foo^", '"', "x = \"^\"", "foo", true);
    }

    public void testEnabledSmartQuotes() throws Exception {
        insertChar("x = ^", '"', "x = \"^\"");
    }

    public void testDisabledSmartQuotes() throws Exception {
        MimeLookup.getLookup(JsTokenId.JAVASCRIPT_MIME_TYPE).lookup(Preferences.class)
                .putBoolean(OptionsUtils.AUTO_COMPLETION_SMART_QUOTES, false);
        insertChar("x = ^", '"', "x = \"^");
    }

    public void testDisabledBrackets1() throws Exception {
        MimeLookup.getLookup(JsTokenId.JAVASCRIPT_MIME_TYPE).lookup(Preferences.class)
                .putBoolean(SimpleValueNames.COMPLETION_PAIR_CHARACTERS, false);
        insertChar("x = ^", '(', "x = (^");
    }

    public void testDisabledBrackets2() throws Exception {
        MimeLookup.getLookup(JsTokenId.JAVASCRIPT_MIME_TYPE).lookup(Preferences.class)
                .putBoolean(SimpleValueNames.COMPLETION_PAIR_CHARACTERS, false);
        insertChar("x = ^", '[', "x = [^");
    }

    public void testDisabledBrackets3() throws Exception {
        MimeLookup.getLookup(JsTokenId.JAVASCRIPT_MIME_TYPE).lookup(Preferences.class)
                .putBoolean(SimpleValueNames.COMPLETION_PAIR_CHARACTERS, false);
        insertChar("x = ^", '{', "x = {^");
    }

    public void testIssue233067() throws Exception {
        insertChar("window.console.log(\"^\")", 'a', "window.console.log(\"a^\")", null, true);
    }

    public void testIssue233292_1() throws Exception {
        insertChar("var a = \"test\"^", ';', "var a = \"test\";^");
    }
    
    public void testIssue233292_2() throws Exception {
        insertChar("a.replace(\"aaa\"^)", ',', "a.replace(\"aaa\",^)");
    }
    
    public void testIssue233292_3() throws Exception {
        insertChar("test(\"asasa\", {pes:\"1\"^)", '}', "test(\"asasa\", {pes:\"1\"}^)");
    }

    public void testIssue233292_4() throws Exception {
        insertChar("var a = {\n    pes: \"1\"^\n}\n", ',', "var a = {\n    pes: \"1\",^\n}\n");
    }

    public void testIssue189443() throws Exception {
        insertChar("function x() {\n"
            + "for(var j in child)^\n"
            + "            alert(child.item(j));\n"
            + "            child.item(j).checked = true;\n"
            + "\n"
            +"}\n",
            '{',
            "function x() {\n"
            + "for(var j in child){^\n"
            + "            alert(child.item(j));\n"
            + "            child.item(j).checked = true;\n"
            + "\n"
            +"}\n");
    }
    
    public void testIssue195515() throws Exception {
        insertChar("function name() { {^}", '}', "function name() { {}^");
    }
}
