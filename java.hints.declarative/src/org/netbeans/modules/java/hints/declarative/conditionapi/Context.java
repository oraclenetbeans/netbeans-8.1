/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints.declarative.conditionapi;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.queries.SourceLevelQuery;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.modules.java.hints.declarative.APIAccessor;
import org.netbeans.modules.java.hints.spiimpl.Hacks;
import org.netbeans.spi.java.hints.HintContext;

/**
 *
 * @author lahvac
 */
public class Context {

            final HintContext ctx;
            final List<Map<String, TreePath>> variables = new LinkedList<Map<String, TreePath>>();
            final List<Map<String, Collection<? extends TreePath>>> multiVariables = new LinkedList<Map<String, Collection<? extends TreePath>>>();
            final List<Map<String, String>> variableNames = new LinkedList<Map<String, String>>();
    private final AtomicInteger auxiliaryVariableCounter = new AtomicInteger();

    //XXX: should not be public:
    public Context(HintContext ctx) {
        this.ctx = ctx;
        this.variables.add(Collections.unmodifiableMap(ctx.getVariables()));
        this.multiVariables.add(Collections.unmodifiableMap(ctx.getMultiVariables()));
        this.variableNames.add(Collections.unmodifiableMap(ctx.getVariableNames()));
    }

    public @NonNull SourceVersion sourceVersion() {
        String sourceLevel = SourceLevelQuery.getSourceLevel(ctx.getInfo().getFileObject());

        if (sourceLevel == null) {
            return SourceVersion.latest(); //TODO
        }

        String[] splited = sourceLevel.split("\\.");
        String   spec    = splited[1];

        return SourceVersion.valueOf("RELEASE_"+  spec);//!!!
    }

    public @NonNull Set<Modifier> modifiers(@NonNull Variable variable) {
        final Element e = ctx.getInfo().getTrees().getElement(getSingleVariable(variable));

        if (e == null) {
            return Collections.unmodifiableSet(EnumSet.noneOf(Modifier.class));
        }

        return Collections.unmodifiableSet(e.getModifiers());
    }

    public @CheckForNull ElementKind elementKind(@NonNull Variable variable) {
        final Element e = ctx.getInfo().getTrees().getElement(getSingleVariable(variable));

        if (e == null) {
            return null;
        }

        return e.getKind();
    }

    public @CheckForNull TypeKind typeKind(@NonNull Variable variable) {
        final TypeMirror tm = ctx.getInfo().getTrees().getTypeMirror(getSingleVariable(variable));

        if (tm == null) {
            return null;
        }

        return tm.getKind();
    }

    public @CheckForNull String name(@NonNull Variable variable) {
        final Element e = ctx.getInfo().getTrees().getElement(getSingleVariable(variable));

        if (e == null) {
            return null;
        }

        return e.getSimpleName().toString();
    }

    public @CheckForNull Variable parent(@NonNull Variable variable) {
        TreePath tp = getSingleVariable(variable);

        if (tp.getParentPath() == null) {
            return null;
        }

        return enterAuxiliaryVariable(tp.getParentPath());
    }

    private Variable enterAuxiliaryVariable(TreePath path) {
        String output = "*" + auxiliaryVariableCounter.getAndIncrement();

        variables.get(0).put(output, path);

        return new Variable(output);
    }
    
    public @NonNull Variable variableForName(@NonNull String variableName) {
        Variable result = new Variable(variableName);

        if (getSingleVariable(result) == null) {
            throw new IllegalStateException("Unknown variable");
        }
        
        return result;
    }

    public void createRenamed(@NonNull Variable from, @NonNull Variable to, @NonNull String newName) {
        //TODO: check (the variable should not exist)
        variableNames.get(0).put(to.variableName, newName);
        TreePath origVariablePath = getSingleVariable(from);
        TreePath newVariablePath = new TreePath(origVariablePath.getParentPath(), Hacks.createRenameTree(origVariablePath.getLeaf(), newName));
        variables.get(0).put(to.variableName, newVariablePath);
    }

    public boolean isNullLiteral(@NonNull Variable var) {
        TreePath varPath = getSingleVariable(var);

        return varPath.getLeaf().getKind() == Kind.NULL_LITERAL;
    }

    public @NonNull Iterable<? extends Variable> getIndexedVariables(@NonNull Variable multiVariable) {
        Iterable<? extends TreePath> paths = getMultiVariable(multiVariable);

        if (paths == null) {
            throw new IllegalArgumentException("TODO: explanation");
        }

        Collection<Variable> result = new LinkedList<Variable>();
        int index = 0;

        for (TreePath tp : paths) {
            result.add(new Variable(multiVariable.variableName, index++));
        }

        return result;
    }

    public void enterScope() {
        variables.add(0, new HashMap<String, TreePath>());
        multiVariables.add(0, new HashMap<String, Collection<? extends TreePath>>());
        variableNames.add(0, new HashMap<String, String>());
    }

    public void leaveScope() {
        variables.remove(0);
        multiVariables.remove(0);
        variableNames.remove(0);
    }

    Iterable<? extends TreePath> getVariable(Variable v) {
        if (isMultistatementWildcard(v.variableName) && v.index == (-1)) {
            return getMultiVariable(v);
        } else {
            return Collections.singletonList(getSingleVariable(v));
        }
    }

    //XXX: copied from jackpot30.impl.Utilities:
    private static boolean isMultistatementWildcard(/*@NonNull */CharSequence name) {
        return name.charAt(name.length() - 1) == '$';
    }

    //TODO: check if correct variable is provided:
    TreePath getSingleVariable(Variable v) {
        if (v.index == (-1)) {
            for (Map<String, TreePath> map : variables) {
                if (map.containsKey(v.variableName)) {
                    return map.get(v.variableName);
                }
            }
            
            return null;
        } else {
            return new ArrayList<TreePath>(getMultiVariable(v)).get(v.index);
        }
    }

    private Collection<? extends TreePath> getMultiVariable(Variable v) {
        for (Map<String, Collection<? extends TreePath>> multi : multiVariables) {
            if (multi.containsKey(v.variableName)) {
                return multi.get(v.variableName);
            }
        }

        return null;
    }

    static {
        APIAccessor.IMPL = new APIAccessorImpl();
    }

    /**Returns canonical names of classes that enclose the {@link Variable}.
     * If the given {@link Variable} represents a class, its canonical name is also listed.
     * The names are given from the innermost class to the outermost class.
     *
     * @return the canonical names of the enclosing classes
     */
    public @NonNull Iterable<? extends String> enclosingClasses(Variable forVariable) {
        List<String> result = new ArrayList<String>();
        TreePath path = getSingleVariable(forVariable);

        while (path != null) {
            TreePath current = path;

            path = path.getParentPath();
            
            if (!TreeUtilities.CLASS_TREE_KINDS.contains(current.getLeaf().getKind())) continue;

            Element e = ctx.getInfo().getTrees().getElement(current);

            if (e == null) continue;

            if (e.getKind().isClass() || e.getKind().isInterface()) {
                result.add(((TypeElement) e).getQualifiedName().toString());
            }
        }

        return result;
    }

    /**Returns name of package in which the current file is located. Default package
     * is represented by an empty string.
     *
     * @return the name of the enclosing package
     */
    public @NonNull String enclosingPackage() {
        CompilationUnitTree cut = ctx.getInfo().getCompilationUnit();

        return cut.getPackageName() != null ? cut.getPackageName().toString() : "";
    }
    
    /**Checks whether the given Java element is available in the particular source
     * code or not.
     * 
     * The <code>elementDescription</code> format is as follows:
     * <dl>
     *   <dt>for type (class, enum, interface or annotation type)</dt>
     *     <dd><em>the FQN of the type</em></dd>
     *   <dt>for field or enum constant</dt>
     *     <dd><em>the FQN of the enclosing type</em><code>.</code><em>field name</em></dd>
     *   <dt>for method</dt>
     *     <dd><em>the FQN of the enclosing type</em><code>.</code><em>method name</em><code>(</code><em>comma separated parameter types</em><code>)</code><br>
     *         The parameter types may include type parameters, but these are ignored. The last parameter type can use ellipsis (...) to denote vararg method.</dd>
     *   <dt>for constructor</dt>
     *     <dd><em>the FQN of the enclosing type</em><code>.</code><em>simple name of enclosing type</em><code>(</code><em>comma separated parameter types</em><code>)</code><br>
     *         See method format for more details on parameter types.</dd>
     * </dl>
     * 
     * @param elementDescription the description of the element that should be checked for existence
     * @return true if and only the specified element exists while processing the current source
     * @since nb74
     */
    public boolean isAvailable(@NonNull String description) {
        return ctx.getInfo().getElementUtilities().findElement(description) != null;
    }

    static final class APIAccessorImpl extends APIAccessor {

        @Override
        public TreePath getSingleVariable(Context ctx, Variable var) {
            return ctx.getSingleVariable(var);
        }

        @Override
        public HintContext getHintContext(Context ctx) {
            return ctx.ctx;
        }

        @Override
        public Map<String, TreePath> getVariables(Context ctx) {
            Map<String, TreePath> result = new HashMap<String, TreePath>();

            for (Map<String, TreePath> m : reverse(ctx.variables)) {
                result.putAll(m);
            }

            return result;
        }

        @Override
        public Map<String, Collection<? extends TreePath>> getMultiVariables(Context ctx) {
            Map<String, Collection<? extends TreePath>> result = new HashMap<String, Collection<? extends TreePath>>();

            for (Map<String, Collection<? extends TreePath>> m : reverse(ctx.multiVariables)) {
                result.putAll(m);
            }

            return result;
        }

        @Override
        public Map<String, String> getVariableNames(Context ctx) {
            Map<String, String> result = new HashMap<String, String>();

            for (Map<String, String> m : reverse(ctx.variableNames)) {
                result.putAll(m);
            }

            return result;
        }

        private <T> List<T> reverse(List<T> original) {
            List<T> result = new LinkedList<T>();

            for (T t : original) {
                result.add(0, t);
            }

            return result;
        }

        @Override
        public Variable enterAuxiliaryVariable(Context ctx, TreePath source) {
            return ctx.enterAuxiliaryVariable(source);
        }

    }
}
