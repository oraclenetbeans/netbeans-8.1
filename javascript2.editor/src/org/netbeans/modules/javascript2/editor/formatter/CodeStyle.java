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

package org.netbeans.modules.javascript2.editor.formatter;

import java.util.prefs.Preferences;

import javax.swing.text.Document;
import org.netbeans.modules.editor.indent.spi.CodeStylePreferences;
import static org.netbeans.modules.javascript2.editor.formatter.FmtOptions.*;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;

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

    private final Defaults.Provider provider;

    private final Preferences preferences;

    private CodeStyle(Defaults.Provider provider, Preferences preferences) {
        this.provider = provider;
        this.preferences = preferences;
    }

    /** For testing purposes only */
    public static CodeStyle get(Preferences prefs, Defaults.Provider provider) {
        return new CodeStyle(provider, prefs);
    }

    public static CodeStyle get(Document doc, Defaults.Provider provider) {
        return new CodeStyle(provider, CodeStylePreferences.get(doc).getPreferences());
    }
    
    public static CodeStyle get(FormatContext context) {
        return get(context.getDefaultsProvider(), context.getDocument(), context.isEmbedded());
    }

    public static CodeStyle get(IndentContext context) {
        return get(context.getDefaultsProvider(), context.getDocument(), context.isEmbedded());
    }

    private static CodeStyle get(Defaults.Provider provider, Document doc, boolean embedded) {
        if (embedded) {
            return new CodeStyle(provider, CodeStylePreferences.get(doc, JsTokenId.JAVASCRIPT_MIME_TYPE).getPreferences());
        }
        return new CodeStyle(provider, CodeStylePreferences.get(doc).getPreferences());
    }

    public Holder toHolder() {
        return new Holder(this);
    }

    // General tabs and indents ------------------------------------------------

    public boolean expandTabToSpaces () {
        return preferences.getBoolean(expandTabToSpaces,  provider.getDefaultAsBoolean(expandTabToSpaces));
    }

    public int getTabSize() {
        return preferences.getInt(tabSize, provider.getDefaultAsInt(tabSize));
    }

    public int getIndentSize() {
        return preferences.getInt(indentSize, provider.getDefaultAsInt(indentSize));
    }

    public int getContinuationIndentSize() {
        return preferences.getInt(continuationIndentSize, provider.getDefaultAsInt(continuationIndentSize));
    }

    public int getItemsInArrayDeclarationIndentSize() {
        return preferences.getInt(itemsInArrayDeclarationIndentSize, provider.getDefaultAsInt(itemsInArrayDeclarationIndentSize));
    }

    public int getInitialIndent(){
        return preferences.getInt(initialIndent, provider.getDefaultAsInt(initialIndent));
    }

    public boolean reformatComments() {
        return preferences.getBoolean(reformatComments, provider.getDefaultAsBoolean(reformatComments));
    }

    public boolean indentHtml() {
        return preferences.getBoolean(indentHtml, provider.getDefaultAsBoolean(indentHtml));
    }

    public int getRightMargin() {
        return preferences.getInt(rightMargin, provider.getDefaultAsInt(rightMargin));
    }

    public boolean continuationBeforeObjectLiteral() {
        return preferences.getBoolean(objectLiteralContinuation, provider.getDefaultAsBoolean(objectLiteralContinuation));
    }

    // Brace placement --------------------------------------------------------

    public BracePlacement getFunctionDeclBracePlacement() {
        String placement = preferences.get(functionDeclBracePlacement, provider.getDefaultAsString(functionDeclBracePlacement));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getIfBracePlacement() {
        String placement = preferences.get(ifBracePlacement, provider.getDefaultAsString(ifBracePlacement));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getForBracePlacement() {
        String placement = preferences.get(forBracePlacement, provider.getDefaultAsString(forBracePlacement));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getWhileBracePlacement() {
        String placement = preferences.get(whileBracePlacement, provider.getDefaultAsString(whileBracePlacement));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getSwitchBracePlacement() {
        String placement = preferences.get(switchBracePlacement, provider.getDefaultAsString(switchBracePlacement));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getCatchBracePlacement() {
        String placement = preferences.get(catchBracePlacement, provider.getDefaultAsString(catchBracePlacement));
        return BracePlacement.valueOf(placement);
    }

    public BracePlacement getWithBracePlacement() {
        String placement = preferences.get(withBracePlacement, provider.getDefaultAsString(withBracePlacement));
        return BracePlacement.valueOf(placement);
    }

    // Blank lines -------------------------------------------------------------

//    public int getBlankLinesBeforeNamespace() {
//        return preferences.getInt(blankLinesBeforeNamespace, provider.getDefaultAsInt(blankLinesBeforeNamespace));
//    }
//
//    public int getBlankLinesAfterNamespace() {
//        return preferences.getInt(blankLinesAfterNamespace, provider.getDefaultAsInt(blankLinesAfterNamespace));
//    }
//
//    public int getBlankLinesBeforeUse() {
//        return preferences.getInt(blankLinesBeforeUse, provider.getDefaultAsInt(blankLinesBeforeUse));
//    }
//
//    public int getBlankLinesBeforeUseTrait() {
//        return preferences.getInt(blankLinesBeforeUseTrait, provider.getDefaultAsInt(blankLinesBeforeUseTrait));
//    }
//
//    public int getBlankLinesAfterUse() {
//        return preferences.getInt(blankLinesAfterUse, provider.getDefaultAsInt(blankLinesAfterUse));
//    }
//
//    public int getBlankLinesBeforeClass() {
//        return preferences.getInt(blankLinesBeforeClass, provider.getDefaultAsInt(blankLinesBeforeClass));
//    }
//
//    public int getBlankLinesAfterClass() {
//        return preferences.getInt(blankLinesAfterClass, provider.getDefaultAsInt(blankLinesAfterClass));
//    }
//
//    public int getBlankLinesAfterClassHeader() {
//        return preferences.getInt(blankLinesAfterClassHeader, provider.getDefaultAsInt(blankLinesAfterClassHeader));
//    }
//
//    public int getBlankLinesBeforeClassEnd() {
//        return preferences.getInt(blankLinesBeforeClassEnd, provider.getDefaultAsInt(blankLinesBeforeClassEnd));
//    }
//
//    public int getBlankLinesBeforeFields() {
//        return preferences.getInt(blankLinesBeforeFields, provider.getDefaultAsInt(blankLinesBeforeFields));
//    }
//
//    public int getBlankLinesBetweenFields() {
//        return preferences.getInt(blankLinesBetweenFields, provider.getDefaultAsInt(blankLinesBetweenFields));
//    }
//
//    public int getBlankLinesAfterFields() {
//        return preferences.getInt(blankLinesAfterFields, provider.getDefaultAsInt(blankLinesAfterFields));
//    }
//
//    /**
//     *
//     * @return true it the fields will be group without php doc together (no empty line between them)
//     */
//    public boolean getBlankLinesGroupFieldsWithoutDoc() {
//	return preferences.getBoolean(blankLinesGroupFieldsWithoutDoc, provider.getDefaultAsBoolean(blankLinesGroupFieldsWithoutDoc));
//    }
//
//    public int getBlankLinesBeforeFunction() {
//        return preferences.getInt(blankLinesBeforeFunction, provider.getDefaultAsInt(blankLinesBeforeFunction));
//    }
//
//    public int getBlankLinesAfterFunction() {
//        return preferences.getInt(blankLinesAfterFunction, provider.getDefaultAsInt(blankLinesAfterFunction));
//    }
//
//    public int getBlankLinesBeforeFunctionEnd() {
//        return preferences.getInt(blankLinesBeforeFunctionEnd, provider.getDefaultAsInt(blankLinesBeforeFunctionEnd));
//    }
//
//    public int getBlankLinesAfterOpenPHPTag() {
//        return preferences.getInt(blankLinesAfterOpenPHPTag, provider.getDefaultAsInt(blankLinesAfterOpenPHPTag));
//    }
//
//    public int getBlankLinesAfterOpenPHPTagInHTML() {
//        return preferences.getInt(blankLinesAfterOpenPHPTagInHTML, provider.getDefaultAsInt(blankLinesAfterOpenPHPTagInHTML));
//    }
//
//    public int getBlankLinesBeforeClosePHPTag() {
//        return preferences.getInt(blankLinesBeforeClosePHPTag, provider.getDefaultAsInt(blankLinesBeforeClosePHPTag));
//    }

    // Spaces ------------------------------------------------------------------

    public boolean spaceBeforeWhile() {
        return preferences.getBoolean(spaceBeforeWhile, provider.getDefaultAsBoolean(spaceBeforeWhile));
    }

    public boolean spaceBeforeElse() {
        return preferences.getBoolean(spaceBeforeElse, provider.getDefaultAsBoolean(spaceBeforeElse));
    }

    public boolean spaceBeforeCatch() {
        return preferences.getBoolean(spaceBeforeCatch, provider.getDefaultAsBoolean(spaceBeforeCatch));
    }

    public boolean spaceBeforeFinally() {
        return preferences.getBoolean(spaceBeforeFinally, provider.getDefaultAsBoolean(spaceBeforeFinally));
    }

    public boolean spaceBeforeMethodDeclParen() {
        return preferences.getBoolean(spaceBeforeMethodDeclParen, provider.getDefaultAsBoolean(spaceBeforeMethodDeclParen));
    }
    
    public boolean spaceBeforeAnonMethodDeclParen() {
        return preferences.getBoolean(spaceBeforeAnonMethodDeclParen, provider.getDefaultAsBoolean(spaceBeforeAnonMethodDeclParen));
    }

    public boolean spaceBeforeMethodCallParen() {
        return preferences.getBoolean(spaceBeforeMethodCallParen, provider.getDefaultAsBoolean(spaceBeforeMethodCallParen));
    }

    public boolean spaceBeforeIfParen() {
        return preferences.getBoolean(spaceBeforeIfParen, provider.getDefaultAsBoolean(spaceBeforeIfParen));
    }

    public boolean spaceBeforeForParen() {
        return preferences.getBoolean(spaceBeforeForParen, provider.getDefaultAsBoolean(spaceBeforeForParen));
    }

    public boolean spaceBeforeWhileParen() {
        return preferences.getBoolean(spaceBeforeWhileParen, provider.getDefaultAsBoolean(spaceBeforeWhileParen));
    }

    public boolean spaceBeforeCatchParen() {
        return preferences.getBoolean(spaceBeforeCatchParen, provider.getDefaultAsBoolean(spaceBeforeCatchParen));
    }

    public boolean spaceBeforeSwitchParen() {
        return preferences.getBoolean(spaceBeforeSwitchParen, provider.getDefaultAsBoolean(spaceBeforeSwitchParen));
    }

    public boolean spaceBeforeWithParen() {
        return preferences.getBoolean(spaceBeforeWithParen, provider.getDefaultAsBoolean(spaceBeforeWithParen));
    }

    public boolean spaceAroundUnaryOps() {
        return preferences.getBoolean(spaceAroundUnaryOps, provider.getDefaultAsBoolean(spaceAroundUnaryOps));
    }

    public boolean spaceAroundBinaryOps() {
        return preferences.getBoolean(spaceAroundBinaryOps, provider.getDefaultAsBoolean(spaceAroundBinaryOps));
    }

    public boolean spaceAroundStringConcatOps() {
        return preferences.getBoolean(spaceAroundStringConcatOps, provider.getDefaultAsBoolean(spaceAroundStringConcatOps));
    }

    public boolean spaceAroundTernaryOps() {
        return preferences.getBoolean(spaceAroundTernaryOps, provider.getDefaultAsBoolean(spaceAroundTernaryOps));
    }

    public boolean spaceAroundKeyValueOps() {
        return preferences.getBoolean(spaceAroundKeyValueOps, provider.getDefaultAsBoolean(spaceAroundKeyValueOps));
    }

    public boolean spaceAroundAssignOps() {
        return preferences.getBoolean(spaceAroundAssignOps, provider.getDefaultAsBoolean(spaceAroundAssignOps));
    }

    public boolean spaceAroundObjectOps() {
        return preferences.getBoolean(spaceAroundObjectOps, provider.getDefaultAsBoolean(spaceAroundObjectOps));
    }

    public boolean spaceBeforeClassDeclLeftBrace() {
        return preferences.getBoolean(spaceBeforeClassDeclLeftBrace, provider.getDefaultAsBoolean(spaceBeforeClassDeclLeftBrace));
    }

    public boolean spaceBeforeMethodDeclLeftBrace() {
        return preferences.getBoolean(spaceBeforeMethodDeclLeftBrace, provider.getDefaultAsBoolean(spaceBeforeMethodDeclLeftBrace));
    }

    public boolean spaceBeforeIfLeftBrace() {
        return preferences.getBoolean(spaceBeforeIfLeftBrace, provider.getDefaultAsBoolean(spaceBeforeIfLeftBrace));
    }

    public boolean spaceBeforeElseLeftBrace() {
        return preferences.getBoolean(spaceBeforeElseLeftBrace, provider.getDefaultAsBoolean(spaceBeforeElseLeftBrace));
    }

    public boolean spaceBeforeWhileLeftBrace() {
        return preferences.getBoolean(spaceBeforeWhileLeftBrace, provider.getDefaultAsBoolean(spaceBeforeWhileLeftBrace));
    }

    public boolean spaceBeforeForLeftBrace() {
        return preferences.getBoolean(spaceBeforeForLeftBrace, provider.getDefaultAsBoolean(spaceBeforeForLeftBrace));
    }

    public boolean spaceBeforeDoLeftBrace() {
        return preferences.getBoolean(spaceBeforeDoLeftBrace, provider.getDefaultAsBoolean(spaceBeforeDoLeftBrace));
    }

    public boolean spaceBeforeSwitchLeftBrace() {
        return preferences.getBoolean(spaceBeforeSwitchLeftBrace, provider.getDefaultAsBoolean(spaceBeforeSwitchLeftBrace));
    }

    public boolean spaceBeforeTryLeftBrace() {
        return preferences.getBoolean(spaceBeforeTryLeftBrace, provider.getDefaultAsBoolean(spaceBeforeTryLeftBrace));
    }

    public boolean spaceBeforeCatchLeftBrace() {
        return preferences.getBoolean(spaceBeforeCatchLeftBrace, provider.getDefaultAsBoolean(spaceBeforeCatchLeftBrace));
    }

    public boolean spaceBeforeFinallyLeftBrace() {
        return preferences.getBoolean(spaceBeforeFinallyLeftBrace, provider.getDefaultAsBoolean(spaceBeforeFinallyLeftBrace));
    }

    public boolean spaceBeforeWithLeftBrace() {
        return preferences.getBoolean(spaceBeforeWithLeftBrace, provider.getDefaultAsBoolean(spaceBeforeWithLeftBrace));
    }

    public boolean spaceWithinParens() {
        return preferences.getBoolean(spaceWithinParens, provider.getDefaultAsBoolean(spaceWithinParens));
    }

    public boolean spaceWithinMethodDeclParens() {
        return preferences.getBoolean(spaceWithinMethodDeclParens, provider.getDefaultAsBoolean(spaceWithinMethodDeclParens));
    }

    public boolean spaceWithinMethodCallParens() {
        return preferences.getBoolean(spaceWithinMethodCallParens, provider.getDefaultAsBoolean(spaceWithinMethodCallParens));
    }

    public boolean spaceWithinIfParens() {
        return preferences.getBoolean(spaceWithinIfParens, provider.getDefaultAsBoolean(spaceWithinIfParens));
    }

    public boolean spaceWithinForParens() {
        return preferences.getBoolean(spaceWithinForParens, provider.getDefaultAsBoolean(spaceWithinForParens));
    }

    public boolean spaceWithinWhileParens() {
        return preferences.getBoolean(spaceWithinWhileParens, provider.getDefaultAsBoolean(spaceWithinWhileParens));
    }

    public boolean spaceWithinSwitchParens() {
        return preferences.getBoolean(spaceWithinSwitchParens, provider.getDefaultAsBoolean(spaceWithinSwitchParens));
    }

    public boolean spaceWithinCatchParens() {
        return preferences.getBoolean(spaceWithinCatchParens, provider.getDefaultAsBoolean(spaceWithinCatchParens));
    }

    public boolean spaceWithinWithParens() {
        return preferences.getBoolean(spaceWithinWithParens, provider.getDefaultAsBoolean(spaceWithinWithParens));
    }

    public boolean spaceWithinTypeCastParens() {
        return preferences.getBoolean(spaceWithinTypeCastParens, provider.getDefaultAsBoolean(spaceWithinTypeCastParens));
    }

    public boolean spaceWithinArrayDeclParens() {
        return preferences.getBoolean(spaceWithinArrayDeclParens, provider.getDefaultAsBoolean(spaceWithinArrayDeclParens));
    }

    public boolean spaceWithinBraces() {
        return preferences.getBoolean(spaceWithinBraces, provider.getDefaultAsBoolean(spaceWithinBraces));
    }

    public boolean spaceWithinArrayBrackets() {
        return preferences.getBoolean(spaceWithinArrayBrackets, provider.getDefaultAsBoolean(spaceWithinArrayBrackets));
    }

    public boolean spaceBeforeComma() {
        return preferences.getBoolean(spaceBeforeComma, provider.getDefaultAsBoolean(spaceBeforeComma));
    }

    public boolean spaceAfterComma() {
        return preferences.getBoolean(spaceAfterComma, provider.getDefaultAsBoolean(spaceAfterComma));
    }

    public boolean spaceBeforeSemi() {
        return preferences.getBoolean(spaceBeforeSemi, provider.getDefaultAsBoolean(spaceBeforeSemi));
    }

    public boolean spaceAfterSemi() {
        return preferences.getBoolean(spaceAfterSemi, provider.getDefaultAsBoolean(spaceAfterSemi));
    }

    public boolean spaceBeforeColon() {
        return preferences.getBoolean(spaceBeforeColon, provider.getDefaultAsBoolean(spaceBeforeColon));
    }

    public boolean spaceAfterColon() {
        return preferences.getBoolean(spaceAfterColon, provider.getDefaultAsBoolean(spaceAfterColon));
    }

//    // alignment
//    public boolean alignMultilineMethodParams() {
//        return preferences.getBoolean(alignMultilineMethodParams, provider.getDefaultAsBoolean(alignMultilineMethodParams));
//    }
//
//    public boolean alignMultilineCallArgs() {
//        return preferences.getBoolean(alignMultilineCallArgs, provider.getDefaultAsBoolean(alignMultilineCallArgs));
//    }
//
//    public boolean alignMultilineImplements() {
//        return preferences.getBoolean(alignMultilineImplements, provider.getDefaultAsBoolean(alignMultilineImplements));
//    }
//
//    public boolean alignMultilineParenthesized() {
//        return preferences.getBoolean(alignMultilineParenthesized, provider.getDefaultAsBoolean(alignMultilineParenthesized));
//    }
//
//    public boolean alignMultilineBinaryOp() {
//        return preferences.getBoolean(alignMultilineBinaryOp, provider.getDefaultAsBoolean(alignMultilineBinaryOp));
//    }
//
//    public boolean alignMultilineTernaryOp() {
//        return preferences.getBoolean(alignMultilineTernaryOp, provider.getDefaultAsBoolean(alignMultilineTernaryOp));
//    }
//
//    public boolean alignMultilineAssignment() {
//        return preferences.getBoolean(alignMultilineAssignment, provider.getDefaultAsBoolean(alignMultilineAssignment));
//    }
//
//    public boolean alignMultilineFor() {
//        return preferences.getBoolean(alignMultilineFor, provider.getDefaultAsBoolean(alignMultilineFor));
//    }
//
//    public boolean alignMultilineArrayInit() {
//        return preferences.getBoolean(alignMultilineArrayInit, provider.getDefaultAsBoolean(alignMultilineArrayInit));
//    }

    public boolean placeElseOnNewLine() {
        return preferences.getBoolean(placeElseOnNewLine, provider.getDefaultAsBoolean(placeElseOnNewLine));
    }

    public boolean placeWhileOnNewLine() {
        return preferences.getBoolean(placeWhileOnNewLine, provider.getDefaultAsBoolean(placeWhileOnNewLine));
    }

    public boolean placeCatchOnNewLine() {
        return preferences.getBoolean(placeCatchOnNewLine, provider.getDefaultAsBoolean(placeCatchOnNewLine));
    }

    public boolean placeFinallyOnNewLine() {
        return preferences.getBoolean(placeFinallyOnNewLine, provider.getDefaultAsBoolean(placeFinallyOnNewLine));
    }

//    public boolean groupMulitlineAssignment() {
//        return preferences.getBoolean(groupAlignmentAssignment, provider.getDefaultAsBoolean(groupAlignmentAssignment));
//    }
//
//    public boolean groupMulitlineArrayInit() {
//        return preferences.getBoolean(groupAlignmentArrayInit, provider.getDefaultAsBoolean(groupAlignmentArrayInit));
//    }

    // Wrapping ----------------------------------------------------------------

    public WrapStyle wrapStatement() {
        String wrap = preferences.get(wrapStatement, provider.getDefaultAsString(wrapStatement));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapVariables() {
        String wrap = preferences.get(wrapVariables, provider.getDefaultAsString(wrapVariables));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapMethodParams() {
        String wrap = preferences.get(wrapMethodParams, provider.getDefaultAsString(wrapMethodParams));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapMethodCallArgs() {
        String wrap = preferences.get(wrapMethodCallArgs, provider.getDefaultAsString(wrapMethodCallArgs));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapChainedMethodCalls() {
        String wrap = preferences.get(wrapChainedMethodCalls, provider.getDefaultAsString(wrapChainedMethodCalls));
        return WrapStyle.valueOf(wrap);
    }

    public boolean wrapAfterDotInChainedMethodCalls() {
        return preferences.getBoolean(wrapAfterDotInChainedMethodCalls, provider.getDefaultAsBoolean(wrapAfterDotInChainedMethodCalls));
    }

    public WrapStyle wrapArrayInit() {
        String wrap = preferences.get(wrapArrayInit, provider.getDefaultAsString(wrapArrayInit));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapArrayInitItems() {
        String wrap = preferences.get(wrapArrayInitItems, provider.getDefaultAsString(wrapArrayInitItems));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapFor() {
        String wrap = preferences.get(wrapFor, provider.getDefaultAsString(wrapFor));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapForStatement() {
        String wrap = preferences.get(wrapForStatement, provider.getDefaultAsString(wrapForStatement));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapIfStatement() {
        String wrap = preferences.get(wrapIfStatement, provider.getDefaultAsString(wrapIfStatement));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapWhileStatement() {
        String wrap = preferences.get(wrapWhileStatement, provider.getDefaultAsString(wrapWhileStatement));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapDoWhileStatement() {
        String wrap = preferences.get(wrapDoWhileStatement, provider.getDefaultAsString(wrapDoWhileStatement));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapWithStatement() {
        String wrap = preferences.get(wrapWithStatement, provider.getDefaultAsString(wrapWithStatement));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapBinaryOps() {
        String wrap = preferences.get(wrapBinaryOps, provider.getDefaultAsString(wrapBinaryOps));
        return WrapStyle.valueOf(wrap);
    }

    public boolean wrapAfterBinaryOps() {
        return preferences.getBoolean(wrapAfterBinaryOps, provider.getDefaultAsBoolean(wrapAfterBinaryOps));
    }
    
    public WrapStyle wrapTernaryOps() {
        String wrap = preferences.get(wrapTernaryOps, provider.getDefaultAsString(wrapTernaryOps));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapAssignOps() {
        String wrap = preferences.get(wrapAssignOps, provider.getDefaultAsString(wrapAssignOps));
        return WrapStyle.valueOf(wrap);
    }

    public boolean wrapAfterTernaryOps() {
        return preferences.getBoolean(wrapAfterTernaryOps, provider.getDefaultAsBoolean(wrapAfterTernaryOps));
    }

    public boolean wrapBlockBrace() {
        return preferences.getBoolean(wrapBlockBraces, provider.getDefaultAsBoolean(wrapBlockBraces));
    }

    public boolean wrapStatementsOnTheSameLine() {
        return preferences.getBoolean(wrapStatementsOnTheLine, provider.getDefaultAsBoolean(wrapStatementsOnTheLine));
    }

    public WrapStyle wrapObjects() {
        String wrap = preferences.get(wrapObjects, provider.getDefaultAsString(wrapObjects));
        return WrapStyle.valueOf(wrap);
    }

    public WrapStyle wrapProperties() {
        String wrap = preferences.get(wrapProperties, provider.getDefaultAsString(wrapProperties));
        return WrapStyle.valueOf(wrap);
    }

    // Uses

//    public boolean preferFullyQualifiedNames() {
//        return preferences.getBoolean(preferFullyQualifiedNames, provider.getDefaultAsBoolean(preferFullyQualifiedNames));
//    }
//
//    public boolean preferMultipleUseStatementsCombined() {
//        return preferences.getBoolean(preferMultipleUseStatementsCombined, provider.getDefaultAsBoolean(preferMultipleUseStatementsCombined));
//    }
//
//    public boolean startUseWithNamespaceSeparator() {
//        return preferences.getBoolean(startUseWithNamespaceSeparator, provider.getDefaultAsBoolean(startUseWithNamespaceSeparator));
//    }

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

    final static class Holder {

        final boolean expandTabsToSpaces;

        final int tabSize;

        final int indentSize;

        final int continuationIndentSize;

        final int itemsInArrayDeclarationIndentSize;

        final int initialIndent;

        final boolean reformatComments;

        final boolean indentHtml;

        final int rightMargin;

        final CodeStyle.BracePlacement functionDeclBracePlacement;

        final CodeStyle.BracePlacement ifBracePlacement;

        final CodeStyle.BracePlacement forBracePlacement;

        final CodeStyle.BracePlacement whileBracePlacement;

        final CodeStyle.BracePlacement switchBracePlacement;

        final CodeStyle.BracePlacement catchBracePlacement;

        final CodeStyle.BracePlacement withBracePlacement;

        final boolean objectLiteralContinuation;

        final boolean spaceBeforeWhile;

        final boolean spaceBeforeElse;

        final boolean spaceBeforeCatch;

        final boolean spaceBeforeFinally;

        final boolean spaceBeforeAnonMethodDeclParen;
        
        final boolean spaceBeforeMethodDeclParen;

        final boolean spaceBeforeMethodCallParen;

        final boolean spaceBeforeIfParen;

        final boolean spaceBeforeForParen;

        final boolean spaceBeforeWhileParen;

        final boolean spaceBeforeCatchParen;

        final boolean spaceBeforeSwitchParen;

        final boolean spaceBeforeWithParen;

        final boolean spaceAroundUnaryOps;

        final boolean spaceAroundBinaryOps;

        final boolean spaceAroundStringConcatOps;

        final boolean spaceAroundTernaryOps;

        final boolean spaceAroundKeyValueOps;

        final boolean spaceAroundAssignOps;

        final boolean spaceAroundObjectOps;

        final boolean spaceBeforeClassDeclLeftBrace;

        final boolean spaceBeforeMethodDeclLeftBrace;

        final boolean spaceBeforeIfLeftBrace;

        final boolean spaceBeforeElseLeftBrace;

        final boolean spaceBeforeWhileLeftBrace;

        final boolean spaceBeforeForLeftBrace;

        final boolean spaceBeforeDoLeftBrace;

        final boolean spaceBeforeSwitchLeftBrace;

        final boolean spaceBeforeTryLeftBrace;

        final boolean spaceBeforeCatchLeftBrace;

        final boolean spaceBeforeFinallyLeftBrace;

        final boolean spaceBeforeWithLeftBrace;

        final boolean spaceWithinParens;

        final boolean spaceWithinMethodDeclParens;

        final boolean spaceWithinMethodCallParens;

        final boolean spaceWithinIfParens;

        final boolean spaceWithinForParens;

        final boolean spaceWithinWhileParens;

        final boolean spaceWithinSwitchParens;

        final boolean spaceWithinCatchParens;

        final boolean spaceWithinWithParens;

        final boolean spaceWithinTypeCastParens;

        final boolean spaceWithinArrayDeclParens;

        final boolean spaceWithinBraces;

        final boolean spaceWithinArrayBrackets;

        final boolean spaceBeforeComma;

        final boolean spaceAfterComma;

        final boolean spaceBeforeSemi;

        final boolean spaceAfterSemi;

        final boolean spaceBeforeColon;

        final boolean spaceAfterColon;

        final CodeStyle.WrapStyle wrapStatement;

        final CodeStyle.WrapStyle wrapVariables;

        final CodeStyle.WrapStyle wrapMethodParams;

        final CodeStyle.WrapStyle wrapMethodCallArgs;

        final CodeStyle.WrapStyle wrapChainedMethodCalls;

        final boolean wrapAfterDotInChainedMethodCalls;

        final CodeStyle.WrapStyle wrapArrayInit;

        final CodeStyle.WrapStyle wrapArrayInitItems;

        final CodeStyle.WrapStyle wrapFor;

        final CodeStyle.WrapStyle wrapForStatement;

        final CodeStyle.WrapStyle wrapIfStatement;

        final CodeStyle.WrapStyle wrapWhileStatement;

        final CodeStyle.WrapStyle wrapDoWhileStatement;

        final CodeStyle.WrapStyle wrapWithStatement;

        final CodeStyle.WrapStyle wrapBinaryOps;

        final boolean wrapAfterBinaryOps;

        final CodeStyle.WrapStyle wrapTernaryOps;

        final CodeStyle.WrapStyle wrapAssignOps;

        final boolean wrapAfterTernaryOps;

        final boolean wrapBlockBrace;

        final boolean wrapStatementsOnTheSameLine;

        final CodeStyle.WrapStyle wrapObjects;

        final CodeStyle.WrapStyle wrapProperties;

        final boolean placeElseOnNewLine;
        final boolean placeWhileOnNewLine;
        final boolean placeCatchOnNewLine;
        final boolean placeFinallyOnNewLine;

        private Holder(CodeStyle style) {
            expandTabsToSpaces = style.expandTabToSpaces();
            tabSize = style.getTabSize();
            indentSize = style.getIndentSize();
            continuationIndentSize = style.getContinuationIndentSize();
            itemsInArrayDeclarationIndentSize = style.getItemsInArrayDeclarationIndentSize();
            initialIndent = style.getInitialIndent();
            reformatComments = style.reformatComments();
            indentHtml = style.indentHtml();
            rightMargin = style.getRightMargin();

            functionDeclBracePlacement = style.getFunctionDeclBracePlacement();
            ifBracePlacement = style.getIfBracePlacement();
            forBracePlacement = style.getForBracePlacement();
            whileBracePlacement = style.getWhileBracePlacement();
            switchBracePlacement = style.getSwitchBracePlacement();
            catchBracePlacement = style.getCatchBracePlacement();
            withBracePlacement = style.getWithBracePlacement();
            objectLiteralContinuation = style.continuationBeforeObjectLiteral();

            spaceBeforeWhile = style.spaceBeforeWhile();
            spaceBeforeElse = style.spaceBeforeElse();
            spaceBeforeCatch = style.spaceBeforeCatch();
            spaceBeforeFinally = style.spaceBeforeFinally();
            spaceBeforeAnonMethodDeclParen = style.spaceBeforeAnonMethodDeclParen();
            spaceBeforeMethodDeclParen = style.spaceBeforeMethodDeclParen();
            spaceBeforeMethodCallParen = style.spaceBeforeMethodCallParen();
            spaceBeforeIfParen = style.spaceBeforeIfParen();
            spaceBeforeForParen = style.spaceBeforeForParen();
            spaceBeforeWhileParen = style.spaceBeforeWhileParen();
            spaceBeforeCatchParen = style.spaceBeforeCatchParen();
            spaceBeforeSwitchParen = style.spaceBeforeSwitchParen();
            spaceBeforeWithParen = style.spaceBeforeWithParen();
            spaceAroundUnaryOps = style.spaceAroundUnaryOps();
            spaceAroundBinaryOps = style.spaceAroundBinaryOps();
            spaceAroundStringConcatOps = style.spaceAroundStringConcatOps();
            spaceAroundTernaryOps = style.spaceAroundTernaryOps();
            spaceAroundKeyValueOps = style.spaceAroundKeyValueOps();
            spaceAroundAssignOps = style.spaceAroundAssignOps();
            spaceAroundObjectOps = style.spaceAroundObjectOps();
            spaceBeforeClassDeclLeftBrace = style.spaceBeforeClassDeclLeftBrace();
            spaceBeforeMethodDeclLeftBrace = style.spaceBeforeMethodDeclLeftBrace();
            spaceBeforeIfLeftBrace = style.spaceBeforeIfLeftBrace();
            spaceBeforeElseLeftBrace = style.spaceBeforeElseLeftBrace();
            spaceBeforeWhileLeftBrace = style.spaceBeforeWhileLeftBrace();
            spaceBeforeForLeftBrace = style.spaceBeforeForLeftBrace();
            spaceBeforeDoLeftBrace = style.spaceBeforeDoLeftBrace();
            spaceBeforeSwitchLeftBrace = style.spaceBeforeSwitchLeftBrace();
            spaceBeforeTryLeftBrace = style.spaceBeforeTryLeftBrace();
            spaceBeforeCatchLeftBrace = style.spaceBeforeCatchLeftBrace();
            spaceBeforeFinallyLeftBrace = style.spaceBeforeFinallyLeftBrace();
            spaceBeforeWithLeftBrace = style.spaceBeforeWithLeftBrace();
            spaceWithinParens = style.spaceWithinParens();
            spaceWithinMethodDeclParens = style.spaceWithinMethodDeclParens();
            spaceWithinMethodCallParens = style.spaceWithinMethodCallParens();
            spaceWithinIfParens = style.spaceWithinIfParens();
            spaceWithinForParens = style.spaceWithinForParens();
            spaceWithinWhileParens = style.spaceWithinWhileParens();
            spaceWithinSwitchParens = style.spaceWithinSwitchParens();
            spaceWithinCatchParens = style.spaceWithinCatchParens();
            spaceWithinWithParens = style.spaceWithinWithParens();
            spaceWithinTypeCastParens = style.spaceWithinTypeCastParens();
            spaceWithinArrayDeclParens = style.spaceWithinArrayDeclParens();
            spaceWithinBraces = style.spaceWithinBraces();
            spaceWithinArrayBrackets = style.spaceWithinArrayBrackets();
            spaceBeforeComma = style.spaceBeforeComma();
            spaceAfterComma = style.spaceAfterComma();
            spaceBeforeSemi = style.spaceBeforeSemi();
            spaceAfterSemi = style.spaceAfterSemi();
            spaceBeforeColon = style.spaceBeforeColon();
            spaceAfterColon = style.spaceAfterColon();

            wrapStatement = style.wrapStatement();
            wrapVariables = style.wrapVariables();
            wrapMethodParams = style.wrapMethodParams();
            wrapMethodCallArgs = style.wrapMethodCallArgs();
            wrapChainedMethodCalls = style.wrapChainedMethodCalls();
            wrapAfterDotInChainedMethodCalls = style.wrapAfterDotInChainedMethodCalls();
            wrapArrayInit = style.wrapArrayInit();
            wrapArrayInitItems = style.wrapArrayInitItems();
            wrapFor = style.wrapFor();
            wrapForStatement = style.wrapForStatement();
            wrapIfStatement = style.wrapIfStatement();
            wrapWhileStatement = style.wrapWhileStatement();
            wrapDoWhileStatement = style.wrapDoWhileStatement();
            wrapWithStatement = style.wrapWithStatement();
            wrapBinaryOps = style.wrapBinaryOps();
            wrapAfterBinaryOps = style.wrapAfterBinaryOps();
            wrapTernaryOps = style.wrapTernaryOps();
            wrapAssignOps = style.wrapAssignOps();
            wrapAfterTernaryOps = style.wrapAfterTernaryOps();
            wrapBlockBrace = style.wrapBlockBrace();
            wrapStatementsOnTheSameLine = style.wrapStatementsOnTheSameLine();
            wrapObjects = style.wrapObjects();
            wrapProperties = style.wrapProperties();

            placeElseOnNewLine = style.placeElseOnNewLine();
            placeWhileOnNewLine = style.placeWhileOnNewLine();
            placeCatchOnNewLine = style.placeCatchOnNewLine();
            placeFinallyOnNewLine = style.placeFinallyOnNewLine();
        }
    }
}
