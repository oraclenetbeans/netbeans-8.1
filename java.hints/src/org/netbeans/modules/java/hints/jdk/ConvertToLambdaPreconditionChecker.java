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
 * Contributor(s): Lyle Franklin <lylejfranklin@gmail.com>
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints.jdk;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.hints.errors.Utilities;


public class ConvertToLambdaPreconditionChecker {

    private final TreePath pathToNewClassTree;
    private final NewClassTree newClassTree;
    private final MethodTree lambdaMethodTree;
    private final Scope localScope;
    private final CompilationInfo info;
    private final Types types;
    private boolean foundRefToThisOrSuper = false;
    private boolean foundShadowedVariable = false;
    private boolean foundRecursiveCall = false;
    private boolean foundOverloadWhichMakesLambdaAmbiguous = false;
    private boolean foundAssignmentToRawType = false;
    private boolean foundAssignmentToSupertype = false;
    private boolean foundErroneousTargetType = false;
    private BlockTree singleStatementLambdaMethodBody = null;
    private boolean foundMemberReferenceCandidate = false;
    private boolean havePreconditionsBeenChecked = false;
    private boolean foundRefToUninitializedVar = false;
    private final Element ownerClass;
    private final Element createdClass;

    public ConvertToLambdaPreconditionChecker(TreePath pathToNewClassTree, CompilationInfo info) {

        this.pathToNewClassTree = pathToNewClassTree;
        this.newClassTree = (NewClassTree) pathToNewClassTree.getLeaf();
        this.info = info;
        this.types = info.getTypes();

        Element el = info.getTrees().getElement(pathToNewClassTree);
        if (el.getKind() == ElementKind.CONSTRUCTOR) {
            createdClass = el.getEnclosingElement();
        } else {
            createdClass = null;
        }
        this.lambdaMethodTree = getMethodFromFunctionalInterface(this.newClassTree);
        this.localScope = getScopeFromTree(this.pathToNewClassTree);
        this.ownerClass = findFieldOwner();
    }
    
    /**
     * Returns the class Element, if the pathToNewClassTree is a part of class field's
     * init expression; otherwise null.
     * @return 
     */
    private Element findFieldOwner() {
        TreePath p = pathToNewClassTree.getParentPath();
        while (p != null) {
            Tree t = p.getLeaf();
            if (t.getKind() == Tree.Kind.METHOD) {
                // this is OK
                return null;
            } else if (t.getKind() == Tree.Kind.CLASS) {
                return info.getTrees().getElement(p);
            }
            p = p.getParentPath();
        }
        return null;
    }
    
    private static MethodTree getMethodFromFunctionalInterface(NewClassTree newClassTree) {
        //ignore first member, which is a synthetic constructor call
        ClassTree classTree = newClassTree.getClassBody();
        return (MethodTree) classTree.getMembers().get(1);
    }

    public boolean passesAllPreconditions() {

        ensurePreconditionsAreChecked();

        return !foundRefToThisOrSuper()
                && !foundShadowedVariable()
                && !foundRecursiveCall()
                && !foundOverloadWhichMakesLambdaAmbiguous()
                && !foundAssignmentToSupertype()
                && !foundAssignmentToRawtype()
                && !foundRefToUninitializedVar();
    }
    
    private void ensurePreconditionsAreChecked() {
        if (!havePreconditionsBeenChecked) {
            TreePath path = new TreePath(pathToNewClassTree, lambdaMethodTree);
            new PreconditionScanner().scan(path, info.getTrees());
            checkForOverload();
            verifyTargetType();
            
            havePreconditionsBeenChecked = true;
        }
    }
    
    boolean foundRefToUninitializedVar() {
        ensurePreconditionsAreChecked();
        return foundRefToUninitializedVar;
    }
    
    public boolean passesFatalPreconditions() {
        return !foundRefToThisOrSuper() &&
               !foundRecursiveCall() &&
               !foundErroneousTargetType() && 
               !foundRefToUninitializedVar();
    }

    public boolean needsCastToExpectedType() {
        ensurePreconditionsAreChecked();
        return foundOverloadWhichMakesLambdaAmbiguous()
                || foundAssignmentToSupertype() 
                // #234080: cannot infer lambda types from the raw type
                || foundAssignmentToRawtype();
    }

    public boolean foundRefToThisOrSuper() {
        ensurePreconditionsAreChecked();
        return foundRefToThisOrSuper;
    }

    public boolean foundShadowedVariable() {
        ensurePreconditionsAreChecked();
        return foundShadowedVariable;
    }

    public boolean foundRecursiveCall() {
        ensurePreconditionsAreChecked();
        return foundRecursiveCall;
    }

    public boolean foundOverloadWhichMakesLambdaAmbiguous() {
        ensurePreconditionsAreChecked();
        return foundOverloadWhichMakesLambdaAmbiguous;
    }

    public boolean foundAssignmentToSupertype() {
        ensurePreconditionsAreChecked();
        return foundAssignmentToSupertype;
    }

    public boolean foundAssignmentToRawtype() {
        ensurePreconditionsAreChecked();
        return foundAssignmentToRawType;
    }

    public boolean foundErroneousTargetType() {
        ensurePreconditionsAreChecked();
        return foundErroneousTargetType;
    }

    public boolean foundMemberReferenceCandidate() {
        ensurePreconditionsAreChecked();
        return foundMemberReferenceCandidate;
    }

    private void checkForOverload() {
        foundOverloadWhichMakesLambdaAmbiguous = doesOverloadMakeLambdaAmbiguous();
    }

    private class PreconditionScanner extends TreePathScanner<Tree, Trees> {

        @Override
        public Tree visitClass(ClassTree node, Trees p) {
            return null;
        }

        @Override
        public Tree visitMethod(MethodTree methodTree, Trees trees) {

            //don't visit synthetic methods
            TreePath path = getCurrentPath();
            if (info.getTreeUtilities().isSynthetic(path)) {
                return methodTree;
            }
            return super.visitMethod(methodTree, trees);
        }

        @Override
        public Tree visitBlock(BlockTree blockTree, Trees trees) {
            TreePath path = getCurrentPath();
            if (path.getParentPath().getLeaf() == lambdaMethodTree && blockTree.getStatements().size() == 1) {
                singleStatementLambdaMethodBody = blockTree;
            }
            return super.visitBlock(blockTree, trees);
        }

        @Override
        public Tree visitIdentifier(IdentifierTree identifierTree, Trees trees) {

            //check for ref to 'this'
             IDENT: if (identifierTree.getName().contentEquals("this") || identifierTree.getName().contentEquals("super")) {
                Tree parent = getCurrentPath().getParentPath().getLeaf();
                if (parent.getKind() == Tree.Kind.MEMBER_SELECT) {
                    // something.this or something.super - resolve the type
                    Element el = info.getTrees().getElement(getCurrentPath().getParentPath());
                    if (el == createdClass) {
                        foundRefToThisOrSuper = true;
                    } else {
                        // this.something or super.something, dereference
                        TypeMirror m = info.getTrees().getTypeMirror(getCurrentPath());
                        if (!Utilities.isValidType(m)) {
                            // just to be sure
                            foundRefToThisOrSuper = true;
                        } else if (m.getKind() == TypeKind.DECLARED) {
                            if (createdClass != null && 
                                info.getTypes().isSubtype(createdClass.asType(), m)) {
                                foundRefToThisOrSuper = true;
                            }
                        }
                    }
                } else {
                    // unqualified this/super
                    foundRefToThisOrSuper = true;
                }
            }
            Element el = info.getTrees().getElement(getCurrentPath());
            if (el != null && el.getKind() == ElementKind.FIELD) {
                if (ownerClass == el.getEnclosingElement()) {
                    if (!el.getModifiers().contains(Modifier.FINAL)) {
                        foundRefToUninitializedVar = true;
                    } else {
                        // PENDING: shortcut: the field must be assigned 
                        // in its initializer. It would more appropriate to check whether it
                        // is definitively assigned before the lambda code - future enhancement
                        VariableTree vt = (VariableTree)info.getTrees().getTree(el);
                        foundRefToUninitializedVar |= vt.getInitializer() == null;
                    }
                }
            }
            return super.visitIdentifier(identifierTree, trees);
        }

        @Override
        public Tree visitVariable(VariableTree variableDeclTree, Trees trees) {

            //check for shadowed variable
            if (shadowsVariable(variableDeclTree.getName())) {
                foundShadowedVariable = true;
            }
            return super.visitVariable(variableDeclTree, trees);
        }

        @Override
        public Tree visitMethodInvocation(MethodInvocationTree methodInvocationTree, Trees trees) {
            String nameSuggestion = org.netbeans.modules.editor.java.Utilities.varNameSuggestion(methodInvocationTree.getMethodSelect());
            //check for recursion
            if (nameSuggestion != null && lambdaMethodTree.getName().contentEquals(nameSuggestion)) {
                ExpressionTree selector = getSelector(methodInvocationTree);
                if (selector == null || (org.netbeans.modules.editor.java.Utilities.varNameSuggestion(selector) != null
                        && org.netbeans.modules.editor.java.Utilities.varNameSuggestion(selector).contentEquals("this"))) {
                    foundRecursiveCall = true;
                }
            }
            if (singleStatementLambdaMethodBody == getCurrentPath().getParentPath().getParentPath().getLeaf()) {
                Tree parent = getCurrentPath().getParentPath().getLeaf();
                if (parent.getKind() == Tree.Kind.EXPRESSION_STATEMENT || parent.getKind() == Tree.Kind.RETURN) {
                    boolean check = true;
                    Iterator<? extends VariableTree> paramsIt = lambdaMethodTree.getParameters().iterator();
                    ExpressionTree methodSelect = methodInvocationTree.getMethodSelect();
                    if (paramsIt.hasNext() && methodSelect.getKind() == Tree.Kind.MEMBER_SELECT) {
                        ExpressionTree expr = ((MemberSelectTree) methodSelect).getExpression();
                        if (expr.getKind() == Tree.Kind.IDENTIFIER) {
                            if (!((IdentifierTree)expr).getName().contentEquals(paramsIt.next().getName())) {
                                paramsIt = lambdaMethodTree.getParameters().iterator();
                            }
                        }
                    }
                    Iterator<? extends ExpressionTree> argsIt = methodInvocationTree.getArguments().iterator();
                    while (check && argsIt.hasNext() && paramsIt.hasNext()) {
                        ExpressionTree arg = argsIt.next();
                        if (arg.getKind() != Tree.Kind.IDENTIFIER || !paramsIt.next().getName().contentEquals(((IdentifierTree)arg).getName())) {
                            check = false;
                        }
                    }
                    if (check && !paramsIt.hasNext() && !argsIt.hasNext()) {
                        foundMemberReferenceCandidate = true;
                    }
                }
            }
            return super.visitMethodInvocation(methodInvocationTree, trees);
        }
    }

    private boolean shadowsVariable(CharSequence variableName) {
        return Utilities.isSymbolUsed(info, pathToNewClassTree, variableName, localScope);
    }

    private ExpressionTree getSelector(Tree tree) {
        switch (tree.getKind()) {
            case MEMBER_SELECT:
                return ((MemberSelectTree) tree).getExpression();
            case METHOD_INVOCATION:
                return getSelector(((MethodInvocationTree) tree).getMethodSelect());
            case NEW_CLASS:
                return getSelector(((NewClassTree) tree).getIdentifier());
            default:
                return null;
        }
    }

    private boolean doesOverloadMakeLambdaAmbiguous() {
        TreePath parentPath = pathToNewClassTree.getParentPath();
        Tree parentTree = parentPath.getLeaf();
        if (!isInvocationTree(parentTree)) {
            return false;
        }

        ExecutableElement invokingElement = getElementFromInvokingTree(parentPath);
        int indexOfLambdaInArgs = getLambdaIndexFromInvokingTree(parentTree);

        if (invokingElement == null || indexOfLambdaInArgs == -1) {
            return false;
        }

        return isLambdaAnAmbiguousArgument(invokingElement, indexOfLambdaInArgs);
    }

    private ExecutableElement getElementFromInvokingTree(TreePath treePath) {
        Tree invokingTree = treePath.getLeaf();
        Element result;
        if (invokingTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
            MethodInvocationTree invokingMethTree = ((MethodInvocationTree) invokingTree);
            TreePath methodTreePath = new TreePath(treePath, invokingMethTree);
            result = getElementFromTreePath(methodTreePath);
        } else {
            result = getElementFromTreePath(treePath);
        }
        if (result != null && (result.getKind() == ElementKind.CONSTRUCTOR || result.getKind() == ElementKind.METHOD)) {
            return (ExecutableElement) result;
        }
        return null;
    }

    private int getLambdaIndexFromInvokingTree(Tree invokingTree) {
        List<? extends ExpressionTree> invokingArgs;
        if (invokingTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
            MethodInvocationTree invokingMethTree = ((MethodInvocationTree) invokingTree);
            invokingArgs = invokingMethTree.getArguments();
        } else if (invokingTree.getKind() == Tree.Kind.NEW_CLASS) {
            NewClassTree invokingConstrTree = (NewClassTree) invokingTree;
            invokingArgs = invokingConstrTree.getArguments();
        } else {
            return -1;
        }

        return getIndexOfLambdaInArgs(newClassTree, invokingArgs);
    }

    private int getIndexOfLambdaInArgs(Tree treeToFind, List<? extends ExpressionTree> argsToSearch) {

        for (int i = 0; i < argsToSearch.size(); i++) {
            if (argsToSearch.get(i) == treeToFind) {
                return i;
            }
        }
        return -1;
    }

    private boolean isLambdaAnAmbiguousArgument(ExecutableElement invokingElement, int indexOfLambdaInArgs) {
        
        Element classOfInvokingElement = invokingElement.getEnclosingElement();
        for (Element possibleMatchingElement : info.getElementUtilities().getMembers(classOfInvokingElement.asType(), null)) {

            //ignore invoking element
            if (possibleMatchingElement == invokingElement) {
                continue;
            }

            if (possibleMatchingElement.getKind() != invokingElement.getKind()) {
                continue;
            }

            if (doesInvokingElementMatchFound(invokingElement,
                    (ExecutableElement) possibleMatchingElement, indexOfLambdaInArgs)) {
                return true;
            }
        }

        return false;
    }

    private Element getElementFromTreePath(TreePath path) {
        return info.getTrees().getElement(path);
    }

    private boolean doesInvokingElementMatchFound(ExecutableElement invokingElement, ExecutableElement possibleMatchingElement, int indexOfLambdaInArgs) {
        return possibleMatchingElement.getSimpleName().equals(invokingElement.getSimpleName())
                && doInvokingParamsMatchFound(invokingElement, possibleMatchingElement, indexOfLambdaInArgs);
    }

    private boolean doInvokingParamsMatchFound(ExecutableElement invokingElement, ExecutableElement possibleMatchingElement, int indexOfLambdaInArgs) {

        if (possibleMatchingElement.getParameters().size() != invokingElement.getParameters().size()) {
            return false;
        }

        if (!doAllParamsMatchExcludingGivenIndex(invokingElement, possibleMatchingElement, indexOfLambdaInArgs)) {
            return false;
        }

        return doesLambdaElementMatchFound(possibleMatchingElement, indexOfLambdaInArgs);
    }
    
    private boolean doAllParamsMatchExcludingGivenIndex(ExecutableElement invokingElement, ExecutableElement possibleMatchingElement, int indexToIgnore) {
        
        List<TypeMirror> invokingParams = getTypesFromElements(invokingElement.getParameters());
        List<TypeMirror> possibleMatchParams = getTypesFromElements(possibleMatchingElement.getParameters());
        
        for (int i = 0; i < possibleMatchParams.size(); i++) {

            if (i == indexToIgnore) {
                continue;
            }

            TypeMirror foundType = invokingParams.get(i);
            TypeMirror expectedType = possibleMatchParams.get(i);
            if (!types.isAssignable(foundType, expectedType)) {
                return false;
            }
        }
        return true;
    }

    private ExecutableElement getFunctionalMethodFromElement(Element element) {

        Element classType = types.asElement(element.asType());

        //return null if classType is invalid, such as a primitive type
        if (classType == null) {
            return null;
        }

        ExecutableElement elementToReturn = null;
        int methodCounter = 0;
        for (Element e : classType.getEnclosedElements()) {
            if (e.getKind() == ElementKind.METHOD) {
                elementToReturn = (ExecutableElement) e;
                methodCounter++;
            }
        }

        //not a functional element, i.e. doesn't declare a single method
        if (methodCounter != 1) {
            return null;
        }

        return elementToReturn;
    }
    
    private boolean doesLambdaElementMatchFound(ExecutableElement possibleMatchingElement, int indexOfLambdaInArgs) {
        
        TreePath pathToLambdaMethod = new TreePath(pathToNewClassTree, lambdaMethodTree);
        ExecutableElement lambdaMethodElement = (ExecutableElement) getElementFromTreePath(pathToLambdaMethod);
        
        Element paramAtIndexOfLambda = possibleMatchingElement.getParameters().get(indexOfLambdaInArgs);
        ExecutableElement possibleLambdaMatch = getFunctionalMethodFromElement(paramAtIndexOfLambda);

        if (possibleLambdaMatch == null) {
            return false;
        }

        return areLambdaMethodSignaturesAmbiguous(lambdaMethodElement, possibleLambdaMatch);
    }

    private boolean areLambdaMethodSignaturesAmbiguous(ExecutableElement found, ExecutableElement expected) {

        if (!areReturnTypesEquivalent(found, expected)) {
            return false;
        }

        return areMethodParametersEquivalent(found, expected);
    }

    private boolean areReturnTypesEquivalent(ExecutableElement found, ExecutableElement expected) {

        TypeMirror foundReturnType = types.erasure(found.getReturnType());
        TypeMirror expectedReturnType = types.erasure(expected.getReturnType());
        return types.isAssignable(foundReturnType, expectedReturnType);
    }

    private boolean areMethodParametersEquivalent(ExecutableElement found, ExecutableElement expected) {

        if (found.getParameters().size() != expected.getParameters().size()) {
            return false;
        }

        for (int i = 0; i < found.getParameters().size(); i++) {
            TypeMirror foundParamType = types.erasure(found.getParameters().get(i).asType());
            TypeMirror expectedParamType = types.erasure(expected.getParameters().get(i).asType());
            if (!types.isAssignable(foundParamType, expectedParamType)) {
                return false;
            }
        }
        return true;
    }

    private void verifyTargetType() {

        TypeMirror expectedType = findExpectedType(pathToNewClassTree);
        if (!Utilities.isValidType(expectedType)) {
            foundErroneousTargetType = true;
            return;
        }

        TypeMirror erasedExpectedType = info.getTypes().erasure(expectedType);

        TreePath pathForClassIdentifier = new TreePath(pathToNewClassTree, newClassTree.getIdentifier());
        TypeMirror lambdaType = info.getTrees().getTypeMirror(pathForClassIdentifier);
        lambdaType = info.getTypes().erasure(lambdaType);

        foundAssignmentToSupertype = !info.getTypes().isSameType(erasedExpectedType, lambdaType);
        
        if (erasedExpectedType.getKind() == TypeKind.DECLARED) {
            TypeElement te = (TypeElement)((DeclaredType)erasedExpectedType).asElement();
            TypeMirror tt = (DeclaredType)te.asType();
            if (tt.getKind() == TypeKind.DECLARED && !((DeclaredType)tt).getTypeArguments().isEmpty()) {
                foundAssignmentToRawType =  info.getTypes().isSameType(erasedExpectedType, expectedType);
            }
        }
    }

    private TypeMirror findExpectedType(TreePath path) {
        int start = getSourceStartFromPath(path);

        path = path.getParentPath();
        while (path != null) {
            Tree currTree = path.getLeaf();

            if (isVariableTree(currTree)) {
                return info.getTrees().getTypeMirror(path);
            }

            if (isAssignmentTree(currTree)) {
                return info.getTrees().getTypeMirror(path);
            }

            if (isReturnTree(currTree)) {

                TreePath enclMethodPath = getEnclosingMethodPath(path);

                if (enclMethodPath != null) {
                    Tree returnTypeTree = ((MethodTree) enclMethodPath.getLeaf()).getReturnType();
                    return info.getTrees().getTypeMirror(new TreePath(enclMethodPath, returnTypeTree));
                }
            }

            if (isInvocationTree(currTree)) {

                ExecutableElement invokingElement = getElementFromInvokingTree(path);

                if (invokingElement == null) return null;
                
                int lambdaIndex = getLambdaIndexFromInvokingTree(currTree);
                if (lambdaIndex >= 0 && lambdaIndex < invokingElement.getParameters().size()) {
                    return invokingElement.getParameters().get(lambdaIndex).asType();
                }
            }

            if (getSourceStartFromTree(currTree) < start) {
                break;
            }

            path = path.getParentPath();
        }

        return null;
    }

    private boolean isVariableTree(Tree tree) {
        return tree.getKind() == Tree.Kind.VARIABLE
                && ((VariableTree) tree).getInitializer() != null;
    }

    private boolean isAssignmentTree(Tree tree) {
        return tree.getKind() == Tree.Kind.ASSIGNMENT;
    }

    private boolean isReturnTree(Tree tree) {
        return tree.getKind() == Tree.Kind.RETURN;
    }

    private boolean isInvocationTree(Tree tree) {
        Set<Tree.Kind> acceptableKinds = EnumSet.of(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
        return acceptableKinds.contains(tree.getKind());
    }

    private TreePath getEnclosingMethodPath(TreePath childPath) {
        TreePath parentPath = childPath;

        while (parentPath != null && parentPath.getLeaf().getKind() != Tree.Kind.METHOD) {
            parentPath = parentPath.getParentPath();
        }
        return parentPath;
    }

    private int getSourceStartFromPath(TreePath path) {
        return getSourceStartFromTree(path.getLeaf());
    }

    private int getSourceStartFromTree(Tree tree) {
        return (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), tree);
    }

    private List<TypeMirror> getTypesFromElements(List<? extends VariableElement> elements) {
        List<TypeMirror> elementTypes = new ArrayList<TypeMirror>();
        for (Element e : elements) {
            elementTypes.add(e.asType());
        }
        return elementTypes;
    }

    private Scope getScopeFromTree(TreePath path) {
        return info.getTrees().getScope(path);
    }
}
