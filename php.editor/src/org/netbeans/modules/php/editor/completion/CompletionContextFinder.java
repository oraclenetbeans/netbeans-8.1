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
package org.netbeans.modules.php.editor.completion;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.lexer.TokenUtilities;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.php.editor.lexer.LexUtilities;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.api.Utils;
import org.netbeans.modules.php.editor.parser.astnodes.ASTError;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.Block;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.InterfaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;

/**
 *
 * @author Tomasz.Slota@Sun.COM
 */
final class CompletionContextFinder {

    private static final String NAMESPACE_FALSE_TOKEN = "NAMESPACE_FALSE_TOKEN"; //NOI18N
    private static final String COMBINED_USE_STATEMENT_TOKENS = "COMBINED_USE_STATEMENT_TOKENS"; //NOI18N
    private static final String TYPE_KEYWORD = "TYPE_KEYWORD"; //NOI18N
    private static final PHPTokenId[] COMMENT_TOKENS = new PHPTokenId[]{
            PHPTokenId.PHP_COMMENT_START, PHPTokenId.PHP_COMMENT, PHPTokenId.PHP_LINE_COMMENT, PHPTokenId.PHP_COMMENT_END};
    private static final PHPTokenId[] PHPDOC_TOKENS = new PHPTokenId[]{
            PHPTokenId.PHPDOC_COMMENT_START, PHPTokenId.PHPDOC_COMMENT, PHPTokenId.PHPDOC_COMMENT_END};
    private static final List<Object[]> CLASS_NAME_TOKENCHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_NEW},
            new Object[]{PHPTokenId.PHP_NEW, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_NEW, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_NEW, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING});
    private static final List<Object[]> THROW_NEW_TOKEN_CHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_THROW},
            new Object[]{PHPTokenId.PHP_THROW, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_THROW, PHPTokenId.WHITESPACE, PHPTokenId.PHP_NEW},
            new Object[]{PHPTokenId.PHP_THROW, PHPTokenId.WHITESPACE, PHPTokenId.PHP_NEW, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_THROW, PHPTokenId.WHITESPACE, PHPTokenId.PHP_NEW, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_THROW, PHPTokenId.WHITESPACE, PHPTokenId.PHP_NEW, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING});
    private static final List<Object[]> FUNCTION_NAME_TOKENCHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_FUNCTION},
            new Object[]{PHPTokenId.PHP_FUNCTION, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_FUNCTION, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_FUNCTION, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING});
    private static final List<Object[]> USE_KEYWORD_TOKENS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_USE},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_USE, COMBINED_USE_STATEMENT_TOKENS});
    private static final List<Object[]> USE_CONST_KEYWORD_TOKENS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CONST},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CONST, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CONST, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CONST, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CONST, COMBINED_USE_STATEMENT_TOKENS});
    private static final List<Object[]> USE_FUNCTION_KEYWORD_TOKENS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_FUNCTION},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_FUNCTION, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_FUNCTION, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_FUNCTION, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_FUNCTION, COMBINED_USE_STATEMENT_TOKENS});
    private static final List<Object[]> USE_TRAIT_KEYWORD_TOKENS = Arrays.asList(
            new Object[]{TYPE_KEYWORD, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE},
            new Object[]{TYPE_KEYWORD, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE},
            new Object[]{TYPE_KEYWORD, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{TYPE_KEYWORD, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{TYPE_KEYWORD, PHPTokenId.WHITESPACE, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE, COMBINED_USE_STATEMENT_TOKENS},
            new Object[]{TYPE_KEYWORD, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE},
            new Object[]{TYPE_KEYWORD, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE},
            new Object[]{TYPE_KEYWORD, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{TYPE_KEYWORD, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{TYPE_KEYWORD, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_USE, COMBINED_USE_STATEMENT_TOKENS});
    private static final List<Object[]> NAMESPACE_KEYWORD_TOKENS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_NAMESPACE},
            new Object[]{PHPTokenId.PHP_NAMESPACE, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_NAMESPACE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_NAMESPACE, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN});
    private static final List<Object[]> INSTANCEOF_TOKENCHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_INSTANCEOF},
            new Object[]{PHPTokenId.PHP_INSTANCEOF, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_INSTANCEOF, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_INSTANCEOF, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING});
    private static final List<Object[]> CATCH_TOKENCHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_CATCH, PHPTokenId.PHP_TOKEN},
            new Object[]{PHPTokenId.PHP_CATCH, PHPTokenId.PHP_TOKEN, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_CATCH, PHPTokenId.WHITESPACE, PHPTokenId.PHP_TOKEN},
            new Object[]{PHPTokenId.PHP_CATCH, PHPTokenId.WHITESPACE, PHPTokenId.PHP_TOKEN, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_CATCH, PHPTokenId.WHITESPACE, PHPTokenId.PHP_TOKEN, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_CATCH, PHPTokenId.WHITESPACE, PHPTokenId.PHP_TOKEN, PHPTokenId.WHITESPACE, NAMESPACE_FALSE_TOKEN},
            new Object[]{PHPTokenId.PHP_CATCH, PHPTokenId.WHITESPACE, PHPTokenId.PHP_TOKEN, PHPTokenId.PHP_STRING});
    private static final List<Object[]> CLASS_MEMBER_TOKENCHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_OBJECT_OPERATOR},
            new Object[]{PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.PHP_VARIABLE},
            new Object[]{PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.PHP_TOKEN},
            new Object[]{PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.WHITESPACE, PHPTokenId.PHP_VARIABLE},
            new Object[]{PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.WHITESPACE, PHPTokenId.PHP_TOKEN});
    private static final List<Object[]> STATIC_CLASS_MEMBER_TOKENCHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM},
            new Object[]{PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM, PHPTokenId.PHP_VARIABLE},
            new Object[]{PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM, PHPTokenId.PHP_TOKEN},
            new Object[]{PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM, PHPTokenId.WHITESPACE, PHPTokenId.PHP_VARIABLE},
            new Object[]{PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM, PHPTokenId.WHITESPACE, PHPTokenId.PHP_TOKEN});
    private static final List<Object[]> CLASS_MEMBER_IN_STRING_TOKENCHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_ENCAPSED_AND_WHITESPACE, PHPTokenId.PHP_VARIABLE, PHPTokenId.PHP_OBJECT_OPERATOR},
            new Object[]{PHPTokenId.PHP_ENCAPSED_AND_WHITESPACE, PHPTokenId.PHP_VARIABLE, PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_ENCAPSED_AND_WHITESPACE, PHPTokenId.PHP_VARIABLE, PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.PHP_TOKEN},
            new Object[]{PHPTokenId.PHP_ENCAPSED_AND_WHITESPACE, PHPTokenId.PHP_VARIABLE, PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_ENCAPSED_AND_WHITESPACE, PHPTokenId.PHP_VARIABLE, PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_ENCAPSED_AND_WHITESPACE, PHPTokenId.PHP_VARIABLE, PHPTokenId.PHP_OBJECT_OPERATOR, PHPTokenId.WHITESPACE, PHPTokenId.PHP_TOKEN});
    private static final List<Object[]> METHOD_NAME_TOKENCHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_FUNCTION},
            new Object[]{PHPTokenId.PHP_FUNCTION, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_FUNCTION, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING});
    private static final List<Object[]> CLASS_CONTEXT_KEYWORDS_TOKENCHAINS = Arrays.asList(
            new Object[]{PHPTokenId.PHP_PRIVATE},
            new Object[]{PHPTokenId.PHP_PRIVATE, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_PRIVATE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_PROTECTED},
            new Object[]{PHPTokenId.PHP_PROTECTED, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_PROTECTED, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_PUBLIC},
            new Object[]{PHPTokenId.PHP_PUBLIC, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_PUBLIC, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_STATIC},
            new Object[]{PHPTokenId.PHP_STATIC, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_STATIC, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_ABSTRACT},
            new Object[]{PHPTokenId.PHP_ABSTRACT, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_ABSTRACT, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_FINAL},
            new Object[]{PHPTokenId.PHP_FINAL, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_FINAL, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_CURLY_OPEN},
            new Object[]{PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_CURLY_CLOSE},
            new Object[]{PHPTokenId.PHP_CURLY_CLOSE, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_CURLY_CLOSE, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_CURLY_OPEN},
            new Object[]{PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING},
            new Object[]{PHPTokenId.PHP_SEMICOLON},
            new Object[]{PHPTokenId.PHP_SEMICOLON, PHPTokenId.WHITESPACE},
            new Object[]{PHPTokenId.PHP_SEMICOLON, PHPTokenId.WHITESPACE, PHPTokenId.PHP_STRING});
    private static final List<Object[]> SERVER_ARRAY_TOKENCHAINS = Collections.singletonList(
            new Object[]{PHPTokenId.PHP_VARIABLE, PHPTokenId.PHP_TOKEN});
    private static final List<String> SERVER_ARRAY_TOKENTEXTS =
            Arrays.asList(new String[]{"$_SERVER", "["}); //NOI18N

    public static enum CompletionContext {

        EXPRESSION, HTML, CLASS_NAME, INTERFACE_NAME, TYPE_NAME, STRING,
        CLASS_MEMBER, STATIC_CLASS_MEMBER, PHPDOC, INHERITANCE, EXTENDS, IMPLEMENTS, METHOD_NAME,
        CLASS_CONTEXT_KEYWORDS, SERVER_ENTRY_CONSTANTS, NONE, NEW_CLASS, GLOBAL, NAMESPACE_KEYWORD,
        USE_KEYWORD, USE_CONST_KEYWORD, USE_FUNCTION_KEYWORD, DEFAULT_PARAMETER_VALUE, OPEN_TAG, THROW, CATCH, CLASS_MEMBER_IN_STRING,
        INTERFACE_CONTEXT_KEYWORDS, USE_TRAITS
    };

    static enum KeywordCompletionType {

        SIMPLE, CURSOR_INSIDE_BRACKETS, ENDS_WITH_CURLY_BRACKETS,
        ENDS_WITH_SPACE, ENDS_WITH_SEMICOLON, ENDS_WITH_COLON, ENDS_WITH_BRACKETS_AND_CURLY_BRACKETS,
        CURSOR_BEFORE_ENDING_SEMICOLON
    };
    static final Collection<PHPTokenId> CTX_DELIMITERS = Arrays.asList(
            PHPTokenId.PHP_SEMICOLON, PHPTokenId.PHP_CURLY_OPEN, PHPTokenId.PHP_CURLY_CLOSE,
            PHPTokenId.PHP_RETURN, PHPTokenId.PHP_OPERATOR, PHPTokenId.PHP_ECHO,
            PHPTokenId.PHP_EVAL, PHPTokenId.PHP_NEW, PHPTokenId.PHP_NOT, PHPTokenId.PHP_CASE,
            PHPTokenId.PHP_IF, PHPTokenId.PHP_ELSE, PHPTokenId.PHP_ELSEIF, PHPTokenId.PHP_PRINT,
            PHPTokenId.PHP_FOR, PHPTokenId.PHP_FOREACH, PHPTokenId.PHP_WHILE,
            PHPTokenId.PHP_CONSTANT_ENCAPSED_STRING, PHPTokenId.PHP_ENCAPSED_AND_WHITESPACE,
            PHPTokenId.T_OPEN_TAG_WITH_ECHO, PHPTokenId.PHP_OPENTAG, PHPTokenId.PHP_CASTING);

    private CompletionContextFinder() {
    }

    @NonNull
    static CompletionContext findCompletionContext(ParserResult info, int caretOffset) {
        TokenHierarchy<?> th = info.getSnapshot().getTokenHierarchy();
        if (th == null) {
            return CompletionContext.NONE;
        }
        TokenSequence<PHPTokenId> tokenSequence = LexUtilities.getPHPTokenSequence(th, caretOffset);
        if (tokenSequence == null) {
            return CompletionContext.NONE;
        }
        tokenSequence.move(caretOffset);
        final boolean moveNextSucces = tokenSequence.moveNext();
        if (!moveNextSucces && !tokenSequence.movePrevious()) {
            return CompletionContext.NONE;
        }
        Token<PHPTokenId> token = tokenSequence.token();
        PHPTokenId tokenId = token.id();
        if (tokenId.equals(PHPTokenId.PHP_CLOSETAG) && (tokenSequence.offset() < caretOffset)) {
            return CompletionContext.NONE;
        }
        int tokenIdOffset = tokenSequence.token().offset(th);

        CompletionContext clsIfaceDeclContext = getClsIfaceDeclContext(token, (caretOffset - tokenIdOffset), tokenSequence);
        if (clsIfaceDeclContext != null) {
            return clsIfaceDeclContext;
        }
        if (acceptTokenChains(tokenSequence, THROW_NEW_TOKEN_CHAINS, moveNextSucces)) {
            return CompletionContext.THROW;
        } else if (acceptTokenChains(tokenSequence, CLASS_NAME_TOKENCHAINS, moveNextSucces)) {
            // has to be checked AFTER: THROW_NEW_TOKEN_CHAINS
            return CompletionContext.NEW_CLASS;
        } else if (acceptTokenChains(tokenSequence, CLASS_MEMBER_IN_STRING_TOKENCHAINS, moveNextSucces)) {
            return CompletionContext.CLASS_MEMBER_IN_STRING;
        } else if (acceptTokenChains(tokenSequence, CLASS_MEMBER_TOKENCHAINS, moveNextSucces)) {
            // has to be checked AFTER: CLASS_MEMBER_IN_STRING_TOKENCHAINS
            return CompletionContext.CLASS_MEMBER;
        } else if (acceptTokenChains(tokenSequence, STATIC_CLASS_MEMBER_TOKENCHAINS, moveNextSucces)) {
            return CompletionContext.STATIC_CLASS_MEMBER;
        } else if (tokenId == PHPTokenId.PHP_COMMENT) {
            return getCompletionContextInComment(tokenSequence, caretOffset, info);
        } else if (isPhpDocToken(tokenSequence)) {
            return CompletionContext.PHPDOC;
        } else if (acceptTokenChains(tokenSequence, CATCH_TOKENCHAINS, moveNextSucces)) {
            return CompletionContext.CATCH;
        } else if (acceptTokenChains(tokenSequence, USE_TRAIT_KEYWORD_TOKENS, moveNextSucces)) {
            return CompletionContext.USE_TRAITS;
        } else if (acceptTokenChains(tokenSequence, USE_KEYWORD_TOKENS, moveNextSucces)) {
            return CompletionContext.USE_KEYWORD;
        } else if (acceptTokenChains(tokenSequence, USE_CONST_KEYWORD_TOKENS, moveNextSucces)) {
            return CompletionContext.USE_CONST_KEYWORD;
        } else if (acceptTokenChains(tokenSequence, USE_FUNCTION_KEYWORD_TOKENS, moveNextSucces)) {
            return CompletionContext.USE_FUNCTION_KEYWORD;
        } else if (acceptTokenChains(tokenSequence, NAMESPACE_KEYWORD_TOKENS, moveNextSucces)) {
            return CompletionContext.NAMESPACE_KEYWORD;
        } else if (acceptTokenChains(tokenSequence, INSTANCEOF_TOKENCHAINS, moveNextSucces)) {
            return CompletionContext.TYPE_NAME;
        } else if (isInsideInterfaceDeclarationBlock(info, caretOffset, tokenSequence)) {
            CompletionContext paramContext = getParamaterContext(token, caretOffset, tokenSequence);
            if (paramContext != null) {
                return paramContext;
            }
            return CompletionContext.INTERFACE_CONTEXT_KEYWORDS;
        } else if (isInsideClassDeclarationBlock(info, caretOffset, tokenSequence)) {
            if (acceptTokenChains(tokenSequence, METHOD_NAME_TOKENCHAINS, moveNextSucces)) {
                return CompletionContext.METHOD_NAME;
            } else {
                CompletionContext paramContext = getParamaterContext(token, caretOffset, tokenSequence);
                if (paramContext != null) {
                    return paramContext;
                } else if (acceptTokenChains(tokenSequence, CLASS_CONTEXT_KEYWORDS_TOKENCHAINS, moveNextSucces)) {
                    return CompletionContext.CLASS_CONTEXT_KEYWORDS;
                }
                return CompletionContext.NONE;
            }
        } else if (acceptTokenChains(tokenSequence, FUNCTION_NAME_TOKENCHAINS, moveNextSucces)) {
            return CompletionContext.NONE;
        } else if (isCommonCommentToken(tokenSequence)) {
            return CompletionContext.NONE;
        }

        switch (tokenId) {
            case T_INLINE_HTML:
                return CompletionContext.HTML;
            case PHP_CONSTANT_ENCAPSED_STRING:
                char encChar = tokenSequence.token().text().charAt(0);
                if (encChar == '"') { //NOI18N
                    if (acceptTokenChains(tokenSequence, SERVER_ARRAY_TOKENCHAINS, moveNextSucces)
                            && acceptTokenChainTexts(tokenSequence, SERVER_ARRAY_TOKENTEXTS)) {
                        return CompletionContext.SERVER_ENTRY_CONSTANTS;
                    }
                    return CompletionContext.STRING;
                } else if (encChar == '\'') { //NOI18N
                    if (acceptTokenChains(tokenSequence, SERVER_ARRAY_TOKENCHAINS, moveNextSucces)
                            && acceptTokenChainTexts(tokenSequence, SERVER_ARRAY_TOKENTEXTS)) {
                        return CompletionContext.SERVER_ENTRY_CONSTANTS;
                    }
                }
                return CompletionContext.NONE;
            default:
        }
        if (isEachOfTokens(getLeftPreceedingTokens(tokenSequence), new PHPTokenId[]{PHPTokenId.PHP_GLOBAL, PHPTokenId.WHITESPACE})
                || (isWhiteSpace(token) && isEachOfTokens(getLeftPreceedingTokens(tokenSequence), new PHPTokenId[]{PHPTokenId.PHP_GLOBAL}))) {
            return CompletionContext.GLOBAL;
        }

        CompletionContext paramContext = getParamaterContext(token, caretOffset, tokenSequence);
        if (paramContext != null) {
            return paramContext;
        }

        if (tokenSequence.movePrevious() && tokenSequence.token().id() == PHPTokenId.PHP_OPENTAG
                && "<?".equals(tokenSequence.token().text().toString()) && (tokenSequence.offset() + 2) == caretOffset) {
            return CompletionContext.OPEN_TAG;
        }
        return CompletionContext.EXPRESSION;
    }

    private static boolean isPhpDocToken(TokenSequence tokenSequence) {
        return isOneOfTokens(tokenSequence, PHPDOC_TOKENS);
    }

    private static boolean isCommonCommentToken(TokenSequence tokenSequence) {
        return isOneOfTokens(tokenSequence, COMMENT_TOKENS);
    }

    private static boolean isCommentToken(TokenSequence tokenSequence) {
        return isCommonCommentToken(tokenSequence) || isPhpDocToken(tokenSequence);
    }

    private static boolean isOneOfTokens(TokenSequence tokenSequence, PHPTokenId[] tokenIds) {
        TokenId searchedId = tokenSequence.token().id();

        for (TokenId tokenId : tokenIds) {
            if (tokenId.equals(searchedId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEachOfTokens(Token[] tokens, PHPTokenId[] tokenIds) {
        Set<PHPTokenId> set = EnumSet.noneOf(PHPTokenId.class);
        for (Token token : tokens) {
            TokenId searchedId = token.id();
            for (PHPTokenId tokenId : tokenIds) {
                if (tokenId.equals(searchedId)) {
                    set.add(tokenId);
                }
            }
        }
        return set.size() == tokenIds.length;
    }

    private static boolean acceptTokenChainTexts(TokenSequence tokenSequence, List<String> tokenTexts) {
        int orgTokenSequencePos = tokenSequence.offset();
        boolean accept = true;
        boolean moreTokens = tokenSequence.movePrevious();

        for (int i = tokenTexts.size() - 1; i >= 0; i--) {
            String tokenTxt = tokenTexts.get(i);

            if (!moreTokens) {
                accept = false;
                break;
            }

            if (TokenUtilities.textEquals(tokenTxt, tokenSequence.token().text())) {
                moreTokens = tokenSequence.movePrevious();
            } else {
                // NO MATCH
                accept = false;
                break;
            }
        }

        tokenSequence.move(orgTokenSequencePos);
        tokenSequence.moveNext();
        return accept;
    }

    private static boolean acceptTokenChains(TokenSequence tokenSequence, List<Object[]> tokenIdChains, boolean movePrevious) {
        for (Object[] tokenIDChain : tokenIdChains) {
            if (acceptTokenChain(tokenSequence, tokenIDChain, movePrevious)) {
                return true;
            }
        }

        return false;
    }

    private static boolean acceptTokenChain(TokenSequence tokenSequence, Object[] tokenIdChain, boolean movePrevious) {
        int orgTokenSequencePos = tokenSequence.offset();
        boolean accept = true;
        boolean moreTokens = movePrevious ? tokenSequence.movePrevious() : true;
        boolean lastTokenWasComment = false;
        for (int i = tokenIdChain.length - 1; i >= 0; i--) {
            Object tokenID = tokenIdChain[i];

            if (!moreTokens) {
                accept = false;
                break;
            }

            if (tokenID instanceof PHPTokenId) {
                if (isCommentToken(tokenSequence)) {
                    i++;
                    moreTokens = tokenSequence.movePrevious();
                    lastTokenWasComment = true;
                    continue;
                } else if (tokenSequence.token().id() == PHPTokenId.WHITESPACE && lastTokenWasComment) {
                    i++;
                    moreTokens = tokenSequence.movePrevious();
                    lastTokenWasComment = false;
                    continue;
                } else {
                    lastTokenWasComment = false;
                }
                if (tokenSequence.token().id() == tokenID) {
                    moreTokens = tokenSequence.movePrevious();
                } else {
                    // NO MATCH
                    accept = false;
                    break;
                }
            } else if (tokenID == NAMESPACE_FALSE_TOKEN) {
                if (!consumeNameSpace(tokenSequence)) {
                    accept = false;
                    break;
                }
            } else if (tokenID == COMBINED_USE_STATEMENT_TOKENS) {
                if (!consumeClassesInCombinedUse(tokenSequence)) {
                    accept = false;
                    break;
                }
            } else if (tokenID == TYPE_KEYWORD) {
                if (!consumeUntilTypeKeyword(tokenSequence)) {
                    accept = false;
                    break;
                }
            } else {
                assert false : "Unsupported token type: " + tokenID.getClass().getName();
            }
        }

        tokenSequence.move(orgTokenSequencePos);
        tokenSequence.moveNext();
        return accept;
    }

    private static boolean consumeNameSpace(TokenSequence tokenSequence) {
        boolean hadNSSeparator = false;
        if (tokenSequence.token().id() != PHPTokenId.PHP_NS_SEPARATOR
                && tokenSequence.token().id() != PHPTokenId.PHP_STRING) {
            return false;
        }

        do {

            if (tokenSequence.token().id() == PHPTokenId.PHP_NS_SEPARATOR
                    || tokenSequence.token().id() == PHPTokenId.PHP_STRING) {
                hadNSSeparator = true;
            }

            if (!tokenSequence.movePrevious()) {
                return false;
            }

        } while (tokenSequence.token().id() == PHPTokenId.PHP_NS_SEPARATOR
                || tokenSequence.token().id() == PHPTokenId.PHP_STRING);

        return hadNSSeparator;
    }

    private static boolean consumeClassesInCombinedUse(TokenSequence tokenSequence) {
        boolean hasCommaDelimiter = false;
        if (tokenSequence.token().id() != PHPTokenId.PHP_TOKEN
                && tokenSequence.token().id() != PHPTokenId.WHITESPACE
                && !consumeNameSpace(tokenSequence)) {
            return false;
        }

        do {

            if (tokenSequence.token().id() == PHPTokenId.PHP_TOKEN) {
                hasCommaDelimiter = true;
            }

            if (!tokenSequence.movePrevious()) {
                return false;
            }

        } while (tokenSequence.token().id() == PHPTokenId.PHP_TOKEN
                || tokenSequence.token().id() == PHPTokenId.WHITESPACE
                || consumeNameSpace(tokenSequence));

        return hasCommaDelimiter;
    }

    private static boolean consumeUntilTypeKeyword(TokenSequence tokenSequence) {
        boolean result = false;
        do {
            if (tokenSequence.token().id() == PHPTokenId.PHP_CLASS || tokenSequence.token().id() == PHPTokenId.PHP_INTERFACE
                    || tokenSequence.token().id() == PHPTokenId.PHP_TRAIT || tokenSequence.token().id() == PHPTokenId.PHP_EXTENDS
                    || tokenSequence.token().id() == PHPTokenId.PHP_IMPLEMENTS) {
                result = true;
                break;
            }
            if (tokenSequence.token().id() == PHPTokenId.PHP_NAMESPACE) {
                result = false;
                break;
            }
        } while(tokenSequence.movePrevious());
        return result;
    }

    private static Token[] getLeftPreceedingTokens(TokenSequence tokenSequence) {
        Token[] preceedingTokens = getPreceedingTokens(tokenSequence);
        if (preceedingTokens.length == 0) {
            return preceedingTokens;
        }
        Token[] leftPreceedingTokens = new Token[preceedingTokens.length - 1];
        System.arraycopy(preceedingTokens, 1, leftPreceedingTokens, 0, leftPreceedingTokens.length);
        return leftPreceedingTokens;
    }

    private static Token[] getPreceedingTokens(TokenSequence tokenSequence) {
        int orgOffset = tokenSequence.offset();
        LinkedList<Token> tokens = new LinkedList<>();

        boolean success = true;

        // in case we are at the last token
        // include it in the result, see #154055
        if (tokenSequence.moveNext()) {
            success = tokenSequence.movePrevious()
                    && tokenSequence.movePrevious();
        }

        if (success) {
            Token<PHPTokenId> token = tokenSequence.token();
            while (token != null && !CTX_DELIMITERS.contains(token.id())) {
                tokens.addFirst(token);
                if (!tokenSequence.movePrevious()) {
                    break;
                } else {
                    token = tokenSequence.token();
                }
            }
        }

        tokenSequence.move(orgOffset);
        tokenSequence.moveNext();
        return tokens.toArray(new Token[tokens.size()]);
    }

    @CheckForNull
    private static CompletionContext getClsIfaceDeclContext(Token<PHPTokenId> token, int tokenOffset, TokenSequence<PHPTokenId> tokenSequence) {
        boolean isClass = false;
        boolean isIface = false;
        boolean isExtends = false;
        boolean isImplements = false;
        boolean isNsSeparator = false;
        boolean isString = false;
        Token<PHPTokenId> stringToken = null;
        boolean nokeywords;
        List<? extends Token<PHPTokenId>> preceedingLineTokens = getPreceedingLineTokens(token, tokenOffset, tokenSequence);
        for (Token<PHPTokenId> cToken : preceedingLineTokens) {
            TokenId id = cToken.id();
            nokeywords = !isIface && !isClass && !isExtends && !isImplements && !isNsSeparator;
            if (id.equals(PHPTokenId.PHP_CLASS)) {
                isClass = true;
                break;
            } else if (id.equals(PHPTokenId.PHP_INTERFACE)) {
                isIface = true;
                break;
            } else if (id.equals(PHPTokenId.PHP_EXTENDS)) {
                isExtends = true;
            } else if (id.equals(PHPTokenId.PHP_IMPLEMENTS)) {
                isImplements = true;
            } else if (id.equals(PHPTokenId.PHP_NS_SEPARATOR)) {
                isNsSeparator = true;
            } else if (nokeywords && id.equals(PHPTokenId.PHP_STRING)) {
                isString = true;
                stringToken = cToken;
            } else {
                if (nokeywords && id.equals(PHPTokenId.PHP_CURLY_OPEN)) {
                    return null;
                }
            }
        }
        if (isClass || isIface) {
            if (isImplements) {
                return CompletionContext.INTERFACE_NAME;
            } else if (isExtends) {
                if (isString && isClass && stringToken != null && tokenOffset == 0
                        && preceedingLineTokens.size() > 0 && preceedingLineTokens.get(0).text().equals(stringToken.text())) {
                    return CompletionContext.CLASS_NAME;
                } else if (isString && isClass) {
                    return CompletionContext.IMPLEMENTS;
                } else if (!isString && isClass) {
                    return CompletionContext.CLASS_NAME;
                } else if (isIface) {
                    return CompletionContext.INTERFACE_NAME;
                }
                return !isString
                        ? isClass ? CompletionContext.CLASS_NAME : CompletionContext.INTERFACE_NAME
                        : isClass ? CompletionContext.IMPLEMENTS : CompletionContext.INTERFACE_NAME;
            } else if (isIface) {
                return !isString ? CompletionContext.NONE : CompletionContext.EXTENDS;
            } else if (isClass) {
                return !isString ? CompletionContext.NONE : CompletionContext.INHERITANCE;
            }
        } else if (isExtends || isImplements) {
            boolean firstString = false;
            for (Token<PHPTokenId> cToken : preceedingLineTokens) {
                TokenId id = cToken.id();
                if (id == PHPTokenId.PHP_EXTENDS) {
                    return CompletionContext.CLASS_NAME;
                }
                if (id == PHPTokenId.PHP_IMPLEMENTS) {
                    return CompletionContext.INTERFACE_NAME;
                }
                if (id == PHPTokenId.PHP_STRING) {
                    if (!firstString) {
                        firstString = true;
                    } else {
                        break;
                    }
                } else if (id != PHPTokenId.WHITESPACE) {
                    break;
                }

            }
        }
        return null;
    }

    @CheckForNull
    private static CompletionContext getParamaterContext(Token<PHPTokenId> token, int carretOffset, TokenSequence<PHPTokenId> tokenSequence) {
        boolean isFunctionDeclaration = false;
        boolean isCompletionSeparator = false;
        CompletionContext contextForSeparator = null;
        boolean isNamespaceSeparator = false;
        boolean testCompletionSeparator = true;
        int orgOffset = tokenSequence.offset();
        int leftPosition = -1;
        tokenSequence.moveNext();
        while (tokenSequence.movePrevious()) {
            leftPosition++;
            Token<PHPTokenId> cToken = tokenSequence.token();
            PHPTokenId id = cToken.id();
            if (CTX_DELIMITERS.contains(id)) {
                break;
            }
            if (!isFunctionDeclaration) {
                if (!isCompletionSeparator && testCompletionSeparator) {
                    if (isEqualSign(cToken)) {
                        isCompletionSeparator = true;
                        contextForSeparator = CompletionContext.DEFAULT_PARAMETER_VALUE;
                    } else if (isParamSeparator(cToken)) {
                        isCompletionSeparator = true;
                        contextForSeparator = CompletionContext.TYPE_NAME;
                    } else if (isAcceptedPrefix(cToken)) {
                        if (isNamespaceSeparator(cToken)) {
                            isNamespaceSeparator = true;
                            continue;
                        } else if (!isNamespaceSeparator && isString(cToken)) {
                            int offset = cToken.offset(null) + cToken.text().length();
                            if (carretOffset > offset) {
                                testCompletionSeparator = false;
                            }
                        } else if (isReference(cToken) || isRightBracket(cToken) || isVariable(cToken)) {
                            int offset = cToken.offset(null) + cToken.text().length();
                            if (carretOffset >= offset) {
                                testCompletionSeparator = false;
                            }
                        }
                        isNamespaceSeparator = false;
                        continue;
                    } else if (!isCommentToken(tokenSequence)) {
                        testCompletionSeparator = false;
                    }
                } else if (isFunctionDeclaration(cToken)) {
                    isFunctionDeclaration = true;
                    break;
                }
            }
        }
        tokenSequence.move(orgOffset);
        tokenSequence.moveNext();
        return (isFunctionDeclaration && isCompletionSeparator) ? contextForSeparator
                : isFunctionDeclaration ? CompletionContext.NONE : null;
    }

    private static boolean isNamespaceSeparator(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.PHP_NS_SEPARATOR);
    }

    private static boolean isFunctionDeclaration(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.PHP_FUNCTION); //NOI18N
    }

    private static boolean isVariable(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.PHP_VARIABLE); //NOI18N
    }

    private static boolean isReference(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.PHP_TOKEN) && "&".contentEquals(token.text()); //NOI18N
    }

    private static boolean isLeftBracket(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.PHP_TOKEN) && "(".contentEquals(token.text()); //NOI18N
    }

    private static boolean isRightBracket(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.PHP_TOKEN) && ")".contentEquals(token.text()); //NOI18N
    }

    private static boolean isEqualSign(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.PHP_TOKEN) && "=".contentEquals(token.text()); //NOI18N
    }

    private static boolean isParamSeparator(Token<PHPTokenId> token) {
        return isComma(token) || isLeftBracket(token); //NOI18N
    }

    private static boolean isAcceptedPrefix(Token<PHPTokenId> token) {
        return isVariable(token) || isReference(token)
                || isRightBracket(token) || isString(token) || isWhiteSpace(token) || isNamespaceSeparator(token); //NOI18N
    }

    private static boolean isComma(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.PHP_TOKEN) && ",".contentEquals(token.text()); //NOI18N
    }

    private static boolean isWhiteSpace(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.WHITESPACE);
    }

    private static boolean isString(Token<PHPTokenId> token) {
        return token.id().equals(PHPTokenId.PHP_STRING);
    }

    static boolean lineContainsAny(Token<PHPTokenId> token, int tokenOffset, TokenSequence<PHPTokenId> tokenSequence, List<PHPTokenId> ids) {
        List<? extends Token<PHPTokenId>> preceedingLineTokens = getPreceedingLineTokens(token, tokenOffset, tokenSequence);
        for (Token<PHPTokenId> t : preceedingLineTokens) {
            if (ids.contains(t.id())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return all preceding tokens for current line
     */
    private static List<? extends Token<PHPTokenId>> getPreceedingLineTokens(Token<PHPTokenId> token, int tokenOffset, TokenSequence<PHPTokenId> tokenSequence) {
        int orgOffset = tokenSequence.offset();
        LinkedList<Token<PHPTokenId>> tokens = new LinkedList<>();
        if (token.id() != PHPTokenId.WHITESPACE
                || token.text().subSequence(0,
                Math.min(token.text().length(), tokenOffset)).toString().indexOf("\n") == -1) { //NOI18N
            while (true) {
                if (!tokenSequence.movePrevious()) {
                    break;
                }
                Token<PHPTokenId> cToken = tokenSequence.token();
                if (cToken.id() == PHPTokenId.WHITESPACE
                        && cToken.text().toString().indexOf("\n") != -1) { //NOI18N
                    break;
                }
                tokens.addLast(cToken);
            }
        }

        tokenSequence.move(orgOffset);
        tokenSequence.moveNext();

        return tokens;
    }

    private static synchronized boolean isInsideInterfaceDeclarationBlock(final ParserResult info, final int caretOffset, final TokenSequence tokenSequence) {
        boolean retval = false;
        List<ASTNode> nodePath = NavUtils.underCaret(info, lexerToASTOffset(info, caretOffset));
        int nodesCount = nodePath.size();
        if (nodesCount > 0) {
            ASTNode lastNode = nodePath.get(nodesCount - 1);
            if (lastNode instanceof Block) {
                if (nodesCount > 1) {
                    lastNode = nodePath.get(nodesCount - 2);
                    if (lastNode instanceof InterfaceDeclaration) {
                        retval = true;
                    } else {
                        retval = isUnderInterfaceTokenId(tokenSequence);
                    }
                }
            } else {
                retval = isUnderInterfaceTokenId(tokenSequence);
            }
        }
        return retval;
    }

    private static synchronized boolean isUnderInterfaceTokenId(final TokenSequence tokenSequence) {
        boolean retval = false;
        int curlyBalance = -1;
        int orgOffset = tokenSequence.offset();
        try {
            while (tokenSequence.movePrevious()) {
                Token token = tokenSequence.token();
                TokenId id = token.id();
                if (id.equals(PHPTokenId.PHP_INTERFACE) && curlyBalance == 0) {
                    retval = true;
                    break;
                } else if (id.equals(PHPTokenId.PHP_CURLY_OPEN)) {
                    curlyBalance++;
                } else if (id.equals(PHPTokenId.PHP_CURLY_CLOSE)) {
                    curlyBalance--;
                } else if (id.equals(PHPTokenId.PHP_CLASS) || id.equals(PHPTokenId.PHP_WHILE)
                        || id.equals(PHPTokenId.PHP_IF) || id.equals(PHPTokenId.PHP_FOR)
                        || id.equals(PHPTokenId.PHP_FOREACH) || id.equals(PHPTokenId.PHP_TRY)
                        || id.equals(PHPTokenId.PHP_CATCH) || id.equals(PHPTokenId.PHP_FUNCTION)) {
                    // here could be more tokens which can interrupt interface scope, but theese are good enough
                    retval = false;
                    break;
                }
            }
        } finally {
            tokenSequence.move(orgOffset);
            tokenSequence.moveNext();
        }
        return retval;
    }

    private static synchronized boolean isInsideClassDeclarationBlock(ParserResult info,
            int caretOffset, TokenSequence tokenSequence) {
        List<ASTNode> nodePath = NavUtils.underCaret(info, lexerToASTOffset(info, caretOffset));
        boolean methDecl = false;
        boolean funcDecl = false;
        boolean clsDecl = false;
        boolean isClassInsideFunc = false;
        boolean isFuncInsideClass = false;
        for (ASTNode aSTNode : nodePath) {
            if (aSTNode instanceof FunctionDeclaration) {
                funcDecl = true;
                if (clsDecl) {
                    isFuncInsideClass = true;
                }
            } else if (aSTNode instanceof MethodDeclaration) {
                methDecl = true;
            } else if (aSTNode instanceof ClassDeclaration) {
                if (aSTNode.getEndOffset() != caretOffset) {
                    clsDecl = true;
                    if (funcDecl) {
                        isClassInsideFunc = true;
                    }
                } else {
                    return false;
                }
            }
        }
        if (funcDecl && !methDecl && !clsDecl) {
            final StringBuilder sb = new StringBuilder();
            new DefaultVisitor() {

                @Override
                public void visit(ASTError astError) {
                    super.visit(astError);
                    sb.append(astError.toString());
                }
            }.scan(Utils.getRoot(info));
            if (sb.length() == 0) {
                return false;
            }
        }
        if (isClassInsideFunc && !isFuncInsideClass) {
            return true;
        }
        int orgOffset = tokenSequence.offset();
        try {
            int curlyOpen = 0;
            int curlyClose = 0;
            while (tokenSequence.movePrevious()) {
                Token token = tokenSequence.token();
                TokenId id = token.id();
                if (id.equals(PHPTokenId.PHP_CURLY_OPEN)) {
                    curlyOpen++;
                } else if (id.equals(PHPTokenId.PHP_CURLY_CLOSE)) {
                    curlyClose++;
                } else if ((id.equals(PHPTokenId.PHP_FUNCTION)
                        || id.equals(PHPTokenId.PHP_WHILE)
                        || id.equals(PHPTokenId.PHP_IF)
                        || id.equals(PHPTokenId.PHP_FOR)
                        || id.equals(PHPTokenId.PHP_FOREACH)
                        || id.equals(PHPTokenId.PHP_TRY)
                        || id.equals(PHPTokenId.PHP_CATCH))
                        && (curlyOpen > curlyClose)) {
                    return false;
                } else if (id.equals(PHPTokenId.PHP_CLASS)) {
                    boolean isClassScope = curlyOpen > 0 && (curlyOpen > curlyClose);
                    return isClassScope;
                }
            }
        } finally {
            tokenSequence.move(orgOffset);
            tokenSequence.moveNext();
        }
        return false;
    }

    static CompletionContext getCompletionContextInComment(TokenSequence<PHPTokenId> tokenSeq, final int caretOffset, ParserResult info) {
        Token<PHPTokenId> token = tokenSeq.token();
        CharSequence text = token.text();

        if (text == null || text.length() == 0) {
            return CompletionContext.NONE;
        }

        int offset = caretOffset - tokenSeq.offset() - 1;
        char charAt = 0;

        if (offset > -1) {
            charAt = text.charAt(offset--);
            while (-1 < offset && !Character.isWhitespace(charAt) && charAt != '$') {
                charAt = text.charAt(offset);
                offset--;
            }
        }

        if (offset < text.length() && charAt == '$') {
            return CompletionContext.STRING;
        }
        return CompletionContext.TYPE_NAME;
    }

    static int lexerToASTOffset(PHPParseResult result, int lexerOffset) {
//        if (result.getTranslatedSource() != null) {
//            return result.getTranslatedSource().getAstOffset(lexerOffset);
//        }
        return lexerOffset;
    }

    static int lexerToASTOffset(ParserResult info, int lexerOffset) {
        int value = 0;
        if (info instanceof PHPParseResult) {
            PHPParseResult result = (PHPParseResult) info;
            value = lexerToASTOffset(result, lexerOffset);
        }
        return value;
    }
}
