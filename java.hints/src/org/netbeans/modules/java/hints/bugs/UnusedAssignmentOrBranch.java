/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints.bugs;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.CompilationInfo.CacheClearPolicy;
import org.netbeans.api.java.source.support.CancellableTreePathScanner;
import org.netbeans.modules.java.hints.introduce.Flow;
import org.netbeans.modules.java.hints.introduce.Flow.FlowResult;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.hints.Hint.Options;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerPatterns;
import org.openide.util.NbBundle;
import org.openide.util.Pair;

/**
 *
 * @author lahvac
 */
public class UnusedAssignmentOrBranch {
    
    private static final String UNUSED_ASSIGNMENT_ID = "org.netbeans.modules.java.hints.bugs.UnusedAssignmentOrBranch.unusedAssignment";
    private static final String DEAD_BRANCH_ID = "org.netbeans.modules.java.hints.bugs.UnusedAssignmentOrBranch.deadBranch";
    private static final Object KEY_COMPUTED_ASSIGNMENTS = new Object();

    private static Pair<Set<Tree>, Set<Element>> computeUsedAssignments(final HintContext ctx) {
        final CompilationInfo info = ctx.getInfo();
        Pair<Set<Tree>, Set<Element>> result = (Pair<Set<Tree>, Set<Element>>) info.getCachedValue(KEY_COMPUTED_ASSIGNMENTS);

        if (result != null) return result;

        FlowResult flow = Flow.assignmentsForUse(ctx);

        if (flow == null) return null;

        final Set<Tree> usedAssignments = new HashSet<Tree>();

        for (Iterable<? extends TreePath> i : flow.getAssignmentsForUse().values()) {
            for (TreePath tp : i) {
                if (tp == null) continue;

                usedAssignments.add(tp.getLeaf());
            }
        }

        final Set<Element> usedVariables = new HashSet<Element>();

        new CancellableTreePathScanner<Void, Void>() {
            @Override public Void visitAssignment(AssignmentTree node, Void p) {
                Element var = info.getTrees().getElement(new TreePath(getCurrentPath(), node.getVariable()));

                if (var != null && LOCAL_VARIABLES.contains(var.getKind()) && !usedAssignments.contains(node.getExpression())) {
                    scan(node.getExpression(), null);
                    return null;
                }

                return super.visitAssignment(node, p);
            }
            @Override public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
                Element var = info.getTrees().getElement(new TreePath(getCurrentPath(), node.getVariable()));

                if (var != null && LOCAL_VARIABLES.contains(var.getKind()) && !usedAssignments.contains(node.getExpression())) {
                    scan(node.getExpression(), null);
                    return null;
                }

                return super.visitCompoundAssignment(node, p);
            }
            @Override public Void visitIdentifier(IdentifierTree node, Void p) {
                Element var = info.getTrees().getElement(getCurrentPath());

                if (var != null && LOCAL_VARIABLES.contains(var.getKind())) {
                    usedVariables.add(var);
                }
                return super.visitIdentifier(node, p);
            }
            @Override protected boolean isCanceled() {
                return ctx.isCanceled();
            }
        }.scan(info.getCompilationUnit(), null);

        info.putCachedValue(KEY_COMPUTED_ASSIGNMENTS, result = Pair.<Set<Tree>, Set<Element>>of(usedAssignments, usedVariables), CacheClearPolicy.ON_TASK_END);

        return result;
    }

    private static final Set<ElementKind> LOCAL_VARIABLES = EnumSet.of(ElementKind.EXCEPTION_PARAMETER, ElementKind.LOCAL_VARIABLE, ElementKind.PARAMETER);

    @Hint(displayName = "#DN_org.netbeans.modules.java.hints.bugs.UnusedAssignmentOrBranch.unusedAssignment", description = "#DESC_org.netbeans.modules.java.hints.bugs.UnusedAssignmentOrBranch.unusedAssignment", category="bugs", id=UNUSED_ASSIGNMENT_ID, options={Options.QUERY}, suppressWarnings="UnusedAssignment")
    @TriggerPatterns({
        @TriggerPattern("$var = $value"),
        @TriggerPattern("$mods$ $type $var = $value;")
    })
    public static ErrorDescription unusedAssignment(final HintContext ctx) {
        final String unusedAssignmentLabel = NbBundle.getMessage(UnusedAssignmentOrBranch.class, "LBL_UNUSED_ASSIGNMENT_LABEL");
        Pair<Set<Tree>, Set<Element>> computedAssignments = computeUsedAssignments(ctx);

        if (ctx.isCanceled() || computedAssignments == null) return null;

        final CompilationInfo info = ctx.getInfo();
        final Set<Tree> usedAssignments = computedAssignments.first();
        final Set<Element> usedVariables = computedAssignments.second();
        Element var = info.getTrees().getElement(ctx.getVariables().get("$var"));
        Tree value = ctx.getVariables().get("$value").getLeaf();

        if (var != null && LOCAL_VARIABLES.contains(var.getKind()) && !usedAssignments.contains(value) && usedVariables.contains(var)) {
            return ErrorDescriptionFactory.forTree(ctx, value, unusedAssignmentLabel);
        }

        return null;
    }

    @Hint(displayName = "#DN_org.netbeans.modules.java.hints.bugs.UnusedAssignmentOrBranch.deadBranch", description = "#DESC_org.netbeans.modules.java.hints.bugs.UnusedAssignmentOrBranch.deadBranch", category="bugs", id=DEAD_BRANCH_ID, options=Options.QUERY, suppressWarnings="DeadBranch")
    @TriggerTreeKind(Tree.Kind.IF)
    public static List<ErrorDescription> deadBranch(HintContext ctx) {
        String deadBranchLabel = NbBundle.getMessage(UnusedAssignmentOrBranch.class, "LBL_DEAD_BRANCH");
        FlowResult flow = Flow.assignmentsForUse(ctx);

        if (flow == null) return null;

        List<ErrorDescription> result = new ArrayList<ErrorDescription>();
        Set<? extends Tree> flowResult = flow.getDeadBranches();
        IfTree it = (IfTree) ctx.getPath().getLeaf();
        
        if (flowResult.contains(it.getThenStatement())) {
            result.add(ErrorDescriptionFactory.forTree(ctx, it.getThenStatement(), deadBranchLabel));
        }
        Tree t = it.getElseStatement();
        if (flowResult.contains(t)) {
            result.add(ErrorDescriptionFactory.forTree(ctx, t, deadBranchLabel));
            while (t != null && t.getKind() == Tree.Kind.IF) {
                it = (IfTree)t;
                t = it.getElseStatement();
                result.add(ErrorDescriptionFactory.forTree(ctx, t, deadBranchLabel));
            }
        }
        return result;
    }

}
