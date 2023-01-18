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
package org.netbeans.modules.php.editor.csl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.HtmlFormatter;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.StructureItem;
import org.netbeans.modules.php.editor.api.AliasedName;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.api.elements.ElementFilter;
import org.netbeans.modules.php.editor.api.elements.ParameterElement;
import org.netbeans.modules.php.editor.api.elements.TypeElement;
import org.netbeans.modules.php.editor.api.elements.TypeResolver;
import org.netbeans.modules.php.editor.model.ClassConstantElement;
import org.netbeans.modules.php.editor.model.ClassScope;
import org.netbeans.modules.php.editor.model.ConstantElement;
import org.netbeans.modules.php.editor.model.FieldElement;
import org.netbeans.modules.php.editor.model.FileScope;
import org.netbeans.modules.php.editor.model.FunctionScope;
import org.netbeans.modules.php.editor.model.InterfaceScope;
import org.netbeans.modules.php.editor.model.MethodScope;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.NamespaceScope;
import org.netbeans.modules.php.editor.model.Scope;
import org.netbeans.modules.php.editor.model.TraitScope;
import org.netbeans.modules.php.editor.model.TypeScope;
import org.netbeans.modules.php.editor.model.UseScope;
import org.netbeans.modules.php.editor.model.impl.VariousUtils;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public final class NavigatorScanner {
    private static final Logger LOGGER = Logger.getLogger(NavigatorScanner.class.getName());
    private static final String FONT_GRAY_COLOR = "<font color=\"#999999\">"; //NOI18N
    private static final String CLOSE_FONT = "</font>"; //NOI18N
    private static ImageIcon interfaceIcon = null;
    private static ImageIcon traitIcon = null;
    private static boolean isLogged = false;
    private final FileScope fileScope;
    private final Set<TypeElement> deprecatedTypes;

    public static NavigatorScanner create(Model model, boolean resolveDeprecatedElements) {
        return new NavigatorScanner(model, resolveDeprecatedElements);
    }

    private NavigatorScanner(Model model, boolean resolveDeprecatedElements) {
        fileScope = model.getFileScope();
        if (resolveDeprecatedElements) {
            if (!isLogged) {
                LOGGER.info("Resolving of deprecated elements in Navigator scanner - IDE will be possibly slow!");
                isLogged = true;
            }
            deprecatedTypes = ElementFilter.forDeprecated(true).filter(model.getIndexScope().getIndex().getTypes(NameKind.empty()));
        } else {
            deprecatedTypes = Collections.<TypeElement>emptySet();
        }
    }

    public List<? extends StructureItem> scan() {
        final List<StructureItem> items = new ArrayList<>();
        processNamespaces(items, fileScope.getDeclaredNamespaces());
        return items;
    }

    private void processNamespaces(List<StructureItem> items, Collection<? extends NamespaceScope> declaredNamespaces) {
        for (NamespaceScope nameScope : declaredNamespaces) {
            List<StructureItem> namespaceChildren = nameScope.isDefaultNamespace() ? items : new ArrayList<StructureItem>();
            if (!nameScope.isDefaultNamespace()) {
                items.add(new PHPNamespaceStructureItem(nameScope, namespaceChildren));
            }
            Collection<? extends UseScope> declaredUses = nameScope.getDeclaredUses();
            for (UseScope useElement : declaredUses) {
                namespaceChildren.add(new PHPUseStructureItem(useElement));
            }

            Collection<? extends FunctionScope> declaredFunctions = nameScope.getDeclaredFunctions();
            for (FunctionScope fnc : declaredFunctions) {
                if (fnc.isAnonymous()) {
                    continue;
                }
                List<StructureItem> variables = new ArrayList<>();
                namespaceChildren.add(new PHPFunctionStructureItem(fnc, variables));
            }
            Collection<? extends ConstantElement> declaredConstants = nameScope.getDeclaredConstants();
            for (ConstantElement constant : declaredConstants) {
                namespaceChildren.add(new PHPConstantStructureItem(constant, "const"));
            }
            processTypes(items, namespaceChildren, nameScope.getDeclaredTypes());
        }
    }

    private void processTypes(List<StructureItem> items, List<StructureItem> namespaceChildren, Collection<? extends TypeScope> declaredTypes) {
        for (TypeScope type : declaredTypes) {
            List<StructureItem> children = new ArrayList<>();
            if (type instanceof ClassScope) {
                namespaceChildren.add(new PHPClassStructureItem((ClassScope) type, children));
            } else if (type instanceof InterfaceScope) {
                namespaceChildren.add(new PHPInterfaceStructureItem((InterfaceScope) type, children));
            } else if (type instanceof TraitScope) {
                namespaceChildren.add(new PHPTraitStructureItem((TraitScope) type, children));
            }
            Collection<? extends MethodScope> declaredMethods = type.getDeclaredMethods();
            for (MethodScope method : declaredMethods) {
                // The method name doesn't have to be always defined during parsing.
                // For example when user writes in  a php doc @method and parsing is
                // started when there is no name yet.
                if (method.getName() != null && !method.getName().isEmpty()) {
                    List<StructureItem> variables = new ArrayList<>();
                    if (method.isConstructor()) {
                        children.add(new PHPConstructorStructureItem(method, variables));
                    } else {
                        children.add(new PHPMethodStructureItem(method, variables));
                    }
                }
            }
            Collection<? extends ClassConstantElement> declaredClsConstants = type.getDeclaredConstants();
            for (ClassConstantElement classConstant : declaredClsConstants) {
                children.add(new PHPConstantStructureItem(classConstant, "con")); //NOI18N
            }
            if (type instanceof ClassScope) {
                ClassScope cls = (ClassScope) type;
                Collection<? extends FieldElement> declaredFields = cls.getDeclaredFields();
                for (FieldElement field : declaredFields) {
                    children.add(new PHPFieldStructureItem(field));
                }
            }
            if (type instanceof TraitScope) {
                TraitScope trait = (TraitScope) type;
                Collection<? extends FieldElement> declaredFields = trait.getDeclaredFields();
                for (FieldElement field : declaredFields) {
                    children.add(new PHPFieldStructureItem(field));
                }
            }
        }
    }

    private boolean isDeprecatedType(String type, ModelElement modelElement) {
        boolean result = false;
        QualifiedName fullyQualifiedName = VariousUtils.getFullyQualifiedName(QualifiedName.create(type), modelElement.getOffset(), modelElement.getInScope());
        for (TypeElement typeElement : deprecatedTypes) {
            if (typeElement.getFullyQualifiedName().equals(fullyQualifiedName)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private abstract class PHPStructureItem implements StructureItem {

        private final ModelElement modelElement;
        private final List<? extends StructureItem> children;
        private final String sortPrefix;

        public PHPStructureItem(ModelElement elementHandle, List<? extends StructureItem> children, String sortPrefix) {
            this.modelElement = elementHandle;
            this.sortPrefix = sortPrefix;
            if (children != null) {
                this.children = children;
            } else {
                this.children = Collections.emptyList();
            }
        }

        @Override
        public boolean equals(Object obj) {
            boolean thesame = false;
            if (obj instanceof PHPStructureItem) {
                PHPStructureItem item = (PHPStructureItem) obj;
                if (item.getName() != null && this.getName() != null) {
                    thesame = item.modelElement.getName().equals(modelElement.getName()) && item.modelElement.getOffset() == modelElement.getOffset();
                }
            }
            return thesame;
        }

        @Override
        public int hashCode() {
            //int hashCode = super.hashCode();
            int hashCode = 11;
            if (getName() != null) {
                hashCode = 31 * getName().hashCode() + hashCode;
            }
            hashCode = (int) (31 * getPosition() + hashCode);
            return hashCode;
        }

        @Override
        public String getName() {
            return modelElement.getName();
        }

        @Override
        public String getSortText() {
            return sortPrefix + modelElement.getName();
        }

        @Override
        public ElementHandle getElementHandle() {
            return modelElement.getPHPElement();
        }

        public ModelElement getModelElement() {
            return modelElement;
        }

        @Override
        public ElementKind getKind() {
            return modelElement.getPHPElement().getKind();
        }

        @Override
        public Set<Modifier> getModifiers() {
            return modelElement.getPHPElement().getModifiers();
        }

        @Override
        public boolean isLeaf() {
            return (children.isEmpty());
        }

        @Override
        public List<? extends StructureItem> getNestedItems() {
            return children;
        }

        @Override
        public long getPosition() {
            return modelElement.getOffset();
        }

        @Override
        public long getEndPosition() {
            if (modelElement instanceof Scope) {
                final OffsetRange blockRange = ((Scope) modelElement).getBlockRange();
                if (blockRange != null) {
                    return blockRange.getEnd();
                }
            }
            return modelElement.getNameRange().getEnd();
        }

        @Override
        public ImageIcon getCustomIcon() {
            return null;
        }

        protected void appendInterfaces(Collection<? extends InterfaceScope> interfaes, HtmlFormatter formatter) {
            boolean first = true;
            for (InterfaceScope interfaceScope : interfaes) {
                if (interfaceScope != null) {
                    if (!first) {
                        formatter.appendText(", ");  //NOI18N
                    } else {
                        first = false;
                    }
                    appendName(interfaceScope, formatter);
                }
            }
        }

        protected void appendUsedTraits(Collection<? extends TraitScope> usedTraits, HtmlFormatter formatter) {
            boolean first = true;
            for (TraitScope traitScope : usedTraits) {
                if (!first) {
                    formatter.appendText(", ");  //NOI18N
                } else {
                    first = false;
                }
                appendName(traitScope, formatter);
            }
        }

        protected void appendFunctionDescription(FunctionScope function, HtmlFormatter formatter) {
            formatter.reset();
            if (function == null) {
                return;
            }
            if (function.isDeprecated()) {
                formatter.deprecated(true);
            }
            formatter.appendText(function.getName());
            if (function.isDeprecated()) {
                formatter.deprecated(false);
            }
            formatter.appendText("(");   //NOI18N
            List<? extends ParameterElement> parameters = function.getParameters();
            if (parameters != null && parameters.size() > 0) {
                processParameters(function, formatter, parameters);
            }
            formatter.appendText(")");   //NOI18N
            Collection<? extends String> returnTypes = function.getReturnTypeNames();
            if (!returnTypes.isEmpty()) {
                processReturnTypes(function, formatter, returnTypes);
            }
        }

        private void processParameters(FunctionScope function, HtmlFormatter formatter, List<? extends ParameterElement> parameters) {
            boolean first = true;
            for (ParameterElement formalParameter : parameters) {
                String name = formalParameter.getName();
                Set<TypeResolver> types = formalParameter.getTypes();
                if (name != null) {
                    if (!first) {
                        formatter.appendText(", "); //NOI18N
                    }
                    if (!types.isEmpty()) {
                        formatter.appendHtml(FONT_GRAY_COLOR);
                        int i = 0;
                        for (TypeResolver typeResolver : types) {
                            i++;
                            if (typeResolver.isResolved()) {
                                QualifiedName typeName = typeResolver.getTypeName(false);
                                if (typeName != null) {
                                    if (i > 1) {
                                        formatter.appendText("|"); //NOI18N
                                    }
                                    boolean deprecatedType = isDeprecatedType(typeName.toString(), function);
                                    if (deprecatedType) {
                                        formatter.deprecated(true);
                                    }
                                    formatter.appendText(typeName.toString());
                                    if (deprecatedType) {
                                        formatter.deprecated(false);
                                    }
                                }
                            }
                        }
                        formatter.appendText(" ");   //NOI18N
                        formatter.appendHtml(CLOSE_FONT);
                    }
                    formatter.appendText(name);
                    first = false;
                }
            }
        }

        private void processReturnTypes(FunctionScope function, HtmlFormatter formatter, Collection<? extends String> returnTypes) {
            formatter.appendHtml(FONT_GRAY_COLOR + ":"); //NOI18N
            int i = 0;
            for (String type : returnTypes) {
                i++;
                if (i > 1) {
                    formatter.appendText(", "); //NOI18N
                }
                boolean deprecatedType = isDeprecatedType(type.toString(), function);
                if (deprecatedType) {
                    formatter.deprecated(true);
                }
                formatter.appendText(type);
                if (deprecatedType) {
                    formatter.deprecated(false);
                }
            }
            formatter.appendHtml(CLOSE_FONT);
        }
    }

    protected void appendName(ModelElement modelElement, HtmlFormatter formatter) {
        if (modelElement.isDeprecated()) {
            formatter.deprecated(true);
            formatter.appendText(modelElement.getName());
            formatter.deprecated(false);
        } else {
            formatter.appendText(modelElement.getName());
        }
    }

    private class PHPFieldStructureItem extends PHPSimpleStructureItem {
        public PHPFieldStructureItem(FieldElement elementHandle) {
            super(elementHandle, "field"); //NOI18N
        }

        public FieldElement getField() {
            return (FieldElement) getElementHandle();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            FieldElement field = getField();
            if (field.isDeprecated()) {
                formatter.deprecated(true);
            }
            formatter.appendText(field.getName());
            if (field.isDeprecated()) {
                formatter.deprecated(false);
            }
            Collection<? extends String> types = field.getDefaultTypeNames();
            if (!types.isEmpty()) {
                formatter.appendHtml(FONT_GRAY_COLOR + ":"); //NOI18N
                int i = 0;
                for (String type : types) {
                    i++;
                    if (i > 1) {
                        formatter.appendText(", "); //NOI18N
                    }
                    boolean deprecatedType = isDeprecatedType(type, field);
                    if (deprecatedType) {
                        formatter.deprecated(true);
                    }
                    formatter.appendText(type);
                    if (deprecatedType) {
                        formatter.deprecated(false);
                    }
                }
                formatter.appendHtml(CLOSE_FONT);
            }
            return formatter.getText();
        }

    }
    private class PHPSimpleStructureItem extends PHPStructureItem {

        private String simpleText;

        public PHPSimpleStructureItem(ModelElement elementHandle, String prefix) {
            super(elementHandle, null, prefix);
            this.simpleText = elementHandle.getName();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.appendText(simpleText);
            return formatter.getText();
        }

    }

    private class PHPNamespaceStructureItem extends PHPStructureItem {
        public PHPNamespaceStructureItem(NamespaceScope elementHandle, List<? extends StructureItem> children) {
            super(elementHandle, children, "namespace"); //NOI18N
        }

        public NamespaceScope getNamespaceScope() {
            return (NamespaceScope) getModelElement();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.reset();
            if (getNamespaceScope().isDeprecated()) {
                formatter.deprecated(true);
            }
            formatter.appendText(getName());
            if (getNamespaceScope().isDeprecated()) {
                formatter.deprecated(false);
            }
            return formatter.getText();
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.MODULE;
        }
    }

    private class PHPUseStructureItem extends PHPStructureItem {

        public PHPUseStructureItem(UseScope elementHandle) {
            super(elementHandle, null, "aaaa_use"); //NOI18N
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.reset();
            UseScope useElement = (UseScope) getElementHandle();
            String name = getName();
            boolean deprecatedType = isDeprecatedType(name, useElement);
            if (deprecatedType) {
                formatter.deprecated(true);
            }
            formatter.appendText(name);
            final AliasedName aliasedName = useElement.getAliasedName();
            if (aliasedName != null) {
                formatter.appendText(" as "); //NOI18N
                formatter.appendText(aliasedName.getAliasName());
            }
            if (deprecatedType) {
                formatter.deprecated(false);
            }
            return formatter.getText();
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.RULE;
        }

    }

    private class PHPClassStructureItem extends PHPStructureItem {
        private final String superClassName;
        private final Collection<? extends InterfaceScope> interfaces;
        private final Collection<? extends TraitScope> usedTraits;

        public PHPClassStructureItem(ClassScope elementHandle, List<? extends StructureItem> children) {
            super(elementHandle, children, "cl"); //NOI18N
            superClassName = ModelUtils.getFirst(getClassScope().getSuperClassNames());
            interfaces = getClassScope().getSuperInterfaceScopes();
            usedTraits = getClassScope().getTraits();
        }

        private ClassScope getClassScope() {
            return (ClassScope) getModelElement();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.reset();
            appendName(getClassScope(), formatter);
            if (superClassName != null) {
                formatter.appendHtml(FONT_GRAY_COLOR + "::"); //NOI18N
                formatter.appendText(superClassName);
                formatter.appendHtml(CLOSE_FONT);
            }
            if (interfaces != null && interfaces.size() > 0) {
                formatter.appendHtml(FONT_GRAY_COLOR + ":"); //NOI18N
                appendInterfaces(interfaces, formatter);
                formatter.appendHtml(CLOSE_FONT);
            }
            if (usedTraits != null && usedTraits.size() > 0) {
                formatter.appendHtml(FONT_GRAY_COLOR + "#"); //NOI18N
                appendUsedTraits(usedTraits, formatter);
                formatter.appendHtml(CLOSE_FONT);
            }
            return formatter.getText();
        }

    }

    private class PHPConstantStructureItem extends PHPStructureItem {

        public PHPConstantStructureItem(ConstantElement elementHandle, String prefix) {
            super(elementHandle, null, prefix);
        }

        public ConstantElement getConstant() {
            return (ConstantElement) getModelElement();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.reset();
            if (getConstant().isDeprecated()) {
                formatter.deprecated(true);
            }
            formatter.appendText(getName());
            if (getConstant().isDeprecated()) {
                formatter.deprecated(false);
            }
            final ConstantElement constant = getConstant();
            String value = constant.getValue();
            if (value != null) {
                formatter.appendText(" "); //NOI18N
                formatter.appendHtml(FONT_GRAY_COLOR); //NOI18N
                formatter.appendText(value);
                formatter.appendHtml(CLOSE_FONT);
            }
            return formatter.getText();
        }

    }

    private class PHPFunctionStructureItem extends PHPStructureItem {

        public PHPFunctionStructureItem(FunctionScope elementHandle, List<? extends StructureItem> children) {
            super(elementHandle, children, "fn"); //NOI18N
        }

        public FunctionScope getFunctionScope() {
            return (FunctionScope) getModelElement();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.reset();
            appendFunctionDescription(getFunctionScope(), formatter);
            return formatter.getText();
        }

    }

    private class PHPMethodStructureItem extends PHPStructureItem {

        public PHPMethodStructureItem(MethodScope elementHandle, List<? extends StructureItem> children) {
            super(elementHandle, children, "fn"); //NOI18N
        }

        public MethodScope getMethodScope() {
            return (MethodScope) getModelElement();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.reset();
            appendFunctionDescription(getMethodScope(), formatter);
            return formatter.getText();
        }

    }

    private class PHPInterfaceStructureItem extends PHPStructureItem {
        private static final String PHP_INTERFACE_ICON = "org/netbeans/modules/php/editor/resources/interface.png"; //NOI18N
        private final Collection<? extends InterfaceScope> interfaces;

        public PHPInterfaceStructureItem(InterfaceScope elementHandle, List<? extends StructureItem> children) {
            super(elementHandle, children, "cl"); //NOI18N
            interfaces = getInterfaceScope().getSuperInterfaceScopes();
        }

        @Override
        public ImageIcon getCustomIcon() {
            if (interfaceIcon == null) {
                interfaceIcon = new ImageIcon(ImageUtilities.loadImage(PHP_INTERFACE_ICON));
            }
            return interfaceIcon;
        }

        private InterfaceScope getInterfaceScope() {
            return (InterfaceScope) getModelElement();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.reset();
            appendName(getInterfaceScope(), formatter);
            if (interfaces != null && interfaces.size() > 0) {
                formatter.appendHtml(FONT_GRAY_COLOR + "::"); //NOI18N
                appendInterfaces(interfaces, formatter);
                formatter.appendHtml(CLOSE_FONT);
            }
            return formatter.getText();
        }

    }

    private class PHPTraitStructureItem extends PHPStructureItem {
        private static final String PHP_TRAIT_ICON = "org/netbeans/modules/php/editor/resources/trait.png"; //NOI18N
        private final Collection<? extends TraitScope> usedTraits;

        public PHPTraitStructureItem(ModelElement elementHandle, List<? extends StructureItem> children) {
            super(elementHandle, children, "cl"); //NOI18N
            usedTraits = getTraitScope().getTraits();
        }

        @Override
        public ImageIcon getCustomIcon() {
            if (traitIcon == null) {
                traitIcon = new ImageIcon(ImageUtilities.loadImage(PHP_TRAIT_ICON));
            }
            return traitIcon;
        }

        private TraitScope getTraitScope() {
            return (TraitScope) getModelElement();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.reset();
            appendName(getTraitScope(), formatter);
            if (usedTraits != null && usedTraits.size() > 0) {
                formatter.appendHtml(FONT_GRAY_COLOR + "#"); //NOI18N
                appendUsedTraits(usedTraits, formatter);
                formatter.appendHtml(CLOSE_FONT);
            }
            return formatter.getText();
        }

    }

    private class PHPConstructorStructureItem extends PHPStructureItem {

        public PHPConstructorStructureItem(MethodScope elementHandle, List<? extends StructureItem> children) {
            super(elementHandle, children, "con"); //NOI18N
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.CONSTRUCTOR;
        }

        public MethodScope getMethodScope() {
            return (MethodScope) getModelElement();
        }

        @Override
        public String getHtml(HtmlFormatter formatter) {
            formatter.reset();
            appendFunctionDescription(getMethodScope(), formatter);
            return formatter.getText();
        }

    }

}
