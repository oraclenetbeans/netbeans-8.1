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
package org.netbeans.modules.db.sql.lexer;

import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.PartType;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.junit.NbTestCase;
import org.netbeans.lib.lexer.test.ModificationTextDocument;

/**
 *
 * @author Andrei Badea, Jiri Skrivanek
 */
public class SQLLexerTest extends NbTestCase {

    public SQLLexerTest(String testName) {
        super(testName);
    }

    public void testSimple() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("select -/ from 'a' + 1, dto");
        assertTokens(seq, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR,
                SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE, SQLTokenId.KEYWORD,
                SQLTokenId.WHITESPACE, SQLTokenId.STRING, SQLTokenId.WHITESPACE,
                SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE, SQLTokenId.INT_LITERAL,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.WHITESPACE);
    }

    public void testQuotedIdentifiers() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("select \"derby\", `mysql`, [mssql], `quo + ted`");
        assertTokens(seq, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.WHITESPACE);
    }

    /**
     * Test escape in quoted identifiers following SQL99 methology
     */
    public void testQuotedIdentifiers2() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("\"derby\"\"\",`mysql```,[mssql]]]");
        assertTokens(seq, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.IDENTIFIER,
                SQLTokenId.WHITESPACE);
    }

    public void testSimpleSQL99Quoting() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("select -/ from 'a''' + 1, dto");
        assertTokens(seq, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR,
                SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE, SQLTokenId.KEYWORD,
                SQLTokenId.WHITESPACE, SQLTokenId.STRING, SQLTokenId.WHITESPACE,
                SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE, SQLTokenId.INT_LITERAL,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.WHITESPACE);
    }

    public void testQuotedIdentifiersSQL99Quote() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("select \"\"\"derby\", `mysql`, [mssql], `quo + ted`");
        assertTokens(seq, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.WHITESPACE);
    }

    public void testIncompleteIdentifier() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("select \"\"\"derby\", `mysql`, [mssql], `quo + ted");
        assertTokens(seq, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.INCOMPLETE_IDENTIFIER);
        seq = getTokenSequence("select \"\"\"derby\", `mysql`, [mssql");
        assertTokens(seq, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.INCOMPLETE_IDENTIFIER);
        seq = getTokenSequence("select \"\"\"derby\", `mysql`, [mssql]");
        assertTokens(seq, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.COMMA, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER,
                SQLTokenId.WHITESPACE);
        seq = getTokenSequence("select \"\"\"derby");
        assertTokens(seq, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.INCOMPLETE_IDENTIFIER);
    }

    public void testComments() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("-- line comment\n# mysql comment\n/* block \ncomment*/\n#notComment");
        assertTokens(seq, SQLTokenId.LINE_COMMENT, SQLTokenId.LINE_COMMENT,
                SQLTokenId.BLOCK_COMMENT, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
    }

    public void testNewLineInString() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("'new\nline'");
        assertTokens(seq, SQLTokenId.STRING, SQLTokenId.WHITESPACE);
    }

    public void testIncompleteString() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("'incomplete");
        assertTokens(seq, SQLTokenId.INCOMPLETE_STRING);
    }
    
    public void testSingleQuote() throws Exception {
        // See bug #200479 - the fix introduced with this text reverts a "fix"
        // for bug #152325 - the latter bug can't be fixed, as it is clear violation
        // to SQL Standard - as this is a SQL lexer, not a MySQL lexer, this should
        // should follow the standard
        TokenSequence<SQLTokenId> seq = getTokenSequence("'\\'");
        assertTokens(seq, SQLTokenId.STRING, SQLTokenId.WHITESPACE);
    }
    
    public void testPlaceHolder() throws Exception {
        assertTokens(getTokenSequence("SELECT a FROM b WHERE c = :var"),
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE,
                SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("SELECT a FROM b WHERE c = :var"),
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE,
                SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
    }
    
    public void testVariableDeclaration() throws Exception {
        assertTokens(getTokenSequence("@a:='test'"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.STRING, SQLTokenId.WHITESPACE);
    }
    
    public void testOperators() throws Exception {
        // Basic Arithmetic
        assertTokens(getTokenSequence("a+b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER,  SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a-b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a* b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a /b"), SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a % b"), SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        // Bitwise operators (MSSQL)
        assertTokens(getTokenSequence("a&b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a|b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a^b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        // Un????res bitwise not (ones complement)
        assertTokens(getTokenSequence("~b"), SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        // Comparison operators
        assertTokens(getTokenSequence("a=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a>b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a<b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a<>b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a>=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a<=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a !=b"), SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a!< b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a !> b"), SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        // Concatenation (Informix)
        assertTokens(getTokenSequence("a||b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        // Assignement Operator (SPL)
        assertTokens(getTokenSequence("a:=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        // Compound Operators (Transact-SQL)
        assertTokens(getTokenSequence("a+=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a-=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a*=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a/=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a%=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a&=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a^=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
        assertTokens(getTokenSequence("a|=b"), SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.IDENTIFIER, SQLTokenId.WHITESPACE);
    }
    
    /**
     * Check correct handling of multiline comments (bug #)
     *
     * @throws Exception
     */
    public void testMultiLineComment() throws Exception {
        TokenSequence<SQLTokenId> seq = getTokenSequence("/**/\n"
                + "select * from test;");
        assertTokens(seq, SQLTokenId.BLOCK_COMMENT, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR,
                SQLTokenId.WHITESPACE, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE);
        seq = getTokenSequence("/****/\n"
                + "select * from test;");
        assertTokens(seq, SQLTokenId.BLOCK_COMMENT, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR,
                SQLTokenId.WHITESPACE, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE);
        // Bug #: The following sequences led to only one token
        seq = getTokenSequence("/***/\n"
                + "select * from test;");
        assertTokens(seq, SQLTokenId.BLOCK_COMMENT, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR,
                SQLTokenId.WHITESPACE, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE);
        seq = getTokenSequence("/*****/\n"
                + "select * from test;");
        assertTokens(seq, SQLTokenId.BLOCK_COMMENT, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR,
                SQLTokenId.WHITESPACE, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE);
        seq = getTokenSequence("/*** Test **/\n"
                + "select * from test;");
        assertTokens(seq, SQLTokenId.BLOCK_COMMENT, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR,
                SQLTokenId.WHITESPACE, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE);
        seq = getTokenSequence("/*** \n*  Test\n **/\n"
                + "select * from test;");
        assertTokens(seq, SQLTokenId.BLOCK_COMMENT, SQLTokenId.WHITESPACE,
                SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE, SQLTokenId.OPERATOR,
                SQLTokenId.WHITESPACE, SQLTokenId.KEYWORD, SQLTokenId.WHITESPACE,
                SQLTokenId.IDENTIFIER, SQLTokenId.OPERATOR, SQLTokenId.WHITESPACE);
    }

    private static TokenSequence<SQLTokenId> getTokenSequence(String sql) throws BadLocationException {
        ModificationTextDocument doc = new ModificationTextDocument();
        doc.insertString(0, sql, null);
        doc.putProperty(Language.class, SQLTokenId.language());
        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
        doc.readLock();
        TokenSequence<SQLTokenId> seq = hi.tokenSequence(SQLTokenId.language());
        doc.readUnlock();
        seq.moveStart();
        return seq;
    }

    private static CharSequence dumpTokens(TokenSequence<?> seq) {
        seq.moveStart();
        StringBuilder builder = new StringBuilder();
        Token<?> token = null;
        while (seq.moveNext()) {
            if (token != null) {
                builder.append('\n');
            }
            token = seq.token();
            builder.append(token.id());
            PartType part = token.partType();
            if (part != PartType.COMPLETE) {
                builder.append(' ');
                builder.append(token.partType());
            }
            builder.append(' ');
            builder.append('\'');
            builder.append(token.text());
            builder.append('\'');
        }
        return builder;
    }

    private static void assertTokens(TokenSequence<SQLTokenId> seq, SQLTokenId... ids) {
        if (ids == null) {
            ids = new SQLTokenId[0];
        }
        assertEquals("Wrong token count.", ids.length, seq.tokenCount());
        seq.moveNext();
        for (SQLTokenId id : ids) {
            assertEquals("Wrong token ID at index " + seq.index(), id, seq.token().id());
            seq.moveNext();
        }
    }
}
