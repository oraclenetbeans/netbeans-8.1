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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.source.indexing;

import com.sun.tools.javac.api.JavacTaskImpl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.modules.java.source.TreeLoader;
import org.netbeans.modules.java.source.base.Module;
import org.netbeans.modules.java.source.parsing.FileManagerTransaction;
import org.netbeans.modules.java.source.parsing.FileObjects;
import org.netbeans.modules.java.source.parsing.JavacParser;
import org.netbeans.modules.java.source.parsing.ProcessorGenerated;
import org.netbeans.modules.java.source.usages.BinaryAnalyser;
import org.netbeans.modules.java.source.usages.ClassIndexEventsTransaction;
import org.netbeans.modules.java.source.usages.ClassIndexImpl;
import org.netbeans.modules.java.source.usages.ClassIndexManager;
import org.netbeans.modules.java.source.usages.ClasspathInfoAccessor;
import org.netbeans.modules.parsing.impl.indexing.friendapi.IndexingController;
import org.netbeans.modules.parsing.spi.indexing.BinaryIndexer;
import org.netbeans.modules.parsing.spi.indexing.BinaryIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author Dusan Balek
 */
public class JavaBinaryIndexer extends BinaryIndexer {

    static final Logger LOG = Logger.getLogger(JavaBinaryIndexer.class.getName());

    private static final int CLEAN_ALL_LIMIT = 1000;

    @Override
    protected void index(final Context context) {
        LOG.log(Level.FINE, "index({0})", context.getRootURI());
        try {
            final ClassIndexManager cim = ClassIndexManager.getDefault();
            ClassIndexImpl uq = cim.createUsagesQuery(context.getRootURI(), false);
            if (uq == null) {
                return;    //IDE is exiting, indeces are already closed.
            }
 
            if (context.isAllFilesIndexing()) {
                final BinaryAnalyser ba = uq.getBinaryAnalyser();
                if (ba != null) { //ba == null => IDE is exiting, indexing will be done on IDE restart
                    final BinaryAnalyser.Changes changes = ba.analyse(context);
                    if (changes.done) {
                        final Map<URL, List<URL>> binDeps = IndexingController.getDefault().getBinaryRootDependencies();
                        final Map<URL, List<URL>> srcDeps = IndexingController.getDefault().getRootDependencies();
                        final Map<URL, List<URL>> peers = IndexingController.getDefault().getRootPeers();
                        final List<ElementHandle<TypeElement>> changed = new ArrayList<ElementHandle<TypeElement>>(changes.changed.size()+changes.removed.size());
                        changed.addAll(changes.changed);
                        changed.addAll(changes.removed);
                        if (!changes.changed.isEmpty() || !changes.added.isEmpty() || !changes.removed.isEmpty()) {
                            deleteSigFiles(context.getRootURI(), changed);
                            if (changes.preBuildArgs) {
                                preBuildArgs(javax.swing.JComponent.class.getName(), context.getRootURI());
                            }
                        }
                        final Map<URL,Set<URL>> toRebuild = JavaCustomIndexer.findDependent(context.getRootURI(), srcDeps, binDeps, peers, changed, !changes.added.isEmpty(), false);
                        for (Map.Entry<URL, Set<URL>> entry : toRebuild.entrySet()) {
                            context.addSupplementaryFiles(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        } catch (IllegalArgumentException iae) {
            Exceptions.printStackTrace(iae);
        } catch (IOException ioe) {
            Exceptions.printStackTrace(ioe);
        }
    }

    private static void deleteSigFiles(final URL root, final List<? extends ElementHandle<TypeElement>> toRemove) throws IOException {
        File cacheFolder = JavaIndex.getClassFolder(root);
        if (cacheFolder.exists()) {
            if (toRemove.size() > CLEAN_ALL_LIMIT) {
                //Todo: do as SlowIOTask
                FileObjects.deleteRecursively(cacheFolder);
            } else {
                for (ElementHandle<TypeElement> eh : toRemove) {
                    final StringBuilder sb = new StringBuilder(FileObjects.convertPackage2Folder(eh.getBinaryName(),File.separatorChar));
                    sb.append('.'); //NOI18N
                    sb.append(FileObjects.SIG);
                    final File f = new File (cacheFolder, sb.toString());
                    f.delete();
                }
            }
        }
    }
    
    public static void preBuildArgs(
        @NonNull final FileObject root,
        @NonNull final FileObject file) throws IOException {
        
        final String relativePath = 
            FileObjects.convertFolder2Package(
                FileObjects.stripExtension(
                    FileUtil.getRelativePath(root, file)));
        final TransactionContext txCtx = TransactionContext.beginTrans()
                .register(FileManagerTransaction.class, FileManagerTransaction.writeThrough())
                .register(ProcessorGenerated.class, ProcessorGenerated.nullWrite());
        try {
            final Collection<ClassPath.Entry> entries = JavaPlatform.getDefault().getBootstrapLibraries().entries();
            final URL[] roots = new URL[1+entries.size()];
            roots[0] = root.toURL();
            final Iterator<ClassPath.Entry> eit = entries.iterator();
            for (int i=1; eit.hasNext(); i++) {
                roots[i] = eit.next().getURL();
            }
            preBuildArgs(relativePath, roots);
        } finally {
            txCtx.commit();
        }
     }
    
    /**
     * Pre builds argument names for {@link javax.swing.JComponent} to speed up first
     * call of code completion on swing classes. Has no semantic impact only improves performance,
     * so it's can be safely disabled.
     * @param archiveFile the archive
     * @param archiveUrl URL of an archive
     */
    private static void preBuildArgs (
            @NonNull final String fqn,
            @NonNull final URL... archiveUrls) {
        class DevNullDiagnosticListener implements DiagnosticListener<JavaFileObject> {
            @Override
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Diagnostic reported during prebuilding args: {0}", diagnostic.toString()); //NOI18N
                }
            }
        }
        ClasspathInfo cpInfo = ClasspathInfoAccessor.getINSTANCE().create(
            ClassPathSupport.createClassPath(archiveUrls),
            ClassPathSupport.createClassPath(new URL[0]),
            ClassPathSupport.createClassPath(new URL[0]),
            null,
            true,
            true,
            false,
            false);
        final JavacTaskImpl jt = JavacParser.createJavacTask(cpInfo, new DevNullDiagnosticListener(), null, null, null, null, null, null);
        TreeLoader.preRegister(jt.getContext(), cpInfo, true);
        //Force JTImpl.prepareCompiler to get JTImpl into Context
        try {
            jt.parse(new JavaFileObject[0]);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        TypeElement jc = (TypeElement) jt.getElements().getTypeElementByBinaryName(fqn);
        if (jc != null) {
            List<ExecutableElement> methods = ElementFilter.methodsIn(jt.getElements().getAllMembers(jc));
            for (ExecutableElement method : methods) {
                List<? extends VariableElement> params = method.getParameters();
                if (!params.isEmpty()) {
                    params.get(0).getSimpleName();
                }
            }
        }
    }
    
    public static class Factory extends BinaryIndexerFactory {

        @Override
        public BinaryIndexer createIndexer() {
            return new JavaBinaryIndexer();
        }

        @Override
        public String getIndexerName() {
            return JavaIndex.NAME;
        }

        @Override
        public int getIndexVersion() {
            return JavaIndex.VERSION;
        }

        @Override
        public void rootsRemoved (final Iterable<? extends URL> removedRoots) {
            assert removedRoots != null;
            final TransactionContext txCtx = TransactionContext.beginTrans().register(
                ClassIndexEventsTransaction.class,
                ClassIndexEventsTransaction.create(false));
            try {
                final ClassIndexManager cim = ClassIndexManager.getDefault();
                for (URL removedRoot : removedRoots) {
                    cim.removeRoot(removedRoot);
                }
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            } finally {
                try {
                    if (Module.isClosed()) {
                        txCtx.rollBack();
                    } else {
                        txCtx.commit();
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        @Override
        public boolean scanStarted(final Context context) {
            try {
                TransactionContext.beginStandardTransaction(
                        context.getRootURI(),
                        false,
                        context.isAllFilesIndexing(),
                        context.checkForEditorModifications());
                final ClassIndexImpl uq = ClassIndexManager.getDefault().createUsagesQuery(context.getRootURI(), false);
                if (uq == null) {
                    //Closing...
                    return true;
                }
                if (uq.getState() != ClassIndexImpl.State.NEW) {
                    //Already checked
                    return true;
                }
                return uq.isValid();
            } catch (IOException ioe) {
                JavaIndex.LOG.log(Level.WARNING, "Exception while checking cache validity for root: "+context.getRootURI(), ioe); //NOI18N
                return false;
            }
        }

        @Override
        public void scanFinished(Context context) {
            final TransactionContext txCtx = TransactionContext.get();
            assert txCtx != null;
            try {
                if (context.isCancelled()) {
                    txCtx.rollBack();
                } else {
                    txCtx.commit();
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
