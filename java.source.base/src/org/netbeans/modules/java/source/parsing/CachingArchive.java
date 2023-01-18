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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;
import javax.tools.JavaFileObject;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.java.preprocessorbridge.spi.JavaFileFilterImplementation;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

public class CachingArchive implements Archive, FileChangeListener {

    private static final Logger LOGGER = Logger.getLogger(CachingArchive.class.getName());

    private final File archiveFile;
    private final boolean keepOpened;
    private final String pathToRootInArchive;
    private ZipFile zipFile;

    //@GuardedBy("this")
    byte[] names;// = new byte[16384];
    private int nameOffset = 0;
    final static int[] EMPTY = new int[0];
    //@GuardedBy("this")
    private Map<String, Folder> folders; // = new HashMap<String, Folder>();

        // Constructors ------------------------------------------------------------

    /** Creates a new instance of archive from zip file */
    public CachingArchive(
            @NonNull final File archiveFile,
            final boolean keepOpened) {
        this(archiveFile, null, keepOpened);
    }

    public CachingArchive(
            @NonNull final File archiveFile,
            @NullAllowed final String pathToRootInArchive,
            final boolean keepOpened) {
        Parameters.notNull("archiveFile", archiveFile); //NOI18N
        if (pathToRootInArchive != null) {
            if (!keepOpened) {
                throw new UnsupportedOperationException(String.format(
                    "FastJar not supported for relocated root of archive %s, relocation %s",    //NOI18N
                    archiveFile.getAbsolutePath(),
                    pathToRootInArchive));
            }
            if (pathToRootInArchive.charAt(pathToRootInArchive.length()-1) != FileObjects.NBFS_SEPARATOR_CHAR) {
                throw new IllegalArgumentException(String.format(
                    "Path to root: %s has to end with /",   //NOI18N
                    pathToRootInArchive));
            }
        }
        this.archiveFile = archiveFile;
        this.pathToRootInArchive = pathToRootInArchive;
        this.keepOpened = keepOpened;

        FileUtil.addFileChangeListener(this, FileUtil.normalizeFile(archiveFile));
    }

    // Archive implementation --------------------------------------------------

    /** Gets all files in given folder
     */
    @Override
    public Iterable<JavaFileObject> getFiles( String folderName, ClassPath.Entry entry, Set<JavaFileObject.Kind> kinds, JavaFileFilterImplementation filter ) throws IOException {
        Map<String, Folder> folders = doInit();
        Folder files = folders.get( folderName );
        if (files == null) {
            return Collections.<JavaFileObject>emptyList();
        }
        else {
            assert !keepOpened || zipFile != null;
            List<JavaFileObject> l = new ArrayList<>(files.idx / files.delta);
            final Predicate<String> predicate = kinds == null ? new Tautology() : new HasKind(kinds);
            for (int i = 0; i < files.idx; i += files.delta){
                final JavaFileObject fo = create(folderName, files, i, predicate);
                if (fo != null) {
                    l.add(fo);
                }
            }
            return l;
        }
    }

    @Override
    public JavaFileObject create (final String relativePath, final JavaFileFilterImplementation filter) {
        throw new UnsupportedOperationException("Write into archives not supported");   //NOI18N
    }

    @Override
    public synchronized void clear () {
        folders = null;
        names = null;
        nameOffset = 0;
    }

    @Override
    public JavaFileObject getFile(final @NonNull String name) {
        Map<String, Folder> folders = doInit();
        final int index = name.lastIndexOf(FileObjects.NBFS_SEPARATOR_CHAR);
        String folder, sn;
        if (index<=0) {
            folder = "";    //NOI18N
            sn = name;
        } else {
            folder = name.substring(0,index);
            sn = name.substring(index+1);
        }
        Folder files = folders.get(folder);
        if (files == null) {
            return null;
        }
        else {
            assert !keepOpened || zipFile != null;
            final Predicate<String> predicate = new NameIs(sn);
            for (int i = 0; i < files.idx; i += files.delta){
                final JavaFileObject fo = create(folder, files, i, predicate);
                if (fo != null) {
                    return fo;
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format(
            "%s[archive: %s]",   //NOI18N
            getClass().getSimpleName(),
            archiveFile.getAbsolutePath());
    }
    //Protected methods --------------------------------------------------------
    protected void beforeInit() throws IOException {
    }

    protected short getFlags(@NonNull final String dirname) throws IOException {
        return 0;
    }

    protected boolean includes(final int flags, final String folder, final String name) {
        return true;
    }

    protected void afterInit(boolean success) throws IOException {
    }

    protected ZipFile getArchive(short flags) {
        return zipFile;
    }

    protected String getPathToRoot(short flags) {
        return pathToRootInArchive;
    }
    // Private methods ---------------------------------------------------------

    /*test*/ synchronized Map<String, Folder> doInit() {
        if (folders == null) {
            try {
                boolean success = false;
                beforeInit();
                try {
                    names = new byte[16384];
                    folders = createMap(archiveFile);
                    trunc();
                    success = true;
                } finally {
                    afterInit(success);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Broken zip file: {0}", archiveFile.getAbsolutePath());
                LOGGER.log(Level.FINE, null, e);
                names = new byte[0];
                nameOffset = 0;
                folders = new HashMap<>();

                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING, "Cannot close archive: {0}", archiveFile.getAbsolutePath());
                        LOGGER.log(Level.FINE, null, ex);
                    }
                }
            }
        }

        return folders;
    }

    private void trunc() {
        assert Thread.holdsLock(this);
        // strip the name array:
        byte[] newNames = new byte[nameOffset];
        System.arraycopy(names, 0, newNames, 0, nameOffset);
        names = newNames;

        // strip all the indices arrays:
        for (Iterator it = folders.values().iterator(); it.hasNext();) {
            ((Folder) it.next()).trunc();
        }
    }

    @NbBundle.Messages({
    "# {0} - the ZIP filename",
    "ERR_CorruptedZipFile=The ZIP file {0} is either corrupted, or is being built by an external process. Some entries may not be accessible"
    })
    private Map<String,Folder> createMap(File file ) throws IOException {
        if (!file.canRead()) {
            return Collections.<String, Folder>emptyMap();
        }
        Map<String,Folder> map = null;
        if (!keepOpened) {
            map = new HashMap<>();
            try {
                Iterable<? extends FastJar.Entry> e = FastJar.list(file);
                for (FastJar.Entry entry : e) {
                    String name = entry.name;
                    int i = name.lastIndexOf(FileObjects.NBFS_SEPARATOR_CHAR);
                    String dirname = i == -1 ? "" : name.substring(0, i /* +1 */);
                    String basename = name.substring(i+1);
                    if (basename.length() == 0) {
                        basename = null;
                    }
                    Folder fld = map.get(dirname);
                    if (fld == null) {
                        fld = new Folder (true, getFlags(dirname));
                        map.put(dirname.intern(), fld);
                    }
                    if ( basename != null ) {
                        fld.appendEntry(this, basename, entry.getTime(), entry.offset);
                    }
                }
            } catch (IOException ioe) {
                map = null;
                Logger.getLogger(CachingArchive.class.getName()).log(Level.WARNING, "Fallback to ZipFile: {0}", file.getPath());       //NOI18N
            }
        }
        if (map == null) {
            map = new HashMap<>();
            ZipFile zip = new ZipFile (file);
            try {
                for ( Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry;
                    try {
                        //May throw IllegalArgumentException
                        entry = e.nextElement();
                    } catch (IllegalArgumentException iae) {
                        throw new IOException(iae);
                    } catch (ZipError ze) {
                        // the JAR may be corrupted somehow; no further entry read
                        // will probably succeed, so just skip the rest of the jar.
                        Exceptions.printStackTrace(
                                Exceptions.attachLocalizedMessage(
                                Exceptions.attachSeverity(ze, Level.WARNING),
                                Bundle.ERR_CorruptedZipFile(file)));
                        break;
                    }
                    String name = entry.getName();
                    String dirname;
                    String basename;
                    if (pathToRootInArchive != null) {
                        if (!name.startsWith(pathToRootInArchive)) {
                            continue;
                        }
                        final int i = name.lastIndexOf(FileObjects.NBFS_SEPARATOR_CHAR);
                        dirname = i < pathToRootInArchive.length() ?
                                "" :    //NOI18N
                                name.substring(pathToRootInArchive.length(), i);
                        basename = name.substring(i+1);
                    } else {
                        final int i = name.lastIndexOf(FileObjects.NBFS_SEPARATOR_CHAR);
                        dirname = i == -1 ? "" : name.substring(0, i);
                        basename = name.substring(i+1);
                    }
                    if (basename.length() == 0) {
                        basename = null;
                    }
                    Folder fld = map.get(dirname);
                    if (fld == null) {
                        fld = new Folder(false, getFlags(dirname));
                        map.put(dirname.intern(), fld);
                    }

                    if ( basename != null && includes(fld.flags, dirname, basename)) {
                        fld.appendEntry(this, basename, entry.getTime(),-1);
                    }
                }
            } finally {
                if (keepOpened) {
                    this.zipFile = zip;
                }
                else {
                    try {
                        zip.close();
                    } catch (IOException ioe) {
                        Exceptions.printStackTrace(ioe);
                    }
                }
            }
        }
        return map;
    }

    private synchronized String getString(int off, int len) {
        if (names == null) {
            return null;
        }
        byte[] name = new byte[len];
        System.arraycopy(names, off, name, 0, len);
        try {
            return new String(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("No UTF-8");
        }
    }

    /*test*/ static long join(int higher, int lower) {
        return (((long)higher) << 32) | (((long) lower) & 0xFFFFFFFFL);
    }

    private JavaFileObject create(
            final @NonNull String pkg,
            final @NonNull Folder f,
            final @NonNull int off,
            final @NonNull Predicate<String> predicate) {
        String baseName = getString(f.indices[off], f.indices[off+1]);
        if (baseName != null && predicate.apply(baseName)) {
            long mtime = join(f.indices[off+3], f.indices[off+2]);
            if (zipFile == null) {
                if (f.delta == 4) {
                    return FileObjects.zipFileObject(archiveFile, pkg, baseName, mtime);
                } else {
                    assert f.delta == 6;
                    long offset = join(f.indices[off+5], f.indices[off+4]);
                    return FileObjects.zipFileObject(archiveFile, pkg, baseName, mtime, offset);
                }
            } else {
                return FileObjects.zipFileObject(
                    getArchive(f.flags),
                    getPathToRoot(f.flags),
                    pkg,
                    baseName,
                    mtime);
            }
        }
        return null;
    }

    /*test*/ synchronized int putName(byte[] name) {
        int start = nameOffset;

        if ((start + name.length) > names.length) {
            byte[] newNames = new byte[(names.length * 2) + name.length];
            System.arraycopy(names, 0, newNames, 0, start);
            names = newNames;
        }

        System.arraycopy(name, 0, names, start, name.length);
        nameOffset += name.length;

        return start;
    }

    @Override
    public void fileFolderCreated(FileEvent fe) { }

    @Override
    public void fileDataCreated(FileEvent fe) {
        clear();
    }

    @Override
    public void fileChanged(FileEvent fe) {
        clear();
    }

    @Override
    public void fileDeleted(FileEvent fe) {
        clear();
    }

    @Override
    public void fileRenamed(FileRenameEvent fe) {
        clear();
    }

    @Override
    public void fileAttributeChanged(FileAttributeEvent fe) { }

    // Innerclasses ------------------------------------------------------------
    private static class Folder {
        int[] indices = EMPTY; // off, len, mtimeL, mtimeH
        int idx = 0;
        final short flags;
        final short delta;

        public Folder(
            final boolean fastJar,
            final short flags) {
            if (fastJar) {
                delta = 6;
            }
            else {
                delta = 4;
            }
            this.flags = flags;
        }

        void appendEntry(CachingArchive outer, String name, long mtime, long offset) {
            // ensure enough space
            if ((idx + delta) > indices.length) {
                int[] newInd = new int[(2 * indices.length) + delta];
                System.arraycopy(indices, 0, newInd, 0, idx);
                indices = newInd;
            }

            try {
                byte[] bytes = name.getBytes("UTF-8");
                indices[idx++] = outer.putName(bytes);
                indices[idx++] = bytes.length;
                indices[idx++] = (int)(mtime & 0xFFFFFFFF);
                indices[idx++] = (int)(mtime >> 32);
                if (delta == 6) {
                    indices[idx++] = (int)(offset & 0xFFFFFFFF);
                    indices[idx++] = (int)(offset >> 32);
                }
            } catch (UnsupportedEncodingException e) {
                throw new InternalError("No UTF-8");
            }
        }

        void trunc() {
            if (indices.length > idx) {
                int[] newInd = new int[idx];
                System.arraycopy(indices, 0, newInd, 0, idx);
                indices = newInd;
            }
        }
    }

    private static interface Predicate<T> {
        boolean apply(@NonNull T value);
    }

    private static class HasKind implements Predicate<String> {

        private final Set<JavaFileObject.Kind> kinds;

        private HasKind(final @NonNull Set<JavaFileObject.Kind> kinds) {
            Parameters.notNull("kinds", kinds); //NOI18N
            this.kinds = kinds;
        }

        @Override
        public boolean apply(final @NonNull String value) {
            return kinds.contains(FileObjects.getKind(FileObjects.getExtension(value)));
        }
    }

    private static class NameIs implements Predicate<String> {

        private final String name;

        private NameIs (final @NonNull String name) {
            Parameters.notNull("name", name);   //NOI18N
            this.name = name;
        }

        @Override
        public boolean apply(final @NonNull String value) {
            return name.equals(value);
        }
    }

    private static class Tautology implements Predicate<String> {
        @Override
        public boolean apply(final @NonNull String value) {
            return true;
        }

    }


}
