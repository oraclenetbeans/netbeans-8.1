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

package org.netbeans.modules.java.source.parsing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.openide.util.Exceptions;
import org.openide.util.Parameters;

/**
 *
 * @author Tomas Zezula
 */
public final class CachingArchiveClassLoader extends ClassLoader {

    private static final int INI_SIZE = 16384;
    private static final Logger LOG = Logger.getLogger(CachingArchiveClassLoader.class.getName());
    //Todo: Performance Trie<File,ReentrantReadWriteLock>
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    private final Archive[] archives;
    private byte[] buffer;

    private CachingArchiveClassLoader(final @NonNull Archive[] archives, final ClassLoader parent) {
        super (parent);
        assert archives != null;
        this.archives = archives;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        final StringBuilder sb = new StringBuilder(FileObjects.convertPackage2Folder(name, '/'));
        sb.append(JavaFileObject.Kind.CLASS.extension);
        Class<?> c = null;
        try {
            c = readAction(new Callable<Class<?>>() {
                @Override
                public Class<?> call() throws Exception {
                    final FileObject file = findFileObject(sb.toString());
                    if (file != null) {
                        try {
                            final int len = readJavaFileObject(file);
                            int lastDot = name.lastIndexOf('.');
                            if (lastDot != (-1)) {
                                String pack = name.substring(0, lastDot);
                                if (getPackage(pack) == null) {
                                    definePackage(pack, null, null, null, null, null, null, null);
                                }
                            }
                            return defineClass(
                                    name,
                                    com.sun.tools.hc.LambdaMetafactory.translateClassFile(buffer,0,len),
                                    0,
                                    len);
                        } catch (FileNotFoundException fnf) {
                            LOG.log(Level.FINE, "Resource: {0} does not exist.", file.toUri()); //NOI18N
                        } catch (IOException ioe) {
                            LOG.log(Level.INFO, "Resource: {0} cannot be read.", file.toUri()); //NOI18N
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
        return c != null ?
            c :
            super.findClass(name);
    }

    @Override
    protected URL findResource(final String name) {
        FileObject file = null;
        try {
            file = readAction(new Callable<FileObject>() {
                @Override
                public FileObject call() throws Exception {
                    return findFileObject(name);
                }
            });
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
        if (file != null) {
            try {
                return file.toUri().toURL();
            } catch (MalformedURLException ex) {
                LOG.log(Level.INFO, ex.getMessage(), ex);
            }
        }
        return super.findResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(final String name) throws IOException {
        try {
            return readAction(new Callable<Enumeration<URL>>(){
                @Override
                public Enumeration<URL> call() throws Exception {
                    @SuppressWarnings("UseOfObsoleteCollectionType")
                    final Vector<URL> v = new Vector<URL>();
                    for (Archive archive : archives) {
                        final FileObject file = archive.getFile(name);
                        if (file != null) {
                            v.add(file.toUri().toURL());
                        }
                    }
                    return v.elements();
                }
            });
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private int readJavaFileObject(final FileObject jfo) throws IOException {
        assert LOCK.getReadLockCount() > 0;
        if (buffer == null) {
            buffer = new byte[INI_SIZE];
        }
        int len = 0;
        final InputStream in = jfo.openInputStream();
        try {
            while (true) {
                if (buffer.length == len) {
                    byte[] nb = new byte[2*buffer.length];
                    System.arraycopy(buffer, 0, nb, 0, len);
                    buffer = nb;
                }
                int l = in.read(buffer,len,buffer.length-len);
                if (l<=0) {
                    break;
                }
                len+=l;
            }

        } finally {
            in.close();
        }
        return len;
    }

    private FileObject findFileObject(final String resName) {
        assert LOCK.getReadLockCount() > 0;
        for (Archive archive : archives) {
            try {
                final FileObject file = archive.getFile(resName);
                if (file != null) {
                    return file;
                }
            } catch (IOException ex) {
                LOG.log(
                    Level.INFO,
                    "Cannot read: " + archive,  //NOI18N
                    ex);
            }
        }
        return null;
    }

    public static ClassLoader forClassPath(final @NonNull ClassPath classPath,
            final @NullAllowed ClassLoader parent) {
        Parameters.notNull("classPath", classPath); //NOI18N
        final List<ClassPath.Entry> entries = classPath.entries();
        final URL[] urls = new URL[entries.size()];
        final Iterator<ClassPath.Entry> eit = entries.iterator();
        for (int i=0; eit.hasNext(); i++) {
            urls[i] = eit.next().getURL();
        }
        return forURLs(urls, parent);
    }

    public static ClassLoader forURLs(final @NonNull URL[] urls,
            final @NullAllowed ClassLoader parent) {
        Parameters.notNull("urls", urls);       //NOI18N
        final List<Archive> archives = new ArrayList<Archive>(urls.length);
        for (URL url : urls) {
            final Archive arch = CachingArchiveProvider.getDefault().getArchive(url, false);
            if (arch != null) {
                archives.add(arch);
            }
        }
        return new CachingArchiveClassLoader(archives.toArray(new Archive[archives.size()]), parent);
    }

    public static <T> T readAction(@NonNull final Callable<T> action) throws Exception {
        Parameters.notNull("action", action);   //NOI18N
        LOCK.readLock().lock();
        try {
            LOG.log(Level.FINE, "Read locked by {0}", Thread.currentThread());  //NOI18N
            return action.call();
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static <T> T writeAction(@NonNull final Callable<T> action) throws Exception {
        Parameters.notNull("action", action);   //NOI18N
        LOCK.writeLock().lock();
        try {
            LOG.log(Level.FINE, "Write locked by {0}", Thread.currentThread());  //NOI18N
            return action.call();
        } finally {
            LOCK.writeLock().unlock();
        }
    }

}
