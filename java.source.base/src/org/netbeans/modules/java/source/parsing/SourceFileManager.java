/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.java.source.parsing;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.java.source.indexing.TransactionContext;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Parameters;
import org.openide.util.BaseUtilities;

/**
 *
 * @author Tomas Zezula
 */
public class SourceFileManager implements JavaFileManager {
    
    final ClassPath sourceRoots;
    private final boolean ignoreExcludes;
    private static final Logger LOG = Logger.getLogger(SourceFileManager.class.getName());
    private static final ModifiedFiles modifiedFiles = new ModifiedFiles();
    
    /** Creates a new instance of SourceFileManager */
    public SourceFileManager (final ClassPath sourceRoots, final boolean ignoreExcludes) {
        this.sourceRoots = sourceRoots;
        this.ignoreExcludes = ignoreExcludes;
    }

    @Override
    public Iterable<JavaFileObject> list(final Location l, final String packageName, final Set<JavaFileObject.Kind> kinds, final boolean recursive) {
        //Todo: Caching of results, needs listening on FS
        List<JavaFileObject> result = new ArrayList<JavaFileObject> ();
        String _name = packageName.replace('.','/');    //NOI18N
        if (_name.length() != 0) {
            _name+='/';                                 //NOI18N
        }
        for (ClassPath.Entry entry : this.sourceRoots.entries()) {
            if (ignoreExcludes || entry.includes(_name)) {
                FileObject root = entry.getRoot();
                if (root != null) {
                    FileObject tmpFile = root.getFileObject(_name);
                    if (tmpFile != null && tmpFile.isFolder()) {
                        Enumeration<? extends FileObject> files = tmpFile.getChildren (recursive);
                        while (files.hasMoreElements()) {
                            FileObject file = files.nextElement();
                            if (ignoreExcludes || entry.includes(file)) {
                                final JavaFileObject.Kind kind = FileObjects.getKind(file.getExt());                                
                                if (kinds.contains(kind)) {                        
                                    result.add (FileObjects.sourceFileObject(file, root));
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public javax.tools.FileObject getFileForInput (final Location l, final String pkgName, final String relativeName) {
        final String rp = FileObjects.resolveRelativePath (pkgName, relativeName);
        final FileObject[] fileRootPair = findFile(rp);
        return fileRootPair == null ? null : FileObjects.sourceFileObject(fileRootPair[0], fileRootPair[1]);
    }

    @Override
    public JavaFileObject getJavaFileForInput (Location l, final String className, JavaFileObject.Kind kind) {
        String[] namePair = FileObjects.getParentRelativePathAndName (className);
        String ext = kind == JavaFileObject.Kind.CLASS ? FileObjects.SIG : kind.extension.substring(1);   //tzezula: Clearly wrong in compile on save, but "class" is also wrong
        for (ClassPath.Entry entry : this.sourceRoots.entries()) {
            FileObject root = entry.getRoot();
            if (root != null) {
                FileObject parent = root.getFileObject(namePair[0]);
                if (parent != null) {
                    FileObject[] children = parent.getChildren();
                    for (FileObject child : children) {
                        if (namePair[1].equals(child.getName()) && ext.equalsIgnoreCase(child.getExt()) && (ignoreExcludes || entry.includes(child))) {
                            return FileObjects.sourceFileObject(child, root);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public javax.tools.FileObject getFileForOutput(final Location l, final String pkgName, final String relativeName, final javax.tools.FileObject sibling)
        throws IOException, UnsupportedOperationException, IllegalArgumentException {
        if (StandardLocation.SOURCE_PATH != l) {
            throw new UnsupportedOperationException("Only StandardLocation.SOURCE_PATH is supported."); // NOI18N
        }
        final String rp = FileObjects.resolveRelativePath (pkgName, relativeName);
        final FileObject[] fileRootPair = findFile(rp);
        if (fileRootPair == null) {
            final FileObject[] roots = this.sourceRoots.getRoots();
            if (roots.length == 0) {
                throw new UnsupportedOperationException("No source path");  //NOI18N
            }
            final File rootFile = FileUtil.toFile(roots[0]);
            if (rootFile == null) {
                throw new UnsupportedOperationException("No source path");  //NOI18N
            }
            return FileObjects.sourceFileObject(
                BaseUtilities.toURI(new File(rootFile,FileObjects.convertFolder2Package(rp, File.separatorChar))).toURL(),
                roots[0]);
        } else {
            return FileObjects.sourceFileObject(fileRootPair[0], fileRootPair[1]); //Todo: wrap to protect from write
        }
    }

    @Override
    public JavaFileObject getJavaFileForOutput (Location l, String className, JavaFileObject.Kind kind, javax.tools.FileObject sibling)
        throws IOException, UnsupportedOperationException, IllegalArgumentException {
        throw new UnsupportedOperationException("The SourceFileManager does not support write operations."); // NOI18N
    }       
    
    @Override
    public void flush() throws java.io.IOException {
        //Nothing to do
    }

    @Override
    public void close() throws java.io.IOException {
        //Nothing to do
    }            
    
    @Override
    public int isSupportedOption(String string) {
        return -1;
    }
    
    @Override
    public boolean handleOption (final String head, final Iterator<String> tail) {
        return false;
    }
 
    @Override
    public boolean hasLocation(Location location) {
        return true;
    }
       
    @Override
    public ClassLoader getClassLoader (Location l) {
        return null;
    }

    @Override
    public String inferBinaryName (final Location l, final JavaFileObject jfo) {
        try {
            if (jfo instanceof InferableJavaFileObject) {
                final String result = ((InferableJavaFileObject)jfo).inferBinaryName();
                if (result != null) {
                    return result;
                }
            }
            FileObject fo = URLMapper.findFileObject(jfo.toUri().toURL());
            FileObject root = null;
            if (root == null) {
                for (FileObject rc : this.sourceRoots.getRoots()) {
                    if (FileUtil.isParentOf(rc,fo)) {
                        root = rc;
                    }
                }
            }

            if (root != null) {
                String relativePath = FileUtil.getRelativePath(root,fo);
                int index = relativePath.lastIndexOf('.');
                assert index > 0;
                final String result = relativePath.substring(0,index).replace('/','.');
                return result;
            }
        } catch (MalformedURLException e) {
            if (LOG.isLoggable(Level.SEVERE))
                LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean isSameFile(javax.tools.FileObject a, javax.tools.FileObject b) {
        return
            a instanceof AbstractSourceFileObject  &&
            b instanceof AbstractSourceFileObject &&
            ((AbstractSourceFileObject)a).getHandle().file != null &&
            ((AbstractSourceFileObject)a).getHandle().file.equals(
                ((AbstractSourceFileObject)b).getHandle().file);
    }

    private FileObject[] findFile (final String relativePath) {
        for (ClassPath.Entry entry : this.sourceRoots.entries()) {
            if (ignoreExcludes || entry.includes(relativePath)) {
                FileObject root = entry.getRoot();
                if (root != null) {
                    FileObject file = root.getFileObject(relativePath);
                    if (file != null) {
                        return new FileObject[] {file, root};
                    }
                }
            }
        }
        return null;
    }

    public static final class ModifiedFiles {

        private final Object lock = new  Object();
        //@GuardedBy("lock")
        private final Set<URI> files = new HashSet<URI>();
        //@GuardedBy("lock")
        private Set<URI> addedFiles;
        //@GuardedBy("lock")
        private Set<URI> removedFiles;

        private ModifiedFiles() {}
        
        private void beginUpdate() {
            synchronized (lock) {
                LOG.fine("beginUpdate");    //NOI18N
                assert addedFiles == null;
                assert removedFiles == null;
                addedFiles = new HashSet<>();
                removedFiles = new HashSet<>();
            }
        }
       
        private void commitUpdate() {
            synchronized(lock) {
                LOG.fine("commitUpdate");   //NOI18N
                assert addedFiles != null;
                assert removedFiles != null;
                files.removeAll(removedFiles);
                files.addAll(addedFiles);
                addedFiles = null;
                removedFiles = null;
            }
        }

        private void rollBackUpdate() throws IOException {
            synchronized(lock) {
                LOG.fine("rollBackUpdate"); //NOI18N
                addedFiles = null;
                removedFiles = null;
            }
        }

        private void cacheUpdated(@NonNull final URI file) {
            Parameters.notNull("file", file);   //NOI18N
            synchronized (lock) {
                LOG.log(
                    Level.FINE,
                    "cacheUpdated: {0}",    //NOI18N
                    file);
                assert removedFiles != null;
                removedFiles.add(file);
            }
            
        }

        public void fileModified(@NonNull final URI file) {
            Parameters.notNull("file", file);   //NOI18N
            synchronized (lock) {
                LOG.log(
                    Level.FINE,
                    "fileModified: {0}",    //NOI18N
                    file);
                final Set<URI> addInto = addedFiles != null ? addedFiles : files;
                addInto.add(file);
            }
        }

        public boolean isModified(@NonNull final URI file) {
            Parameters.notNull("file", file);   //NOI18N
            synchronized (lock) {
                final boolean res = files.contains(file) ||
                    (addedFiles != null && addedFiles.contains(file));
                LOG.log(
                    Level.FINE,
                    "isModified: {0} -> {1}",   //NOI18N
                    new Object[]{
                        file,
                        res
                    });
                return res;
            }
        }
    }

    public static abstract class ModifiedFilesTransaction extends TransactionContext.Service {
        public  abstract void cacheUpdated(@NonNull final URI file);
        abstract void begin();
    }
    
    private static final class PermanentSourceScan extends ModifiedFilesTransaction {

        private final ModifiedFiles delegate;

        private PermanentSourceScan(@NonNull final ModifiedFiles delegate) {
            Parameters.notNull("delegate", delegate);   //NOI18N
            this.delegate = delegate;
        }

        public  void cacheUpdated(@NonNull final URI file) {
            delegate.cacheUpdated(file);
        }

        @Override
        void begin() {
            delegate.beginUpdate();
        }

        @Override
        protected void commit() throws IOException {
            delegate.commitUpdate();
        }

        @Override
        protected void rollBack() throws IOException {
            delegate.rollBackUpdate();
        }
    }

    private  static final class TransientSourceScan extends ModifiedFilesTransaction {

        @Override
        public void cacheUpdated(URI file) {
        }

        @Override
        void begin() {
        }

        @Override
        protected void commit() throws IOException {
        }

        @Override
        protected void rollBack() throws IOException {
        }
    }

    public static ModifiedFiles getModifiedFiles() {        
        return modifiedFiles;
    }

    public static ModifiedFilesTransaction newModifiedFilesTransaction(
            final boolean srcIndex,
            final boolean checkForEditorModifications) {
        final ModifiedFilesTransaction tx = (srcIndex && !checkForEditorModifications) ?
                new PermanentSourceScan(modifiedFiles) :
                new TransientSourceScan();
        tx.begin();
        return tx;
    }

}
