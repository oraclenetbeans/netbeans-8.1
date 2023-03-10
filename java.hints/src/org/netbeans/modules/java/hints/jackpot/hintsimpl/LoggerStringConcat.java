/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints.jackpot.hintsimpl;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.java.hints.errors.Utilities;
import org.netbeans.spi.java.hints.ConstraintVariableType;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerPatterns;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.JavaFix;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.hints.JavaFixUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author lahvac
 */
@Hint(displayName = "#DN_org.netbeans.modules.java.hints.jackpot.hintsimpl.LoggerStringConcat", description = "#DESC_org.netbeans.modules.java.hints.jackpot.hintsimpl.LoggerStringConcat", id="org.netbeans.modules.java.hints.jackpot.hintsimpl.LoggerStringConcat", category="logging", suppressWarnings="LoggerStringConcat")
public class LoggerStringConcat {

    private static final Logger LOG = Logger.getLogger(LoggerStringConcat.class.getName());

    @TriggerPattern(value = "$logger.log($level, $message)",
                    constraints = {
                        @ConstraintVariableType(variable="$logger", type="java.util.logging.Logger"),
                        @ConstraintVariableType(variable="$level", type="java.util.logging.Level"),
                        @ConstraintVariableType(variable="$message", type="java.lang.String")
                    })
    public static ErrorDescription hint1(HintContext ctx) {
        return compute(ctx, null);
    }

//    @Hint("org.netbeans.modules.java.hints.jackpot.hintsimpl.LoggerStringConcat")
//    @TriggerPattern(value = "$logger.fine($message)",
//                    constraints = {
//                        @Constraint(variable="$logger", type="java.util.logging.Logger"),
//                        @Constraint(variable="$message", type="java.lang.String")
//                    })
//    public static ErrorDescription hint2(HintContext ctx) {
//        String methodName = ctx.getVariableNames().get("$method");
//
//        if (findConstant(ctx.getInfo(), methodName) == null) {
//            return null;
//        }
//
//        return compute(ctx, methodName);
//    }

    @TriggerPatterns({
        @TriggerPattern(value = "$logger.severe($message)",
                        constraints = {
                            @ConstraintVariableType(variable="$logger", type="java.util.logging.Logger"),
                            @ConstraintVariableType(variable="$message", type="java.lang.String")
                        }),
        @TriggerPattern(value = "$logger.warning($message)",
                        constraints = {
                            @ConstraintVariableType(variable="$logger", type="java.util.logging.Logger"),
                            @ConstraintVariableType(variable="$message", type="java.lang.String")
                        }),
        @TriggerPattern(value = "$logger.info($message)",
                        constraints = {
                            @ConstraintVariableType(variable="$logger", type="java.util.logging.Logger"),
                            @ConstraintVariableType(variable="$message", type="java.lang.String")
                        }),
        @TriggerPattern(value = "$logger.config($message)",
                        constraints = {
                            @ConstraintVariableType(variable="$logger", type="java.util.logging.Logger"),
                            @ConstraintVariableType(variable="$message", type="java.lang.String")
                        }),
        @TriggerPattern(value = "$logger.fine($message)",
                        constraints = {
                            @ConstraintVariableType(variable="$logger", type="java.util.logging.Logger"),
                            @ConstraintVariableType(variable="$message", type="java.lang.String")
                        }),
        @TriggerPattern(value = "$logger.finer($message)",
                        constraints = {
                            @ConstraintVariableType(variable="$logger", type="java.util.logging.Logger"),
                            @ConstraintVariableType(variable="$message", type="java.lang.String")
                        }),
        @TriggerPattern(value = "$logger.finest($message)",
                        constraints = {
                            @ConstraintVariableType(variable="$logger", type="java.util.logging.Logger"),
                            @ConstraintVariableType(variable="$message", type="java.lang.String")
                        })
    })
    public static ErrorDescription hint2(HintContext ctx) {
        TreePath inv = ctx.getPath();
        MethodInvocationTree mit = (MethodInvocationTree) inv.getLeaf();
        ExpressionTree sel = mit.getMethodSelect();
        String methodName = sel.getKind() == Kind.MEMBER_SELECT ? ((MemberSelectTree) sel).getIdentifier().toString() : ((IdentifierTree) sel).getName().toString();

        if (findConstant(ctx.getInfo(), methodName) != null) {
            return compute(ctx, methodName);
        } else {
            //#180865: should not happen, but apparently does. Print some debug info in dev builds:
            boolean dev = false;

            assert dev = true;

            if (dev) {
                StringBuilder log = new StringBuilder();
                
                log.append("Please add the following info the bug #180865:\n");
                log.append("tree: ").append(ctx.getPath().getLeaf()).append("\n");
                TreePath loggerVar = ctx.getVariables().get("$logger");
                if (loggerVar != null) {
                    log.append("logger type: ").append(ctx.getInfo().getTrees().getTypeMirror(loggerVar)).append("\n");
                } else {
                    log.append("$logger == null\n");
                }
                log.append("source level: ").append(ctx.getInfo().getSourceVersion()).append("\n");
                log.append("End of #180865 debug info");
                LOG.info(log.toString());
            }

            return null;
        }
    }

    private static ErrorDescription compute(HintContext ctx, String methodName) {
        TreePath message = ctx.getVariables().get("$message");
        List<List<TreePath>> sorted = Utilities.splitStringConcatenationToElements(ctx.getInfo(), message);

        if (sorted.size() <= 1) {
            return null;
        }

        //check for erroneous trees:
        for (List<TreePath> tps : sorted)
            for (TreePath tp : tps)
                if (tp.getLeaf().getKind() == Kind.ERRONEOUS) return null;

        FixImpl fix = new FixImpl(NbBundle.getMessage(LoggerStringConcat.class, "MSG_LoggerStringConcat_fix"), methodName, TreePathHandle.create(ctx.getPath(), ctx.getInfo()), TreePathHandle.create(message, ctx.getInfo()));

        return ErrorDescriptionFactory.forTree(ctx, message, NbBundle.getMessage(LoggerStringConcat.class, "MSG_LoggerStringConcat"), fix.toEditorFix());
    }

    private static void rewrite(WorkingCopy wc, ExpressionTree level, MethodInvocationTree invocation, TreePath message) {
        List<List<TreePath>> sorted = Utilities.splitStringConcatenationToElements(wc, message);
        StringBuilder workingLiteral = new StringBuilder();
        List<Tree> newMessage = new LinkedList<Tree>();
        List<ExpressionTree> newParams = new LinkedList<ExpressionTree>();
        int variablesCount = 0;
        TreeMaker make = wc.getTreeMaker();

        for (List<TreePath> element : sorted) {
            if (element.size() == 1 && Utilities.isStringOrCharLiteral(element.get(0).getLeaf())) {
                String literalValue = ((LiteralTree) element.get(0).getLeaf()).getValue().toString();

                literalValue = literalValue.replaceAll("'", "''");
                literalValue = literalValue.replaceAll(Pattern.quote("{"), Matcher.quoteReplacement("'{'"));
                literalValue = literalValue.replaceAll(Pattern.quote("}"), Matcher.quoteReplacement("'}'"));
                workingLiteral.append(literalValue);
            } else {
                if (element.size() == 1 && !Utilities.isConstantString(wc, element.get(0), true)) {
                    workingLiteral.append("{");
                    workingLiteral.append(Integer.toString(variablesCount++));
                    workingLiteral.append("}");
                    newParams.add((ExpressionTree) element.get(0).getLeaf());
                } else {
                    if (workingLiteral.length() > 0) {
                        newMessage.add(make.Literal(workingLiteral.toString()));
                        workingLiteral.delete(0, workingLiteral.length());
                    }

                    for (Iterator<TreePath> it = element.iterator(); it.hasNext(); ) {
                        TreePath tp = it.next();
                        
                        if (Utilities.isStringOrCharLiteral(tp.getLeaf())) {
                            String literalValue = ((LiteralTree) tp.getLeaf()).getValue().toString();

                            if (literalValue.contains("'") || literalValue.contains("{") || literalValue.contains("}")) {
                                literalValue = literalValue.replaceAll("'", "''");
                                literalValue = literalValue.replaceAll(Pattern.quote("{"), Matcher.quoteReplacement("'{'"));
                                literalValue = literalValue.replaceAll(Pattern.quote("}"), Matcher.quoteReplacement("'}'"));
                                if (it.hasNext()) {
                                    newMessage.add(make.Literal(literalValue));
                                } else {
                                    workingLiteral.append(literalValue);
                                }
                            } else {
                                if (it.hasNext()) {
                                    newMessage.add(tp.getLeaf());
                                } else {
                                    workingLiteral.append(literalValue);
                                }
                            }
                        } else {
                            newMessage.add(tp.getLeaf());
                        }
                    }
                }
            }
        }

        if (workingLiteral.length() > 0) {
            newMessage.add(make.Literal(workingLiteral.toString()));
        }

        ExpressionTree messageFinal = (ExpressionTree) newMessage.remove(0);

        while (!newMessage.isEmpty()) {
            messageFinal = make.Binary(Kind.PLUS, messageFinal, (ExpressionTree) newMessage.remove(0));
        }

        List<ExpressionTree> nueParams = new LinkedList<ExpressionTree>();

        nueParams.add(level);
        nueParams.add(messageFinal);

        if (newParams.size() > 1) {
            nueParams.add(make.NewArray(make.QualIdent(wc.getElements().getTypeElement("java.lang.Object")), Collections.<ExpressionTree>emptyList(), newParams));
        } else {
            nueParams.addAll(newParams);
        }

        ExpressionTree sel = invocation.getMethodSelect();
        ExpressionTree nueSel;

        if (sel.getKind() == Kind.MEMBER_SELECT)
            nueSel = make.MemberSelect(((MemberSelectTree) sel).getExpression(), "log");
        else
            nueSel = make.Identifier("log");
        
        MethodInvocationTree nue = make.MethodInvocation((List<? extends ExpressionTree>) invocation.getTypeArguments(), nueSel, nueParams);

        wc.rewrite(invocation, nue);
    }

    private static VariableElement findConstant(CompilationInfo info, String logMethodName) {
        logMethodName = logMethodName.toUpperCase();
        
        TypeElement julLevel = info.getElements().getTypeElement("java.util.logging.Level");

        if (julLevel == null) {
            return null;
        }
        
        for (VariableElement el : ElementFilter.fieldsIn(julLevel.getEnclosedElements())) {
            if (el.getSimpleName().contentEquals(logMethodName)) {
                return el;
            }
        }

        return null;
    }

    private static final class FixImpl extends JavaFix {

        private final String displayName;
        private final String logMethodName; //only if != log
        private final TreePathHandle message;

        public FixImpl(String displayName, String logMethodName, TreePathHandle invocation, TreePathHandle message) {
            super(invocation);
            this.displayName = displayName;
            this.logMethodName = logMethodName;
            this.message = message;
        }

        public String getText() {
            return displayName;
        }

        @Override
        protected void performRewrite(TransformationContext ctx) {
            WorkingCopy wc = ctx.getWorkingCopy();
            TreePath invocation = ctx.getPath();
            TreePath message    = FixImpl.this.message.resolve(wc);
            MethodInvocationTree mit = (MethodInvocationTree) invocation.getLeaf();
            ExpressionTree level = null;

            if (logMethodName != null) {
                String logMethodNameUpper = logMethodName.toUpperCase();
                VariableElement c = findConstant(wc, logMethodNameUpper);

                level = wc.getTreeMaker().QualIdent(c);
            } else {
                level = mit.getArguments().get(0);
            }

            rewrite(wc, level, mit, message);
        }

    }

}
