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
 * Contributor(s): theanuradha@netbeans.org
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven.indexer.api;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeListener;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.maven.embedder.EmbedderFactory;
import org.netbeans.modules.maven.embedder.MavenEmbedder;
import static org.netbeans.modules.maven.indexer.api.Bundle.*;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.sonatype.aether.repository.MirrorSelector;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;

/**
 * List of Maven repositories of interest.
 */
public final class RepositoryPreferences {

    private static final Logger LOG = Logger.getLogger(RepositoryPreferences.class.getName());

    private static RepositoryPreferences instance;

    private static final String KEY_DISPLAY_NAME = "name";//NOI18N
    private static final String KEY_PATH = "path";//NOI18N
    private static final String KEY_INDEX_URL = "index";//NOI18N
    private static final String KEY_REPO_URL = "url";//NOI18N
    /*index settings */
    public static final String PROP_INDEX_FREQ = "indexUpdateFrequency"; //NOI18N
    public static final String PROP_INDEX = "createIndex"; //NOI18N
    public static final String PROP_LAST_INDEX_UPDATE = "lastIndexUpdate"; //NOI18N
    public static final int FREQ_ONCE_WEEK = 0;
    public static final int FREQ_ONCE_DAY = 1;
    public static final int FREQ_STARTUP = 2;
    public static final int FREQ_NEVER = 3;
    private final Map<String,RepositoryInfo> infoCache = new HashMap<String,RepositoryInfo>();
    private final Map<Object,List<RepositoryInfo>> transients = new LinkedHashMap<Object,List<RepositoryInfo>>();
    private RepositoryInfo local;
    private final RepositoryInfo central;
    private final ChangeSupport cs = new ChangeSupport(this);
    
    private RepositoryPreferences() {
        try {
            central = new RepositoryInfo(RepositorySystem.DEFAULT_REMOTE_REPO_ID, /* XXX pull display name from superpom? */RepositorySystem.DEFAULT_REMOTE_REPO_ID, null, RepositorySystem.DEFAULT_REMOTE_REPO_URL);
            //this repository can be mirrored
            central.setMirrorStrategy(RepositoryInfo.MirrorStrategy.ALL);

        } catch (URISyntaxException x) {
            throw new AssertionError(x);
        }
    }

    private static Preferences getPreferences() {
        return NbPreferences.root().node("org/netbeans/modules/maven/nexus/indexing"); //NOI18N
    }

    private static Preferences storage() {
        return NbPreferences.root().node("org/netbeans/modules/maven/repositories");
    }

    public synchronized static RepositoryPreferences getInstance() {
        if (instance == null) {
            instance = new RepositoryPreferences();
        }
        return instance;
    }

    private static @CheckForNull RepositoryInfo createRepositoryInfo(Preferences p) throws URISyntaxException {
        String id = p.name();
        String name = p.get(KEY_DISPLAY_NAME, null);
        if (name == null) {
            return null;
        }
        String path = p.get(KEY_PATH, null);
        String repourl = p.get(KEY_REPO_URL, null);
        String indexurl = p.get(KEY_INDEX_URL, null);
        RepositoryInfo repo = new RepositoryInfo(id, name, path, repourl, indexurl);
        //repository infos from preferences cannot be wildcard mirrored.
        repo.setMirrorStrategy(RepositoryInfo.MirrorStrategy.NON_WILDCARD);
        return repo;
    }

    /** @since 2.2 */
    @Messages("local=Local")
    public @NonNull synchronized RepositoryInfo getLocalRepository() {
        if (local == null) {
            try {
                //TODO do we care about changing the instance when localrepo location changes?
                local = new RepositoryInfo(RepositorySystem.DEFAULT_LOCAL_REPO_ID, local(), EmbedderFactory.getProjectEmbedder().getLocalRepositoryFile().getAbsolutePath(), null);
                local.setMirrorStrategy(RepositoryInfo.MirrorStrategy.NONE);
            } catch (URISyntaxException x) {
                throw new AssertionError(x);
            }
        }
        return local;
    }

    /**
     * returns the RepositoryInfo object with the given id or a mirror repository info
     * that mirrors the given id.
     * @param id
     * @return 
     */
    public @CheckForNull RepositoryInfo getRepositoryInfoById(String id) {
        List<RepositoryInfo> infos = getRepositoryInfos();
        //repository infos are now including mirrors
        //first check if the repository itself in the list has the id
        for (RepositoryInfo ri : infos) {
            if (ri.getId().equals(id)) {
                return ri;
            }
        }
        //if not, then try checking the mirrored repos..
        for (RepositoryInfo ri : infos) {
            if (ri.isMirror()) {
                for (RepositoryInfo rii : ri.getMirroredRepositories()) {
                    if (rii.getId().equals(id)) {
                        return ri;
                    }
                }
            }
        }
        return null;
    }

    public List<RepositoryInfo> getRepositoryInfos() {
        List<RepositoryInfo> toRet = new ArrayList<RepositoryInfo>();
        toRet.add(getLocalRepository());
        Set<String> ids = new HashSet<String>();
        ids.add(RepositorySystem.DEFAULT_LOCAL_REPO_ID);
        Set<String> urls = new HashSet<String>();
        synchronized (infoCache) {
            Preferences storage = storage();
            try {
                Set<String> gone = new HashSet<String>(infoCache.keySet());
                for (String c : storage.childrenNames()) {
                    RepositoryInfo ri = infoCache.get(c);
                    if (ri == null) {
                        Preferences child = storage.node(c);
                        try {
                            ri = createRepositoryInfo(child);
                            if (ri == null) {
                                continue;
                            }
                            infoCache.put(c, ri);
                        } catch (/*IllegalArgument,URISyntax*/Exception x) {
                            LOG.log(Level.INFO, c, x);
                            try {
                                child.removeNode();
                            } catch (BackingStoreException x2) {
                                LOG.log(Level.INFO, null, x2);
                            }
                            continue;
                        }
                    }
                    toRet.add(ri);
                    gone.remove(c);
                    ids.add(ri.getId());
                    urls.add(ri.getRepositoryUrl());
                }
                for (String g : gone) {
                    infoCache.remove(g);
                }
            } catch (BackingStoreException x) {
                LOG.log(Level.INFO, null, x);
            }
            if (transients.isEmpty()) {
                if (ids.add(central.getId()) && urls.add(central.getRepositoryUrl())) {
                    toRet.add(central);
                }
            } else {
                for (List<RepositoryInfo> infos : transients.values()) {
                    for (RepositoryInfo info : infos) {
                        if (ids.add(info.getId()) && urls.add(info.getRepositoryUrl())) {
                            toRet.add(info);
                        }
                    }
                }
            }
        }
            MavenEmbedder embedder2 = EmbedderFactory.getOnlineEmbedder();
            DefaultMirrorSelector selectorWithGroups = new DefaultMirrorSelector();
            DefaultMirrorSelector selectorWithoutGroups = new DefaultMirrorSelector();
            final Settings settings = embedder2.getSettings();
            for (Mirror mirror : settings.getMirrors()) {
                String mirrorOf = mirror.getMirrorOf();
                selectorWithGroups.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirrorOf, mirror.getMirrorOfLayouts());
                if (!mirrorOf.contains("*")) {
                    selectorWithoutGroups.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirrorOf, mirror.getMirrorOfLayouts());
                }
            }

            List<RepositoryInfo> semiTreed = new ArrayList<RepositoryInfo>();
            for (RepositoryInfo in: toRet) {
                if (in.getMirrorStrategy() == RepositoryInfo.MirrorStrategy.ALL || in.getMirrorStrategy() == RepositoryInfo.MirrorStrategy.NON_WILDCARD) {
                    RepositoryInfo processed = getMirrorInfo(in, in.getMirrorStrategy() == RepositoryInfo.MirrorStrategy.ALL ? selectorWithGroups : selectorWithoutGroups, settings);
                    boolean isMirror = true;
                    if (processed == null) {
                        isMirror = false;
                        processed = in;
                    }
                    int index = semiTreed.indexOf(processed);
                    if (index > -1) {
                        processed = semiTreed.get(index);
                    } else {
                        semiTreed.add(processed);
                    }
                    if (isMirror) {
                        processed.addMirrorOfRepository(in);
                    }
                } else {
                    semiTreed.add(in);
                }
            }
            return semiTreed;
    }
    
    /**
     * if the repository has a mirror, then create a repositoryinfo object for it..
     */
    
    private RepositoryInfo getMirrorInfo(RepositoryInfo info, MirrorSelector selector, Settings settings) {
        RemoteRepository original = new RemoteRepository(info.getId(), /* XXX do we even support any other layout?*/"default", info.getRepositoryUrl());
        RemoteRepository mirror = selector.getMirror(original);
        if (mirror != null) {
            try {
                String name = mirror.getId();
                //#213078 need to lookup name for mirror
                for (Mirror m : settings.getMirrors()) {
                    if (m.getId() != null && m.getId().equals(mirror.getId())) {
                        name = m.getName();
                        break;
                    }
                }
                RepositoryInfo toret = new RepositoryInfo(mirror.getId(), name, null, mirror.getUrl());
                toret.setMirrorStrategy(RepositoryInfo.MirrorStrategy.NONE);
                return toret;
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }

    public void addOrModifyRepositoryInfo(RepositoryInfo info) {
        String id = info.getId();
        synchronized (infoCache) {
            infoCache.put(id, info);
            Preferences p = storage().node(id);
            p.put(KEY_DISPLAY_NAME, info.getName());
            put(p, KEY_PATH, info.getRepositoryPath());
            put(p, KEY_REPO_URL, info.getRepositoryUrl());
            if (info.getRepositoryUrl() != null) {
                put(p, KEY_INDEX_URL, info.getIndexUpdateUrl().equals(info.getRepositoryUrl() + RepositoryInfo.DEFAULT_INDEX_SUFFIX) ? null : info.getIndexUpdateUrl());
            }
        }
        cs.fireChange();
    }
    private static void put(@NonNull Preferences p, @NonNull String key, @NullAllowed String value) {
        if (value != null) {
            p.put(key, value);
        } else {
            p.remove(key);
        }
    }

    /**
     * Checks whether a given repository is persisted.
     * @param id the repository's ID
     * @return true if it is persistent (custom), false if it is the local repository or was added transiently
     * @since 2.1
     */
    public boolean isPersistent(String id) {
        return storage().node(id).get(KEY_DISPLAY_NAME, null) != null;
    }
    
    public void removeRepositoryInfo(RepositoryInfo info) {
        synchronized (infoCache) {
            String id = info.getId();
            infoCache.remove(id);
            try {
                storage().node(id).removeNode();
            } catch (BackingStoreException x) {
                LOG.log(Level.INFO, null, x);
            }
        }
        cs.fireChange();
    }

    public static void setIndexUpdateFrequency(int fr) {
        getPreferences().putInt(PROP_INDEX_FREQ, fr);
    }

    public static int getIndexUpdateFrequency() {
        return getPreferences().getInt(PROP_INDEX_FREQ, Boolean.getBoolean("netbeans.full.hack") ? FREQ_NEVER : FREQ_ONCE_WEEK);
    }
    /**
     * @since 2.27
     * @param bool 
     */
    public static void setIndexRepositories(boolean bool) {
        getPreferences().putBoolean(PROP_INDEX, bool);
    }

    /**
     * @since 2.27
     * @return 
     */
    public static boolean isIndexRepositories() {
        return getPreferences().getBoolean(PROP_INDEX, true);
    }

    public static Date getLastIndexUpdate(String repoId) {
        if(repoId.contains("//")) {
            repoId = repoId.replace("//", "_");
            LOG.log(Level.FINE, "Getting last index update: Repo''s id contains consecutive slashes, replacing them with '_': {0}", repoId);
        }
        long old = getPreferences().getLong(PROP_LAST_INDEX_UPDATE + "." + repoId, 0); // compatibility
        if (old != 0) { // upgrade it
            getPreferences().remove(PROP_LAST_INDEX_UPDATE + "." + repoId);
            storage().node(repoId).putLong(PROP_LAST_INDEX_UPDATE, old);
        }
        return new Date(storage().node(repoId).getLong(PROP_LAST_INDEX_UPDATE, 0));
    }

    public static void setLastIndexUpdate(String repoId,Date date) {
        if(repoId.contains("//")) {
            repoId = repoId.replace("//", "_");
            LOG.log(Level.FINE, "Setting last index update: Repo''s id contains consecutive slashes, replacing them with '_': {0}", repoId);
        }
        getPreferences().remove(PROP_LAST_INDEX_UPDATE + "." + repoId);
        storage().node(repoId).putLong(PROP_LAST_INDEX_UPDATE, date.getTime());
    }

    /**
     * Register a transient repository, the effective url actually used depends on maven settings for mirrors.
     * Its definition will not be persisted.
     * Repositories whose ID or URL duplicate that of a persistent repository,
     * or previously registered transient repository, will be ignored
     * (unless and until that repository is removed).
     * @param key an arbitrary key for use with {@link #removeTransientRepositories}
     * @param id the repository ID
     * @param displayName a display name (may just be {@code id})
     * @param url the remote URL (prefer the canonical public URL to that of a mirror)
     * @param strategy how is the url parameter processed by local maven mirror settings
     * @throws URISyntaxException in case the URL is malformed
     * @since 2.11
     */
    public void addTransientRepository(Object key, String id, String displayName, String url, RepositoryInfo.MirrorStrategy strategy) throws URISyntaxException {
        if(id.contains("//")) {
            id = id.replace("//", "_");
            LOG.log(Level.FINE, "Adding transient repository: Repo''s id contains consecutive slashes, replacing them with '_': {0}", id);
        }
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file:")) {
            //only register repositories we can safely handle.. #227322
            return;
        }
        synchronized (infoCache) {
            List<RepositoryInfo> infos = transients.get(key);
            if (infos == null) {
                infos = new ArrayList<RepositoryInfo>();
                transients.put(key, infos);
            }
            RepositoryInfo info = new RepositoryInfo(id, displayName, null, url);
            info.setMirrorStrategy(strategy);
            infos.add(info);
        }
        cs.fireChange();
    }

    

    /**
     * Remote all transient repositories associated with a given ID.
     * @param key a key as with {@link #addTransientRepository}
     * @since 2.1
     */
    public void removeTransientRepositories(Object key) {
        synchronized (infoCache) {
            transients.remove(key);
        }
        cs.fireChange();
    }

    /**
     * @since 2.1
     */
    public void addChangeListener(ChangeListener l) {
        cs.addChangeListener(l);
    }

    /**
     * @since 2.1
     */
    public void removeChangeListener(ChangeListener l) {
        cs.removeChangeListener(l);
    }

    /**
     * Produces a list of remote repositories.
     * @see MavenEmbedder#resolve
     * @see ProjectBuildingRequest#setRemoteRepositories
     * @since 2.12
     */
    public List<ArtifactRepository> remoteRepositories(MavenEmbedder embedder) {
        List<ArtifactRepository> remotes = new ArrayList<ArtifactRepository>();
        for (RepositoryInfo info : getRepositoryInfos()) {
            // XXX should there be a String preferredId parameter to limit the remote repositories used in case we have a "reference" ID somehow?
            if (!info.isLocal()) {
                remotes.add(embedder.createRemoteRepository(info.getRepositoryUrl(), info.getId()));
            }
            // XXX do we care to handle mirrors specially?
        }
        return remotes;
    }

}
