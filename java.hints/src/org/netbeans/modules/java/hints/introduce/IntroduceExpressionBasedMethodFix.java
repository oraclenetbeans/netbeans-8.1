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
package org.netbeans.modules.java.hints.introduce;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.swing.JButton;
import javax.swing.text.Document;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.api.java.source.TypeMirrorHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.java.source.matching.Matcher;
import org.netbeans.api.java.source.matching.Occurrence;
import org.netbeans.api.java.source.matching.Pattern;
import org.netbeans.modules.java.hints.errors.Utilities;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.Union2;

/**
 * Refactored from IntroduceFix originally by lahvac
 *
 * @author sdedic
 */
final class IntroduceExpressionBasedMethodFix extends IntroduceFixBase implements Fix {

    static List<TargetDescription> computeViableTargets(final CompilationInfo info, TreePath commonParent, Iterable<? extends Tree> toInclude, Iterable<? extends Occurrence> duplicates, 
            AtomicBoolean cancel, AtomicBoolean allIfaces) {
        List<TargetDescription> targets = new ArrayList<>();
        TreePath acceptableParent = commonParent;
        boolean allInterfaces = true;
        // for each enclosing class-like Tree (not interface !) define a target. Check if all duplicates fall inside the target
        // and if so, mark it as 'duplicatesAcceptable', because all duplicate sites sees that class as an enclosing scope.
        while (acceptableParent != null) {
            if (cancel.get()) {
                return null;
            }
            if (TreeUtilities.CLASS_TREE_KINDS.contains(acceptableParent.getLeaf().getKind())) {
                boolean duplicatesAcceptable = true;
                DUPLICATES_ACCEPTABLE:
                for (Occurrence duplicate : duplicates) {
                    for (Tree t : duplicate.getOccurrenceRoot()) {
                        if (t == acceptableParent.getLeaf()) {
                            continue DUPLICATES_ACCEPTABLE;
                        }
                    }
                    duplicatesAcceptable = false;
                    break;
                }
                Element el = info.getTrees().getElement(acceptableParent);
                if (el != null) {
                    boolean isIface = el.getKind().isInterface();
                    if (el.getKind().isClass() || isIface) {
                        targets.add(TargetDescription.create(info, (TypeElement) el, duplicatesAcceptable, isIface));
                        allInterfaces &= isIface;
                    }
                }
            }
            acceptableParent = acceptableParent.getParentPath();
        }
        // sort the targets from in top-down order 
        Collections.reverse(targets);
        InstanceRefFinder finder = new InstanceRefFinder(info, commonParent);
        for (Tree include : toInclude) {
            finder.process(new TreePath(commonParent, include));
        }
        Set<Element> usedMembers = finder.getUsedMembers();
        Set<? extends Element> requiredEnclosing = finder.getRequiredInstances();
        // starting with the outermost scope, go to first enclosing class that sees all the usedMembers
        for (Iterator<TargetDescription> it = targets.iterator(); it.hasNext() && (!usedMembers.isEmpty() || !requiredEnclosing.isEmpty());) {
            TargetDescription td = it.next();
            TypeElement type = td.type.resolve(info);
            if (type == null) {
                it.remove();
                continue;
            }
            usedMembers.removeAll(info.getElements().getAllMembers(type));
            requiredEnclosing.remove(type);
            if (!usedMembers.isEmpty() || !requiredEnclosing.isEmpty()) {
                it.remove();
                continue;
            } 
            allInterfaces &= type.getKind() == ElementKind.INTERFACE;
        }
        if (targets.isEmpty()) {
            TreePath clazz = TreeUtils.findClass(commonParent);
            Element el = info.getTrees().getElement(clazz);
            if (el == null || (!el.getKind().isClass() && !el.getKind().isInterface())) {
                return null;
            }
            allInterfaces = el.getKind().isInterface();
            targets.add(TargetDescription.create(info, (TypeElement) el, true, el.getKind().isInterface()));
        }
        allIfaces.set(allInterfaces);
        return targets;
    }
    
    private final List<TreePathHandle> parameters;
    private final Set<TypeMirrorHandle> thrownTypes;
    private final List<TreePathHandle> typeVars;
    private final Map<TargetDescription, Set<String>> targets;

    public IntroduceExpressionBasedMethodFix(JavaSource js, TreePathHandle expression, List<TreePathHandle> parameters, Set<TypeMirrorHandle> thrownTypes, int duplicatesCount, List<TreePathHandle> typeVars, int offset, Map<TargetDescription, Set<String>> targets) {
        super(js, expression, duplicatesCount, offset);
        this.parameters = parameters;
        this.thrownTypes = thrownTypes;
        this.typeVars = typeVars;
        this.targets = targets;
    }

    public String getText() {
        return NbBundle.getMessage(IntroduceHint.class, "FIX_IntroduceMethod");
    }

    public String toString() {
        return "[IntroduceExpressionBasedMethodFix]"; // NOI18N
    }

    @NbBundle.Messages(value = {"MSG_ExpressionContainsLocalReferences=Could not move the expression that references local classes"})
    public ChangeInfo implement() throws Exception {
        JButton btnOk = new JButton(NbBundle.getMessage(IntroduceHint.class, "LBL_Ok"));
        JButton btnCancel = new JButton(NbBundle.getMessage(IntroduceHint.class, "LBL_Cancel"));
        IntroduceMethodPanel panel = new IntroduceMethodPanel("", duplicatesCount, targets, targetIsInterface); //NOI18N
        panel.setOkButton(btnOk);
        String caption = NbBundle.getMessage(IntroduceHint.class, "CAP_IntroduceMethod");
        DialogDescriptor dd = new DialogDescriptor(panel, caption, true, new Object[]{btnOk, btnCancel}, btnOk, DialogDescriptor.DEFAULT_ALIGN, null, null);
        if (DialogDisplayer.getDefault().notify(dd) != btnOk) {
            return null; //cancel
        }
        final String name = panel.getMethodName();
        final Set<Modifier> access = panel.getAccess();
        final boolean replaceOther = panel.getReplaceOther();
        final TargetDescription target = panel.getSelectedTarget();
        js.runModificationTask(new Task<WorkingCopy>() {
            public void run(WorkingCopy copy) throws Exception {
                copy.toPhase(JavaSource.Phase.RESOLVED);
                TreePath expression = IntroduceExpressionBasedMethodFix.this.handle.resolve(copy);
                InstanceRefFinder finder = new InstanceRefFinder(copy, expression);
                finder.process();
                if (finder.containsLocalReferences()) {
                    NotifyDescriptor dd = new NotifyDescriptor.Message(Bundle.MSG_ExpressionContainsLocalReferences(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notifyLater(dd);
                    return;
                }
                boolean referencesInstances = finder.containsInstanceReferences();
                TypeMirror returnType = expression != null ? IntroduceHint.resolveType(copy, expression) : null;
                if (expression == null || returnType == null) {
                    return; //TODO...
                }
                returnType = Utilities.convertIfAnonymous(Utilities.resolveCapturedType(copy, returnType));
                final TreeMaker make = copy.getTreeMaker();
                Tree returnTypeTree = make.Type(returnType);
                List<VariableElement> parameters = IntroduceHint.resolveVariables(copy, IntroduceExpressionBasedMethodFix.this.parameters);
                List<ExpressionTree> realArguments = IntroduceHint.realArguments(make, parameters);
                ExpressionTree invocation = make.MethodInvocation(Collections.<ExpressionTree>emptyList(), make.Identifier(name), realArguments);
                TypeElement targetType = target.type.resolve(copy);
                TreePath pathToClass = targetType != null ? copy.getTrees().getPath(targetType) : null;
                if (pathToClass == null) {
                    pathToClass = TreeUtils.findClass(expression);
                }
                assert pathToClass != null;
                List<VariableTree> formalArguments = IntroduceHint.createVariables(copy, parameters, pathToClass, 
                        Collections.singletonList(expression));
                if (formalArguments == null) {
                    return; //XXX
                }
                List<ExpressionTree> thrown = IntroduceHint.typeHandleToTree(copy, thrownTypes);
                if (thrownTypes == null) {
                    return; //XXX
                }
                List<StatementTree> methodStatements = new LinkedList<StatementTree>();
                methodStatements.add(make.Return((ExpressionTree) expression.getLeaf()));
                List<TypeParameterTree> typeVars = new LinkedList<TypeParameterTree>();
                for (TreePathHandle tph : IntroduceExpressionBasedMethodFix.this.typeVars) {
                    typeVars.add((TypeParameterTree) tph.resolve(copy).getLeaf());
                }
                boolean isStatic = !referencesInstances || IntroduceHint.needsStaticRelativeTo(copy, pathToClass, expression);
                Tree parentTree = expression.getParentPath().getLeaf();
                Tree nueParent = copy.getTreeUtilities().translate(parentTree, Collections.singletonMap(expression.getLeaf(), invocation));
                copy.rewrite(parentTree, nueParent);
                if (replaceOther) {
                    //handle duplicates
                    Document doc = copy.getDocument();
                    Pattern p = Pattern.createPatternWithRemappableVariables(expression, parameters, true);
                    for (Occurrence desc : Matcher.create(copy).setCancel(new AtomicBoolean()).match(p)) {
                        TreePath firstLeaf = desc.getOccurrenceRoot();
                        int startOff = (int) copy.getTrees().getSourcePositions().getStartPosition(copy.getCompilationUnit(), firstLeaf.getLeaf());
                        int endOff = (int) copy.getTrees().getSourcePositions().getEndPosition(copy.getCompilationUnit(), firstLeaf.getLeaf());
                        if (!IntroduceHint.shouldReplaceDuplicate(doc, startOff, endOff)) {
                            continue;
                        }
                        //XXX:
                        List<Union2<VariableElement, TreePath>> dupeParameters = new LinkedList<Union2<VariableElement, TreePath>>();
                        for (VariableElement ve : parameters) {
                            if (desc.getVariablesRemapToTrees().containsKey(ve)) {
                                dupeParameters.add(Union2.<VariableElement, TreePath>createSecond(desc.getVariablesRemapToTrees().get(ve)));
                            } else {
                                dupeParameters.add(Union2.<VariableElement, TreePath>createFirst(ve));
                            }
                        }
                        List<ExpressionTree> dupeRealArguments = IntroduceHint.realArgumentsForTrees(make, dupeParameters);
                        ExpressionTree dupeInvocation = make.MethodInvocation(Collections.<ExpressionTree>emptyList(), make.Identifier(name), dupeRealArguments);
                        copy.rewrite(firstLeaf.getLeaf(), dupeInvocation);
                        isStatic |= IntroduceHint.needsStaticRelativeTo(copy, pathToClass, firstLeaf);
                    }
                    IntroduceHint.introduceBag(doc).clear();
                    //handle duplicates end
                }
                Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
                
                if (target.iface) {
                    modifiers.add(Modifier.DEFAULT);
                } else if (isStatic) {
                    modifiers.add(Modifier.STATIC);
                }
                modifiers.addAll(access);
                ModifiersTree mods = make.Modifiers(modifiers);
                MethodTree method = make.Method(mods, name, returnTypeTree, typeVars, formalArguments, thrown, make.Block(methodStatements, false), null);
                ClassTree nueClass = IntroduceHint.INSERT_CLASS_MEMBER.insertClassMember(copy, (ClassTree) pathToClass.getLeaf(), method, offset);
                copy.rewrite(pathToClass.getLeaf(), nueClass);
            }
        }).commit();
        return null;
    }
    
}
