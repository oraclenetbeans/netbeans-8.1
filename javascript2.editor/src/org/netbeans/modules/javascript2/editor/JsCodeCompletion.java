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
package org.netbeans.modules.javascript2.editor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.*;
import org.netbeans.modules.csl.spi.DefaultCompletionResult;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.csl.spi.support.CancelSupport;
import org.netbeans.modules.css.indexing.api.CssIndex;
import org.netbeans.modules.html.editor.lib.api.HtmlVersion;
import org.netbeans.modules.html.editor.lib.api.model.HtmlModel;
import org.netbeans.modules.html.editor.lib.api.model.HtmlModelFactory;
import org.netbeans.modules.html.editor.lib.api.model.HtmlTag;
import org.netbeans.modules.html.editor.lib.api.model.HtmlTagAttribute;
import org.netbeans.modules.html.editor.lib.api.model.HtmlTagType;
import org.netbeans.modules.javascript2.editor.spi.CompletionContext;
import org.netbeans.modules.javascript2.editor.JsCompletionItem.CompletionRequest;
import org.netbeans.modules.javascript2.editor.doc.JsDocumentationCodeCompletion;
import org.netbeans.modules.javascript2.editor.doc.JsDocumentationElement;
import org.netbeans.modules.javascript2.editor.index.IndexedElement;
import org.netbeans.modules.javascript2.editor.index.JsIndex;
import org.netbeans.modules.javascript2.editor.lexer.JsDocumentationTokenId;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.api.lexer.LexUtilities;
import org.netbeans.modules.javascript2.editor.model.*;
import org.netbeans.modules.javascript2.editor.model.impl.*;
import org.netbeans.modules.javascript2.editor.options.OptionsUtils;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;
import static org.netbeans.modules.javascript2.editor.spi.CompletionContext.EXPRESSION;
import static org.netbeans.modules.javascript2.editor.spi.CompletionContext.OBJECT_MEMBERS;
import static org.netbeans.modules.javascript2.editor.spi.CompletionContext.OBJECT_PROPERTY;
import org.netbeans.modules.javascript2.editor.spi.CompletionProvider;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Petr Pisl
 */
class JsCodeCompletion implements CodeCompletionHandler2 {

    private static final Logger LOGGER = Logger.getLogger(JsCodeCompletion.class.getName());

    private static final List<String> WINDOW_EXPRESSION_CHAIN = Arrays.<String>asList("window", "@pro"); //NOI18N

    private boolean caseSensitive;
    
    private static final String CHARS_NO_AUTO_COMPLETE = ";,/+-\\:={}[]()"; //NOI18N

    @Override
    public CodeCompletionResult complete(CodeCompletionContext ccContext) {
        final CancelSupport cancelSupport = CancelSupport.getDefault();
        if (cancelSupport.isCancelled()) {
            return CodeCompletionResult.NONE;
        }
        long start = System.currentTimeMillis();
        
        
        BaseDocument doc = (BaseDocument) ccContext.getParserResult().getSnapshot().getSource().getDocument(false);
        if (doc == null) {
            return CodeCompletionResult.NONE;
        }

        this.caseSensitive = ccContext.isCaseSensitive();
        
        ParserResult info = ccContext.getParserResult();
        int caretOffset = ccContext.getParserResult().getSnapshot().getEmbeddedOffset(ccContext.getCaretOffset());
        FileObject fileObject = ccContext.getParserResult().getSnapshot().getSource().getFileObject();
        JsParserResult jsParserResult = (JsParserResult)info;
        CompletionContext context = CompletionContextFinder.findCompletionContext(info, caretOffset);
        
        LOGGER.log(Level.FINE, String.format("CC context: %s", context.toString()));
        
        JsCompletionItem.CompletionRequest request = new JsCompletionItem.CompletionRequest();
            String pref = ccContext.getPrefix();
            //pref = pref == null ? "" : pref;

            request.anchor = pref == null ? caretOffset : caretOffset
                    // can't just use 'prefix.getLength()' here cos it might have been calculated with
                    // the 'upToOffset' flag set to false
                    - pref.length();
            request.result = jsParserResult;
            request.info = info;
            request.prefix = pref;
            request.completionContext = context;
            request.addHtmlTagAttributes = false;
            request.cancelSupport = cancelSupport;
        
        jsParserResult.getModel().resolve();
        final List<CompletionProposal> resultList = new ArrayList<CompletionProposal>();
        HashMap<String, List<JsElement>> added = new HashMap<String, List<JsElement>>();
        if (cancelSupport.isCancelled()) {
            return CodeCompletionResult.NONE;
        }
        if (ccContext.getQueryType() == QueryType.ALL_COMPLETION) {
            switch (context) {
                case GLOBAL:
                    addGlobalObjectsFromIndex(request, added);
                    break;    
                case EXPRESSION:
                    completeKeywords(request, resultList);
                    completeExpression(request, added);
                    break;
                case OBJECT_PROPERTY:
                    completeObjectProperty(request, added);
                    break;
                case OBJECT_MEMBERS:
                    completeObjectMember(request, added);
                    break;
                default:
                    break;
            }
            if ((context == CompletionContext.EXPRESSION || context == CompletionContext.OBJECT_MEMBERS || context == CompletionContext.OBJECT_PROPERTY) && !request.prefix.isEmpty()) {
                Collection<? extends IndexResult> indexResults = JsIndex.get(fileObject).query(JsIndex.FIELD_BASE_NAME, request.prefix, QuerySupport.Kind.PREFIX, JsIndex.TERMS_BASIC_INFO);
                for (IndexResult indexResult : indexResults) {
                    IndexedElement indexElement = IndexedElement.create(indexResult);
                    addPropertyToMap(request, added, indexElement);
                }
            }
        } else {
            switch (context) {
                case STRING:
                    //XXX should be treated in the getPrefix method, but now
                    // there is hardcoded behavior for jQuery
                    if (request.prefix.startsWith(".")) {
                        request.prefix = request.prefix.substring(1);
                        request.anchor = request.anchor + 1;
                    }
                    List<String> expression = resolveExpressionChainFromString(request);
                    Map<String, List<JsElement>> toAdd = getCompletionFromExpressionChain(request, expression);

                    // create code completion results
                    JsCompletionItem.Factory.create(toAdd, request, resultList);
                    break;
                case STRING_ELEMENTS_BY_ID:
                    completeTagIds(request, resultList);
                    break;
                case STRING_ELEMENTS_BY_CLASS_NAME:
                    completeCSSClassNames(request, resultList);
                    break;
                case GLOBAL:
                    HashMap<String, List<JsElement>> addedProperties = new HashMap<String, List<JsElement>>();
                    addedProperties.putAll(getDomCompletionResults(request));
                    for (JsObject libGlobal : ModelExtender.getDefault().getExtendingGlobalObjects(fileObject)) {
                        for (JsObject object : libGlobal.getProperties().values()) {
                            addPropertyToMap(request, addedProperties, object);
                        }
                    }
                    for (JsObject object : request.result.getModel().getVariables(caretOffset)) {
                        if (!(object instanceof JsFunction && ((JsFunction) object).isAnonymous())) {
                            addPropertyToMap(request, addedProperties, object);
                        }
                    }
                    completeKeywords(request, resultList);
                    addGlobalObjectsFromIndex(request, addedProperties);
                    completeInWith(request, addedProperties);
                    JsCompletionItem.Factory.create(addedProperties, request, resultList);
                    break;
                case CALL_ARGUMENT:
                    completeCallArguments(request, resultList);
                case EXPRESSION:
                    completeKeywords(request, resultList);
                    completeExpression(request, added);
                    completeObjectProperty(request, added);
                    completeInWith(request, added);
                    added.remove(ModelUtils.PROTOTYPE);
                    break;
                case OBJECT_PROPERTY:
                    completeObjectProperty(request, added);
                    break;
                case OBJECT_MEMBERS:
                    completeObjectMember(request, added);
                    break;
                case DOCUMENTATION:
                    JsDocumentationCodeCompletion.complete(request, resultList);
                    break;
                case OBJECT_PROPERTY_NAME:
                    completeObjectPropertyName(request, added);
                default:
                    break;
            }
        }
        JsCompletionItem.Factory.create(added, request, resultList);
        if (request.addHtmlTagAttributes) {
            completeTagAttributes(request, resultList);
        }
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "Counting JS CC took {0}ms ",  (end - start));
        for (CompletionProvider interceptor : EditorExtender.getDefault().getCompletionProviders()) {
            resultList.addAll(interceptor.complete(ccContext, context, pref));
        }
        if (!resultList.isEmpty()) {
            return new DefaultCompletionResult(resultList, false);
        }
        return CodeCompletionResult.NONE;
    }

    private void addGlobalObjectsFromIndex(CompletionRequest request, HashMap<String, List<JsElement>> addedProperties) {
        FileObject fileObject = request.result.getSnapshot().getSource().getFileObject();
        if (fileObject != null) {
            JsIndex jsIndex = JsIndex.get(fileObject);
            Collection<IndexedElement> fromIndex = jsIndex.getGlobalVar(request.prefix);
            for (IndexedElement indexElement : fromIndex) {
                addPropertyToMap(request, addedProperties, indexElement);
            }

            fromIndex = jsIndex.getPropertiesWithPrefix("window", request.prefix);
            for (IndexedElement indexElement : fromIndex) {
                addPropertyToMap(request, addedProperties, indexElement);
            }
        }
    }

    @Override
    public String document(ParserResult info, ElementHandle element) {
        Documentation doc = documentElement(info, element, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return false;
            }
        });
        if (doc != null) {
            return doc.getContent();
        }
        return null;
    }

    @Override
    public Documentation documentElement(ParserResult info, ElementHandle element, Callable<Boolean> cancel) {
        if (element == null) {
            return null;
        }
        if (element instanceof IndexedElement) {
            final Documentation[] result = new Documentation[1];
            final IndexedElement indexedElement = (IndexedElement)element;
            FileObject nextFo = indexedElement.getFileObject();
            if (nextFo != null) {
                try {
                    ParserManager.parse(Collections.singleton(Source.create(nextFo)), new UserTask () {

                        @Override
                        public void run(ResultIterator resultIterator) throws Exception {
                            Result parserResult = resultIterator.getParserResult();
                            if (parserResult instanceof JsParserResult) {
                                JsParserResult jsInfo = (JsParserResult)parserResult;

                                String fqn = indexedElement.getFQN();
                                JsObject jsObjectGlobal  = jsInfo.getModel().getGlobalObject();
                                JsObject property = ModelUtils.findJsObjectByName(jsObjectGlobal, fqn);
                                if (property != null) {
                                    Documentation doc = property.getDocumentation();
                                    result[0] = doc;
                                }

                            } else {
                                LOGGER.log(Level.INFO, "Not instance of JsParserResult: {0}", parserResult);
                            }
                        }

                    });
                } catch (ParseException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
            }
            if (result[0] != null) {
                return result[0];
            }
        } else if (element instanceof JsObject) {
            JsObject jsObject = (JsObject) element;
            if (jsObject.getDocumentation() != null) {
                return jsObject.getDocumentation();
            }
        }

        for (CompletionProvider interceptor : EditorExtender.getDefault().getCompletionProviders()) {
            String doc = interceptor.getHelpDocumentation(info, element);
            if (doc != null && !doc.isEmpty()) {
                return Documentation.create(doc);
            }
        }

        if (element instanceof JsDocumentationElement) {
            String documentation = ((JsDocumentationElement) element).getDocumentation();
            return documentation != null ? Documentation.create(documentation) : null;
        }
        
        if (element instanceof JsCompletionItem.SimpleDocElement) {
            String documentation = ((JsCompletionItem.SimpleDocElement) element).getDocumentation();
            return documentation != null ? Documentation.create(documentation) : null;
        }
        if (OffsetRange.NONE.equals(element.getOffsetRange(info))) {
            return Documentation.create(NbBundle.getMessage(JsCodeCompletion.class, "MSG_ItemFromUsageDoc"));
        }

        return Documentation.create(NbBundle.getMessage(JsCodeCompletion.class, "MSG_DocNotAvailable"));
    }

    
    @Override
    public ElementHandle resolveLink(String link, ElementHandle originalHandle) {
        return null;
    }

    @Override
    public String getPrefix(ParserResult info, int caretOffset, boolean upToOffset) {
        String prefix = "";

        BaseDocument doc = (BaseDocument) info.getSnapshot().getSource().getDocument(false);
        if (doc == null) {
            return null;
        }

        //caretOffset = info.getSnapshot().getEmbeddedOffset(caretOffset);
        TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(info.getSnapshot(), caretOffset);
        if (ts == null) {
            return null;
        }

        int offset = info.getSnapshot().getEmbeddedOffset(caretOffset);
        ts.move(offset);

        if (!ts.moveNext() && !ts.movePrevious()) {
            return null;
        }
        
        if (ts.offset() == offset) {
            // We're looking at the offset to the RIGHT of the caret
            // and here I care about what's on the left
            ts.movePrevious();
        }

        Token<? extends JsTokenId> token = ts.token();

        if (token != null && token.id() != JsTokenId.EOL) {
            JsTokenId id = token.id();
            if (id == JsTokenId.STRING_END && ts.movePrevious()) {
                if (ts.token().id() == JsTokenId.STRING_BEGIN) {
                    return "";
                } else {
                    ts.moveNext();
                }
            }
            if (id == JsTokenId.STRING) {
                prefix = token.text().toString();
                if (upToOffset) {
                    int prefixIndex = getPrefixIndexFromSequence(prefix.substring(0, offset - ts.offset()));
                    prefix = prefix.substring(prefixIndex, offset - ts.offset());
                }
            }
            if (id == JsTokenId.IDENTIFIER || id.isKeyword()) {
                prefix = token.text().toString();
                if (upToOffset) {
                    if (offset - ts.offset() >= 0) {
                        prefix = prefix.substring(0, offset - ts.offset());
                    }
                }
            }
            if (id == JsTokenId.DOC_COMMENT) {
                TokenSequence<? extends JsDocumentationTokenId> docTokenSeq =
                        LexUtilities.getJsDocumentationTokenSequence(info.getSnapshot(), offset);
                if (docTokenSeq == null) {
                    return null;
                }

                docTokenSeq.move(offset);
                // initialize moved token
                if (!docTokenSeq.moveNext() && !docTokenSeq.movePrevious()) {
                    return null;
                }

                if (docTokenSeq.token().id() == JsDocumentationTokenId.KEYWORD) {
                    // inside the keyword tag
                    prefix = docTokenSeq.token().text().toString();
                    if (upToOffset) {
                        prefix = prefix.substring(0, offset - docTokenSeq.offset());
                    }
                } else {
                    // get the token before
                    docTokenSeq.movePrevious();
                    prefix = docTokenSeq.token().text().toString();
                }
            }
            if (id.isError()) {
                prefix = token.text().toString();
                //if (upToOffset) {
                    prefix = prefix.substring(0, offset - ts.offset());
                //}
            }
        }
        LOGGER.log(Level.FINE, String.format("Prefix for cc: %s", prefix));
        return prefix;
    }

    @Override
    public QueryType getAutoQuery(JTextComponent component, String typedText) {
        if (typedText.length() == 0) {
            return QueryType.NONE;
        }

        int offset = component.getCaretPosition();
        TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(component.getDocument(), offset);
        if (ts != null) {
            int diff = ts.move(offset);
            TokenId currentTokenId = null;
            if (diff == 0 && ts.movePrevious() || ts.moveNext()) {
                currentTokenId = ts.token().id();
            }

            char lastChar = typedText.charAt(typedText.length() - 1);
            if (currentTokenId == JsTokenId.BLOCK_COMMENT || currentTokenId == JsTokenId.DOC_COMMENT
                    || currentTokenId == JsTokenId.LINE_COMMENT) {
                if (lastChar == '@') { //NOI18N
                    return QueryType.COMPLETION;
                }
            } else if (currentTokenId == JsTokenId.STRING && lastChar == '/') {
                return QueryType.COMPLETION;
            } else {
                switch (lastChar) {
                    case '.': //NOI18N
                        if (OptionsUtils.forLanguage(JsTokenId.javascriptLanguage()).autoCompletionAfterDot()) {
                            return QueryType.COMPLETION;
                        }
                        break;
                    default:
                        if (OptionsUtils.forLanguage(JsTokenId.javascriptLanguage()).autoCompletionFull()) {
                            if (!Character.isWhitespace(lastChar) && CHARS_NO_AUTO_COMPLETE.indexOf(lastChar) == -1) {
                                return QueryType.COMPLETION;
                            }
                        }
                        return QueryType.NONE;
                }
            }
        }
        return QueryType.NONE;
    }

    @Override
    public String resolveTemplateVariable(String variable, ParserResult info, int caretOffset, String name, Map parameters) {
        return null;
    }

    @Override
    public Set<String> getApplicableTemplates(Document doc, int selectionBegin, int selectionEnd) {
        // must return null - CSL reasons, see #217101 for more information
        return null;
    }

    @Override
    public ParameterInfo parameters(ParserResult info, int caretOffset, CompletionProposal proposal) {
        // TODO needs to be implemented.
        return ParameterInfo.NONE;
    }

    private void completeExpression(CompletionRequest request, HashMap <String, List<JsElement>> addedItems) {
        
        FileObject fo = request.info.getSnapshot().getSource().getFileObject();
        addedItems.putAll(getDomCompletionResults(request));
        // from index
        JsIndex index = JsIndex.get(fo);
        Collection<IndexedElement> fromIndex = index.getGlobalVar(request.prefix);
        for (IndexedElement indexedElement : fromIndex) {
            addPropertyToMap(request, addedItems, indexedElement);
        }
        
        // from libraries
        for (JsObject libGlobal : ModelExtender.getDefault().getExtendingGlobalObjects(fo)) {
            for (JsObject object : libGlobal.getProperties().values()) {
                addPropertyToMap(request, addedItems, object);
            }
        }
        
        // from model
        //int offset = request.info.getSnapshot().getEmbeddedOffset(request.anchor);
        for(JsObject object : request.result.getModel().getVariables(request.anchor)) {
            if (!(object instanceof JsFunction && ((JsFunction) object).isAnonymous())) {
                addPropertyToMap(request, addedItems, object);
            }
        }

//        addedItems.putAll(getWithCompletionResults(request, null));
    }

    private int checkRecursion;

    private void completeObjectProperty(CompletionRequest request, Map<String, List<JsElement>> addedItems) {
        List<String> expChain = ModelUtils.resolveExpressionChain(request.result.getSnapshot(), request.anchor, false);
        if (!expChain.isEmpty()) {
            Map<String, List<JsElement>> toAdd = getCompletionFromExpressionChain(request, expChain);
            if (request.cancelSupport.isCancelled()) {
                return;
            }
            FileObject fo = request.result.getSnapshot().getSource().getFileObject();
            if (fo != null) {
                long start = System.currentTimeMillis();
                Collection<IndexedElement> fromUsages = JsIndex.get(request.result.getSnapshot().getSource().getFileObject()).getUsagesFromExpression(expChain);
                for (IndexedElement indexedElement : fromUsages) {
                    if (!fo.equals(indexedElement.getFileObject()) || !indexedElement.getName().equals(request.prefix)) { 
                        addPropertyToMap(request, addedItems, indexedElement);
                    }
                }
                long end = System.currentTimeMillis();
                LOGGER.log(Level.FINE, String.format("Counting cc based on usages took: %dms", (end - start)));
            }
            addedItems.putAll(toAdd);
        }
    }

    private Map<String, List<JsElement>> getCompletionFromExpressionChain(CompletionRequest request, List<String> expChain) {
        FileObject fo = request.info.getSnapshot().getSource().getFileObject();
        JsIndex jsIndex = JsIndex.get(fo);
        Collection<TypeUsage> resolveTypeFromExpression = new ArrayList<TypeUsage>();
        HashMap<String, List<JsElement>> addedProperties = new HashMap<String, List<JsElement>>();
        resolveTypeFromExpression.addAll(ModelUtils.resolveTypeFromExpression(request.result.getModel(), jsIndex, expChain, request.anchor, true));
        if (request.cancelSupport.isCancelled()) {
            return addedProperties;
        }
        resolveTypeFromExpression = ModelUtils.resolveTypes(resolveTypeFromExpression, request.result, true, true);
        
        // try to map window property
        Collection<String> windowProp = new ArrayList<String>();
        for (TypeUsage typeUsage : resolveTypeFromExpression) {
            if (typeUsage.isResolved() && !typeUsage.getType().startsWith("window")) {
                windowProp.add("window." + typeUsage.getType());
            }
        }
            
        Collection<String> prototypeChain = new ArrayList<String>();
        for (TypeUsage typeUsage : resolveTypeFromExpression) {
            prototypeChain.addAll(ModelUtils.findPrototypeChain(typeUsage.getType(), jsIndex));
        }

        for (String string : windowProp) {
            resolveTypeFromExpression.add(new TypeUsageImpl(string));
        }
        
        for (String string : prototypeChain) {
            resolveTypeFromExpression.add(new TypeUsageImpl(string));
        }
        if (request.cancelSupport.isCancelled()) {
            return addedProperties;
        }
        boolean isFunction = false; // addding Function to the prototype chain?
        List<JsObject> lastResolvedObjects = new ArrayList<JsObject>();
        for (TypeUsage typeUsage : resolveTypeFromExpression) {
            checkRecursion = 0;
            boolean addFunctionProp = processTypeInModel(request, request.result.getModel(), typeUsage, lastResolvedObjects, expChain.get(1).equals("@pro"), jsIndex, addedProperties);
            isFunction = isFunction || addFunctionProp;
            if (typeUsage.isResolved()) {
                addObjectPropertiesFromIndex(typeUsage.getType(), jsIndex, request, addedProperties);
            }
        }
        boolean isPublic = lastResolvedObjects.isEmpty();
        for (JsObject resolved : lastResolvedObjects) {
            if(!isFunction && resolved.getJSKind().isFunction()) {
                isFunction = true;
            }
            addObjectPropertiesToCC(resolved, request, addedProperties);
            if (!resolved.isDeclared()) {
                // if the object is not defined here, look to the index as well
                addObjectPropertiesFromIndex(resolved.getFullyQualifiedName(), jsIndex, request, addedProperties);
                isPublic = true;
            } else {
                if (!resolved.getModifiers().contains(Modifier.PRIVATE)) {
                    isPublic = true;
                }
            }
        }

        if (isFunction) {
            addObjectPropertiesFromIndex("Function", jsIndex, request, addedProperties); //NOI18N
        }

        if (request.cancelSupport.isCancelled()) {
            return addedProperties;
        }
        addObjectPropertiesFromIndex("Object", jsIndex, request, addedProperties); //NOI18N
        
        if (isPublic) {
            // now look to the index again for declared item outside
            StringBuilder fqn = new StringBuilder();
            for (int i = expChain.size() - 1; i > -1; i--) {
                fqn.append(expChain.get(--i));
                fqn.append('.');
            }
            if (fqn.length() > 0) {
                Collection<IndexedElement> indexResults = jsIndex.getPropertiesWithPrefix(fqn.toString().substring(0, fqn.length() - 1), request.prefix);
                for (IndexedElement indexedElement : indexResults) {
                    if (!indexedElement.isAnonymous()
                            && indexedElement.getModifiers().contains(Modifier.PUBLIC)) {
                        addPropertyToMap(request, addedProperties, indexedElement);
                    }
                }
            }
        }
        return addedProperties;
    }
    
    private Identifier findNameOfFunctionCall (CompletionRequest request) {
        // is an argument of a function call?
        TokenHierarchy<?> th = request.result.getSnapshot().getTokenHierarchy();
        if (th == null) {
            return null;
        }
        TokenSequence<JsTokenId> ts = th.tokenSequence(JsTokenId.javascriptLanguage());
        if (ts == null) {
            return null;
        }
        
        ts.move(request.anchor);
        
        if (!ts.moveNext() && !ts.movePrevious()){
            return null;
        }
            
        int curlyDeep = 0;
        Token<? extends JsTokenId> token = ts.token();
        JsTokenId tokenId = token.id();
        while (ts.movePrevious() && tokenId != JsTokenId.BRACKET_LEFT_PAREN
                && tokenId != JsTokenId.OPERATOR_SEMICOLON) {
            if (tokenId == JsTokenId.BRACKET_LEFT_CURLY) {
                curlyDeep++;
            }
            token = ts.token();
            tokenId = token.id();
        }
        
        
        if (tokenId == JsTokenId.BRACKET_LEFT_PAREN) {
            token = LexUtilities.findPreviousNonWsNonComment(ts);
            if (token != null && token.id() == JsTokenId.IDENTIFIER) {
                String functionName = token.text().toString();
                return new IdentifierImpl(functionName, new OffsetRange(ts.offset(), ts.offset() + functionName.length()));
            }
        }
        return null;
    }
    
    private List<IndexedElement.FunctionIndexedElement> findFunctionInIndex(Identifier functionName, CompletionRequest request) {
        List<IndexedElement.FunctionIndexedElement> result = new ArrayList<IndexedElement.FunctionIndexedElement>();
        List<String> expChain = ModelUtils.resolveExpressionChain(request.result.getSnapshot(), functionName.getOffsetRange().getStart() - 1, false);
        FileObject fo = request.info.getSnapshot().getSource().getFileObject();
        if (fo != null) {
            JsIndex jsIndex = JsIndex.get(fo);
            if (expChain.isEmpty()) {
                // global space
                Collection<IndexedElement> globalVars = jsIndex.getGlobalVar(functionName.getName());
                for (IndexedElement globalVar : globalVars) {
                    if (globalVar.getName().equals(functionName) && globalVar.getJSKind().isFunction()) {
                        result.add((IndexedElement.FunctionIndexedElement)globalVar);
                    }
                }
            } else {
                // the expression needs to be resolved
                Collection<TypeUsage> types = ModelUtils.resolveTypeFromExpression(request.result.getModel(), jsIndex, expChain, request.anchor, false);
                for (TypeUsage type : types) {
                    Collection<IndexedElement> properties = jsIndex.getPropertiesWithPrefix(type.getType(), functionName.getName());
                    properties.addAll(jsIndex.getPropertiesWithPrefix(type.getType() + "." + ModelUtils.PROTOTYPE, functionName.getName()));
                    for (IndexedElement property : properties) {
                        if (property.getName().equals(functionName.getName()) && property.getJSKind().isFunction()) {
                            IndexedElement.FunctionIndexedElement function = (IndexedElement.FunctionIndexedElement)property;
                            result.add(function);
                        }
                    }
                }
            }
        }
        return result;
    }
    
    private List<TypeUsage> findPossibleCallArgTypes(CompletionRequest request) {
        Identifier functionName = findNameOfFunctionCall(request);
        if (functionName == null) {
            // probably not in a call
            return null;
        }
        List<TypeUsage> result = new ArrayList<TypeUsage>();
        List<IndexedElement.FunctionIndexedElement> functions = findFunctionInIndex(functionName, request);
        for (IndexedElement.FunctionIndexedElement function : functions) {
            LinkedHashMap<String, Collection<String>> parameters = function.getParameters();
            for (Collection<String> assignments: parameters.values()) {
                if (!assignments.isEmpty()) {
                    for (String assignment : assignments) {
                        result.add(new TypeUsageImpl(assignment));
                    }
                }
            }
        }
        return result;
    }
    
    private void completeObjectPropertyName(CompletionRequest request, Map<String, List<JsElement>> addedItems) {
        // is an argument of the function call?
        TokenHierarchy<?> th = request.result.getSnapshot().getTokenHierarchy();
        if (th == null) {
            return;
        }
        TokenSequence<JsTokenId> ts = th.tokenSequence(JsTokenId.javascriptLanguage());
        if (ts == null) {
            return;
        }
        
        ts.move(request.anchor);
        
        if (!ts.moveNext() && !ts.movePrevious()){
            return;
        }
            
        int curlyDeep = 0;
        Token<? extends JsTokenId> token = ts.token();
        JsTokenId tokenId = token.id();
        while (ts.movePrevious() && tokenId != JsTokenId.BRACKET_LEFT_PAREN
                && tokenId != JsTokenId.OPERATOR_SEMICOLON) {
            if (tokenId == JsTokenId.BRACKET_LEFT_CURLY) {
                curlyDeep++;
            }
            token = ts.token();
            tokenId = token.id();
        }
        
        // what is the function?
        if (curlyDeep == 1 && tokenId == JsTokenId.BRACKET_LEFT_PAREN) {
            token = LexUtilities.findPreviousNonWsNonComment(ts);
            if (token != null && token.id() == JsTokenId.IDENTIFIER) {
                String functionName = token.text().toString();
                List<String> expChain = ModelUtils.resolveExpressionChain(request.result.getSnapshot(), ts.offset() - 1, false);
                List<TypeUsage> possibleTypes = new ArrayList<TypeUsage>();
                FileObject fo = request.info.getSnapshot().getSource().getFileObject();
                JsIndex jsIndex = JsIndex.get(fo);
                if (expChain.isEmpty()) {
                    // global space
                    Collection<? extends JsObject> variables = ModelUtils.getVariables(request.result.getModel(), request.anchor);
                    for (JsObject variable : variables) {
                        if (variable.getName().equals(functionName) && variable.getJSKind().isFunction()) {
                            // do we now the tape of the argument?
                            JsFunction function = (JsFunction)variable;
                            Collection<? extends JsObject> parameters = function.getParameters();
                            for (JsObject parameter: parameters) {
                                if (!parameter.getAssignments().isEmpty()) {
                                    possibleTypes.addAll(parameter.getAssignments());
                                }
                            }
                            break;
                        }
                    }
                    Collection<IndexedElement> globalVars = jsIndex.getGlobalVar(functionName);
                    for (IndexedElement globalVar : globalVars) {
                        if (globalVar.getName().equals(functionName) && globalVar.getJSKind().isFunction()) {
                            IndexedElement.FunctionIndexedElement function = (IndexedElement.FunctionIndexedElement)globalVar;
                            LinkedHashMap<String, Collection<String>> parameters = function.getParameters();
                            for (Collection<String> assignments: parameters.values()) {
                                if (!assignments.isEmpty()) {
                                    for (String type : assignments) {
                                        possibleTypes.add(new TypeUsageImpl(type));
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Collection<TypeUsage> types = ModelUtils.resolveTypeFromExpression(request.result.getModel(), jsIndex, expChain, request.anchor, false);
                    for (TypeUsage type : types) {
                        Collection<IndexedElement> properties = jsIndex.getPropertiesWithPrefix(type.getType(), functionName);
                        properties.addAll(jsIndex.getPropertiesWithPrefix(type.getType() + "." + ModelUtils.PROTOTYPE, functionName));
                        for (IndexedElement property : properties) {
                            if (property.getName().equals(functionName) && property.getJSKind().isFunction()) {
                                IndexedElement.FunctionIndexedElement function = (IndexedElement.FunctionIndexedElement)property;
                                LinkedHashMap<String, Collection<String>> parameters = function.getParameters();
                                for (Collection<String> assignments: parameters.values()) {
                                    if (!assignments.isEmpty()) {
                                        for (String assignment : assignments) {
                                            possibleTypes.add(new TypeUsageImpl(assignment));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!possibleTypes.isEmpty()) {
                    for (TypeUsage type : possibleTypes) {
                        addObjectPropertiesFromIndex(type.getType(), jsIndex, request, addedItems);
                    }
                }
            }
        }
        
    }
    
    private List<String> resolveExpressionChainFromString(CompletionRequest request) {
        TokenHierarchy<?> th = request.info.getSnapshot().getTokenHierarchy();
        TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(th, request.anchor);
        if (ts == null) {
            return Collections.<String>emptyList();
        }

        int offset = request.info.getSnapshot().getEmbeddedOffset(request.anchor);
        ts.move(offset);
        String text = null;
        if (ts.moveNext()) {
            if (ts.token().id() == JsTokenId.STRING_END) {
                if (ts.movePrevious() && ts.token().id() == JsTokenId.STRING) {
                    text = ts.token().text().toString();
                }
            } else if (ts.token().id() == JsTokenId.STRING) {
                text = ts.token().text().toString().substring(0, offset - ts.offset());
            }
        }
        if (text != null && !text.isEmpty()) {
            int index = text.length() - 1;
            List<String> exp = new ArrayList<String>();
            int parenBalancer = 0;
            boolean methodCall = false;
            char ch = text.charAt(index);
            String part = "";
            while (index > -1 && ch != ' ' && ch != '\n' && ch != ';' && ch != '}'
                    && ch != '{' && ch != '(' && ch != '=' && ch != '+' && ch != '[') {
                if (ch == '.') {
                    if (!part.isEmpty()) {
                        exp.add(part);
                        part = "";
                        if (methodCall) {
                            exp.add("@mtd");
                            methodCall = false;
                        } else {
                            exp.add("@pro");
                        }
                    }
                } else {
                    if (ch == ')') {
                        parenBalancer++;
                        methodCall = true;
                        while (parenBalancer > 0 && --index > -1) {
                            ch = text.charAt(index);
                            if (ch == ')') {
                                parenBalancer++;
                            } else if (ch == '(') {
                                parenBalancer--;
                            }
                        }
                    } else {
                        part = ch + part;
                    }
                }
                if (--index > -1) {
                    ch = text.charAt(index);
                }
            }
            if (!part.isEmpty()) {
                exp.add(part);
                if (methodCall) {
                    exp.add("@mtd");
                } else {
                    exp.add("@pro");
                }
            }
            return exp;
        }
        return Collections.<String>emptyList();
    }

    /**
     * 
     * @param request
     * @param offset offset where the expression should be resolved
     * @param lookBefore if yes, looks for the beginning of the expression before the offset,
     *                  if no, it can be in a middle of expression
     * @return 
     */
    private List<String> resolveExpressionChain(CompletionRequest request, int offset, boolean lookBefore) {
        TokenHierarchy<?> th = request.info.getSnapshot().getTokenHierarchy();
        TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(th, offset);
        if (ts == null) {
            return Collections.<String>emptyList();
        }

        ts.move(offset);
        if (ts.movePrevious() && (ts.moveNext() || ((ts.offset() + ts.token().length()) == request.result.getSnapshot().getText().length()))) {
            if (!lookBefore && ts.token().id() != JsTokenId.OPERATOR_DOT) {
                ts.movePrevious();
            }
            Token<? extends JsTokenId> token = lookBefore ? LexUtilities.findPrevious(ts, Arrays.asList(JsTokenId.WHITESPACE, JsTokenId.BLOCK_COMMENT, JsTokenId.EOL)) : ts.token();
            int parenBalancer = 0;
            // 1 - method call, 0 - property, 2 - array
            int partType = 0;
            boolean wasLastDot = lookBefore;
            int offsetFirstRightParen = -1;
            List<String> exp = new ArrayList();

            while (token.id() != JsTokenId.WHITESPACE && token.id() != JsTokenId.OPERATOR_SEMICOLON
                    && token.id() != JsTokenId.BRACKET_RIGHT_CURLY && token.id() != JsTokenId.BRACKET_LEFT_CURLY
                    && token.id() != JsTokenId.BRACKET_LEFT_PAREN
                    && token.id() != JsTokenId.BLOCK_COMMENT
                    && token.id() != JsTokenId.LINE_COMMENT
                    && token.id() != JsTokenId.OPERATOR_ASSIGNMENT
                    && token.id() != JsTokenId.OPERATOR_PLUS) {

                if (token.id() != JsTokenId.EOL) {
                    if (token.id() != JsTokenId.OPERATOR_DOT) {
                        if (token.id() == JsTokenId.BRACKET_RIGHT_PAREN) {
                            parenBalancer++;
                            partType = 1;
                            if (offsetFirstRightParen == -1) {
                                offsetFirstRightParen = ts.offset();
                            }
                            while (parenBalancer > 0 && ts.movePrevious()) {
                                token = ts.token();
                                if (token.id() == JsTokenId.BRACKET_RIGHT_PAREN) {
                                    parenBalancer++;
                                } else {
                                    if (token.id() == JsTokenId.BRACKET_LEFT_PAREN) {
                                        parenBalancer--;
                                    }
                                }
                            }
                        } else if (token.id() == JsTokenId.BRACKET_RIGHT_BRACKET) {
                            parenBalancer++;
                            partType = 2;
                            while (parenBalancer > 0 && ts.movePrevious()) {
                                token = ts.token();
                                if (token.id() == JsTokenId.BRACKET_RIGHT_BRACKET) {
                                    parenBalancer++;
                                } else {
                                    if (token.id() == JsTokenId.BRACKET_LEFT_BRACKET) {
                                        parenBalancer--;
                                    }
                                }
                            }
                        } else if (parenBalancer == 0 && "operator".equals(token.id().primaryCategory())) { // NOI18N
                            return exp;
                        } else {
                            exp.add(token.text().toString());
                            switch (partType) {
                                case 0:
                                    exp.add("@pro");   // NOI18N
                                    break;
                                case 1:
                                    exp.add("@mtd");   // NOI18N
                                    offsetFirstRightParen = -1;
                                    break;
                                case 2:
                                    exp.add("@arr");    // NOI18N
                                    break;
                                default:
                                    break;
                            }
                            partType = 0;
                            wasLastDot = false;
                        }
                    } else {
                        wasLastDot = true;
                    }
                } else {
                    if (!wasLastDot && ts.movePrevious()) {
                        // check whether it's continuatino of previous line
                        token = LexUtilities.findPrevious(ts, Arrays.asList(JsTokenId.WHITESPACE, JsTokenId.BLOCK_COMMENT, JsTokenId.LINE_COMMENT));
                        if (token.id() != JsTokenId.OPERATOR_DOT) {
                            // the dot was not found => it's not continuation of expression
                            break;
                        }
                    }
                }
                if (!ts.movePrevious()) {
                    break;
                }
                token = ts.token();
            }
            if (token.id() == JsTokenId.WHITESPACE) {
                if (ts.movePrevious()) {
                    token = LexUtilities.findPrevious(ts, Arrays.asList(JsTokenId.WHITESPACE, JsTokenId.BLOCK_COMMENT, JsTokenId.EOL));
                    if (token.id() == JsTokenId.KEYWORD_NEW && !exp.isEmpty()) {
                        exp.remove(exp.size() - 1);
                        exp.add("@pro");    // NOI18N
                    } else if (!lookBefore && offsetFirstRightParen > -1) {
                        // in the case when the expression is like ( new Object()).someMethod
                        exp.addAll(resolveExpressionChain(request, offsetFirstRightParen - 1, true));
                    }
                }
            } else if (exp.isEmpty() && !lookBefore && offsetFirstRightParen > -1) {
                // in the case when the expression is like ( new Object()).someMethod
                exp.addAll(resolveExpressionChain(request, offsetFirstRightParen - 1, true));
            }
            return exp;
        }
        return Collections.<String>emptyList();
    }

    private void completeObjectMember(CompletionRequest request, Map<String, List<JsElement>> addedItems) {
        JsParserResult result = (JsParserResult)request.info;
        JsObject jsObject = (JsObject)ModelUtils.getDeclarationScope(result.getModel(), request.anchor);
        
        if (jsObject.getJSKind() == JsElement.Kind.METHOD) {
            jsObject = jsObject.getParent();
        }
        
        completeObjectMembers(jsObject, request, addedItems);
        
        if (ModelUtils.PROTOTYPE.equals(jsObject.getName())) {  //NOI18N
            completeObjectMembers(jsObject.getParent(), request, addedItems);
        }
    }
    
    private void completeObjectMembers(JsObject jsObject, CompletionRequest request, Map<String, List<JsElement>> properties) {
        if (jsObject.getJSKind() == JsElement.Kind.OBJECT || jsObject.getJSKind() == JsElement.Kind.CONSTRUCTOR
                || jsObject.getJSKind() == JsElement.Kind.OBJECT_LITERAL) {
            for (JsObject property : jsObject.getProperties().values()) {
                if(!(request.completionContext == OBJECT_MEMBERS && property.getModifiers().contains(Modifier.PRIVATE) && property.getModifiers().size() == 1) && !property.isAnonymous()) {
                    addPropertyToMap(request, properties, property);
                }
            }
        }
        
        String fqn = jsObject.getFullyQualifiedName();
        
        FileObject fo = request.info.getSnapshot().getSource().getFileObject();
        Collection<IndexedElement> indexedProperties = JsIndex.get(fo).getProperties(fqn);
        for (IndexedElement indexedElement : indexedProperties) {
            addPropertyToMap(request, properties, indexedElement);
        }
    }

    private void completeInWith (CompletionRequest request,HashMap <String, List<JsElement>> addedItems) {
        int offset = request.anchor;
        Collection<? extends TypeUsage> typesFromWith = ModelUtils.getTypeFromWith(request.result.getModel(), offset);
        if (!typesFromWith.isEmpty()) {
            FileObject fo = request.info.getSnapshot().getSource().getFileObject();
            JsIndex jsIndex = JsIndex.get(fo);
            Collection<TypeUsage> resolveTypes = ModelUtils.resolveTypes(typesFromWith, request.result, true, true);
            for (TypeUsage type : resolveTypes) {
                JsObject localObject = ModelUtils.findJsObjectByName(request.result.getModel(), type.getType());
                if (localObject != null) {
                    addObjectPropertiesToCC(localObject, request, addedItems);
                } 
                
                addObjectPropertiesFromIndex(type.getType(), jsIndex, request, addedItems);
            }
        }
    }
    
    private void completeCallArguments(CompletionRequest request, List<CompletionProposal> resultList) {
        // find (if exist) the function which is called.
        List<TypeUsage> types = findPossibleCallArgTypes(request);
        FileObject fo = request.result.getSnapshot().getSource().getFileObject();
        if (types != null && fo != null && !types.isEmpty()) {
            JsIndex jsIndex = JsIndex.get(fo);
            for (TypeUsage type: types) {
                Collection<? extends IndexResult> fromIndex = jsIndex.findByFqn(type.getType(), JsIndex.TERMS_BASIC_INFO);
                for (IndexResult indexResult: fromIndex) {
                    IndexedElement indexElement = IndexedElement.create(indexResult);
                    if (indexElement.getJSKind() == JsElement.Kind.CALLBACK) {
                        resultList.add(new JsCompletionItem.JsCallbackCompletionItem((IndexedElement.FunctionIndexedElement)indexElement, request));
                    }
                }
            }
        }
    }
    
    private void completeKeywords(CompletionRequest request, List<CompletionProposal> resultList) {
        for (String keyword : JsKeyWords.KEYWORDS.keySet()) {
            if (startsWith(keyword, request.prefix)) {
                resultList.add(new JsCompletionItem.KeywordItem(keyword, request));
            }
        }
    }
    
    private void completeTagAttributes(CompletionRequest request,  List<CompletionProposal> resultList) {
        
        for(HtmlTagAttribute attribute: getAllAttributes())  {
            if (attribute.getName().startsWith(request.prefix)) {
                resultList.add(new JsCompletionItem.JsHtmlAttributeItem(attribute, request));
            }
        }
    }
    
    private void completeTagIds(CompletionRequest request, List<CompletionProposal> resultList) {
        FileObject fo = request.result.getSnapshot().getSource().getFileObject();
        if (fo == null) {
            return;
        }
        Project project = FileOwnerQuery.getOwner(fo);
        HashSet<String> unique = new HashSet<String>();
        try {
            CssIndex cssIndex = CssIndex.create(project);
            Map<FileObject, Collection<String>> findIdsByPrefix = cssIndex.findIdsByPrefix(request.prefix);

            for (Collection<String> ids : findIdsByPrefix.values()) {
                for (String id : ids) {
                    if (!id.isEmpty()) {
                        unique.add(id);
                    }
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (!unique.isEmpty()) {
            for (Iterator<String> iterator = unique.iterator(); iterator.hasNext();) {
                resultList.add(new JsCompletionItem.CssCompletionItem(iterator.next(), request));
            }
        }
    }
    
    private void completeCSSClassNames(CompletionRequest request, List<CompletionProposal> resultList) {
        FileObject fo = request.result.getSnapshot().getSource().getFileObject();
        if(fo == null) {
            return;
        }
        Project project = FileOwnerQuery.getOwner(fo);
        HashSet<String> unique = new HashSet<String>();
        try {
            CssIndex cssIndex = CssIndex.create(project);
            Map<FileObject, Collection<String>> findIdsByPrefix = cssIndex.findClassesByPrefix(request.prefix);

            for (Collection<String> ids : findIdsByPrefix.values()) {
                for (String id : ids) {
                    if (!id.isEmpty()) {
                        unique.add(id);
                    }
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (!unique.isEmpty()) {
            for (Iterator<String> iterator = unique.iterator(); iterator.hasNext();) {
                resultList.add(new JsCompletionItem.CssCompletionItem(iterator.next(), request));
            }
        }
    }
    
    private boolean startsWith(String theString, String prefix) {
        if (prefix == null || prefix.length() == 0) {
            return true;
        }

        return caseSensitive ? theString.startsWith(prefix)
                : theString.toLowerCase().startsWith(prefix.toLowerCase());
    }

    private boolean processTypeInModel(CompletionRequest request, Model model, TypeUsage type, List<JsObject> lastResolvedObjects, boolean prop, JsIndex index, Map<String, List<JsElement>> addedProperties) {
        if (++checkRecursion > 10) {
            return false;
        }
        boolean isFunction = false;
        // at first try to find the type in the model
        JsObject jsObject = ModelUtils.findJsObjectByName(model, type.getType());
        if (jsObject != null) {
            lastResolvedObjects.add(jsObject);
        }

        for (JsObject libGlobal : ModelExtender.getDefault().getExtendingGlobalObjects(request.result.getSnapshot().getSource().getFileObject())) {
            JsObject found = ModelUtils.findJsObjectByName(libGlobal, type.getType());
            if (found != null && found != libGlobal) {
                jsObject = found;
                lastResolvedObjects.add(jsObject);
                break;
            }
        }

        if (jsObject == null || !jsObject.isDeclared()) {
            boolean isObject = type.getType().equals("Object");   //NOI18N
            if (prop && !isObject) {
                for (IndexResult indexResult : index.findByFqn(type.getType(), JsIndex.FIELD_FLAG)) {
                    JsElement.Kind kind = IndexedElement.Flag.getJsKind(Integer.parseInt(indexResult.getValue(JsIndex.FIELD_FLAG)));
                    if (kind.isFunction()) {
                        isFunction = true;
                    }
                }
            }
            if (!isObject) {
                addObjectPropertiesFromIndex(type.getType(), index, request, addedProperties);
            }
        } else if (jsObject.getDeclarationName() != null) {
            Collection<? extends TypeUsage> assignments = jsObject.getAssignmentForOffset(jsObject.getDeclarationName().getOffsetRange().getEnd());
            for (TypeUsage assignment : assignments) {
                boolean isFun = processTypeInModel(request, model, assignment, lastResolvedObjects, prop, index, addedProperties);
                isFunction = isFunction ? true : isFun;
            }
        }
        return isFunction;
    }
    
    private void addObjectPropertiesToCC(JsObject jsObject, CompletionRequest request, Map<String, List<JsElement>> addedProperties) {
        JsObject prototype = jsObject.getProperty(ModelUtils.PROTOTYPE); // NOI18N
        if (prototype != null) {
            // at first add all prototype properties
            // if the same property is declared in the project directly, then this is replaced.
            addObjectPropertiesToCC(prototype, request, addedProperties);
        }
        for (JsObject property : jsObject.getProperties().values()) {
            if (!(property instanceof JsFunction && ((JsFunction) property).isAnonymous())
                    && !(ModelUtils.getDisplayName(property.getName()).isEmpty())
                    && !property.getModifiers().contains(Modifier.PRIVATE)
                    && !property.getJSKind().isPropertyGetterSetter()) {
                addPropertyToMap(request, addedProperties, property);
            }
        }
    }
    
    private void addObjectPropertiesFromIndex(String fqn, JsIndex jsIndex, CompletionRequest request, Map<String, List<JsElement>> addedProperties) {
        Collection<IndexedElement> properties = jsIndex.getProperties(fqn);
        for (IndexedElement indexedElement : properties) {
            addPropertyToMap(request, addedProperties, indexedElement);
            if (ModelUtils.PROTOTYPE.equals(indexedElement.getName())) {
                Collection<IndexedElement> protoProperties = jsIndex.getProperties(indexedElement.getFQN());
                for (IndexedElement protoProperty : protoProperties) {
                    addPropertyToMap(request, addedProperties, protoProperty);
                }
            }
        }
        if (fqn.equals("Element")) {
            request.addHtmlTagAttributes = true;
        }
    }
    
    private void addPropertyToMap(CompletionRequest request, Map<String, List<JsElement>> addedProperties, JsElement property) {    
        String name = property.getName();
        if (startsWith(name, request.prefix) && !(ModelUtils.getDisplayName(property.getName()).isEmpty())
                && property.getJSKind() != JsElement.Kind.CALLBACK) {
            if (!(name.equals(request.prefix) && !property.isDeclared() && request.anchor == property.getOffset())) { // don't include just the prefix
                List<JsElement> elements = addedProperties.get(name);
                if (!ModelUtils.PROTOTYPE.equals(name)) {
                    if (elements == null || elements.isEmpty()) {
                        List<JsElement> properties = new ArrayList<JsElement>(1);
                        properties.add(property);
                        addedProperties.put(name, properties);
                    } else {
                        if (property.isDeclared()) {
                            boolean addAsNew = true;
                            if (!elements.isEmpty()) {
                                for (int i = 0; i < elements.size(); i++) {
                                    JsElement element = elements.get(i);
                                    FileObject fo = element.getFileObject();
                                    if (!element.isDeclared() || (fo != null && fo.equals(property.getFileObject()))) {
                                        if (!element.isDeclared() || (element.getOffsetRange() == OffsetRange.NONE && property.getOffsetRange() != OffsetRange.NONE)) {
                                            elements.remove(i);
                                            elements.add(property);
                                            addAsNew = false;
                                            break;
                                        } else if (fo != null && fo.equals(property.getFileObject())) {
                                            addAsNew = false;
                                            break;
                                        }
                                    } else if (element.isPlatform() && property.isPlatform()) {
                                        addAsNew = false;
                                        break;
                                    }
                                }
                            }
                            if (addAsNew) {
                                // expect that all items are declaration -> so just add the next declaraiton
                                elements.add(property);
                            }
                        }
                    }
                } else {
                    if (elements == null && property.isPlatform()) {
                        List<JsElement> properties = new ArrayList<JsElement>(1);
                        properties.add(property);
                        addedProperties.put(name, properties);
                    }
                }
            }
        }
    }
    
    
    
    private Map<String, List<JsElement>> getDomCompletionResults(CompletionRequest request) {
        Map<String, List<JsElement>> result = new HashMap<String, List<JsElement>>(1);
        // default window object
        result.putAll(getCompletionFromExpressionChain(request, WINDOW_EXPRESSION_CHAIN));
        return result;
    }

    /** XXX - Once the JS framework support becomes plugable, should be moved to jQueryCompletionHandler getPrefix() */
    private static int getPrefixIndexFromSequence(String prefix) {
        int spaceIndex = prefix.lastIndexOf(" ") + 1; //NOI18N
        int dotIndex = prefix.lastIndexOf(".") + 1; //NOI18N
        int hashIndex = prefix.lastIndexOf("#") + 1; //NOI18N
        int bracketIndex = prefix.lastIndexOf("[") + 1; //NOI18N
        int columnIndex = prefix.lastIndexOf(":") + 1; //NOI18N
        int parenIndex = prefix.lastIndexOf("(") + 1; //NOI18N
        // for file code completion
        int slashIndex = prefix.lastIndexOf('/') + 1; //NOI18N
        return (Math.max(0, Math.max(hashIndex, Math.max(dotIndex, Math.max(parenIndex,Math.max(columnIndex, Math.max(bracketIndex, Math.max(spaceIndex, slashIndex))))))));
    }
    
    
    private Collection<HtmlTagAttribute> getAllAttributes() {
        HtmlModel htmlModel = HtmlModelFactory.getModel(HtmlVersion.HTML5);
        Map<String, HtmlTagAttribute> result = new HashMap<String, HtmlTagAttribute>();
        for (HtmlTag htmlTag : htmlModel.getAllTags()) {
            for (HtmlTagAttribute htmlTagAttribute : htmlTag.getAttributes()) {
                // attributes can probably differ per tag so we can just offer some of them,
                // at least for the CC purposes it should be complete list of attributes for unknown tag
                if (!result.containsKey(htmlTagAttribute.getName())) {
                    result.put(htmlTagAttribute.getName(), htmlTagAttribute);
                }
            }
        }
        return result.values();
    }

}
