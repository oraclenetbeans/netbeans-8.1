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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.indent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.spi.GsfUtilities;
import org.netbeans.modules.php.editor.indent.FormatToken.AssignmentAnchorToken;
import org.netbeans.modules.php.editor.indent.TokenFormatter.DocumentOptions;
import org.netbeans.modules.php.editor.lexer.LexUtilities;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.editor.parser.astnodes.ASTError;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayCreation;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayElement;
import org.netbeans.modules.php.editor.parser.astnodes.Assignment;
import org.netbeans.modules.php.editor.parser.astnodes.Block;
import org.netbeans.modules.php.editor.parser.astnodes.CastExpression;
import org.netbeans.modules.php.editor.parser.astnodes.CatchClause;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ClassInstanceCreation;
import org.netbeans.modules.php.editor.parser.astnodes.ConditionalExpression;
import org.netbeans.modules.php.editor.parser.astnodes.ConstantDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.DoStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.ExpressionStatement;
import org.netbeans.modules.php.editor.parser.astnodes.FieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.FieldsDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.FinallyClause;
import org.netbeans.modules.php.editor.parser.astnodes.ForEachStatement;
import org.netbeans.modules.php.editor.parser.astnodes.ForStatement;
import org.netbeans.modules.php.editor.parser.astnodes.FormalParameter;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.IfStatement;
import org.netbeans.modules.php.editor.parser.astnodes.InfixExpression;
import org.netbeans.modules.php.editor.parser.astnodes.InterfaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.LambdaFunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.netbeans.modules.php.editor.parser.astnodes.ReturnStatement;
import org.netbeans.modules.php.editor.parser.astnodes.SingleFieldDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Statement;
import org.netbeans.modules.php.editor.parser.astnodes.StaticFieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.StaticMethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.StaticStatement;
import org.netbeans.modules.php.editor.parser.astnodes.SwitchCase;
import org.netbeans.modules.php.editor.parser.astnodes.SwitchStatement;
import org.netbeans.modules.php.editor.parser.astnodes.TraitConflictResolutionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TraitDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TraitMethodAliasDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TryStatement;
import org.netbeans.modules.php.editor.parser.astnodes.UseStatement;
import org.netbeans.modules.php.editor.parser.astnodes.UseStatementPart;
import org.netbeans.modules.php.editor.parser.astnodes.UseTraitStatement;
import org.netbeans.modules.php.editor.parser.astnodes.UseTraitStatementPart;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.netbeans.modules.php.editor.parser.astnodes.VariableBase;
import org.netbeans.modules.php.editor.parser.astnodes.WhileStatement;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.util.Exceptions;

/**
 *
 * @author Petr Pisl
 */
public class FormatVisitor extends DefaultVisitor {

    private static final Logger LOGGER = Logger.getLogger(FormatVisitor.class.getName());
    private final BaseDocument document;
    private final List<FormatToken> formatTokens;
    private final TokenSequence<PHPTokenId> ts;
    private final LinkedList<ASTNode> path;
    private final DocumentOptions options;
    private final Stack<GroupAlignmentTokenHolder> groupAlignmentTokenHolders;
    private final int caretOffset;
    private final int startOffset;
    private final int endOffset;
    private boolean includeWSBeforePHPDoc;
    private boolean isCurly; // whether the last visited block is curly or standard syntax.
    private boolean isMethodInvocationShifted; // is continual indentation already included ?
    private boolean isFirstUseStatementPart;
    private boolean isFirstUseTraitStatementPart;
    private boolean inArray;

    public FormatVisitor(BaseDocument document, DocumentOptions documentOptions, final int caretOffset, final int startOffset, final int endOffset) {
        this.document = document;
        ts = LexUtilities.getPHPTokenSequence(document, 0);
        path = new LinkedList<>();
        options = documentOptions;
        includeWSBeforePHPDoc = true;
        formatTokens = new ArrayList<>(ts == null ? 1 : ts.tokenCount() * 2);
        this.caretOffset = caretOffset;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        formatTokens.add(new FormatToken.InitToken());
        isMethodInvocationShifted = false;
        groupAlignmentTokenHolders = new Stack<>();
    }

    public List<FormatToken> getFormatTokens() {
        return formatTokens;
    }

    @Override
    public void scan(ASTNode node) {
        if (node == null) {
            return;
        }

        // find comment before the node.
        List<FormatToken> beforeTokens = new ArrayList<>(30);
        int indexBeforeLastComment = -1;  // remember last comment
        while (moveNext() && ts.offset() < node.getStartOffset()
                && lastIndex < ts.index()
                && ((ts.offset() + ts.token().length()) <= node.getStartOffset()
                || ts.token().id() == PHPTokenId.PHP_CLOSETAG)) {
            if (ts.token().id() == PHPTokenId.PHP_CURLY_CLOSE
                    && path.size() > 1 && path.get(1) instanceof NamespaceDeclaration) {
                // this a a fix for probalem that namespace declaration through {}, doesn't end with the end of  }
                formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.indentSize));
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_OTHER_RIGHT_BRACE, ts.offset()));
            }
            addFormatToken(beforeTokens);
            if (ts.token().id() == PHPTokenId.PHPDOC_COMMENT_START
                    || (ts.token().id() == PHPTokenId.PHP_LINE_COMMENT
                    && "//".equals(ts.token().text().toString()))
                    && indexBeforeLastComment == -1) {
                if (ts.movePrevious() && ts.token().id() == PHPTokenId.WHITESPACE && countOfNewLines(ts.token().text()) > 0) {
                    // don't change if the line comment or a comment starts on the same line
                    indexBeforeLastComment = beforeTokens.size() - 1;
                }
                ts.moveNext();
            }
        }
        includeWSBeforePHPDoc = true;
        if (indexBeforeLastComment > 0) { // if there is a comment, put the new lines befere the comment, not directly before the node.
            for (int i = 0; i < indexBeforeLastComment; i++) {
                formatTokens.add(beforeTokens.get(i));
            }
            if (node instanceof ClassDeclaration || node instanceof InterfaceDeclaration || node instanceof TraitDeclaration) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CLASS, ts.offset()));
                includeWSBeforePHPDoc = false;
            } else if (node instanceof FunctionDeclaration || node instanceof MethodDeclaration) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FUNCTION, ts.offset()));
                includeWSBeforePHPDoc = false;
            } else if (node instanceof FieldsDeclaration || node instanceof ConstantDeclaration) {
                if (isPreviousNodeTheSameInBlock(path.get(0), (Statement) node)) {
                    formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BETWEEN_FIELDS, ts.offset()));
                } else {
                    formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FIELDS, ts.offset()));
                }
                includeWSBeforePHPDoc = false;
            } else if (node instanceof UseStatement) {
                if (isPreviousNodeTheSameInBlock(path.get(0), (Statement) node)) {
                    formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BETWEEN_USE, ts.offset()));
                } else {
                    formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_USE, ts.offset()));
                }
                includeWSBeforePHPDoc = false;
            } else if (node instanceof UseTraitStatement) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_USE_TRAIT, ts.offset()));
                includeWSBeforePHPDoc = false;
            }
            for (int i = indexBeforeLastComment; i < beforeTokens.size(); i++) {
                formatTokens.add(beforeTokens.get(i));
            }
        } else {
            formatTokens.addAll(beforeTokens);
        }

        ts.movePrevious();

        path.addFirst(node);
        super.scan(node);
        path.removeFirst();

        while (moveNext()
                && lastIndex < ts.index()
                && (ts.offset() + ts.token().length()) <= node.getEndOffset()) {
            addFormatToken(formatTokens);
        }
        ts.movePrevious();
    }

    @Override
    public void scan(Iterable<? extends ASTNode> nodes) {
        super.scan(nodes);
    }

    @Override
    public void visit(StaticStatement node) {
        List<Expression> expressions = node.getExpressions();
        for (Expression expression : expressions) {
            addAllUntilOffset(expression.getStartOffset());
            if (moveNext() && lastIndex < ts.index()) {
                addFormatToken(formatTokens); // add the first token of the expression and then add the indentation
                formatTokens.add(new FormatToken.IndentToken(ts.offset() + ts.token().length(), options.continualIndentSize));
                scan(expression);
                formatTokens.add(new FormatToken.IndentToken(expression.getEndOffset(), -1 * options.continualIndentSize));
            }
        }
    }

    @Override
    public void visit(ArrayCreation node) {
        inArray = true;
        int delta = options.indentArrayItems - options.continualIndentSize;
        if (ts.token().id() != PHPTokenId.PHP_ARRAY && lastIndex <= ts.index() // it's possible that the expression starts with array
                && !ts.token().text().toString().equals("[")) {  //NOI18N
            while (ts.moveNext() && (ts.token().id() != PHPTokenId.PHP_ARRAY && !ts.token().text().toString().equals("[")) && lastIndex < ts.index()) { //NOI18N
                addFormatToken(formatTokens);
            }
            if (formatTokens.get(formatTokens.size() - 1).getId() == FormatToken.Kind.WHITESPACE_INDENT
                    || path.get(1) instanceof ArrayElement
                    || path.get(1) instanceof FormalParameter
                    || path.get(1) instanceof CastExpression) {
                // when the array is on the beginning of the line, indent items in normal way
                delta = options.indentArrayItems;
            }
            delta = modifyDeltaForEnclosingFunctionInvocations(delta);
            if (path.get(1) instanceof FunctionInvocation && ((FunctionInvocation) path.get(1)).getParameters().size() == 1) {
                int hindex = formatTokens.size() - 1;
                while (hindex > 0 && formatTokens.get(hindex).getId() != FormatToken.Kind.TEXT
                        && formatTokens.get(hindex).getId() != FormatToken.Kind.WHITESPACE_INDENT
                        && lastIndex < ts.index()) {
                    hindex--;
                }
                if (hindex > 0 && formatTokens.get(hindex).getId() == FormatToken.Kind.WHITESPACE_INDENT) {
                    delta = options.indentArrayItems;
                }
            }
            if (ts.token().text().toString().equals("[")) { //NOI18N
                formatTokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
            } else if (lastIndex < ts.index()) {
                addFormatToken(formatTokens); // add array keyword
            }
        }
        formatTokens.add(new FormatToken.IndentToken(ts.offset(), delta));
        createGroupAlignment();
        List<ArrayElement> arrayElements = node.getElements();
        if (arrayElements != null && arrayElements.size() > 0) {
            ArrayElement arrayElement = arrayElements.get(0);
            addAllUntilOffset(arrayElement.getStartOffset());
            scan(arrayElement);
            for (int i = 1; i < arrayElements.size(); i++) {
                arrayElement = arrayElements.get(i);
                addAllUntilOffset(arrayElement.getStartOffset());
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_IN_ARRAY_ELEMENT_LIST, ts.offset() + ts.token().length()));
                scan(arrayElement);
            }
        }
        formatTokens.add(new FormatToken.IndentToken(ts.offset() + ts.token().length(), -1 * delta));
        addAllUntilOffset(node.getEndOffset());
        resetGroupAlignment();
        inArray = false;
    }

    private int modifyDeltaForEnclosingFunctionInvocations(int delta) {
        int depthInFunctionInvocation = 0;
        for (int i = 1; i < path.size(); i++) {
            if (path.get(i) instanceof FunctionInvocation) {
                depthInFunctionInvocation++;
            } else {
                break;
            }
        }
        // move indenting left for every enclosing function invocation
        return depthInFunctionInvocation > 1 ? delta + (-1 * options.continualIndentSize * (depthInFunctionInvocation - 1)) : delta;
    }

    @Override
    public void visit(ArrayElement node) {
        boolean multilinedArray = isMultilinedNode(getParentArrayCreation());
        if (node.getKey() != null && node.getValue() != null) {
            scan(node.getKey());
            while (ts.moveNext() && ts.offset() < node.getValue().getStartOffset()) {
                if (isKeyValueOperator(ts.token())) {
                    handleGroupAlignment(node.getKey(), multilinedArray);
                }
                addFormatToken(formatTokens);
            }
            ts.movePrevious();
            scan(node.getValue());
        } else {
            super.visit(node);
        }
    }

    private boolean isMultilinedNode(ASTNode node) {
        boolean result = false;
        try {
            result = document.getText(node.getStartOffset(), node.getEndOffset() - node.getStartOffset()).contains("\n"); //NOI18N
        } catch (BadLocationException ex) {
            LOGGER.log(Level.FINE, null, ex);
        }
        return result;
    }

    private ArrayCreation getParentArrayCreation() {
        ArrayCreation result = null;
        for (int i = 0; i < path.size(); i++) {
            ASTNode parentInPath = path.get(i);
            if (parentInPath instanceof ArrayCreation) {
                result = (ArrayCreation) parentInPath;
                break;
            }
        }
        return result;
    }

    private static boolean isKeyValueOperator(Token<PHPTokenId> token) {
        return token.id() == PHPTokenId.PHP_OPERATOR && "=>".equals(token.text().toString()); //NOI18N
    }

    @Override
    public void visit(Assignment node) {
        scan(node.getLeftHandSide());
        while (ts.moveNext() && ((ts.offset() + ts.token().length()) < node.getRightHandSide().getStartOffset())
                && ts.token().id() != PHPTokenId.PHP_TOKEN) {
            addFormatToken(formatTokens);
        }
        if (ts.token().id() == PHPTokenId.PHP_TOKEN) {
            if (path.size() > 1) {
                ASTNode parent = path.get(1);
                if (parent instanceof StaticStatement) {
                    VariableBase leftHandSide = node.getLeftHandSide();
                    if (leftHandSide instanceof Variable || leftHandSide instanceof FieldAccess) {
                        StaticStatement staticParent = (StaticStatement) parent;
                        handleGroupAlignment(leftHandSide.getEndOffset() - staticParent.getStartOffset());
                    }
                } else if (path.size() > 1 && !(parent instanceof ForStatement)) {
                    VariableBase leftHandSide = node.getLeftHandSide();
                    if (leftHandSide instanceof Variable || leftHandSide instanceof FieldAccess || leftHandSide instanceof StaticFieldAccess) {
                        handleGroupAlignment(leftHandSide);
                    }
                }
                addFormatToken(formatTokens);
            }
        } else {
            ts.movePrevious();
        }
        scan(node.getRightHandSide());
    }

    @Override
    public void visit(Block node) {
        resetAndCreateGroupAlignment(); // for every block reset group of alignment
        if (path.size() > 1 && (path.get(1) instanceof NamespaceDeclaration
                && !((NamespaceDeclaration) path.get(1)).isBracketed())) {
            // dont process blok for namespace
            super.visit(node);
            return;
        }

        isCurly = node.isCurly();
        // move ts in every case to the next token
        while (ts.moveNext() && node.isCurly() && ts.token().id() != PHPTokenId.PHP_CURLY_OPEN
                && (ts.offset() < node.getStartOffset())
                && lastIndex < ts.index()) {
            addFormatToken(formatTokens);
        }

        ASTNode parent = path.get(1);

        if (ts.token().id() == PHPTokenId.PHP_CURLY_OPEN) {
            if (parent instanceof ClassDeclaration || parent instanceof InterfaceDeclaration || parent instanceof TraitDeclaration) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CLASS_LEFT_BRACE, ts.offset()));
            } else if (parent instanceof FunctionDeclaration || parent instanceof MethodDeclaration) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FUNCTION_LEFT_BRACE, ts.offset()));
            } else if (parent instanceof IfStatement) {
                IfStatement ifStatement = (IfStatement) parent;
                if (ifStatement.getFalseStatement() != null
                        && ifStatement.getFalseStatement().getStartOffset() <= node.getStartOffset()) {
                    formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_ELSE_LEFT_BRACE, ts.offset()));
                } else {
                    formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_IF_LEFT_BRACE, ts.offset()));
                }
            } else if (parent instanceof ForStatement || parent instanceof ForEachStatement) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FOR_LEFT_BRACE, ts.offset()));
            } else if (parent instanceof WhileStatement) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_WHILE_LEFT_BRACE, ts.offset()));
            } else if (parent instanceof DoStatement) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_DO_LEFT_BRACE, ts.offset()));
            } else if (parent instanceof SwitchStatement) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_SWITCH_LEFT_BACE, ts.offset()));
            } else if (parent instanceof TryStatement) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_TRY_LEFT_BRACE, ts.offset()));
            } else if (parent instanceof CatchClause) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CATCH_LEFT_BRACE, ts.offset()));
            } else if (parent instanceof FinallyClause) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FINALLY_LEFT_BRACE, ts.offset()));
            } else if (parent instanceof UseTraitStatement) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_USE_TRAIT_BODY_LEFT_BRACE, ts.offset()));
            } else {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_OTHER_LEFT_BRACE, ts.offset()));
            }
            addFormatToken(formatTokens);

            boolean indentationIncluded = false;
            while (ts.moveNext() && (ts.token().id() == PHPTokenId.WHITESPACE && countOfNewLines(ts.token().text()) == 0)
                    || isComment(ts.token())) {
                if (ts.token().id() == PHPTokenId.PHP_LINE_COMMENT && !indentationIncluded) {
                    formatTokens.add(new FormatToken.IndentToken(ts.offset(), options.indentSize));
                    indentationIncluded = true;
                }
                addFormatToken(formatTokens);
            }

            if (!indentationIncluded) {
                formatTokens.add(new FormatToken.IndentToken(ts.offset(), options.indentSize));
            }

            if (parent instanceof ClassDeclaration || parent instanceof InterfaceDeclaration || parent instanceof TraitDeclaration) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_CLASS_LEFT_BRACE, ts.offset()));
            } else {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_OTHER_LEFT_BRACE, ts.offset()));
            }

        }

        ts.movePrevious();


        super.visit(node);

        if (node.isCurly() && (ts.offset() + ts.token().length()) <= node.getEndOffset()) {
            while (ts.moveNext() && (ts.offset() + ts.token().length()) <= node.getEndOffset()) {
                if (ts.token().id() == PHPTokenId.PHP_CURLY_CLOSE) {
                    FormatToken lastToken = formatTokens.get(formatTokens.size() - 1);

                    if (lastToken.getId() == FormatToken.Kind.WHITESPACE
                            || lastToken.getId() == FormatToken.Kind.WHITESPACE_INDENT) {
                        formatTokens.remove(formatTokens.size() - 1);
                        formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.indentSize));
                        formatTokens.add(lastToken);
                    } else {
                        formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.indentSize));
                    }

                    boolean includeWBC = false;  // is open after close ? {}
                    if (ts.movePrevious()
                            && (ts.token().id() == PHPTokenId.PHP_CURLY_OPEN
                            || ts.token().id() == PHPTokenId.WHITESPACE)) {
                        if (ts.token().id() == PHPTokenId.WHITESPACE) {
                            if (ts.movePrevious() && ts.token().id() == PHPTokenId.PHP_CURLY_OPEN) {
                                includeWBC = true;
                            }
                            ts.moveNext();
                        }
                        if (ts.token().id() == PHPTokenId.PHP_CURLY_OPEN) {
                            includeWBC = true;
                        }
                    }
                    ts.moveNext();
                    if (includeWBC) {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BETWEEN_OPEN_CLOSE_BRACES, ts.offset()));
                    }

                    if (parent instanceof ClassDeclaration || parent instanceof InterfaceDeclaration || parent instanceof TraitDeclaration) {
                        if (includeWSBeforePHPDoc) {
                            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CLASS_RIGHT_BRACE, ts.offset()));
                        }
                        addFormatToken(formatTokens);
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_CLASS, ts.offset() + ts.token().length()));
                    } else if (parent instanceof FunctionDeclaration || parent instanceof MethodDeclaration) {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FUNCTION_RIGHT_BRACE, ts.offset()));
                        addFormatToken(formatTokens);
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_FUNCTION, ts.offset() + ts.token().length()));
                    } else if (parent instanceof IfStatement) {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_IF_RIGHT_BRACE, ts.offset()));
                        addFormatToken(formatTokens);
                    } else if (parent instanceof ForStatement || parent instanceof ForEachStatement) {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FOR_RIGHT_BRACE, ts.offset()));
                        addFormatToken(formatTokens);
                    } else if (parent instanceof WhileStatement || parent instanceof DoStatement) {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_WHILE_RIGHT_BRACE, ts.offset()));
                        addFormatToken(formatTokens);
                    } else if (parent instanceof SwitchStatement) {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_SWITCH_RIGHT_BACE, ts.offset()));
                        addFormatToken(formatTokens);
                    } else if (parent instanceof CatchClause || parent instanceof TryStatement || parent instanceof FinallyClause) {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CATCH_RIGHT_BRACE, ts.offset()));
                        addFormatToken(formatTokens);
                    } else if (parent instanceof UseTraitStatement) {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_USE_TRAIT_BODY_RIGHT_BRACE, ts.offset()));
                        addFormatToken(formatTokens);
                    } else {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_OTHER_RIGHT_BRACE, ts.offset()));
                        addFormatToken(formatTokens);
                    }
                } else {
                    FormatToken lastToken = formatTokens.get(formatTokens.size() - 1);
                    if (!(lastToken.getId() == FormatToken.Kind.TEXT && lastToken.getOffset() >= ts.offset())
                            && lastIndex < ts.index()) {
                        addFormatToken(formatTokens);
                    }
                }
            }
            ts.movePrevious();
        }
        resetGroupAlignment(); //reset alignment when leaving a block
    }

    @Override
    public void visit(CastExpression node) {
        super.visit(node);
    }

    @Override
    public void visit(InterfaceDeclaration node) {
        addAllUntilOffset(node.getStartOffset());
        if (includeWSBeforePHPDoc) {
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CLASS, ts.offset()));
        } else {
            includeWSBeforePHPDoc = true;
        }
        super.visit(node);
    }

    @Override
    public void visit(ClassDeclaration node) {

        addAllUntilOffset(node.getStartOffset());
        if (includeWSBeforePHPDoc) {
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CLASS, ts.offset()));
        } else {
            includeWSBeforePHPDoc = true;
        }
        while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_CURLY_OPEN) {
            switch (ts.token().id()) {
                case PHP_CLASS:
                    if (!ClassDeclaration.Modifier.NONE.equals(node.getModifier())) {
                        FormatToken lastWhitespace = formatTokens.remove(formatTokens.size() - 1);
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_MODIFIERS, lastWhitespace.getOffset(), lastWhitespace.getOldText()));
                    }
                    addFormatToken(formatTokens);
                    break;
                case PHP_IMPLEMENTS:
                    if (node.getInterfaes().size() > 0) {
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_EXTENDS_IMPLEMENTS, ts.offset(), ts.token().text().toString()));
                        ts.movePrevious();
                        addListOfNodes(node.getInterfaes(), FormatToken.Kind.WHITESPACE_IN_INTERFACE_LIST);
                    }
                    break;
                case PHP_EXTENDS:
                    formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_EXTENDS_IMPLEMENTS, ts.offset(), ts.token().text().toString()));
                    addFormatToken(formatTokens);
                    break;
                default:
                    addFormatToken(formatTokens);
            }
        }

        ts.movePrevious();
        super.visit(node);
    }

    private void addListOfNodes(List<? extends ASTNode> nodes, FormatToken.Kind dividingToken) {
        addUnbreakalbeSequence(nodes.get(0), true);
        for (int i = 1; i < nodes.size(); i++) {
            if (ts.moveNext() && ts.token().id() == PHPTokenId.WHITESPACE) {
                addFormatToken(formatTokens);
            } else {
                ts.movePrevious();
            }
            formatTokens.add(new FormatToken(dividingToken, ts.offset() + ts.token().length()));
            addUnbreakalbeSequence(nodes.get(i), false);
        }
    }

    @Override
    public void visit(TraitDeclaration node) {
        addAllUntilOffset(node.getStartOffset());
        if (includeWSBeforePHPDoc) {
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CLASS, ts.offset()));
        } else {
            includeWSBeforePHPDoc = true;
        }
        while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_CURLY_OPEN) {
            addFormatToken(formatTokens);
        }
        ts.movePrevious();
        super.visit(node);
    }

    @Override
    public void visit(ClassInstanceCreation node) {
        scan(node.getClassName());
        if (node.ctorParams() != null && node.ctorParams().size() > 0) {
            boolean addIndentation = (path.size() > 2 && (path.get(1) instanceof ArrayElement) && (path.get(2) instanceof ArrayCreation));
            if (addIndentation) {
                formatTokens.add(new FormatToken.IndentToken(node.getClassName().getEndOffset(), options.continualIndentSize));
            }
            processArguments(node.ctorParams());
            if (addIndentation) {
                formatTokens.add(new FormatToken.IndentToken(node.ctorParams().get(node.ctorParams().size() - 1).getEndOffset(), -1 * options.continualIndentSize));
            }
            addAllUntilOffset(node.getEndOffset());
        } else {
            super.visit(node);
        }
    }

    private void processArguments(final List<Expression> arguments) {
        while (ts.moveNext() && ts.offset() < arguments.get(0).getStartOffset()
                && lastIndex < ts.index()) {
            addFormatToken(formatTokens);
        }
        ts.movePrevious();
        addListOfNodes(arguments, FormatToken.Kind.WHITESPACE_IN_ARGUMENT_LIST);
    }

    @Override
    public void visit(ConditionalExpression node) {
        scan(node.getCondition());
        boolean wrap = node.getIfTrue() != null ? true : false;
        ASTNode astNode = path.get(1);
        boolean putContinualIndent = !(astNode instanceof Assignment
                || astNode instanceof ReturnStatement);
        if (wrap) {
            while (ts.moveNext()
                    && !(ts.token().id() == PHPTokenId.PHP_TOKEN && "?".equals(ts.token().text().toString()))
                    && lastIndex < ts.index()) {
                addFormatToken(formatTokens);
            }

            int start = ts.offset();
            if (putContinualIndent) {
                formatTokens.add(new FormatToken.IndentToken(start, options.continualIndentSize));
            }
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_IN_TERNARY_OP, start));
            ts.movePrevious();
            addAllUntilOffset(node.getIfTrue().getStartOffset());
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset(), null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));

        }
        scan(node.getIfTrue());
        if (wrap) {
            addEndOfUnbreakableSequence(node.getIfTrue().getEndOffset());
            if (putContinualIndent) {
                formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.continualIndentSize));
            }
        }
        wrap = node.getIfFalse() != null ? true : false;
        if (wrap) {
            while (ts.moveNext()
                    && !(ts.token().id() == PHPTokenId.PHP_TOKEN && ":".equals(ts.token().text().toString()))
                    && lastIndex < ts.index()) {
                addFormatToken(formatTokens);
            }
            int start = ts.offset();
            if (putContinualIndent) {
                formatTokens.add(new FormatToken.IndentToken(start, options.continualIndentSize));
            }
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_IN_TERNARY_OP, start));
            ts.movePrevious();
            addAllUntilOffset(node.getIfFalse().getStartOffset());
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(start, null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));

        }
        scan(node.getIfFalse());
        if (wrap) {
            addEndOfUnbreakableSequence(node.getIfFalse().getEndOffset());
            if (putContinualIndent) {
                formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.continualIndentSize));
            }
        }
    }

    @Override
    public void visit(ConstantDeclaration node) {
        if (path.size() > 1 && path.get(1) instanceof Block) {
            Block block = (Block) path.get(1);
            int index = 0;
            List<Statement> statements = block.getStatements();
            while (index < statements.size() && statements.get(index).getStartOffset() < node.getStartOffset()) {
                index++;
            }
            addAllUntilOffset(node.getStartOffset());
            if (includeWSBeforePHPDoc && index < statements.size()
                    && index > 0 && statements.get(index - 1) instanceof ConstantDeclaration) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BETWEEN_FIELDS, ts.offset()));
            } else {
                if (includeWSBeforePHPDoc) {
                    formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FIELDS, ts.offset()));
                } else {
                    includeWSBeforePHPDoc = true;
                }
            }
            while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_STRING) {
                addFormatToken(formatTokens);
            }
            FormatToken lastWhitespace = formatTokens.remove(formatTokens.size() - 1);
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_MODIFIERS, lastWhitespace.getOffset(), lastWhitespace.getOldText()));
            addFormatToken(formatTokens);
            formatTokens.add(new FormatToken.IndentToken(node.getStartOffset(), options.continualIndentSize));
            scan(node.getNames());
            if (node.getNames().size() == 1) {
                while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_TOKEN) {
                    addFormatToken(formatTokens);
                }
                if (ts.token().id() == PHPTokenId.PHP_TOKEN) {
                    handleGroupAlignment(node.getNames().get(0));
                    addFormatToken(formatTokens);
                } else {
                    ts.movePrevious();
                }

            }
            scan(node.getInitializers());
            formatTokens.add(new FormatToken.IndentToken(node.getStartOffset(), options.continualIndentSize * -1));
            if (index == statements.size() - 1
                    || ((index < statements.size() - 1) && !(statements.get(index + 1) instanceof ConstantDeclaration))) {
                addRestOfLine();
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_FIELDS, ts.offset() + ts.token().length()));
            }
        } else {
            addAllUntilOffset(node.getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(node.getStartOffset(), options.continualIndentSize));
            super.visit(node);
            formatTokens.add(new FormatToken.IndentToken(node.getEndOffset(), options.continualIndentSize * -1));
        }
    }

    @Override
    public void visit(DoStatement node) {
        ASTNode body = node.getBody();
        if (body != null && !(body instanceof Block)) {
            addAllUntilOffset(body.getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(body.getStartOffset(), options.indentSize));
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_DO_STATEMENT, ts.offset()));
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset(), null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));
            scan(body);
            addEndOfUnbreakableSequence(body.getEndOffset());
            formatTokens.add(new FormatToken.IndentToken(body.getEndOffset(), -1 * options.indentSize));
        } else {
            scan(body);
        }
        scan(node.getCondition());
        addAllUntilOffset(node.getEndOffset());
    }

    @Override
    public void visit(ExpressionStatement node) {
        if (node.getExpression() instanceof FunctionInvocation) {
            super.visit(node);
        } else {
            addAllUntilOffset(node.getStartOffset());
            if (moveNext() && lastIndex < ts.index()) {
                addFormatToken(formatTokens); // add the first token of the expression and then add the indentation
                Expression expression = node.getExpression();
                boolean addIndent = !(expression instanceof MethodInvocation || expression instanceof StaticMethodInvocation);
                if (addIndent) {
                    formatTokens.add(new FormatToken.IndentToken(ts.offset() + ts.token().length(), options.continualIndentSize));
                    super.visit(node);
                    formatTokens.add(new FormatToken.IndentToken(node.getEndOffset(), -1 * options.continualIndentSize));
                } else {
                    super.visit(node);
                }

            }
        }
    }

    @Override
    public void visit(FieldsDeclaration node) {
        Block block = (Block) path.get(1);
        int index = 0;
        List<Statement> statements = block.getStatements();
        while (index < statements.size() && statements.get(index).getStartOffset() < node.getStartOffset()) {
            index++;
        }
        addAllUntilOffset(node.getStartOffset());
        if (includeWSBeforePHPDoc && index < statements.size()
                && index > 0 && statements.get(index - 1) instanceof FieldsDeclaration) {
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BETWEEN_FIELDS, ts.offset()));
        } else {
            if (includeWSBeforePHPDoc) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FIELDS, ts.offset()));
            } else {
                includeWSBeforePHPDoc = true;
            }
        }
        while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_VARIABLE) {
            addFormatToken(formatTokens);
        }
        ts.movePrevious();
        FormatToken lastWhitespace = formatTokens.remove(formatTokens.size() - 1);
        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_MODIFIERS, lastWhitespace.getOffset(), lastWhitespace.getOldText()));
        if (node.getFields().size() > 1) {
            formatTokens.add(new FormatToken.IndentToken(node.getStartOffset(), options.continualIndentSize));
            super.visit(node);
            formatTokens.add(new FormatToken.IndentToken(node.getStartOffset(), options.continualIndentSize * -1));
        } else {
            super.visit(node);
        }
        if (index == statements.size() - 1
                || ((index < statements.size() - 1) && !(statements.get(index + 1) instanceof FieldsDeclaration))) {
            addRestOfLine();
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_FIELDS, ts.offset() + ts.token().length()));
        }
    }

    @Override
    public void visit(ForEachStatement node) {
        scan(node.getExpression());
        boolean wrap = node.getKey() != null;
        if (wrap) {
            int start = node.getKey().getStartOffset();
            addAllUntilOffset(node.getKey().getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(start, options.continualIndentSize));
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_IN_FOR, start));
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(start, null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));
        }
        scan(node.getKey());
        if (wrap) {
            addEndOfUnbreakableSequence(node.getKey().getEndOffset());
            formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.continualIndentSize));
        }
        wrap = node.getValue() != null;
        if (wrap) {
            int start = node.getValue().getStartOffset();
            addAllUntilOffset(node.getValue().getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(start, options.continualIndentSize));
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_IN_FOR, start));
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(start, null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));
        }
        scan(node.getValue());
        if (wrap) {
            addEndOfUnbreakableSequence(node.getValue().getEndOffset());
            formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.continualIndentSize));
        }
        ASTNode body = node.getStatement();
        if (body != null && (body instanceof Block && !((Block) body).isCurly())) {
            addAllUntilOffset(body.getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(body.getStartOffset(), options.indentSize));
            scan(node.getStatement());
            while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_ENDFOREACH) {
                addFormatToken(formatTokens);
            }
            ts.movePrevious();
            formatTokens.add(new FormatToken.IndentToken(body.getEndOffset(), -1 * options.indentSize));
        } else if (body != null && !(body instanceof Block)) {
            addNoCurlyBody(body, FormatToken.Kind.WHITESPACE_BEFORE_FOR_STATEMENT);
        } else {
            scan(node.getStatement());
        }
    }

    @Override
    public void visit(ForStatement node) {
        scan(node.getInitializers());
        boolean wrap = node.getConditions() != null && node.getConditions().size() > 0 ? true : false;
        if (wrap) {
            int start = node.getConditions().get(0).getStartOffset();
            addAllUntilOffset(start);
            formatTokens.add(new FormatToken.IndentToken(start, options.continualIndentSize));
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_IN_FOR, start));
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(start, null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));
        }
        scan(node.getConditions());
        if (wrap) {
            addEndOfUnbreakableSequence(node.getConditions().get(node.getConditions().size() - 1).getEndOffset());
            formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.continualIndentSize));
        }
        wrap = node.getUpdaters() != null && node.getUpdaters().size() > 0 ? true : false;
        if (wrap) {
            int start = node.getUpdaters().get(0).getStartOffset();
            addAllUntilOffset(start);
            formatTokens.add(new FormatToken.IndentToken(start, options.continualIndentSize));
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_IN_FOR, start));
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(start, null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));
        }
        scan(node.getUpdaters());
        if (wrap) {
            addEndOfUnbreakableSequence(node.getUpdaters().get(node.getUpdaters().size() - 1).getEndOffset());
            formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.continualIndentSize));
        }
        ASTNode body = node.getBody();
        if (body != null && (body instanceof Block && !((Block) body).isCurly())) {
            addAllUntilOffset(body.getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(body.getStartOffset(), options.indentSize));
            scan(node.getBody());
            while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_ENDFOR) {
                addFormatToken(formatTokens);
            }
            ts.movePrevious();
            formatTokens.add(new FormatToken.IndentToken(body.getEndOffset(), -1 * options.indentSize));
        } else if (body != null && !(body instanceof Block)) {
            addNoCurlyBody(body, FormatToken.Kind.WHITESPACE_BEFORE_FOR_STATEMENT);
        } else {
            scan(node.getBody());
        }
    }

    @Override
    public void visit(FunctionDeclaration node) {
        if (!(path.size() > 1 && path.get(1) instanceof MethodDeclaration)) {
            while (ts.moveNext() && (ts.token().id() == PHPTokenId.WHITESPACE
                    || isComment(ts.token()))) {
                addFormatToken(formatTokens);
            }
            if (includeWSBeforePHPDoc) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FUNCTION, ts.offset()));
            } else {
                includeWSBeforePHPDoc = true;
            }
            ts.movePrevious();
        }
        scan(node.getFunctionName());
        List<FormalParameter> parameters = node.getFormalParameters();
        if (parameters != null && parameters.size() > 0) {
            while (ts.moveNext() && ts.offset() < parameters.get(0).getStartOffset()
                    && lastIndex < ts.index()) {
                addFormatToken(formatTokens);
            }
            ts.movePrevious();
            addListOfNodes(parameters, FormatToken.Kind.WHITESPACE_IN_PARAMETER_LIST);
        }
        scan(node.getBody());
    }

    @Override
    public void visit(LambdaFunctionDeclaration node) {
        scan(node.getFormalParameters());
        scan(node.getLexicalVariables());
        Block body = node.getBody();
        if (body != null) {
            addAllUntilOffset(body.getStartOffset());
            if (!inArray) {
                formatTokens.add(new FormatToken.IndentToken(body.getStartOffset(), -1 * options.continualIndentSize));
            }
            scan(body);
            if (!inArray) {
                formatTokens.add(new FormatToken.IndentToken(body.getEndOffset(), options.continualIndentSize));
            }
        }
    }

    @Override
    public void visit(FunctionInvocation node) {
        if (path.size() > 1 && path.get(1) instanceof MethodInvocation) {
            while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_OBJECT_OPERATOR
                    && ((ts.offset() + ts.token().length()) < node.getStartOffset())) {
                addFormatToken(formatTokens);
            }
            if (ts.token().id() == PHPTokenId.PHP_OBJECT_OPERATOR) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_IN_CHAINED_METHOD_CALLS, ts.offset()));
                addFormatToken(formatTokens);
            } else {
                ts.movePrevious();
            }

        }
        scan(node.getFunctionName());
        List<Expression> parameters = node.getParameters();
        if (parameters != null && parameters.size() > 0) {
            boolean addIndentation = !(path.get(1) instanceof ReturnStatement
                    || path.get(1) instanceof Assignment
                    || (path.size() > 2 && path.get(1) instanceof MethodInvocation && path.get(2) instanceof Assignment));
            if (addIndentation) {
                formatTokens.add(new FormatToken.IndentToken(node.getFunctionName().getEndOffset(), options.continualIndentSize));
            }
            processArguments(parameters);
            if (addIndentation) {
                List<FormatToken> removed = new ArrayList<>();
                FormatToken ftoken = formatTokens.get(formatTokens.size() - 1);
                while (ftoken.getId() == FormatToken.Kind.UNBREAKABLE_SEQUENCE_END
                        || (ftoken.isWhitespace() && ftoken.getId() != FormatToken.Kind.WHITESPACE_INDENT)
                        || ftoken.getId() == FormatToken.Kind.COMMENT
                        || ftoken.getId() == FormatToken.Kind.COMMENT_START
                        || ftoken.getId() == FormatToken.Kind.COMMENT_END
                        || ftoken.getId() == FormatToken.Kind.INDENT
                        || (ftoken.getId() == FormatToken.Kind.TEXT && (")".equals(ftoken.getOldText().toString()) || "]".equals(ftoken.getOldText().toString())))) {
                    formatTokens.remove(formatTokens.size() - 1);
                    removed.add(ftoken);
                    ftoken = formatTokens.get(formatTokens.size() - 1);
                }
                if (ftoken.getId() == FormatToken.Kind.WHITESPACE_INDENT) {
                    formatTokens.remove(formatTokens.size() - 1); // remove WHITESPACE_INDENT
                    formatTokens.add(new FormatToken.IndentToken(node.getEndOffset(), -1 * options.continualIndentSize));
                    formatTokens.add(ftoken); // re-add WHITESPACE_INDENT
                    for (int i = removed.size() - 1; i > -1; i--) {
                        formatTokens.add(removed.get(i));
                    }
                } else {
                    for (int i = removed.size() - 1; i > -1; i--) {
                        formatTokens.add(removed.get(i));
                    }
                    formatTokens.add(new FormatToken.IndentToken(node.getEndOffset(), -1 * options.continualIndentSize));
                }
            }
        }
        addAllUntilOffset(node.getEndOffset());
    }

    @Override
    public void visit(InfixExpression node) {
        scan(node.getLeft());
        FormatToken.Kind whitespaceBefore = FormatToken.Kind.WHITESPACE_BEFORE_BINARY_OP;
        FormatToken.Kind whitespaceAfter = FormatToken.Kind.WHITESPACE_AFTER_BINARY_OP;

        if (node.getOperator() == InfixExpression.OperatorType.CONCAT) {
            whitespaceAfter = FormatToken.Kind.WHITESPACE_AROUND_CONCAT_OP;
            whitespaceBefore = whitespaceAfter;
        }

        while (ts.moveNext() && ts.offset() < node.getRight().getStartOffset()
                && ts.token().id() != PHPTokenId.PHP_TOKEN && ts.token().id() != PHPTokenId.PHP_OPERATOR
                && lastIndex < ts.index()) {
            addFormatToken(formatTokens);
        }
        if (ts.token().id() == PHPTokenId.PHP_TOKEN || ts.token().id() == PHPTokenId.PHP_OPERATOR) {
            formatTokens.add(new FormatToken(whitespaceBefore, ts.offset()));
            addFormatToken(formatTokens);
            formatTokens.add(new FormatToken(whitespaceAfter, ts.offset() + ts.token().length()));
        } else {
            ts.movePrevious();
        }
        scan(node.getRight());
    }

    @Override
    public void visit(IfStatement node) {
        addAllUntilOffset(node.getCondition().getStartOffset());
        formatTokens.add(new FormatToken.IndentToken(ts.offset(), options.continualIndentSize));
        scan(node.getCondition());
        Statement trueStatement = node.getTrueStatement();
        formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.continualIndentSize));
        if (trueStatement != null && trueStatement instanceof Block && !((Block) trueStatement).isCurly()) {
            isCurly = false;
            addAllUntilOffset(trueStatement.getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(trueStatement.getStartOffset(), options.indentSize));
            scan(trueStatement);
            if (ts.token().id() == PHPTokenId.T_INLINE_HTML
                    && ts.moveNext() && ts.token().id() == PHPTokenId.PHP_OPENTAG) {
                addFormatToken(formatTokens);
            }
            formatTokens.add(new FormatToken.IndentToken(trueStatement.getEndOffset(), -1 * options.indentSize));
        } else if (trueStatement != null && !(trueStatement instanceof Block)) {
            isCurly = false;
            addNoCurlyBody(trueStatement, FormatToken.Kind.WHITESPACE_BEFORE_IF_ELSE_STATEMENT);
        } else {
            scan(trueStatement);
        }
        Statement falseStatement = node.getFalseStatement();
        if (falseStatement != null && falseStatement instanceof Block && !((Block) falseStatement).isCurly()
                && !(falseStatement instanceof IfStatement)) {
            isCurly = false;
            while (ts.moveNext() && ts.offset() < falseStatement.getStartOffset()) {
                if (ts.token().id() == PHPTokenId.PHP_ELSE || ts.token().id() == PHPTokenId.PHP_ELSEIF) {
                    formatTokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                } else if (lastIndex < ts.index()) {
                    addFormatToken(formatTokens);
                }
            }
            formatTokens.add(new FormatToken.IndentToken(falseStatement.getStartOffset(), options.indentSize));
            ts.movePrevious();
            scan(falseStatement);
            formatTokens.add(new FormatToken.IndentToken(falseStatement.getEndOffset(), -1 * options.indentSize));
        } else if (falseStatement != null && !(falseStatement instanceof Block) && !(falseStatement instanceof IfStatement)) {
            isCurly = false;
            while (ts.moveNext() && ts.offset() < falseStatement.getStartOffset()) {
                if (ts.token().id() == PHPTokenId.PHP_ELSE || ts.token().id() == PHPTokenId.PHP_ELSEIF) {
                    formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_ELSE_WITHOUT_CURLY, ts.offset()));
                    formatTokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                } else {
                    addFormatToken(formatTokens);
                }
            }
            ts.movePrevious();
            addAllUntilOffset(falseStatement.getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(falseStatement.getStartOffset(), options.indentSize));
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_IF_ELSE_STATEMENT, ts.offset()));
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset(), null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));
            scan(falseStatement);
            addEndOfUnbreakableSequence(falseStatement.getEndOffset());
            formatTokens.add(new FormatToken.IndentToken(falseStatement.getEndOffset(), -1 * options.indentSize));
        } else {
            scan(falseStatement);
        }

    }

    @Override
    public void visit(MethodDeclaration node) {
        while (ts.moveNext() && (ts.token().id() == PHPTokenId.WHITESPACE
                || isComment(ts.token())) && lastIndex < ts.index()) {
            addFormatToken(formatTokens);
        }
        if (includeWSBeforePHPDoc) {
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FUNCTION, ts.offset()));
        } else {
            includeWSBeforePHPDoc = true;
        }
        if (lastIndex < ts.index()) {
            addFormatToken(formatTokens);
        }
        while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_STRING) {
            switch (ts.token().id()) {
                case PHP_FUNCTION:
                    if (node.getModifier() > 0) {
                        FormatToken lastWhitespace = formatTokens.remove(formatTokens.size() - 1);
                        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_MODIFIERS, lastWhitespace.getOffset(), lastWhitespace.getOldText()));
                    }
                    addFormatToken(formatTokens);
                    break;
                default:
                    addFormatToken(formatTokens);
            }
        }
        ts.movePrevious();
        super.visit(node);
    }

    @Override
    public void visit(MethodInvocation node) {
        boolean shift = false;
        if (!isMethodInvocationShifted) {
            try {
                int startText = node.getDispatcher().getEndOffset();
                int endText = node.getMethod().getStartOffset();
                if (document.getText(startText, endText - startText).contains("\n")) {
                    shift = true;
                    addAllUntilOffset(node.getStartOffset());
                    boolean addIndent = !(path.size() > 1 && (path.get(1) instanceof Assignment));
                    if (addIndent) {
                        formatTokens.add(new FormatToken.IndentToken(ts.offset() + ts.token().length(), options.continualIndentSize));
                    }
                    isMethodInvocationShifted = true;
                    super.visit(node);
                    addAllUntilOffset(node.getEndOffset());
                    if (addIndent) {
                        formatTokens.add(new FormatToken.IndentToken(ts.offset() + ts.token().length(), -1 * options.continualIndentSize));
                    }
                    isMethodInvocationShifted = false;
                }
            } catch (BadLocationException ex) {
                LOGGER.log(Level.WARNING, "Exception in scanning method invocation", ex);  //NOI18N
                shift = false;
            }

        }
        if (!shift) {
            super.visit(node);
        }
    }

    @Override
    public void visit(NamespaceDeclaration node) {
        addAllUntilOffset(node.getStartOffset());
        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_NAMESPACE, node.getStartOffset()));
        scan(node.getName());
        addRestOfLine();
        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_NAMESPACE, ts.offset() + ts.token().length()));
        scan(node.getBody());
    }

    @Override
    public void visit(Program program) {
        if (ts != null) {
            path.addFirst(program);
            ts.move(0);
            ts.moveNext();
            ts.movePrevious();
            addFormatToken(formatTokens);
            super.visit(program);
            FormatToken lastToken = formatTokens.size() > 0
                    ? formatTokens.get(formatTokens.size() - 1)
                    : null;
            while (ts.moveNext()) {
                if (lastToken == null || lastToken.isWhitespace() || lastToken.getOffset() > ts.offset()) {
                    if (lastIndex < ts.index()) {
                        addFormatToken(formatTokens);
                        lastToken = formatTokens.get(formatTokens.size() - 1);
                    }
                }
            }
            path.removeFirst();
        }
    }

    @Override
    public void visit(ReturnStatement node) {
        while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_RETURN
                && ((ts.offset() + ts.token().length()) <= node.getEndOffset())) {
            addFormatToken(formatTokens);
        }
        if (ts.token().id() == PHPTokenId.PHP_RETURN) {
            addFormatToken(formatTokens);
            formatTokens.add(new FormatToken.IndentToken(ts.offset(), options.continualIndentSize));
            super.visit(node);
            formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.continualIndentSize));
        }
    }

    @Override
    public void visit(SingleFieldDeclaration node) {
        Variable name = node.getName();
        scan(name);
        if (node.getValue() != null) {
            while (ts.moveNext() && ts.offset() < node.getValue().getStartOffset()) {
                ASTNode parent = path.get(1);
                assert (parent instanceof FieldsDeclaration);
                FieldsDeclaration fieldsDeclaration = (FieldsDeclaration) path.get(1);
                if (ts.token().id() == PHPTokenId.PHP_TOKEN && "=".equals(ts.token().text().toString())) { //NOI18N
                    int realNodeLength = fieldsDeclaration.getModifierString().length() + " ".length() + name.getEndOffset() - name.getStartOffset(); //NOI18N
                    handleGroupAlignment(realNodeLength);
                    addFormatToken(formatTokens);
                } else {
                    addFormatToken(formatTokens);
                }
            }
            ts.movePrevious();
            if (node.getValue() != null) {
                formatTokens.add(new FormatToken.IndentToken(ts.offset() + ts.token().length(), options.continualIndentSize));
                scan(node.getValue());
                formatTokens.add(new FormatToken.IndentToken(ts.offset() + ts.token().length(), -1 * options.continualIndentSize));
            }
        }
    }

    @Override
    public void visit(SwitchCase node) {
        if (node.getValue() == null) {
            ts.moveNext();
            if (lastIndex < ts.index()) {
                addFormatToken(formatTokens);
            }
        } else {
            scan(node.getValue());
        }
        formatTokens.add(new FormatToken.IndentToken(ts.offset(), options.indentSize));
        if (node.getActions() != null) {
            scan(node.getActions());
            formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.indentSize));
        }

    }

    @Override
    public void visit(SwitchStatement node) {
        scan(node.getExpression());
        if (node.getBody() != null && !((Block) node.getBody()).isCurly()) {
            addAllUntilOffset(node.getBody().getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(node.getBody().getStartOffset(), options.indentSize));

            if (node.getBody().getStatements().size() > 0) {
                scan(node.getBody());
                Statement lastOne = node.getBody().getStatements().get(node.getBody().getStatements().size() - 1);
                while (lastOne.getEndOffset() < formatTokens.get(formatTokens.size() - 1).getOffset()) {
                    formatTokens.remove(formatTokens.size() - 1);
                }
                while (lastOne.getEndOffset() < ts.offset()) {
                    ts.movePrevious();
                }
            } else {
                while (ts.moveNext() && ts.token().id() != PHPTokenId.PHP_ENDSWITCH
                        && ts.offset() < node.getBody().getEndOffset()) {
                    addFormatToken(formatTokens);
                }
                ts.movePrevious();
            }
            formatTokens.add(new FormatToken.IndentToken(ts.offset(), -1 * options.indentSize));
            addAllUntilOffset(node.getEndOffset());
        } else {
            scan(node.getBody());
        }
    }

    @Override
    public void visit(TryStatement node) {
        scan(node.getBody());
        scan(node.getCatchClauses());
        scan(node.getFinallyClause());
    }

    @Override
    public void visit(WhileStatement node) {
        scan(node.getCondition());
        ASTNode body = node.getBody();
        if (body != null && (body instanceof Block && !((Block) body).isCurly())) {
            addAllUntilOffset(body.getStartOffset());
            formatTokens.add(new FormatToken.IndentToken(body.getStartOffset(), options.indentSize));
            scan(node.getBody());
            if (ts.token().id() == PHPTokenId.T_INLINE_HTML
                    && ts.moveNext() && ts.token().id() == PHPTokenId.PHP_OPENTAG) {
                addFormatToken(formatTokens);
            }
            formatTokens.add(new FormatToken.IndentToken(body.getEndOffset(), -1 * options.indentSize));
        } else if (body != null && !(body instanceof Block)) {
            addNoCurlyBody(body, FormatToken.Kind.WHITESPACE_BEFORE_WHILE_STATEMENT);
        } else {
            scan(node.getBody());
        }
    }

    @Override
    public void visit(UseStatement node) {

        if (isPreviousNodeTheSameInBlock(path.get(1), node)) {
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BETWEEN_USE, ts.offset()));
        } else {
            if (includeWSBeforePHPDoc) {
                formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_USE, ts.offset()));
            }
        }
        includeWSBeforePHPDoc = true;

        isFirstUseStatementPart = true;
        super.visit(node);
        if (isNextNodeTheSameInBlock(path.get(1), node)) {
            addRestOfLine();
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_USE, ts.offset() + ts.token().length()));
        }
    }

    @Override
    public void visit(UseTraitStatement node) {
        if (includeWSBeforePHPDoc) {
            formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_USE_TRAIT, ts.offset()));
        }
        includeWSBeforePHPDoc = true;
        isFirstUseTraitStatementPart = true;
        super.visit(node);
    }

    @Override
    public void visit(UseStatementPart statementPart) {
        FormatToken lastFormatToken = formatTokens.get(formatTokens.size() - 1);
        boolean lastRemoved = false;
        if (ts.token().id() == PHPTokenId.PHP_NS_SEPARATOR
                && lastFormatToken.getId() == FormatToken.Kind.TEXT
                && "\\".equals(lastFormatToken.getOldText())) {
            formatTokens.remove(formatTokens.size() - 1);
            lastRemoved = true;
        }
        if (isFirstUseStatementPart) {
            formatTokens.add(new FormatToken.AnchorToken(ts.offset()));
            isFirstUseStatementPart = false;
        }
        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_USES_PART, ts.offset()));
        if (lastRemoved) {
            formatTokens.add(lastFormatToken);
        }
        super.visit(statementPart);
    }

    @Override
    public void visit(UseTraitStatementPart node) {
        if (isFirstUseTraitStatementPart) {
            formatTokens.add(new FormatToken.AnchorToken(ts.offset()));
            isFirstUseTraitStatementPart = false;
        }
        formatTokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_USE_TRAIT_PART, ts.offset()));
        super.visit(node);
    }

    @Override
    public void visit(TraitMethodAliasDeclaration node) {
        addRestOfLine();
        super.visit(node);
    }

    @Override
    public void visit(TraitConflictResolutionDeclaration node) {
        addRestOfLine();
        super.visit(node);
    }
    private int lastIndex = -1;

    private String showAssertionFor188809() {
        String result = "";
        try {
            result = "The same token (index: " + ts.index() + " - " + ts.token().id() + ", format tokens: " + formatTokens.size() //NOI18N
                    + ")  was precessed before.\nPlease report this to help fix issue 188809.\n\n" //NOI18N
                    + document.getText(0, document.getLength() - 1);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return result;
    }

    private void addFormatToken(List<FormatToken> tokens) {
        if (lastIndex == ts.index()) {
            assert false : showAssertionFor188809();
            ts.moveNext();
            return;
        }
        lastIndex = ts.index();
        switch (ts.token().id()) {
            case WHITESPACE:
                tokens.addAll(resolveWhitespaceTokens());
                break;
            case PHP_LINE_COMMENT:
                String text = ts.token().text().toString();
                if (ts.token().text().charAt(ts.token().length() - 1) == '\n') {
                    text = text.substring(0, text.length() - 1);
                    int newOffset = ts.offset() + ts.token().length() - 1;
                    if (text.length() > 0) {
                        tokens.add(new FormatToken(FormatToken.Kind.LINE_COMMENT, ts.offset(), text));
                    }
                    if (ts.moveNext()) {
                        if (ts.token().id() == PHPTokenId.WHITESPACE) {
                            if (countOfNewLines(ts.token().text()) > 0) {
                                // reset group alignment, if there is an empty line
                                resetAndCreateGroupAlignment();
                            }
                            tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_INDENT, newOffset, "\n" + ts.token().text().toString()));
                            if (ts.moveNext() && ts.token().id() == PHPTokenId.PHP_LINE_COMMENT) {
                                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BETWEEN_LINE_COMMENTS, ts.offset()));
                            } else {
                                ts.movePrevious();
                            }
                        } else {
                            tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_INDENT, newOffset, "\n"));
                            ts.movePrevious();
                        }
                    } else {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_INDENT, newOffset, "\n"));
                    }

                } else {
                    tokens.add(new FormatToken(FormatToken.Kind.LINE_COMMENT, ts.offset(), text));
                }
                break;
            case PHP_OPENTAG:
            case T_OPEN_TAG_WITH_ECHO:
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_OPEN_PHP_TAG, ts.offset()));
                tokens.add(new FormatToken(FormatToken.Kind.OPEN_TAG, ts.offset(), ts.token().text().toString()));
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_OPEN_PHP_TAG, ts.offset() + ts.token().length()));
                break;
            case PHP_CLOSETAG:
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CLOSE_PHP_TAG, ts.offset()));
                tokens.add(new FormatToken(FormatToken.Kind.CLOSE_TAG, ts.offset(), ts.token().text().toString()));
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_CLOSE_PHP_TAG, ts.offset() + ts.token().length()));
                break;
            case PHP_COMMENT_START:
                tokens.add(new FormatToken(FormatToken.Kind.COMMENT_START, ts.offset(), ts.token().text().toString()));
                break;
            case PHP_COMMENT_END:
                tokens.add(new FormatToken(FormatToken.Kind.COMMENT_END, ts.offset(), ts.token().text().toString()));
                break;
            case PHP_COMMENT:
                tokens.add(new FormatToken(FormatToken.Kind.COMMENT, ts.offset(), ts.token().text().toString()));
                break;
            case PHPDOC_COMMENT:
                tokens.add(new FormatToken(FormatToken.Kind.DOC_COMMENT, ts.offset(), ts.token().text().toString()));
                break;
            case PHPDOC_COMMENT_START:
                tokens.add(new FormatToken(FormatToken.Kind.DOC_COMMENT_START, ts.offset(), ts.token().text().toString()));
                break;
            case PHPDOC_COMMENT_END:
                tokens.add(new FormatToken(FormatToken.Kind.DOC_COMMENT_END, ts.offset(), ts.token().text().toString()));
                break;
            case PHP_OBJECT_OPERATOR:
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AROUND_OBJECT_OP, ts.offset()));
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AROUND_OBJECT_OP, ts.offset() + ts.token().length()));
                break;
            case PHP_CASTING:
                text = ts.token().text().toString();
                String part1 = text.substring(0, text.indexOf('(') + 1);
                String part2 = text.substring(part1.length(), text.indexOf(')'));
                String part3 = text.substring(part1.length() + part2.length());
                StringBuilder ws1 = new StringBuilder();
                StringBuilder ws2 = new StringBuilder();
                int index = 0;
                while (index < part2.length() && part2.charAt(index) == ' ') {
                    ws1.append(' ');
                    index++;
                }
                index = part2.length() - 1;
                while (index > 0 && part2.charAt(index) == ' ') {
                    ws2.append(' ');
                    index--;
                }
                part2 = part2.trim();
                int length = 0;
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), part1));
                length += part1.length();
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_TYPE_CAST_PARENS, ts.offset() + part1.length()));
                if (ws1.length() > 0) {
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE, ts.offset() + length, ws1.toString()));
                    length += ws1.length();
                }
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset() + length, part2));
                length += part2.length();
                if (ws2.length() > 0) {
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE, ts.offset() + length, ws2.toString()));
                    length += ws2.length();
                }
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_TYPE_CAST_PARENS, ts.offset() + length));
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset() + length, part3));
                length += part3.length();
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_TYPE_CAST, ts.offset() + length));
                break;
            case PHP_TOKEN:
                text = ts.token().text().toString();
                ASTNode parent = path.get(0);
                if ("(".equals(text)) {
                    if (parent instanceof FunctionDeclaration || parent instanceof MethodDeclaration) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_METHOD_DEC_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_METHOD_DECL_PARENS, ts.offset() + ts.token().length()));
                    } else if (parent instanceof FunctionInvocation || parent instanceof MethodInvocation || parent instanceof ClassInstanceCreation) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_METHOD_CALL_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_METHOD_CALL_PARENS, ts.offset() + ts.token().length()));
                    } else if (parent instanceof IfStatement) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_IF_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_IF_PARENS, ts.offset() + ts.token().length()));
                    } else if (parent instanceof ForEachStatement || parent instanceof ForStatement) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FOR_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_FOR_PARENS, ts.offset() + ts.token().length()));
                    } else if (parent instanceof WhileStatement || parent instanceof DoStatement) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_WHILE_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_WHILE_PARENS, ts.offset() + ts.token().length()));
                    } else if (parent instanceof SwitchStatement) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_SWITCH_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_SWITCH_PARENS, ts.offset() + ts.token().length()));
                    } else if (parent instanceof CatchClause) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CATCH_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_CATCH_PARENS, ts.offset() + ts.token().length()));
                    } else if (parent instanceof ArrayCreation) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_ARRAY_DECL_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_ARRAY_DECL_LEFT_PAREN, ts.offset() + ts.token().length()));
                    } else {
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    }
                } else if (")".equals(text)) {
                    if (parent instanceof FunctionDeclaration || parent instanceof MethodDeclaration) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_METHOD_DECL_PARENS, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    } else if (parent instanceof FunctionInvocation || parent instanceof MethodInvocation || parent instanceof ClassInstanceCreation) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_METHOD_CALL_PARENS, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    } else if (parent instanceof IfStatement) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_IF_PARENS, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    } else if (parent instanceof ForEachStatement || parent instanceof ForStatement) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_FOR_PARENS, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    } else if (parent instanceof WhileStatement || parent instanceof DoStatement) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_WHILE_PARENS, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    } else if (parent instanceof SwitchStatement) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_SWITCH_PARENS, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    } else if (parent instanceof CatchClause) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_CATCH_PARENS, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    } else if (parent instanceof ArrayCreation) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_ARRAY_DECL_RIGHT_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    } else {
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    }
                } else if ("[".equals(text)) {
                    if (parent instanceof ArrayCreation) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_ARRAY_DECL_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_ARRAY_DECL_LEFT_PAREN, ts.offset() + ts.token().length()));
                    } else {
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_ARRAY_BRACKETS_PARENS, ts.offset() + ts.token().length()));
                    }
                } else if ("]".equals(text)) {
                    if (parent instanceof ArrayCreation) {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_ARRAY_DECL_RIGHT_PAREN, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    } else {
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_WITHIN_ARRAY_BRACKETS_PARENS, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    }
                } else if (parent instanceof ConditionalExpression
                        && ("?".equals(text) || ":".equals(text))) {
                    tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AROUND_TERNARY_OP, ts.offset() + ts.token().length()));
                } else if (",".equals(text)) {
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_COMMA, ts.offset()));
                    tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_COMMA, ts.offset() + ts.token().length()));
                } else if ("!".equals(text)) {
                    int origOffset = ts.offset();
                    if (ts.movePrevious()) {
                        Token<? extends PHPTokenId> previous = LexUtilities.findPrevious(ts, Arrays.asList(PHPTokenId.WHITESPACE));
                        if (previous.id() == PHPTokenId.PHP_RETURN) {
                            tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_KEYWORD, origOffset));
                        }
                        ts.move(origOffset);
                        ts.moveNext();
                    }
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AROUND_UNARY_OP, ts.offset()));
                    tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), text));
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AROUND_UNARY_OP, ts.offset() + ts.token().length()));
                } else if ("=".equals(text)) {
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_ASSIGN_OP, ts.offset()));
                    tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), text));
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_ASSIGN_OP, ts.offset() + ts.token().length()));
                } else {
                    tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                }
                break;
            case PHP_OPERATOR:
                text = ts.token().text().toString();
                switch (text) {
                    case "=>": //NOI18N
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AROUND_KEY_VALUE_OP, ts.offset()));
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AROUND_KEY_VALUE_OP, ts.offset() + ts.token().length()));
                        break;
                    case "++": //NOI18N
                    case "--": //NOI18N
                        int origOffset = ts.offset();
                        if (ts.movePrevious()) {
                            if (ts.token().id() == PHPTokenId.PHP_VARIABLE || ts.token().id() == PHPTokenId.PHP_STRING) {
                                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AROUND_UNARY_OP, ts.offset() + ts.token().length()));
                            } else if (ts.token().id() != PHPTokenId.WHITESPACE) {
                                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE, ts.offset() + ts.token().length()));
                            }
                            ts.move(origOffset);
                            ts.moveNext();
                        }
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), text));
                        tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AROUND_UNARY_OP, ts.offset() + ts.token().length()));
                        break;
                    default:
                        tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                        break;
                }
                break;
            case PHP_WHILE:
                if (path.get(0) instanceof DoStatement && isCurly) {
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_WHILE, ts.offset()));
                }
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                break;
            case PHP_ELSE:
            case PHP_ELSEIF:
                if (isCurly) {
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_ELSE, ts.offset()));
                }
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                break;
            case PHP_SEMICOLON:
                if (!(ts.movePrevious() && ts.token().id() == PHPTokenId.WHITESPACE
                        && countOfNewLines(ts.token().text()) > 0)) {
                    // if a line starts with the semicolon, don't put this whitespace
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_SEMI, ts.offset() + ts.token().length()));
                }
                ts.moveNext();
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                if (path.size() > 0 && !(path.get(0) instanceof ForStatement)) {
                    tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_AFTER_SEMI, ts.offset() + ts.token().length()));
                }
                break;
            case PHP_CATCH:
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_CATCH, ts.offset()));
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                break;
            case PHP_FINALLY:
                tokens.add(new FormatToken(FormatToken.Kind.WHITESPACE_BEFORE_FINALLY, ts.offset()));
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
                break;
            case T_INLINE_HTML:
                FormatToken.InitToken token = (FormatToken.InitToken) formatTokens.get(0);
                if (!token.hasHTML() && !isWhitespace(ts.token().text())) {
                    token.setHasHTML(true);
                }
                int tokenStartOffset = ts.offset();
                StringBuilder sb = new StringBuilder(ts.token().text());
                // merge all html following tokens to one format token;
                while (ts.moveNext() && ts.token().id() == PHPTokenId.T_INLINE_HTML) {
                    sb.append(ts.token().text());
                }

                if (ts.moveNext()) {
                    ts.movePrevious();
                    ts.movePrevious();
                    tokens.add(new FormatToken(FormatToken.Kind.HTML, tokenStartOffset, sb.toString()));
                } else {
                    // this is the last token in the document
                    lastIndex--;
                }
                break;
            default:
                tokens.add(new FormatToken(FormatToken.Kind.TEXT, ts.offset(), ts.token().text().toString()));
        }
    }

    private List<FormatToken> resolveWhitespaceTokens() {
        final List<FormatToken> result = new LinkedList<>();
        int countNewLines = countOfNewLines(ts.token().text());
        if (countNewLines > 1) {
            // reset group alignment, if there is an empty line
            resetAndCreateGroupAlignment();
        }
        String tokenText = ts.token().text().toString();
        int tokenStartOffset = ts.offset();
        if (countNewLines > 0) {
            result.add(new FormatToken(FormatToken.Kind.WHITESPACE_INDENT, tokenStartOffset, adjustLastWhitespaceToken(ts.token())));
        } else {
            int tokenEndOffset = tokenStartOffset + ts.token().length();
            if (GsfUtilities.isCodeTemplateEditing(document)
                    && caretOffset > tokenStartOffset
                    && caretOffset < tokenEndOffset
                    && tokenStartOffset > startOffset
                    && tokenEndOffset < endOffset) {
                int devideIndex = caretOffset - tokenStartOffset;
                String firstTextPart = tokenText.substring(0, devideIndex);
                result.add(new FormatToken(FormatToken.Kind.WHITESPACE, tokenStartOffset, firstTextPart));
                result.add(new FormatToken(FormatToken.Kind.WHITESPACE, tokenStartOffset + firstTextPart.length(), tokenText.substring(devideIndex)));
            } else {
                result.add(new FormatToken(FormatToken.Kind.WHITESPACE, tokenStartOffset, adjustLastWhitespaceToken(ts.token())));
            }
        }
        return result;
    }

    /**
     * This is an ugly hack.
     *
     * Source which is lexed is adjusted by someone and '\n' is added at the end of source,
     * even though there is NO new line at the end of file (source). Then FormatVisitor adds an extra
     * formatting token of an invalid value and TokenFormatter can't count trailing new lines
     * properly.
     *
     * @param token
     * @return if last token is processed, then text without one '\n', tokenText otherwise
     */
    private String adjustLastWhitespaceToken(Token<PHPTokenId> token) {
        assert token.id() == PHPTokenId.WHITESPACE;
        String result;
        String tokenText = token.text().toString();
        boolean isLast;
        if (ts.moveNext()) {
            isLast = false;
            ts.movePrevious();
        } else {
            isLast = true;
        }
        if (isLast) {
            int firstNewLineOffset = tokenText.indexOf('\n');
            result = tokenText.substring(0, firstNewLineOffset) + tokenText.substring(firstNewLineOffset + 1);
        } else {
            result = tokenText;
        }
        return result;
    }

    private void addAllUntilOffset(int offset) {
        while (moveNext() && ts.offset() < offset
                && (ts.offset() + ts.token().length()) <= offset) {
            addFormatToken(formatTokens);
        }
        ts.movePrevious();
    }

    private void addRestOfLine() {
        while (ts.moveNext()
                && ts.token().id() != PHPTokenId.PHP_LINE_COMMENT
                && ((ts.token().id() == PHPTokenId.WHITESPACE && countOfNewLines(ts.token().text()) == 0)
                || isComment(ts.token())
                || ts.token().id() == PHPTokenId.PHP_SEMICOLON) && lastIndex < ts.index()) {
            addFormatToken(formatTokens);
        }
        if (ts.token().id() == PHPTokenId.PHP_LINE_COMMENT
                || (ts.token().id() == PHPTokenId.WHITESPACE && countOfNewLines(ts.token().text()) == 0)) {
            addFormatToken(formatTokens);
            if (ts.token().id() == PHPTokenId.PHP_LINE_COMMENT && ts.moveNext() && ts.token().id() == PHPTokenId.PHP_LINE_COMMENT) {
                addFormatToken(formatTokens);
            } else {
                ts.movePrevious();
            }
        } else {
            ts.movePrevious();
        }
    }

    /**
     *
     * @param chs
     * @return number of new lines in the input
     */
    private int countOfNewLines(CharSequence chs) {
        int count = 0;
        for (int i = 0; i < chs.length(); i++) {
            if (chs.charAt(i) == '\n') { // NOI18N
                count++;
            }
        }
        return count;
    }

    private void addEndOfUnbreakableSequence(int endOffset) {
        boolean wasLastLineComment = false;
        while (ts.moveNext()
                && ((ts.token().id() == PHPTokenId.WHITESPACE
                && countOfNewLines(ts.token().text()) == 0)
                || isComment(ts.token()))
                && lastIndex < ts.index()) {
            if (ts.token().id() == PHPTokenId.PHP_LINE_COMMENT
                    && !"//".equals(ts.token().text().toString())) {
                addFormatToken(formatTokens);
                wasLastLineComment = true;
                break;
            }
            addFormatToken(formatTokens);

        }
        if (wasLastLineComment) {
            while (ts.moveNext()
                    && (ts.token().id() == PHPTokenId.PHP_COMMENT_START
                        || ts.token().id() == PHPTokenId.PHP_COMMENT_END
                        || ts.token().id() == PHPTokenId.PHP_COMMENT)) {
                addFormatToken(formatTokens);
            }
            ts.movePrevious();
            FormatToken last = formatTokens.remove(formatTokens.size() - 1);
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset() + ts.token().length() - 1, null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_END));
            formatTokens.add(last);
        } else {
            ts.movePrevious();
            if ((ts.token().id() == PHPTokenId.WHITESPACE
                    && countOfNewLines(ts.token().text()) == 0)) {
                List<FormatToken> removedWhitespaces = new ArrayList<>();
                do {
                    removedWhitespaces.add(formatTokens.remove(formatTokens.size() - 1));
                } while (formatTokens.get(formatTokens.size() - 1).isWhitespace());
                formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset(), null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_END));
                Collections.reverse(removedWhitespaces);
                for (FormatToken formatToken : removedWhitespaces) {
                    formatTokens.add(formatToken);
                }
            } else {
                formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset() + ts.token().length(), null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_END));
            }
        }
    }

    private void addUnbreakalbeSequence(ASTNode node, boolean addAnchor) {
        formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset() + ts.token().length(), null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));
        addAllUntilOffset(node.getStartOffset());
        if (addAnchor) {
            formatTokens.add(new FormatToken.AnchorToken(ts.offset() + ts.token().length()));
        }
        scan(node);
        while (ts.moveNext()
                && (ts.token().id() == PHPTokenId.WHITESPACE
                || isComment(ts.token())
                || (ts.token().id() == PHPTokenId.PHP_TOKEN && ",".equals(ts.token().text().toString())))
                && lastIndex < ts.index()) {
            addFormatToken(formatTokens);
        }
        ts.movePrevious();

        int index = formatTokens.size() - 1;
        FormatToken lastToken = formatTokens.get(index);
        FormatToken removedWS = null;
        if (lastToken.getId() == FormatToken.Kind.WHITESPACE || lastToken.getId() == FormatToken.Kind.WHITESPACE_INDENT) {
            removedWS = formatTokens.remove(formatTokens.size() - 1);
            index--;
            lastToken = formatTokens.get(index);
        }

        if (lastToken.getId() == FormatToken.Kind.WHITESPACE_AFTER_COMMA) {
            formatTokens.remove(index);
            formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset() + ts.token().length(), null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_END));
            formatTokens.add(lastToken);
            if (removedWS != null) {
                formatTokens.add(removedWS);
            }
        } else {
            if (lastToken.getId() == FormatToken.Kind.LINE_COMMENT && removedWS != null) {
                formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset() + ts.token().length() - 1, null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_END));
                formatTokens.add(removedWS);
            } else {
                formatTokens.add(new FormatToken.UnbreakableSequenceToken(ts.offset() + ts.token().length(), null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_END));
                if (removedWS != null) {
                    formatTokens.add(removedWS);
                }
            }

        }
    }

    private boolean isComment(Token<? extends PHPTokenId> token) {
        return token.id() == PHPTokenId.PHPDOC_COMMENT
                || token.id() == PHPTokenId.PHPDOC_COMMENT_END
                || token.id() == PHPTokenId.PHPDOC_COMMENT_START
                || token.id() == PHPTokenId.PHP_COMMENT
                || token.id() == PHPTokenId.PHP_COMMENT_END
                || token.id() == PHPTokenId.PHP_COMMENT_START
                || token.id() == PHPTokenId.PHP_LINE_COMMENT;
    }

    private boolean isPreviousNodeTheSameInBlock(ASTNode astNode, Statement statement) {
        int index = 0;   // index of the current statement in the block
        List<Statement> statements = null;

        if (astNode instanceof Block) {
            statements = ((Block) astNode).getStatements();
        } else if (astNode instanceof Program) {
            statements = ((Program) astNode).getStatements();
        }
        if (statements != null) {
            while (index < statements.size() && statements.get(index).getStartOffset() < statement.getStartOffset()) {
                index++;
            }
            return (index < statements.size()
                    && index > 0
                    && statements.get(index - 1).getClass().equals(statement.getClass()));
        }
        return false;
    }

    private boolean isNextNodeTheSameInBlock(ASTNode astNode, Statement statement) {
        int index = 0;   // index of the current statement in the block
        List<Statement> statements = null;

        if (astNode instanceof Block) {
            statements = ((Block) astNode).getStatements();
        } else if (astNode instanceof Program) {
            statements = ((Program) astNode).getStatements();
        }
        if (statements != null) {
            while (index < statements.size() && statements.get(index).getStartOffset() < statement.getStartOffset()) {
                index++;
            }
            return (index == statements.size() - 1
                    || !((index < statements.size() - 1) && (statements.get(index + 1).getClass().equals(statement.getClass()))));
        }
        return false;
    }

    private void addNoCurlyBody(ASTNode body, FormatToken.Kind before) {
        addAllUntilOffset(body.getStartOffset());
        if (ts.moveNext() && ts.token().id() == PHPTokenId.PHP_TOKEN && ")".equals(ts.token().text().toString())) {
            // the body is not defined yet. See issue #187665
            addFormatToken(formatTokens);
        } else {
            ts.movePrevious();
        }
        formatTokens.add(new FormatToken.IndentToken(body.getStartOffset(), options.indentSize));
        if (!(body instanceof ASTError)) {
            formatTokens.add(new FormatToken(before, body.getStartOffset()));
        }
        formatTokens.add(new FormatToken.UnbreakableSequenceToken(body.getStartOffset(), null, FormatToken.Kind.UNBREAKABLE_SEQUENCE_START));
        scan(body);
        addEndOfUnbreakableSequence(body.getEndOffset());
        formatTokens.add(new FormatToken.IndentToken(body.getEndOffset(), -1 * options.indentSize));
    }

    private boolean moveNext() {
        boolean value = ts.moveNext();
        if (value) {
            FormatToken last = formatTokens.get(formatTokens.size() - 1);
            value = !(last.getId() == FormatToken.Kind.TEXT && last.getOffset() >= ts.offset());
        }
        return value;
    }

    /**
     *
     * @param node and identifier that is before the operator that is aligned in
     * the group
     */
    private void handleGroupAlignment(int nodeLength, boolean multilined) {
        if (groupAlignmentTokenHolders.empty()) {
            createGroupAlignment();
        }
        GroupAlignmentTokenHolder tokenHolder = groupAlignmentTokenHolders.peek();
        FormatToken.AssignmentAnchorToken previousGroupToken = tokenHolder.getToken();
        if (previousGroupToken == null) {
            // it's the first line in the group
            previousGroupToken = new FormatToken.AssignmentAnchorToken(ts.offset(), multilined);
            previousGroupToken.setLenght(nodeLength);
            previousGroupToken.setMaxLength(nodeLength);
        } else {
            // it's a next line in the group.
            FormatToken.AssignmentAnchorToken aaToken = new FormatToken.AssignmentAnchorToken(ts.offset(), multilined);
            aaToken.setLenght(nodeLength);
            aaToken.setPrevious(previousGroupToken);
            aaToken.setIsInGroup(true);
            if (!previousGroupToken.isInGroup()) {
                previousGroupToken.setIsInGroup(true);
            }
            if (previousGroupToken.getMaxLength() < nodeLength) {
                // if the length of the current identifier is bigger, then is in
                // the group so far, change max length for all items in the group
                previousGroupToken = aaToken;
                do {
                    aaToken.setMaxLength(nodeLength);
                    aaToken = aaToken.getPrevious();
                } while (aaToken != null);
            } else {
                aaToken.setMaxLength(previousGroupToken.getMaxLength());
                previousGroupToken = aaToken;
            }
        }
        tokenHolder.setToken(previousGroupToken);
        formatTokens.add(previousGroupToken);
    }

    private void handleGroupAlignment(int nodeLength) {
        handleGroupAlignment(nodeLength, false);
    }

    private void handleGroupAlignment(ASTNode node) {
        handleGroupAlignment(node.getEndOffset() - node.getStartOffset(), false);
    }

    private void handleGroupAlignment(ASTNode node, boolean multilined) {
        handleGroupAlignment(node.getEndOffset() - node.getStartOffset(), multilined);
    }

    private void resetAndCreateGroupAlignment() {
        resetGroupAlignment();
        createGroupAlignment();
    }

    private void resetGroupAlignment() {
        if (!groupAlignmentTokenHolders.empty()) {
            groupAlignmentTokenHolders.pop();
        }
    }

    private void createGroupAlignment() {
        groupAlignmentTokenHolders.push(new GroupAlignmentTokenHolderImpl());
    }

    private interface GroupAlignmentTokenHolder {

        void setToken(FormatToken.AssignmentAnchorToken token);

        FormatToken.AssignmentAnchorToken getToken();
    }

    private static class GroupAlignmentTokenHolderImpl implements GroupAlignmentTokenHolder {
        private AssignmentAnchorToken token;

        @Override
        public void setToken(AssignmentAnchorToken token) {
            this.token = token;
        }

        @Override
        public AssignmentAnchorToken getToken() {
            return token;
        }

    }

    protected static boolean isWhitespace(final CharSequence text) {
        int index = 0;
        while (index < text.length()
                && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index == text.length();
    }
}
