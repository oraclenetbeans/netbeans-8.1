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
package org.netbeans.modules.versioning.core.api;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.extexecution.ProcessBuilder;
import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.modules.versioning.core.APIAccessor;
import org.netbeans.modules.versioning.core.FlatFolder;
import org.netbeans.modules.versioning.core.Utils;
import org.netbeans.modules.versioning.core.filesystems.VCSFileProxyOperations;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

/**
 * Represents a file on a file system. 
 *
 * @author Alexander Simon
 * @author Tomas Stupka
 */
public final class VCSFileProxy {

    private final String path;
    private final VCSFileProxyOperations proxy;
    
    /**
     * Flag if this file is to be handled as a flat folder. 
     * @see NonRecursiveFolder
     */
    private boolean isFlat = false;
    
    /**
     * Cache if a file is a directory. We know that in case of an io.File based FileObject 
     * this value is already cached as well so we are able to avoid unnecessary io access.
     */
    private Boolean isDirectory = null; // XXX might change for a file!!!
    
    /**
     * Listen on the FileObject this VCSFileProxy was created from. Some fields are cached 
     * (e.g. isDirectory) and changes in the FileObject-s state should be reflected accordingly.
     */
    private FileChangeListener fileChangeListener = null;
    
    static {
        APIAccessor.IMPL = new APIAccessorImpl();
    }
    
    private VCSFileProxy(String path, VCSFileProxyOperations proxy) {
        this.path = path;
        this.proxy = proxy;
    }
    
    /**
     * Creates a VCSFileProxy based on io.File.
     * 
     * @param file the file to be represented by VCSFileProxy
     * @return a VCSFileProxy representing the given file
     */
    public static VCSFileProxy createFileProxy(File file) {
        VCSFileProxy p = new VCSFileProxy(file.getAbsolutePath(), null);
        if(file instanceof FlatFolder) {
            p.setFlat(true);
        }
        return p;
    }

    /**
     * Creates a new VCSFileProxy from the given parent and child name
     * 
     * @param parent the parent file
     * @param child the child name
     * @return a VCSFileProxy representing the a file given by the parent and child values
     */
    public static VCSFileProxy createFileProxy(VCSFileProxy parent, String child) {
        File parentFile = parent.toFile();
        if (parentFile != null) {
            return createFileProxy(new File(parentFile, child));
        } else {
            final String p = parent.getPath();
            if (p.endsWith("/")) {   // NOI18N
                return new VCSFileProxy(p + child, parent.proxy);
            } else {
                if (p.isEmpty() && child.startsWith("/")) {
                    return new VCSFileProxy(child, parent.proxy);   // NOI18N
                } else {
                    return new VCSFileProxy(p + "/" + child, parent.proxy);   // NOI18N
                }
            }
        }
    }
    
    static VCSFileProxy createFileProxy(String path) {
        return new VCSFileProxy(path, null);
    }
    
    static VCSFileProxy createFileProxy(VCSFileProxy parent, String child, boolean isDirectory) {
        VCSFileProxy file = createFileProxy(parent, child);
        file.isDirectory = isDirectory;
        return file;
    }

    public static VCSFileProxy createFileProxy(URI uri) {
        if (uri.getScheme().equals("file")) { //NOI18N
            return createFileProxy(Utilities.toFile(uri));
        } else {
            VCSFileProxyOperations fileProxyOperations = getFileProxyOperations(uri);
            if (fileProxyOperations != null) {
                VCSFileProxy file = new VCSFileProxy(uri.getPath(), fileProxyOperations);
                return file;
            }
            return null;
        }
    }

    /**
     * Creates a VCSFileProxy based on the given {@link FileObject}. In case there is a 
     * corresponding java.io.File to the FileObject the the io.File will be used as in 
     * {@link #createFileProxy(java.io.File)}.
     * 
     * @param fileObject the file to be represented by VCSFileProxy
     * @return a VCSFileProxy representing the given file or null if the given 
     * FileObject-s Filesystem isn't supported - e.g. jar filesystem. 
     */
    public static VCSFileProxy createFileProxy(final FileObject fileObject) {        
        try {
            VCSFileProxyOperations fileProxyOperations = getFileProxyOperations(fileObject.getFileSystem());
            if (fileProxyOperations == null) {
                File file = FileUtil.toFile(fileObject);
                if(file != null) {
                    final VCSFileProxy p = createFileProxy(file);
                    p.isDirectory = fileObject.isFolder();
                    p.fileChangeListener = new FileChangeListener() {
                        @Override public void fileDeleted(FileEvent fe) {
                            p.isDirectory = null;
                        }
                        @Override public void fileFolderCreated(FileEvent fe) { }
                        @Override public void fileDataCreated(FileEvent fe) { }
                        @Override public void fileChanged(FileEvent fe) { }
                        @Override public void fileRenamed(FileRenameEvent fe) { }
                        @Override public void fileAttributeChanged(FileAttributeEvent fe) { }
                    };
                    Runnable run = new Runnable() {

                        @Override
                        public void run () {
                            fileObject.addFileChangeListener(WeakListeners.create(FileChangeListener.class, p.fileChangeListener, fileObject));
                        }
                    };
                    if (EventQueue.isDispatchThread()) {
                        Utils.postParallel(run);
                    } else {
                        run.run();
                    }
                    return p;
                } else {
                    return null; // e.g. FileObject from a jar filesystem
                }
            } else {
                return new VCSFileProxy(fileObject.getPath(), fileProxyOperations);
            }
        } catch (FileStateInvalidException ex) {
            Logger.getLogger(VCSFileProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new VCSFileProxy(fileObject.getPath(), null);
    }
    
    /**
     * Determines this files path. Depending on its origin it will be either 
     * {@link FileObject#getPath()} or {@link File#getAbsoluteFile()}.
     * 
     * @return this files path
     * @see File#getPath() 
     * @see FileObject#getPath() 
     */
    public String getPath() {
        return path;
    }

    /**
     * Determines this files URI. Depending on its origin it will be either
     * {@link FileObject#toURI()} or {@link File#toURI()}.
     *
     * @return this file URI
     * @see File#toURI()
     * @see FileObject#toURI()
     */
    public URI toURI() throws URISyntaxException {
        if (this.proxy == null) {
            return  Utilities.toURI(new File(path));
        } else {
            return proxy.toURI(this);
        }
    }

    /**
     * Returns the name of this file.
     * 
     * @return this files name
     * @see File#getName() () 
     */
    public String getName() {
        if (proxy == null) {
            return new File(path).getName();
        } else {
            return proxy.getName(this);
        }
    }
    
    /**
     * Determines whether this file is a directory or not.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return <code>true</code> if this file exists and is a directory, otherwise <code>false</code>
     * @see File#isDirectory() 
     */
    public boolean isDirectory() {
        if (proxy == null) {
            if(isDirectory != null) {
                return isDirectory;
            } else {
                return new File(path).isDirectory();
            }
        } else {
            return proxy.isDirectory(this);
        }
    }
    
    /**
     * Determines whether this file is a normal file or not.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return <code>true</code> if this file is a normal file, otherwise <code>false</code>
     * @see File#isFile() 
     */
    public boolean isFile() {
        if (proxy == null) { 
            if(isDirectory != null) {
                return !isDirectory;
            } else {
                return new File(path).isFile();
            }
        } else {
            return proxy.isFile(this);
        }
    }
    
    /**
     * Determines whether this file is writable or not.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return <code>true</code> if this file is writable, otherwise <code>false</code>
     * @see File#canWrite() 
     */
    public boolean canWrite() {
        if (proxy == null) {
            return new File(path).canWrite();
        } else {
            return proxy.canWrite(this);
        }
    }
    
    /**
     * Determines the last time this file was modified.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return A <code>long</code> value representing the time the file was
     *         last modified.
     * @see File#lastModified() 
     */
    public long lastModified() {
        if (proxy == null) {
            return new File(path).lastModified();
        } else {
            return proxy.lastModified(this);
        }
    }
    
    /**
     * Returns this files parent or <code>null</code> if this file doesn't have a parent.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return this files parent 
     * @see File#getParentFile() 
     */
    public VCSFileProxy getParentFile() {
        if (proxy == null) {
            File parent = new File(path).getParentFile();
            if(parent == null) {
                return null;
            }
            return VCSFileProxy.createFileProxy(parent);
        } else {
            return proxy.getParentFile(this);
        }
    }

    public InputStream getInputStream(boolean checkLock) throws FileNotFoundException {
        if (proxy == null) {
            if (checkLock) {
                FileObject fo = toFileObject();
                if (fo != null) {
                    return fo.getInputStream();
                } else {
                    throw new FileNotFoundException("File not found: " + path); //NOI18N
                }
            } else {
                return new FileInputStream(new File(path));
            }
        } else {
            return proxy.getInputStream(this, checkLock);
        }
    }
    
    /**
     * Determines whether this file exists or not.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return <code>true</code> if this files exists, otherwise <code>false</code>
     * @see File#exists() 
     */
    public boolean exists() {
        if (proxy == null) {
            return new File(path).exists();
        } else {
            return proxy.exists(this);
        }
    }
    
    /**
     * Returns an array of files located in a directory given by this file.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return an array of files located in a directory given by this file or 
     * <code>null</code> if this file isn't a directory or an error occurs.
     * @see File#listFiles() 
     */
    public VCSFileProxy[] listFiles() {
        if (proxy == null) {
            File[] files = new File(path).listFiles();
            if(files != null) {
                VCSFileProxy[] ret = new VCSFileProxy[files.length];
                for (int i = 0; i < files.length; i++) {
                    ret[i] = VCSFileProxy.createFileProxy(files[i]);
                }
                return ret;
            }
            return null;
        } else {
            return proxy.list(this);
        }
        
    }
        
    /**
     * Returns the corresponding java.io.File in case this instance was created 
     * based either on java.io.File or a {@link FileObject} based on java.io.File.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return the corresponding java.io.File instance or <Code>null</code> if none
     * is available.
     * @see #createFileProxy(java.io.File) 
     * @see #createFileProxy(org.openide.filesystems.FileObject) 
     */
    public File toFile() {
        if(proxy == null) {
            return isFlat ? new FlatFolder(path) : new File(path);
        }
        return null;
    }
    
    /**
     * Returns the corresponding FileObject.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return the corresponding FileObject or <code>null</code> if none available
     */
    public FileObject toFileObject() {
        if (proxy == null) {
            return FileUtil.toFileObject(new File(FileUtil.normalizePath(path)));
        } else {
            return proxy.toFileObject(this);
        }
    }

    /**
     * Normalize a file path to a clean form.
     * <b>This method might block for a longer time and shouldn't be called in EDT.
     * 
     * @return a VCSFileProxy with a normalized file path
     * @see FileUtil#normalizePath(java.lang.String) 
     */
    public VCSFileProxy normalizeFile() {
        if (proxy == null) {
            return new VCSFileProxy(FileUtil.normalizePath(path), null);
        } else {
            return proxy.normalize(this);
        }
    }    
    
    @Override
    public String toString() {
        return path;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + (this.path != null ? this.path.hashCode() : 0);
        hash = 61 * hash + (this.proxy != null ? this.proxy.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VCSFileProxy other = (VCSFileProxy) obj;
        if ((this.path == null) ? (other.path != null) : !this.path.equals(other.path)) {
            return false;
        }
        if (this.proxy != other.proxy && (this.proxy == null || !this.proxy.equals(other.proxy))) {
            return false;
        }
        return true;
    }

    void setFlat(boolean flat) {
        this.isFlat = flat;
    }
    
    boolean isFlat() {
        return isFlat;
    }
    
    private static VCSFileProxyOperations getFileProxyOperations(FileSystem fs) {
        return (VCSFileProxyOperations) getAttribute(fs, VCSFileProxyOperations.ATTRIBUTE);
    }

    private static VCSFileProxyOperations getFileProxyOperations(URI uri) {
        for (VCSFileProxyOperations.Provider provider : Lookup.getDefault().lookupAll(VCSFileProxyOperations.Provider.class)) {
            VCSFileProxyOperations fileProxyOperations = provider.getVCSFileProxyOperations(uri);
            if (fileProxyOperations != null) {
                return fileProxyOperations;
            }
        }
        return null;
    }

    private static Object getAttribute(FileSystem fileSystem, String attrName) {
        return fileSystem.getRoot().getAttribute(attrName);
    }        
    
    ProcessBuilder createProcessBuilder() {
        if (proxy == null) {
            return ProcessBuilder.getLocal();
        } else {
            return proxy.createProcessBuilder(this);
        }
    }

    VCSFileProxyOperations getFileProxyOperations() {
         return proxy;
    }
}
