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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.php.editor.indent;

import java.util.prefs.Preferences;
import javax.swing.text.Document;
import org.netbeans.modules.editor.indent.spi.CodeStylePreferences;
import static org.netbeans.modules.php.editor.indent.FmtOptions.*;

/**
 *  XXX make sure the getters get the defaults from somewhere
 *  XXX add support for profiles
 *  XXX get the preferences node from somewhere else in odrer to be able not to
 *      use the getters and to be able to write to it.
 *
 * @author Dusan Balek
 * @author Petr Pisl
 */
public final class CodeStyle {

    static {
        FmtOptions.codeStyleProducer = new Producer();
    }

    private Preferences preferences;

    private CodeStyle(Preferences preferences) {
        this.preferences = preferences;
    }

    /** For testing purposes only. */
    public static CodeStyle get(Preferences prefs) {
        return new CodeStyle(prefs);
    }

    public static CodeStyle get(Document doc) {
        return new CodeStyle(CodeStylePreferences.get(doc).getPreferences());
    }

    // General tabs and indents ------------------------------------------------

    public boolean expandTabToSpaces() {
        return preferences.getBoolean(EXPAND_TAB_TO_SPACES,  getDefaultAsBoolean(EXPAND_TAB_TO_SPACES));
    }

    public int getTabSize() {
        return preferences.getInt(TAB_SIZE, getDefaultAsInt(TAB_SIZE));
    }

    public int getIndentSize() {
        return preferences.getInt(INDENT_SIZE, getDefaultAsInt(INDENT_SIZE));
    }

    public int getContinuationIndentSize() {
        return preferences.getInt(CONTINUATION_INDENT_SIZE, getDefaultAsInt(CONTINUATION_INDENT_SIZE));
    }

    public int getItemsInArrayDeclarationIndentSize() {
        return preferences.getInt(ITEMS_IN_ARRAY_DECLARATION_INDENT_SIZE, getDefaultAsInt(ITEMS_IN_ARRAY_DECLARATION_INDENT_SIZE));
    }

    public int getInitialIndent() {
        return preferences.getInt(INITIAL_INDENT, getDefaultAsInt(INITIAL_INDENT));
    }

    public boolean reformatComments() {
        return preferences.getBoolean(REFORMAT_COMMENTS, getDefaultAsBoolean(REFORMAT_COMMENTS));
    }

    public boolean indentHtml() {
        return preferences.getBoolean(INDENT_HTML, getDefaultAsBoolean(INDENT_HTML));
    }

    public int getRightMargin() {
        return preferences.getInt(RIGHT_MARGIN, getDefaultAsInt(RIGHT_MARGIN));
    }

    // Brace placement --------------------------------------------------------

    public BracePlacement getClassDeclBracePlacement() {
        String placement = preferences.get(CLASS_DECL_BRACE_PLACEMENT, getDefaultAsString(CLASS_DECL_BRACE_PLACEMENT));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getMethodDeclBracePlacement() {
        String placement = preferences.get(METHOD_DECL_BRACE_PLACEMENT, getDefaultAsString(METHOD_DECL_BRACE_PLACEMENT));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getIfBracePlacement() {
        String placement = preferences.get(IF_BRACE_PLACEMENT, getDefaultAsString(IF_BRACE_PLACEMENT));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getForBracePlacement() {
        String placement = preferences.get(FOR_BRACE_PLACEMENT, getDefaultAsString(FOR_BRACE_PLACEMENT));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getWhileBracePlacement() {
        String placement = preferences.get(WHILE_BRACE_PLACEMENT, getDefaultAsString(WHILE_BRACE_PLACEMENT));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getSwitchBracePlacement() {
        String placement = preferences.get(SWITCH_BRACE_PLACEMENT, getDefaultAsString(SWITCH_BRACE_PLACEMENT));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getCatchBracePlacement() {
        String placement = preferences.get(CATCH_BRACE_PLACEMENT, getDefaultAsString(CATCH_BRACE_PLACEMENT));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getUseTraitBodyBracePlacement() {
        String placement = preferences.get(USE_TRAIT_BODY_BRACE_PLACEMENT, getDefaultAsString(USE_TRAIT_BODY_BRACE_PLACEMENT));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getOtherBracePlacement() {
        String placement = preferences.get(OTHER_BRACE_PLACEMENT, getDefaultAsString(OTHER_BRACE_PLACEMENT));
        return BracePlacement.valueOf(placement);
    }

    // Blank lines -------------------------------------------------------------

    public int getBlankLinesBeforeNamespace() {
        return preferences.getInt(BLANK_LINES_BEFORE_NAMESPACE, getDefaultAsInt(BLANK_LINES_BEFORE_NAMESPACE));
    }

    public int getBlankLinesAfterNamespace() {
        return preferences.getInt(BLANK_LINES_AFTER_NAMESPACE, getDefaultAsInt(BLANK_LINES_AFTER_NAMESPACE));
    }

    public int getBlankLinesBeforeUse() {
        return preferences.getInt(BLANK_LINES_BEFORE_USE, getDefaultAsInt(BLANK_LINES_BEFORE_USE));
    }

    public int getBlankLinesBeforeUseTrait() {
        return preferences.getInt(BLANK_LINES_BEFORE_USE_TRAIT, getDefaultAsInt(BLANK_LINES_BEFORE_USE_TRAIT));
    }

    public int getBlankLinesAfterUse() {
        return preferences.getInt(BLANK_LINES_AFTER_USE, getDefaultAsInt(BLANK_LINES_AFTER_USE));
    }

    public int getBlankLinesBeforeClass() {
        return preferences.getInt(BLANK_LINES_BEFORE_CLASS, getDefaultAsInt(BLANK_LINES_BEFORE_CLASS));
    }

    public int getBlankLinesAfterClass() {
        return preferences.getInt(BLANK_LINES_AFTER_CLASS, getDefaultAsInt(BLANK_LINES_AFTER_CLASS));
    }

    public int getBlankLinesAfterClassHeader() {
        return preferences.getInt(BLANK_LINES_AFTER_CLASS_HEADER, getDefaultAsInt(BLANK_LINES_AFTER_CLASS_HEADER));
    }

    public int getBlankLinesBeforeClassEnd() {
        return preferences.getInt(BLANK_LINES_BEFORE_CLASS_END, getDefaultAsInt(BLANK_LINES_BEFORE_CLASS_END));
    }

    public int getBlankLinesBeforeFields() {
        return preferences.getInt(BLANK_LINES_BEFORE_FIELDS, getDefaultAsInt(BLANK_LINES_BEFORE_FIELDS));
    }

    public int getBlankLinesBetweenFields() {
        return preferences.getInt(BLANK_LINES_BETWEEN_FIELDS, getDefaultAsInt(BLANK_LINES_BETWEEN_FIELDS));
    }

    public int getBlankLinesAfterFields() {
        return preferences.getInt(BLANK_LINES_AFTER_FIELDS, getDefaultAsInt(BLANK_LINES_AFTER_FIELDS));
    }

    /**
     *
     * @return true it the fields will be group without php doc together (no empty line between them)
     */
    public boolean getBlankLinesGroupFieldsWithoutDoc() {
        return preferences.getBoolean(BLANK_LINES_GROUP_FIELDS_WITHOUT_DOC, getDefaultAsBoolean(BLANK_LINES_GROUP_FIELDS_WITHOUT_DOC));
    }

    public int getBlankLinesBeforeFunction() {
        return preferences.getInt(BLANK_LINES_BEFORE_FUNCTION, getDefaultAsInt(BLANK_LINES_BEFORE_FUNCTION));
    }

    public int getBlankLinesAfterFunction() {
        return preferences.getInt(BLANK_LINES_AFTER_FUNCTION, getDefaultAsInt(BLANK_LINES_AFTER_FUNCTION));
    }

    public int getBlankLinesBeforeFunctionEnd() {
        return preferences.getInt(BLANK_LINES_BEFORE_FUNCTION_END, getDefaultAsInt(BLANK_LINES_BEFORE_FUNCTION_END));
    }

    public int getBlankLinesAfterOpenPHPTag() {
        return preferences.getInt(BLANK_LINES_AFTER_OPEN_PHP_TAG, getDefaultAsInt(BLANK_LINES_AFTER_OPEN_PHP_TAG));
    }

    public int getBlankLinesAfterOpenPHPTagInHTML() {
        return preferences.getInt(BLANK_LINES_AFTER_OPEN_PHP_TAG_IN_HTML, getDefaultAsInt(BLANK_LINES_AFTER_OPEN_PHP_TAG_IN_HTML));
    }

    public int getBlankLinesBeforeClosePHPTag() {
        return preferences.getInt(BLANK_LINES_BEFORE_CLOSE_PHP_TAG, getDefaultAsInt(BLANK_LINES_BEFORE_CLOSE_PHP_TAG));
    }

    // Spaces ------------------------------------------------------------------

    public boolean spaceBeforeWhile() {
        return preferences.getBoolean(SPACE_BEFORE_WHILE, getDefaultAsBoolean(SPACE_BEFORE_WHILE));
    }

    public boolean spaceBeforeElse() {
        return preferences.getBoolean(SPACE_BEFORE_ELSE, getDefaultAsBoolean(SPACE_BEFORE_ELSE));
    }

    public boolean spaceBeforeCatch() {
        return preferences.getBoolean(SPACE_BEFORE_CATCH, getDefaultAsBoolean(SPACE_BEFORE_CATCH));
    }

    public boolean spaceBeforeFinally() {
        return preferences.getBoolean(SPACE_BEFORE_FINALLY, getDefaultAsBoolean(SPACE_BEFORE_FINALLY));
    }

    public boolean spaceBeforeMethodDeclParen() {
        return preferences.getBoolean(SPACE_BEFORE_METHOD_DECL_PAREN, getDefaultAsBoolean(SPACE_BEFORE_METHOD_DECL_PAREN));
    }

    public boolean spaceBeforeMethodCallParen() {
        return preferences.getBoolean(SPACE_BEFORE_METHOD_CALL_PAREN, getDefaultAsBoolean(SPACE_BEFORE_METHOD_CALL_PAREN));
    }

    public boolean spaceBeforeIfParen() {
        return preferences.getBoolean(SPACE_BEFORE_IF_PAREN, getDefaultAsBoolean(SPACE_BEFORE_IF_PAREN));
    }

    public boolean spaceBeforeForParen() {
        return preferences.getBoolean(SPACE_BEFORE_FOR_PAREN, getDefaultAsBoolean(SPACE_BEFORE_FOR_PAREN));
    }

    public boolean spaceBeforeWhileParen() {
        return preferences.getBoolean(SPACE_BEFORE_WHILE_PAREN, getDefaultAsBoolean(SPACE_BEFORE_WHILE_PAREN));
    }

    public boolean spaceBeforeCatchParen() {
        return preferences.getBoolean(SPACE_BEFORE_CATCH_PAREN, getDefaultAsBoolean(SPACE_BEFORE_CATCH_PAREN));
    }

    public boolean spaceBeforeSwitchParen() {
        return preferences.getBoolean(SPACE_BEFORE_SWITCH_PAREN, getDefaultAsBoolean(SPACE_BEFORE_SWITCH_PAREN));
    }

    public boolean spaceBeforeArrayDeclParen() {
        return preferences.getBoolean(SPACE_BEFORE_ARRAY_DECL_PAREN, getDefaultAsBoolean(SPACE_BEFORE_ARRAY_DECL_PAREN));
    }

    public boolean spaceAroundUnaryOps() {
        return preferences.getBoolean(SPACE_AROUND_UNARY_OPS, getDefaultAsBoolean(SPACE_AROUND_UNARY_OPS));
    }

    public boolean spaceAroundBinaryOps() {
        return preferences.getBoolean(SPACE_AROUND_BINARY_OPS, getDefaultAsBoolean(SPACE_AROUND_BINARY_OPS));
    }

    public boolean spaceAroundStringConcatOps() {
        return preferences.getBoolean(SPACE_AROUND_STRING_CONCAT_OPS, getDefaultAsBoolean(SPACE_AROUND_STRING_CONCAT_OPS));
    }

    public boolean spaceAroundTernaryOps() {
        return preferences.getBoolean(SPACE_AROUND_TERNARY_OPS, getDefaultAsBoolean(SPACE_AROUND_TERNARY_OPS));
    }

    public boolean spaceAroundKeyValueOps() {
        return preferences.getBoolean(SPACE_AROUND_KEY_VALUE_OPS, getDefaultAsBoolean(SPACE_AROUND_KEY_VALUE_OPS));
    }

    public boolean spaceAroundAssignOps() {
        return preferences.getBoolean(SPACE_AROUND_ASSIGN_OPS, getDefaultAsBoolean(SPACE_AROUND_ASSIGN_OPS));
    }

    public boolean spaceAroundObjectOps() {
        return preferences.getBoolean(SPACE_AROUND_OBJECT_OPS, getDefaultAsBoolean(SPACE_AROUND_OBJECT_OPS));
    }

    public boolean spaceBeforeClassDeclLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_CLASS_DECL_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_CLASS_DECL_LEFT_BRACE));
    }

    public boolean spaceBeforeMethodDeclLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_METHOD_DECL_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_METHOD_DECL_LEFT_BRACE));
    }

    public boolean spaceBeforeIfLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_IF_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_IF_LEFT_BRACE));
    }

    public boolean spaceBeforeElseLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_ELSE_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_ELSE_LEFT_BRACE));
    }

    public boolean spaceBeforeWhileLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_WHILE_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_WHILE_LEFT_BRACE));
    }

    public boolean spaceBeforeForLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_FOR_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_FOR_LEFT_BRACE));
    }

    public boolean spaceBeforeDoLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_DO_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_DO_LEFT_BRACE));
    }

    public boolean spaceBeforeSwitchLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_SWITCH_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_SWITCH_LEFT_BRACE));
    }

    public boolean spaceBeforeTryLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_TRY_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_TRY_LEFT_BRACE));
    }

    public boolean spaceBeforeCatchLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_CATCH_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_CATCH_LEFT_BRACE));
    }

    public boolean spaceBeforeFinallyLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_FINALLY_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_FINALLY_LEFT_BRACE));
    }

    public boolean spaceBeforeUseTraitBodyLeftBrace() {
        return preferences.getBoolean(SPACE_BEFORE_USE_TRAIT_BODY_LEFT_BRACE, getDefaultAsBoolean(SPACE_BEFORE_USE_TRAIT_BODY_LEFT_BRACE));
    }

    public boolean spaceWithinMethodDeclParens() {
        return preferences.getBoolean(SPACE_WITHIN_METHOD_DECL_PARENS, getDefaultAsBoolean(SPACE_WITHIN_METHOD_DECL_PARENS));
    }

    public boolean spaceWithinMethodCallParens() {
        return preferences.getBoolean(SPACE_WITHIN_METHOD_CALL_PARENS, getDefaultAsBoolean(SPACE_WITHIN_METHOD_CALL_PARENS));
    }

    public boolean spaceWithinIfParens() {
        return preferences.getBoolean(SPACE_WITHIN_IF_PARENS, getDefaultAsBoolean(SPACE_WITHIN_IF_PARENS));
    }

    public boolean spaceWithinForParens() {
        return preferences.getBoolean(SPACE_WITHIN_FOR_PARENS, getDefaultAsBoolean(SPACE_WITHIN_FOR_PARENS));
    }

    public boolean spaceWithinWhileParens() {
        return preferences.getBoolean(SPACE_WITHIN_WHILE_PARENS, getDefaultAsBoolean(SPACE_WITHIN_WHILE_PARENS));
    }

    public boolean spaceWithinSwitchParens() {
        return preferences.getBoolean(SPACE_WITHIN_SWITCH_PARENS, getDefaultAsBoolean(SPACE_WITHIN_SWITCH_PARENS));
    }

    public boolean spaceWithinCatchParens() {
        return preferences.getBoolean(SPACE_WITHIN_CATCH_PARENS, getDefaultAsBoolean(SPACE_WITHIN_CATCH_PARENS));
    }

    public boolean spaceWithinTypeCastParens() {
        return preferences.getBoolean(SPACE_WITHIN_TYPE_CAST_PARENS, getDefaultAsBoolean(SPACE_WITHIN_TYPE_CAST_PARENS));
    }

    public boolean spaceWithinArrayDeclParens() {
        return preferences.getBoolean(SPACE_WITHIN_ARRAY_DECL_PARENS, getDefaultAsBoolean(SPACE_WITHIN_ARRAY_DECL_PARENS));
    }

    public boolean spaceWithinArrayBrackets() {
        return preferences.getBoolean(SPACE_WITHIN_ARRAY_BRACKETS, getDefaultAsBoolean(SPACE_WITHIN_ARRAY_BRACKETS));
    }

    public boolean spaceBeforeComma() {
        return preferences.getBoolean(SPACE_BEFORE_COMMA, getDefaultAsBoolean(SPACE_BEFORE_COMMA));
    }

    public boolean spaceAfterComma() {
        return preferences.getBoolean(SPACE_AFTER_COMMA, getDefaultAsBoolean(SPACE_AFTER_COMMA));
    }

    public boolean spaceBeforeSemi() {
        return preferences.getBoolean(SPACE_BEFORE_SEMI, getDefaultAsBoolean(SPACE_BEFORE_SEMI));
    }

    public boolean spaceAfterSemi() {
        return preferences.getBoolean(SPACE_AFTER_SEMI, getDefaultAsBoolean(SPACE_AFTER_SEMI));
    }

    public boolean spaceAfterTypeCast() {
        return preferences.getBoolean(SPACE_AFTER_TYPE_CAST, getDefaultAsBoolean(SPACE_AFTER_TYPE_CAST));
    }

    public boolean spaceCheckAfterKeywords() {
        return preferences.getBoolean(SPACE_CHECK_AFTER_KEYWORDS, getDefaultAsBoolean(SPACE_CHECK_AFTER_KEYWORDS));
    }

    public boolean spaceAfterShortPHPTag() {
        return preferences.getBoolean(SPACE_AFTER_SHORT_PHP_TAG, getDefaultAsBoolean(SPACE_AFTER_SHORT_PHP_TAG));
    }

    public boolean spaceBeforeClosePHPTag() {
        return preferences.getBoolean(SPACE_BEFORE_CLOSE_PHP_TAG, getDefaultAsBoolean(SPACE_BEFORE_CLOSE_PHP_TAG));
    }

    // alignment
    public boolean alignMultilineMethodParams() {
        return preferences.getBoolean(ALIGN_MULTILINE_METHOD_PARAMS, getDefaultAsBoolean(ALIGN_MULTILINE_METHOD_PARAMS));
    }

    public boolean alignMultilineCallArgs() {
        return preferences.getBoolean(ALIGN_MULTILINE_CALL_ARGS, getDefaultAsBoolean(ALIGN_MULTILINE_CALL_ARGS));
    }

    public boolean alignMultilineImplements() {
        return preferences.getBoolean(ALIGN_MULTILINE_IMPLEMENTS, getDefaultAsBoolean(ALIGN_MULTILINE_IMPLEMENTS));
    }

    public boolean alignMultilineParenthesized() {
        return preferences.getBoolean(ALIGN_MULTILINE_PARENTHESIZED, getDefaultAsBoolean(ALIGN_MULTILINE_PARENTHESIZED));
    }

    public boolean alignMultilineBinaryOp() {
        return preferences.getBoolean(ALIGN_MULTILINE_BINARY_OP, getDefaultAsBoolean(ALIGN_MULTILINE_BINARY_OP));
    }

    public boolean alignMultilineTernaryOp() {
        return preferences.getBoolean(ALIGN_MULTILINE_TERNARY_OP, getDefaultAsBoolean(ALIGN_MULTILINE_TERNARY_OP));
    }

    public boolean alignMultilineAssignment() {
        return preferences.getBoolean(ALIGN_MULTILINE_ASSIGNMENT, getDefaultAsBoolean(ALIGN_MULTILINE_ASSIGNMENT));
    }

    public boolean alignMultilineFor() {
        return preferences.getBoolean(ALIGN_MULTILINE_FOR, getDefaultAsBoolean(ALIGN_MULTILINE_FOR));
    }

    public boolean alignMultilineArrayInit() {
        //return preferences.getBoolean(alignMultilineArrayInit, getDefaultAsBoolean(alignMultilineArrayInit));
        return false;
    }

    public boolean placeElseOnNewLine() {
        return preferences.getBoolean(PLACE_ELSE_ON_NEW_LINE, getDefaultAsBoolean(PLACE_ELSE_ON_NEW_LINE));
    }

    public boolean placeWhileOnNewLine() {
        return preferences.getBoolean(PLACE_WHILE_ON_NEW_LINE, getDefaultAsBoolean(PLACE_WHILE_ON_NEW_LINE));
    }

    public boolean placeCatchOnNewLine() {
        return preferences.getBoolean(PLACE_CATCH_ON_NEW_LINE, getDefaultAsBoolean(PLACE_CATCH_ON_NEW_LINE));
    }

    public boolean placeFinallyOnNewLine() {
        return preferences.getBoolean(PLACE_FINALLY_ON_NEW_LINE, getDefaultAsBoolean(PLACE_FINALLY_ON_NEW_LINE));
    }

    public boolean placeNewLineAfterModifiers() {
        return preferences.getBoolean(PLACE_NEW_LINE_AFTER_MODIFIERS, getDefaultAsBoolean(PLACE_NEW_LINE_AFTER_MODIFIERS));
    }

    public boolean groupMulitlineAssignment() {
        return preferences.getBoolean(GROUP_ALIGNMENT_ASSIGNMENT, getDefaultAsBoolean(GROUP_ALIGNMENT_ASSIGNMENT));
    }

    public boolean groupMulitlineArrayInit() {
        return preferences.getBoolean(GROUP_ALIGNMENT_ARRAY_INIT, getDefaultAsBoolean(GROUP_ALIGNMENT_ARRAY_INIT));
    }

    // Wrapping ----------------------------------------------------------------

    public WrapStyle wrapExtendsImplementsKeyword() {
        String wrap = preferences.get(WRAP_EXTENDS_IMPLEMENTS_KEYWORD, getDefaultAsString(WRAP_EXTENDS_IMPLEMENTS_KEYWORD));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapExtendsImplementsList() {
        String wrap = preferences.get(WRAP_EXTENDS_IMPLEMENTS_LIST, getDefaultAsString(WRAP_EXTENDS_IMPLEMENTS_LIST));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapMethodParams() {
        String wrap = preferences.get(WRAP_METHOD_PARAMS, getDefaultAsString(WRAP_METHOD_PARAMS));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapMethodCallArgs() {
        String wrap = preferences.get(WRAP_METHOD_CALL_ARGS, getDefaultAsString(WRAP_METHOD_CALL_ARGS));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapChainedMethodCalls() {
        String wrap = preferences.get(WRAP_CHAINED_METHOD_CALLS, getDefaultAsString(WRAP_CHAINED_METHOD_CALLS));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapArrayInit() {
        String wrap = preferences.get(WRAP_ARRAY_INIT, getDefaultAsString(WRAP_ARRAY_INIT));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapFor() {
        String wrap = preferences.get(WRAP_FOR, getDefaultAsString(WRAP_FOR));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapForStatement() {
        String wrap = preferences.get(WRAP_FOR_STATEMENT, getDefaultAsString(WRAP_FOR_STATEMENT));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapIfStatement() {
        String wrap = preferences.get(WRAP_IF_STATEMENT, getDefaultAsString(WRAP_IF_STATEMENT));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapWhileStatement() {
        String wrap = preferences.get(WRAP_WHILE_STATEMENT, getDefaultAsString(WRAP_WHILE_STATEMENT));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapDoWhileStatement() {
        String wrap = preferences.get(WRAP_DO_WHILE_STATEMENT, getDefaultAsString(WRAP_DO_WHILE_STATEMENT));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapBinaryOps() {
        String wrap = preferences.get(WRAP_BINARY_OPS, getDefaultAsString(WRAP_BINARY_OPS));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapTernaryOps() {
        String wrap = preferences.get(WRAP_TERNARY_OPS, getDefaultAsString(WRAP_TERNARY_OPS));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapAssignOps() {
        String wrap = preferences.get(WRAP_ASSIGN_OPS, getDefaultAsString(WRAP_ASSIGN_OPS));
        return WrapStyle.valueOf(wrap);
    }

    public boolean wrapBlockBrace() {
        return preferences.getBoolean(WRAP_BLOCK_BRACES, getDefaultAsBoolean(WRAP_BLOCK_BRACES));
    }

    public boolean wrapStatementsOnTheSameLine() {
        return preferences.getBoolean(WRAP_STATEMENTS_ON_THE_LINE, getDefaultAsBoolean(WRAP_STATEMENTS_ON_THE_LINE));
    }

    public boolean wrapAfterBinOps() {
        return preferences.getBoolean(WRAP_AFTER_BIN_OPS, getDefaultAsBoolean(WRAP_AFTER_BIN_OPS));
    }

    public boolean wrapAfterAssignOps() {
        return preferences.getBoolean(WRAP_AFTER_ASSIGN_OPS, getDefaultAsBoolean(WRAP_AFTER_ASSIGN_OPS));
    }

    // Uses

    public boolean preferFullyQualifiedNames() {
        return preferences.getBoolean(PREFER_FULLY_QUALIFIED_NAMES, getDefaultAsBoolean(PREFER_FULLY_QUALIFIED_NAMES));
    }

    public boolean preferMultipleUseStatementsCombined() {
        return preferences.getBoolean(PREFER_MULTIPLE_USE_STATEMENTS_COMBINED, getDefaultAsBoolean(PREFER_MULTIPLE_USE_STATEMENTS_COMBINED));
    }

    public boolean startUseWithNamespaceSeparator() {
        return preferences.getBoolean(START_USE_WITH_NAMESPACE_SEPARATOR, getDefaultAsBoolean(START_USE_WITH_NAMESPACE_SEPARATOR));
    }

    public boolean aliasesFromCapitalsOfNamespaces() {
        return preferences.getBoolean(ALIASES_CAPITALS_OF_NAMESPACES, getDefaultAsBoolean(ALIASES_CAPITALS_OF_NAMESPACES));
    }

    private static class Producer implements FmtOptions.CodeStyleProducer {

        @Override
        public CodeStyle create(Preferences preferences) {
            return new CodeStyle(preferences);
        }
    }

    public enum BracePlacement {
        SAME_LINE,
        NEW_LINE,
        NEW_LINE_INDENTED,
        PRESERVE_EXISTING
    }

    public enum WrapStyle {
        WRAP_ALWAYS,
        WRAP_IF_LONG,
        WRAP_NEVER
    }
}
