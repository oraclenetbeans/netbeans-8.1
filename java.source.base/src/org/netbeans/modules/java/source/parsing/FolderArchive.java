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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaFileObject;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.modules.java.preprocessorbridge.spi.JavaFileFilterImplementation;
import org.netbeans.modules.java.source.indexing.JavaIndex;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import org.openide.util.BaseUtilities;

/**
 *
 * @author Tomas Zezula
 */
public class FolderArchive implements Archive {

    private static final Logger LOG = Logger.getLogger(FolderArchive.class.getName());
    private static final boolean normalize = Boolean.getBoolean("FolderArchive.normalize"); //NOI18N
    
    final File root;
    volatile Charset encoding;
    
    private boolean sourceRootInitialized;
    private URL sourceRoot;

    /** Creates a new instance of FolderArchive */
    public FolderArchive (final File root) {
        assert root != null;
        this.root = root;
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "creating FolderArchive for {0}", root.getAbsolutePath());
        }
    }
    
    private Charset encoding() {
        Charset e = encoding;
        if (e == null) {
            FileObject file = FileUtil.toFileObject(root);
            if (file != null) {
                e = FileEncodingQuery.getEncoding(file);
            } else {
                // avoid further checks
                e = UNKNOWN_CHARSET;
            }
            encoding = e;
        }
        return e != UNKNOWN_CHARSET ? e : null;
    }
    
    private static final Charset UNKNOWN_CHARSET = new Charset("UNKNOWN", null) {
        @Override
        public boolean contains(Charset cs) {
            throw new UnsupportedOperationException("Unexpected call");
        }

        @Override
        public CharsetDecoder newDecoder() {
            throw new UnsupportedOperationException("Unexpected call");
        }

        @Override
        public CharsetEncoder newEncoder() {
            throw new UnsupportedOperationException("Unexpected call");
        }
    };
    
    @Override
    public Iterable<JavaFileObject> getFiles(String folderName, ClassPath.Entry entry, Set<JavaFileObject.Kind> kinds, JavaFileFilterImplementation filter) throws IOException {
        assert folderName != null;
        if (folderName.length()>0) {
            folderName+='/';                                                                            //NOI18N
        }
        if (entry == null || entry.includes(folderName)) {
            File folder = new File (this.root, folderName.replace('/', File.separatorChar));      //NOI18N
            //Issue: #126392 on Mac
            //The problem when File ("A/").listFiles()[0].equals(new File("a/").listFiles[0]) returns false
            //Normalization is slow - turn on this workaround only for users which require it.
            //The problem only happens in case when there is file with wrong case in import.
            if (normalize) {
                folder = FileUtil.normalizeFile(folder);
            }
            final File[] content = folder.listFiles();
            if (content != null) {
                List<JavaFileObject> result = new ArrayList<>(content.length);
                for (File f : content) {
                    final JavaFileObject.Kind fKind = FileObjects.getKind(FileObjects.getExtension(f.getName()));
                    if ((kinds == null || kinds.contains(fKind)) &&
                        f.isFile() &&
                        (entry == null || entry.includes(BaseUtilities.toURI(f).toURL()))) {
                        result.add(FileObjects.fileFileObject(
                                f,
                                this.root,
                                filter,
                                fKind == JavaFileObject.Kind.CLASS ?
                                        UNKNOWN_CHARSET :
                                        encoding()));
                    }
                }
                return Collections.unmodifiableList(result);
            }
        }
        return Collections.<JavaFileObject>emptyList();
    }

    @Override
    public JavaFileObject create (String relativePath, final JavaFileFilterImplementation filter) throws UnsupportedOperationException {
        if (File.separatorChar != '/') {    //NOI18N
            relativePath = relativePath.replace('/', File.separatorChar);
        }
        final File file = new File (root, relativePath);
        return FileObjects.fileFileObject(file, root, filter, encoding());
    }

    @Override
    public void clear () {
    }

    @Override
    public JavaFileObject getFile(final @NonNull String name) {
        final String path = name.replace('/', File.separatorChar);        //NOI18N
        File file = new File (this.root, path);
        if (file.exists()) {
            return FileObjects.fileFileObject(file,this.root,null,encoding());
        }
        try {
            final URL srcRoot = getBaseSourceRoot(BaseUtilities.toURI(this.root).toURL());
            if (srcRoot != null && JavaIndex.hasSourceCache(srcRoot, false)) {
                if ("file".equals(srcRoot.getProtocol())) {         //NOI18N
                    final File folder = BaseUtilities.toFile(srcRoot.toURI());
                    file = new File (folder,path);
                    if (file.exists()) {
                        return FileObjects.fileFileObject(file,folder,null,encoding());
                    }
                } else {
                    final FileObject srcRootFo = URLMapper.findFileObject(srcRoot);
                    if (srcRootFo != null) {
                        final FileObject resource = srcRootFo.getFileObject(name);
                        if (resource != null) {
                            return  FileObjects.sourceFileObject(resource, srcRootFo);
                        }
                    }
                }
            } else {
                LOG.log(
                    Level.FINE,
                    "No source in: {0}.",    //NOI18N
                    srcRoot);
            }
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
        } catch (URISyntaxException e) {
            Exceptions.printStackTrace(e);
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format(
            "%s[folder: %s]",   //NOI18N
            getClass().getSimpleName(),
            root.getAbsolutePath()
        );
    }

    private URL getBaseSourceRoot(final URL binRoot) {
        synchronized (this) {
            if (sourceRootInitialized) {
                return sourceRoot;
            }
        }
        final URL tmpSourceRoot = JavaIndex.getSourceRootForClassFolder(binRoot);
        synchronized (this) {
            sourceRoot = tmpSourceRoot;
            sourceRootInitialized = true;
            return sourceRoot;
        }
    }
}
