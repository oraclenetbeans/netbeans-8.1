/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.CodeCompletionContext;
import org.netbeans.modules.csl.api.CodeCompletionHandler.QueryType;
import org.netbeans.modules.csl.api.CodeCompletionHandler2;
import org.netbeans.modules.csl.api.CodeCompletionResult;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.api.Documentation;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ParameterInfo;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport.Kind;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.completion.CompletionContextFinder.CompletionContext;
import org.netbeans.modules.php.editor.completion.CompletionContextFinder.KeywordCompletionType;
import static org.netbeans.modules.php.editor.completion.CompletionContextFinder.lexerToASTOffset;
import org.netbeans.modules.php.editor.completion.PHPCompletionItem.CompletionRequest;
import org.netbeans.modules.php.editor.completion.PHPCompletionItem.FieldItem;
import org.netbeans.modules.php.editor.completion.PHPCompletionItem.MethodElementItem;
import org.netbeans.modules.php.editor.completion.PHPCompletionItem.TypeConstantItem;
import org.netbeans.modules.php.editor.PredefinedSymbols;
import org.netbeans.modules.php.editor.api.AliasedName;
import org.netbeans.modules.php.editor.api.ElementQueryFactory;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.NameKind.CaseInsensitivePrefix;
import org.netbeans.modules.php.editor.api.PhpElementKind;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.api.QualifiedNameKind;
import org.netbeans.modules.php.editor.api.elements.AliasedElement.Trait;
import org.netbeans.modules.php.editor.api.elements.ClassElement;
import org.netbeans.modules.php.editor.api.elements.ConstantElement;
import org.netbeans.modules.php.editor.api.elements.ElementFilter;
import org.netbeans.modules.php.editor.api.elements.FieldElement;
import org.netbeans.modules.php.editor.api.elements.FunctionElement;
import org.netbeans.modules.php.editor.api.elements.InterfaceElement;
import org.netbeans.modules.php.editor.api.elements.MethodElement;
import org.netbeans.modules.php.editor.api.elements.NamespaceElement;
import org.netbeans.modules.php.editor.api.elements.PhpElement;
import org.netbeans.modules.php.editor.api.elements.TraitElement;
import org.netbeans.modules.php.editor.api.elements.TypeConstantElement;
import org.netbeans.modules.php.editor.api.elements.TypeElement;
import org.netbeans.modules.php.editor.api.elements.VariableElement;
import org.netbeans.modules.php.editor.elements.TypeResolverImpl;
import org.netbeans.modules.php.editor.elements.VariableElementImpl;
import org.netbeans.modules.php.editor.indent.CodeStyle;
import org.netbeans.modules.php.editor.lexer.LexUtilities;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.NamespaceScope;
import org.netbeans.modules.php.editor.model.ParameterInfoSupport;
import org.netbeans.modules.php.editor.model.TypeScope;
import org.netbeans.modules.php.editor.model.VariableName;
import org.netbeans.modules.php.editor.model.VariableScope;
import org.netbeans.modules.php.editor.model.impl.Type;
import org.netbeans.modules.php.editor.model.impl.VariousUtils;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.options.CodeCompletionPanel.VariablesScope;
import org.netbeans.modules.php.editor.options.OptionsUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.TypeDeclaration;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Tomasz.Slota@Sun.COM
 */
public class PHPCodeCompletion implements CodeCompletionHandler2 {

    private static final Logger LOGGER = Logger.getLogger(PHPCodeCompletion.class.getName());
    static final Map<String, KeywordCompletionType> PHP_KEYWORDS = new HashMap<>();

    static {
        PHP_KEYWORDS.put("use", KeywordCompletionType.SIMPLE); //NOI18N
        PHP_KEYWORDS.put("namespace", KeywordCompletionType.SIMPLE); //NOI18N
        PHP_KEYWORDS.put("class", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("const", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("continue", KeywordCompletionType.ENDS_WITH_SEMICOLON); //NOI18N
        PHP_KEYWORDS.put("function", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("new", KeywordCompletionType.SIMPLE); //NOI18N
        PHP_KEYWORDS.put("static", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("var", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("final", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("interface", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("instanceof", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("implements", KeywordCompletionType.SIMPLE); //NOI18N
        PHP_KEYWORDS.put("extends", KeywordCompletionType.SIMPLE); //NOI18N
        PHP_KEYWORDS.put("public", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("private", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("protected", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("abstract", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("clone", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("global", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("goto", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("throw", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("if", KeywordCompletionType.CURSOR_INSIDE_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("switch", KeywordCompletionType.CURSOR_INSIDE_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("for", KeywordCompletionType.CURSOR_INSIDE_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("array", KeywordCompletionType.CURSOR_INSIDE_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("foreach", KeywordCompletionType.CURSOR_INSIDE_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("while", KeywordCompletionType.CURSOR_INSIDE_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("catch", KeywordCompletionType.CURSOR_INSIDE_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("try", KeywordCompletionType.ENDS_WITH_CURLY_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("default", KeywordCompletionType.ENDS_WITH_COLON); //NOI18N
        PHP_KEYWORDS.put("break", KeywordCompletionType.ENDS_WITH_SEMICOLON); //NOI18N
        PHP_KEYWORDS.put("endif", KeywordCompletionType.ENDS_WITH_SEMICOLON); //NOI18N
        PHP_KEYWORDS.put("endfor", KeywordCompletionType.ENDS_WITH_SEMICOLON); //NOI18N
        PHP_KEYWORDS.put("endforeach", KeywordCompletionType.ENDS_WITH_SEMICOLON); //NOI18N
        PHP_KEYWORDS.put("endwhile", KeywordCompletionType.ENDS_WITH_SEMICOLON); //NOI18N
        PHP_KEYWORDS.put("endswitch", KeywordCompletionType.ENDS_WITH_SEMICOLON); //NOI18N
        PHP_KEYWORDS.put("case", KeywordCompletionType.ENDS_WITH_COLON); //NOI18N
        PHP_KEYWORDS.put("and", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("as", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("declare", KeywordCompletionType.CURSOR_INSIDE_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("do", KeywordCompletionType.ENDS_WITH_CURLY_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("else", KeywordCompletionType.ENDS_WITH_CURLY_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("elseif", KeywordCompletionType.ENDS_WITH_BRACKETS_AND_CURLY_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("enddeclare", KeywordCompletionType.ENDS_WITH_SEMICOLON); //NOI18N
        PHP_KEYWORDS.put("or", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("xor", KeywordCompletionType.ENDS_WITH_SPACE); //NOI18N
        PHP_KEYWORDS.put("finally", KeywordCompletionType.ENDS_WITH_CURLY_BRACKETS); //NOI18N
        PHP_KEYWORDS.put("yield", KeywordCompletionType.CURSOR_BEFORE_ENDING_SEMICOLON); //NOI18N
    }
    private static final String[] PHP_LANGUAGE_CONSTRUCTS_WITH_QUOTES = {
        "echo", "include", "include_once", "require", "require_once", "print" // NOI18N
    };
    private static final String[] PHP_LANGUAGE_CONSTRUCTS_WITH_PARENTHESES = {
        "die", "eval", "exit", "empty", "isset", "list", "unset" // NOI18N
    };
    private static final String[] PHP_LANGUAGE_CONSTRUCTS_WITH_SEMICOLON = {
        "return" // NOI18N
    };
    private static final String[] PHP_LANGUAGE_CONSTRUCTS_FOR_TYPE_HINTS = {
        "callable" //NOI18N
    };
    static final String PHP_CLASS_KEYWORD_THIS = "$this->"; //NOI18N
    static final String[] PHP_CLASS_KEYWORDS = {
        PHP_CLASS_KEYWORD_THIS, "self::", "parent::", "static::" //NOI18N
    };
    static final String[] PHP_STATIC_CLASS_KEYWORDS = {
        "self::", "parent::", "static::" //NOI18N
    };
    private static final Collection<Character> AUTOPOPUP_STOP_CHARS = new TreeSet<>(
            Arrays.asList('=', ';', '+', '-', '*', '/',
            '%', '(', ')', '[', ']', '{', '}', '?'));
    private static final Collection<PHPTokenId> TOKENS_TRIGGERING_AUTOPUP_TYPES_WS =
            Arrays.asList(PHPTokenId.PHP_NEW, PHPTokenId.PHP_EXTENDS, PHPTokenId.PHP_IMPLEMENTS, PHPTokenId.PHP_INSTANCEOF);
    private static final List<String> INVALID_PROPOSALS_FOR_CLS_MEMBERS =
            Arrays.asList(new String[]{"__construct", "__destruct", "__call", "__callStatic",
                "__clone", "__get", "__invoke", "__isset", "__set", "__set_state",
                "__sleep", "__toString", "__unset", "__wakeup"}); //NOI18N
    private static final List<String> CLASS_CONTEXT_KEYWORD_PROPOSAL =
            Arrays.asList(new String[]{"abstract", "const", "function", "private", "final",
                "protected", "public", "static", "var"}); //NOI18N
    private static final List<String> INTERFACE_CONTEXT_KEYWORD_PROPOSAL =
            Arrays.asList(new String[]{"const", "function", "public", "static"}); //NOI18N
    private static final List<String> INHERITANCE_KEYWORDS =
            Arrays.asList(new String[]{"extends", "implements"}); //NOI18N
    private static final String EXCEPTION_CLASS_NAME = "\\Exception"; // NOI18N
    private boolean caseSensitive;
    private QuerySupport.Kind nameKind;

    @Override
    public CodeCompletionResult complete(CodeCompletionContext completionContext) {
        long startTime = 0;
        if (LOGGER.isLoggable(Level.FINE)) {
            startTime = System.currentTimeMillis();
        }

        BaseDocument doc = (BaseDocument) completionContext.getParserResult().getSnapshot().getSource().getDocument(false);
        if (doc == null) {
            return CodeCompletionResult.NONE;
        }

        // TODO: separate the code that uses informatiom from lexer
        // and avoid running the index/ast analysis under read lock
        // in order to improve responsiveness
        // doc.readLock();        //TODO: use token hierarchy from snapshot and not use read lock in CC #171702

        final PHPCompletionResult completionResult = new PHPCompletionResult(completionContext);
        ParserResult info = completionContext.getParserResult();
        int caretOffset = completionContext.getCaretOffset();

        this.caseSensitive = completionContext.isCaseSensitive();
        this.nameKind = caseSensitive ? QuerySupport.Kind.PREFIX : QuerySupport.Kind.CASE_INSENSITIVE_PREFIX;

        PHPParseResult result = (PHPParseResult) info;

        if (result.getProgram() == null) {
            return CodeCompletionResult.NONE;
        }
        final FileObject fileObject = result.getSnapshot().getSource().getFileObject();
        if (fileObject == null) {
            return CodeCompletionResult.NONE;
        }

        CompletionContext context = CompletionContextFinder.findCompletionContext(info, caretOffset);
        LOGGER.log(Level.FINE, String.format("CC context: %s", context.toString()));

        if (context == CompletionContext.NONE) {
            return CodeCompletionResult.NONE;
        }

        PHPCompletionItem.CompletionRequest request = new PHPCompletionItem.CompletionRequest();
        request.context = context;
        String prefix = getPrefix(info, caretOffset, true, PrefixBreaker.WITH_NS_PARTS);
        if (prefix == null) {
            return CodeCompletionResult.NONE;
        }
        prefix = prefix.trim().isEmpty() ? completionContext.getPrefix() : prefix;

        request.anchor = caretOffset
                // can't just use 'prefix.getLength()' here cos it might have been calculated with
                // the 'upToOffset' flag set to false
                - prefix.length();

        request.result = result;
        request.info = info;
        request.prefix = prefix;
        request.index = ElementQueryFactory.getIndexQuery(info);

        request.currentlyEditedFileURL = fileObject.toURL().toString();
        switch (context) {
            case DEFAULT_PARAMETER_VALUE:
                final CaseInsensitivePrefix nameKindPrefix = NameKind.caseInsensitivePrefix(request.prefix);
                autoCompleteKeywords(completionResult, request, Arrays.asList("array")); //NOI18N
                autoCompleteNamespaces(completionResult, request);
                autoCompleteTypeNames(completionResult, request, null, true);
                final ElementFilter forName = ElementFilter.forName(nameKindPrefix);
                final Model model = request.result.getModel();
                final Set<AliasedName> aliasedNames = ModelUtils.getAliasedNames(model, request.anchor);
                Set<ConstantElement> constants = request.index.getConstants(nameKindPrefix, aliasedNames, Trait.ALIAS);
                for (ConstantElement constant : forName.filter(constants)) {
                    completionResult.add(new PHPCompletionItem.ConstantItem(constant, request));
                }
                final ClassDeclaration enclosingClass = findEnclosingClass(request.info, lexerToASTOffset(request.result, request.anchor));
                if (enclosingClass != null) {
                    String clsName = enclosingClass.getName().getName();
                    for (String classKeyword : PHP_STATIC_CLASS_KEYWORDS) {
                        if (classKeyword.toLowerCase().startsWith(request.prefix)) { //NOI18N
                            completionResult.add(new PHPCompletionItem.ClassScopeKeywordItem(clsName, classKeyword, request));
                        }
                    }
                }
                break;
            case NAMESPACE_KEYWORD:
                Set<NamespaceElement> namespaces = request.index.getNamespaces(
                        NameKind.caseInsensitivePrefix(QualifiedName.create(request.prefix).toNotFullyQualified()));
                for (NamespaceElement namespace : namespaces) {
                    completionResult.add(new PHPCompletionItem.NamespaceItem(namespace, request, QualifiedNameKind.QUALIFIED));
                }
                break;
            case GLOBAL:
                autoCompleteGlobals(completionResult, request);
                break;
            case EXPRESSION:
                autoCompleteNamespaces(completionResult, request);
                autoCompleteExpression(completionResult, request);
                break;
            case HTML:
            case OPEN_TAG:
                completionResult.add(new PHPCompletionItem.TagItem("<?php", 1, request)); //NOI18N
                completionResult.add(new PHPCompletionItem.TagItem("<?=", 2, request)); //NOI18N
                break;
            case NEW_CLASS:
                autoCompleteNamespaces(completionResult, request);
                autoCompleteNewClass(completionResult, request);
                break;
            case CLASS_NAME:
                autoCompleteNamespaces(completionResult, request);
                autoCompleteClassNames(completionResult, request, false);
                break;
            case INTERFACE_NAME:
                autoCompleteNamespaces(completionResult, request);
                autoCompleteInterfaceNames(completionResult, request);
                break;
            case USE_KEYWORD: {
                CodeStyle codeStyle = CodeStyle.get(request.result.getSnapshot().getSource().getDocument(caseSensitive));
                autoCompleteAfterUses(
                        completionResult,
                        request,
                        codeStyle.startUseWithNamespaceSeparator() ? QualifiedNameKind.FULLYQUALIFIED : QualifiedNameKind.QUALIFIED,
                        false);
                break;
            }
            case USE_CONST_KEYWORD: {
                CodeStyle codeStyle = CodeStyle.get(request.result.getSnapshot().getSource().getDocument(caseSensitive));
                autoCompleteAfterUsesConst(
                        completionResult,
                        request,
                        codeStyle.startUseWithNamespaceSeparator() ? QualifiedNameKind.FULLYQUALIFIED : QualifiedNameKind.QUALIFIED);
                break;
            }
            case USE_FUNCTION_KEYWORD: {
                CodeStyle codeStyle = CodeStyle.get(request.result.getSnapshot().getSource().getDocument(caseSensitive));
                autoCompleteAfterUsesFunction(
                        completionResult,
                        request,
                        codeStyle.startUseWithNamespaceSeparator() ? QualifiedNameKind.FULLYQUALIFIED : QualifiedNameKind.QUALIFIED);
                break;
            }
            case USE_TRAITS:
                CodeStyle traitCodeStyle = CodeStyle.get(request.result.getSnapshot().getSource().getDocument(caseSensitive));
                autoCompleteAfterUseTrait(
                        completionResult,
                        request,
                        traitCodeStyle.startUseWithNamespaceSeparator() ? QualifiedNameKind.FULLYQUALIFIED : QualifiedNameKind.QUALIFIED);
                break;
            case TYPE_NAME:
                autoCompleteNamespaces(completionResult, request);
                autoCompleteTypeNames(completionResult, request);
                break;
            case STRING:
                // LOCAL VARIABLES
                completionResult.addAll(getVariableProposals(request, null));
                // are we in class?
                if (request.prefix.length() == 0 || startsWith(PHP_CLASS_KEYWORD_THIS, request.prefix)) {
                    final ClassDeclaration classDecl = findEnclosingClass(info, caretOffset);
                    if (classDecl != null) {
                        final String className = CodeUtils.extractClassName(classDecl);
                        if (className != null) {
                            completionResult.add(new PHPCompletionItem.ClassScopeKeywordItem(className, PHP_CLASS_KEYWORD_THIS, request));
                        }
                    }
                }
                break;
            case CLASS_MEMBER:
                autoCompleteClassMembers(completionResult, request, false);
                break;
            case STATIC_CLASS_MEMBER:
                autoCompleteClassMembers(completionResult, request, true);
                break;
            case PHPDOC:
                PHPDOCCodeCompletion.complete(completionResult, request);
                if (PHPDOCCodeCompletion.isTypeCtx(request)) {
                    autoCompleteTypeNames(completionResult, request);
                    autoCompleteNamespaces(completionResult, request);
                }
                break;
            case CLASS_CONTEXT_KEYWORDS:
                autoCompleteInClassContext(info, caretOffset, completionResult, request);
                break;
            case INTERFACE_CONTEXT_KEYWORDS:
                autoCompleteInInterfaceContext(completionResult, request);
                break;
            case METHOD_NAME:
                autoCompleteMethodName(info, caretOffset, completionResult, request);
                break;
            case IMPLEMENTS:
                autoCompleteKeywords(completionResult, request, Collections.singletonList("implements")); //NOI18N
                break;
            case EXTENDS:
                autoCompleteKeywords(completionResult, request, Collections.singletonList("extends")); //NOI18N
                break;
            case INHERITANCE:
                autoCompleteKeywords(completionResult, request, INHERITANCE_KEYWORDS);
                break;
            case THROW:
                autoCompleteNamespaces(completionResult, request);
                autoCompleteExceptions(completionResult, request, true);
                break;
            case CATCH:
                autoCompleteNamespaces(completionResult, request);
                autoCompleteExceptions(completionResult, request, false);
                break;
            case CLASS_MEMBER_IN_STRING:
                autoCompleteClassFields(completionResult, request);
                break;
            case SERVER_ENTRY_CONSTANTS:
                //TODO: probably better PHPCompletionItem instance should be used
                //autoCompleteMagicItems(proposals, request, PredefinedSymbols.SERVER_ENTRY_CONSTANTS);
                for (String keyword : PredefinedSymbols.SERVER_ENTRY_CONSTANTS) {
                    if (keyword.startsWith(request.prefix)) {
                        completionResult.add(new PHPCompletionItem.KeywordItem(keyword, request) {
                            @Override
                            public ImageIcon getIcon() {
                                return null;
                            }
                        });
                    }
                }

                break;
            default:
                assert false : context;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long time = System.currentTimeMillis() - startTime;
            LOGGER.fine(String.format("complete() took %d ms, result contains %d items", time, completionResult.getItems().size()));
        }

        return completionResult;
    }

    private List<ElementFilter> createTypeFilter(final ClassDeclaration enclosingClass) {
        List<ElementFilter> superTypeIndices = new ArrayList<>();
        Expression superClass = enclosingClass.getSuperClass();
        if (superClass != null) {
            String superClsName = CodeUtils.extractUnqualifiedSuperClassName(enclosingClass);
            superTypeIndices.add(ElementFilter.forSuperClassName(QualifiedName.create(superClsName)));
        }
        List<Expression> interfaces = enclosingClass.getInterfaes();
        Set<QualifiedName> superIfaceNames = new HashSet<>();
        for (Expression identifier : interfaces) {
            String ifaceName = CodeUtils.extractUnqualifiedName(identifier);
            if (ifaceName != null) {
                superIfaceNames.add(QualifiedName.create(ifaceName));
            }
        }
        if (!superIfaceNames.isEmpty()) {
            superTypeIndices.add(ElementFilter.forSuperInterfaceNames(superIfaceNames));
        }
        return superTypeIndices;
    }

    private void autoCompleteMethodName(ParserResult info, int caretOffset, final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request) {
        ClassDeclaration enclosingClass = findEnclosingClass(info, lexerToASTOffset(info, caretOffset));
        if (enclosingClass != null) {
            List<ElementFilter> superTypeIndices = createTypeFilter(enclosingClass);
            String clsName = enclosingClass.getName().getName();
            NamespaceScope namespaceScope = ModelUtils.getNamespaceScope(request.result.getModel().getFileScope(), request.anchor);
            String fullyQualifiedClassName = VariousUtils.qualifyTypeNames(clsName, request.anchor, namespaceScope);
            if (fullyQualifiedClassName != null) {
                final FileObject fileObject = request.result.getSnapshot().getSource().getFileObject();
                final ElementFilter classFilter = ElementFilter.allOf(
                        ElementFilter.forFiles(fileObject), ElementFilter.allOf(superTypeIndices));
                Set<ClassElement> classes = classFilter.filter(request.index.getClasses(NameKind.exact(fullyQualifiedClassName)));
                for (ClassElement classElement : classes) {
                    ElementFilter methodFilter = ElementFilter.allOf(
                            ElementFilter.forExcludedNames(toNames(request.index.getDeclaredMethods(classElement)), PhpElementKind.METHOD),
                            ElementFilter.forName(NameKind.caseInsensitivePrefix(QualifiedName.create(request.prefix))));
                    Set<MethodElement> accessibleMethods = methodFilter.filter(request.index.getAccessibleMethods(classElement, classElement));
                    for (MethodElement method : accessibleMethods) {
                        if (!method.isFinal()) {
                            completionResult.add(PHPCompletionItem.MethodDeclarationItem.forMethodName(method, request));
                        }
                    }
                    Set<MethodElement> magicMethods = methodFilter.filter(request.index.getAccessibleMagicMethods(classElement));
                    for (MethodElement magicMethod : magicMethods) {
                        if (magicMethod != null) {
                            completionResult.add(PHPCompletionItem.MethodDeclarationItem.forMethodName(magicMethod, request));
                        }
                    }
                    break;
                }
            }
        }

    }

    /**
     * Finding item after new keyword.
     *
     * @param completionResult
     * @param request
     */
    private void autoCompleteNewClass(final PHPCompletionResult completionResult, PHPCompletionItem.CompletionRequest request) {
        // At first find all classes that match the prefix
        final boolean isCamelCase = isCamelCaseForTypeNames(request.prefix);
        final QualifiedName prefix = QualifiedName.create(request.prefix).toNotFullyQualified();
        final NameKind nameQuery = NameKind.create(request.prefix,
                isCamelCase ? Kind.CAMEL_CASE : Kind.CASE_INSENSITIVE_PREFIX);
        Model model = request.result.getModel();
        Set<ClassElement> classes = request.index.getClasses(nameQuery, ModelUtils.getAliasedNames(model, request.anchor), Trait.ALIAS);

        if (!classes.isEmpty()) {
            completionResult.setFilterable(false);
        }
        boolean addedExact = false;
        final NameKind query;
        if (classes.size() == 1) {
            ClassElement clazz = (ClassElement) classes.toArray()[0];
            if (!clazz.isAbstract()) {
                // if there is only once class find constructors for it
                query = isCamelCase ? NameKind.create(prefix.toString(), QuerySupport.Kind.CAMEL_CASE) : NameKind.caseInsensitivePrefix(prefix);
                autoCompleteConstructors(completionResult, request, model, query);
            }
        } else {
            for (ClassElement clazz : classes) {
                if (!clazz.isAbstract()) {
                    // check whether the prefix is exactly the class
                    NamespaceScope namespaceScope = ModelUtils.getNamespaceScope(request.result.getModel().getFileScope(), request.anchor);
                    String fqPrefixName = VariousUtils.qualifyTypeNames(request.prefix, request.anchor, namespaceScope);
                    if (clazz.getFullyQualifiedName().toString().equals(fqPrefixName)) {
                        // find constructor of the class
                        if (!addedExact) { // add the constructors only once
                            autoCompleteConstructors(completionResult, request, model, NameKind.exact(fqPrefixName));
                            addedExact = true;
                        }
                    } else {
                        // put to the cc just the class
                        completionResult.add(new PHPCompletionItem.ClassItem(clazz, request, false, null));
                    }
                }
            }
        }
    }

    private void autoCompleteConstructors(final PHPCompletionResult completionResult, final PHPCompletionItem.CompletionRequest request, final Model model, final NameKind query) {
        Set<AliasedName> aliasedNames = ModelUtils.getAliasedNames(model, request.anchor);
        Set<MethodElement> constructors = request.index.getConstructors(query, aliasedNames, Trait.ALIAS);
        for (MethodElement constructor : constructors) {
            for (final PHPCompletionItem.NewClassItem newClassItem : PHPCompletionItem.NewClassItem.getNewClassItems(constructor, request)) {
                completionResult.add(newClassItem);
            }
        }
    }

    private void autoCompleteExceptions(final PHPCompletionResult completionResult, PHPCompletionItem.CompletionRequest request, boolean withConstructors) {
        final boolean isCamelCase = isCamelCaseForTypeNames(request.prefix);
        final NameKind nameQuery = NameKind.create(request.prefix, isCamelCase ? Kind.CAMEL_CASE : Kind.CASE_INSENSITIVE_PREFIX);
        final Set<ClassElement> classes = request.index.getClasses(nameQuery);
        final Model model = request.result.getModel();
        final Set<QualifiedName> constructorClassNames = new HashSet<>();
        for (ClassElement classElement : classes) {
            if (isExceptionClass(classElement)) {
                completionResult.add(new PHPCompletionItem.ClassItem(classElement, request, false, null));
                if (withConstructors) {
                    constructorClassNames.add(classElement.getFullyQualifiedName());
                }
                continue;
            }
            if (classElement.getSuperClassName() != null) {
                Set<ClassElement> inheritedClasses = request.index.getInheritedClasses(classElement);
                for (ClassElement inheritedClass : inheritedClasses) {
                    if (isExceptionClass(inheritedClass)) {
                        completionResult.add(new PHPCompletionItem.ClassItem(classElement, request, false, null));
                        if (withConstructors) {
                            constructorClassNames.add(inheritedClass.getFullyQualifiedName());
                        }
                        break;
                    }
                }
            }
        }
        for (QualifiedName qualifiedName : constructorClassNames) {
            autoCompleteConstructors(completionResult, request, model, NameKind.exact(qualifiedName));
        }
    }

    private boolean isExceptionClass(ClassElement classElement) {
        return classElement.getFullyQualifiedName().toString().equals(EXCEPTION_CLASS_NAME);
    }

    private void autoCompleteClassNames(final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request, boolean endWithDoubleColon) {
        autoCompleteClassNames(completionResult, request, endWithDoubleColon, null);
    }

    private void autoCompleteClassNames(final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request, boolean endWithDoubleColon, QualifiedNameKind kind) {
        final boolean isCamelCase = isCamelCaseForTypeNames(request.prefix);
        final NameKind nameQuery = NameKind.create(request.prefix,
                isCamelCase ? Kind.CAMEL_CASE : Kind.CASE_INSENSITIVE_PREFIX);
        Model model = request.result.getModel();
        Set<ClassElement> classes = request.index.getClasses(nameQuery, ModelUtils.getAliasedNames(model, request.anchor), Trait.ALIAS);

        if (!classes.isEmpty()) {
            completionResult.setFilterable(false);
        }
        for (ClassElement clazz : classes) {
            completionResult.add(new PHPCompletionItem.ClassItem(clazz, request, endWithDoubleColon, kind));
        }
    }

    private void autoCompleteInterfaceNames(final PHPCompletionResult completionResult, PHPCompletionItem.CompletionRequest request) {
        autoCompleteInterfaceNames(completionResult, request, null);
    }

    private void autoCompleteInterfaceNames(final PHPCompletionResult completionResult, PHPCompletionItem.CompletionRequest request, QualifiedNameKind kind) {
        final boolean isCamelCase = isCamelCaseForTypeNames(request.prefix);
        final NameKind nameQuery = NameKind.create(request.prefix,
                isCamelCase ? Kind.CAMEL_CASE : Kind.CASE_INSENSITIVE_PREFIX);

        Model model = request.result.getModel();
        Set<InterfaceElement> interfaces = request.index.getInterfaces(nameQuery, ModelUtils.getAliasedNames(model, request.anchor), Trait.ALIAS);
        if (!interfaces.isEmpty()) {
            completionResult.setFilterable(false);
        }

        for (InterfaceElement iface : interfaces) {
            completionResult.add(new PHPCompletionItem.InterfaceItem(iface, request, kind, false));
        }
    }

    private void autoCompleteTypeNames(final PHPCompletionResult completionResult, PHPCompletionItem.CompletionRequest request) {
        autoCompleteTypeNames(completionResult, request, null, false);
    }

    private void autoCompleteAfterUseTrait(final PHPCompletionResult completionResult, final PHPCompletionItem.CompletionRequest request, final QualifiedNameKind kind) {
        Set<NamespaceElement> namespaces = request.index.getNamespaces(
                NameKind.caseInsensitivePrefix(QualifiedName.create(request.prefix).toNotFullyQualified()));
        for (NamespaceElement namespace : namespaces) {
            completionResult.add(new PHPCompletionItem.NamespaceItem(namespace, request, QualifiedNameKind.FULLYQUALIFIED));
        }
        final NameKind nameQuery = NameKind.caseInsensitivePrefix(request.prefix);
        for (TraitElement trait : request.index.getTraits(nameQuery)) {
            completionResult.add(new PHPCompletionItem.TraitItem(trait, request));
        }
    }

    private void autoCompleteAfterUses(
            final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request,
            QualifiedNameKind kind,
            boolean endWithDoubleColon) {
        Set<NamespaceElement> namespaces = request.index.getNamespaces(
                NameKind.caseInsensitivePrefix(QualifiedName.create(request.prefix).toNotFullyQualified()));
        for (NamespaceElement namespace : namespaces) {
            completionResult.add(new PHPCompletionItem.NamespaceItem(namespace, request, kind));
        }
        final NameKind nameQuery = NameKind.caseInsensitivePrefix(request.prefix);
        for (ClassElement clazz : request.index.getClasses(nameQuery)) {
            completionResult.add(new PHPCompletionItem.ClassItem(clazz, request, endWithDoubleColon, kind));
        }
        for (InterfaceElement iface : request.index.getInterfaces(nameQuery)) {
            completionResult.add(new PHPCompletionItem.InterfaceItem(iface, request, kind, false));
        }
    }

    private void autoCompleteAfterUsesConst(
            final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request,
            QualifiedNameKind kind) {
        Set<NamespaceElement> namespaces = request.index.getNamespaces(
                NameKind.caseInsensitivePrefix(QualifiedName.create(request.prefix).toNotFullyQualified()));
        for (NamespaceElement namespace : namespaces) {
            completionResult.add(new PHPCompletionItem.NamespaceItem(namespace, request, kind));
        }
        final NameKind nameQuery = NameKind.caseInsensitivePrefix(request.prefix);
        for (ConstantElement constant : request.index.getConstants(nameQuery)) {
            completionResult.add(new PHPCompletionItem.ConstantItem(constant, request));
        }
    }

    private void autoCompleteAfterUsesFunction(
            final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request,
            QualifiedNameKind kind) {
        Set<NamespaceElement> namespaces = request.index.getNamespaces(NameKind.caseInsensitivePrefix(QualifiedName.create(request.prefix).toNotFullyQualified()));
        for (NamespaceElement namespace : namespaces) {
            completionResult.add(new PHPCompletionItem.NamespaceItem(namespace, request, kind));
        }
        final NameKind nameQuery = NameKind.caseInsensitivePrefix(request.prefix);
        for (FunctionElement function : request.index.getFunctions(nameQuery)) {
            List<PHPCompletionItem.FunctionElementItem> items = PHPCompletionItem.FunctionElementItem.getItems(function, request);
            for (PHPCompletionItem.FunctionElementItem item : items) {
                completionResult.add(item);
            }
        }
    }

    private void autoCompleteTypeNames(
            final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request,
            QualifiedNameKind kind,
            boolean endWithDoubleColon) {
        if (request.prefix.trim().length() > 0) {
            autoCompleteClassNames(completionResult, request, endWithDoubleColon, kind);
            autoCompleteInterfaceNames(completionResult, request, kind);
        } else {
            Model model = request.result.getModel();
            Set<AliasedName> aliasedNames = ModelUtils.getAliasedNames(model, request.anchor);
            Collection<PhpElement> allTopLevel = request.index.getTopLevelElements(NameKind.empty(), aliasedNames, Trait.ALIAS);
            for (PhpElement element : allTopLevel) {
                if (element instanceof ClassElement) {
                    completionResult.add(new PHPCompletionItem.ClassItem((ClassElement) element, request, endWithDoubleColon, kind));
                } else if (element instanceof InterfaceElement) {
                    completionResult.add(new PHPCompletionItem.InterfaceItem((InterfaceElement) element, request, kind, endWithDoubleColon));
                }
            }
        }
        for (String construct : PHP_LANGUAGE_CONSTRUCTS_FOR_TYPE_HINTS) {
            if (startsWith(construct, request.prefix)) {
                completionResult.add(new PHPCompletionItem.LanguageConstructForTypeHint(construct, request));
            }
        }
    }

    private void autoCompleteKeywords(final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request, List<String> keywordList) {
        for (String keyword : keywordList) {
            if (keyword.startsWith(request.prefix)) {
                completionResult.add(new PHPCompletionItem.KeywordItem(keyword, request));
            }
        }

    }

    private void autoCompleteNamespaces(final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request) {
        autoCompleteNamespaces(completionResult, request, null);
    }

    private void autoCompleteNamespaces(final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request, QualifiedNameKind kind) {
        final QualifiedName prefix = QualifiedName.create(request.prefix).toNotFullyQualified();
        Model model = request.result.getModel();
        Set<NamespaceElement> namespaces = request.index.getNamespaces(NameKind.caseInsensitivePrefix(prefix),
                ModelUtils.getAliasedNames(model, request.anchor), Trait.ALIAS);
        for (NamespaceElement namespace : namespaces) {
            completionResult.add(new PHPCompletionItem.NamespaceItem(namespace, request, kind));
        }
    }

    private void autoCompleteInInterfaceContext(final PHPCompletionResult completionResult, final PHPCompletionItem.CompletionRequest request) {
        autoCompleteKeywords(completionResult, request, INTERFACE_CONTEXT_KEYWORD_PROPOSAL);
    }

    private void autoCompleteInClassContext(
            ParserResult info,
            int caretOffset,
            final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request) {
        TokenHierarchy<?> th = info.getSnapshot().getTokenHierarchy();
        TokenSequence<PHPTokenId> tokenSequence = th.tokenSequence(PHPTokenId.language());
        assert tokenSequence != null;

        tokenSequence.move(caretOffset);
        boolean offerMagicAndInherited = true;
        if (!(!tokenSequence.moveNext() && !tokenSequence.movePrevious())) {
            Token<PHPTokenId> token = tokenSequence.token();
            int tokenIdOffset = tokenSequence.token().offset(th);
            offerMagicAndInherited = !CompletionContextFinder.lineContainsAny(token, caretOffset - tokenIdOffset, tokenSequence, Arrays.asList(new PHPTokenId[]{
                        PHPTokenId.PHP_PRIVATE,
                        PHPTokenId.PHP_PUBLIC,
                        PHPTokenId.PHP_PROTECTED,
                        PHPTokenId.PHP_ABSTRACT,
                        PHPTokenId.PHP_VAR,
                        PHPTokenId.PHP_STATIC,
                        PHPTokenId.PHP_CONST
                    }));
        }

        autoCompleteKeywords(completionResult, request, CLASS_CONTEXT_KEYWORD_PROPOSAL);
        if (offerMagicAndInherited) {
            ClassDeclaration enclosingClass = findEnclosingClass(info, lexerToASTOffset(info, caretOffset));
            if (enclosingClass != null) {
                List<ElementFilter> superTypeIndices = createTypeFilter(enclosingClass);
                String clsName = enclosingClass.getName().getName();
                NamespaceScope namespaceScope = ModelUtils.getNamespaceScope(request.result.getModel().getFileScope(), request.anchor);
                String fullyQualifiedClassName = VariousUtils.qualifyTypeNames(clsName, request.anchor, namespaceScope);
                if (fullyQualifiedClassName != null) {
                    final FileObject fileObject = request.result.getSnapshot().getSource().getFileObject();
                    final ElementFilter classFilter = ElementFilter.allOf(
                            ElementFilter.forFiles(fileObject), ElementFilter.allOf(superTypeIndices));
                    Set<ClassElement> classes = classFilter.filter(request.index.getClasses(NameKind.exact(fullyQualifiedClassName)));
                    for (ClassElement classElement : classes) {
                        ElementFilter methodFilter = ElementFilter.allOf(
                                ElementFilter.forExcludedNames(toNames(request.index.getDeclaredMethods(classElement)), PhpElementKind.METHOD),
                                ElementFilter.forName(NameKind.caseInsensitivePrefix(QualifiedName.create(request.prefix))));
                        Set<MethodElement> accessibleMethods = methodFilter.filter(request.index.getAccessibleMethods(classElement, classElement));
                        for (MethodElement method : accessibleMethods) {
                            if (!method.isFinal()) {
                                completionResult.add(PHPCompletionItem.MethodDeclarationItem.getDeclarationItem(method, request));
                            }
                        }
                        Set<MethodElement> magicMethods = methodFilter.filter(request.index.getAccessibleMagicMethods(classElement));
                        for (MethodElement magicMethod : magicMethods) {
                            if (magicMethod != null) {
                                completionResult.add(PHPCompletionItem.MethodDeclarationItem.getDeclarationItem(magicMethod, request));
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private static Set<String> toNames(Set<? extends PhpElement> elements) {
        Set<String> names = new HashSet<>();
        for (PhpElement elem : elements) {
            names.add(elem.getName());
        }
        return names;
    }

    private void autoCompleteClassMembers(
            final PHPCompletionResult completionResult,
            PHPCompletionItem.CompletionRequest request,
            boolean staticContext) {
        // TODO: remove duplicate/redundant code from here

        TokenHierarchy<?> th = request.info.getSnapshot().getTokenHierarchy();
        TokenSequence<PHPTokenId> tokenSequence = LexUtilities.getPHPTokenSequence(th, request.anchor);

        if (tokenSequence == null) {
            return;
        }

        tokenSequence.move(request.anchor);
        if (tokenSequence.movePrevious()) {
            boolean instanceContext = !staticContext;

            if (tokenSequence.token().id() != PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM
                    && tokenSequence.token().id() != PHPTokenId.PHP_OBJECT_OPERATOR) {
                tokenSequence.movePrevious();
            }
            tokenSequence.movePrevious();
            if (tokenSequence.token().id() == PHPTokenId.WHITESPACE) {
                tokenSequence.movePrevious();
            }
            String varName = tokenSequence.token().text().toString();
            tokenSequence.moveNext();

            List<String> invalidProposalsForClsMembers = INVALID_PROPOSALS_FOR_CLS_MEMBERS;
            Model model = request.result.getModel();

            if (staticContext && varName.startsWith("$")) {
                return;
            }
            Collection<? extends TypeScope> types = ModelUtils.resolveTypeAfterReferenceToken(model, tokenSequence, request.anchor);
            boolean selfContext = false;
            boolean staticLateBindingContext = false;
            switch (varName) {
                case "self": //NOI18N
                    staticContext = true;
                    selfContext = true;
                    break;
                case "parent": //NOI18N
                    invalidProposalsForClsMembers = Collections.emptyList();
                    staticContext = true;
                    instanceContext = true;
                    break;
                case "$this": //NOI18N
                    staticContext = false;
                    instanceContext = true;
                    break;
                case "static": //NOI18N
                    staticContext = true;
                    instanceContext = false;
                    staticLateBindingContext = true;
                    break;
                default:
                    // no-op
            }

            if (types != null) {
                TypeElement enclosingType = getEnclosingType(request, types);
                Set<PhpElement> duplicateElementCheck = new HashSet<>();
                for (TypeScope typeScope : types) {
                    final StaticOrInstanceMembersFilter staticFlagFilter =
                            new StaticOrInstanceMembersFilter(staticContext, instanceContext, selfContext, staticLateBindingContext);

                    final ElementFilter methodsFilter = ElementFilter.allOf(
                            ElementFilter.forKind(PhpElementKind.METHOD),
                            ElementFilter.forName(NameKind.caseInsensitivePrefix(request.prefix)),
                            staticFlagFilter,
                            ElementFilter.forExcludedNames(invalidProposalsForClsMembers, PhpElementKind.METHOD),
                            ElementFilter.forInstanceOf(MethodElement.class));
                    final ElementFilter fieldsFilter = ElementFilter.allOf(
                            ElementFilter.forKind(PhpElementKind.FIELD),
                            ElementFilter.forName(NameKind.caseInsensitivePrefix(request.prefix)),
                            staticFlagFilter,
                            ElementFilter.forInstanceOf(FieldElement.class));
                    final ElementFilter constantsFilter = ElementFilter.allOf(
                            ElementFilter.forKind(PhpElementKind.TYPE_CONSTANT),
                            ElementFilter.forName(NameKind.caseInsensitivePrefix(request.prefix)),
                            ElementFilter.forInstanceOf(TypeConstantElement.class));
                    for (final PhpElement phpElement : request.index.getAccessibleTypeMembers(typeScope, enclosingType)) {
                        if (duplicateElementCheck.add(phpElement)) {
                            if (methodsFilter.isAccepted(phpElement)) {
                                MethodElement method = (MethodElement) phpElement;
                                List<MethodElementItem> items = PHPCompletionItem.MethodElementItem.getItems(method, request);
                                for (MethodElementItem methodItem : items) {
                                    completionResult.add(methodItem);
                                }
                            } else if (fieldsFilter.isAccepted(phpElement)) {
                                FieldElement field = (FieldElement) phpElement;
                                FieldItem fieldItem = PHPCompletionItem.FieldItem.getItem(field, request);
                                completionResult.add(fieldItem);
                            } else if (staticContext && constantsFilter.isAccepted(phpElement)) {
                                TypeConstantElement constant = (TypeConstantElement) phpElement;
                                TypeConstantItem constantItem = PHPCompletionItem.TypeConstantItem.getItem(constant, request);
                                completionResult.add(constantItem);
                            }
                        }
                    }
                    if (staticContext) {
                        Set<TypeConstantElement> magicConstants = constantsFilter.filter(request.index.getAccessibleMagicConstants(typeScope));
                        for (TypeConstantElement magicConstant : magicConstants) {
                            if (magicConstant != null) {
                                completionResult.add(PHPCompletionItem.TypeConstantItem.getItem(magicConstant, request));
                            }
                        }
                    }
                }
            }
        }
    }

    private void autoCompleteClassFields(final PHPCompletionResult completionResult, final PHPCompletionItem.CompletionRequest request) {
        TokenHierarchy<?> th = request.info.getSnapshot().getTokenHierarchy();
        TokenSequence<PHPTokenId> tokenSequence = LexUtilities.getPHPTokenSequence(th, request.anchor);
        Model model = request.result.getModel();
        Collection<? extends TypeScope> types = ModelUtils.resolveTypeAfterReferenceToken(model, tokenSequence, request.anchor);
        final ElementFilter fieldsFilter = ElementFilter.allOf(
                ElementFilter.forKind(PhpElementKind.FIELD),
                ElementFilter.forName(NameKind.caseInsensitivePrefix(request.prefix)),
                ElementFilter.forInstanceOf(FieldElement.class));
        if (types != null) {
            TypeElement enclosingType = getEnclosingType(request, types);
            for (TypeScope typeScope : types) {
                for (final PhpElement phpElement : request.index.getAccessibleTypeMembers(typeScope, enclosingType)) {
                    if (fieldsFilter.isAccepted(phpElement)) {
                        FieldElement field = (FieldElement) phpElement;
                        FieldItem fieldItem = PHPCompletionItem.FieldItem.getItem(field, request);
                        completionResult.add(fieldItem);
                    }
                }
            }
        }
    }

    private TypeElement getEnclosingType(CompletionRequest request, Collection<? extends TypeScope> types) {
        final TypeDeclaration enclosingType = findEnclosingType(request.info, lexerToASTOffset(request.result, request.anchor));
        final String enclosingTypeName = (enclosingType != null) ? CodeUtils.extractTypeName(enclosingType) : null;
        NamespaceScope namespaceScope = ModelUtils.getNamespaceScope(request.result.getModel().getFileScope(), request.anchor);
        final String enclosingFQTypeName = VariousUtils.qualifyTypeNames(enclosingTypeName, request.anchor, namespaceScope);
        final NameKind enclosingTypeNameKind = (enclosingFQTypeName != null && !enclosingFQTypeName.trim().isEmpty()) ? NameKind.exact(enclosingFQTypeName) : null;
        Set<FileObject> preferedFileObjects = new HashSet<>();
        Set<TypeElement> enclosingTypes = null;
        FileObject currentFile = request.result.getSnapshot().getSource().getFileObject();
        if (currentFile != null) {
            preferedFileObjects.add(currentFile);
        }
        for (TypeScope typeScope : types) {
            final FileObject fileObject = typeScope.getFileObject();
            if (fileObject != null) {
                preferedFileObjects.add(fileObject);
            }
            if (enclosingTypeNameKind != null && enclosingTypes == null) {
                if (enclosingTypeNameKind.matchesName(typeScope)) {
                    enclosingTypes = Collections.<TypeElement>singleton((TypeElement) typeScope);
                }
            }
        }
        if (enclosingTypeNameKind != null && enclosingTypes == null) {
            final ElementFilter forFiles = ElementFilter.forFiles(preferedFileObjects.toArray(new FileObject[preferedFileObjects.size()]));
            Set<TypeElement> indexTypes = forFiles.prefer(request.index.getTypes(enclosingTypeNameKind));
            if (!indexTypes.isEmpty()) {
                enclosingTypes = new HashSet<>(indexTypes);
            }
        }
        return (enclosingTypes == null || enclosingTypes.isEmpty()) ? null : enclosingTypes.iterator().next();
    }

    private static TypeDeclaration findEnclosingType(ParserResult info, int offset) {
        List<ASTNode> nodes = NavUtils.underCaret(info, offset);
        for (ASTNode node : nodes) {
            if (node instanceof TypeDeclaration && node.getEndOffset() != offset) {
                return (TypeDeclaration) node;
            }
        }
        return null;
    }

    private static ClassDeclaration findEnclosingClass(ParserResult info, int offset) {
        List<ASTNode> nodes = NavUtils.underCaret(info, offset);
        for (ASTNode node : nodes) {
            if (node instanceof ClassDeclaration && node.getEndOffset() != offset) {
                return (ClassDeclaration) node;
            }
        }
        return null;
    }

    private void autoCompleteExpression(final PHPCompletionResult completionResult, PHPCompletionItem.CompletionRequest request) {
        // KEYWORDS
        for (String keyword : PHP_KEYWORDS.keySet()) {
            if (startsWith(keyword, request.prefix)) {
                completionResult.add(new PHPCompletionItem.KeywordItem(keyword, request));
            }
        }

        for (String keyword : PHP_LANGUAGE_CONSTRUCTS_WITH_QUOTES) {
            if (startsWith(keyword, request.prefix)) {
                completionResult.add(new PHPCompletionItem.LanguageConstructWithQuotesItem(keyword, request));
            }
        }

        for (String construct : PHP_LANGUAGE_CONSTRUCTS_WITH_PARENTHESES) {
            if (startsWith(construct, request.prefix)) {
                completionResult.add(new PHPCompletionItem.LanguageConstructWithParenthesesItem(construct, request));
            }
        }

        for (String construct : PHP_LANGUAGE_CONSTRUCTS_WITH_SEMICOLON) {
            if (startsWith(construct, request.prefix)) {
                completionResult.add(new PHPCompletionItem.LanguageConstructWithSemicolonItem(construct, request));
            }
        }

        final boolean offerGlobalVariables = OptionsUtils.codeCompletionVariablesScope().equals(VariablesScope.ALL);
        final boolean isCamelCase = isCamelCaseForTypeNames(request.prefix);
        final NameKind prefix = NameKind.create(request.prefix,
                isCamelCase ? Kind.CAMEL_CASE : Kind.CASE_INSENSITIVE_PREFIX);

        final Set<VariableElement> globalVariables = new HashSet<>();

        Model model = request.result.getModel();
        Set<AliasedName> aliasedNames = ModelUtils.getAliasedNames(model, request.anchor);

        for (final PhpElement element : request.index.getTopLevelElements(prefix, aliasedNames, Trait.ALIAS)) {
            if (element instanceof FunctionElement) {
                for (final PHPCompletionItem.FunctionElementItem functionItem
                        : PHPCompletionItem.FunctionElementItem.getItems((FunctionElement) element, request)) {
                    completionResult.add(functionItem);
                }
            } else if (element instanceof ClassElement) {
                completionResult.add(new PHPCompletionItem.ClassItem((ClassElement) element, request, true, null));
            } else if (element instanceof InterfaceElement) {
                completionResult.add(new PHPCompletionItem.InterfaceItem((InterfaceElement) element, request, true));
            } else if (offerGlobalVariables && element instanceof VariableElement) {
                globalVariables.add((VariableElement) element);
            } else if (element instanceof ConstantElement) {
                completionResult.add(new PHPCompletionItem.ConstantItem((ConstantElement) element, request));
            }
        }
        FileObject fileObject = request.result.getSnapshot().getSource().getFileObject();
        final ElementFilter forCurrentFile = ElementFilter.forFiles(fileObject);
        completionResult.addAll(getVariableProposals(request, forCurrentFile.reverseFilter(globalVariables)));

        // Special keywords applicable only inside a class
        final ClassDeclaration classDecl = findEnclosingClass(request.info, lexerToASTOffset(request.result, request.anchor));
        if (classDecl != null) {
            final String className = CodeUtils.extractClassName(classDecl);
            if (className != null) {
                for (final String keyword : PHP_CLASS_KEYWORDS) {
                    if (startsWith(keyword, request.prefix)) {
                        completionResult.add(new PHPCompletionItem.ClassScopeKeywordItem(className, keyword, request));
                    }
                }
            }
        }
    }

    private void autoCompleteGlobals(final PHPCompletionResult completionResult, PHPCompletionItem.CompletionRequest request) {
        if (OptionsUtils.codeCompletionVariablesScope().equals(VariablesScope.ALL)) {
            final CaseInsensitivePrefix prefix = NameKind.caseInsensitivePrefix(QualifiedName.create(request.prefix));
            for (VariableElement variableElement : request.index.getTopLevelVariables(prefix)) {
                completionResult.add(new PHPCompletionItem.VariableItem(variableElement, request));
            }
        }
    }

    /**
     * @param globalVariables (can be bull) if null then will be looked up in
     * index
     */
    private Collection<CompletionProposal> getVariableProposals(final CompletionRequest request, Set<VariableElement> globalVariables) {
        final Map<String, CompletionProposal> proposals = new LinkedHashMap<>();
        Model model = request.result.getModel();
        VariableScope variableScope = model.getVariableScope(request.anchor);
        if (variableScope != null) {
            if (variableScope instanceof NamespaceScope) {
                if (globalVariables == null) {
                    FileObject fileObject = request.result.getSnapshot().getSource().getFileObject();
                    final ElementFilter forCurrentFile = ElementFilter.forFiles(fileObject);
                    globalVariables = forCurrentFile.reverseFilter(request.index.getTopLevelVariables(NameKind.caseInsensitivePrefix(QualifiedName.create(request.prefix))));
                }

                for (final VariableElement globalVariable : globalVariables) {
                    proposals.put(globalVariable.getName(), new PHPCompletionItem.VariableItem(globalVariable, request));
                }
            }
            Collection<? extends VariableName> declaredVariables = ModelUtils.filter(variableScope.getDeclaredVariables(),
                    nameKind, request.prefix);
            final int caretOffset = request.anchor + request.prefix.length();
            for (VariableName varName : declaredVariables) {
                final FileObject realFileObject = varName.getRealFileObject();
                if (realFileObject != null || varName.getNameRange().getEnd() < caretOffset) {
                    final String name = varName.getName();
                    String notDollaredName = name.startsWith("$") ? name.substring(1) : name;
                    if (PredefinedSymbols.SUPERGLOBALS.contains(notDollaredName)) {
                        continue;
                    }
                    if (varName.representsThis()) {
                        continue;
                    }
                    final Collection<? extends String> typeNames = varName.getTypeNames(request.anchor);
                    String typeName = typeNames.size() > 1 ? Type.MIXED : ModelUtils.getFirst(typeNames);
                    final Set<QualifiedName> qualifiedNames = typeName != null
                            ? Collections.singleton(QualifiedName.create(typeName))
                            : Collections.<QualifiedName>emptySet();
                    if (realFileObject != null) {
                        //#183928 -  Extend model to allow CTRL + click for 'view/action' variables
                        proposals.put(name, new PHPCompletionItem.VariableItem(
                                VariableElementImpl.create(name, 0, realFileObject,
                                varName.getElementQuery(), TypeResolverImpl.forNames(qualifiedNames), varName.isDeprecated()), request) {
                            @Override
                            public boolean isSmart() {
                                return true;
                            }
                        });
                    } else {
                        proposals.put(name, new PHPCompletionItem.VariableItem(
                                VariableElementImpl.create(name, 0, request.currentlyEditedFileURL,
                                varName.getElementQuery(), TypeResolverImpl.forNames(qualifiedNames), varName.isDeprecated()), request));
                    }
                }
            }

            for (final String name : PredefinedSymbols.SUPERGLOBALS) {
                if (isPrefix("$" + name, request.prefix)) { //NOI18N
                    proposals.put(name, new PHPCompletionItem.SuperGlobalItem(request, name));
                }
            }

        }
        return proposals.values();
    }

    private boolean isPrefix(String name, String prefix) {
        return name != null && (name.startsWith(prefix)
                || nameKind == QuerySupport.Kind.CASE_INSENSITIVE_PREFIX && name.toLowerCase().startsWith(prefix.toLowerCase()));
    }

    @Override
    public Documentation documentElement(ParserResult info, ElementHandle element, Callable<Boolean> cancel) {
        Documentation result;
        if (element instanceof ModelElement) {
            ModelElement mElem = (ModelElement) element;
            ModelElement parentElem = mElem.getInScope();
            FileObject fileObject = mElem.getFileObject();
            String fName = fileObject == null ? "?" : fileObject.getNameExt(); //NOI18N
            String tooltip;
            if (parentElem instanceof TypeScope) {
                tooltip = mElem.getPhpElementKind() + ": " + parentElem.getName() + "<b> " + mElem.getName() + " </b>" + "(" + fName + ")"; //NOI18N
            } else {
                tooltip = mElem.getPhpElementKind() + ":<b> " + mElem.getName() + " </b>" + "(" + fName + ")"; //NOI18N
            }
            result = Documentation.create(String.format("<div align=\"right\"><font size=-1>%s</font></div>", tooltip)); //NOI18N
        } else {
            result = ((element instanceof MethodElement) && ((MethodElement) element).isMagic()) ? null : DocRenderer.document(info, element);
        }
        return result;
    }

    @Override
    public String document(ParserResult info, ElementHandle element) {
        return null;
    }

    @Override
    public ElementHandle resolveLink(String link, ElementHandle originalHandle) {
        return null;
    }

    private static boolean isPHPIdentifierPart(char c) {
        return Character.isJavaIdentifierPart(c) || c == '@';
    }

    private String getPrefix(ParserResult info, int caretOffset, boolean upToOffset, PrefixBreaker prefixBreaker) {
        try {
            BaseDocument doc = (BaseDocument) info.getSnapshot().getSource().getDocument(false);
            if (doc == null) {
                return null;
            }
            int lineBegin = Utilities.getRowStart(doc, caretOffset);
            if (lineBegin != -1) {
                int lineEnd = Utilities.getRowEnd(doc, caretOffset);
                String line = doc.getText(lineBegin, lineEnd - lineBegin);
                int lineOffset = caretOffset - lineBegin;
                int start = lineOffset;
                if (lineOffset > 0) {
                    char c = 0;
                    for (int i = lineOffset - 1; i >= 0; i--) {
                        assert i >= 0 && i <= line.length() - 1 : "line:" + line + " | i:" + i + " | line.length():" + line.length() + " | lineBegin:" + lineBegin + " | lineEnd:" + lineEnd + " | caretOffset:" + caretOffset;
                        if (i >= 0 && i <= line.length() - 1) {
                            c = line.charAt(i);
                            if (!isPHPIdentifierPart(c) && c != '\\') {
                                break;
                            } else {
                                start = i;
                            }
                        }
                    }
                    if (start == lineOffset && c == '?'
                            && lineOffset - 2 >= 0 && line.charAt(lineOffset - 2) == '<') {
                        start -= 2;
                    }
                }

                // Find identifier end
                String prefix;
                if (upToOffset) {
                    prefix = line.substring(start, lineOffset);
                    int lastIndexOfDollar = prefix.lastIndexOf('$'); //NOI18N
                    if (lastIndexOfDollar > 0) {
                        prefix = prefix.substring(lastIndexOfDollar);
                    }
                } else {
                    if (lineOffset == line.length()) {
                        prefix = line.substring(start);
                    } else {
                        int n = line.length();
                        int end = lineOffset;
                        for (int j = lineOffset; j < n; j++) {
                            char d = line.charAt(j);
                            // Try to accept Foo::Bar as well
                            if (!isPHPIdentifierPart(d)) {
                                break;
                            } else {
                                end = j + 1;
                            }
                        }
                        prefix = line.substring(start, end);
                    }
                }

                if (prefix.length() > 0) {
                    if (prefix.endsWith("::")) {
                        return "";
                    }

                    if (prefix.endsWith(":") && prefix.length() > 1) {
                        return null;
                    }

                    // Strip out LHS if it's a qualified method, e.g.  Benchmark::measure -> measure
                    int q = prefix.lastIndexOf("::");

                    if (q != -1) {
                        prefix = prefix.substring(q + 2);
                    }

                    // The identifier chars identified by JsLanguage are a bit too permissive;
                    // they include things like "=", "!" and even "&" such that double-clicks will
                    // pick up the whole "token" the user is after. But "=" is only allowed at the
                    // end of identifiers for example.
                    if (prefix.length() == 1) {
                        char c = prefix.charAt(0);
                        if (prefixBreaker.isBreaker(c)) {
                            return null;
                        }
                    } else if (!"<?".equals(prefix)) {    //NOI18N
                        for (int i = prefix.length() - 1; i >= 0; i--) { // -2: the last position (-1) can legally be =, ! or ?

                            char c = prefix.charAt(i);
                            if (i == 0 && c == ':') {
                                // : is okay at the begining of prefixes
                            } else if (prefixBreaker.isBreaker(c)) {
                                prefix = prefix.substring(i + 1);
                                break;
                            }
                        }
                    }
                }

                if (prefix != null && prefix.startsWith("@")) { //NOI18N
                    final TokenHierarchy<?> tokenHierarchy = info.getSnapshot().getTokenHierarchy();
                    TokenSequence<PHPTokenId> tokenSequence = tokenHierarchy != null ? LexUtilities.getPHPTokenSequence(tokenHierarchy, caretOffset) : null;
                    if (tokenSequence != null) {
                        tokenSequence.move(caretOffset);
                        if (tokenSequence.moveNext() && tokenSequence.movePrevious()) {
                            Token<PHPTokenId> token = tokenSequence.token();
                            PHPTokenId id = token.id();
                            if (id.equals(PHPTokenId.PHP_STRING) || id.equals(PHPTokenId.PHP_TOKEN)) {
                                prefix = prefix.substring(1);
                            }
                        }
                    }
                }
                return prefix;
            }
            // Else: normal identifier: just return null and let the machinery do the rest
        } catch (BadLocationException ble) {
            //Exceptions.printStackTrace(ble);
        }

        return null;
    }

    @Override
    public String getPrefix(ParserResult info, int caretOffset, boolean upToOffset) {
        return getPrefix(info, caretOffset, upToOffset, PrefixBreaker.COMMON);
    }

    @Override
    public QueryType getAutoQuery(JTextComponent component, String typedText) {
        if (typedText.length() == 0) {
            return QueryType.NONE;
        }
        char lastChar = typedText.charAt(typedText.length() - 1);
        Document document = component.getDocument();
        //TokenHierarchy th = TokenHierarchy.get(document);
        int offset = component.getCaretPosition();
        TokenSequence<PHPTokenId> ts = LexUtilities.getPHPTokenSequence(document, offset);
        if (ts == null) {
            return QueryType.STOP;
        }
        int diff = ts.move(offset);
        if (diff > 0 && ts.moveNext() || ts.movePrevious()) {
            Token t = ts.token();
            if (t != null) {
                if (t.id() == PHPTokenId.T_INLINE_HTML) {
                    return QueryType.ALL_COMPLETION;
                } else {
                    if (AUTOPOPUP_STOP_CHARS.contains(Character.valueOf(lastChar))) {
                        return QueryType.STOP;
                    }
                    if (OptionsUtils.autoCompletionTypes()) {
                        if (lastChar == ' ' || lastChar == '\t') {
                            if (ts.movePrevious()
                                    && TOKENS_TRIGGERING_AUTOPUP_TYPES_WS.contains(ts.token().id())) {

                                return QueryType.ALL_COMPLETION;
                            } else {
                                return QueryType.STOP;
                            }
                        }

                        if (t.id() == PHPTokenId.PHP_OBJECT_OPERATOR || t.id() == PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM) {
                            return QueryType.ALL_COMPLETION;
                        }
                    }
                    if (OptionsUtils.autoCompletionVariables()) {
                        if ((t.id() == PHPTokenId.PHP_TOKEN && lastChar == '$')
                                || (t.id() == PHPTokenId.PHP_CONSTANT_ENCAPSED_STRING && lastChar == '$')) {
                            return QueryType.ALL_COMPLETION;
                        }
                    }
                    if (OptionsUtils.autoCompletionNamespaces()) {
                        if (t.id() == PHPTokenId.PHP_NS_SEPARATOR) {
                            return isPhp53(document) ? QueryType.ALL_COMPLETION : QueryType.NONE;
                        }
                    }
                    if (t.id() == PHPTokenId.PHPDOC_COMMENT && lastChar == '@') {
                        return QueryType.ALL_COMPLETION;
                    }
                    if (OptionsUtils.autoCompletionFull()) {
                        TokenId id = t.id();
                        if ((id.equals(PHPTokenId.PHP_STRING) || id.equals(PHPTokenId.PHP_VARIABLE)) && t.length() > 0) {
                            return QueryType.ALL_COMPLETION;
                        }
                    }
                }
            }
        }
        return QueryType.NONE;
    }

    public static boolean isPhp53(Document document) {
        final FileObject fileObject = CodeUtils.getFileObject(document);
        assert fileObject != null;
        return CodeUtils.isPhp53(fileObject);
    }

    @Override
    public String resolveTemplateVariable(String variable, ParserResult info, int caretOffset, String name, Map parameters) {
        return null;
    }

    @Override
    public Set<String> getApplicableTemplates(Document doc, int selectionBegin, int selectionEnd) {
        return null;
    }

    @Override
    public ParameterInfo parameters(final ParserResult info, final int caretOffset, CompletionProposal proposal) {
        final org.netbeans.modules.php.editor.model.Model model = ((PHPParseResult) info).getModel();
        ParameterInfoSupport infoSupport = model.getParameterInfoSupport(caretOffset);
        ParameterInfo parameterInfo = infoSupport.getParameterInfo();
        return parameterInfo == null ? ParameterInfo.NONE : parameterInfo;
    }

    private boolean startsWith(String theString, String prefix) {
        if (prefix.length() == 0) {
            return true;
        }

        return caseSensitive ? theString.startsWith(prefix)
                : theString.toLowerCase().startsWith(prefix.toLowerCase());
    }

    private static class StaticOrInstanceMembersFilter extends ElementFilter {

        private final boolean forStaticContext;
        private final boolean forInstanceContext;
        private final boolean forSelfContext;
        private final boolean staticAllowed;
        private final boolean nonstaticAllowed;
        private final boolean forStaticLateBinding;

        public StaticOrInstanceMembersFilter(final boolean forStaticContext, final boolean forInstanceContext,
                final boolean forSelfContext, final boolean forStaticLateBinding) {
            this.forStaticContext = forStaticContext;
            this.forInstanceContext = forInstanceContext;
            this.forSelfContext = forSelfContext;
            this.forStaticLateBinding = forStaticLateBinding;
            this.staticAllowed = OptionsUtils.codeCompletionStaticMethods();
            this.nonstaticAllowed = OptionsUtils.codeCompletionNonStaticMethods();
        }

        @Override
        public boolean isAccepted(final PhpElement element) {
            if (forSelfContext && isAcceptedForSelfContext(element)) {
                return true;
            }
            if (forStaticContext && isAcceptedForStaticContext(element)) {
                return true;
            }
            if (forInstanceContext && isAcceptedForNotStaticContext(element)) {
                return true;
            }
            return false;
        }

        private boolean isAcceptedForNotStaticContext(final PhpElement element) {
            final boolean isStatic = element.getPhpModifiers().isStatic();
            return !isStatic || (staticAllowed && element.getPhpElementKind().equals(PhpElementKind.METHOD));
        }

        private boolean isAcceptedForStaticContext(final PhpElement element) {
            final boolean isStatic = element.getPhpModifiers().isStatic();
            return isStatic || (nonstaticAllowed && !forStaticLateBinding && element.getPhpElementKind().equals(PhpElementKind.METHOD));
        }

        private boolean isAcceptedForSelfContext(final PhpElement element) {
            return forSelfContext && nonstaticAllowed && !element.getPhpElementKind().equals(PhpElementKind.FIELD);
        }
    }

    private interface PrefixBreaker {
        PrefixBreaker COMMON = new PrefixBreaker() {

            @Override
            public boolean isBreaker(char c) {
                return !(isPHPIdentifierPart(c) || c == ':');
            }
        };

        PrefixBreaker WITH_NS_PARTS = new PrefixBreaker() {

            @Override
            public boolean isBreaker(char c) {
                return !(isPHPIdentifierPart(c) || c == '\\' || c == ':');
            }
        };

        boolean isBreaker(char c);
    }

    private static boolean isCamelCaseForTypeNames(final String query) {
        return false;
    }
}
