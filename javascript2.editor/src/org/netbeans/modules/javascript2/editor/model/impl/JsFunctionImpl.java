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

import java.util.*;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.api.FrameworksUtils;
import org.netbeans.modules.javascript2.editor.doc.spi.JsDocumentationHolder;
import org.netbeans.modules.javascript2.editor.model.*;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Petr Pisl
 */
public class JsFunctionImpl extends DeclarationScopeImpl implements JsFunction {

    final private HashMap <String, JsObject> parametersByName;
    final private List<JsObject> parameters;
    final private Set<TypeUsage> returnTypes;

    public JsFunctionImpl(DeclarationScope scope, JsObject parentObject, Identifier name,
            List<Identifier> parameters, OffsetRange offsetRange, String mimeType, String sourceLabel) {
        super(scope, parentObject, name, offsetRange, mimeType, sourceLabel);
        this.parametersByName = new HashMap<String, JsObject>(parameters.size());
        this.parameters = new ArrayList<JsObject>(parameters.size());
        for (Identifier identifier : parameters) {
            JsObject parameter = new ParameterObject(this, identifier, mimeType, sourceLabel);
            addParameter(parameter);
        }
        setAnonymous(false);
        this.returnTypes = new HashSet<TypeUsage>();
        setDeclared(true);
        if (parentObject != null) {
            // creating arguments variable
            JsObjectImpl arguments = new JsObjectImpl(this, 
                    new IdentifierImpl(ModelUtils.ARGUMENTS, new OffsetRange(name.getOffsetRange().getStart(), name.getOffsetRange().getStart())), 
                    name.getOffsetRange(),  false, EnumSet.of(Modifier.PRIVATE), mimeType, sourceLabel);
            arguments.addAssignment(new TypeUsageImpl("Arguments", getOffset(), true), getOffset());    // NOI18N
            this.addProperty(arguments.getName(), arguments);
        }
    }

    protected JsFunctionImpl(FileObject file, JsObject parentObject, Identifier name,
            List<Identifier> parameters, String mimeType, String sourceLabel) {
        this(null, parentObject, name, parameters, name.getOffsetRange(), mimeType, sourceLabel);
        this.setFileObject(file);
        this.setDeclared(false);
    }

    private JsFunctionImpl(FileObject file, Identifier name, String mimeType, String sourceLabel) {
        this(null, null, name, Collections.EMPTY_LIST, name.getOffsetRange(), mimeType, sourceLabel);
        this.setFileObject(file);
    }
    
    public static JsFunctionImpl createGlobal(FileObject fileObject, int length, String mimeType) {
        String name = fileObject != null ? fileObject.getName() : "VirtualSource"; //NOI18N
        Identifier ident = new IdentifierImpl(name, new OffsetRange(0, length));
        return new JsFunctionImpl(fileObject, ident, mimeType, null);
    }
    
    @Override
    public final Collection<? extends JsObject> getParameters() {
        return this.parameters;
    }

    public final void addParameter(JsObject object) {
        assert object.getParent() == this;
        this.parametersByName.put(object.getName(), object);
        this.parameters.add(object);
    }

    @Override
    public Kind getJSKind() {
        if (kind != null) {
            return kind;
        }
        if (getParent() == null) {
            // global function
            return JsElement.Kind.FILE;
        }
        if (getName().startsWith("get ")) { //NOI18N
            return JsElement.Kind.PROPERTY_GETTER;
        }
        if (getName().startsWith("set ")) { //NOI18N
            return JsElement.Kind.PROPERTY_SETTER;
        }
        if (getParent() != null /*&& getParent() instanceof JsFunction*/) {
             JsObject prototype = null;
            for (JsObject property : getProperties().values()) {
                if (property.isDeclared() 
                        && (property.getModifiers().contains(Modifier.PROTECTED)
                        || (property.getModifiers().contains(Modifier.PUBLIC) &&  !property.getModifiers().contains(Modifier.STATIC)))
                        && !isAnonymous() && !property.isAnonymous()
                        && (property.getDeclarationName() != null && property.getDeclarationName().getOffsetRange().getStart() < property.getDeclarationName().getOffsetRange().getEnd())) {
                    if(!ModelUtils.PROTOTYPE.equals(getParent().getName())) {
                        return JsElement.Kind.CONSTRUCTOR;
                    }
                }
                if (ModelUtils.PROTOTYPE.equals(property.getName())) {
                    prototype = property;
                }
            }
            if (prototype != null /*&& !prototype.getProperties().isEmpty()*/) {
                return JsElement.Kind.CONSTRUCTOR;
            }
        }
//        if (getParent() != null && !getParent().isDeclared()) {
//            
//        }

        JsElement.Kind result = JsElement.Kind.FUNCTION;

        if (getParent().getJSKind() != JsElement.Kind.FILE) {
            result = JsElement.Kind.METHOD;
        }
        return result;
    }

    @Override
    public JsObject getParameter(String name) {
        JsObject result = parametersByName.get(name);
        return result;
    }

    private boolean areReturnTypesResolved = false;
    
    @Override
    public Collection<? extends TypeUsage> getReturnTypes() {
        if (areReturnTypesResolved) {
            return Collections.EMPTY_LIST;
        }
        Collection<TypeUsage> returns = new HashSet();
        HashSet<String> nameReturnTypes = new HashSet<String>();
        areReturnTypesResolved = true;
        for(TypeUsage type : returnTypes) {
             if (type.isResolved()) {
                 if (!nameReturnTypes.contains(type.getType())){
                    returns.add(type);
                    nameReturnTypes.add(type.getType());
                 }
            } else {
                 if (type.getType().startsWith("@")) {
                     String typeName = type.getType();
                     if (!(typeName.endsWith(getName()) && typeName.startsWith("@call"))) {
                        Collection<TypeUsage> resolved = ModelUtils.resolveTypeFromSemiType(this, type);
                        for (TypeUsage typeResolved : resolved) {
                            if (!nameReturnTypes.contains(type.getType())) {
                                returns.add(typeResolved);
                                nameReturnTypes.add(typeResolved.getType());
                            }
                        }
                     }
                 } else {
                    JsObject jsObject = ModelUtils.getJsObjectByName(this,type.getType());
                    if (jsObject == null) {
                        // try to find according the fqn
                        JsObject global = ModelUtils.getGlobalObject(this);
                        jsObject = ModelUtils.findJsObjectByName(global, type.getType());
                    }
                    if(jsObject != null) {
                       Collection<TypeUsage> resolveAssignments = resolveAssignments(jsObject, type.getOffset());
                       for (TypeUsage typeResolved: resolveAssignments) {
                           if (!nameReturnTypes.contains(typeResolved.getType())){
                              returns.add(typeResolved);
                              nameReturnTypes.add(typeResolved.getType());
                           }
                       }
                    } else {
                        returns.add(type);
                        nameReturnTypes.add(type.getType());
                    }
                 }
            }
        }
        areReturnTypesResolved = false;
        return returns;
    }    
        
    @Override
    public void addReturnType(TypeUsage type) {
        boolean isThere = false;
        for (TypeUsage typeUsage : this.returnTypes) {
            if (type.getType().equals(typeUsage.getType())) {
                isThere = true;
            }
        }
        if (!isThere){
            this.returnTypes.add(type);
        }
    }
    
    public void addReturnType(Collection<TypeUsage> types) {
        for (TypeUsage typeUsage : types) {
            addReturnType(typeUsage);
        }
    }
    
    public boolean areReturnTypesEmpty() {
        return returnTypes.isEmpty();
    }

    @Override
    public boolean moveProperty(String name, JsObject newParent) {
        JsObject property = getProperty(name);
        if (property != null && (newParent instanceof DeclarationScope)) {
            FrameworksUtils.changeDeclarationScope(property, (DeclarationScope)newParent);
        }
        return super.moveProperty(name, newParent); 
    }

    @Override
    public void resolveTypes(JsDocumentationHolder docHolder) {
        super.resolveTypes(docHolder);
        HashSet<String> nameReturnTypes = new HashSet<String>();
        Collection<TypeUsage> resolved = new ArrayList();
        for (TypeUsage type : returnTypes) {
            if (!(type.getType().equals(Type.UNRESOLVED) && returnTypes.size() > 1)) {
                if (!type.isResolved()) {
                    for (TypeUsage rType : ModelUtils.resolveTypeFromSemiType(this, type)) {
                        if (!nameReturnTypes.contains(rType.getType())) {
                            if ("@this;".equals(type.getType())) { // NOI18N
                                rType = new TypeUsageImpl(rType.getType(), -1, rType.isResolved());
                            }
                            resolved.add(rType);
                            nameReturnTypes.add(rType.getType());
                        }
                    }
//                    resolved.addAll(ModelUtils.resolveTypeFromSemiType(this, type));
                } else {
                    if (!nameReturnTypes.contains(type.getType())) {
                        resolved.add(type);
                        nameReturnTypes.add(type.getType());
                    }
                }
            }
        }
        
        for (TypeUsage type : resolved) {
            if (type.getOffset() > 0) {
                String typeName = type.getType();
                JsObject jsObject = null;
                // at first check whether is not a parameter
                if (typeName.indexOf('.') == -1) {
                    JsObject parameter = null;
                    JsFunction scope = this;
                    while (scope != null && parameter == null && jsObject == null) {
                        parameter = scope.getParameter(typeName);
                        jsObject = scope.getProperty(typeName);
                        scope = (JsFunction)((DeclarationScope)scope).getParentScope();
                    }
                    if (jsObject == null && parameter != null) {
                        jsObject = parameter;
                    }
                    if (jsObject != null) {
                        jsObject.addOccurrence(new OffsetRange(type.getOffset(), type.getOffset() + typeName.length()));
                    }
                }
                if (jsObject == null) {
                    jsObject = ModelUtils.findJsObjectByName(this, typeName);
                    if (jsObject == null) {
                        JsObject global = ModelUtils.getGlobalObject(this);
                        jsObject = ModelUtils.findJsObjectByName(global, typeName);
                    }
                    if (jsObject != null && containsOffset(type.getOffset()) && !getJSKind().equals(JsElement.Kind.FILE)) {
                        int index = typeName.lastIndexOf('.');
                        int typeLength = (index > -1) ? typeName.length() - index - 1 : typeName.length();
                        ((JsObjectImpl)jsObject).addOccurrence(new OffsetRange(type.getOffset(), jsObject.isAnonymous() ? type.getOffset() : type.getOffset() + typeLength));
                    }
                }
            }
        }
        returnTypes.clear();
        returnTypes.addAll(resolved);
         
        // parameters and type type resolving for occurrences
        JsObject global = ModelUtils.getGlobalObject(this);
        for(JsObject param : parameters) {
            Collection<? extends TypeUsage> types = param.getAssignmentForOffset(param.getDeclarationName().getOffsetRange().getStart());
            for(TypeUsage type: types) {
                JsObject jsObject = ModelUtils.findJsObjectByName(global, type.getType());//getJsObjectByName(this, type.getType());
                if (jsObject != null) {
                    ModelUtils.addDocTypesOccurence(jsObject, docHolder);
                    moveOccurrenceOfProperties((JsObjectImpl)jsObject, param);
                    if (type.getType().indexOf('.') > -1) {
                        // mark occurrences also for the parent if the type is like Contex.Object
                        String[] typeParts = type.getType().split("\\.");
                        JsObject parent = jsObject.getParent();
                        for (int i = (typeParts.length - 2); i > -1 && parent != null; i--) {
                            if (parent.getName().equals(typeParts[i])) {
                                ModelUtils.addDocTypesOccurence(parent, docHolder);
                            }
                            parent = parent.getParent();
                        }
                    }
                }
            }
            List<JsObject> paramProperties = new ArrayList(param.getProperties().values());
            for(JsObject paramProperty: paramProperties) {
               ((JsObjectImpl)paramProperty).resolveTypes(docHolder);
            }
        }
    }

//    @Override
//    public String toString() {
//        return "JsFunctionImpl{" + "declarationName=" + getDeclarationName() + ", parent=" + getParent() + ", kind=" + kind + ", parameters=" + parameters + ", returnTypes=" + returnTypes + '}';
//    }

    @Override
    protected void correctTypes(String fromType, String toType) {
        super.correctTypes(fromType, toType);
        String typeR;
        String typeFQN;
        Set<TypeUsage> copy = new HashSet<TypeUsage>(returnTypes);
        for (TypeUsage type : copy) {
            typeFQN = type.getType();
            typeR = replaceTypeInFQN(typeFQN, fromType, toType);
            if (typeR != null) {
                returnTypes.remove(type);
                returnTypes.add(new TypeUsageImpl(typeR, type.getOffset(), type.isResolved()));
            }
        }
    }
    
}
