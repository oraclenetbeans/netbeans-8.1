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
package org.netbeans.modules.java.platform.queries;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.Parameters;
import org.openide.util.WeakListeners;


/**
 * This implementation of the SourceForBinaryQueryImplementation
 * provides sources for the active platform.
 */

@org.openide.util.lookup.ServiceProvider(service=org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation.class, position=90)
public class PlatformSourceForBinaryQuery implements SourceForBinaryQueryImplementation2 {

    private static final String JAR_FILE = "jar:file:";                 //NOI18N
    private static final String RTJAR_PATH = "/jre/lib/rt.jar!/";       //NOI18N
    private static final String SRC_ZIP = "/src.zip";                    //NOI18N

    private Map<URL,SourceForBinaryQueryImplementation2.Result> cache = new HashMap<>();

    public PlatformSourceForBinaryQuery () {
    }

    /**
     * Tries to locate the source root for given classpath root.
     * @param binaryRoot the URL of a classpath root (platform supports file and jar protocol)
     * @return FileObject[], never returns null
     */
    @Override
    public SourceForBinaryQueryImplementation2.Result findSourceRoots2(URL binaryRoot) {
        SourceForBinaryQueryImplementation2.Result res = this.cache.get (binaryRoot);
        if (res != null) {
            return res;
        }
        final JavaPlatformManager jpm = JavaPlatformManager.getDefault();
        final Collection<JavaPlatform> candidates = new ArrayDeque<>();
        for (JavaPlatform platform : jpm.getInstalledPlatforms()) {
            if (contains(platform, binaryRoot)) {
                candidates.add(platform);
            }
        }
        if (!candidates.isEmpty()) {
            res = new Result(
                jpm,
                binaryRoot,
                candidates);
            this.cache.put (binaryRoot, res);
            return res;
        }
        String binaryRootS = binaryRoot.toExternalForm();
        if (binaryRootS.startsWith(JAR_FILE)) {
            if (binaryRootS.endsWith(RTJAR_PATH)) {
                //Unregistered platform
                String srcZipS = binaryRootS.substring(4,binaryRootS.length() - RTJAR_PATH.length()) + SRC_ZIP;
                try {
                    URL srcZip = FileUtil.getArchiveRoot(new URL(srcZipS));
                    FileObject fo = URLMapper.findFileObject(srcZip);
                    if (fo != null) {
                        return new UnregisteredPlatformResult (fo);
                    }
                } catch (MalformedURLException mue) {
                    Exceptions.printStackTrace(mue);
                }
            }
        }
        return null;
    }

    @Override
    public SourceForBinaryQuery.Result findSourceRoots (URL binaryRoot) {
        return this.findSourceRoots2(binaryRoot);
    }

    static boolean contains(
        @NonNull final JavaPlatform platform,
        @NonNull final URL artifact) {
        for (ClassPath.Entry entry : platform.getBootstrapLibraries().entries()) {
            if (entry.getURL().equals (artifact)) {
                return true;
            }
        }
        return false;
    }

    private static final class Result implements SourceForBinaryQueryImplementation2.Result, PropertyChangeListener {

        private final JavaPlatformManager jpm;
        private final URL artifact;
        private final ChangeSupport cs = new ChangeSupport(this);
        //@GuardedBy("this")
        private Map<JavaPlatform,PropertyChangeListener> platforms;


        public Result (
            @NonNull final JavaPlatformManager jpm,
            @NonNull final URL artifact,
            @NonNull final Collection<? extends JavaPlatform> platforms) {
            Parameters.notNull("jpm", jpm); //NOI18N
            Parameters.notNull("artifact", artifact);   //NOI18N
            Parameters.notNull("platforms", platforms); //NOI18N
            this.jpm = jpm;
            this.artifact = artifact;
            synchronized (this) {
                this.platforms = new LinkedHashMap<>();
                for (JavaPlatform platform : platforms) {
                    final PropertyChangeListener l = WeakListeners.propertyChange(this, platform);
                    platform.addPropertyChangeListener(l);
                    this.platforms.put(platform, l);
                }
                this.jpm.addPropertyChangeListener(WeakListeners.propertyChange(this, this.jpm));
            }
        }

        @Override
        @NonNull
        public FileObject[] getRoots () {       //No need for caching, platforms does.
            for (JavaPlatform platform : platforms.keySet()) {
                final ClassPath sourcePath = platform.getSourceFolders();
                final FileObject[] sourceRoots = sourcePath.getRoots();
                if (sourceRoots.length > 0) {
                    return sourceRoots;
                }
            }
            return new FileObject[0];
        }

        @Override
        public void addChangeListener (@NonNull final ChangeListener listener) {
            Parameters.notNull("listener", listener);   //NOI18N
            cs.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener (@NonNull final ChangeListener listener) {
            Parameters.notNull("listener", listener);   //NOI18N
            cs.removeChangeListener(listener);
        }

        @Override
        public void propertyChange (@NonNull final PropertyChangeEvent event) {
            if (JavaPlatform.PROP_SOURCE_FOLDER.equals(event.getPropertyName())) {
                cs.fireChange();
            } else if (JavaPlatformManager.PROP_INSTALLED_PLATFORMS.equals(event.getPropertyName())) {
                if (updateCandidates()) {
                    cs.fireChange();
                }
            }
        }

        @Override
        public boolean preferSources() {
            return false;
        }

        private synchronized boolean updateCandidates() {
            boolean affected = false;
            final JavaPlatform[] newPlatforms = jpm.getInstalledPlatforms();
            final Map<JavaPlatform, PropertyChangeListener> oldPlatforms = new HashMap<>(platforms);
            final Map<JavaPlatform, PropertyChangeListener> newState = new LinkedHashMap<>(newPlatforms.length);
            for (JavaPlatform jp : newPlatforms) {
                PropertyChangeListener l;
                if ((l=oldPlatforms.remove(jp))!=null) {
                    newState.put(jp,l);
                } else if (contains(jp,artifact)) {
                    affected = true;
                    l = WeakListeners.propertyChange(this, this.jpm);
                    jp.addPropertyChangeListener(l);
                    newState.put(jp,l);
                }
            }
            for (Map.Entry<JavaPlatform,PropertyChangeListener> e : oldPlatforms.entrySet()) {
                affected = true;
                e.getKey().removePropertyChangeListener(e.getValue());
            }
            platforms = newState;
            return affected;
        }

    }

    private static class UnregisteredPlatformResult implements SourceForBinaryQueryImplementation2.Result {

        private final FileObject srcRoot;

        private UnregisteredPlatformResult (FileObject fo) {
            Parameters.notNull("fo", fo);   //NOI18N
            srcRoot = fo;
        }

        @Override
        public FileObject[] getRoots() {
            return srcRoot.isValid() ? new FileObject[] {srcRoot} : new FileObject[0];
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            //Not supported, no listening.
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            //Not supported, no listening.
        }

        @Override
        public boolean preferSources() {
            return false;
        }
    }}

