/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints.suggestions;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LambdaExpressionTree.BodyKind;

import static com.sun.source.tree.LambdaExpressionTree.BodyKind.STATEMENT;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.JavaFix;
import org.netbeans.spi.java.hints.JavaFixUtilities;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author lahvac
 */
public class Lambda {
    
    @Hint(displayName="#DN_lambda2Class", description="#DESC_lambda2Class", category="suggestions", hintKind=Hint.Kind.ACTION)
    @Messages({
        "DN_lambda2Class=Convert Lambda Expression to Anonymous Innerclass",
        "DESC_lambda2Class=Converts lambda expressions to anonymous inner classes",
        "ERR_lambda2Class=Anonymous class can be used"
    })
    @TriggerTreeKind(Kind.LAMBDA_EXPRESSION)
    public static ErrorDescription lambda2Class(HintContext ctx) {
        TypeMirror samType = ctx.getInfo().getTrees().getTypeMirror(ctx.getPath());
        
        if (samType == null || samType.getKind() != TypeKind.DECLARED) {
            return null;
        }
        
        return ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), Bundle.ERR_lambda2Class(), new Lambda2Anonymous(ctx.getInfo(), ctx.getPath()).toEditorFix());
    }
    
    @Hint(displayName="#DN_lambda2MemberReference", description="#DESC_lambda2MemberReference", category="suggestions", hintKind=Hint.Kind.ACTION)
    @Messages({
        "DN_lambda2MemberReference=Convert Lambda Expression to Member Reference",
        "DESC_lambda2MemberReference=Converts lambda expressions to member references",
        "ERR_lambda2MemberReference=Member reference can be used",
        "FIX_lambda2MemberReference=Use member reference"
    })
    @TriggerTreeKind(Kind.LAMBDA_EXPRESSION)
    public static ErrorDescription lambda2MemberReference(HintContext ctx) {
        TypeMirror samType = ctx.getInfo().getTrees().getTypeMirror(ctx.getPath());        
        if (samType == null || samType.getKind() != TypeKind.DECLARED) {
            return null;
        }

        LambdaExpressionTree lambda = (LambdaExpressionTree) ctx.getPath().getLeaf();
        Tree tree = lambda.getBody();
        if (tree == null) {
            return null;
        }
        if (tree.getKind() == Tree.Kind.BLOCK) {
            if (((BlockTree)tree).getStatements().size() == 1) {
                tree = ((BlockTree)tree).getStatements().get(0);
                if (tree.getKind() == Tree.Kind.EXPRESSION_STATEMENT) {
                    tree = ((ExpressionStatementTree)tree).getExpression();
                } else if (tree.getKind() == Tree.Kind.RETURN) {
                    tree = ((ReturnTree)tree).getExpression();
                } else {
                    return null;
                }
                if (tree == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        if (tree.getKind() != Tree.Kind.METHOD_INVOCATION) {
            return null;
        }

        boolean check = true;
        Iterator<? extends VariableTree> paramsIt = lambda.getParameters().iterator();
        ExpressionTree methodSelect = ((MethodInvocationTree)tree).getMethodSelect();
        if (paramsIt.hasNext() && methodSelect.getKind() == Tree.Kind.MEMBER_SELECT) {
            ExpressionTree expr = ((MemberSelectTree) methodSelect).getExpression();
            if (expr.getKind() == Tree.Kind.IDENTIFIER) {
                if (!((IdentifierTree)expr).getName().contentEquals(paramsIt.next().getName())) {
                    paramsIt = lambda.getParameters().iterator();
                }
            }
        }
        Iterator<? extends ExpressionTree> argsIt = ((MethodInvocationTree)tree).getArguments().iterator();
        while (check && argsIt.hasNext() && paramsIt.hasNext()) {
            ExpressionTree arg = argsIt.next();
            if (arg.getKind() != Tree.Kind.IDENTIFIER || !paramsIt.next().getName().contentEquals(((IdentifierTree)arg).getName())) {
                check = false;
            }
        }
        if (!check || paramsIt.hasNext() || argsIt.hasNext()) {
            return null;
        }

        return ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), Bundle.ERR_lambda2MemberReference(), new Lambda2MemberReference(ctx.getInfo(), ctx.getPath()).toEditorFix());
    }
    
    @Hint(displayName="#DN_expression2Return", description="#DESC_expression2Return", category="suggestions", hintKind=Hint.Kind.ACTION)
    @Messages({
        "DN_expression2Return=Convert Lambda Body to Use a Block",
        "DESC_expression2Return=Converts lambda bodies to use blocks rather than expressions",
        "ERR_expression2Return=",
        "FIX_expression2Return=Use block as the lambda's body"
    })
    @TriggerPattern("($args$) -> $lambdaExpression")
    public static ErrorDescription expression2Return(HintContext ctx) {
        if (((LambdaExpressionTree) ctx.getPath().getLeaf()).getBodyKind() != BodyKind.EXPRESSION) {
            return null;
        }
        
        TypeMirror lambdaExpressionType = ctx.getInfo().getTrees().getTypeMirror(ctx.getVariables().get("$lambdaExpression"));
        String target =   lambdaExpressionType == null || lambdaExpressionType.getKind() != TypeKind.VOID
                        ? "($args$) -> { return $lambdaExpression; }"
                        : "($args$) -> { $lambdaExpression; }";
        
        return ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), Bundle.ERR_expression2Return(), JavaFixUtilities.rewriteFix(ctx, Bundle.FIX_expression2Return(), ctx.getPath(), target));
    }
    
    @Hint(displayName="#DN_memberReference2Lambda", description="#DESC_memberReference2Lambda", category="suggestions", hintKind=Hint.Kind.ACTION)
    @Messages({
        "DN_memberReference2Lambda=Convert Member Reference to Lambda Expression",
        "DESC_memberReference2Lambda=Converts member references to lambda expressions",
        "ERR_memberReference2Lambda=",
        "FIX_memberReference2Lambda=Use lambda expression"
    })
    @TriggerTreeKind(Kind.MEMBER_REFERENCE)
    public static ErrorDescription reference2Lambda(HintContext ctx) {
        Element refered = ctx.getInfo().getTrees().getElement(ctx.getPath());
        
        if (refered == null || refered.getKind() != ElementKind.METHOD) {
            return null;//XXX: constructors!
        }        
        return ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), Bundle.ERR_memberReference2Lambda(), new MemberReference2Lambda(ctx.getInfo(), ctx.getPath()).toEditorFix());
    }
    
    @Hint(displayName="#DN_addExplicitLambdaParameters", description="#DESC_addExplicitLambdaParameters", category="suggestions", hintKind=Hint.Kind.ACTION)
    @Messages({
        "DN_addExplicitLambdaParameters=Convert Lambda to Use Explicit Parameter Types",
        "DESC_addExplicitLambdaParameters=Converts lambdas to use explicit parameter types",
        "ERR_addExplicitLambdaParameters=",
        "FIX_addExplicitLambdaParameters=Use explicit parameter types"
    })
    @TriggerTreeKind(Kind.LAMBDA_EXPRESSION)
    public static ErrorDescription explicitParameterTypes(HintContext ctx) {
        LambdaExpressionTree let = (LambdaExpressionTree) ctx.getPath().getLeaf();
        boolean hasSyntheticParameterName = false;
        
        for (VariableTree var : let.getParameters()) {
            hasSyntheticParameterName |= var.getType() == null || ctx.getInfo().getTreeUtilities().isSynthetic(TreePath.getPath(ctx.getPath(), var.getType()));
        }
        
        if (!hasSyntheticParameterName) {
            return null;
        }
        
        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), Bundle.ERR_addExplicitLambdaParameters(), new AddExplicitLambdaParameterTypes(ctx.getInfo(), ctx.getPath()).toEditorFix());
    }
    
    private static ExecutableElement findAbstractMethod(CompilationInfo info, TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        
        TypeElement clazz = (TypeElement) ((DeclaredType) type).asElement();
        
        if (!clazz.getKind().isInterface()) {
            return null;
        }
        
        for (ExecutableElement ee : ElementFilter.methodsIn(clazz.getEnclosedElements())) {
            if (ee.getModifiers().contains(Modifier.ABSTRACT)) {
                return ee;
            }
        }
        
        for (TypeMirror tm : info.getTypes().directSupertypes(type)) {
            ExecutableElement ee = findAbstractMethod(info, tm);
            
            if (ee != null) {
                return ee;
            }
        }
        
        return null;
    }
    
    private static final class Lambda2Anonymous extends JavaFix {

        public Lambda2Anonymous(CompilationInfo info, TreePath tp) {
            super(info, tp);
        }

        @Override
        @Messages("FIX_lambda2Class=Use anonymous inner class")
        protected String getText() {
            return Bundle.FIX_lambda2Class();
        }
        
        private static TypeMirror avoidIntersectionType(CompilationInfo copy, TypeMirror org) {
            if (org.getKind() == TypeKind.INTERSECTION) {
                Element objEl = copy.getElements().getTypeElement("java.lang.Object"); // NOI18N
                if (objEl == null) {
                    // TODO: report
                    return org;
                }
                return objEl.asType();
            } else {
                return org;
            }
        }

        @Override
        protected void performRewrite(TransformationContext ctx) throws Exception {
            final WorkingCopy copy = ctx.getWorkingCopy();
            LambdaExpressionTree lambda = (LambdaExpressionTree) ctx.getPath().getLeaf();
            TypeMirror samType = copy.getTrees().getTypeMirror(ctx.getPath());
            
            if (samType == null || samType.getKind() != TypeKind.DECLARED) {
                // FIXME: report
                return ;
            }
            
            ExecutableType descriptorType = copy.getTypeUtilities().getDescriptorType((DeclaredType) samType);
            ExecutableElement abstractMethod = findAbstractMethod(copy, samType);
            TypeElement samTypeElement = (TypeElement) ((DeclaredType) samType).asElement();
            List<VariableTree> methodParams = new ArrayList<>();
            Iterator<? extends TypeMirror> resolvedParamTypes = descriptorType.getParameterTypes().iterator();
            Iterator<? extends VariableTree> actualParams = lambda.getParameters().iterator();
            final TreeMaker make = copy.getTreeMaker();
            
            while (resolvedParamTypes.hasNext() && actualParams.hasNext()) {
                VariableTree p = actualParams.next();
                TypeMirror resolvedType = resolvedParamTypes.next();
                
                //XXX: should handle anonymous lambda parameters ('_')
                if (p.getType() == null || copy.getTreeUtilities().isSynthetic(new TreePath(ctx.getPath(), p.getType()))) {
                    methodParams.add(make.Variable(p.getModifiers(), p.getName(), make.Type(SourceUtils.resolveCapturedType(copy, resolvedType)), null));
                } else {
                    methodParams.add(p);
                }
            }
            
            BlockTree newMethodBody;
            switch (lambda.getBodyKind()) {
                case STATEMENT:
                    newMethodBody = (BlockTree) lambda.getBody();
                    break;
                case EXPRESSION:
                    StatementTree mainStatement;
                    if (descriptorType.getReturnType() == null || descriptorType.getReturnType().getKind() != TypeKind.VOID) {
                        mainStatement = make.Return((ExpressionTree) lambda.getBody());
                    } else {
                        mainStatement = make.ExpressionStatement((ExpressionTree) lambda.getBody());
                    }
                    newMethodBody = make.Block(Collections.singletonList(mainStatement), false);
                    break;
                default:
                    throw new IllegalStateException();
            }
            
            List<ExpressionTree> thrownTypes = new ArrayList<>(abstractMethod.getThrownTypes().size());
            for (TypeMirror tm : abstractMethod.getThrownTypes()) {
                // ErrorTypes are somehow handled, too, by make.Type
                thrownTypes.add((ExpressionTree)make.Type(tm));
            }
            ModifiersTree mt = make.Modifiers(EnumSet.of(Modifier.PUBLIC));
            // should I ever test for >= source 5, if there's a Lambda :) in the source already ?
//            if (copy.getSourceVersion().compareTo(SourceVersion.RELEASE_5) >= 0) {
                boolean generate = copy.getElements().getTypeElement("java.lang.Override") != null;

                if (generate) {
                   mt = make.addModifiersAnnotation(
                           mt, make.Annotation(make.Identifier("Override"), Collections.<ExpressionTree>emptyList()));
                }
//            }
    
            TypeMirror retType = avoidIntersectionType(copy, descriptorType.getReturnType());
            
            MethodTree newMethod = make.Method(mt,
                                               abstractMethod.getSimpleName(),
                                               make.Type(retType),
                                               Collections.<TypeParameterTree>emptyList(), //XXX: type parameters
                                               methodParams,
                                               // TODO: possibly filter out those exceptions, which are handled/never thrown 
                                               // from the body
                                               thrownTypes,
                                               newMethodBody,
                                               null);
            ClassTree innerClass = make.Class(make.Modifiers(EnumSet.noneOf(Modifier.class)),
                                              samTypeElement.getSimpleName(),
                                              Collections.<TypeParameterTree>emptyList(),
                                              null,
                                              Collections.<Tree>emptyList(),
                                              Collections.singletonList(newMethod));
            ExpressionTree targetTypeTree;
            
            if (((DeclaredType) samType).getTypeArguments().isEmpty()) {
                targetTypeTree = make.QualIdent(samTypeElement);
            } else {
                List<Tree> typeArguments = new ArrayList<>();
                for (TypeMirror ta : ((DeclaredType) samType).getTypeArguments()) {
                    typeArguments.add(make.Type(
                            avoidIntersectionType(copy, SourceUtils.resolveCapturedType(copy, ta))));
                }
                targetTypeTree = (ExpressionTree) make.ParameterizedType(make.QualIdent(samTypeElement), typeArguments);
            }
            
            NewClassTree newClass = make.NewClass(null, Collections.<ExpressionTree>emptyList(), targetTypeTree, Collections.<ExpressionTree>emptyList(), innerClass);
            
            copy.rewrite(ctx.getPath().getLeaf(), newClass);
            
            TreePath clazz = ctx.getPath();
            
            while (clazz != null && !TreeUtilities.CLASS_TREE_KINDS.contains(clazz.getLeaf().getKind())) {
                clazz = clazz.getParentPath();
            }
            
            if (clazz != null) {
                final Name outterClassName = ((ClassTree) clazz.getLeaf()).getSimpleName();
                
                new TreeScanner<Void, Void>() {
                    @Override public Void visitIdentifier(IdentifierTree node, Void p) {
                        if (node.getName().contentEquals("this")) {
                            copy.rewrite(node, make.MemberSelect(make.Identifier(outterClassName), "this"));
                        }
                        return super.visitIdentifier(node, p);
                    }
                }.scan(lambda.getBody(), null);
            }
        }        
    }

    private static final class Lambda2MemberReference extends JavaFix {

        public Lambda2MemberReference(CompilationInfo info, TreePath tp) {
            super(info, tp);
        }

        @Override
        protected String getText() {
            return Bundle.FIX_lambda2MemberReference();
        }

        @Override
        protected void performRewrite(TransformationContext ctx) throws Exception {
            final WorkingCopy copy = ctx.getWorkingCopy();
            TypeMirror samType = copy.getTrees().getTypeMirror(ctx.getPath());
            if (samType == null || samType.getKind() != TypeKind.DECLARED) {
                // FIXME: report
                return ;
            }

            LambdaExpressionTree lambda = (LambdaExpressionTree) ctx.getPath().getLeaf();
            Tree tree = lambda.getBody();
            if (tree.getKind() == Tree.Kind.BLOCK) {
                if (((BlockTree)tree).getStatements().size() == 1) {
                    tree = ((BlockTree)tree).getStatements().get(0);
                    if (tree.getKind() == Tree.Kind.EXPRESSION_STATEMENT) {
                        tree = ((ExpressionStatementTree)tree).getExpression();
                    } else if (tree.getKind() == Tree.Kind.RETURN) {
                        tree = ((ReturnTree)tree).getExpression();
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }

            if (tree.getKind() != Tree.Kind.METHOD_INVOCATION) {
                return;
            }

            ExpressionTree ms = ((MethodInvocationTree)tree).getMethodSelect();
            Name name = null;
            ExpressionTree expr = null;
            TreeMaker make = copy.getTreeMaker();
            if (ms.getKind() == Tree.Kind.IDENTIFIER) {
                name = ((IdentifierTree)ms).getName();
                expr = make.Identifier("this"); //NOI18N
            } else if (ms.getKind() == Tree.Kind.MEMBER_SELECT) {
                name = ((MemberSelectTree)ms).getIdentifier();
                if (lambda.getParameters().size() == ((MethodInvocationTree)tree).getArguments().size()) {
                    expr = ((MemberSelectTree)ms).getExpression();
                } else {
                    Element e = copy.getTrees().getElement(new TreePath(ctx.getPath(), ms));
                    if (e != null && e.getKind() == ElementKind.METHOD) {
                        expr = make.Identifier(e.getEnclosingElement());
                    }
                }
            }
            if (name == null || expr == null) {
                return;
            }

            MemberReferenceTree referenceTree = make.MemberReference(MemberReferenceTree.ReferenceMode.INVOKE, expr, name, Collections.<ExpressionTree>emptyList());
            copy.rewrite(lambda, referenceTree);
        }
    }

    private static final class MemberReference2Lambda extends JavaFix {

        public MemberReference2Lambda(CompilationInfo info, TreePath tp) {
            super(info, tp);
        }

        @Override
        protected String getText() {
            return Bundle.FIX_memberReference2Lambda();
        }

        @Override
        protected void performRewrite(TransformationContext ctx) throws Exception {
            TreePath reference = ctx.getPath();
            Element refered = ctx.getWorkingCopy().getTrees().getElement(reference);

            if (refered == null || refered.getKind() != ElementKind.METHOD) {
                //TODO: log
                return ;
            }

            MemberReferenceTree mrt = (MemberReferenceTree) ctx.getPath().getLeaf();

            Element on = ctx.getWorkingCopy().getTrees().getElement(new TreePath(ctx.getPath(), mrt.getQualifierExpression()));
            ExpressionTree reciever = mrt.getQualifierExpression();
            List<VariableTree> formals = new ArrayList<>();
            List<IdentifierTree> actuals = new ArrayList<>();
            Scope scope = ctx.getWorkingCopy().getTrees().getScope(reference);
            Set<String> usedNames = new HashSet<>();
            TreeMaker make = ctx.getWorkingCopy().getTreeMaker();
            
            if (on != null && (on.getKind().isClass() || on.getKind().isInterface()) && !refered.getModifiers().contains(Modifier.STATIC)) {
                //static reference to instance method:
                String name = org.netbeans.modules.java.hints.errors.Utilities.getName(on.asType());
                name = org.netbeans.modules.java.hints.errors.Utilities.makeNameUnique(ctx.getWorkingCopy(), scope, name);
                formals.add(make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), name, null, null));
                reciever = make.Identifier(name);
                usedNames.add(name);
            }
            
            for (VariableElement param : ((ExecutableElement) refered).getParameters()) {
                String name = org.netbeans.modules.java.hints.errors.Utilities.makeNameUnique(ctx.getWorkingCopy(), scope, param.getSimpleName().toString(), usedNames, null, null);                
                formals.add(make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), name, null, null));
                actuals.add(make.Identifier(name));
            }
            
            LambdaExpressionTree lambda = make.LambdaExpression(formals, make.MethodInvocation(Collections.<ExpressionTree>emptyList(), make.MemberSelect(reciever, mrt.getName()), actuals));
            
            ctx.getWorkingCopy().rewrite(mrt, lambda);
        }
    }
    
    private static final class AddExplicitLambdaParameterTypes extends JavaFix {

        public AddExplicitLambdaParameterTypes(CompilationInfo info, TreePath tp) {
            super(info, tp);
        }

        @Override
        protected String getText() {
            return Bundle.FIX_addExplicitLambdaParameters();
        }

        @Override
        protected void performRewrite(TransformationContext ctx) throws Exception {
            LambdaExpressionTree let = (LambdaExpressionTree) ctx.getPath().getLeaf();

            for (VariableTree var : let.getParameters()) {
                TreePath typePath = TreePath.getPath(ctx.getPath(), var.getType());
                if (ctx.getWorkingCopy().getTreeUtilities().isSynthetic(typePath)) {
                    Tree imported = ctx.getWorkingCopy().getTreeMaker().Type(ctx.getWorkingCopy().getTrees().getTypeMirror(typePath));
                    ctx.getWorkingCopy().rewrite(var.getType(), imported);
                }
            }
        }
    }
}
