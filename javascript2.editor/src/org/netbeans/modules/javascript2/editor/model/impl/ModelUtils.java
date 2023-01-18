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
package org.netbeans.modules.javascript2.editor.model.impl;

import jdk.nashorn.internal.ir.Node;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.SwingUtilities;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.JsCompletionItem;
import org.netbeans.modules.javascript2.editor.doc.spi.JsDocumentationHolder;
import org.netbeans.modules.javascript2.editor.embedding.JsEmbeddingProvider;
import org.netbeans.modules.javascript2.editor.index.IndexedElement;
import org.netbeans.modules.javascript2.editor.index.JsIndex;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.api.lexer.LexUtilities;
import org.netbeans.modules.javascript2.editor.model.DeclarationScope;
import org.netbeans.modules.javascript2.editor.model.Identifier;
import org.netbeans.modules.javascript2.editor.model.JsArray;
import org.netbeans.modules.javascript2.editor.model.JsElement;
import org.netbeans.modules.javascript2.editor.model.JsFunction;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.model.JsReference;
import org.netbeans.modules.javascript2.editor.model.JsWith;
import org.netbeans.modules.javascript2.editor.model.Model;
import org.netbeans.modules.javascript2.editor.model.Occurrence;
import org.netbeans.modules.javascript2.editor.model.Type;
import org.netbeans.modules.javascript2.editor.model.TypeUsage;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Petr Pisl
 */
public class ModelUtils {
      
    public static final String PROTOTYPE = "prototype"; //NOI18N

    public static final String ARGUMENTS = "arguments"; //NOI18N

    private static final String GENERATED_FUNCTION_PREFIX = "_L"; //NOI18N
    
    private static final String GENERATED_ANONYM_PREFIX = "Anonym$"; //NOI18N
    
    private static final List<String> KNOWN_TYPES = Arrays.asList(Type.ARRAY, Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.UNDEFINED);
    
    private static final int MAX_RECURSION_DEEP_RESOLVING_ASSIGNMENTS = 10;
    
    public static JsObjectImpl getJsObject (ModelBuilder builder, List<Identifier> fqName, boolean isLHS) {
        JsObject result = builder.getCurrentObject();
        JsObject tmpObject = null;
        String firstName = fqName.get(0).getName();
        
        while (tmpObject == null && result != null && result.getParent() != null) {
            if (result instanceof JsFunctionImpl) {
                tmpObject = ((JsFunctionImpl)result).getParameter(firstName);
            }
            if (tmpObject == null) {
                if (result.getProperty(firstName) != null) {
                    tmpObject = result;
                }
                result = result.getParent();
            } else {
                result = tmpObject;
            }
        }
        if (tmpObject == null) {
            JsObject current = builder.getCurrentObject();
            if (current instanceof JsWith) {
                tmpObject = current;
            } else {
                DeclarationScope scope = builder.getCurrentDeclarationFunction();
                while (scope != null && tmpObject == null && scope.getParentScope() != null) {
                    tmpObject = ((JsFunction)scope).getParameter(firstName);
                    if (tmpObject == null) {
                        tmpObject = ((JsObject)scope).getProperty(firstName);
                    }
                    scope = scope.getParentScope();
                }
                if (tmpObject == null) {
                    tmpObject = builder.getGlobal();
                } else {
                    result = tmpObject;
                }
            }
        }
        for (int index = (tmpObject instanceof ParameterObject ? 1 : 0); index < fqName.size() ; index++) {
            Identifier name = fqName.get(index);
            result = tmpObject.getProperty(name.getName());
            if (result == null) {
                result = new JsObjectImpl(tmpObject, name, name.getOffsetRange(),
                        (index < (fqName.size() - 1)) ? false : isLHS, tmpObject.getMimeType(), tmpObject.getSourceLabel());
                tmpObject.addProperty(name.getName(), result);
            }
            tmpObject = result;
        }
        return (JsObjectImpl)result;        
    }

    public static boolean isGlobal(JsObject object) {
        return object != null && object.getJSKind() == JsElement.Kind.FILE;
    }

    public static boolean isDescendant(JsObject possibleDescendant, JsObject possibleAncestor) {
        JsObject parent = possibleDescendant;
        while (parent != null && !parent.equals(possibleAncestor)) {
            parent = parent.getParent();
        }
        return parent != null;
    }
    
    public static JsObject findJsObject(Model model, int offset) {
        JsObject result = null;
        JsObject global = model.getGlobalObject();
        result = findJsObject(global, offset);
        if (result == null) {
            result = global;
        }
        return result;
    }
    
    public static JsObject findJsObject(JsObject object, int offset) {
        HashSet<String> visited = new HashSet<String>();
        return findJsObject(object, offset, visited);
    }
    
    public static JsObject findJsObject(JsObject object, int offset, Set<String> visited) {
        JsObjectImpl jsObject = (JsObjectImpl)object;
        visited.add(jsObject.getFullyQualifiedName());
        JsObject result = null;
        JsObject tmpObject = null;
        if (jsObject.containsOffset(offset)) {
            result = jsObject;
            for (JsObject property : jsObject.getProperties().values()) {
                JsElement.Kind kind = property.getJSKind();
                if (kind == JsElement.Kind.OBJECT || kind == JsElement.Kind.ANONYMOUS_OBJECT || kind == JsElement.Kind.OBJECT_LITERAL
                        || kind == JsElement.Kind.FUNCTION || kind == JsElement.Kind.METHOD || kind == JsElement.Kind.CONSTRUCTOR
                        || kind == JsElement.Kind.WITH_OBJECT) {
                    if (!visited.contains(property.getFullyQualifiedName())) {
                        tmpObject = findJsObject(property, offset, visited);
                    }
                }
                if (tmpObject != null) {
                    result = tmpObject;
                    break;
                }
            }
            if ( object.getJSKind() == JsElement.Kind.WITH_OBJECT) {
                for (JsWith innerWith :((JsWith)object).getInnerWiths()){
                    if (!visited.contains(innerWith.getFullyQualifiedName())) {
                        tmpObject = findJsObject(innerWith, offset, visited);
                    }
                    if (tmpObject != null) {
                        result = tmpObject;
                        break;
                    }
                }
            }
            if (object instanceof JsArray) {
                JsArray array = (JsArray)object;
                for (TypeUsage type : array.getTypesInArray()) {
                    if (type.getType().startsWith(SemiTypeResolverVisitor.ST_ANONYM)) {
                        int anonymOffset = Integer.parseInt(type.getType().substring(SemiTypeResolverVisitor.ST_ANONYM.length()));
                        if (anonymOffset > 0) {
                            DeclarationScope scope = getDeclarationScope(array);
                            for (JsObject property : ((JsObject)scope).getProperties().values()) {
                                JsElement.Kind kind = property.getJSKind();
                                if (kind == JsElement.Kind.ANONYMOUS_OBJECT && !visited.contains(property.getFullyQualifiedName())) { 
                                    tmpObject = findJsObject(property, offset, visited);
                                }
                                if (tmpObject != null) {
                                    result = tmpObject;
                                    break;
                                }
                            }
                        }   
                    }
                }
            }
        }
        return result;
    }
    
    public static JsObject findJsObjectByName(JsObject global, String fqName) {
        JsObject result = global;
        JsObject property = result;
        for (StringTokenizer stringTokenizer = new StringTokenizer(fqName, "."); stringTokenizer.hasMoreTokens() && result != null;) {
            String token = stringTokenizer.nextToken();
            property = result.getProperty(token);
            if (property == null) {
                result = (result instanceof JsFunction)
                        ? ((JsFunction)result).getParameter(token)
                        : null;
                if (result == null) {
                    break;
                }
            } else {
                result = property;
            }
        }
        return result;
    }
    
    public static JsObject findJsObjectByName(Model model, String fqName) {
        return findJsObjectByName(model.getGlobalObject(), fqName);
    }
    
    public static JsObject getGlobalObject(JsObject jsObject) {
        JsObject result = jsObject;
        while(result.getParent() != null) {
            result = result.getParent();
        }
        return result;
    }

    public static DeclarationScope getDeclarationScope(JsObject object) {
        assert object != null;

        JsObject result =  object;
        while (result.getParent() != null && !(result.getParent() instanceof DeclarationScope)) {
            result = result.getParent();
        }
        if (result.getParent() != null && result.getParent() instanceof DeclarationScope) {
            result = result.getParent();
        } 
        if (!(result instanceof DeclarationScope)) {
            // this shouldn't happened, basically it means that the model is broken and has an object without parent
            result = getGlobalObject(object);
        }
        return (DeclarationScope)result;
    }

    public static DeclarationScope getDeclarationScope(Model model, int offset) {
        DeclarationScope result = null;
        JsObject global = model.getGlobalObject();
        result = getDeclarationScope((DeclarationScope)global, offset);
        if (result == null) {
            result = (DeclarationScope)global;
        }
        return result;
    }
    
    public static DeclarationScope getDeclarationScope(DeclarationScope scope, int offset) {
        
        DeclarationScopeImpl dScope = (DeclarationScopeImpl)scope;
        DeclarationScope result = null; 
        if (result == null) {
            if (dScope.getOffsetRange().containsInclusive(offset)) {
                result = dScope;
                boolean deep = true;
                while (deep) {
                    deep = false;
                    for (DeclarationScope innerScope : result.getChildrenScopes()) {
                        if ((innerScope instanceof DeclarationScopeImpl)
                                && ((DeclarationScopeImpl)innerScope).getOffsetRange().containsInclusive(offset)) {
                            result = innerScope;
                            deep = true;
                            break;
                        }
                        
                    }
                }
            }
        }
        return result;
    }
    
    public static OffsetRange documentOffsetRange(JsParserResult result, int start, int end) {
        int lStart = LexUtilities.getLexerOffset(result, start);
        int lEnd = LexUtilities.getLexerOffset(result, end);
        if (lStart == -1 || lEnd == -1) {
            return OffsetRange.NONE;
        }
        if (lEnd < lStart) {
            // TODO this is a workaround for bug in nashorn, when sometime the start and end are not crorrect
            int length = lStart - lEnd;
            lEnd = lStart + length;
        }
        return new OffsetRange(lStart, lEnd);
    }
    
    /**
     * Returns all variables that are available in the scope
     * @param inScope
     * @return 
     */
    public static Collection<? extends JsObject> getVariables(DeclarationScope inScope) {
        HashMap<String, JsObject> result = new HashMap<String, JsObject>();
        while (inScope != null) {
            for (JsObject object : ((JsObject)inScope).getProperties().values()) {
                if (!result.containsKey(object.getName()) && object.getModifiers().contains(Modifier.PRIVATE)) {
                    result.put(object.getName(), object);
                }
            }
            for (JsObject object : ((JsFunction)inScope).getParameters()) {
                if (!result.containsKey(object.getName())) {
                    result.put(object.getName(), object);
                }
            }
            for (JsObject object : ((JsObject)inScope).getProperties().values()) {
                if (!result.containsKey(object.getName())) {
                    result.put(object.getName(), object);
                }
            }
            if (inScope.getParentScope() != null && !result.containsKey(((JsObject)inScope).getName())) {
                result.put(((JsObject)inScope).getName(), (JsObject)inScope);
            }
            inScope = inScope.getParentScope();
        }
        return result.values();
    }
    

    public static Collection<? extends JsObject> getVariables(Model model, int offset) {
        DeclarationScope scope = ModelUtils.getDeclarationScope(model, offset);
        return  getVariables(scope);
    }
    
    public static JsObject getJsObjectByName(DeclarationScope inScope, String simpleName) {
        Collection<? extends JsObject> variables = ModelUtils.getVariables(inScope);
        for (JsObject jsObject : variables) {
            if (simpleName.equals(jsObject.getName())) {
                return jsObject;
            }
        }
        return null;
    }
    private static final Collection<JsTokenId> CTX_DELIMITERS = Arrays.asList(
            JsTokenId.BRACKET_LEFT_CURLY, JsTokenId.BRACKET_RIGHT_CURLY,
            JsTokenId.OPERATOR_SEMICOLON);

    private static Collection<TypeUsage> tryResolveWindowProperty(Model model, JsIndex jsIndex, String name) {
        // since issue #215863
        String fqn = null;
        int offset = -1;
        for (IndexedElement indexedElement : jsIndex.getProperties("window")) { //NOI18N
            if (indexedElement.getName().equals(name)) {
                offset = indexedElement.getOffset();
                fqn = "window." + name;
                break;
            }
        }
        if (fqn == null) {
            for (IndexedElement indexedElement : jsIndex.getProperties("Window.prototype")) { //NOI18N
                if (indexedElement.getName().equals(name)) {
                    offset = indexedElement.getOffset();
                    fqn = "Window.prototype." + name; //NOI18N
                    break;
                }
            }
        }
        if (fqn != null) {
            List<TypeUsage> fromAssignment = new ArrayList<TypeUsage>();
            resolveAssignments(model, jsIndex, fqn, offset, fromAssignment);
            if (fromAssignment.isEmpty()) {
                fromAssignment.add(new TypeUsageImpl(fqn));
            } 
            return fromAssignment;
        }
        return null;
    }
    
    private enum State {
        INIT
    }
    
    private static String getSemiType(TokenSequence<JsTokenId> ts, int offset) {
        
        String result = "UNKNOWN";
        ts.move(offset);
        if (!ts.moveNext()) {
           return result;
        }
    
        State state = State.INIT;
        while (ts.movePrevious()) {
            Token<JsTokenId> token = ts.token();
            if (!CTX_DELIMITERS.contains(token.id())){
                switch (state) {
                    case INIT:
                        if (token.id() == JsTokenId.IDENTIFIER)
                        break;
                }
            }
        }
        return result;
    }

    public static Collection<TypeUsage> resolveSemiTypeOfExpression(ModelBuilder builder, Node expression) {
        Collection<TypeUsage> result = new HashSet<TypeUsage>();
        SemiTypeResolverVisitor visitor = new SemiTypeResolverVisitor();
        if (expression != null) {
            result = visitor.getSemiTypes(expression);
        }
        if (builder.getCurrentWith()!= null) {
            Collection<TypeUsage> withResult = new HashSet<TypeUsage>();
            String withSemi = SemiTypeResolverVisitor.ST_WITH + builder.getCurrentWith().getFullyQualifiedName();
            
            for(TypeUsage type : result) {
                if (!KNOWN_TYPES.contains(type.getType())) {
                    withResult.add(new TypeUsageImpl(withSemi + type.getType(), type.getOffset(), type.isResolved()));
                } else {
                    withResult.add(type);
                }
            }
            result = withResult;
        }
        return result;
    }
    
    public static Collection<TypeUsage> resolveTypeFromSemiType(JsObject object, TypeUsage type) {
        Set<TypeUsage> result = new HashSet<TypeUsage>();
        if (type.isResolved()) {
            result.add(type);
        } else if (Type.UNDEFINED.equals(type.getType())) {
            if (object.getJSKind() == JsElement.Kind.CONSTRUCTOR) {
                result.add(new TypeUsageImpl(object.getFullyQualifiedName(), type.getOffset(), true));
            } else {
                result.add(new TypeUsageImpl(Type.UNDEFINED, type.getOffset(), true));
            }
        } else if (JsEmbeddingProvider.containsGeneratedIdentifier(type.getType())) {
            result.add(new TypeUsageImpl(Type.UNDEFINED, type.getOffset(), true));
        } else if (SemiTypeResolverVisitor.ST_THIS.equals(type.getType())) {
            JsObject parent = resolveThis(object);
            if (parent != null) {
                result.add(new TypeUsageImpl(parent.getFullyQualifiedName(), type.getOffset(), true));
            }
        } else if (type.getType().startsWith(SemiTypeResolverVisitor.ST_THIS)) {
             JsObject parent = resolveThis(object);
            if (parent != null) {
                Collection<TypeUsage> locally = resolveSemiTypeChain(parent, type.getType().substring(6));
                if (locally.isEmpty()) {
                    result.add(new TypeUsageImpl(type.getType().replace(SemiTypeResolverVisitor.ST_THIS, parent.getFullyQualifiedName()), type.getOffset(), false));
                } else {
                    if (locally.size() == 1) {
                        TypeUsage localType = locally.iterator().next();
                        if (localType.isResolved()) {
                            JsObject rObject = ModelUtils.findJsObjectByName(ModelUtils.getGlobalObject(object), localType.getType());
                            JsFunction function = rObject instanceof JsFunctionImpl
                                    ? (JsFunctionImpl) rObject
                                    : rObject instanceof JsFunctionReference ? ((JsFunctionReference) rObject).getOriginal() : null;
                            if (function != null && function.getParent() != null && object != null
                                    && function.getParent().equals(object.getParent())
                                    && object.getDeclarationName() != null) {
                                // creates reference to the original function
                                object.getParent().addProperty(object.getName(), new JsFunctionReference(
                                        object.getParent(), object.getDeclarationName(), function, true, null));
                            } 
                        } 
                    }
                    result.addAll(locally);
                }
            }
        } else if (type.getType().startsWith(SemiTypeResolverVisitor.ST_NEW)) {
            result.addAll(resolveSemiTypeCallChain(object, type));
        } else if (type.getType().startsWith(SemiTypeResolverVisitor.ST_CALL)) {
            result.addAll(resolveSemiTypeCallChain(object, type));
        } else if(type.getType().startsWith(SemiTypeResolverVisitor.ST_ANONYM)){
            String offsetPart = type.getType().substring(8);
            String rest = "";
            int index = offsetPart.indexOf(SemiTypeResolverVisitor.ST_START_DELIMITER);
            if (index > -1) {
                rest = offsetPart.substring(index);
                offsetPart = offsetPart.substring(0, index);
            }
            int start = Integer.parseInt(offsetPart);
//            JsObject globalObject = ModelUtils.getGlobalObject(object);
            JsObject byOffset = ModelUtils.findJsObject(object, start);
            if (byOffset == null) {
                JsObject globalObject = ModelUtils.getGlobalObject(object);
                byOffset = ModelUtils.findJsObject(globalObject, start);
            }
            if(byOffset != null && byOffset.isAnonymous()) {
                if (rest.isEmpty()) {
                    result.add(new TypeUsageImpl(byOffset.getFullyQualifiedName(), byOffset.getOffset(), true));
                } else {
                    String newType= SemiTypeResolverVisitor.ST_EXP + byOffset.getFullyQualifiedName().replace(".", SemiTypeResolverVisitor.ST_PRO);
                    newType = newType + rest;
                    result.add(new TypeUsageImpl(newType, byOffset.getOffset(), false));
                }
            }
//            for(JsObject children : globalObject.getProperties().values()) {
//                if(children.getOffset() == start && children.getName().startsWith("Anonym$")) {
//                    result.add(new TypeUsageImpl(ModelUtils.createFQN(children), children.getOffset(), true));
//                    break;
//                }
//                
//            }
        } else if(type.getType().startsWith(SemiTypeResolverVisitor.ST_VAR)){
            String name = type.getType().substring(5);
            JsFunction declarationScope = object instanceof DeclarationScope ? (JsFunction)object : (JsFunction)getDeclarationScope(object);
            Collection<? extends JsObject> variables = ModelUtils.getVariables(declarationScope);
            if (declarationScope != null) {
                boolean resolved = false;
                for (JsObject variable : variables) {
                    if (variable.getName().equals(name)) {
                        String newVarType;
                        if (!variable.getAssignments().isEmpty()) {
                             newVarType= SemiTypeResolverVisitor.ST_EXP + variable.getFullyQualifiedName().replace(".", SemiTypeResolverVisitor.ST_PRO);
                             result.add(new TypeUsageImpl(newVarType, type.getOffset(), false));
                             resolved = true;
                             break;
                        } else {
                            if (variable.getJSKind() != JsElement.Kind.PARAMETER) {
                                if (variable.getJSKind().isFunction() && object.getAssignments().size() == 1
                                        && object.getParent() != null && object.getDeclarationName() != null) {
                                    JsObject oldProperty = object.getParent().getProperty(object.getName());
                                    JsObject newProperty = new JsFunctionReference(object.getParent(), object.getDeclarationName(), (JsFunction)variable, true, oldProperty.getModifiers());
                                    for (Occurrence occurrence : oldProperty.getOccurrences()) {
                                        newProperty.addOccurrence(occurrence.getOffsetRange());
                                    }
                                    object.getParent().addProperty(object.getName(), newProperty);
                                    
                                } else {
                                    newVarType = variable.getFullyQualifiedName();
                                    result.add(new TypeUsageImpl(newVarType, type.getOffset(), false));
                                }
                                resolved = true;
                                break;
                            }
                        }
                    }
                }
                if (!resolved) {
                    Collection<? extends JsObject> parameters = declarationScope.getParameters();
                    boolean isParameter = false;
                    for (JsObject parameter : parameters) {
                        if (name.equals(parameter.getName())) {
                            Collection<? extends TypeUsage> assignments = parameter.getAssignmentForOffset(parameter.getOffset());
                            result.addAll(assignments);
                            isParameter = true;
                            break;
                        }
                    }
                    if (!isParameter) {
                        result.add(new TypeUsageImpl(name, type.getOffset(), false));
                    }
                }
            }
        } else if(type.getType().startsWith("@param;")) {
            String functionName = type.getType().substring(7);
            int index = functionName.indexOf(":");
            if (index > 0) {
                String fqn = functionName.substring(0, index);
                JsObject globalObject = ModelUtils.getGlobalObject(object);
                JsObject function = ModelUtils.findJsObjectByName(globalObject, fqn);
                if(function instanceof JsFunction) {
                    JsObject param = ((JsFunction)function).getParameter(functionName.substring(index + 1));
                    if(param != null) {
                        result.addAll(param.getAssignments());
                    }
                }
            }

        } else {
            result.add(type);
        }
        return result;
    }
    
    private static JsObject resolveThis(JsObject object) {
        JsObject parent = null;
        if (object.getJSKind() == JsElement.Kind.CONSTRUCTOR) {
            parent = object;
        } else {
            if (object.getParent() != null && object.getParent().getJSKind() != JsElement.Kind.FILE) {
                parent = object.getParent();
            } else {
                parent = object;
            }
        }
        if (parent != null && (parent.getJSKind() == JsElement.Kind.FUNCTION || parent.getJSKind() == JsElement.Kind.METHOD)) {
            if (parent.getParent().getJSKind() != JsElement.Kind.FILE) {
                JsObject grandParent = parent.getParent();
                if (grandParent != null 
                        && (grandParent.getJSKind() == JsElement.Kind.OBJECT_LITERAL || PROTOTYPE.equals(grandParent.getName()))) {
                    parent = grandParent;
                    if (PROTOTYPE.equals(parent.getName()) && parent.getParent() != null) {
                        parent = parent.getParent();
                    }
                }
            }
        }
        // if the parent is priviliged the this refers the constructor => find the constructor
        while (parent != null && parent.getParent() != null && parent.getModifiers().contains(Modifier.PROTECTED)) {
            parent = parent.getParent();
        }
        return parent;
    }
    
    private static Collection<TypeUsage> resolveSemiTypeCallChain(JsObject object, TypeUsage type) {
        Set<TypeUsage> result = new HashSet<TypeUsage>();
        DeclarationScope declarationScope = ModelUtils.getDeclarationScope(object);
        JsObject function = null;
        boolean calledNew = false;
        int index = -1;
        int dotIndex = -1;
        if (type.getType().startsWith(SemiTypeResolverVisitor.ST_CALL)) {
            index = 6;
        } else if (type.getType().startsWith(SemiTypeResolverVisitor.ST_NEW)) {
            index = 5;
            calledNew = true;
        }
        String name = type.getType().substring(index);
        if (declarationScope != null) {
            index = name.indexOf(SemiTypeResolverVisitor.ST_START_DELIMITER);
            if (index > -1) {
                name = name.substring(0, index);
            }
            Collection<? extends JsObject> variables = ModelUtils.getVariables(declarationScope);
            dotIndex = name.indexOf('.');
            String firstSpace = dotIndex == -1 ? name : name.substring(0, name.indexOf('.'));
            
            for (JsObject variable : variables) {
                if (variable.getName().equals(firstSpace)) {
                    function = variable;
                    break;
                }
            }
        }
        if (dotIndex != -1 && function != null) {
            function = ModelUtils.findJsObjectByName(function, name.substring(dotIndex + 1));
        }
        if (function != null) {
            if (index == -1) {
                if (function instanceof JsFunction) {
                    if (calledNew) {
                        result.add(new TypeUsageImpl(function.getFullyQualifiedName(), type.getOffset(), true));
                    } else {
                        result.addAll(((JsFunction) function).getReturnTypes());
                    }
                } else {
                    if (calledNew) {
                        result.add(new TypeUsageImpl(function.getFullyQualifiedName(), type.getOffset(), true));
                    } else {
                        result.add(type);
                    }
                }
            } else {
                result.add(new TypeUsageImpl(type.getType().replace(name, function.getFullyQualifiedName()), type.getOffset(), false));
            }
        } else {
            result.add(type);
        }
        return result;
    }
    
    /**
     * 
     * @param object
     * @param chain
     * @return 
     */
    private static Collection<TypeUsage> resolveSemiTypeChain(JsObject object, String chain) {
        Collection<TypeUsage> result = new HashSet<TypeUsage>();
        if (chain.isEmpty()) {
            return result;
        }
        if (PROTOTYPE.equals(object.getName())) {
            object = object.getParent();
            if (object == null) {
                return result;
            }
        }
        String[] parts = chain.substring(1).split(SemiTypeResolverVisitor.ST_START_DELIMITER);
        JsObject resultObject = null;
        JsObject testObject = object;
        String kind = "";   //NOI18N
        String name;
        for (String part : parts) {
            int index = part.indexOf(";");  //NOI18N
            if (index > 0) {
                kind = part.substring(0, index);
                name = part.substring(index + 1);
                resultObject = testObject.getProperty(name);
                if (resultObject == null) {
                    JsObject prototype = testObject.getProperty(PROTOTYPE);
                    if (prototype != null) {
                        resultObject = prototype.getProperty(name);
                    }
                }
                if (resultObject == null) {
                    break;
                }
                testObject = resultObject;
            }
            else {
                break;
            }
        }
        if (resultObject != null) {
            if (resultObject instanceof JsFunction) {
                if ("call".endsWith(kind)) {
                    ModelUtils.addUniqueType(result, (Collection<TypeUsage>)((JsFunction)resultObject).getReturnTypes());
                } else {
                    ModelUtils.addUniqueType(result, new TypeUsageImpl(resultObject.getFullyQualifiedName(), -1, true));
                }
            }else {
                Collection<? extends TypeUsage> assignments = resultObject.getAssignments();
                if (assignments.isEmpty()) {
                    ModelUtils.addUniqueType(result, new TypeUsageImpl(resultObject.getFullyQualifiedName(), -1, true));
                } else {
                    ModelUtils.addUniqueType(result, (Collection<TypeUsage>)resultObject.getAssignments());
                }
            }
        }
        return result;
    }
    
    public static Collection<TypeUsage> resolveTypeFromExpression (Model model, @NullAllowed JsIndex jsIndex, List<String> exp, int offset, boolean includeAllPossible) {
        List<JsObject> localObjects = new ArrayList<JsObject>();
        List<JsObject> lastResolvedObjects = new ArrayList<JsObject>();
        List<TypeUsage> lastResolvedTypes = new ArrayList<TypeUsage>();
        
            for (int i = exp.size() - 1; i > -1; i--) {
                String kind = exp.get(i);
                String name = exp.get(--i);
                if (name.startsWith("@ano:")){
                    String[] parts = name.split(":");
                    int anoOffset = Integer.parseInt(parts[1]);
                    JsObject anonym = ModelUtils.findJsObject(model, anoOffset);
                    lastResolvedObjects.add(anonym);
                    continue;
                }
                if ("this".equals(name)) {
                    JsObject thisObject = ModelUtils.findJsObject(model, offset);
                    JsObject first = thisObject;
                    while (thisObject != null && thisObject.getParent() != null && thisObject.getParent().getJSKind() != JsElement.Kind.FILE
                            && thisObject.getJSKind() != JsElement.Kind.CONSTRUCTOR
                            && thisObject.getJSKind() != JsElement.Kind.ANONYMOUS_OBJECT
                            && thisObject.getJSKind() != JsElement.Kind.OBJECT_LITERAL) {
                        thisObject = thisObject.getParent();
                    }
                    if ((thisObject == null || thisObject.getParent() == null) && first != null) {
                        thisObject = first;
                    }
                    if (thisObject != null) {
                        name = thisObject.getName();
                    }
                }
                if (i == (exp.size() - 2)) {
                    JsObject localObject = null;
                    // resolving the first part of expression
                    // find possible variables from local context, index contains only 
                    // public definition, we are interested in the private here as well
                    int index = name.lastIndexOf('.');
                    // needs to look, whether the expression is in a with statement
                    Collection<? extends TypeUsage> typeFromWith = getTypeFromWith(model, offset);
                    if (!typeFromWith.isEmpty()) {
                        String firstNamePart = index == -1 ? name : name.substring(0, index);
                        String changedName = name;
                        for (TypeUsage type : typeFromWith) {
                            //Collection<TypeUsage> resolveTypeFromSemiType = ModelUtils.resolveTypeFromSemiType(model.getGlobalObject(), type);
                            String sType = type.getType();
//                            if (sType.startsWith("@exp;")) {
//                                sType = sType.substring(5);
//                                sType = sType.replace("@pro;", ".");
//                            }
                            localObject = ModelUtils.findJsObjectByName(model, sType);
                            if (localObject != null && localObject.getProperty(firstNamePart) != null) {
                                changedName = localObject.getFullyQualifiedName() + "." + name;
                            } else {
                                lastResolvedTypes.add(new TypeUsageImpl(sType + "." + name, -1, true));
                            }
                        }
                        name = changedName;
                    }
                    
                    if (index > -1) { // the first part is a fqn
                        localObject = ModelUtils.findJsObjectByName(model, name);
                        if (localObject != null) {
                            localObjects.add(localObject);
                        }
                    } else {
                        boolean canBeWindowsProp = true;
                        for (JsObject object : model.getVariables(offset)) {
                            if (object.getName().equals(name)) {
                                localObjects.add(object);
                                localObject = object;
                                break;
                            }
                        }
                        if (localObject != null && localObject.getJSKind().isFunction() && i - 2 > -1) {
                            JsFunction localFunc = (JsFunction)localObject;
                            String paramName = exp.get(i - 2);
                            if (localFunc.getParameter(paramName) != null) {
                                canBeWindowsProp = false;
                            }
                        }

                        for (JsObject libGlobal : ModelExtender.getDefault().getExtendingGlobalObjects(model.getGlobalObject().getFileObject())) {
                            assert libGlobal != null;
                            for (JsObject object : libGlobal.getProperties().values()) {
                                if (object.getName().equals(name)) {
                                    //localObjects.add(object);
                                    lastResolvedTypes.add(new TypeUsageImpl(object.getName(), -1, true));
                                    break;
                                }
                            }
                        }
                        if (jsIndex != null && canBeWindowsProp) {
                            Collection<TypeUsage> windowProperty = tryResolveWindowProperty(model, jsIndex, name);
                            if (windowProperty != null && !windowProperty.isEmpty()) {
                                lastResolvedTypes.addAll(windowProperty);
                            }
                        }
                    }
                    if(localObject == null || (localObject.getJSKind() != JsElement.Kind.PARAMETER
                            && (ModelUtils.isGlobal(localObject.getParent()) || localObject.getJSKind() != JsElement.Kind.VARIABLE))) {
                        // Add global variables from index
//                        Collection<IndexedElement> globalVars = jsIndex.getGlobalVar(name);
//                        for (IndexedElement globalVar : globalVars) {
//                            if(name.equals(globalVar.getName())) {
//                                Collection<TypeUsage> assignments = globalVar.getAssignments();
//                                if (assignments.isEmpty()) {
//                                    lastResolvedTypes.add(new TypeUsageImpl(name, -1, true));
//                                } else {
//                                    lastResolvedTypes.addAll(assignments);
//                                    }
//                        }
//                    }
                        List<TypeUsage> fromAssignments = new ArrayList<TypeUsage>();
//                        if (localObject != null) {
//                            //make it only for the right offset
//                            for(TypeUsage type: localObject.getAssignmentForOffset(offset)) {
//                                resolveAssignments(jsIndex, type.getType(), fromAssignments);
//                            }
//                        } else {
                        if ("@pro".equals(kind) && jsIndex != null) { //NOI18N
                            resolveAssignments(model, jsIndex, name, -1,  fromAssignments);
                        } 
//                        }
                        lastResolvedTypes.addAll(fromAssignments);
                        if (!typeFromWith.isEmpty()) {
//                            Collection<TypeUsage> resolveTypes = ModelUtils.resolveTypes(typeFromWith, parserRestult);
                            
                            for (TypeUsage typeUsage : typeFromWith) {
                                String sType = typeUsage.getType();
                                if (sType.startsWith("@exp;")) {
                                    sType = sType.substring(5);
                                    sType = sType.replace("@pro;", ".");
                                }   
                                ModelUtils.resolveAssignments(model, jsIndex, sType, typeUsage.getOffset(), fromAssignments);
                                for (TypeUsage typeUsage1 : fromAssignments) {
                                    String localFqn = localObject != null ? localObject.getFullyQualifiedName() : null;
                                    if (localFqn != null  && name.startsWith(localFqn) && name.length() > localFqn.length() ) {
                                        lastResolvedTypes.add(new TypeUsageImpl(typeUsage1.getType() + kind + ";" + name.substring(localFqn.length() + 1), typeUsage.getOffset(), false));
                                    } else {
                                        if (!typeUsage1.getType().equals(name)) {
                                            lastResolvedTypes.add(new TypeUsageImpl(typeUsage1.getType() + kind + ";" + name, typeUsage.getOffset(), false));
                                        } else {
                                            lastResolvedTypes.add(typeUsage1);
                                        }
                                    }
                                }
                                
                            }
                        }
                    }
                    
                    if(!localObjects.isEmpty()){
                        for(JsObject lObject : localObjects) {
                            if(lObject.getAssignmentForOffset(offset).isEmpty()) {
                                boolean addAsType = lObject.getJSKind() == JsElement.Kind.OBJECT_LITERAL;
                                if (lObject instanceof JsObjectReference) {
                                    // translate reference objects to the original objects / type
                                    JsObject original = ((JsObjectReference)lObject).getOriginal();
                                    if (original != null){
                                        name = original.getDeclarationName() != null ? original.getDeclarationName().getName() : original.getName();
                                    }
                                }
                                if(addAsType) {
                                    // here it doesn't have to be real type, it's possible that it's just an object name
                                    lastResolvedTypes.add(new TypeUsageImpl(name, -1, true));
                                }
                            }
                            if ("@mtd".equals(kind)) {  //NOI18N
                                if (lObject.getJSKind().isFunction()) {
                                    // if it's a method call, add all retuturn types
                                    lastResolvedTypes.addAll(((JsFunction) lObject).getReturnTypes());
                                }
                                int lastCallOffset = -1;
                                for (Occurrence occurrence : lObject.getOccurrences()) {
                                    if (lastCallOffset < occurrence.getOffsetRange().getStart() && occurrence.getOffsetRange().getStart() <= offset) {
                                        lastCallOffset = occurrence.getOffsetRange().getStart();
                                    }
                                }
                                Collection<TypeUsage> returnTypesFromFrameworks = model.getReturnTypesFromFrameworks(lObject.getName(), lastCallOffset);
                                if (returnTypesFromFrameworks != null && !returnTypesFromFrameworks.isEmpty()) {
                                    lastResolvedTypes.addAll(returnTypesFromFrameworks);
                                }
                                if (jsIndex != null) {
                                    Collection<? extends IndexResult> findByFqn = jsIndex.findByFqn(name, JsIndex.TERMS_BASIC_INFO);
                                    for (Iterator<? extends IndexResult> iterator = findByFqn.iterator(); iterator.hasNext();) {
                                        IndexedElement indexElement = IndexedElement.create(iterator.next());
                                        if(indexElement instanceof IndexedElement.FunctionIndexedElement) {
                                            IndexedElement.FunctionIndexedElement iFunction = (IndexedElement.FunctionIndexedElement)indexElement;
                                            for (String type : iFunction.getReturnTypes()) {
                                                lastResolvedTypes.add(new TypeUsageImpl(type, -1, false));
                                            }
                                        }

                                    }
                                }
                            } else if ("@arr".equals(kind) && lObject instanceof JsArray) {
                                lastResolvedTypes.addAll(((JsArray) lObject).getTypesInArray());
                            } else {
                                // just property
                                 Collection<? extends Type> lastTypeAssignment = lObject.getAssignmentForOffset(offset);
                                // we need to process the object later anyway. To get learning cc, see issue #224453
                                lastResolvedObjects.add(lObject);
                                if (!lastTypeAssignment.isEmpty()) {
                                    // go through the assignments and find the last object / type in the assignment chain
                                    // it solve assignements like a = b; b = c; c = d;. the result for a should be d.
                                    resolveAssignments(model, lObject, offset, lastResolvedObjects, lastResolvedTypes);
                                    break;
                                }
                            }
                        }
                    } 
                    // now we should have collected possible local objects
                    // also objects from index, that fits the first part of the expression
                } else {
                    List<JsObject> newResolvedObjects = new ArrayList<JsObject>();
                    List<TypeUsage> newResolvedTypes = new ArrayList<TypeUsage>();
                    for (JsObject localObject : lastResolvedObjects) {
                        // go through the loca object and try find the method / property from the next expression part
                        JsObject property = ((JsObject) localObject).getProperty(name);
                        if (property != null) {
                            if ("@mtd".equals(kind)) {  //NOI18N
                                if (property.getJSKind().isFunction()) {
                                    //Collection<TypeUsage> resovledTypes = resolveTypeFromSemiType(model, property, ((JsFunction) property).getReturnTypes());
                                    Collection<? extends TypeUsage> resovledTypes = ((JsFunction) property).getReturnTypes();
                                    newResolvedTypes.addAll(resovledTypes);
                                }
                            } else if ("@arr".equals(kind)) {
                                if (property instanceof JsArray) {
                                    newResolvedTypes.addAll(((JsArray) property).getTypesInArray());
                                }
                            } else {
                                Collection<? extends TypeUsage> lastTypeAssignment = property.getAssignmentForOffset(offset);
                                if (lastTypeAssignment.isEmpty()) {
                                    newResolvedObjects.add(property);
                                } else {
                                    newResolvedTypes.addAll(lastTypeAssignment);
                                    if(!property.getProperties().isEmpty()) {
                                        newResolvedObjects.add(property);
                                    }
                                }
                            }
                        }
                    }
                    
                    
                    
                    for (TypeUsage typeUsage : lastResolvedTypes) {
                        if (jsIndex != null) {
                            // for the type build the prototype chain.
                            Collection<String> prototypeChain = new ArrayList<String>();
                            String typeName = typeUsage.getType();
                            if (typeName.contains(SemiTypeResolverVisitor.ST_EXP)) {
                                typeName = typeName.substring(typeName.indexOf(SemiTypeResolverVisitor.ST_EXP) + SemiTypeResolverVisitor.ST_EXP.length());
                            }
                            if (typeName.contains(SemiTypeResolverVisitor.ST_PRO)) {
                                typeName = typeName.replace(SemiTypeResolverVisitor.ST_PRO, ".");
                            }
                            prototypeChain.add(typeName);
                            prototypeChain.addAll(findPrototypeChain(typeName, jsIndex));

                            Collection<? extends IndexResult> indexResults = null;
                            String propertyToCheck = null;
                            for (String fqn : prototypeChain) {
                                // at first look at the properties of the object
                                propertyToCheck = fqn + "." + name;
                                indexResults = jsIndex.findByFqn(propertyToCheck,
                                        JsIndex.FIELD_FLAG, JsIndex.FIELD_RETURN_TYPES, JsIndex.FIELD_ARRAY_TYPES, JsIndex.FIELD_ASSIGNMENTS); //NOI18N
                                
                                if (indexResults.isEmpty() && !fqn.endsWith(".prototype")) {
                                    // if the property was not found, try to look at the prototype of the object
                                    propertyToCheck = fqn + ".prototype." + name;
                                    indexResults = jsIndex.findByFqn(propertyToCheck,
                                            JsIndex.FIELD_FLAG, JsIndex.FIELD_RETURN_TYPES, JsIndex.FIELD_ARRAY_TYPES, JsIndex.FIELD_ASSIGNMENTS); //NOI18N
                                }
                                if(!indexResults.isEmpty()) {
                                    // if the property / method was already found, we don't need to continue.
                                    // in the runtime is also used the first one that is found in the prototype chain
                                    break;
                                }
                                propertyToCheck = null;
                            }

                            boolean checkProperty = (indexResults == null || indexResults.isEmpty()) && !"@mtd".equals(kind);
                            if (indexResults != null) {
                                for (IndexResult indexResult : indexResults) {
                                    // go through the resul from index and add appropriate types to the new resolved
                                    JsElement.Kind jsKind = IndexedElement.Flag.getJsKind(Integer.parseInt(indexResult.getValue(JsIndex.FIELD_FLAG)));
                                    if ("@mtd".equals(kind) && jsKind.isFunction()) {
                                        //Collection<TypeUsage> resolved = resolveTypeFromSemiType(model, ModelUtils.findJsObject(model, offset), IndexedElement.getReturnTypes(indexResult));
                                        Collection<TypeUsage> resolvedTypes = IndexedElement.getReturnTypes(indexResult);
                                        ModelUtils.addUniqueType(newResolvedTypes, resolvedTypes);
                                    } else if ("@arr".equals(kind)) { // NOI18N
                                        Collection<TypeUsage> resolvedTypes = IndexedElement.getArrayTypes(indexResult);
                                        ModelUtils.addUniqueType(newResolvedTypes, resolvedTypes);
                                    } else {
                                        checkProperty = true;
                                    }
                                }
                            }
                            if (checkProperty) {
                                String propertyFQN = propertyToCheck != null ? propertyToCheck : typeName + "." + name;
                                List<TypeUsage> fromAssignment = new ArrayList<TypeUsage>();
                                resolveAssignments(model, jsIndex, propertyFQN, -1, fromAssignment);
                                if (fromAssignment.isEmpty()) {
                                    ModelUtils.addUniqueType(newResolvedTypes, new TypeUsageImpl(propertyFQN));
                                } else {
                                    ModelUtils.addUniqueType(newResolvedTypes, fromAssignment);
                                }
                            }
                        }
                        // from libraries look for top level types
                        for (JsObject libGlobal : ModelExtender.getDefault().getExtendingGlobalObjects(model.getGlobalObject().getFileObject())) {
                            for (JsObject object : libGlobal.getProperties().values()) {
                                if (object.getName().equals(typeUsage.getType())) {
                                    JsObject property = object.getProperty(name);
                                    if (property != null) {
                                        JsElement.Kind jsKind = property.getJSKind();
                                        if ("@mtd".equals(kind) && jsKind.isFunction()) {
                                            newResolvedTypes.addAll(((JsFunction) property).getReturnTypes());
                                        } else {
                                            newResolvedObjects.add(property);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    lastResolvedObjects = newResolvedObjects;
                    lastResolvedTypes = newResolvedTypes;
                }
            }
            
            HashMap<String, TypeUsage> resultTypes  = new HashMap<String, TypeUsage> ();
            for (TypeUsage typeUsage : lastResolvedTypes) {
                if(!resultTypes.containsKey(typeUsage.getType())) {
                    resultTypes.put(typeUsage.getType(), typeUsage);
                }
            }
            for (JsObject jsObject : lastResolvedObjects) {
                if (jsObject.isDeclared()) {
                    String fqn = jsObject.getFullyQualifiedName();
                    if (!resultTypes.containsKey(fqn)) {
                        if (includeAllPossible || hasDeclaredProperty(jsObject)) {
                            resultTypes.put(fqn, new TypeUsageImpl(fqn, offset));
                        }
                    }
                }
            }
            return resultTypes.values();
    }

    public static boolean hasDeclaredProperty(JsObject jsObject) {
        boolean result =  false;
        
        Iterator<? extends JsObject> it = jsObject.getProperties().values().iterator();
        while (!result && it.hasNext()) {
            JsObject property = it.next();
            result = property.isDeclared();
            if (!result) {
                result = hasDeclaredProperty(property);
            } 
        }

        return result;
    }
    
    public static List<String> expressionFromType(TypeUsage type) {
        String sexp = type.getType();
        if ((sexp.startsWith("@exp;") || sexp.startsWith("@new;") || sexp.startsWith("@arr;") || sexp.contains("@pro;")
                || sexp.startsWith("@call;") || sexp.startsWith(SemiTypeResolverVisitor.ST_WITH)) && (sexp.length() > 5)) {
            
            if (sexp.charAt(0) == '@') {
                int start = sexp.startsWith("@call;") || sexp.startsWith("@arr;") || sexp.startsWith(SemiTypeResolverVisitor.ST_WITH) ? 1 : sexp.charAt(5) == '@' ? 6 : 5;
                sexp = sexp.substring(start);
            }
            List<String> nExp = new ArrayList<String>();
            String[] split = sexp.split("@");
            for (int i = split.length - 1; i > -1; i--) {
                nExp.add(split[i].substring(split[i].indexOf(';') + 1));
                if (split[i].startsWith("arr;")) {
                    nExp.add("@arr");
                } else if (split[i].startsWith("call;")) {
                    nExp.add("@mtd");
                } else if (split[i].startsWith("with;")) {
                    nExp.add("@with");
                }else {
                    nExp.add("@pro");
                }
            }
            return nExp;
        } else {
            return Collections.singletonList(type.getType());
        }
    }
    
    public static Collection<TypeUsage> resolveTypes(Collection<? extends TypeUsage> unresolved, JsParserResult parserResult, boolean useIndex, boolean includeAllPossible) {
        //assert !SwingUtilities.isEventDispatchThread() : "Type resolution may block AWT due to index search";
        Collection<TypeUsage> types = new ArrayList<TypeUsage>(unresolved);
        if (types.size() == 1 && types.iterator().next().isResolved()) {
            return types;
        }
        Set<String> original = null;
        Model model = parserResult.getModel();
        FileObject fo = parserResult.getSnapshot().getSource().getFileObject();
        JsIndex jsIndex = useIndex ? JsIndex.get(fo) : null;
        int cycle = 0;
        boolean resolvedAll = false;
        while (!resolvedAll && cycle < 10) {
            cycle++;
            resolvedAll = true;
            Collection<TypeUsage> resolved = new ArrayList<TypeUsage>();
            for (TypeUsage typeUsage : types) {
                if (!typeUsage.isResolved()) {
                    if (original == null) {
                        original = new HashSet<String>(unresolved.size());
                        for (TypeUsage t : unresolved) {
                            original.add(t.getType());
                        }
                    }
                    resolvedAll = false;
                    List<String> nExp = expressionFromType(typeUsage);
                    if (nExp.size() > 1) {
                        // passing original prevents the unresolved return types
                        // when recursion in place
                        ModelUtils.addUniqueType(resolved, original, ModelUtils.resolveTypeFromExpression(model, jsIndex, nExp, typeUsage.getOffset(), includeAllPossible));
                    } else {
                        ModelUtils.addUniqueType(resolved, new TypeUsageImpl(typeUsage.getType(), typeUsage.getOffset(), true));
                    }
                } else {
                    ModelUtils.addUniqueType(resolved, (TypeUsage) typeUsage);
                }
            }
            types.clear();
            types = new ArrayList<TypeUsage>(resolved);
        }
        return types;
    }
    
    private static void resolveAssignments(Model model, JsObject jsObject, int offset, List<JsObject> resolvedObjects, List<TypeUsage> resolvedTypes) {
        Collection<? extends TypeUsage> assignments = jsObject.getAssignmentForOffset(offset);
        for (TypeUsage typeName : assignments) {
            if (typeName.isResolved()) {
                resolvedTypes.add(typeName);
                continue;
            }
            String type = typeName.getType();
            if (type.startsWith(SemiTypeResolverVisitor.ST_WITH)) {
                List<String> expression = expressionFromType((TypeUsage)typeName);
                Collection<? extends TypeUsage> typesFromWith = ModelUtils.getTypeFromWith(model, typeName.getOffset());
                expression.remove(expression.size() - 1);
                expression.remove(expression.size() - 1);

                StringBuilder sb = new StringBuilder();
                for (int i = expression.size() - 1; i > 0; i--) {
                    sb.append(expression.get(i--));
                    sb.append(";");
                    sb.append(expression.get(i));
                }
                for (TypeUsage typeWith: typesFromWith) {
                    resolvedTypes.add(new TypeUsageImpl(SemiTypeResolverVisitor.ST_EXP + typeWith.getType() + sb.toString(), typeName.getOffset(), false));
                }
                resolvedTypes.add(new TypeUsageImpl(sb.toString(), typeName.getOffset(), false));
                
            } else {
                JsObject byOffset = findObjectForOffset(typeName.getType(), offset, model);
                if (byOffset != null) {
                    if(!jsObject.getName().equals(byOffset.getName())) {
                        resolvedObjects.add(byOffset);
                        resolveAssignments(model, byOffset, offset, resolvedObjects, resolvedTypes);
                    }
                } else {
                    resolvedTypes.add((TypeUsage)typeName);
                }
            }
        }
    }
    
    private static int deepRA = 0;
    private static void resolveAssignments(Model model, JsIndex jsIndex, String fqn, int offset, List<TypeUsage> resolved) {
        Set<String> alreadyProcessed = new HashSet<String>();
        deepRA = 0;
        for(TypeUsage type : resolved) {
            alreadyProcessed.add(type.getType());
        }
        resolveAssignments(model, jsIndex, fqn, offset, resolved, alreadyProcessed);
    }
    
    private static void resolveAssignments(Model model, JsIndex jsIndex, String fqn, int offset,  List<TypeUsage> resolved, Set<String> alreadyProcessed) {
        if (!alreadyProcessed.contains(fqn)) {
            alreadyProcessed.add(fqn);
            deepRA++;
            String fqnCorrected = fqn;
            if (fqnCorrected.startsWith(SemiTypeResolverVisitor.ST_EXP) && !fqnCorrected.contains(SemiTypeResolverVisitor.ST_CALL)) {
                fqnCorrected = fqnCorrected.substring(fqnCorrected.indexOf(SemiTypeResolverVisitor.ST_EXP) + SemiTypeResolverVisitor.ST_EXP.length());
                fqnCorrected = fqnCorrected.replace(SemiTypeResolverVisitor.ST_PRO, ".");   //NOI18N
            }
            if (!fqnCorrected.startsWith("@")) {
                if (jsIndex != null) {
                    Collection<? extends IndexResult> indexResults = jsIndex.findByFqn(fqnCorrected, JsIndex.FIELD_ASSIGNMENTS);
                    boolean hasAssignments = false;
                    boolean isType = false;
                    for (IndexResult indexResult: indexResults) {
                        Collection<TypeUsage> assignments = IndexedElement.getAssignments(indexResult);
                        if (!assignments.isEmpty()) {
                            hasAssignments = true;
                            for (TypeUsage type : assignments) {
                                if (resolved.size() > 10) {
                                    resolved.clear();
                                    break;
                                }
                                if (!alreadyProcessed.contains(type.getType()) && deepRA < MAX_RECURSION_DEEP_RESOLVING_ASSIGNMENTS) {
                                    resolveAssignments(model, jsIndex, type.getType(), type.getOffset(), resolved, alreadyProcessed);
                                }
                            }
                        }
                    }
                    if (indexResults.isEmpty()) {
                        JsObject found = ModelUtils.findJsObjectByName(model.getGlobalObject(), fqnCorrected);
                        if (found != null) {
                            Collection<? extends TypeUsage> assignments = found.getAssignments();
                            if (!assignments.isEmpty()) {
                                hasAssignments = true;
                                List<TypeUsage> toProcess = new ArrayList<TypeUsage>();
                                for (TypeUsage type : assignments) {
                                    if (!type.isResolved()) {
                                        for (TypeUsage resolvedType : resolveTypeFromSemiType(found, type)) {
                                            toProcess.add(resolvedType);
                                        }
                                    } else {
                                        toProcess.add(type);
                                    }
                                }
                                for (TypeUsage type : toProcess) {
                                    if (!alreadyProcessed.contains(type.getType()) && deepRA < MAX_RECURSION_DEEP_RESOLVING_ASSIGNMENTS) {
                                        resolveAssignments(model, jsIndex, type.getType(), type.getOffset(), resolved, alreadyProcessed);
                                    }
                                }
                            }
                        }
                    }

                    Collection<IndexedElement> properties = jsIndex.getProperties(fqnCorrected);
                    for (IndexedElement property : properties) {
                        if (property.getFQN().startsWith(fqnCorrected) && (property.isDeclared() || ModelUtils.PROTOTYPE.equals(property.getName()))) {
                            isType = true;
                            break;
                        }
                    }


                    if(!hasAssignments || isType) {
                        ModelUtils.addUniqueType(resolved, new TypeUsageImpl(fqnCorrected, offset, true));
                    }
                }
            } else {
                ModelUtils.addUniqueType(resolved, new TypeUsageImpl(fqn, offset, false));
            }
        }
    }
    
    public static JsObject findObjectForOffset(String name, int offset, Model model) {
        for (JsObject object : model.getVariables(offset)) {
            if (object.getName().equals(name)) {
                return object;
            }
        }
        return null;
    }

    public static Collection<String> findPrototypeChain(String fqn, JsIndex jsIndex) {
        Collection<String> chain = findPrototypeChain(fqn, jsIndex, new HashSet<String>());
        return chain;
    }

    private static Collection<String> findPrototypeChain(String fqn, JsIndex jsIndex, Set<String> alreadyCheck) {
        Collection<String> result = new ArrayList<String>();
        if (!alreadyCheck.contains(fqn)) {
            alreadyCheck.add(fqn);
            Collection<IndexedElement> properties = jsIndex.getProperties(fqn);
            for (IndexedElement property : properties) {
                if(ModelUtils.PROTOTYPE.equals(property.getName())) {  //NOI18N
                    Collection<? extends IndexResult> indexResults = jsIndex.findByFqn(property.getFQN(), JsIndex.FIELD_ASSIGNMENTS);
                    for (IndexResult indexResult : indexResults) {
                        Collection<TypeUsage> assignments = IndexedElement.getAssignments(indexResult);
                        for (TypeUsage typeUsage : assignments) {
                            result.add(typeUsage.getType());
                        }
                        for (TypeUsage typeUsage : assignments) {
                            result.addAll(findPrototypeChain(typeUsage.getType(), jsIndex, alreadyCheck));
                        }
                    }
                }
            } 
        } 
        return result;
    }
    
    /**
     * 
     * @param model
     * @param offset
     * @return types from with expressions. The collection has the order of items from most inner with to
     *      the outer with.
     */
    public static Collection <? extends TypeUsage> getTypeFromWith(Model model, int offset) {
        JsObject jsObject = ModelUtils.findJsObject(model, offset);
        JsObject previous = jsObject;
        while (jsObject != null && jsObject.isAnonymous() && jsObject.getJSKind() != JsElement.Kind.WITH_OBJECT) {
            jsObject = ModelUtils.findJsObject(model, jsObject.getOffset() - 1);
            if (jsObject.getFullyQualifiedName().endsWith(previous.getFullyQualifiedName())) {
                break;
            } else {
                previous = jsObject;
            }
        }
        while(jsObject != null && jsObject.getJSKind() != JsElement.Kind.WITH_OBJECT) {
            jsObject = jsObject.getParent();
        }
        if (jsObject != null && jsObject.getJSKind() == JsElement.Kind.WITH_OBJECT) {
            List<TypeUsage> types = new ArrayList<TypeUsage>();
            JsWith wObject = (JsWith)jsObject;
            Collection<? extends TypeUsage> withTypes = wObject.getTypes();
            types.addAll(withTypes);
            while (wObject.getOuterWith() != null) {
                wObject = wObject.getOuterWith();
                withTypes = wObject.getTypes();
                types.addAll(withTypes);
            }
            return types;
        }
        return Collections.EMPTY_LIST;
    }
    
    public static void addUniqueType(Collection <TypeUsage> where, Set<String> forbidden, TypeUsage type) {
        String typeName = type.getType();
        if (forbidden.contains(typeName)) {
            return;
        }
        for (TypeUsage utype : where) {
            if (utype.getType().equals(typeName)) {
                return;
            }
        }
        where.add(type);
    }

    public static void addUniqueType(Collection <TypeUsage> where, TypeUsage type) {
        addUniqueType(where, Collections.<String>emptySet(), type);
    }

    public static void addUniqueType(Collection <TypeUsage> where, Set<String> forbidden, Collection <TypeUsage> what) {
        for (TypeUsage type: what) {
            addUniqueType(where, forbidden, type);
        }
    }

    public static void addUniqueType(Collection <TypeUsage> where, Collection <TypeUsage> what) {
        addUniqueType(where, Collections.<String>emptySet(), what);
    }
    
    
    
    public static void addDocTypesOccurence(JsObject jsObject, JsDocumentationHolder docHolder) {
        if (docHolder.getOccurencesMap().containsKey(jsObject.getFullyQualifiedName())) {
            for (OffsetRange offsetRange : docHolder.getOccurencesMap().get(jsObject.getFullyQualifiedName())) {
                ((JsObjectImpl)jsObject).addOccurrence(offsetRange);
            }
        }
    }
    
    public static String getDisplayName(String typeName) {
        String displayName = typeName;
        if (displayName.startsWith("@param;") || displayName.contains(ModelBuilder.WITH_OBJECT_NAME_START)
                || displayName.contains(ModelBuilder.ANONYMOUS_OBJECT_NAME_START)) {
            displayName = "";
        } else {
            if (displayName.contains(GENERATED_FUNCTION_PREFIX)) {
                displayName = removeGeneratedFromFQN(displayName, GENERATED_FUNCTION_PREFIX);
            }
            if (displayName.contains(GENERATED_ANONYM_PREFIX)) {
                displayName = removeGeneratedFromFQN(displayName, GENERATED_ANONYM_PREFIX);
            }
        }
        return displayName;
    }

    /**
     * 
     * @param fqn fully qualified name of the type
     * @param generated the generated prefix
     * @return the fully qualified name without the generated part or empty string if the generated name is the last one.
     * 
     */
    private static String removeGeneratedFromFQN(String fqn, String generated) {
        String[] parts = fqn.split("\\."); //NOI18N
        String part = parts[parts.length - 1];
        if(part.contains(generated)) {
            try {
                Integer.parseInt(part.substring(generated.length()));
                return ""; // return empty name if the last name is generated
            } catch (NumberFormatException nfe) {
                // do nothing
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            part = parts[i];
            boolean add = true;
            if (part.startsWith(generated) || (i == 0 && part.contains(generated))) {
//            if (part.startsWith(generated)) {
                try {
                    Integer.parseInt(part.substring(part.indexOf(generated) + generated.length()));
//                    Integer.parseInt(part.substring(generated.length()));
                    add = false;
                } catch (NumberFormatException nfe) {
                    // do nothing
                }
            }
            if (add) {
                sb.append(part);
                if (i < (parts.length - 1)) {
                    sb.append(".");
                }
            }
        }
        return sb.toString();
    }
    
    private static List<String> knownGlobalObjects = Arrays.asList("window", "document", "console",
            "clearInterval", "clearTimeout", "event", "frames", "history",
            "Image", "location", "name", "navigator", "Option", "parent", "screen", "setInterval", "setTimeout",
            "XMLHttpRequest", "JSON", "Date", "undefined", "Math",  //NOI18N
            Type.ARRAY, Type.OBJECT, Type.BOOLEAN, Type.NULL, Type.NUMBER, Type.REGEXP, Type.STRING, Type.UNDEFINED, Type.UNRESOLVED);
    
    public static boolean isKnownGLobalType(String type) {
        return knownGlobalObjects.contains(type);
    }
    
    /**
     * 
     * @param snapshot
     * @param offset offset where the expression should be resolved
     * @param lookBefore if yes, looks for the beginning of the expression before the offset,
     *                  if no, it can be in a middle of expression
     * @return 
     */
    public static List<String> resolveExpressionChain(Snapshot snapshot, int offset, boolean lookBefore) {
        TokenHierarchy<?> th = snapshot.getTokenHierarchy();
        TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(th, offset);
        if (ts == null) {
            return Collections.<String>emptyList();
        }

        ts.move(offset);
        if (ts.movePrevious() && (ts.moveNext() || ((ts.offset() + ts.token().length()) == snapshot.getText().length()))) {
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

            while (token.id() != JsTokenId.OPERATOR_SEMICOLON
                    && token.id() != JsTokenId.BRACKET_RIGHT_CURLY && token.id() != JsTokenId.BRACKET_LEFT_CURLY
                    && token.id() != JsTokenId.BRACKET_LEFT_PAREN
                    && token.id() != JsTokenId.BLOCK_COMMENT
                    && token.id() != JsTokenId.LINE_COMMENT
                    && token.id() != JsTokenId.OPERATOR_ASSIGNMENT
                    && token.id() != JsTokenId.OPERATOR_PLUS) {
                
                if (token.id() == JsTokenId.WHITESPACE) {
                    // we need to find out, whether this is a continual expression on the new line
                    int helpOffset = ts.offset();
                    if (ts.movePrevious()) {
                        token = LexUtilities.findPrevious(ts, Arrays.asList(JsTokenId.WHITESPACE, JsTokenId.BLOCK_COMMENT, JsTokenId.LINE_COMMENT, JsTokenId.EOL));
                        if (token.id() != JsTokenId.BRACKET_RIGHT_PAREN && token.id() != JsTokenId.IDENTIFIER
                                && token.id() != JsTokenId.OPERATOR_DOT) {
                            ts.move(helpOffset);
                            ts.moveNext();
                            token = ts.token();
                            break;
                        }
                    }
                }
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
                        exp.addAll(resolveExpressionChain(snapshot, offsetFirstRightParen - 1, true));
                    }
                }
            } else if (exp.isEmpty() && !lookBefore && offsetFirstRightParen > -1) {
                // in the case when the expression is like ( new Object()).someMethod
                exp.addAll(resolveExpressionChain(snapshot, offsetFirstRightParen - 1, true));
            } else if (wasLastDot && !lookBefore && token.id() == JsTokenId.BRACKET_RIGHT_CURLY) {
                int balancer = 1;
                while (balancer > 0 && ts.movePrevious()) {
                    token = ts.token();
                    if (token.id() == JsTokenId.BRACKET_RIGHT_CURLY) {
                        balancer++;
                    } else {
                        if (token.id() == JsTokenId.BRACKET_LEFT_CURLY) {
                            balancer--;
                        }
                    }
                }
                exp.add("@ano:" + ts.offset());
                exp.add("@pro");
            }
            return exp;
        }
        return Collections.<String>emptyList();
    }
    
    public static void moveProperty (JsObject newParent, JsObject property) {
        JsObject newProperty = newParent.getProperty(property.getName());
        if (property.getParent() != null) {
            property.getParent().getProperties().remove(property.getName());
        }
        if (newProperty == null) {
            ((JsObjectImpl)property).setParent(newParent);
            newParent.addProperty(property.getName(), property);
        } else {
            if (property.isDeclared() && !newProperty.isDeclared()) {
                JsObject tmpProperty = newProperty;
                newParent.addProperty(property.getName(), property);
                ((JsObjectImpl)property).setParent(newParent);
                newProperty = property;
                property = tmpProperty;
            }
            JsObjectImpl.moveOccurrenceOfProperties((JsObjectImpl) newProperty, property);
            for (Occurrence occurrence : property.getOccurrences()) {
                newProperty.addOccurrence(occurrence.getOffsetRange());
            }
            List<JsObject>propertiesToMove = new ArrayList(property.getProperties().values());
            for (JsObject propOfProperty: propertiesToMove) {
                moveProperty(newProperty, propOfProperty);
            }
        }
    }
    
    public static void changeDeclarationScope(JsObject where, DeclarationScope newScope) {
        changeDeclarationScope(where, newScope, new HashSet<String>());
    }
    
    private static void changeDeclarationScope(JsObject where, DeclarationScope newScope, Set<String> done) {
        if (!done.contains(where.getFullyQualifiedName())) {
            done.add(where.getFullyQualifiedName());
            if (where instanceof DeclarationScope) {
                if (where.isDeclared()) {
                    DeclarationScope scope = (DeclarationScope)where;
                    DeclarationScope oldScope = scope.getParentScope();
                    if (oldScope != null) {
                        oldScope.getChildrenScopes().remove(scope);
                        if (scope instanceof DeclarationScopeImpl) {
                            ((DeclarationScopeImpl)scope).setParentScope(newScope);
                        }  
                    }
                    newScope.addDeclaredScope(scope);
                }
            } else {
                for (JsObject property : where.getProperties().values()) {
                    changeDeclarationScope(property, newScope, done);
                }
            }
        }
    }
    
    /**
     * This method is useful, when you need to go through the model and be sure that
     * the algorithm will not run into endless cycle. In the model there can be references
     * to an object that can caused endless cycle. This method check whether the object has an reference or
     * the original of the reference object was already processed. Also adds the fully qualified names of the object
     * and the references to the list of processed objects. 
     * @param object object that should be processed
     * @param processedObjects list of already processed object
     * @return true if the object full qualified name or his reference full qualified name is in the processedObjects list.
     */
    public static boolean wasProcessed (JsObject object, List<String> processedObjects) {
        if (processedObjects.contains(object.getFullyQualifiedName())) {
            return true;
        } else if (object instanceof JsReference) {
            JsObject original = ((JsReference) object).getOriginal();
            boolean isOrginalReachable = !original.isAnonymous() && !original.getName().equals(object.getName());
            JsObject origParent = original.getParent();
            while (origParent != null && isOrginalReachable) {
                if (origParent.isAnonymous() && !(origParent.getParent() != null && origParent.getParent().getParent() == null)) {
                    isOrginalReachable = false;
                }
                origParent = origParent.getParent();
            }
            if (isOrginalReachable) {
                processedObjects.add(object.getFullyQualifiedName());
                return true;
            }
            if (processedObjects.contains(original.getFullyQualifiedName())) {
                return true;
            } else {
                processedObjects.add(object.getFullyQualifiedName());
                processedObjects.add(original.getFullyQualifiedName());
            }
        } else {
            if (object.getJSKind() != JsElement.Kind.FILE) {
                processedObjects.add(object.getFullyQualifiedName());
            }
        }
        return false;
    }
}
