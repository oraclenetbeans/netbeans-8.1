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

package org.netbeans.api.java.queries;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.modules.SpecificationVersion;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;
import org.openide.util.Union2;
import org.openide.util.WeakListeners;

/**
 * Returns source level of the given Java source file if it is known.
 * @see org.netbeans.spi.java.queries.SourceLevelQueryImplementation2
 * @author David Konecny
 * @author Tomas Zezula
 * @since org.netbeans.api.java/1 1.5
 */
public class SourceLevelQuery {

    private static final Logger LOGGER = Logger.getLogger(SourceLevelQuery.class.getName());

    private static final Pattern SOURCE_LEVEL = Pattern.compile("\\d+\\.\\d+"); //NOI18N
    private static final Pattern SYNONYM = Pattern.compile("\\d+");             //noI18N
    private static final SpecificationVersion JDK8 = new SpecificationVersion("1.8");   //NOI18N

    @SuppressWarnings("deprecation")
    private static final Lookup.Result<? extends org.netbeans.spi.java.queries.SourceLevelQueryImplementation> implementations =
        Lookup.getDefault().lookupResult (org.netbeans.spi.java.queries.SourceLevelQueryImplementation.class);

    private static final Lookup.Result<? extends SourceLevelQueryImplementation2> implementations2 =
        Lookup.getDefault().lookupResult (SourceLevelQueryImplementation2.class);

    private SourceLevelQuery() {
    }

    /**
     * Returns source level of the given Java file, Java package or source folder. For acceptable return values
     * see the documentation of <code>-source</code> command line switch of 
     * <code>javac</code> compiler .
     * @param javaFile Java source file, Java package or source folder in question
     * @return source level of the Java file, e.g. "1.3", "1.4" or "1.5", or null
     *     if it is not known. Even it is allowed for a SPI implementation to return
     *     a source level synonym e.g. "5" for "1.5" the returned value is always normalized.
     */
    @SuppressWarnings("deprecation")
    public static String getSourceLevel(FileObject javaFile) {
        for (SourceLevelQueryImplementation2 sqi : implementations2.allInstances()) {
            final SourceLevelQueryImplementation2.Result result = sqi.getSourceLevel(javaFile);
            if (result != null) {
                final String s = normalize(result.getSourceLevel());
                if (s != null) {
                    if (!SOURCE_LEVEL.matcher(s).matches()) {
                        LOGGER.log(Level.WARNING, "#83994: Ignoring bogus source level {0} for {1} from {2}", new Object[] {s, javaFile, sqi}); //NOI18N
                        continue;
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Found source level {0} for {1} from {2}", new Object[] {s, javaFile, sqi});     //NOI18N
                    }
                    return s;
                }
            }
        }
        for  (org.netbeans.spi.java.queries.SourceLevelQueryImplementation sqi : implementations.allInstances()) {
            final String s = normalize(sqi.getSourceLevel(javaFile));
            if (s != null) {
                if (!SOURCE_LEVEL.matcher(s).matches()) {
                    LOGGER.log(Level.WARNING, "#83994: Ignoring bogus source level {0} for {1} from {2}", new Object[] {s, javaFile, sqi});
                    continue;
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Found source level {0} for {1} from {2}", new Object[] {s, javaFile, sqi});
                }
                return s;
            }
        }
        LOGGER.log(Level.FINE, "No source level found for {0}", javaFile);
        return null;
    }

    /**
     * Returns a source level of the given Java file, Java package or source folder. For acceptable return values
     * see the documentation of <code>-source</code> command line switch of
     * <code>javac</code> compiler .
     * @param javaFile Java source file, Java package or source folder in question
     * @return a {@link Result} object encapsulating the source level of the Java file. Results created for source
     * levels provided by the {@link SourceLevelQueryImplementation} do not support listening. Use {@link Result#supportsChanges()}
     * to check if the result supports listening.
     * @since 1.30
     */
    @SuppressWarnings("deprecation")
    public static @NonNull Result getSourceLevel2(final @NonNull FileObject javaFile) {
        for (SourceLevelQueryImplementation2 sqi : implementations2.allInstances()) {
            final SourceLevelQueryImplementation2.Result result = sqi.getSourceLevel(javaFile);
            if (result != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Found source level {0} for {1} from {2}", new Object[] {result, javaFile, sqi}); //NOI18N
                }
                return new Result(result);
            }
        }
        LOGGER.log(Level.FINE, "No source level found for {0}", javaFile);
        return new Result(javaFile);
    }
    
    /**
     * The JRE profiles defined by Java 8.
     * <div class="nonnormative">
     * The JDK 8 provides three limited profiles (compact1, compact2, compact3) in addition
     * to the full JDK. Each profile specifies a specific set of Java API packages and
     * contains all of the APIs of the smaller profile, @see http://openjdk.java.net/jeps/161
     * </div>
     * @since 1.47
     */
    public static enum Profile {

        /**
         * The compact1 profile.
         */
        COMPACT1("compact1", Bundle.NAME_Compact1(), JDK8),   //NOI18N

        /**
         * The compact2 profile.
         */
        COMPACT2("compact2", Bundle.NAME_Compact2(), JDK8),   //NOI18N

        /**
         * The compact3 profile.
         */
        COMPACT3("compact3", Bundle.NAME_Compact3(), JDK8),   //NOI18N

        /**
         * The default full JRE profile.
         */
        DEFAULT(Bundle.NAME_FullJRE()) {
            @Override
            public boolean isSupportedIn(@NonNull final String sourceLevel) {
                return true;
            }
        };

        private static final Map<String,Profile> profilesByName = new HashMap<>();
        static {
            for (Profile sp : values()) {
                profilesByName.put(sp.getName(), sp);
            }
        }

        private final String name;
        private final String displayName;
        private final SpecificationVersion supportedFrom;

        @NbBundle.Messages({
        "NAME_Compact1=Compact 1",
        "NAME_Compact2=Compact 2",
        "NAME_Compact3=Compact 3",
        "NAME_FullJRE=Full JRE"
        })
        private Profile(
                @NonNull final String name,
                @NonNull final String displayName,
                @NonNull final SpecificationVersion supportedFrom) {
            assert name != null;
            assert displayName != null;
            assert supportedFrom != null;
            this.name = name;
            this.displayName = displayName;
            this.supportedFrom = supportedFrom;
        }

        private Profile(@NonNull final String displayName) {
            assert displayName != null;
            this.name = "";   //NOI18N
            this.displayName = displayName;
            this.supportedFrom = null;
        }

        /**
         * Returns the name of the profile.
         * @return the name of the profile
         */
        @NonNull
        public String getName() {
            return name;
        }

        /**
         * Returns the display name of the profile.
         * @return the display name of the profile
         */
        @NonNull
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Tests if the profile is supported in gives source level.
         * @param sourceLevel to test if the profile is supported in it
         * @return true if the profile is supported in given source level
         */
        public boolean isSupportedIn(@NonNull String sourceLevel) {
            Parameters.notNull("sourceLevel", sourceLevel); //NOI18N
            sourceLevel = normalize(sourceLevel);
            if (!SOURCE_LEVEL.matcher(sourceLevel).matches()) {
                throw new IllegalArgumentException(sourceLevel);
            }
            return supportedFrom.compareTo(new SpecificationVersion(sourceLevel)) <= 0;
        }

        /**
         * Returns the {@link Profile} for given profile name.
         * @param profileName the name of the profile
         * @return the {@link Profile} for given profile name or null
         * for unknown profile name.
         */
        @CheckForNull
        public static Profile forName(@NullAllowed String profileName) {
            if (profileName == null) {
                profileName = Profile.DEFAULT.getName();
            }
            return Profile.profilesByName.get(profileName);
        }
    }    

    /**
     * Result of finding source level, encapsulating the answer as well as the
     * ability to listen to it.
     * @since 1.30
     */
    public static final class Result {

        private final @NonNull Union2<SourceLevelQueryImplementation2.Result,FileObject> delegate;
        private final ChangeSupport cs = new ChangeSupport(this);
        private /**@GuardedBy("this")*/ ChangeListener spiListener;

        private Result(@NonNull final SourceLevelQueryImplementation2.Result delegate) {
            Parameters.notNull("delegate", delegate);   //NOI18N
            this.delegate = Union2.<SourceLevelQueryImplementation2.Result,FileObject>createFirst(delegate);
        }
        
        private Result(@NonNull final FileObject javaFile) {
            Parameters.notNull("sourceLevel", javaFile);
            this.delegate = Union2.<SourceLevelQueryImplementation2.Result,FileObject>createSecond(javaFile);
        }
        
        /**
         * Get the source level.
         * @return a source level of the Java file, e.g. "1.3", "1.4", "1.5"
         * or null if the source level is unknown. Even it is allowed for a SPI implementation to return
         *     a source level synonym e.g. "5" for "1.5" the returned value is always normalized.
         */
        public @CheckForNull String getSourceLevel() {
            if (delegate.hasFirst()) {
                String sourceLevel = normalize(delegate.first().getSourceLevel());
                if (sourceLevel != null && !SOURCE_LEVEL.matcher(sourceLevel).matches()) {
                    LOGGER.log(
                        Level.WARNING,
                        "#83994: Ignoring bogus source level {0} from {2}",  //NOI18N
                        new Object[] {
                            sourceLevel,
                            delegate.first()
                        });
                    sourceLevel = null;
                }
                return sourceLevel;
            } else {
                return SourceLevelQuery.getSourceLevel(delegate.second());
            }
        }

        /**
         * Returns the required profile.
         * @return the required profile. If the profile is either unknown
         * or unsupported by actual source level it returns the {@link Profile#DEFAULT}.
         * <div class="nonnormative">
         * The JDK 8 provides three limited profiles (compact1, compact2, compact3) in addition
         * to the full JDK. Each profile specifies a specific set of Java API packages and
         * contains all of the APIs of the smaller profile, @see http://openjdk.java.net/jeps/161
         * </div>
         * @since 1.47
         */
        @NonNull
        public Profile getProfile() {
            final SourceLevelQueryImplementation2.Result delegate = getDelegate();
            if (!(delegate instanceof SourceLevelQueryImplementation2.Result2)) {
                return Profile.DEFAULT;
            }
            final Profile result = ((SourceLevelQueryImplementation2.Result2)delegate).getProfile();
            assert result != null : String.format(
                "Null result returned by provider: %s", //NOI18N
                delegate);
            return result;
        }

        /**
         * Add a listener to changes of source level.
         * If {@link #supportsChanges} is false, the listener will never be notified
         * although {@link #getSourceLevel} may change from call to call.
         * @param listener a listener to add
         */
        public void addChangeListener(@NonNull ChangeListener listener) {
            Parameters.notNull("listener", listener);   //NOI18N
            final SourceLevelQueryImplementation2.Result _delegate = getDelegate();
            if (_delegate == null) {
                return;
            }
            cs.addChangeListener(listener);
            synchronized (this) {
                if (spiListener == null) {
                    spiListener = new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                            cs.fireChange();
                        }
                    };
                    _delegate.addChangeListener(WeakListeners.change(spiListener, _delegate));
                }
            }
            
        }

        /**
         * Remove a listener to changes of source level.
         * @param listener a listener to add
         */
        public void removeChangeListener(@NonNull ChangeListener listener) {
            Parameters.notNull("listener", listener);   //NOI18N
            final SourceLevelQueryImplementation2.Result _delegate = getDelegate();
            if (_delegate == null) {
                return;
            }
            cs.removeChangeListener(listener);
        }

        /**
         * Returns true if the result support updates and client may
         * listen on it. If false client should always ask again to
         * obtain current value. The results created for values returned
         * by the {@link SourceLevelQueryImplementation} do not support
         * listening.
         * @return true if the result supports changes and listening
         */
        public boolean supportsChanges() {
            return getDelegate() != null;
        }

        private SourceLevelQueryImplementation2.Result getDelegate() {
            return delegate.hasFirst() ? delegate.first() : null;
        }
    }
    
    @CheckForNull
    private static String normalize(@NullAllowed String sourceLevel) {
        if (sourceLevel != null && SYNONYM.matcher(sourceLevel).matches()) {
            sourceLevel = MessageFormat.format("1.{0}", sourceLevel);   //NOI18N
        }
        return sourceLevel;
    }

}
