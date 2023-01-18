/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.project.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.PlatformsCustomizer;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.libraries.LibrariesCustomizer;
import org.netbeans.api.project.libraries.Library;
import org.netbeans.api.project.libraries.LibraryManager;
import org.netbeans.modules.java.project.ui.FixProjectSourceLevel;
import org.netbeans.modules.java.project.ui.ProfileProblemsProviderImpl;
import org.netbeans.spi.project.libraries.support.LibrariesSupport;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.PropertyEvaluator;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.netbeans.spi.project.support.ant.ReferenceHelper;
import org.netbeans.spi.project.ui.ProjectProblemsProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

import static org.netbeans.modules.java.project.ui.Bundle.*;
import org.netbeans.spi.java.project.support.ui.BrokenReferencesSupport;
import org.netbeans.spi.project.support.ant.ui.VariablesSupport;
import org.netbeans.spi.project.ui.ProjectProblemResolver;
import org.netbeans.spi.project.ui.ProjectProblemsProvider.Result;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.netbeans.spi.project.ui.support.ProjectProblemsProviderSupport;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

/**
 *
 * @author Tomas Zezula
 */
public class ProjectProblemsProviders {

    private static final Logger LOG = Logger.getLogger(ProjectProblemsProviders.class.getName());
    private static final RequestProcessor RP = new RequestProcessor(ProjectProblemsProviders.class);

    private ProjectProblemsProviders() {
        throw new IllegalStateException(String.format("The %s cannot be instantiated.",this.getClass().getName())); //NOI18N
    }


    @NonNull
    public static ProjectProblemsProvider createReferenceProblemProvider(
            @NonNull final AntProjectHelper projectHelper,
            @NonNull final ReferenceHelper referenceHelper,
            @NonNull final PropertyEvaluator evaluator,
            @NonNull final String[] properties,
            @NonNull final String[] platformProperties) {
        final ReferenceProblemProviderImpl pp = new ReferenceProblemProviderImpl(projectHelper, evaluator, referenceHelper, properties, platformProperties);
        pp.attachListeners();
        return pp;
    }

    @NonNull
    public static ProjectProblemsProvider createPlatformVersionProblemProvider(
            @NonNull final AntProjectHelper helper,
            @NonNull final PropertyEvaluator evaluator,
            @NullAllowed final BrokenReferencesSupport.PlatformUpdatedCallBack hook,
            @NonNull final String platformType,
            @NonNull final String platformProperty,
            @NonNull final String... platformVersionProperties) {
        final PlatformVersionProblemProviderImpl pp = new PlatformVersionProblemProviderImpl(
                helper,
                evaluator,
                hook,
                platformType,
                platformProperty,
                platformVersionProperties);
        pp.attachListeners();
        return pp;
    }

    @NonNull
    public static ProjectProblemsProvider createProfileProblemProvider(
            @NonNull final AntProjectHelper antProjectHelper,
            @NonNull final ReferenceHelper refHelper,
            @NonNull final PropertyEvaluator evaluator,
            @NonNull final String profileProperty,
            @NonNull final String... classPathProperties) {
        return new ProfileProblemsProviderImpl(
                antProjectHelper,
                refHelper,
                evaluator,
                profileProperty,
                classPathProperties);
    }

    //<editor-fold defaultstate="collapsed" desc="Helper Methods & Types">
    @NonNull
    private static Set<? extends ProjectProblemsProvider.ProjectProblem> getReferenceProblems(
            @NullAllowed final AntProjectHelper helper,
            @NullAllowed final PropertyEvaluator evaluator,
            @NullAllowed final ReferenceHelper refHelper,
            @NonNull final String[] ps,
            @NullAllowed final Collection<? super File> files,
            final boolean abortAfterFirstProblem) {
        Set<ProjectProblemsProvider.ProjectProblem> set = new LinkedHashSet<ProjectProblemsProvider.ProjectProblem>();
        StringBuilder all = new StringBuilder();
        // this call waits for list of libraries to be refreshhed
        LibraryManager.getDefault().getLibraries();
        if (helper == null || evaluator == null || refHelper == null) {
            return set;
        }
        final Queue<FileResolver> fileReoslvers = new ArrayDeque<FileResolver>();
        EditableProperties ep = helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        for (String p : ps) {
            // evaluate given property and tokenize it

            String prop = evaluator.getProperty(p);
            if (prop == null) {
                continue;
            }
            LOG.log(Level.FINE, "Evaluated {0}={1}", new Object[] {p, prop});
            String[] vals = PropertyUtils.tokenizePath(prop);

            // no check whether after evaluating there are still some
            // references which could not be evaluated
            for (String v : vals) {
                // we are checking only: project reference, file reference, library reference
                if (!(v.startsWith("${file.reference.") ||
                      v.startsWith("${project.") ||
                      (v.startsWith("${libs.") && v.endsWith(".classpath}"))  ||
                      v.startsWith("${var."))) { // NOI18N
                    all.append(v);
                    continue;
                }
                if (v.startsWith("${project.")) { // NOI18N
                    // something in the form: "${project.<projID>}/dist/foo.jar"
                    String val = v.substring(2, v.indexOf('}')); // NOI18N
                    set.add(
                        ProjectProblemsProvider.ProjectProblem.createError(
                            getDisplayName(RefType.PROJECT, val),
                            getDescription(RefType.PROJECT, val),
                            new ProjectResolver(val, helper)));
                } else {
                    final String val = v.substring(2, v.length() - 1);
                    final ProjectProblemsProvider.ProjectProblem problem;
                    if (v.startsWith("${file.reference")) { // NOI18N
                        final FileResolver fr = new FileResolver(val, helper, fileReoslvers);
                        fileReoslvers.offer(fr);
                        problem = ProjectProblemsProvider.ProjectProblem.createError(
                            getDisplayName(RefType.FILE, val),
                            getDescription(RefType.FILE, val),
                            fr);

                    } else if (v.startsWith("${var")) { // NOI18N
                        problem = ProjectProblemsProvider.ProjectProblem.createError(
                            getDisplayName(RefType.VARIABLE, v),
                            getDescription(RefType.VARIABLE, v),
                            new VariableResolver(RefType.VARIABLE, v));
                    } else {
                        // Since 8.1, "junit" library definition and junit-3.8.2 binaries
                        // were removed. Project problems are handled now from class
                        // org.netbeans.modules.junit.ant.ui.JUnitProjectOpenedHook.
                        if (val.equals("libs.junit.classpath")) {
                            continue;
                        }
                        problem = ProjectProblemsProvider.ProjectProblem.createError(
                            getDisplayName(RefType.LIBRARY, val),
                            getDescription(RefType.LIBRARY, val),
                            new LibraryResolver(RefType.LIBRARY, val, refHelper));
                    }
                    set.add(problem);
                }
                if (abortAfterFirstProblem) {
                    break;
                }
            }
            if (set.size() > 0 && abortAfterFirstProblem) {
                break;
            }

            // test that resolved variable based property points to an existing file
            String path = ep.getProperty(p);
            if (path != null) {
                for (String v : PropertyUtils.tokenizePath(path)) {
                    if (v.startsWith("${file.reference.")) {    //NOI18N
                        v = ep.getProperty(v.substring(2, v.length() - 1));
                    }
                    if (v != null && v.startsWith("${var.")) {    //NOI18N
                        String value = evaluator.evaluate(v);
                        if (value.startsWith("${var.")) { // NOI18N
                            // this problem was already reported
                            continue;
                        }
                        File f = getFile(helper, evaluator, value);
                        if (files != null) {
                            files.add(f);
                        }
                        if (f.exists()) {
                            continue;
                        }
                        set.add(
                            ProjectProblemsProvider.ProjectProblem.createError(
                                getDisplayName(RefType.VARIABLE_CONTENT, v),
                                getDescription(RefType.VARIABLE_CONTENT, v),
                                new VariableResolver(RefType.VARIABLE_CONTENT, v)));
                    }
                }
            }

        }

        // Check also that all referenced project really exist and are reachable.
        // If they are not report them as broken reference.
        // XXX: there will be API in PropertyUtils for listing of Ant
        // prop names in String. Consider using it here.
        final Map<String, String> entries = evaluator.getProperties();
        if (entries == null) {
            throw new IllegalArgumentException("Properies mapping could not be computed (e.g. due to a circular definition). Evaluator: "+evaluator.toString());  //NOI18N
        }
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith("project.")) { // NOI18N
                if ("project.license".equals(key)) {    //NOI18N
                    continue;
                }
                File f = getFile(helper, evaluator, value);
                if (files != null) {
                    files.add(f);
                }
                if (f.exists()) {
                    continue;
                }
                // Check that the value is really used by some property.
                // If it is not then ignore such a project.
                if (all.indexOf(value) == -1) {
                    continue;
                }
                set.add(
                    ProjectProblemsProvider.ProjectProblem.createError(
                        getDisplayName(RefType.PROJECT, key),
                        getDescription(RefType.PROJECT, key),
                        new ProjectResolver(key, helper)));
            }
            else if (key.startsWith("file.reference")) {    //NOI18N
                File f = getFile(helper, evaluator, value);
                if (files != null) {
                    files.add(f);
                }
                String unevaluatedValue = ep.getProperty(key);
                boolean alreadyChecked = unevaluatedValue != null ? unevaluatedValue.startsWith("${var.") : false; // NOI18N
                if (f.exists() || all.indexOf(value) == -1 || alreadyChecked) { // NOI18N
                    continue;
                }
                final FileResolver fr = new FileResolver(key, helper, fileReoslvers);
                fileReoslvers.offer(fr);
                set.add(
                    ProjectProblemsProvider.ProjectProblem.createError(
                        getDisplayName(RefType.FILE, key),
                        getDescription(RefType.FILE, key),
                        fr));
            }
        }

        //Check for libbraries with broken classpath content
        Set<String> usedLibraries = new HashSet<String>();
        Pattern libPattern = Pattern.compile("\\$\\{(libs\\.[-._a-zA-Z0-9]+\\.classpath)\\}"); //NOI18N
        for (String p : ps) {
            String propertyValue = ep.getProperty(p);
            if (propertyValue != null) {
                for (String v : PropertyUtils.tokenizePath(propertyValue)) {
                    Matcher m = libPattern.matcher(v);
                    if (m.matches()) {
                        usedLibraries.add (m.group(1));
                    }
                }
            }
        }
        for (String libraryRef : usedLibraries) {
            String libraryName = libraryRef.substring(5,libraryRef.length()-10);
            Library lib = refHelper.findLibrary(libraryName);
            if (lib == null) {
                // Since 8.1, "junit" library definition and junit-3.8.2 binaries
                // were removed. Project problems are handled now from class
                // org.netbeans.modules.junit.ant.ui.JUnitProjectOpenedHook.
                if(libraryName.equals("junit")) {
                    continue;
                }
                // Should already have been caught before?
                set.add(
                    ProjectProblemsProvider.ProjectProblem.createError(
                        getDisplayName(RefType.LIBRARY, libraryRef),
                        getDescription(RefType.LIBRARY, libraryRef),
                        new LibraryResolver(RefType.LIBRARY, libraryRef, refHelper)));
            }
            else {
                //XXX: Should check all the volumes (sources, javadoc, ...)?
                for (URI uri : lib.getURIContent("classpath")) { // NOI18N
                    URI uri2 = LibrariesSupport.getArchiveFile(uri);
                    if (uri2 == null) {
                        uri2 = uri;
                    }
                    FileObject fo = LibrariesSupport.resolveLibraryEntryFileObject(lib.getManager().getLocation(), uri2);
                    if (null == fo && !canResolveEvaluatedUri(helper.getStandardPropertyEvaluator(), lib.getManager().getLocation(), uri2)) {
                        set.add(
                            ProjectProblemsProvider.ProjectProblem.createError(
                                getDisplayName(RefType.LIBRARY_CONTENT, libraryRef),
                                getDescription(RefType.LIBRARY_CONTENT, libraryRef),
                                new LibraryResolver(RefType.LIBRARY_CONTENT, libraryRef, refHelper)));
                        break;
                    }
                }
            }
        }

        return set;
    }

    @NonNull
    private static Set<ProjectProblemsProvider.ProjectProblem> getPlatformProblems(
            @NullAllowed final PropertyEvaluator evaluator,
            @NonNull final String[] platformProperties,
            boolean abortAfterFirstProblem) {
        final Set<ProjectProblemsProvider.ProjectProblem> set = new LinkedHashSet<ProjectProblemsProvider.ProjectProblem>();
        if (evaluator == null) {
            return set;
        }
        for (String pprop : platformProperties) {
            String prop = evaluator.getProperty(pprop);
            if (prop == null) {
                continue;
            }
            if (!existPlatform(prop)) {

                // XXX: the J2ME stores in project.properties also platform
                // display name and so show this display name instead of just
                // prop ID if available.
                if (evaluator.getProperty(pprop + ".description") != null) { // NOI18N
                    prop = evaluator.getProperty(pprop + ".description"); // NOI18N
                }

                set.add(
                    ProjectProblemsProvider.ProjectProblem.createError(
                        getDisplayName(RefType.PLATFORM, prop),
                        getDescription(RefType.PLATFORM, prop),
                        new PlatformResolver(prop)));
            }
            if (set.size() > 0 && abortAfterFirstProblem) {
                break;
            }
        }
        return set;
    }

    private static File getFile (AntProjectHelper helper, PropertyEvaluator evaluator, String name) {
        if (helper != null) {
            return new File(helper.resolvePath(name));
        } else {
            File f = new File(name);
            if (!f.exists()) {
                // perhaps the file is relative?
                String basedir = evaluator.getProperty("basedir"); // NOI18N
                assert basedir != null;
                f = new File(new File(basedir), name);
            }
            return f;
        }
    }

    /** Tests whether evaluated URI can be resolved. To support library entry
     * like "${MAVEN_REPO}/struts/struts.jar".
     */
    private static boolean canResolveEvaluatedUri(PropertyEvaluator eval, URL libBase, URI libUri) {
        if (libUri.isAbsolute()) {
            return false;
        }
        String path = LibrariesSupport.convertURIToFilePath(libUri);
        String newPath = eval.evaluate(path);
        if (newPath.equals(path)) {
            return false;
        }
        URI newUri = LibrariesSupport.convertFilePathToURI(newPath);
        return null != LibrariesSupport.resolveLibraryEntryFileObject(libBase, newUri);
    }

    private static boolean existPlatform(String platform) {
        if (platform.equals("default_platform")) { // NOI18N
            return true;
        }
        for (JavaPlatform plat : JavaPlatformManager.getDefault().getInstalledPlatforms()) {
            // XXX: this should be defined as PROPERTY somewhere
            if (platform.equals(plat.getProperties().get("platform.ant.name")) && // NOI18N
                    plat.getInstallFolders().size() > 0) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @NbBundle.Messages({
        "LBL_BrokenLinksCustomizer_BrokenLibrary=\"{0}\" library could not be found",
        "LBL_BrokenLinksCustomizer_BrokenDefinableLibrary=\"{0}\" library must be defined",
        "LBL_BrokenLinksCustomizer_BrokenLibraryContent=\"{0}\" library has missing items",
        "LBL_BrokenLinksCustomizer_BrokenProjectReference=\"{0}\" project could not be found",
        "LBL_BrokenLinksCustomizer_BrokenFileReference=\"{0}\" file/folder could not be found",
        "LBL_BrokenLinksCustomizer_BrokenVariable=\"{0}\" variable could not be found",
        "LBL_BrokenLinksCustomizer_BrokenVariableContent=\"{0}\" variable based file/folder could not be found",
        "LBL_BrokenLinksCustomizer_BrokenPlatform=\"{0}\" platform could not be found"
    })
    private static String getDisplayName(
            @NonNull final RefType type,
            @NonNull final String id) {
        switch (type) {
            case LIBRARY:
                return LBL_BrokenLinksCustomizer_BrokenLibrary(getDisplayId(type, id));
            case DEFINABLE_LIBRARY:
                return LBL_BrokenLinksCustomizer_BrokenDefinableLibrary(getDisplayId(type, id));
            case LIBRARY_CONTENT:
                return LBL_BrokenLinksCustomizer_BrokenLibraryContent(getDisplayId(type, id));
            case PROJECT:
                return LBL_BrokenLinksCustomizer_BrokenProjectReference(getDisplayId(type, id));
            case FILE:
                return LBL_BrokenLinksCustomizer_BrokenFileReference(getDisplayId(type, id));
            case PLATFORM:
                return LBL_BrokenLinksCustomizer_BrokenPlatform(getDisplayId(type, id));
            case VARIABLE:
                return LBL_BrokenLinksCustomizer_BrokenVariable(getDisplayId(type, id));
            case VARIABLE_CONTENT:
                return LBL_BrokenLinksCustomizer_BrokenVariableContent(getDisplayId(type, id));
            default:
                assert false;
                return id;
        }
    }

    @NbBundle.Messages({
        "LBL_BrokenLinksCustomizer_BrokenLibraryDesc=Problem: The project uses a class library called \"{0}\", but this class library was not found.\nSolution: Click Resolve to open the Library Manager and create a new class library called \"{0}\".",
        "LBL_BrokenLinksCustomizer_BrokenDefinableLibraryDesc=Problem: The project uses a class library called \"{0}\", but this class library is not currently defined locally.\nSolution: Click Resolve to download or otherwise automatically define this library.",
        "LBL_BrokenLinksCustomizer_BrokenLibraryContentDesc=Problem: The project uses the class library called \"{0}\" but the classpath items of this library are missing.\nSolution: Click Resolve to open the Library Manager and locate the missing classpath items of \"{0}\" library.",
        "LBL_BrokenLinksCustomizer_BrokenProjectReferenceDesc=Problem: The project classpath includes a reference to the project called \"{0}\", but this project was not found.\nSolution: Click Resolve and locate the missing project.",
        "LBL_BrokenLinksCustomizer_BrokenFileReferenceDesc=Problem: The project uses the file/folder called \"{0}\", but this file/folder was not found.\nSolution: Click Resolve and locate the missing file/folder.",
        "LBL_BrokenLinksCustomizer_BrokenVariableReferenceDesc=Problem: The project uses the variable called \"{0}\", but this variable was not found.\nSolution: Click Resolve and setup this variable there.",
        "LBL_BrokenLinksCustomizer_BrokenVariableContentDesc=Problem: The project uses the variable based file/folder \"{0}\", but this file/folder was not found.\nSolution: Click Resolve and update your variable to point to correct location.",
        "LBL_BrokenLinksCustomizer_BrokenPlatformDesc=Problem: The project uses the Java Platform called \"{0}\", but this platform was not found.\nSolution: Click Resolve and create new platform called \"{0}\"."
    })
    private static String getDescription(
            @NonNull final RefType type,
            @NonNull final String id
            ) {
        switch (type) {
            case LIBRARY:
                return LBL_BrokenLinksCustomizer_BrokenLibraryDesc(getDisplayId(type, id));
            case DEFINABLE_LIBRARY:
                return LBL_BrokenLinksCustomizer_BrokenDefinableLibraryDesc(getDisplayId(type, id));
            case LIBRARY_CONTENT:
                return LBL_BrokenLinksCustomizer_BrokenLibraryContentDesc(getDisplayId(type, id));
            case PROJECT:
                return LBL_BrokenLinksCustomizer_BrokenProjectReferenceDesc(getDisplayId(type, id));
            case FILE:
                return LBL_BrokenLinksCustomizer_BrokenFileReferenceDesc(getDisplayId(type, id));
            case PLATFORM:
                return LBL_BrokenLinksCustomizer_BrokenPlatformDesc(getDisplayId(type, id));
            case VARIABLE:
                return LBL_BrokenLinksCustomizer_BrokenVariableReferenceDesc(getDisplayId(type, id));
            case VARIABLE_CONTENT:
                return LBL_BrokenLinksCustomizer_BrokenVariableContent(getDisplayId(type, id));
            default:
                assert false;
                return id;
        }
    }

    private static String getDisplayId(
            @NonNull final RefType type,
            @NonNull final String id) {
        switch (type) {
            case LIBRARY:
            case DEFINABLE_LIBRARY:
            case LIBRARY_CONTENT:
                // libs.<name>.classpath
                return id.substring(5, id.length()-10);
            case PROJECT:
                // project.<name>
                return id.substring(8);
            case FILE:
                // file.reference.<name>
                return id.substring(15);
            case PLATFORM:
                return id;
            case VARIABLE:
                return id.substring(6, id.indexOf('}')); // NOI18N
            case VARIABLE_CONTENT:
                return id.substring(6, id.indexOf('}')) + id.substring(id.indexOf('}')+1); // NOI18N
            default:
                assert false;
                return id;
        }
    }

    private enum RefType {
        PROJECT,
        FILE,
        PLATFORM,
        LIBRARY,
        DEFINABLE_LIBRARY,
        LIBRARY_CONTENT,
        VARIABLE,
        VARIABLE_CONTENT,
    }
    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="Resolver implementations">
    private static abstract class BaseResolver implements ProjectProblemResolver {

        protected final RefType type;
        protected final String id;

        BaseResolver(
            @NonNull final RefType type,
            @NonNull final String id) {
            Parameters.notNull("type", type);   //NOI18N
            Parameters.notNull("id", id);   //NOI18N
            this.type = type;
            this.id = id;
        }

        @Override
        public final int hashCode() {
            int result = 17;
            result = 31 * result + type.hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }

        @Override
        public final boolean equals(@NullAllowed final Object other) {
            if (!(other instanceof BaseResolver)) {
                return false;
            }
            final BaseResolver otherResolver = (BaseResolver) other;
            return type.equals(otherResolver.type) &&
                   id.equals(otherResolver.id);
        }

        @Override
        public String toString() {
            return String.format(
               "Resolver for %s %s",    //NOI18N
               type,
               id);
        }



    }

    private static class PlatformResolver extends BaseResolver {

        PlatformResolver(@NonNull final String id) {
            super(RefType.PLATFORM, id);
        }

        @Override
        @NonNull
        public Future<ProjectProblemsProvider.Result> resolve() {
            PlatformsCustomizer.showCustomizer(null);
            return new Done(ProjectProblemsProvider.Result.create(ProjectProblemsProvider.Status.RESOLVED));
        }

    }

    private static class LibraryResolver extends BaseResolver {

        private final Callable<Library> definer;
        private final Reference<ReferenceHelper> refHelper;

        LibraryResolver(
                @NonNull RefType type,
                @NonNull final String id,
                @NonNull final ReferenceHelper refHelper) {
            this(translate(type, id),id, refHelper);
        }

        private LibraryResolver(
                @NonNull final Object[] typeDefiner/*todo: replace by Pair*/,
                @NonNull final String id,
                @NonNull final ReferenceHelper refHelper) {
            super((RefType)typeDefiner[0],id);
            this.definer = (Callable<Library>)typeDefiner[1];
            this.refHelper = new WeakReference<ReferenceHelper>(refHelper);
        }

        @Override
        @NonNull
        public Future<ProjectProblemsProvider.Result> resolve() {
            if (type == RefType.DEFINABLE_LIBRARY) {
                return resolveByDefiner();
            } else {
                return new Done(ProjectProblemsProvider.Result.create(resolveByLibraryManager()));
            }
        }

        private ProjectProblemsProvider.Status resolveByLibraryManager() {
            final LibraryManager lm = getProjectLibraryManager();
            if (lm == null) {
                //Closed and freed project
                return ProjectProblemsProvider.Status.UNRESOLVED;
            }
            LibrariesCustomizer.showCustomizer(null,lm);
            return ProjectProblemsProvider.Status.RESOLVED;
        }

        private Future<ProjectProblemsProvider.Result> resolveByDefiner() {
            assert definer != null;
            final RunnableFuture<ProjectProblemsProvider.Result> future =
                    new FutureTask<ProjectProblemsProvider.Result>(
                    new Callable<ProjectProblemsProvider.Result>() {
                        @Override
                        public ProjectProblemsProvider.Result call() throws Exception {
                            ProjectProblemsProvider.Status result = ProjectProblemsProvider.Status.UNRESOLVED;
                            try {
                                Library lib = definer.call();
                                LOG.log(Level.FINE, "found {0}", lib);  //NOI18N
                                result = ProjectProblemsProvider.Status.RESOLVED;
                            } catch (Exception x) {
                                LOG.log(Level.INFO, null, x);
                                result = resolveByLibraryManager();
                            }
                            return ProjectProblemsProvider.Result.create(result);
                        }
                    });
            RP.post(future);
            return future;
        }

        @CheckForNull
        private LibraryManager getProjectLibraryManager() {
            final ReferenceHelper rh = refHelper.get();
            return rh == null ?
                null:
                rh.getProjectLibraryManager() != null ?
                    rh.getProjectLibraryManager():
                    LibraryManager.getDefault();
        }

        @NonNull
        private static Object[] translate(
           @NonNull RefType original,
           @NonNull String id) {
            Callable<Library> _definer = null;
            if (original == RefType.LIBRARY) {
                final String name = id.substring(5, id.length() - 10);
                for (BrokenReferencesSupport.LibraryDefiner ld : Lookup.getDefault().lookupAll(BrokenReferencesSupport.LibraryDefiner.class)) {
                    _definer = ld.missingLibrary(name);
                    if (_definer != null) {
                        return new Object[] {RefType.DEFINABLE_LIBRARY, _definer};
                    }
                }
            }
            return new Object[] {original, null};
        }
    }

    private static class VariableResolver extends BaseResolver {
        VariableResolver(@NonNull final RefType type, @NonNull final String id) {
            super(type, id);
        }

        @Override
        @NonNull
        public Future<ProjectProblemsProvider.Result> resolve() {
            VariablesSupport.showVariablesCustomizer();
            return new Done(ProjectProblemsProvider.Result.create(ProjectProblemsProvider.Status.RESOLVED));
        }
    }

    private static abstract class ReferenceResolver extends BaseResolver {

        static File lastSelectedFile;

        private final Reference<AntProjectHelper> antProjectHelper;

        ReferenceResolver(
                @NonNull final RefType type,
                @NonNull final String id,
                @NonNull final AntProjectHelper antProjectHelper) {
            super (type, id);
            this.antProjectHelper = new WeakReference<AntProjectHelper>(antProjectHelper);
        }

        abstract void updateReference(@NonNull final File file);

        final void updateReferenceImpl(@NonNull final File file) {
            final String reference = id;
            final AntProjectHelper helper = antProjectHelper.get();
            if (helper == null) {
                //Closed and freed project, ignore
                return;
            }
            FileObject myProjDirFO = helper.getProjectDirectory();
            final String propertiesFile = AntProjectHelper.PRIVATE_PROPERTIES_PATH;
            final String path = file.getAbsolutePath();
            Project p;
            try {
                p = ProjectManager.getDefault().findProject(myProjDirFO);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                p = null;
            }
            final Project proj = p;
            ProjectManager.mutex().postWriteRequest(new Runnable() {
                    public @Override void run() {
                        EditableProperties props = helper.getProperties(propertiesFile);
                        if (!path.equals(props.getProperty(reference))) {
                            props.setProperty(reference, path);
                            helper.putProperties(propertiesFile, props);
                        }

                        if (proj != null) {
                            try {
                                ProjectManager.getDefault().saveProject(proj);
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    }
                });
        }
    }

    private static class ProjectResolver extends ReferenceResolver {
        ProjectResolver(@NonNull final String id, @NonNull AntProjectHelper antProjectHelper) {
            super (RefType.PROJECT, id, antProjectHelper);
        }

        @Override
        @NonNull
        @NbBundle.Messages({
            "LBL_BrokenLinksCustomizer_Resolve_Project=Browse Project \"{0}\""
        })
        public Future<ProjectProblemsProvider.Result> resolve() {
            ProjectProblemsProvider.Status result = ProjectProblemsProvider.Status.UNRESOLVED;
            final JFileChooser chooser = ProjectChooser.projectChooser();
            chooser.setDialogTitle(LBL_BrokenLinksCustomizer_Resolve_Project(getDisplayId(type, id)));
            if (lastSelectedFile != null) {
                chooser.setSelectedFile(lastSelectedFile);
            }
            int option = chooser.showOpenDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                updateReference(chooser.getSelectedFile());
                lastSelectedFile = chooser.getSelectedFile();
                result = ProjectProblemsProvider.Status.RESOLVED;
            }
            return new Done(ProjectProblemsProvider.Result.create(result));
        }

        @Override
        void updateReference(@NonNull final File file) {
            updateReferenceImpl(file);
        }

    }

    private static class FileResolver extends ReferenceResolver {

        private final Queue<? extends FileResolver> peers;
        private ProjectProblemsProvider.Status resolved =
                ProjectProblemsProvider.Status.UNRESOLVED;

        FileResolver(
                @NonNull final String id,
                @NonNull final AntProjectHelper antProjectHelper,
                @NonNull final Queue<? extends FileResolver> peers) {
            super(RefType.FILE, id, antProjectHelper);
            this.peers = peers;
        }

        @Override
        @NonNull
        @NbBundle.Messages({
            "LBL_BrokenLinksCustomizer_Resolve_File=Browse \"{0}\""
        })
        public Future<ProjectProblemsProvider.Result> resolve() {
            final JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setDialogTitle(LBL_BrokenLinksCustomizer_Resolve_File(getDisplayId(type, id)));
            if (lastSelectedFile != null) {
                chooser.setSelectedFile(lastSelectedFile);
            }
            int option = chooser.showOpenDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                updateReference(chooser.getSelectedFile());
                lastSelectedFile = chooser.getSelectedFile();
                resolved = ProjectProblemsProvider.Status.RESOLVED;
            }
            return new Done(ProjectProblemsProvider.Result.create(resolved));
        }

        @Override
        void updateReference(@NonNull final File file) {
            updateReferenceImpl(file);
            final File parentFolder = file.getParentFile();
            for (FileResolver peer : peers) {
                if (this != peer && peer.resolved == ProjectProblemsProvider.Status.UNRESOLVED) {
                    final File f = new File(parentFolder, getDisplayId(type, id));
                    if (f.exists()) {
                        updateReferenceImpl(f);
                    }
                }
            }
        }


    }

    private static class SourceTargetResolver implements ProjectProblemResolver {

        private final String type;
        private final String platformProp;
        private final Collection<? extends String> invalidVersionProps;
        private final SpecificationVersion minVersion;
        private final SpecificationVersion platformVersion;
        private final Reference<AntProjectHelper> helperRef;
        private final BrokenReferencesSupport.PlatformUpdatedCallBack hook;

        SourceTargetResolver(
            @NonNull final AntProjectHelper helper,
            @NullAllowed final BrokenReferencesSupport.PlatformUpdatedCallBack hook,
            @NonNull final String type,
            @NonNull final String platformProp,
            @NonNull final Collection<? extends String> invalidVersionProps,
            @NonNull final SpecificationVersion minVersion,
            @NonNull final SpecificationVersion platformVersion) {
            Parameters.notNull("helper", helper);   //NOI18N
            Parameters.notNull("type", type);   //NOI18N
            Parameters.notNull("platformProp", platformProp);   //NOI18N
            Parameters.notNull("invalidVersionProps", invalidVersionProps); //NOI18N
            Parameters.notNull("minVersion", minVersion);   //NOI18N
            Parameters.notNull("platformVersion", platformVersion); //NOI18N
            this.helperRef = new WeakReference<AntProjectHelper>(helper);
            this.hook = hook;
            this.type = type;
            this.platformProp = platformProp;
            this.invalidVersionProps = invalidVersionProps;
            this.minVersion = minVersion;
            this.platformVersion = platformVersion;
        }


        @NbBundle.Messages({"LBL_ResolveJDKVersion=Resolve Invalid Java Platform Version - \"{0}\" Project"})
        @Override
        public Future<Result> resolve() {
            final AntProjectHelper helper = helperRef.get();
            if (helper != null) {
                final Project project = FileOwnerQuery.getOwner(helper.getProjectDirectory());
                final FixProjectSourceLevel changeVersion = new FixProjectSourceLevel(type, minVersion, platformVersion);
                final DialogDescriptor dd = new DialogDescriptor(changeVersion, LBL_ResolveJDKVersion(ProjectUtils.getInformation(project).getDisplayName()));
                if (DialogDisplayer.getDefault().notify(dd) == DialogDescriptor.OK_OPTION) {
                    final Callable<ProjectProblemsProvider.Result> resultFnc =
                            new Callable<Result>() {
                        @Override
                        public Result call() throws Exception {
                            return ProjectManager.mutex().writeAccess(new Mutex.ExceptionAction<Result>(){
                                @Override
                                public Result run() throws IOException {
                                    if (changeVersion.isDowngradeLevel()) {
                                        final EditableProperties props = helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
                                        for (String p : invalidVersionProps) {
                                            props.put(p, platformVersion.toString());
                                        }
                                        helper.putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, props);
                                        ProjectManager.getDefault().saveProject(FileOwnerQuery.getOwner(helper.getProjectDirectory()));
                                        return ProjectProblemsProvider.Result.create(ProjectProblemsProvider.Status.RESOLVED);
                                    } else {
                                        final JavaPlatform jp = changeVersion.getSelectedPlatform();
                                        if (jp != null) {
                                            final String antName = jp.getProperties().get("platform.ant.name"); //NOI18N
                                            if (antName != null) {
                                                final EditableProperties props = helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
                                                props.setProperty(platformProp, antName);
                                                helper.putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, props);
                                                if (hook != null) {
                                                    hook.platformPropertyUpdated(jp);
                                                }
                                                ProjectManager.getDefault().saveProject(project);
                                                return ProjectProblemsProvider.Result.create(ProjectProblemsProvider.Status.RESOLVED);
                                            }
                                        }
                                    }
                                    return ProjectProblemsProvider.Result.create(ProjectProblemsProvider.Status.UNRESOLVED);
                                }
                            });
                        }
                    };
                    final RunnableFuture<Result> result = new FutureTask<Result>(resultFnc);
                    RP.post(result);
                    return result;
                }
            }
            return new Done(
                    Result.create(ProjectProblemsProvider.Status.UNRESOLVED));
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof SourceTargetResolver)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return 17;

        }


    }
        
    private static final class Done implements Future<ProjectProblemsProvider.Result> {

        private final ProjectProblemsProvider.Result result;

        Done(@NonNull final ProjectProblemsProvider.Result result) {
            Parameters.notNull("result", result);   //NOI18N
            this.result = result;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public ProjectProblemsProvider.Result get() throws InterruptedException, ExecutionException {
            return result;
        }

        @Override
        public ProjectProblemsProvider.Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }

    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="ProjectProblemsProvider implementations">
    private static final class ReferenceProblemProviderImpl implements ProjectProblemsProvider, PropertyChangeListener, FileChangeListener {

        private final ProjectProblemsProviderSupport problemsProviderSupport = new ProjectProblemsProviderSupport(this);
        private final AtomicBoolean listenersInitialized = new AtomicBoolean();

        private final AntProjectHelper helper;
        private final PropertyEvaluator eval;
        private final ReferenceHelper refHelper;
        private final String[] refProps;
        private final String[] platformProps;
        //@GuardedBy("this")
        private final Set<File> currentFiles;

        private Map<URL,Object[]> activeLibManLocs;


        ReferenceProblemProviderImpl(
                @NonNull final AntProjectHelper helper,
                @NonNull final PropertyEvaluator eval,
                @NonNull final ReferenceHelper refHelper,
                @NonNull final String[] refProps,
                @NonNull final String[] platformProps) {
            assert helper != null;
            assert eval != null;
            assert refHelper != null;
            assert refProps != null;
            assert platformProps != null;
            this.helper = helper;
            this.eval = eval;
            this.refHelper = refHelper;
            this.refProps = Arrays.copyOf(refProps, refProps.length);
            this.platformProps = Arrays.copyOf(platformProps, platformProps.length);
            this.currentFiles = new HashSet<>();
        }

        @Override
        public void addPropertyChangeListener(@NonNull final PropertyChangeListener listener) {
            Parameters.notNull("listener", listener);   //NOI18N
            problemsProviderSupport.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(@NonNull final PropertyChangeListener listener) {
            Parameters.notNull("listener", listener);   //NOI18N
            problemsProviderSupport.removePropertyChangeListener(listener);
        }

        @Override
        public Collection<? extends ProjectProblem> getProblems() {
            return problemsProviderSupport.getProblems(new ProjectProblemsProviderSupport.ProblemsCollector() {
                @Override
                public Collection<? extends ProjectProblemsProvider.ProjectProblem> collectProblems() {
                    Collection<? extends ProjectProblemsProvider.ProjectProblem> currentProblems = ProjectManager.mutex().readAccess(
                            new Mutex.Action<Collection<? extends ProjectProblem>>() {
                                @Override
                                public Collection<? extends ProjectProblem> run() {
                                    final Set<ProjectProblem> newProblems = new LinkedHashSet<ProjectProblem>();
                                    final Set<File> allFiles = new HashSet<>();
                                    newProblems.addAll(getReferenceProblems(helper,eval,refHelper,refProps,allFiles,false));
                                    newProblems.addAll(getPlatformProblems(eval,platformProps,false));
                                    updateFileListeners(allFiles);
                                    return Collections.unmodifiableSet(newProblems);
                                }
                            });
                    return currentProblems;
                }
            });
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (LibraryManager.PROP_OPEN_LIBRARY_MANAGERS.equals(evt.getPropertyName())) {
                addLibraryManagerListener();
            }
            problemsProviderSupport.fireProblemsChange();
        }

        @Override
        public void fileDataCreated(FileEvent fe) {
            problemsProviderSupport.fireProblemsChange();
        }

        @Override
        public void fileFolderCreated(FileEvent fe) {
            problemsProviderSupport.fireProblemsChange();
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            problemsProviderSupport.fireProblemsChange();
        }

        @Override
        public void fileRenamed(FileRenameEvent fe) {
            problemsProviderSupport.fireProblemsChange();
        }

        @Override
        public void fileChanged(FileEvent fe) {
        }

        @Override
        public void fileAttributeChanged(FileAttributeEvent fe) {
        }
        
        void attachListeners() {
            if (listenersInitialized.compareAndSet(false, true)) {
                eval.addPropertyChangeListener(this);
                JavaPlatformManager.getDefault().addPropertyChangeListener(WeakListeners.propertyChange(this, JavaPlatformManager.getDefault()));
                LibraryManager.addOpenManagersPropertyChangeListener(new OpenManagersWeakListener(this));
                addLibraryManagerListener();
            } else {
                throw new IllegalStateException();
            }
        }

        private synchronized void updateFileListeners(@NonNull final Collection<? extends File> newFiles) {
            final Collection<File> toAdd = new HashSet<>(newFiles);
            toAdd.removeAll(currentFiles);
            final Collection<File> toRemove = new HashSet<>(currentFiles);
            toRemove.removeAll(newFiles);
            for (File f : toRemove) {
                FileUtil.removeFileChangeListener(this, f);
            }
            for (File f : toAdd) {
                FileUtil.addFileChangeListener(this, f);
            }
            currentFiles.addAll(toAdd);
            currentFiles.removeAll(toRemove);
        }

        private void addLibraryManagerListener() {
            final Map<URL,Object[]> oldLMs;
            final boolean attachToDefault;
            synchronized (this) {
                attachToDefault = activeLibManLocs == null;
                if (attachToDefault) {
                    activeLibManLocs = new HashMap<URL,Object[]>();
                }
                oldLMs = new HashMap<URL,Object[]>(activeLibManLocs);
            }
            if (attachToDefault) {
                final LibraryManager manager = LibraryManager.getDefault();
                manager.addPropertyChangeListener(WeakListeners.propertyChange(this, manager));
            }
            final Collection<? extends LibraryManager> managers = LibraryManager.getOpenManagers();
            final Map<URL,LibraryManager> managerByLocation = new HashMap<URL, LibraryManager>();
            for (LibraryManager manager : managers) {
                final URL url = manager.getLocation();
                if (url != null) {
                    managerByLocation.put(url, manager);
                }
            }
            final HashMap<URL,Object[]> toRemove = new HashMap<URL,Object[]>(oldLMs);
            toRemove.keySet().removeAll(managerByLocation.keySet());
            for (Object[] pair : toRemove.values()) {
                ((LibraryManager)pair[0]).removePropertyChangeListener((PropertyChangeListener)pair[1]);
            }
            managerByLocation.keySet().removeAll(oldLMs.keySet());
            final HashMap<URL,Object[]> toAdd = new HashMap<URL,Object[]>();
            for (Map.Entry<URL,LibraryManager> e : managerByLocation.entrySet()) {
                final LibraryManager manager = e.getValue();
                final PropertyChangeListener listener = WeakListeners.propertyChange(this, manager);
                manager.addPropertyChangeListener(listener);
                toAdd.put(e.getKey(), new Object[] {manager, listener});
            }
            synchronized (this) {
                activeLibManLocs.keySet().removeAll(toRemove.keySet());
                activeLibManLocs.putAll(toAdd);
            }
        }

    }

    private static final class PlatformVersionProblemProviderImpl implements ProjectProblemsProvider, PropertyChangeListener {

        private final ProjectProblemsProviderSupport problemsProviderSupport = new ProjectProblemsProviderSupport(this);
        private final AtomicBoolean listenersInitialized = new AtomicBoolean();

        private final AntProjectHelper helper;
        private final PropertyEvaluator eval;
        private final BrokenReferencesSupport.PlatformUpdatedCallBack hook;
        private final String platformType;
        private final String platformProp;
        private final Set<String> versionProps;

        PlatformVersionProblemProviderImpl(
                @NonNull final AntProjectHelper helper,
                @NonNull final PropertyEvaluator eval,
                @NullAllowed final BrokenReferencesSupport.PlatformUpdatedCallBack hook,
                @NonNull final String platformType,
                @NonNull final String platformProp,
                @NonNull final String... versionProps) {
            assert helper != null;
            assert eval != null;
            assert platformType != null;
            assert platformProp != null;
            assert versionProps != null;
            this.helper = helper;
            this.eval = eval;
            this.hook = hook;
            this.platformType = platformType;
            this.platformProp = platformProp;
            this.versionProps = new HashSet<String>(Arrays.asList(versionProps));
        }

        @Override
        public void addPropertyChangeListener(@NonNull final PropertyChangeListener listener) {
            Parameters.notNull("listener", listener);   //NOI18N
            problemsProviderSupport.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(@NonNull final PropertyChangeListener listener) {
            Parameters.notNull("listener", listener);   //NOI18N
            problemsProviderSupport.removePropertyChangeListener(listener);
        }

        @Override
        @NbBundle.Messages({
            "LBL_Invalid_JDK_Version=Invalid Java Platform Version",
            "HINT_Invalid_JDK_Vernsion=The active project platform is an older version than it's required by project source/binary format."
        })
        public Collection<? extends ProjectProblem> getProblems() {
            return problemsProviderSupport.getProblems(new ProjectProblemsProviderSupport.ProblemsCollector() {
                @Override
                public Collection<? extends ProjectProblemsProvider.ProjectProblem> collectProblems() {
                    Collection<? extends ProjectProblemsProvider.ProjectProblem> currentProblems = ProjectManager.mutex().readAccess(
                            new Mutex.Action<Collection<? extends ProjectProblem>>() {
                                @Override
                                public Collection<? extends ProjectProblem> run() {
                                    final JavaPlatform activePlatform = getActivePlatform();
                                    final SpecificationVersion platformVersion = activePlatform == null ?
                                        null:
                                        activePlatform.getSpecification().getVersion();
                                    final Collection<String> invalidVersionProps = new ArrayList<String>(versionProps.size());
                                    SpecificationVersion minVersion = getInvalidJdkVersion(
                                            platformVersion,
                                            invalidVersionProps);
                                    return minVersion != null ?
                                        Collections.singleton(ProjectProblem.createError(
                                            LBL_Invalid_JDK_Version(),
                                            HINT_Invalid_JDK_Vernsion(),
                                            new SourceTargetResolver(
                                                helper,
                                                hook,
                                                platformType,
                                                platformProp,
                                                invalidVersionProps,
                                                minVersion,
                                                platformVersion))) :
                                        Collections.<ProjectProblem>emptySet();
                                }
                        });
                    return currentProblems;
                }
            });
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final String propName = evt.getPropertyName();
            if (propName == null || platformProp.equals(propName) || versionProps.contains(propName)) {
                problemsProviderSupport.fireProblemsChange();
            }
        }

        void attachListeners() {
            if (listenersInitialized.compareAndSet(false, true)) {
                eval.addPropertyChangeListener(this);
            } else {
                throw new IllegalStateException();
            }
        }

        /**
         * Gets minimal required JDK version or null if the project platform
         * satisfy the required JDK versions.
         * @return The minimal {@link SpecificationVersion} of platform or null.
         */
        @CheckForNull
        private SpecificationVersion getInvalidJdkVersion (
                @NullAllowed final SpecificationVersion platformVersion,
                @NonNull final Collection<? super String> invalidVersionProps) {
            SpecificationVersion minVersion = null;
            if (platformVersion != null) {
                for (String vp : versionProps) {
                    final String value = this.eval.getProperty(vp);
                    if (value == null || value.isEmpty()) {
                        continue;
                    }
                    try {
                        final SpecificationVersion vpVersion = new SpecificationVersion (value);
                        if (vpVersion.compareTo(platformVersion) > 0) {
                            invalidVersionProps.add(vp);
                            minVersion = max(minVersion,vpVersion);
                        }
                    } catch (NumberFormatException nfe) {
                        LOG.log(
                            Level.WARNING,
                            "Property: {0} holds non valid version: {1}",  //NOI18N
                            new Object[]{
                                vp,
                                value
                            });
                    }
                }
            }
            return minVersion;
        }

        @CheckForNull
        private SpecificationVersion max (
            @NullAllowed final SpecificationVersion a,
            @NullAllowed final SpecificationVersion b) {
            if (a == null) {
                return b;
            } else if (b == null) {
                return a;
            } else if (a.compareTo(b)>=0) {
                return a;
            } else {
                return b;
            }
        }

        @CheckForNull
        private JavaPlatform getActivePlatform() {
            final String activePlatformId = this.eval.getProperty(platformProp);
            final JavaPlatformManager pm = JavaPlatformManager.getDefault();
            if (activePlatformId == null) {
                return pm.getDefaultPlatform();
            }
            final JavaPlatform[] installedPlatforms = pm.getPlatforms(
                    null,
                    new Specification(platformType, null));
            for (JavaPlatform javaPlatform : installedPlatforms) {
                final String antName = javaPlatform.getProperties().get("platform.ant.name"); //NOI18N
                if (activePlatformId.equals(antName)) {
                    return javaPlatform;
                }
            }
            return null;
        }
    }

    private static class OpenManagersWeakListener extends WeakReference<PropertyChangeListener> implements Runnable, PropertyChangeListener {

        public OpenManagersWeakListener(final PropertyChangeListener listener) {
            super(listener, Utilities.activeReferenceQueue());
        }

        @Override
        public void run() {
            LibraryManager.removeOpenManagersPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final PropertyChangeListener listener = get();
            if (listener != null) {
                listener.propertyChange(evt);
            }
        }

    }
    //</editor-fold>
}
