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
package org.netbeans.modules.refactoring.java.plugins;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.refactoring.java.api.MemberInfo;
import org.netbeans.modules.refactoring.java.api.MemberInfo.Group;
import org.netbeans.modules.refactoring.java.api.PullUpRefactoring;
import org.netbeans.modules.refactoring.java.spi.RefactoringVisitor;
import org.netbeans.modules.refactoring.java.spi.ToPhaseException;

/**
 *
 * @author Jan Becicka
 * @author Ralph Benjamin Ruijs
 */
public class PullUpTransformer extends RefactoringVisitor {

    private MemberInfo<ElementHandle<? extends Element>>[] members;
    private TypeElement targetType;
    private TypeElement sourceType;
    private PullUpRefactoring refactoring;

    public PullUpTransformer(PullUpRefactoring refactoring) {
        this.refactoring = refactoring;
        this.members = refactoring.getMembers();
    }

    @Override
    public void setWorkingCopy(WorkingCopy copy) throws ToPhaseException {
        super.setWorkingCopy(copy);
        this.targetType = refactoring.getTargetType().resolve(copy);
        this.sourceType = (TypeElement) refactoring.getSourceType().resolveElement(copy);
    }

    @Override
    public Tree visitClass(ClassTree tree, Element p) {
        Element classElement = workingCopy.getTrees().getElement(getCurrentPath());
        GeneratorUtilities genUtils = GeneratorUtilities.get(workingCopy); // helper
        AtomicBoolean classIsAbstract = new AtomicBoolean(classElement.getKind().isInterface());
        if (classElement.equals(targetType)) {
            addMembersToTarget(tree, classIsAbstract, targetType, genUtils);
        } else if (classElement.equals(sourceType)) {
            removeMembersFromSource(tree, classIsAbstract);
        }
        return super.visitClass(tree, p);
    }

    private void addMembersToTarget(ClassTree tree, AtomicBoolean classIsAbstract, TypeElement classElement, GeneratorUtilities genUtils) {
        ClassTree njuClass = tree;
        for (int i = 0; i < members.length; i++) {
            Element member = members[i].getElementHandle().resolve(workingCopy);
            if(member.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) member;
                method = (ExecutableElement) workingCopy.getElementUtilities().getImplementationOf(method, sourceType);
                if(method != null) {
                    member = method;
                }
            }
            Group group = members[i].getGroup();
            if (group == MemberInfo.Group.IMPLEMENTS) {
                njuClass = make.addClassImplementsClause(njuClass, make.QualIdent(member));
            } else {
                if (members[i].isMakeAbstract()) {
                    if (classIsAbstract.compareAndSet(false, true)) {
                        makeClassAbstract(njuClass);
                    }
                    njuClass = addAbstractMemberToTarget(njuClass, member, classElement, genUtils);
                } else {
                    if (member.getModifiers().contains(Modifier.ABSTRACT)
                            && classIsAbstract.compareAndSet(false, true)) {
                        makeClassAbstract(njuClass);
                    }
                    njuClass = addMemberToTarget(njuClass, member, classElement, group, genUtils);
                }
            }
        }
        if (njuClass != tree) {
            rewrite(tree, njuClass);
        }
    }

    private void removeMembersFromSource(ClassTree tree, AtomicBoolean classIsAbstract) {
        ClassTree njuClass = tree;
        for (int i = 0; i < members.length; i++) {
            if (members[i].getGroup() == MemberInfo.Group.IMPLEMENTS) {
                for (Tree t : njuClass.getImplementsClause()) {
                    Element currentInterface = workingCopy.getTrees().getElement(TreePath.getPath(getCurrentPath(), t));
                    if (currentInterface.equals(members[i].getElementHandle().resolve(workingCopy))) {
                        njuClass = make.removeClassImplementsClause(njuClass, t);
                        rewrite(tree, njuClass);
                    }
                }
            } else {
                Element current = workingCopy.getTrees().getElement(getCurrentPath());
                Element member = members[i].getElementHandle().resolve(workingCopy);
                if(member != null && member.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement) member;
                    method = (ExecutableElement) workingCopy.getElementUtilities().getImplementationOf(method, sourceType);
                    if(method != null) {
                        member = method;
                    }
                }
                if (member != null && member.getEnclosingElement().equals(current)) {
                    if ((classIsAbstract.get() && !member.getModifiers().contains(Modifier.DEFAULT)) || !members[i].isMakeAbstract()
                            || (member.getModifiers().contains(Modifier.ABSTRACT) && targetType.getKind().isInterface())) {
                        // in case of interface always remove pulled method
                        njuClass = make.removeClassMember(njuClass, workingCopy.getTrees().getTree(member));
                        rewrite(tree, njuClass);
                    } else if (members[i].isMakeAbstract() && member.getModifiers().contains(Modifier.PRIVATE)) {
                        MethodTree method = (MethodTree) workingCopy.getTrees().getTree(member);
                        ModifiersTree mods = make.removeModifiersModifier(method.getModifiers(), Modifier.PRIVATE);
                        mods = make.addModifiersModifier(mods, targetType.getKind().isInterface() ? Modifier.PUBLIC : Modifier.PROTECTED);
                        rewrite(method.getModifiers(), mods);
                    }
                }
            }
        }
    }

    private ClassTree addAbstractMemberToTarget(ClassTree njuClass, Element methodElm, Element classElement, GeneratorUtilities genUtils) {
        MethodTree method = (MethodTree) workingCopy.getTrees().getTree(methodElm);
        Set<Modifier> flags = method.getModifiers().getFlags();
        Set<Modifier> mod = flags.isEmpty() ? EnumSet.noneOf(Modifier.class) : EnumSet.copyOf(flags);
        mod.add(Modifier.ABSTRACT);
        mod.remove(Modifier.FINAL);
        // abstract method cannot be default
        mod.remove(Modifier.DEFAULT);
        // abstract method cannot be synchronized
        mod.remove(Modifier.SYNCHRONIZED);
        if (classElement.getKind().isInterface()) {
            mod.remove(Modifier.PUBLIC);
            mod.remove(Modifier.PROTECTED);
            mod.remove(Modifier.PRIVATE);
            mod.remove(Modifier.ABSTRACT);
        }
        if (mod.contains(Modifier.PRIVATE)) {
            mod.remove(Modifier.PRIVATE);
            mod.add(Modifier.PROTECTED);
        }
        MethodTree newMethod = make.Method(
                make.Modifiers(mod),
                method.getName(),
                method.getReturnType(),
                method.getTypeParameters(),
                method.getParameters(),
                method.getThrows(),
                (BlockTree) null,
                (ExpressionTree) method.getDefaultValue());
        newMethod = genUtils.importFQNs(newMethod);
        method = genUtils.importComments(method, workingCopy.getTrees().getPath(methodElm).getCompilationUnit());
        genUtils.copyComments(method, newMethod, false);
        genUtils.copyComments(method, newMethod, true);
        njuClass = genUtils.insertClassMember(njuClass, newMethod);
        return njuClass;
    }

    private void makeClassAbstract(ClassTree njuClass) {
        Set<Modifier> mod = EnumSet.copyOf(njuClass.getModifiers().getFlags());
        mod.add(Modifier.ABSTRACT);
        mod.remove(Modifier.FINAL);
        ModifiersTree modifiers = make.Modifiers(mod);
        rewrite(njuClass.getModifiers(), modifiers);
    }

    private ClassTree addMemberToTarget(ClassTree njuClass, Element member, TypeElement classElement, Group group, GeneratorUtilities genUtils) {
        TreePath mpath = workingCopy.getTrees().getPath(member);
        Tree memberTree = genUtils.importComments(mpath.getLeaf(), mpath.getCompilationUnit());
        memberTree = genUtils.importFQNs(memberTree);
        memberTree = fixGenericTypes(memberTree, mpath, member);
        if (member.getModifiers().contains(Modifier.PRIVATE)) {
            Tree newMemberTree = null;
            if (group == Group.METHOD) {
                MethodTree oldOne = (MethodTree) memberTree;
                BlockTree body = updateSuperThisReferences(oldOne.getBody(), mpath);
                newMemberTree = make.Method(
                        make.addModifiersModifier(make.removeModifiersModifier(oldOne.getModifiers(), Modifier.PRIVATE), Modifier.PROTECTED),
                        oldOne.getName(),
                        oldOne.getReturnType(),
                        oldOne.getTypeParameters(),
                        oldOne.getParameters(),
                        oldOne.getThrows(),
                        body,
                        (ExpressionTree) oldOne.getDefaultValue());
            } else if (group == Group.FIELD) {
                VariableTree oldOne = (VariableTree) memberTree;
                newMemberTree = make.Variable(
                        make.addModifiersModifier(make.removeModifiersModifier(oldOne.getModifiers(), Modifier.PRIVATE), Modifier.PROTECTED),
                        oldOne.getName(),
                        oldOne.getType(),
                        oldOne.getInitializer());
            } else if (group == Group.TYPE) {
                ClassTree oldOne = (ClassTree) memberTree;
                switch (member.getKind()) {
                    case CLASS:
                        newMemberTree = make.Class(
                                make.addModifiersModifier(make.removeModifiersModifier(oldOne.getModifiers(), Modifier.PRIVATE), Modifier.PROTECTED),
                                oldOne.getSimpleName(),
                                oldOne.getTypeParameters(),
                                oldOne.getExtendsClause(),
                                oldOne.getImplementsClause(),
                                oldOne.getMembers());
                        break;
                    case INTERFACE:
                        newMemberTree = make.Interface(
                                make.addModifiersModifier(make.removeModifiersModifier(oldOne.getModifiers(), Modifier.PRIVATE), Modifier.PROTECTED),
                                oldOne.getSimpleName(),
                                oldOne.getTypeParameters(),
                                oldOne.getImplementsClause(),
                                oldOne.getMembers());
                        break;
                    case ANNOTATION_TYPE:
                        newMemberTree = make.AnnotationType(
                                make.addModifiersModifier(make.removeModifiersModifier(oldOne.getModifiers(), Modifier.PRIVATE), Modifier.PROTECTED),
                                oldOne.getSimpleName(),
                                oldOne.getMembers());
                        break;
                    case ENUM:
                        newMemberTree = make.Enum(
                                make.addModifiersModifier(make.removeModifiersModifier(oldOne.getModifiers(), Modifier.PRIVATE), Modifier.PROTECTED),
                                oldOne.getSimpleName(),
                                oldOne.getImplementsClause(),
                                oldOne.getMembers());
                        break;
                }
            }
            if (newMemberTree != null) {
                genUtils.copyComments(memberTree, newMemberTree, false);
                genUtils.copyComments(memberTree, newMemberTree, true);
                njuClass = genUtils.insertClassMember(njuClass, newMemberTree);
            }
        } else {
            if (group == Group.METHOD) {
                MethodTree oldOne = (MethodTree) memberTree;
                BlockTree body = updateSuperThisReferences(oldOne.getBody(), mpath);
                ExecutableElement overriddenMethod = workingCopy.getElementUtilities().getOverriddenMethod((ExecutableElement) member);
                MethodTree newMemberTree = make.Method(
                        overriddenMethod != null && workingCopy.getElementUtilities().isMemberOf(overriddenMethod, targetType)? oldOne.getModifiers() : removeAnnotations(workingCopy, make, oldOne.getModifiers(), mpath),
                        oldOne.getName(),
                        oldOne.getReturnType(),
                        oldOne.getTypeParameters(),
                        oldOne.getParameters(),
                        oldOne.getThrows(),
                        body,
                        (ExpressionTree) oldOne.getDefaultValue());
                genUtils.copyComments(memberTree, newMemberTree, false);
                genUtils.copyComments(memberTree, newMemberTree, true);
                njuClass = genUtils.insertClassMember(njuClass, newMemberTree);
            } else {
                njuClass = genUtils.insertClassMember(njuClass, memberTree);
            }
        }
        return njuClass;
    }

    static ModifiersTree removeAnnotations(WorkingCopy workingCopy, TreeMaker make,ModifiersTree oldOne, TreePath path) {
        TypeElement override = workingCopy.getElements().getTypeElement("java.lang.Override");
        if(override == null) {
            return oldOne;
        }
        List<AnnotationTree> newAnnotations = new LinkedList<>();
        for (AnnotationTree annotationTree : oldOne.getAnnotations()) {
            Element el = workingCopy.getTrees().getElement(new TreePath(path, annotationTree));
            if(!override.equals(el)) {
                newAnnotations.add(annotationTree);
            }
        }
        return make.Modifiers(oldOne, newAnnotations);
    }

    private <E extends Tree> E fixGenericTypes(E tree, final TreePath path, final Element member) {
        final Map<TypeMirror, TypeParameterElement> mappings = new HashMap<TypeMirror, TypeParameterElement>();
        DeclaredType declaredType = (DeclaredType) sourceType.asType();
        for (TypeMirror typeMirror : declaredType.getTypeArguments()) {
            DeclaredType currentElement = declaredType;
            deepSearchTypes(currentElement, typeMirror, typeMirror, mappings);
        }
        final Types types = workingCopy.getTypes();

        final Map<IdentifierTree, Tree> original2Translated = new HashMap<IdentifierTree, Tree>();
        TreeScanner<Void, Void> scanner = new TreeScanner<Void, Void>() {

            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                Element element = workingCopy.getTrees().getElement(new TreePath(path, node));
                if (element != null && element.getKind() == ElementKind.TYPE_PARAMETER) {
                    Element typeElement = types.asElement(element.asType());
                    if (typeElement != null && typeElement.getKind() == ElementKind.TYPE_PARAMETER) {
                        TypeParameterElement parameterElement = (TypeParameterElement) typeElement;
                        Element genericElement = parameterElement.getGenericElement();
                        if (genericElement != member) {
                            // genericElement is niet gelijk aan het te verplaatsen element. Dus we moeten deze veranderen.
                            // Is het parameterElement gebruikt bij het maken van de superclass

                            Tree type;
                            TypeParameterElement target = mappings.get(parameterElement.asType());
                            if (target != null) {
                                type = make.Type(target.asType());
                            } else {
                                List<? extends TypeMirror> bounds = parameterElement.getBounds();
                                if (bounds.isEmpty()) {
                                    type = make.Type("Object"); // NOI18N
                                } else {
                                    type = make.Type(bounds.get(0));
                                }
                            }
                            original2Translated.put(node, type);
                        }
                    }
                }
                return super.visitIdentifier(node, p);
            }
        };
        scanner.scan(tree, null);
        E result = (E) workingCopy.getTreeUtilities().translate(tree, original2Translated);
        return result;
    }

    private BlockTree updateSuperThisReferences(BlockTree body, final TreePath mpath) {
        final Map<ExpressionTree, ExpressionTree> original2Translated = new HashMap<ExpressionTree, ExpressionTree>();
        final Trees trees = workingCopy.getTrees();
        TreeScanner<Boolean, Void> idScan = new TreeScanner<Boolean, Void>() {
            @Override
            public Boolean visitMemberSelect(MemberSelectTree node, Void nothing) {
                String isThis = node.getExpression().toString();
                if (isThis.equals("super") || isThis.endsWith(".super")) { //NOI18N
                    TreePath currentPath = new TreePath(mpath, node);
                    Element el = trees.getElement(currentPath);
                    if (el.getEnclosingElement().equals(targetType)) {
                        original2Translated.put(node, make.Identifier(node.getIdentifier()));
                        return Boolean.TRUE;
                    }
                }
                return super.visitMemberSelect(node, nothing);
            }

            @Override
            public Boolean reduce(Boolean r1, Boolean r2) {
                return (r1 == Boolean.TRUE || r2 == Boolean.TRUE);
            }
        };
        boolean update = idScan.scan(body, null) == Boolean.TRUE;

        if (update) {
            body = (BlockTree) workingCopy.getTreeUtilities().translate(body, original2Translated);
        }
        return body;
    }

    private boolean deepSearchTypes(DeclaredType currentElement, TypeMirror orig, TypeMirror something, Map<TypeMirror, TypeParameterElement> mappings) {
        Types types = workingCopy.getTypes();
        List<? extends TypeMirror> directSupertypes = types.directSupertypes(currentElement);
        for (TypeMirror superType : directSupertypes) {
            DeclaredType type = (DeclaredType) superType;
            List<? extends TypeMirror> typeArguments = type.getTypeArguments();
            for (int i = 0; i < typeArguments.size(); i++) {
                TypeMirror typeArgument = typeArguments.get(i);
                if (something.equals(typeArgument)) {
                    TypeElement asElement = (TypeElement) type.asElement();
                    mappings.put(orig, asElement.getTypeParameters().get(i));
                    if (types.erasure(targetType.asType()).equals(types.erasure(superType))) {
                        return true;
                    }
                    if(deepSearchTypes(type, orig, typeArgument, mappings)) {
                        break;
                    }
                }
            }
            if (types.erasure(targetType.asType()).equals(types.erasure(superType))) {
                mappings.remove(orig);
                return true;
            }
        }
        return false;
    }
}
