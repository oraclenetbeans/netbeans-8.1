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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.source.ui;

import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.model.JavacElements;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.swing.Icon;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.project.Project;
import org.netbeans.modules.java.source.ElementHandleAccessor;
import org.netbeans.modules.java.source.indexing.JavaIndex;
import org.netbeans.modules.java.source.parsing.CachingArchiveProvider;
import org.netbeans.modules.java.source.parsing.CachingFileManager;
import org.netbeans.modules.java.source.usages.ClassIndexImpl;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.jumpto.support.AsyncDescriptor;
import org.netbeans.spi.jumpto.support.DescriptorChangeEvent;
import org.netbeans.spi.jumpto.support.DescriptorChangeListener;
import org.netbeans.spi.jumpto.symbol.SymbolDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.BaseUtilities;
import org.openide.util.Exceptions;
import org.openide.util.Pair;
import org.openide.util.Parameters;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tomas Zezula
 */
final class AsyncJavaSymbolDescriptor extends JavaSymbolDescriptorBase implements AsyncDescriptor<SymbolDescriptor> {

    private static final RequestProcessor WORKER = new RequestProcessor(AsyncJavaSymbolDescriptor.class);
    private static final String INIT = "<init>"; //NOI18N
    private static final Logger LOG = Logger.getLogger(AsyncJavaSymbolDescriptor.class.getName());
    private static volatile boolean pkgROELogged = false;
    private static volatile boolean clzROELogged = false;

    private static Reference<JavacTaskImpl> javacRef;

    private final String ident;
    private final boolean caseSensitive;
    private final List<DescriptorChangeListener<SymbolDescriptor>> listeners;
    private final AtomicBoolean initialized;

    AsyncJavaSymbolDescriptor (
            @NullAllowed final Project project,
            @NonNull final FileObject root,
            @NonNull final ClassIndexImpl ci,
            @NonNull final ElementHandle<TypeElement> owner,
            @NonNull final String ident,
            final boolean caseSensitive) {
        super(owner, project, root, ci);
        assert ident != null;
        this.ident = ident;
        this.listeners = new CopyOnWriteArrayList<>();
        this.initialized = new AtomicBoolean();
        this.caseSensitive = caseSensitive;
    }

    @Override
    public Icon getIcon() {
        initialize();
        return null;
    }

    @Override
    public String getSymbolName() {
        initialize();
        return ident;
    }

    @Override
    public String getSimpleName() {
        return ident;
    }

    @Override
    public void open() {
        final Collection<? extends SymbolDescriptor> symbols = resolve();
        if (!symbols.isEmpty()) {
            symbols.iterator().next().open();
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 17;
        hashCode = hashCode * 31 + ident.hashCode();
        hashCode = hashCode * 31 + getRoot().hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AsyncJavaSymbolDescriptor)) {
            return false;
        }
        final AsyncJavaSymbolDescriptor other = (AsyncJavaSymbolDescriptor) obj;
        return caseSensitive == other.caseSensitive &&
               ident.equals(other.ident) &&
               getOwner().equals(other.getOwner()) &&
               getRoot().equals(other.getRoot());
    }

    private void initialize() {
        if (initialized.compareAndSet(false, true)) {
            final Runnable action = new Runnable() {
                @Override
                public void run() {
                    final Collection<? extends SymbolDescriptor> symbols = resolve();
                    fireDescriptorChange(symbols);
                }
            };
            WORKER.execute(action);
        }
    }

    @Override
    public void addDescriptorChangeListener(@NonNull final DescriptorChangeListener<SymbolDescriptor> listener) {
        Parameters.notNull("listener", listener);
        listeners.add(listener);
    }

    @Override
    public void removeDescriptorChangeListener(@NonNull final DescriptorChangeListener<SymbolDescriptor> listener) {
        Parameters.notNull("listener", listener);
        listeners.remove(listener);
    }

    @Override
    public boolean hasCorrectCase() {
        return caseSensitive;
    }

    private void fireDescriptorChange(Collection<? extends SymbolDescriptor> replacement) {
        final DescriptorChangeEvent<SymbolDescriptor> event = new DescriptorChangeEvent<>(
            this,
            replacement);
        for (DescriptorChangeListener<SymbolDescriptor> l : listeners) {
            l.descriptorChanged(event);
        }
    }

    @NonNull
    private Collection<? extends SymbolDescriptor> resolve() {
        try {
            final List<SymbolDescriptor> symbols = new ArrayList<>();
            final JavacTaskImpl jt = getJavac(getRoot());
//            final JavaFileManager fm = jt.getContext().get(JavaFileManager.class);
//            final ClassReader cr = ClassReader.instance(jt.getContext());
//            final Names names = Names.instance(jt.getContext());
//            final String binName = ElementHandleAccessor.getInstance().getJVMSignature(getOwner())[0];
//            final Name jcName = names.fromString(binName);
//            final Symbol.ClassSymbol te = cr.enterClass((com.sun.tools.javac.util.Name) jcName);
//            te.owner.completer = null;
//            te.classfile = fm.getJavaFileForInput(
//                StandardLocation.CLASS_PATH,
//                FileObjects.convertFolder2Package(binName),
//                JavaFileObject.Kind.CLASS);
            final ClassReader cr = ClassReader.instance(jt.getContext());
            final Set<?> pkgs = new HashSet<>(getPackages(cr).keySet());
            final Set<?> clzs = new HashSet<>(getClasses(cr).keySet());
            final JavacElements elements = jt.getElements();
            final TypeElement te = (TypeElement) elements.getTypeElementByBinaryName(
                    ElementHandleAccessor.getInstance().getJVMSignature(getOwner())[0]);
            if (te != null) {
                if (ident.equals(getSimpleName(te, null, caseSensitive))) {
                    final String simpleName = te.getSimpleName().toString();
                    final String simpleNameSuffix = null;
                    final ElementKind kind = te.getKind();
                    final Set<Modifier> modifiers = te.getModifiers();
                    final ElementHandle<?> me = ElementHandle.create(te);
                    symbols.add(new ResolvedJavaSymbolDescriptor(
                            AsyncJavaSymbolDescriptor.this,
                            simpleName,
                            simpleNameSuffix,
                            kind,
                            modifiers,
                            me));
                }
                for (Element ne : te.getEnclosedElements()) {
                    if (ident.equals(getSimpleName(ne, te, caseSensitive))) {
                        final Pair<String,String> name = JavaSymbolProvider.getDisplayName(ne, te);
                        final String simpleName = name.first();
                        final String simpleNameSuffix = name.second();
                        final ElementKind kind = ne.getKind();
                        final Set<Modifier> modifiers = ne.getModifiers();
                        final ElementHandle<?> me = ElementHandle.create(ne);
                        symbols.add(new ResolvedJavaSymbolDescriptor(
                                AsyncJavaSymbolDescriptor.this,
                                simpleName,
                                simpleNameSuffix,
                                kind,
                                modifiers,
                                me));
                    }
                }
            }
            getClasses(cr).keySet().retainAll(clzs);
            getPackages(cr).keySet().retainAll(pkgs);
            return symbols;
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
           return Collections.<SymbolDescriptor>emptyList();
        }
    }

    @NonNull
    private Map<?,?> getPackages(final ClassReader cr) {
        Map<?,?> res = Collections.emptyMap();
        try {
            final Field fld = ClassReader.class.getDeclaredField("packages");    //NOI18N
            fld.setAccessible(true);
            final Map<?,?> pkgs = (Map<?,?>) fld.get(cr);
            if (pkgs != null) {
                res = pkgs;
            }
        } catch (ReflectiveOperationException e) {
            if (!pkgROELogged) {
                LOG.warning(e.getMessage());
                pkgROELogged = true;
            }
        }
        return res;
    }

    @NonNull
    private Map<?,?> getClasses(final ClassReader cr) {
        Map<?,?> res = Collections.emptyMap();
        try {
            final Field fld = ClassReader.class.getDeclaredField("classes");    //NOI18N
            fld.setAccessible(true);
            Map<?,?> clzs = (Map<?,?>) fld.get(cr);
            if (clzs != null) {
                res = clzs;
            }
        } catch (ReflectiveOperationException e) {
            if (!clzROELogged) {
                LOG.warning(e.getMessage());
                clzROELogged = true;
            }
        }
        return res;
    }

    private static JavacTaskImpl getJavac(FileObject root) throws IOException {
        JavacTaskImpl javac;
        Reference<JavacTaskImpl> ref = javacRef;
        if (ref == null || (javac = ref.get()) == null) {
            javac = (JavacTaskImpl)JavacTool.create().getTask(
                    null,
                    new RootChange(),
                    new Listener(),
                    Collections.<String>emptySet(),
                    Collections.<String>emptySet(),
                    Collections.<JavaFileObject>emptySet());
            javacRef = new WeakReference<>(javac);
        }
        final RootChange rc = (RootChange) javac.getContext().get(JavaFileManager.class);
        rc.setRoot(root);
        return javac;
    }

    @NonNull
    private static String getSimpleName (
            @NonNull final Element element,
            @NullAllowed final Element enclosingElement,
            final boolean caseSensitive) {
        String result = element.getSimpleName().toString();
        if (enclosingElement != null && INIT.equals(result)) {
            result = enclosingElement.getSimpleName().toString();
        }
        if (!caseSensitive) {
            result = result.toLowerCase();
        }
        return result;
    }

    @ClientCodeWrapper.Trusted
    private static final class RootChange implements JavaFileManager {

        private JavaFileManager delegate;

        void setRoot(FileObject root) throws IOException {
            final File classes = JavaIndex.getClassFolder(root.toURL());
            final CachingFileManager fm = new CachingFileManager(
                    CachingArchiveProvider.getDefault(),
                    ClassPathSupport.createClassPath(BaseUtilities.toURI(classes).toURL()),
                    false,
                    true);
            this.delegate = fm;
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
            return delegate.getClassLoader(location);
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
            return delegate.list(location, packageName, kinds, recurse);
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            return delegate.inferBinaryName(location, file);
        }

        @Override
        public boolean isSameFile(javax.tools.FileObject a, javax.tools.FileObject b) {
            return delegate.isSameFile(a, b);
        }

        @Override
        public boolean handleOption(String current, Iterator<String> remaining) {
            return delegate.handleOption(current, remaining);
        }

        @Override
        public boolean hasLocation(Location location) {
            return delegate.hasLocation(location);
        }

        @Override
        public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
            return delegate.getJavaFileForInput(location, className, kind);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, javax.tools.FileObject sibling) throws IOException {
            return delegate.getJavaFileForOutput(location, className, kind, sibling);
        }

        @Override
        public javax.tools.FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
            return delegate.getFileForInput(location, packageName, relativeName);
        }

        @Override
        public javax.tools.FileObject getFileForOutput(Location location, String packageName, String relativeName, javax.tools.FileObject sibling) throws IOException {
            return delegate.getFileForOutput(location, packageName, relativeName, sibling);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public int isSupportedOption(String option) {
            return delegate.isSupportedOption(option);
        }
    }

    private static final class Listener implements DiagnosticListener<JavaFileObject> {
        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        }
    }
}
