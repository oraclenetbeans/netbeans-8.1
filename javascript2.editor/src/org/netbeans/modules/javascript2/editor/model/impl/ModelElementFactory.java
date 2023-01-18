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

import jdk.nashorn.internal.ir.FunctionNode;
import jdk.nashorn.internal.ir.IdentNode;
import jdk.nashorn.internal.ir.LiteralNode;
import jdk.nashorn.internal.ir.ObjectNode;
import jdk.nashorn.internal.ir.PropertyNode;
import jdk.nashorn.internal.parser.Token;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.doc.spi.JsDocumentationHolder;
import org.netbeans.modules.javascript2.editor.doc.spi.JsModifier;
import static org.netbeans.modules.javascript2.editor.doc.spi.JsModifier.PRIVATE;
import org.netbeans.modules.javascript2.editor.embedding.JsEmbeddingProvider;
import org.netbeans.modules.javascript2.editor.model.*;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;

/**
 *
 * @author Petr Pisl
 */
class ModelElementFactory {

    @CheckForNull
    static JsFunctionImpl create(JsParserResult parserResult, FunctionNode functionNode, List<Identifier> fqName, ModelBuilder modelBuilder, boolean isAnnonymous, JsObject parent) {
        if (JsEmbeddingProvider.containsGeneratedIdentifier(fqName.get(fqName.size() - 1).getName())) {
            return null;
        }
        JsObjectImpl inObject = modelBuilder.getCurrentObject();
        JsObject globalObject = modelBuilder.getGlobal();
        JsObject parentObject = parent;
        if (parent == null) {
            if (isAnnonymous) {
                DeclarationScopeImpl decScope = modelBuilder.getCurrentDeclarationScope();
//                while (decScope != null && decScope.isAnonymous()) {
//                    decScope = (DeclarationScopeImpl)decScope.getParentScope();
//                }
                parentObject = decScope == null ? globalObject : decScope;
            } else {
                parentObject = inObject;
            }
            while(parentObject.getParent() != null && parentObject.getModifiers().contains(Modifier.PROTECTED)) {
                parentObject = parentObject.getParent();
            }
        }
        int start = Token.descPosition(functionNode.getFirstToken());
        int end = Token.descPosition(functionNode.getLastToken()) + Token.descLength(functionNode.getLastToken());
        List<Identifier> parameters = new ArrayList(functionNode.getParameters().size());
        for(IdentNode node: functionNode.getParameters()) {
            IdentifierImpl param = create(parserResult, node);
            if (param != null) {
                // can be null, if it's a generated embeding. 
                parameters.add(param);
            }
        }
        JsFunctionImpl result; 
        if (fqName.size() > 1) {
            List<Identifier> objectName = fqName.subList(0, fqName.size() - 1);
            parentObject = isAnnonymous ? globalObject : ModelUtils.getJsObject(modelBuilder, objectName, false);
            result = new JsFunctionImpl(modelBuilder.getCurrentDeclarationFunction(), 
                    parentObject, fqName.get(fqName.size() - 1), parameters,
                    new OffsetRange(start, end), parserResult.getSnapshot().getMimeType(), null);
            if (parentObject instanceof JsFunction && !ModelUtils.PROTOTYPE.equals(parentObject.getName())) {
                result.addModifier(Modifier.STATIC);
            } 
        } else {
            result = new JsFunctionImpl(modelBuilder.getCurrentDeclarationFunction(),
                    parentObject, fqName.get(fqName.size() - 1), parameters,
                    new OffsetRange(start, end), parserResult.getSnapshot().getMimeType(), null);
        }
        String propertyName = result.getDeclarationName().getName();
        if (parentObject == null) {
            parentObject = globalObject;
        }
        JsObject property = parentObject.getProperty(propertyName); // the already existing property
        
        parentObject.addProperty(result.getDeclarationName().getName(), result);
        if (property != null) {
            if (property.getDeclarationName() != null) {
                result.addOccurrence(property.getDeclarationName().getOffsetRange());
            }
            for(Occurrence occurrence : property.getOccurrences()) {
                result.addOccurrence(occurrence.getOffsetRange());
            }
        }
        JsDocumentationHolder docHolder = parserResult.getDocumentationHolder();
        if (docHolder != null) {
            result.setDocumentation(docHolder.getDocumentation(functionNode));
        }
        result.setAnonymous(isAnnonymous);
        return result;
    }

    @NonNull
    static JsFunctionImpl createVirtualFunction(JsParserResult parserResult, JsObject parentObject, Identifier name, int paramCount) {
        List<Identifier> params = new ArrayList<Identifier>(paramCount);
        if (paramCount == 1) {
            params.add(new IdentifierImpl("param", OffsetRange.NONE));
        } else {
            for(int i = 0; i < paramCount; i++) {
                params.add(new IdentifierImpl("param" + (i + 1), OffsetRange.NONE));
            }
        }
        JsFunctionImpl virtual = new JsFunctionImpl(parserResult.getSnapshot().getSource().getFileObject(),
                parentObject, name, params, parserResult.getSnapshot().getMimeType(), null);
        if (virtual.hasExactName()) {
            virtual.addOccurrence(name.getOffsetRange());
        }
        return virtual;
    }

    @CheckForNull
    static IdentifierImpl create(JsParserResult parserResult, IdentNode node) {
        return create(parserResult, node.getName(), node.getStart(), node.getFinish());
    }

    @CheckForNull
    static IdentifierImpl create(JsParserResult parserResult, LiteralNode node) {
        return create(parserResult, node.getString(), node.getStart(), node.getFinish());
    }

    @CheckForNull
    static IdentifierImpl create(JsParserResult parserResult, String name, int start, int end) {
        if (JsEmbeddingProvider.containsGeneratedIdentifier(name)) {
            return null;
        }
        return new IdentifierImpl(name, new OffsetRange(start, end));
    }

    @CheckForNull
    static JsObjectImpl create(JsParserResult parserResult, ObjectNode objectNode, List<Identifier> fqName, ModelBuilder modelBuilder, boolean belongsToParent) {
        if (JsEmbeddingProvider.containsGeneratedIdentifier(fqName.get(fqName.size() - 1).getName())) {
            return null;
        }
        JsObjectImpl scope = modelBuilder.getCurrentObject();
        JsObject parent = scope;
        JsObject result = null;
        Identifier name = fqName.get(fqName.size() - 1);
        JsObjectImpl newObject;
        if (!belongsToParent) {
            List<Identifier> objectName = fqName.size() > 1 ? fqName.subList(0, fqName.size() - 1) : fqName;
            parent = ModelUtils.getJsObject(modelBuilder, objectName, false);
            if (parent != null) {
                parent = parent.getParent();
            }
            if (parent == null) {
                parent = modelBuilder.getGlobal();
            }
        }
        result = parent.getProperty(name.getName());
        newObject = new JsObjectImpl(parent, name, new OffsetRange(objectNode.getStart(), objectNode.getFinish()),
                parserResult.getSnapshot().getMimeType(), null);
        newObject.setDeclared(true);
        if (result != null) {
            // the object already exist due a definition of a property => needs to be copied
            for (String propertyName : result.getProperties().keySet()) {
                newObject.addProperty(propertyName, result.getProperty(propertyName));
            }
        }
        JsDocumentationHolder docHolder = parserResult.getDocumentationHolder();
        if (docHolder != null) {
            newObject.setDeprecated(docHolder.isDeprecated(objectNode));
            newObject.setDocumentation(docHolder.getDocumentation(objectNode));
        }
        parent.addProperty(name.getName(), newObject);
        if (newObject.hasExactName()) {
            newObject.addOccurrence(newObject.getDeclarationName().getOffsetRange());
        }
        return (JsObjectImpl)newObject;
    }
    
    @CheckForNull
    static JsArrayImpl create(JsParserResult parserResult, LiteralNode.ArrayLiteralNode aNode, List<Identifier> fqName, ModelBuilder modelBuilder, boolean belongsToParent, JsObject suggestedParent) {
        if (JsEmbeddingProvider.containsGeneratedIdentifier(fqName.get(fqName.size() - 1).getName())) {
            return null;
        }
        JsObject parent = suggestedParent != null ? suggestedParent : modelBuilder.getCurrentObject();
        JsObject result = null;
        Identifier name = fqName.get(fqName.size() - 1);
        JsArrayImpl newObject;
        if (!belongsToParent) {
            List<Identifier> objectName = fqName.size() > 1 ? fqName.subList(0, fqName.size() - 1) : fqName;
            parent = ModelUtils.getJsObject(modelBuilder, objectName, false);
        }
        result = parent.getProperty(name.getName());
        newObject = new JsArrayImpl(parent, name, new OffsetRange(aNode.getStart(), aNode.getFinish()), 
                parserResult.getSnapshot().getMimeType(), null);
        newObject.setDeclared(true);
        if (result != null) {
            // the object already exist due a definition of a property => needs to be copied
            for (String propertyName : result.getProperties().keySet()) {
                newObject.addProperty(propertyName, result.getProperty(propertyName));
            }
            for (Occurrence occurence: result.getOccurrences()) {
                newObject.addOccurrence(occurence.getOffsetRange());
            }
            if (result.isDeclared()) {
                newObject.getModifiers().clear();
                newObject.getModifiers().addAll(result.getModifiers());
                newObject.setDeclarationName(result.getDeclarationName());
            }
        }
        JsDocumentationHolder docHolder = parserResult.getDocumentationHolder();
        if (docHolder != null) {
            newObject.setDeprecated(docHolder.isDeprecated(aNode));
            newObject.setDocumentation(docHolder.getDocumentation(aNode));
        }
        parent.addProperty(name.getName(), newObject);
        if (newObject.hasExactName() && newObject.getDeclarationName() != null) {
            newObject.addOccurrence(newObject.getDeclarationName().getOffsetRange());
        }
        return newObject;
    }

    @NonNull
    static JsObjectImpl createAnonymousObject(JsParserResult parserResult, ObjectNode objectNode, ModelBuilder modelBuilder) {
        String name = modelBuilder.getUnigueNameForAnonymObject(parserResult);
        JsObjectImpl result = new AnonymousObject(modelBuilder.getCurrentDeclarationFunction(),
                    name, new OffsetRange(objectNode.getStart(), objectNode.getFinish()), parserResult.getSnapshot().getMimeType(), null);
        modelBuilder.getCurrentDeclarationFunction().addProperty(name, result);
        JsDocumentationHolder docHolder = parserResult.getDocumentationHolder();
        if (docHolder != null) {
            result.setDocumentation(docHolder.getDocumentation(objectNode));
            result.setDeprecated(docHolder.isDeprecated(objectNode));
        }
        return result;
    }
    
    @NonNull
    static JsArrayImpl createAnonymousObject(JsParserResult parserResult, LiteralNode.ArrayLiteralNode aNode, ModelBuilder modelBuilder) {
        String name = modelBuilder.getUnigueNameForAnonymObject(parserResult);
        JsArrayImpl result = new AnonymousObject.AnonymousArray(modelBuilder.getCurrentDeclarationFunction(),
                    name, new OffsetRange(aNode.getStart(), aNode.getFinish()), parserResult.getSnapshot().getMimeType(), null);
        modelBuilder.getCurrentDeclarationFunction().addProperty(name, result);
        JsDocumentationHolder docHolder = parserResult.getDocumentationHolder();
        if (docHolder != null) {
            result.setDocumentation(docHolder.getDocumentation(aNode));
            result.setDeprecated(docHolder.isDeprecated(aNode));
        }
        return result;
    }

    @CheckForNull
    static JsObjectImpl create(JsParserResult parserResult, PropertyNode propertyNode, Identifier name, ModelBuilder modelBuilder, boolean belongsToParent) {
        if (JsEmbeddingProvider.containsGeneratedIdentifier(name.getName())) {
            return null;
        }
        JsObjectImpl scope = modelBuilder.getCurrentObject();
        JsObjectImpl property = new JsObjectImpl(scope, name, name.getOffsetRange(), parserResult.getSnapshot().getMimeType(), null);
        JsDocumentationHolder docHolder = parserResult.getDocumentationHolder();
        property.setDocumentation(docHolder.getDocumentation(propertyNode));
        property.setDeprecated(docHolder.isDeprecated(propertyNode));
        if (property.hasExactName()) {
            property.addOccurrence(property.getDeclarationName().getOffsetRange());
        }
        return property;
    }
}
