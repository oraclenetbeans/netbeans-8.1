/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.autoupdate.pluginimporter;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.autoupdate.UpdateUnitProvider;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    public static final String KEY_IMPORT_FROM = "import-from";
    public static final String CODE_NAME = "ClusterUpdateProvider";
    public static final String REMOVED = "_removed"; // NOI18N

    private static final Logger LOG = Logger.getLogger (Installer.class.getName ());
    // XXX: copy from o.n.upgrader
    private static final List<String> VERSION_TO_CHECK =
            Arrays.asList (".netbeans/7.1.2", ".netbeans/7.1.1", ".netbeans/7.1", ".netbeans/7.0", ".netbeans/6.9"); //NOI18N
    private static final List<String> NEWER_VERSION_TO_CHECK =
            Arrays.asList ("8.0.2", "8.0.1", "8.0", "7.4", "7.3.1", "7.3", "7.2.1", "7.2"); //NOI18N
    private static final String IMPORTED = "imported"; // NOI18N

    @Override
    public void restored () {
        // don't try to invoke at all in these special cases
        if (Boolean.getBoolean("netbeans.full.hack") || Boolean.getBoolean("netbeans.close")) { // NOI18N
            return;
        }
        
        // remove ClusterUpdateProvider from available update providers
        Preferences au_pref = NbPreferences.root ().node ("/org/netbeans/modules/autoupdate"); // NOI18N
        au_pref.node (Installer.CODE_NAME + Installer.REMOVED).putBoolean (Installer.REMOVED, true);

        // install plugin importer when UI is ready (main window shown)
        WindowManager.getDefault ().invokeWhenUIReady (new Runnable () {

            @Override
            public void run () {
                RequestProcessor.getDefault ().post (doCheck, getImportDelay ()); // XXX: Need to wait until UC downloaded&parsed
            }
        });
    }

    private Runnable doCheck = new Runnable () {
        @Override
        public void run () {
            // check user wants to import previous userdir
            File importFrom = null;
            String from = System.getProperty ("plugin.manager.import.from", ""); // NOI18N
            Preferences pref = NbPreferences.forModule (Installer.class);
            Preferences au_pref = NbPreferences.root ().node ("/org/netbeans/modules/autoupdate"); // NOI18N
            if (from.length () > 0) {
                importFrom = new File (from);
            } else if (pref.get (KEY_IMPORT_FROM, null) != null) {
                // was remind later
                importFrom = new File (pref.get (KEY_IMPORT_FROM, "")); // NOI18N
            } else {
                // try OS specific place for userdir (see issue 196075)
                importFrom = checkPreviousOnOsSpecificPlace (NEWER_VERSION_TO_CHECK);
                if (importFrom == null) {
                    // try former root
                    importFrom = checkPrevious (VERSION_TO_CHECK);
                }

                // check if the userdir was imported already
                boolean imported = au_pref.getBoolean (IMPORTED, false);
                if (! imported) {
                    // don't import
                    importFrom = null;
                }
            }

            // don't import again from previous userdir
            au_pref.putBoolean (IMPORTED, false);

            if (importFrom == null || ! importFrom.exists ()) {
                // nothing to do => return
                LOG.fine ("Nothing to import from " + importFrom); // NOI18N
                return ;
            }
            try {
                // XXX: Hack Autoupdate API
                // find own provider
                Preferences p = au_pref.node (CODE_NAME + REMOVED);
                p.removeNode ();
            } catch (BackingStoreException ex) {
                LOG.log (Level.INFO, ex.getLocalizedMessage (), ex);
                return ;
            }
            UpdateUnitProvider clusterUpdateProvider = null;
            for (UpdateUnitProvider p : UpdateUnitProviderFactory.getDefault ().getUpdateUnitProviders (false)) {
                if (CODE_NAME.contains (p.getName ())) {
                    clusterUpdateProvider = p;
                }
            }
            assert clusterUpdateProvider != null : "clusterUpdateProvider must found";
            if (clusterUpdateProvider != null) {
                try {
                    assert importFrom != null && importFrom.exists () : importFrom + " exists.";
                    ClusterUpdateProvider.attachCluster (importFrom);
                    Collection<UpdateUnit> units = clusterUpdateProvider.getUpdateUnits (UpdateManager.TYPE.MODULE);
                    UpdateUnitProviderFactory.getDefault ().remove (clusterUpdateProvider);
                    PluginImporter importer = new PluginImporter (units);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine ("Already installed plugins: " + importer.getInstalledPlugins ());
                        LOG.fine ("Plugins available on UC: " + importer.getPluginsAvailableToInstall ());
                        LOG.fine ("Plugins available for import: " + importer.getPluginsToImport ());
                    }
                    if (! importer.getBrokenPlugins ().isEmpty ()) {
                        LOG.info ("Plugins for import with broken dependencies: " + importer.getBrokenPlugins ());
                    }
                    if ( ! importer.getPluginsToImport ().isEmpty () ||  ! importer.getPluginsAvailableToInstall ().isEmpty ()) {
                        LOG.info ((importer.getPluginsToImport ().size () + importer.getPluginsAvailableToInstall ().size ()) +
                                " available plugins for import in " + importFrom); // NOI18N
                        ImportManager notifier = new ImportManager (importFrom, getUserDir (), importer);
                        notifier.notifyAvailable ();
                    } else {
                        LOG.fine ((importer.getPluginsToImport ().size () + importer.getPluginsAvailableToInstall ().size ()) +
                                " available plugins for import in " + importFrom); // NOI18N
                    }
                } catch (Exception x) {
                    LOG.log (Level.INFO, x.getLocalizedMessage () + " while importing plugins from " + importFrom, x);
                } finally {
                    UpdateUnitProviderFactory.getDefault ().remove (clusterUpdateProvider);
                }
            }
        }
    };

    private static File getUserDir () {
        // bugfix #50242: the property "netbeans.user" can return dir with non-normalized file e.g. duplicate //
        // and path and value of this property wrongly differs
        String user = System.getProperty ("netbeans.user"); // NOI18N
        File userDir = null;
        if (user != null) {
            userDir = FileUtil.normalizeFile(new File(user));
        }

        return userDir;
    }

    // XXX: copy from o.n.upgrader
    private static File checkPrevious (final List<String> versionsToCheck) {
        String userHome = System.getProperty ("user.home"); // NOI18N
        File sourceFolder;
        if (userHome != null) {
            File userHomeFile = new File (userHome);
            for (String ver : versionsToCheck) {
                sourceFolder = new File (userHomeFile.getAbsolutePath (), ver);
                if (sourceFolder.exists () && sourceFolder.isDirectory ()) {
                    return sourceFolder;
                }
            }
        }
        return null;
    }

    private static File checkPreviousOnOsSpecificPlace (final List<String> versionsToCheck) {
        String defaultUserdirRoot = System.getProperty ("netbeans.default_userdir_root"); // NOI18N
        LOG.log (Level.FINER, "netbeans.default_userdir_root: " + defaultUserdirRoot);
        File sourceFolder;
        if (defaultUserdirRoot != null) {
            File userHomeFile = new File (defaultUserdirRoot);
            for (String ver : versionsToCheck) {
                sourceFolder = new File (userHomeFile.getAbsolutePath (), ver);
                if (sourceFolder.exists () && sourceFolder.isDirectory ()) {
                    return sourceFolder;
                }
            }
        }
        return null;
    }

    private int getImportDelay () {
        int delay = 50000; // the defalut value
        String delay_prop = System.getProperty ("plugin.manager.import.delay");
        try {
            delay = Integer.parseInt (delay_prop);
        } catch (NumberFormatException x) {
            // ignore, use the default value
        }
        return delay;
    }
}
