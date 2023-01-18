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

package org.netbeans.modules.java.hints.jdk;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CodeStyle;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.support.CancellableTreePathScanner;
import org.netbeans.modules.java.hints.errors.Utilities;
import org.netbeans.modules.java.hints.introduce.TreeUtils;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.hints.ConstraintVariableType;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.JavaFix;
import org.netbeans.spi.java.hints.JavaFixUtilities;
import org.netbeans.spi.java.hints.MatcherUtilities;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerPatterns;
import org.openide.util.NbBundle.Messages;

@Hint(displayName="#DN_IteratorToFor", description="#DESC_IteratorToFor", category="rules15", suppressWarnings={"", "ForLoopReplaceableByForEach", "WhileLoopReplaceableByForEach"})
@Messages({
    "DN_IteratorToFor=Use JDK 5 for-loop",
    "DESC_IteratorToFor=Replaces simple uses of Iterator with a corresponding for-loop.",
    "ERR_IteratorToFor=Use of Iterator for simple loop",
    "ERR_IteratorToForArray=Use enhanced for loop to iterate over the array",
    "FIX_IteratorToFor=Convert to for-loop"
})
public class IteratorToFor {

    @TriggerPattern(value = "java.util.Iterator $it = $coll.iterator(); while ($it.hasNext()) {$type $elem = ($type) $it.next(); $rest$;}", 
            constraints = @ConstraintVariableType(variable = "$coll", type = "java.lang.Iterable"))
    public static ErrorDescription whileIdiom(HintContext ctx) {
        if (ctx.getInfo().getSourceVersion().compareTo(SourceVersion.RELEASE_5) < 0) {
            return null;
        }
        if (uses(ctx, ctx.getMultiVariables().get("$rest$"), ctx.getVariables().get("$it"))) {
            return null;
        }
        if (!iterable(ctx, ctx.getVariables().get("$coll"), ctx.getVariables().get("$type"))) {
            return null;
        }
        String colString = ctx.getVariables().containsKey("$coll") ? "$coll" : "this";
        Tree highlightTarget = ctx.getPath().getLeaf();
        TreePath elem = ctx.getVariables().get("$elem");
        if (elem.getParentPath() != null && elem.getParentPath().getLeaf().getKind() == Kind.BLOCK) elem = elem.getParentPath();
        if (elem.getParentPath() != null && elem.getParentPath().getLeaf().getKind() == Kind.WHILE_LOOP) highlightTarget = elem.getParentPath().getLeaf();
        return ErrorDescriptionFactory.forName(ctx, highlightTarget, Bundle.ERR_IteratorToFor(),
                JavaFixUtilities.rewriteFix(ctx, Bundle.FIX_IteratorToFor(), ctx.getPath(), "for ($type $elem : " + colString + ") {$rest$;}"));
    }

    @TriggerPatterns({
        @TriggerPattern(value = "for (java.util.Iterator $it = $coll.iterator(); $it.hasNext(); ) {$type $elem = ($type) $it.next(); $rest$;}",
                constraints = @ConstraintVariableType(variable = "$coll", type = "java.lang.Iterable")),
        @TriggerPattern(value = "for (java.util.Iterator<$typaram> $it = $coll.iterator(); $it.hasNext(); ) {$type $elem = ($type) $it.next(); $rest$;}",
                constraints = @ConstraintVariableType(variable = "$coll", type = "java.lang.Iterable")),
        @TriggerPattern(value = "for (java.util.Iterator<$typaram> $it = $coll.iterator(); $it.hasNext(); ) {$type $elem = $it.next(); $rest$;}",
                constraints = @ConstraintVariableType(variable = "$coll", type = "java.lang.Iterable"))
    })
    public static ErrorDescription forIdiom(HintContext ctx) {
        if (ctx.getInfo().getSourceVersion().compareTo(SourceVersion.RELEASE_5) < 0) {
            return null;
        }
        if (uses(ctx, ctx.getMultiVariables().get("$rest$"), ctx.getVariables().get("$it"))) {
            return null;
        }
        if (!iterable(ctx, ctx.getVariables().get("$coll"), ctx.getVariables().get("$type"))) {
            return null;
        }
        String colString = ctx.getVariables().containsKey("$coll") ? "$coll" : "this";
        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), Bundle.ERR_IteratorToFor(),
                JavaFixUtilities.rewriteFix(ctx, Bundle.FIX_IteratorToFor(), ctx.getPath(), "for ($type $elem : " + colString + ") {$rest$;}"));
    }
    
    private static class AccessAndVarVisitor extends CancellableTreePathScanner<Void, Void> {
        protected final HintContext ctx;
        protected final Set<String> definedVariables = new HashSet<>();
        
        private boolean insideClass;
        protected boolean unsuitable;
        protected final List<TreePath> toReplace = new ArrayList<>();
        
        public AccessAndVarVisitor(HintContext ctx) {
            this.ctx = ctx;
        }
        
        protected void unsuitable() {
            unsuitable = true;
            cancel();
        }
        
        @Override public Void visitIdentifier(IdentifierTree node, Void p) {
            if (MatcherUtilities.matches(ctx, getCurrentPath(), "$index")) { // NOI18N
                unsuitable();
                return null;
            }
            return super.visitIdentifier(node, p);
        }
        @Override public Void visitVariable(VariableTree node, Void p) {
            if (!insideClass) {
                definedVariables.add(node.getName().toString());
            }
            return super.visitVariable(node, p);
        }
        @Override public Void visitClass(ClassTree node, Void p) {
            boolean origInsideClass = insideClass;
            try {
                insideClass = true;
                return super.visitClass(node, p);
            } finally {
                insideClass = origInsideClass;
            }
        }
        @Override protected boolean isCanceled() {
            return ctx.isCanceled() || super.isCanceled();
        }
    }
    
    /**
     * Names of methods that can be safely called on Collection (List) while iterating through the contents.
     */
    static final Set<String> SAFE_COLLECTION_METHODS = new HashSet<String>();
    static {
        SAFE_COLLECTION_METHODS.add("size"); // NOI18N
        SAFE_COLLECTION_METHODS.add("isEmpty"); // NOI18N
        SAFE_COLLECTION_METHODS.add("contains"); // NOI18N
        SAFE_COLLECTION_METHODS.add("containsAll"); // NOI18N
        SAFE_COLLECTION_METHODS.add("iterator"); // NOI18N
        SAFE_COLLECTION_METHODS.add("listIterator"); // NOI18N
        SAFE_COLLECTION_METHODS.add("indexOf"); // NOI18N
        SAFE_COLLECTION_METHODS.add("lastIndexOf"); // NOI18N
        SAFE_COLLECTION_METHODS.add("subList"); // NOI18N
        SAFE_COLLECTION_METHODS.add("toArray"); // NOI18N
        SAFE_COLLECTION_METHODS.add("get"); // NOI18N
    }
    
    @TriggerPattern(value = "for (int $index = 0; $index < $col.size(); $index++) $statement;", constraints = @ConstraintVariableType(variable = "$col", type = "java.util.List"))
    public static ErrorDescription forListCollection(final HintContext ctx) {
        if (ctx.getInfo().getSourceVersion().compareTo(SourceVersion.RELEASE_5) < 0) {
            return null;
        }
        final boolean implicitThis = !ctx.getVariableNames().containsKey("$col"); // NOI18N
        AccessAndVarVisitor v = new AccessAndVarVisitor(ctx) {
            @Override public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
                ExpressionTree select = node.getMethodSelect();
                String methodName = null;
                if (implicitThis) {
                    if (select.getKind() == Tree.Kind.IDENTIFIER) {
                        methodName = ((IdentifierTree)select).getName().toString();
                    } else if (select.getKind() == Tree.Kind.MEMBER_SELECT) {
                        MemberSelectTree msel = (MemberSelectTree)select;
                        if (msel.getExpression().getKind() == Tree.Kind.IDENTIFIER && 
                            ((IdentifierTree)msel.getExpression()).getName().contentEquals("this")) { // NOI18N
                            // this.get is used
                            methodName = msel.getIdentifier().toString();
                        }
                    }
                } else if (select.getKind() == Tree.Kind.MEMBER_SELECT) {
                    MemberSelectTree msel = (MemberSelectTree)select;
                    if (MatcherUtilities.matches(ctx, new TreePath(new TreePath(getCurrentPath(), select), msel.getExpression()), "$col", true)) { // NOI18N
                        methodName = msel.getIdentifier().toString();
                    }
                }
                if (methodName != null) {
                    if ("get".equals(methodName)) {
                        if (MatcherUtilities.matches(ctx, getCurrentPath(), "$col.get($index)", true)) { // NOI18N
                            toReplace.add(getCurrentPath());
                            // skip the super visitor
                            return null;
                        } else {
                            unsuitable();
                        }
                    }
                    if (!SAFE_COLLECTION_METHODS.contains(methodName)) {
                        unsuitable();
                    }
                }
                return super.visitMethodInvocation(node, p);
            }
        };
        v.scan(ctx.getVariables().get("$statement"), null); // NOI18N
        if (v.unsuitable) return null;
        
        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), Bundle.ERR_IteratorToForArray(), 
                new ReplaceIndexedForEachLoop(ctx.getInfo(), ctx.getPath(), ctx.getVariables().get("$col"),  // NOI18N
                v.toReplace, v.definedVariables).toEditorFix());
    }
    
    @TriggerPattern(value="for (int $index = 0; $index < $arr.length; $index++) $statement;", constraints=@ConstraintVariableType(variable="$arr", type="Object[]"))
    public static ErrorDescription forIndexedArray(final HintContext ctx) {
        if (ctx.getInfo().getSourceVersion().compareTo(SourceVersion.RELEASE_5) < 0) {
            return null;
        }
        AccessAndVarVisitor v = new AccessAndVarVisitor(ctx) {
            @Override public Void visitArrayAccess(ArrayAccessTree node, Void p) {
            TreePath path = getCurrentPath();
                if (MatcherUtilities.matches(ctx, path, "$arr[$index]")) { // NOI18N
                    if (path.getParentPath() != null) {
                        if (   path.getParentPath().getLeaf().getKind() == Kind.ASSIGNMENT
                            && ((AssignmentTree) path.getParentPath().getLeaf()).getVariable() == node) {
                            unsuitable();
                        }
                        if (CompoundAssignmentTree.class.isAssignableFrom(path.getParentPath().getLeaf().getKind().asInterface())
                            && ((CompoundAssignmentTree) path.getParentPath().getLeaf()).getVariable() == node) {
                            unsuitable();
                        }
                    }
                    toReplace.add(path);
                    return null;
                }
                return super.visitArrayAccess(node, p);
            }
        };
        v.scan(ctx.getVariables().get("$statement"), null); // NOI18N
        if (v.unsuitable) return null;
        
        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), Bundle.ERR_IteratorToForArray(), 
                new ReplaceIndexedForEachLoop(ctx.getInfo(), ctx.getPath(), ctx.getVariables().get("$arr"), 
                v.toReplace, v.definedVariables).toEditorFix());
    }

    // adapted from org.netbeans.modules.java.hints.declarative.conditionapi.Matcher.referencedIn
    private static boolean uses(final HintContext ctx, Collection<? extends TreePath> statements, TreePath var) {
        final Element e = ctx.getInfo().getTrees().getElement(var);
        if (e == null) {
            return false;
        }
        for (TreePath tp : statements) {
            boolean occurs = Boolean.TRUE.equals(new TreePathScanner<Boolean, Void>() {
                @Override public Boolean scan(Tree tree, Void p) {
                    if (tree == null) {
                        return false;
                    }
                    TreePath currentPath = new TreePath(getCurrentPath(), tree);
                    Element currentElement = ctx.getInfo().getTrees().getElement(currentPath);
                    if (e.equals(currentElement)) {
                        return true;
                    }
                    return super.scan(tree, p);
                }
                @Override public Boolean reduce(Boolean r1, Boolean r2) {
                    if (r1 == null) {
                        return r2;
                    }
                    if (r2 == null) {
                        return r1;
                    }
                    return r1 || r2;
                }
            }.scan(tp, null));
            if (occurs) {
                return true;
            }
        }
        return false;
    }

    private static boolean iterable(HintContext ctx, TreePath collection, TreePath type) {
        TypeMirror collectionType = null;
        
        if (collection != null) {
            collectionType = ctx.getInfo().getTrees().getTypeMirror(collection);
        } else {
            // the collection may be the implicit 'this'
            TreePath enclClass = TreeUtils.findClass(type);
            if (enclClass != null) {
                collectionType = ctx.getInfo().getTrees().getTypeMirror(enclClass);
            }
        }
        if (collectionType == null) {
            return false;
        }
        TypeElement iterable = ctx.getInfo().getElements().getTypeElement("java.lang.Iterable");
        if (!Utilities.isValidType(collectionType) || iterable == null) return false;
        TypeMirror typeMirror = ctx.getInfo().getTrees().getTypeMirror(type);
        if (typeMirror != null && typeMirror.getKind().isPrimitive()) {
            typeMirror = ctx.getInfo().getTypes().boxedClass((PrimitiveType)typeMirror).asType();
        }
        TypeMirror iterableType = ctx.getInfo().getTypes().getDeclaredType(iterable, ctx.getInfo().getTypes().getWildcardType(typeMirror, null));
        TypeMirror bogusIterableType = ctx.getInfo().getTypes().getDeclaredType(iterable, ctx.getInfo().getTypes().getNullType());
        return ctx.getInfo().getTypes().isAssignable(collectionType, iterableType) && !ctx.getInfo().getTypes().isAssignable(collectionType, bogusIterableType);
    }
    
    private static final class ReplaceIndexedForEachLoop extends JavaFix {

        private final TreePathHandle arrHandle;
        private final List<TreePathHandle> toReplace;
        private final Set<String> definedVariables;
        
        public ReplaceIndexedForEachLoop(CompilationInfo info, TreePath tp, TreePath arr, List<TreePath> toReplace, Set<String> definedVariables) {
            super(info, tp);
            this.arrHandle = arr == null ? null : TreePathHandle.create(arr, info);
            this.toReplace = new ArrayList<>();
            
            for (TreePath tr : toReplace) {
                this.toReplace.add(TreePathHandle.create(tr, info));
            }
            this.definedVariables = definedVariables;
        }

        @Override
        protected String getText() {
            return Bundle.FIX_IteratorToFor();
        }

        private String assignedToVariable(TransformationContext ctx, TypeMirror variableType, TreePath forStatement, List<TreePath> toReplace) {
            if (forStatement.getLeaf().getKind() != Kind.BLOCK) return null;

            BlockTree block = (BlockTree) forStatement.getLeaf();

            if (block.getStatements().isEmpty()) return null;

            StatementTree first = block.getStatements().get(0);

            if (first.getKind() != Kind.VARIABLE) return null;

            VariableTree var = (VariableTree) first;
            TypeMirror varType = ctx.getWorkingCopy().getTrees().getTypeMirror(new TreePath(forStatement, var.getType()));

            if (varType == null || !ctx.getWorkingCopy().getTypes().isSameType(variableType, varType)) return null;

            for (TreePath tp : toReplace) {
                if (tp.getLeaf() == var.getInitializer()) return var.getName().toString();
            }

            return null;
        }

        @Override
        protected void performRewrite(TransformationContext ctx) throws Exception {
            Tree loop = GeneratorUtilities.get(ctx.getWorkingCopy()).importComments(ctx.getPath().getLeaf(), ctx.getPath().getCompilationUnit());
            TreeMaker make = ctx.getWorkingCopy().getTreeMaker();
            TypeMirror arrType = null;
            TypeMirror variableType = null;
            String treeName;
            Tree colArrTree = null;
            
            if (arrHandle != null) {
                TreePath arr = arrHandle.resolve(ctx.getWorkingCopy());
                if (arr == null) {
                    return;
                }
                colArrTree = arr.getLeaf();
                treeName = org.netbeans.modules.editor.java.Utilities.varNameSuggestion(arr);
                arrType = ctx.getWorkingCopy().getTrees().getTypeMirror(arr);
                if (!Utilities.isValidType(arrType)) {
                    // FIXME - report
                    return;
                }
                if (arrType.getKind() == TypeKind.ARRAY) {
                    variableType = ((ArrayType) arrType).getComponentType();
                }
            } else {
                // this branch should be only valid for 'collection' case and only if the 
                // collection is "this".
                // take the nearest class enclosing the ctx.getPath();
                TreePath enclClass = TreeUtils.findClass(ctx.getPath());
                if (enclClass == null) {
                    // FIXME - report
                    return;
                }
                // type of the enclosing class == this
                arrType = ctx.getWorkingCopy().getTrees().getTypeMirror(enclClass);
                treeName = "my"; // FIXME - I18N
                colArrTree = make.Identifier("this"); // NOI18N
            }
            
            if (arrType.getKind() != TypeKind.ARRAY) {
                //TODO: can happen?
                TypeElement listEl = ctx.getWorkingCopy().getElements().getTypeElement("java.util.Collection"); // NOI18N
                if (listEl == null) {
                    return;
                }
                TypeMirror listType = ctx.getWorkingCopy().getTypes().erasure(listEl.asType());
                if (listType.getKind() == TypeKind.ERROR) {
                    return;
                }
                if (!ctx.getWorkingCopy().getTypes().isAssignable(arrType, listType)) {
                    return;
                }
                Element addEl = ctx.getWorkingCopy().getElementUtilities().findElement("java.util.Collection.add(java.lang.Object)"); // NOI18N
                assert addEl != null;
                TypeMirror addType = ctx.getWorkingCopy().getTypes().asMemberOf(((DeclaredType)arrType), addEl);
                variableType = ((ExecutableType)addType).getParameterTypes().get(0);
            } else if (variableType == null) {
                // should never happen
                return;
            }
             
            StatementTree statement = ((ForLoopTree) ctx.getPath().getLeaf()).getStatement();
            List<TreePath> convertedToReplace = new ArrayList<>();

            for (TreePathHandle tr : toReplace) {
                TreePath tp = tr.resolve(ctx.getWorkingCopy());

                convertedToReplace.add(tp);
            }

            String variableName = assignedToVariable(ctx, variableType, new TreePath(ctx.getPath(), statement), convertedToReplace);
            EnhancedForLoopTree newLoop;

            if (variableName != null) {
                BlockTree block = (BlockTree) statement;
                newLoop = make.EnhancedForLoop(make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), variableName, make.Type(variableType), null), (ExpressionTree) colArrTree, make.Block(block.getStatements().subList(1, block.getStatements().size()), false));
            } else {
                variableName = treeName;

                if (variableName != null && variableName.endsWith("s")) variableName = variableName.substring(0, variableName.length() - 1);  // FIXME - I18N
                if (variableName == null || variableName.isEmpty()) variableName = "item";  // FIXME - I18N

                CodeStyle cs = CodeStyle.getDefault(ctx.getWorkingCopy().getFileObject());

                if (variableName.equals(treeName) && cs.getLocalVarNamePrefix() == null && cs.getLocalVarNameSuffix() == null) {
                    if(Character.isAlphabetic(variableName.charAt(0))) {
                        StringBuilder nameSb = new StringBuilder(variableName);
                        nameSb.setCharAt(0, Character.toUpperCase(nameSb.charAt(0)));
                        nameSb.indexOf("a"); 
                        variableName = nameSb.toString();
                    }
                }

                variableName = Utilities.makeNameUnique(ctx.getWorkingCopy(), ctx.getWorkingCopy().getTrees().getScope(ctx.getPath()), variableName, definedVariables, cs.getLocalVarNamePrefix(), cs.getLocalVarNameSuffix());

                newLoop = make.EnhancedForLoop(make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), variableName, make.Type(variableType), null), 
                        (ExpressionTree) colArrTree, statement);
            }
            
            ctx.getWorkingCopy().rewrite(loop, newLoop);
            
            for (TreePathHandle tr : toReplace) {
                TreePath tp = tr.resolve(ctx.getWorkingCopy());
                
                ctx.getWorkingCopy().rewrite(tp.getLeaf(), make.Identifier(variableName));
            }
        }
        
    }

}
