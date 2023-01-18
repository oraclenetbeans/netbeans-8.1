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

package org.netbeans.modules.java.hints.jdk;

import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.netbeans.modules.java.hints.ArithmeticUtilities;
import org.netbeans.modules.java.hints.errors.Utilities;
import org.netbeans.modules.java.hints.suggestions.IfToSwitchSupport;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.MatcherUtilities;
import org.netbeans.spi.java.hints.BooleanOption;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.IntegerOption;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
@Hint(displayName = "#DN_org.netbeans.modules.java.hints.jdk.ConvertToStringSwitch", description = "#DESC_org.netbeans.modules.java.hints.jdk.ConvertToStringSwitch", category="rules15", suppressWarnings="ConvertToStringSwitch")
@NbBundle.Messages({
    "# {0} - string literal value",
    "TEXT_ChainedIfContainsSameValues=The string value `{0}'' used in String comparison appears earlier in the chained if-else-if statement. This condition never evaluates to true",
    "TEXT_ConvertToSwitch=Convert to switch",
    "# initial label for breaking out of the innermost loop",
    "LABEL_OuterGeneratedLabelInitial=OUTER",
    "# template for generated label names, must form a valid Java identifiers",
    "# {0} - unique integer",
    "LABEL_OuterGeneratedLabel=OUTER_{0}"
})
public class ConvertToStringSwitch {

    static final boolean DEF_ALSO_EQ = true;
    static final int DEF_THRESHOLD = 3;
    
    @BooleanOption(displayName = "#LBL_org.netbeans.modules.java.hints.jdk.ConvertToStringSwitch.KEY_ALSO_EQ", tooltip = "#TP_org.netbeans.modules.java.hints.jdk.ConvertToStringSwitch.KEY_ALSO_EQ", defaultValue=DEF_ALSO_EQ)
    static final String KEY_ALSO_EQ = "also-equals";
    
    @IntegerOption(displayName = "#LBL_org.netbeans.modules.java.hints.jdk.ConvertToStringSwitch.KEY_THRESHOLD", 
            tooltip = "#TP_org.netbeans.modules.java.hints.jdk.ConvertToStringSwitch.KEY_THRESHOLD", defaultValue = DEF_THRESHOLD,
            minValue = 2, step = 1)
    static final String KEY_THRESHOLD = "threshold";
    
    public static final boolean DEFAULT_OPTION_GENERATE_EMPTY_DEFAULT = true;

    @BooleanOption(displayName = "#OPT_ConvertIfToSwitch_EmptyDefault", tooltip = "#DESC_ConvertIfToSwitch_EmptyDefault",
            defaultValue = DEFAULT_OPTION_GENERATE_EMPTY_DEFAULT)
    public static final String OPTION_GENERATE_EMPTY_DEFAULT = "iftoswitch.generate.default"; // NOI18N
    
    private static final String[] INIT_PATTERNS = {
        "$c1.equals($c2)",
        "$c1.contentEquals($c2)"
    };

    private static final String[] INIT_PATTERNS_EQ = {
        "$c1 == $c2"
    };

    private static final String[] PATTERNS = {
        "$var.equals($constant)",
        "$constant.equals($var)",
        "$var.contentEquals($constant)",
        "$constant.contentEquals($var)"
    };
    
    private static final String[] PATTERNS_EQ = {
        "$var == $constant",
        "$constant == $var",
    };

    @TriggerPattern(value="if ($cond) $body; else $else;")
    public static List<ErrorDescription> hint(final HintContext ctx) {
        if (   ctx.getPath().getParentPath().getLeaf().getKind() == Kind.IF
            || ctx.getInfo().getSourceVersion().compareTo(SourceVersion.RELEASE_7) < 0) {
            return null;
        }

        final TypeElement jlString = ctx.getInfo().getElements().getTypeElement("java.lang.String"); // NOI18N

        if (jlString == null) {
            return null;
        }
        final Collection<String> initPatterns = new ArrayList<String>(INIT_PATTERNS.length + INIT_PATTERNS_EQ.length);

        initPatterns.addAll(Arrays.asList(INIT_PATTERNS));
        
        if (ctx.getPreferences().getBoolean(KEY_ALSO_EQ, DEF_ALSO_EQ)) {
            initPatterns.addAll(Arrays.asList(INIT_PATTERNS_EQ));
        }
        
        IfToSwitchSupport eval = new IfToSwitchSupport(ctx) {
            private boolean [] varConst = new boolean[1];

            @Override
            protected Object evalConstant(TreePath path) {
                TypeMirror m = ci.getTrees().getTypeMirror(path);
                if (ci.getTypes().asElement(m) == jlString) {
                    Object o = ArithmeticUtilities.compute(ci, path, true, true);
                    if (ArithmeticUtilities.isRealValue(o)) {
                        return o;
                    }
                }
                return null;
            }
            
            
            @Override
            protected TreePath matchesChainedItem(TreePath test, TreePath variable) {
                varConst[0] = false;
                TreePath p = isStringComparison(ctx, test, varConst, variable);
                if (p == null) {
                    return null;
                }
                if (varConst[0]) {
                    controlVariableNotNull();
                }
                return p;
            }

            @Override
            protected TreePath matches(TreePath test, boolean initial) {
                for (String pat : initPatterns) {
                    if (MatcherUtilities.matches(ctx, test, pat, true)) {
                        TreePath c1 = ctx.getVariables().get("$c1");
                        TypeMirror m = ctx.getInfo().getTrees().getTypeMirror(c1);
                        if (!Utilities.isValidType(m) ||
                             ctx.getInfo().getTypes().asElement(m) != jlString) {
                            continue;
                        }
                        TreePath c2 = ctx.getVariables().get("$c2");
                        m = ctx.getInfo().getTrees().getTypeMirror(c2);
                        if (!Utilities.isValidType(m) ||
                             ctx.getInfo().getTypes().asElement(m) != jlString) {
                            continue;
                        }
                        reportConstantAndLiteral(c1, c2);
                        return c1;
                    }
                }
                return null;
            }
            
        };
        if (!eval.process(ctx.getVariables().get("$cond"))) {
            return null;
        }
        int minBranches = ctx.getPreferences().getInt(KEY_THRESHOLD, DEF_THRESHOLD);
        if (eval.getNumberOfBranches() < minBranches) {
            return null;
        }
        
        if (eval.containsDuplicateConstants()) {
            List<ErrorDescription> descs = new ArrayList<>(eval.getDuplicateConstants().size());
            Set<Object> seen = new HashSet<>();
            for (Map.Entry<TreePath, Object> en : eval.getDuplicateConstants().entrySet()) {
                String lit = en.getValue().toString();
                // do not report a single value more than once; confusing.
                if (!seen.add(lit)) {
                    continue;
                }
                TreePath t = en.getKey();
                descs.add(ErrorDescriptionFactory.forTree(ctx, t, Bundle.TEXT_ChainedIfContainsSameValues(lit)));
            }
            return descs;
        }

        Fix convert = eval.createFix(NbBundle.getMessage(ConvertToStringSwitch.class, "FIX_ConvertToStringSwitch"),
                ctx.getPreferences().getBoolean(OPTION_GENERATE_EMPTY_DEFAULT, DEFAULT_OPTION_GENERATE_EMPTY_DEFAULT)).toEditorFix(); // NOI18N
        ErrorDescription ed = ErrorDescriptionFactory.forName(ctx,
                                                              ctx.getPath(),
                                                              Bundle.TEXT_ConvertToSwitch(),
                                                              convert);

        return Collections.singletonList(ed);
    }

    private static TreePath isStringComparison(HintContext ctx, TreePath tp, boolean[] varConst, TreePath var) {
        Tree leaf = tp.getLeaf();

        while (leaf.getKind() == Kind.PARENTHESIZED) {
            tp = new TreePath(tp, ((ParenthesizedTree) leaf).getExpression());
            leaf = tp.getLeaf();
        }

        Collection<String> patterns = new ArrayList<String>(PATTERNS.length + PATTERNS_EQ.length);

        patterns.addAll(Arrays.asList(PATTERNS));

        ctx.getVariables().put("$var", var);
        if (ctx.getPreferences().getBoolean(KEY_ALSO_EQ, DEF_ALSO_EQ)) {
            patterns.addAll(Arrays.asList(PATTERNS_EQ));
        }
        int i = -1;
        assert PATTERNS.length == 4; // the cycle counts with specific positions
        for (String patt : patterns) {
            ++i;
            ctx.getVariables().remove("$constant"); // NOI18N

            if (!MatcherUtilities.matches(ctx, tp, patt, true))
                continue;
            if (i % 2 == 0 && i < 4) {
                varConst[0] = true;
            }
            return ctx.getVariables().get("$constant"); // NOI18N
        }

        return null;
    }

}
