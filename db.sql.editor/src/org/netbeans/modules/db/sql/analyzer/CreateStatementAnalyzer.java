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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.db.sql.analyzer;

import java.util.Collections;
import java.util.List;
import org.netbeans.api.db.sql.support.SQLIdentifiers.Quoter;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.db.sql.analyzer.SQLStatement.Context;
import org.netbeans.modules.db.sql.lexer.SQLTokenId;

/**
 * Parse SQL Create procedure/function statement. Expected format is:
 *
 * DELIMITER \\
 * CREATE PROCEDURE p2()
 * BEGIN
 *   SELECT * FROM a;
 *   SELECT * FROM b;
 * END\\
 *
 * @author Jiri Skrivanek
 */
class CreateStatementAnalyzer extends SQLStatementAnalyzer {

    private int bodyStartOffset;
    private int bodyEndOffset;

    public static CreateStatement analyze(TokenSequence<SQLTokenId> seq, Quoter quoter) {
        seq.moveStart();
        if (!seq.moveNext()) {
            return null;
        }
        CreateStatementAnalyzer sa = new CreateStatementAnalyzer(seq, quoter);
        sa.parse();
        return new CreateStatement(sa.startOffset, seq.offset() + seq.token().length(), sa.offset2Context, sa.bodyStartOffset, sa.bodyEndOffset, null, Collections.unmodifiableList(sa.subqueries));
    }

    private CreateStatementAnalyzer(TokenSequence<SQLTokenId> seq, Quoter quoter) {
        super(seq, quoter);
    }

    private void parse() {
        startOffset = seq.offset();
        do {
            switch (context) {
                case START:
                    if (SQLStatementAnalyzer.isKeyword("CREATE", seq)) { // NOI18N
                        moveToContext(Context.CREATE);
                    }
                    break;
                case CREATE:
                    if (SQLStatementAnalyzer.isKeyword("PROCEDURE", seq)) { // NOI18N
                        moveToContext(Context.CREATE_PROCEDURE);
                    } else if (SQLStatementAnalyzer.isKeyword("FUNCTION", seq)) { // NOI18N
                        moveToContext(Context.CREATE_FUNCTION);
                    } else if (SQLStatementAnalyzer.isKeyword("TABLE", seq)) {
                        moveToContext(Context.CREATE_TABLE);
                    } else if (SQLStatementAnalyzer.isKeyword("TEMPORARY", seq)) {
                        moveToContext(Context.CREATE_TEMPORARY_TABLE);
                    } else if (SQLStatementAnalyzer.isKeyword("DATABASE", seq)) {
                        moveToContext(Context.CREATE_DATABASE);
                    } else if (SQLStatementAnalyzer.isKeyword("SCHEMA", seq)) {
                        moveToContext(Context.CREATE_SCHEMA);
                    } else if (SQLStatementAnalyzer.isKeyword("VIEW", seq)) {
                        moveToContext(Context.CREATE_VIEW);
                    }
                    break;
                case CREATE_PROCEDURE:
                case CREATE_FUNCTION:
                    if (SQLStatementAnalyzer.isKeyword("BEGIN", seq)) { // NOI18N
                        moveToContext(Context.BEGIN);
                        bodyStartOffset = seq.offset() + seq.token().length();
                    }
                    break;
                case BEGIN:
                    if (SQLStatementAnalyzer.isKeyword("END", seq)) { // NOI18N
                        moveToContext(Context.END);
                        bodyEndOffset = seq.offset();
                    }
                    break;
                case CREATE_TEMPORARY_TABLE:
                    if (SQLStatementAnalyzer.isKeyword("TABLE", seq)) {
                        moveToContext(Context.CREATE_TABLE);
                    }
                    break;
                case CREATE_VIEW:
                    if(SQLStatementAnalyzer.isKeyword("AS", seq)) {
                        moveToContext(Context.CREATE_VIEW_AS);
                    }
                    break;
                case CREATE_VIEW_AS:
                    if(SQLStatementAnalyzer.isKeyword("SELECT", seq)) {
                        moveToContext(Context.SELECT);
                    }
                    break;
                default:
                // skip anything else
            }
        } while (nextToken());
        // unfinished body
        if (context == Context.BEGIN) {
            bodyEndOffset = seq.offset() + seq.token().length();
        }
    }
}
