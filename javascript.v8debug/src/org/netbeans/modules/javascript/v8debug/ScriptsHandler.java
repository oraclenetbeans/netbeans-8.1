/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javascript.v8debug;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.lib.v8debug.V8Command;
import org.netbeans.lib.v8debug.V8Request;
import org.netbeans.lib.v8debug.V8Response;
import org.netbeans.lib.v8debug.V8Script;
import org.netbeans.lib.v8debug.commands.Scripts;
import org.netbeans.modules.javascript2.debug.sources.SourceContent;
import org.netbeans.modules.javascript2.debug.sources.SourceFilesCache;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.NbBundle;

/**
 *
 * @author Martin Entlicher
 */
public class ScriptsHandler {
    
    private static final Logger LOG = Logger.getLogger(ScriptsHandler.class.getName());

    private final Map<Long, V8Script> scriptsById = new HashMap<>();
    private final Map<URL, V8Script> scriptsByURL = new HashMap<>();

    private final boolean doPathTranslation;
    private final int numPrefixes;
    @NullAllowed
    private final String[] localPathPrefixes;
    private final char localPathSeparator;
    @NullAllowed
    private final FileObject[] localRoots;
    @NullAllowed
    private final FileObject[] localPathExclusionFilters;
    @NullAllowed
    private final String[] serverPathPrefixes;
    private final char serverPathSeparator;
    private final String remotePathPrefix;
    private final V8Debugger dbg;
    
    ScriptsHandler(@NullAllowed List<String> localPaths,
                   @NullAllowed List<String> serverPaths,
                   Collection<String> localPathExclusionFilters,
                   @NullAllowed V8Debugger dbg) {
        if (dbg != null) {
            this.remotePathPrefix = dbg.getHost()+"_"+dbg.getPort()+"/";
        } else {
            // dbg can be null in tests
            this.remotePathPrefix = "";
        }
        if (!localPaths.isEmpty() && !serverPaths.isEmpty()) {
            this.doPathTranslation = true;
            int n = localPaths.size();
            this.numPrefixes = n;
            this.localPathPrefixes = new String[n];
            this.serverPathPrefixes = new String[n];
            for (int i = 0; i < n; i++) {
                this.localPathPrefixes[i] = stripSeparator(localPaths.get(i));
            }
            this.localPathSeparator = findSeparator(localPaths.get(0));
            for (int i = 0; i < n; i++) {
                this.serverPathPrefixes[i] = stripSeparator(serverPaths.get(i));
            }
            this.serverPathSeparator = findSeparator(serverPaths.get(0));
        } else {
            this.doPathTranslation = false;
            this.localPathPrefixes = this.serverPathPrefixes = null;
            this.localPathSeparator = this.serverPathSeparator = 0;
            this.numPrefixes = 0;
        }
        if (!localPaths.isEmpty()) {
            this.localRoots = new FileObject[localPaths.size()];
            int i = 0;
            for (String localPath : localPaths) {
                FileObject localRoot = FileUtil.toFileObject(new File(localPath));
                if (localRoot != null) {
                    this.localRoots[i++] = localRoot;
                }
            }
        } else {
            this.localRoots = null;
        }
        if (!localPathExclusionFilters.isEmpty()) {
            FileObject[] lpefs = new FileObject[localPathExclusionFilters.size()];
            int i = 0;
            for (String lpef : localPathExclusionFilters) {
                FileObject localRoot = FileUtil.toFileObject(new File(lpef));
                if (localRoot != null) {
                    lpefs[i++] = localRoot;
                } else {
                    lpefs = Arrays.copyOf(lpefs, lpefs.length - 1);
                }
            }
            this.localPathExclusionFilters = (lpefs.length > 0) ? lpefs : null;
        } else {
            this.localPathExclusionFilters = null;
        }
        LOG.log(Level.FINE,
                "ScriptsHandler: doPathTranslation = {0}, localPathPrefixes = {1}, separator = {2}, "+
                                "serverPathPrefixes = {3}, separator = {4}, "+
                                "localRoots = {5}, localPathExclusionFilters = {6}.",
                new Object[]{doPathTranslation, Arrays.toString(localPathPrefixes), localPathSeparator,
                             Arrays.toString(serverPathPrefixes), serverPathSeparator,
                             Arrays.toString(this.localRoots),
                             Arrays.toString(this.localPathExclusionFilters) });
        this.dbg = dbg;
    }

    void add(V8Script script) {
        synchronized (scriptsById) {
            scriptsById.put(script.getId(), script);
        }
    }
    
    void add(V8Script[] scripts) {
        synchronized (scriptsById) {
            for (V8Script script : scripts) {
                scriptsById.put(script.getId(), script);
            }
        }
    }
    
    void remove(long scriptId) {
        V8Script removed;
        synchronized (scriptsById) {
            removed = scriptsById.remove(scriptId);
        }
        if (removed != null) {
            synchronized (scriptsByURL) {
                for (Map.Entry<URL, V8Script> entry : scriptsByURL.entrySet()) {
                    if (removed == entry.getValue()) {
                        scriptsByURL.remove(entry.getKey());
                        break;
                    }
                }
            }
        }
    }

    @CheckForNull
    public V8Script getScript(long id) {
        synchronized (scriptsById) {
            return scriptsById.get(id);
        }
    }
    
    @NonNull
    public Collection<V8Script> getScripts() {
        synchronized (scriptsById) {
            return new ArrayList<>(scriptsById.values());
        }
    }
    
    public boolean containsLocalFile(FileObject fo) {
        if (fo == null) {
            return false;
        }
        if (SourceFilesCache.URL_PROTOCOL.equals(fo.toURL().getProtocol())) {
            // virtual file created from source content
            return true;
        }
        if (localPathExclusionFilters != null) {
            for (FileObject lpef : localPathExclusionFilters) {
                if (FileUtil.isParentOf(lpef, fo)) {
                    return false;
                }
            }
        }
        if (localRoots == null) {
            return true;
        }
        for (FileObject localRoot : localRoots) {
            if (FileUtil.isParentOf(localRoot, fo)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean containsRemoteFile(URL url) {
        if (!SourceFilesCache.URL_PROTOCOL.equals(url.getProtocol())) {
            return false;
        }
        String path;
        try {
            path = url.toURI().getPath();
        } catch (URISyntaxException usex) {
            return false;
        }

        int l = path.length();
        int index = 0;
        while (index < l && path.charAt(index) == '/') {
            index++;
        }
        int begin = path.indexOf('/', index);
        if (begin > 0) {
            // path.substring(begin + 1).startsWith(remotePathPrefix)
            return path.regionMatches(begin + 1, remotePathPrefix, 0, remotePathPrefix.length());
        } else {
            return false;
        }
    }
    
    @CheckForNull
    public FileObject getFile(long scriptId) {
        V8Script script = getScript(scriptId);
        if (script == null) {
            return null;
        } else {
            return getFile(script);
        }
    }
    
    @NonNull
    public FileObject getFile(@NonNull V8Script script) {
        String name = script.getName();
        if (name != null && script.getScriptType() == V8Script.Type.NORMAL) {
            File localFile = null;
            if (doPathTranslation) {
                try {
                    String lp = getLocalPath(name);
                    localFile = new File(lp);
                } catch (OutOfScope oos) {
                }
            } else {
                File f = new File(name);
                if (f.isAbsolute()) {
                    localFile = f;
                }
            }
            if (localFile != null) {
                FileObject fo = FileUtil.toFileObject(localFile);
                if (fo != null) {
                    synchronized (scriptsByURL) {
                        scriptsByURL.put(fo.toURL(), script);
                    }
                    return fo;
                }
            }
        }
        if (name == null) {
            name = "unknown.js";
        }
        // prepend <host>_<port>/ to the name.
        name = remotePathPrefix + name;
        String content = script.getSource();
        URL sourceURL;
        if (content != null) {
            sourceURL = SourceFilesCache.getDefault().getSourceFile(name, content.hashCode(), content);
        } else {
            sourceURL = SourceFilesCache.getDefault().getSourceFile(name, 1234, new ScriptContentLoader(script, dbg));
        }
        synchronized (scriptsByURL) {
            scriptsByURL.put(sourceURL, script);
        }
        return URLMapper.findFileObject(sourceURL);
    }
    
    /**
     * Find a known script by it's actual URL.
     * @param scriptURL Script's URL returned by {@link #getFile(org.netbeans.lib.v8debug.V8Script)}
     * @return the script or <code>null</code> when not found.
     */
    @CheckForNull
    public V8Script findScript(@NonNull URL scriptURL) {
        synchronized (scriptsByURL) {
            return scriptsByURL.get(scriptURL);
        }
    }
    
    @CheckForNull
    public String getServerPath(@NonNull FileObject fo) {
        String serverPath;
        File file = FileUtil.toFile(fo);
        if (file != null) {
            String localPath = file.getAbsolutePath();
            try {
                serverPath = getServerPath(localPath);
            } catch (ScriptsHandler.OutOfScope oos) {
                serverPath = null;
            }
        } else {
            URL url = fo.toURL();
            V8Script script = findScript(url);
            if (script != null) {
                serverPath = script.getName();
            } else if (SourceFilesCache.URL_PROTOCOL.equals(url.getProtocol())) {
                String path = fo.getPath();
                int begin = path.indexOf('/');
                if (begin > 0) {
                    path = path.substring(begin + 1);
                    // subtract <host>_<port>/ :
                    if (path.startsWith(remotePathPrefix)) {
                        serverPath = path.substring(remotePathPrefix.length());
                    } else {
                        serverPath = null;
                    }
                } else {
                    serverPath = null;
                }
            } else {
                serverPath = null;
            }
        }
        return serverPath;
    }
    
    @CheckForNull
    public String getServerPath(@NonNull URL url) {
        if (!SourceFilesCache.URL_PROTOCOL.equals(url.getProtocol())) {
            return null;
        }
        String path;
        try {
            path = url.toURI().getPath();
        } catch (URISyntaxException usex) {
            return null;
        }
        int l = path.length();
        int index = 0;
        while (index < l && path.charAt(index) == '/') {
            index++;
        }
        int begin = path.indexOf('/', index);
        if (begin > 0) {
            // path.substring(begin + 1).startsWith(remotePathPrefix)
            if (path.regionMatches(begin + 1, remotePathPrefix, 0, remotePathPrefix.length())) {
                path = path.substring(begin + 1 + remotePathPrefix.length());
                return path;
            } else {
                // Path with a different prefix
                return null;
            }
        } else {
            return null;
        }
    }
    
    public String getLocalPath(@NonNull String serverPath) throws OutOfScope {
        if (!doPathTranslation) {
            return serverPath;
        } else {
            for (int i = 0; i < numPrefixes; i++) {
                if (isChildOf(serverPathPrefixes[i], serverPath)) {
                    return translate(serverPath, serverPathPrefixes[i], serverPathSeparator,
                                     localPathPrefixes[i], localPathSeparator);
                }
            }
        }
        throw new OutOfScope(serverPath, Arrays.toString(serverPathPrefixes));
    }
    
    public String getServerPath(@NonNull String localPath) throws OutOfScope {
        if (!doPathTranslation) {
            return localPath;
        } else {
            for (int i = 0; i < numPrefixes; i++) {
                if (isChildOf(localPathPrefixes[i], localPath)) {
                    return translate(localPath, localPathPrefixes[i], localPathSeparator,
                                     serverPathPrefixes[i], serverPathSeparator);
                }
            }
        }
        throw new OutOfScope(localPath, Arrays.toString(localPathPrefixes));
    }
    
    public File[] getLocalRoots() {
        if (localRoots == null) {
            return new File[]{};
        }
        int l = localRoots.length;
        File[] roots = new File[l];
        for (int i = 0; i < l; i++) {
            roots[i] = FileUtil.toFile(localRoots[i]);
        }
        return roots;
    }
    
    private static boolean isChildOf(String parent, String child) {
        if (!child.startsWith(parent)) {
            return false;
        }
        int l = parent.length();
        if (!isRootPath(parent)) { // When the parent is the root, do not do further checks.
            if (child.length() > l && !isSeparator(child.charAt(l))) {
                return false;
            }
        }
        return true;
    }
    
    private static String translate(String path, String pathPrefix, char pathSeparator, String otherPathPrefix, char otherPathSeparator) throws OutOfScope {
        if (!path.startsWith(pathPrefix)) {
            throw new OutOfScope(path, pathPrefix);
        }
        int l = pathPrefix.length();
        if (!isRootPath(pathPrefix)) { // When the prefix is the root, do not do further checks.
            if (path.length() > l && !isSeparator(path.charAt(l))) {
                throw new OutOfScope(path, pathPrefix);
            }
        }
        while (path.length() > l && isSeparator(path.charAt(l))) {
            l++;
        }
        String otherPath = path.substring(l);
        if (pathSeparator != otherPathSeparator) {
            otherPath = otherPath.replace(pathSeparator, otherPathSeparator);
        }
        if (otherPath.isEmpty()) {
            return otherPathPrefix;
        } else {
            if (isRootPath(otherPathPrefix)) { // Do not append further slashes to the root
                return otherPathPrefix + otherPath;
            } else {
                return otherPathPrefix + otherPathSeparator + otherPath;
            }
        }
    }
    
    private static char findSeparator(String path) {
        if (path.indexOf('/') >= 0) {
            return '/';
        }
        if (path.indexOf('\\') >= 0) {
            return '\\';
        }
        return '/';
    }

    private static boolean isSeparator(char c) {
        return c == '/' || c == '\\';
    }
    
    private static String stripSeparator(String path) {
        if (isRootPath(path)) { // Do not remove slashes the root
            return path;
        }
        while (path.length() > 1 && (path.endsWith("/") || path.endsWith("\\"))) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
    
    private static boolean isRootPath(String path) {
        if ("/".equals(path)) {
            return true;
        }
        if (path.length() == 4 && path.endsWith(":\\\\")) { // "C:\\"
            return true;
        }
        return false;
    }

    public static final class OutOfScope extends Exception {
        
        private OutOfScope(String path, String scope) {
            super(path);
        }
    }
    
    private static final class ScriptContentLoader implements SourceContent,
                                                              V8Debugger.CommandResponseCallback {
        
        private final V8Script script;
        private final V8Debugger dbg;
        private String content;
        private final Object contentLock = new Object();
        private String contentLoadError;
        
        public ScriptContentLoader(V8Script script, V8Debugger dbg) {
            this.script = script;
            this.dbg = dbg;
        }

        @NbBundle.Messages({ "ERR_NoSourceRequest=No source request has been sent.",
                             "ERR_Interrupted=Interrupted" })
        @Override
        public String getContent() throws IOException {
            if (content != null) {
                return content;
            }
            V8Script.Type st = script.getScriptType();
            V8Script.Types types = new V8Script.Types(st.NATIVE == st, st.EXTENSION == st, st.NORMAL == st);
            Scripts.Arguments sa = new Scripts.Arguments(types, new long[] { script.getId() },
                                                         true, null);
            V8Request request = dbg.sendCommandRequest(V8Command.Scripts, sa, this);
            if (request == null) {
                throw new IOException(Bundle.ERR_NoSourceRequest());
            }
            synchronized (contentLock) {
                if (content == null && contentLoadError == null) {
                    try {
                        contentLock.wait();
                    } catch (InterruptedException iex) {
                        throw new IOException(Bundle.ERR_Interrupted(), iex);
                    }
                }
                if (contentLoadError != null) {
                    throw new IOException(contentLoadError);
                } else {
                    return content;
                }
            }
        }

        @Override
        public long getLength() {
            return script.getSourceLength().getValue();
        }

        @NbBundle.Messages({ "ERR_ScriptFailedToLoad=The script failed to load.",
                             "ERR_ScriptHasNoSource=The script has no source." })
        @Override
        public void notifyResponse(V8Request request, V8Response response) {
            V8Script[] scripts;
            if (response != null) {
                Scripts.ResponseBody srb = (Scripts.ResponseBody) response.getBody();
                scripts = srb.getScripts();
            } else {
                scripts = null;
            }
            synchronized (contentLock) {
                if (scripts == null || scripts.length == 0) {
                    contentLoadError = Bundle.ERR_ScriptFailedToLoad();
                } else {
                    String source = scripts[0].getSource();
                    if (source == null) {
                        contentLoadError = Bundle.ERR_ScriptHasNoSource();
                    } else {
                        content = source;
                    }
                }
                contentLock.notifyAll();
            }
        }
    }

}
