/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.source.save;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.netbeans.modules.java.source.builder.ASTService;
import org.netbeans.modules.java.source.builder.QualIdentTree;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.netbeans.modules.refactoring.spi.RefactoringPluginFactory;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class ElementOverlay {

    private static final Logger LOG = Logger.getLogger(ElementOverlay.class.getName());
    private static final ThreadLocal<ElementOverlay> transactionOverlay = new ThreadLocal<ElementOverlay>();
    
    public static void beginTransaction() {
        transactionOverlay.set(new ElementOverlay());
        LOG.log(Level.FINE, "transaction started");
    }
    
    public static void endTransaction() {
        transactionOverlay.set(null);
        LOG.log(Level.FINE, "transaction end");
    }
    
    public static ElementOverlay getOrCreateOverlay() {
        ElementOverlay overlay = transactionOverlay.get();
        
        if (overlay == null) {
            overlay = new ElementOverlay();
        }
        
        return overlay;
    }

    private ElementOverlay() {}
    
    private final Map<String, List<String>> class2Enclosed = new HashMap<String, List<String>>();
    private final Map<String, Collection<String>> class2SuperElementTrees = new HashMap<String, Collection<String>>();
    private final Set<String> packages = new HashSet<String>();
    private final Map<String, Set<Modifier>> classes = new HashMap<String, Set<Modifier>>();
    private final Map<String, Element> elementCache = new HashMap<String, Element>();

    public List<Element> getEnclosedElements(ASTService ast, Elements elements, String parent) {
        List<Element> result = new LinkedList<Element>();
        List<String> enclosed = class2Enclosed.get(parent);

        if (enclosed != null) {
            for (String enc : enclosed) {
                Element el = resolve(ast, elements, enc);

                if (el != null) {
                    result.add(el);
                }
            }
            
            return result;
        }
        
        Element parentEl = resolve(ast, elements, parent);

        if (parentEl == null) throw new IllegalStateException(parent);
        if (parentEl instanceof FakeTypeElement) {
            TypeElement original = elements.getTypeElement(parent);

            if (original != null) result.addAll(wrap(ast, elements, original.getEnclosedElements()));
        } else if (parentEl instanceof FakePackageElement) {
            PackageElement original = elements.getPackageElement(parent);

            if (original != null) result.addAll(wrap(ast, elements, original.getEnclosedElements()));
        } else {
            result.addAll(parentEl.getEnclosedElements());
        }


        return result;
    }

    private Element createElement(ASTService ast, Elements elements, String name, Element original) {
        Element el = elementCache.get(name);

        if (el == null) {
            if (original != null) {
                if (original.getKind().isClass() || original.getKind().isInterface()) {
                    elementCache.put(name, el = new TypeElementWrapper(ast, elements, (TypeElement) original));
                    return el;
                }
                if (original.getKind() == ElementKind.PACKAGE) {
                    elementCache.put(name, el = new PackageElementWrapper(ast, elements, (PackageElement) original));
                    return el;
                }

                return original;
            }
            
            int lastDot = name.lastIndexOf('.');
            Name simpleName = elements.getName(name.substring(lastDot + 1));
            Name fqnName = elements.getName(name);

            if (classes.containsKey(name)) {
                Element parent = lastDot > 0 ? resolve(ast, elements, name.substring(0, lastDot)) : elements.getPackageElement("");

                elementCache.put(name, el = new FakeTypeElement(ast, elements, simpleName, fqnName, name, parent, classes.get(name)));
            } else if (packages.contains(name)) {
                elementCache.put(name, el = new FakePackageElement(ast, elements, fqnName, name, simpleName));
            } else {
                return null;//XXX: handling of this null in callers!
            }
        }

        return el;
    }
    
    public Element getOriginal(Element e) {
        if (e instanceof TypeElementWrapper)
            return ((TypeElementWrapper)e).delegateTo;
        if (e instanceof PackageElementWrapper)
            return ((PackageElementWrapper)e).delegateTo;
        return e;
    }

    public Element resolve(ASTService ast, Elements elements, String what) {
        Element result = null;
        
        if (classes.containsKey(what)) {
            result = createElement(ast, elements, what, null);
        }

        if (result == null) {
            result = elements.getTypeElement(what);
        }

        if (result == null) {
            result = elements.getPackageElement(what);
        }

        result = createElement(ast, elements, what, result);

        return result;
    }

    public void registerClass(String parent, String clazz, ClassTree tree, boolean modified) {
        if (clazz == null) return;
        
        Element myself = ASTService.getElementImpl(tree);

        boolean newOrModified =    myself == null
                                || (!myself.getKind().isClass() && !myself.getKind().isInterface())
                                || !((QualifiedNameable) myself).getQualifiedName().contentEquals(clazz);

        if (newOrModified || class2Enclosed.containsKey(parent)) {
            List<String> c = class2Enclosed.get(parent);

            if (c == null) {
                class2Enclosed.put(parent, c = new ArrayList<String>());
            }

            c.add(clazz);
        }

        if (modified) {
            class2Enclosed.put(clazz, new ArrayList<String>());
        }

        Set<String> superFQNs = superFQNs(tree);

        boolean hadObject = superFQNs.remove("java.lang.Object");

        Set<String> original;

        if (!newOrModified) {
            original = new LinkedHashSet<String>();

            TypeElement tel = (TypeElement) myself;

            if (tel.getSuperclass() != null && tel.getSuperclass().getKind() == TypeKind.DECLARED) {
                original.add(((TypeElement) ((DeclaredType) tel.getSuperclass()).asElement()).getQualifiedName().toString());
            }

            for (TypeMirror intf : tel.getInterfaces()) {
                original.add(((TypeElement) ((DeclaredType) intf).asElement()).getQualifiedName().toString());
            }

            original.remove("java.lang.Object");
        } else {
            original = null;
        }

        if (!superFQNs.equals(original)) {
            if (hadObject) superFQNs.add("java.lang.Object");
            
            Set<Modifier> mods = EnumSet.noneOf(Modifier.class);

            mods.addAll(tree.getModifiers().getFlags());
            classes.put(clazz, mods);
            class2SuperElementTrees.put(clazz, superFQNs);
        }
    }

    private Set<String> superFQNs(ClassTree tree) {
        Set<String> superFQNs = new LinkedHashSet<String>(tree.getImplementsClause().size() + 1);

        if (tree.getExtendsClause() != null) {
            String fqn = fqnFor(tree.getExtendsClause());

            if (fqn != null) {
                superFQNs.add(fqn);
            }
        }

        for (Tree i : tree.getImplementsClause()) {
            String fqn = fqnFor(i);

            if (fqn != null) {
                superFQNs.add(fqn);
            }
        }

        return superFQNs;
    }

    public void registerPackage(String currentPackage) {
        packages.add(currentPackage);
    }

    public Iterable<? extends Element> getAllSuperElements(ASTService ast, Elements elements, Element forElement) {
        List<Element> result = new LinkedList<Element>();
        if (forElement instanceof FakeTypeElement) {
            for (String fqn : class2SuperElementTrees.get(((FakeTypeElement) forElement).fqnString)) {
                Element el = resolve(ast, elements, fqn);

                if (el != null) {
                    result.add(el);
                } else {
                    Logger.getLogger(ElementOverlay.class.getName()).log(Level.SEVERE, "Cannot resolve {0} to element", fqn);
                }
            }
        } else if (forElement.getKind().isClass() || forElement.getKind().isInterface()) {
            addElement(ast, elements, ((TypeElement) forElement).getSuperclass(), result);
            for (TypeMirror i : ((TypeElement) forElement).getInterfaces()) {
                addElement(ast, elements, i, result);
            }
        }

        return result;
    }

    private String fqnFor(Tree t) {
        Element el = ASTService.getElementImpl(t);

        if (el != null) {
            if (el.getKind().isClass() || el.getKind().isInterface() || el.getKind() == ElementKind.PACKAGE) {
                return ((QualifiedNameable) el).getQualifiedName().toString();
            } else {
                Logger.getLogger(ElementOverlay.class.getName()).log(Level.SEVERE, "Not a QualifiedNameable: {0}", el);
                return null;
            }
        } else if (t instanceof QualIdentTree) {
            return ((QualIdentTree) t).getFQN();
        } else if (t.getKind() == Kind.PARAMETERIZED_TYPE) {
            return fqnFor(((ParameterizedTypeTree) t).getType());
        } else {
            Logger.getLogger(ElementOverlay.class.getName()).log(Level.FINE, "No element and no QualIdent");
            return null;
        }
    }

    private void addElement(ASTService ast, Elements elements, TypeMirror tm, List<Element> result) {
        if (tm == null || tm.getKind() != TypeKind.DECLARED) {
            return;
        }
        
        String fqn = ((TypeElement) ((DeclaredType) tm).asElement()).getQualifiedName().toString();
        Element resolved = resolve(ast, elements, fqn);
        
        if (resolved != null) {
            result.add(resolved);
        } else {
            Logger.getLogger(ElementOverlay.class.getName()).log(Level.FINE, "cannot resolve {0}", fqn);
        }
    }

    public Element wrap(ASTService ast, Elements elements, Element original) {
        if (original == null) return null;

        if (original.getKind().isClass() || original.getKind().isInterface()) {
            return resolve(ast, elements, ((TypeElement)original).getQualifiedName().toString());
        } else {
            return original;
        }
    }

    private List<? extends Element> wrap(ASTService ast, Elements elements, Collection<? extends Element> original) {
        List<Element> result = new ArrayList<Element>(original.size());

        for (Element el : original) {
            Element wrapped = wrap(ast, elements, el);

            if (wrapped != null) { //may be null for classes living elsewhere (when original is from a package)
                result.add(wrapped);
            }
        }

        return result;
    }
    
    public void clearElementsCache() {
        elementCache.clear();
    }

    //for tests only:
    public int totalMapsSize() {
        return this.class2Enclosed.size() +
               this.class2SuperElementTrees.size() +
               this.classes.size() +
               this.elementCache.size() +
               this.packages.size();
    }

    public Collection<? extends Element> getAllVisibleThrough(ASTService ast, Elements elements, String what, ClassTree tree) {
        Collection<Element> result = new ArrayList<Element>();
        Element current = what != null ? resolve(ast, elements, what) : null;

        if (current == null) {
            //can happen if:
            //what == null: anonymous class
            //TODO: what != null: apparently happens for JDK (where the same class occurs twice on one source path)
            //might be possible to use ASTService.getElementImpl(tree) to thet the correct element
            //use only supertypes:
            //XXX: will probably not work correctly for newly created NCT (as the ClassTree does not have the correct extends/implements:
            for (String sup : superFQNs(tree)) {
                Element c = resolve(ast, elements, sup);

                if (c != null) {//may legitimely be null, e.g. if the super type is not resolvable at all.
                    result.add(c);
                }
            }
        } else {
            result.addAll(getAllMembers(ast, elements, current));
        }

        return result;
    }

    private Collection<? extends Element> getAllMembers(ASTService ast, Elements elements, Element el) {
        List<Element> result = new ArrayList<Element>();

        result.addAll(el.getEnclosedElements());

        for (Element parent : getAllSuperElements(ast, elements, el)) {
            if (!el.equals(parent)) {
                result.addAll(getAllMembers(ast, elements, parent));
            }
        }

        return result;
    }

    public PackageElement unnamedPackage(ASTService ast, Elements elements) {
        return (PackageElement) resolve(ast, elements, "");
    }

    private final class FakeTypeElement implements TypeElement {

        private final ASTService ast;
        private final Elements elements;
        private final Name simpleName;
        private final Name fqn;
        private final String fqnString;
        private final Element parent;
        private final Set<Modifier> mods;

        public FakeTypeElement(ASTService ast, Elements elements, Name simpleName, Name fqn, String fqnString, Element parent, Set<Modifier> mods) {
            this.ast = ast;
            this.elements = elements;
            this.simpleName = simpleName;
            this.fqn = fqn;
            this.fqnString = fqnString;
            this.parent = parent;
            this.mods = mods;
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
            return ElementOverlay.this.getEnclosedElements(ast, elements, fqnString);
        }

        @Override
        public NestingKind getNestingKind() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Name getQualifiedName() {
            return fqn;
        }

        @Override
        public TypeMirror getSuperclass() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<? extends TypeMirror> getInterfaces() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<? extends TypeParameterElement> getTypeParameters() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public TypeMirror asType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.CLASS;
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<Modifier> getModifiers() {
            return mods;
        }

        @Override
        public Name getSimpleName() {
            return simpleName;
        }

        @Override
        public Element getEnclosingElement() {
            return parent;
        }

        @Override
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    private final class TypeElementWrapper implements TypeElement {

        private final ASTService ast;
        private final Elements elements;
        private final TypeElement delegateTo;

        public TypeElementWrapper(ASTService ast, Elements elements, TypeElement delegateTo) {
            this.ast = ast;
            this.elements = elements;
            this.delegateTo = delegateTo;
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
            return wrap(ast, elements, delegateTo.getEnclosedElements());
        }

        @Override
        public NestingKind getNestingKind() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Name getQualifiedName() {
            return delegateTo.getQualifiedName();
        }

        @Override
        public TypeMirror getSuperclass() {
            return delegateTo.getSuperclass();
        }

        @Override
        public List<? extends TypeMirror> getInterfaces() {
            return delegateTo.getInterfaces();
        }

        @Override
        public List<? extends TypeParameterElement> getTypeParameters() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public TypeMirror asType() {
            return delegateTo.asType();
        }

        @Override
        public ElementKind getKind() {
            return delegateTo.getKind();
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<Modifier> getModifiers() {
            return delegateTo.getModifiers();
        }

        @Override
        public Name getSimpleName() {
            return delegateTo.getSimpleName();
        }

        @Override
        public Element getEnclosingElement() {
            return ElementOverlay.this.resolve(ast, elements, ((QualifiedNameable/*XXX*/) delegateTo.getEnclosingElement()).getQualifiedName().toString());
        }

        @Override
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    private class FakePackageElement implements PackageElement {

        private final ASTService ast;
        private final Elements elements;
        private final Name fqn;
        private final String fqnString;
        private final Name simpleName;

        public FakePackageElement(ASTService ast, Elements elements, Name fqn, String fqnString, Name simpleName) {
            this.ast = ast;
            this.elements = elements;
            this.fqn = fqn;
            this.fqnString = fqnString;
            this.simpleName = simpleName;
        }

        @Override
        public Name getQualifiedName() {
            return fqn;
        }

        @Override
        public boolean isUnnamed() {
            return false;
        }

        @Override
        public TypeMirror asType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.PACKAGE;
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<Modifier> getModifiers() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Name getSimpleName() {
            return simpleName;
        }

        @Override
        public Element getEnclosingElement() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
            //should delegate to pre-existing PackageElement, if available:
            return ElementOverlay.this.getEnclosedElements(ast, elements, fqnString);
        }

        @Override
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    private class PackageElementWrapper implements PackageElement {

        private final ASTService ast;
        private final Elements elements;
        private final PackageElement delegateTo;

        public PackageElementWrapper(ASTService ast, Elements elements, PackageElement delegateTo) {
            this.ast = ast;
            this.elements = elements;
            this.delegateTo = delegateTo;
        }

        @Override
        public Name getQualifiedName() {
            return delegateTo.getQualifiedName();
        }

        @Override
        public boolean isUnnamed() {
            return delegateTo.isUnnamed();
        }

        @Override
        public TypeMirror asType() {
            return delegateTo.asType();
        }

        @Override
        public ElementKind getKind() {
            return delegateTo.getKind();
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<Modifier> getModifiers() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Name getSimpleName() {
            return delegateTo.getSimpleName();
        }

        @Override
        public Element getEnclosingElement() {
            return null;
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
            return wrap(ast, elements, delegateTo.getEnclosedElements());
        }

        @Override
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    public static class FQNComputer {
        private final StringBuilder fqn = new StringBuilder();
        private int anonymousCounter = 0;
        public void setCompilationUnit(CompilationUnitTree cut) {
            setPackageNameTree(cut.getPackageName());
        }
        public void enterClass(ClassTree ct) {
            if (ct.getSimpleName() == null || ct.getSimpleName().length() == 0 || anonymousCounter > 0) {
                anonymousCounter++;
            } else {
                if (fqn.length() > 0) fqn.append('.');
                fqn.append(ct.getSimpleName());
            }
        }
        public void leaveClass() {
            if (anonymousCounter > 0) {
                anonymousCounter--;
            } else {
                int dot = Math.max(0, fqn.lastIndexOf("."));

                fqn.delete(dot, fqn.length());
            }
        }
        public String getFQN() {
            if (anonymousCounter > 0) return null;
            return fqn.toString();
        }

        public void setPackageNameTree(ExpressionTree packageNameTree) {
            fqn.delete(0, fqn.length());
            if (packageNameTree != null) {
                fqn.append(packageNameTree.toString()); //XXX: should not use toString
            }
        }
    }

    //TODO: need to have some kind of "transactions" for ElementOverlay handling, so that the whole Java refactoring
    //runs with the same instance of ElementOverlay. A real API to begin&end the "transaction" would be better
    @ServiceProvider(service=RefactoringPluginFactory.class, position=Integer.MIN_VALUE)
    public static class RunFirstFactory implements RefactoringPluginFactory {
        @Override public RefactoringPlugin createInstance(AbstractRefactoring refactoring) {
            return new RefactoringPlugin() {
                @Override public Problem preCheck() { return null; }
                @Override
                public Problem checkParameters() { return null; }

                @Override
                public Problem fastCheckParameters() { return null; }

                @Override
                public void cancelRequest() { endTransaction(); }

                @Override
                public Problem prepare(RefactoringElementsBag refactoringElements) {
                    beginTransaction();
                    return null;
                }
            };
        }
    }
    
    @ServiceProvider(service=RefactoringPluginFactory.class, position=Integer.MAX_VALUE)
    public static class RunLastFactory implements RefactoringPluginFactory {
        @Override public RefactoringPlugin createInstance(AbstractRefactoring refactoring) {
            return new RefactoringPlugin() {
                @Override public Problem preCheck() { return null; }
                @Override
                public Problem checkParameters() { return null; }

                @Override
                public Problem fastCheckParameters() { return null; }

                @Override
                public void cancelRequest() { endTransaction(); }

                @Override
                public Problem prepare(RefactoringElementsBag refactoringElements) {
                    endTransaction();
                    return null;
                }
            };
        }
    }
}
