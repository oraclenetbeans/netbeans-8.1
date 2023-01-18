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

package org.netbeans.modules.java.project.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tools.ant.module.spi.AntEvent;
import org.apache.tools.ant.module.spi.AntLogger;
import org.apache.tools.ant.module.spi.AntSession;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.IOColorPrint;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

/**
 * Ant logger which handles Java- and Java-project-specific UI.
 * Specifically, handles stack traces hyperlinking and suppresses
 * hyperlinking of nbproject/build-impl.xml files.
 * @author Jesse Glick
 * @see "#42525"
 */
@ServiceProvider(service=AntLogger.class, position=50)
public final class JavaAntLogger extends AntLogger {
    
    public static final int LOGGER_MAX_LINE_LENGTH = Integer.getInteger("logger.max.line.length", 3000); //NOI18N
    
    static final class StackTraceParse {
        final String line;
        final String resource;
        final int lineNumber;
        final String prePart, midPart, endPart;
        StackTraceParse(String line, String resource, int lineNumber, String prePart, String midPart, String endPart) {
            this.line = line;
            this.resource = resource;
            this.lineNumber = lineNumber;
            this.prePart = prePart;
            this.midPart = midPart;
            this.endPart = endPart;
        }
        public @Override String toString() {
            return resource + ":" + lineNumber;
        }
        void hyperlink(AntSession session, AntEvent event, FileObject source,
                int messageLevel, int sessionLevel, SessionData data) {
            if (messageLevel <= sessionLevel && !event.isConsumed()) {
                OutputListener hyperlink;
                try {
                    hyperlink = session.createStandardHyperlink(source.getURL(), guessExceptionMessage(data), lineNumber, -1, -1, -1);
                } catch (FileStateInvalidException e) {
                    assert false : e;
                    return;
                }
                event.consume();
                InputOutput io = session.getIO();
                if (IOColorPrint.isSupported(io)) {
                    try {
                        OutputWriter out = messageLevel <= AntEvent.LOG_WARN ? io.getErr() : /* #174781 1/2 */io.getOut();
                        boolean important = prePart.contains(/* #174781 2/2 */"at ") && /* e.g. InstalledFileLocatorImpl.findCaller */!prePart.contains("WARNING"); // NOI18N
                        out.print(prePart);
                        IOColorPrint.print(io, midPart, hyperlink, important, null);
                        out.println(endPart);
                        return;
                    } catch (IOException x) {
                        Exceptions.printStackTrace(x);
                    }
                }
                session.println(line, true, hyperlink);
            }
        }
    }
    /** Java identifier */
    private static final String JIDENT = "[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*"; // NOI18N
    // should be consistent with o.apache.tools.ant.module.STACK_TRACE
    // would be nice to match org.netbeans.modules.hudson.impl.JavaHudsonLogger.STACK_TRACE, but would need to copy more
    /**
     * <ol>
     * <li>non-hyperlinkable initial portion
     * <li>package
     * <li>class
     * <li>hyperlinkable portion
     * <li>filename
     * <li>line number
     * <li>non-hyperlinkable final portion
     * </ol>
     */
    private static final Pattern STACK_TRACE = Pattern.compile(
            "(.*?((?:" + JIDENT + "[.])*)(" + JIDENT + ")[.](?:" + JIDENT + "|<init>|<clinit>)" + // NOI18N
            "[(])((" + JIDENT + "[.]java):([0-9]+)|Unknown Source)([)].*)"); // NOI18N
    static StackTraceParse/*|null*/ parseStackTraceLine(String line) {
        if (line.length() >= LOGGER_MAX_LINE_LENGTH) { // too long message, probably coming from user, so do not check for stacktrace
            return null;
        }
        Matcher m = STACK_TRACE.matcher(line);
        if (m.matches()) {
            // We have a stack trace.
            String pkg = m.group(2);
            String filename = m.group(5);
            int lineNumber;
            if (filename == null) {
                filename = m.group(3).replaceFirst("[$].+", "") + ".java"; // NOI18N
                lineNumber = 1;
            } else {
                lineNumber = Integer.parseInt(m.group(6));
            }
            return new StackTraceParse(line, pkg.replace('.', '/') + filename, lineNumber, m.group(1), m.group(4), m.group(7));
        } else {
            return null;
        }
    }

    /**
     * Regexp matching the first line of a stack trace, with the exception message.
     * Captured groups:
     * <ol>
     * <li>unqualified name of exception class plus possible message
     * </ol>
     */
    private static final Pattern EXCEPTION_MESSAGE = Pattern.compile(
    // #42894: JRockit uses "Main Thread" not "main"
    "(?:Exception in thread \"(?:main|Main Thread)\" )?(?:(?:" + JIDENT + "\\.)+)(" + JIDENT + "(?:: .+)?)"); // NOI18N
    
    /**
     * Regexp matching part of a Java task's invocation debug message
     * that specifies the classpath.
     * Hack to find the classpath an Ant task is using.
     * Cf. Commandline.describeArguments, issue #28190.
     * Captured groups:
     * <ol>
     * <li>the classpath
     * </ol>
     */
    private static final Pattern CLASSPATH_ARGS = Pattern.compile("\r?\n'-classpath'\r?\n'(.*)'\r?\n"); // NOI18N
    
    /**
     * Regexp matching part of a Java task's invocation debug message
     * that specifies java executable.
     * Hack to find JDK used for execution.
     */
    private static final Pattern JAVA_EXECUTABLE = Pattern.compile("^Executing '(.*)' with arguments:$", Pattern.MULTILINE); // NOI18N
    
    /**
     * Ant task names we will pay attention to.
     */
    private static final String[] TASKS_OF_INTEREST = {
        // XXX should this really be restricted? what about stack traces printed during shutdown?
        "java", // NOI18N
        // #44328: unit tests run a different task:
        "junit", // NOI18N
        "testng", // NOI18N
        // Nice to handle stack traces from e.g. NB's own build system too!
        "exec", // NOI18N
        // #63065: Mobility execution
        "nb-run",     //NOI18N
    };
    
    private static final int[] LEVELS_OF_INTEREST = {
        AntEvent.LOG_VERBOSE, // for CLASSPATH_ARGS
        AntEvent.LOG_INFO, // for some stack traces
        AntEvent.LOG_WARN, // for most stack traces
        AntEvent.LOG_ERR, // for some stack traces, incl. those redelivered from StandardLogger
    };
    
    /**
     * Data stored in the session.
     */
    private static final class SessionData {
        public ClassPath platformSources = null;
        public String classpath = null;
        public volatile Collection<FileObject> classpathSourceRoots = null;
        public volatile String possibleExceptionText = null;
        public volatile String lastExceptionMessage = null;
        public SessionData() {}
        public void setClasspath(String cp) {
            classpath = cp;
            classpathSourceRoots = null;
        }
        public void setPlatformSources(ClassPath platformSources) {
            this.platformSources = platformSources;
            classpathSourceRoots = null;
        }
    }
    
    /** Default constructor for lookup. */
    public JavaAntLogger() {}
    
    @Override
    public boolean interestedInSession(AntSession session) {
        return true;
    }
    
    @Override
    public boolean interestedInAllScripts(AntSession session) {
        return true;
    }
    
    @Override
    public String[] interestedInTargets(AntSession session) {
        return AntLogger.ALL_TARGETS;
    }
    
    @Override
    public String[] interestedInTasks(AntSession session) {
        return TASKS_OF_INTEREST;
    }
    
    @Override
    public int[] interestedInLogLevels(AntSession session) {
        // XXX could exclude those in [INFO..ERR] greater than session.verbosity
        return LEVELS_OF_INTEREST;
    }
    
    private SessionData getSessionData(AntSession session) {
        SessionData data = (SessionData) session.getCustomData(this);
        if (data == null) {
            data = new SessionData();
            session.putCustomData(this, data);
        }
        return data;
    }

    @Override
    public void messageLogged(AntEvent event) {
        AntSession session = event.getSession();
        int messageLevel = event.getLogLevel();
        int sessionLevel = session.getVerbosity();
        SessionData data = getSessionData(session);
        String line = event.getMessage();
        assert line != null;

        StackTraceParse parse = parseStackTraceLine(line);
        if (parse != null) {
            // Check to see if the class is listed in our per-task sourcepath.
            // XXX could also look for -Xbootclasspath etc., but probably less important
            for (FileObject root : getCurrentSourceRootsForClasspath(data)) {
                FileObject source = root.getFileObject(parse.resource);
                if (source != null) {
                    // Got it!
                    parse.hyperlink(session, event, source, messageLevel, sessionLevel, data);
                    break;
                }
            }
            // Also check global sourcepath (sources of open projects, and sources
            // corresponding to compile or boot classpaths of open projects).
            // Fallback in case a JAR file is copied to an unknown location, etc.
            // In this case we can't be sure that this source file really matches
            // the .class used in the stack trace, but it is a good guess.
            if (!event.isConsumed()) {
                FileObject source = GlobalPathRegistry.getDefault().findResource(parse.resource);
                if (source != null) {
                    parse.hyperlink(session, event, source, messageLevel, sessionLevel, data);
                } else if (messageLevel <= sessionLevel && "java".equals(event.getTaskName())) {
                    event.consume();
                    session.println(line, event.getLogLevel() <= AntEvent.LOG_WARN, null);
                }
            }
        } else {
            // Track the last line which was not a stack trace - probably the exception message.            
            data.lastExceptionMessage = null;
            data.possibleExceptionText = line;
        }
        
        // Look for classpaths.
        if (messageLevel == AntEvent.LOG_VERBOSE) {
            if (line.length() >= LOGGER_MAX_LINE_LENGTH) { // too long message, probably coming from user, so do not care about it
                return;
            }
            Matcher m2 = CLASSPATH_ARGS.matcher(line);
            if (m2.find()) {
                String cp = m2.group(1);
                data.setClasspath(cp);
            }
            // XXX should also probably clear classpath when taskFinished called
            m2 = JAVA_EXECUTABLE.matcher(line);
            if (m2.find()) {
                String executable = m2.group(1);
                ClassPath platformSources = findPlatformSources(executable);
                if (platformSources != null) {
                    data.setPlatformSources(platformSources);
                }
            }
        }
    }
    
    private ClassPath findPlatformSources(String javaExecutable) {
        for (JavaPlatform p : JavaPlatformManager.getDefault().getInstalledPlatforms()) {
            FileObject fo = p.findTool("java"); // NOI18N
            if (fo != null) {
                File f = FileUtil.toFile(fo);
                if (f.getAbsolutePath().startsWith(javaExecutable)) {
                    return p.getSourceFolders();
                }
            }
        }
        return null;
    }
    
    /**
     * Finds source roots corresponding to the apparently active classpath
     * (as reported by logging from Ant when it runs the Java launcher with -cp).
     */
    private static Collection<FileObject> getCurrentSourceRootsForClasspath(SessionData data) {
        if (data.classpath == null) {
            return Collections.emptySet();
        }
        Collection<FileObject> result;
        synchronized (data) {
            result = data.classpathSourceRoots;
        }
        if (result == null) {
            result = new LinkedHashSet<FileObject>();
            StringTokenizer tok = new StringTokenizer(data.classpath, File.pathSeparator);
            while (tok.hasMoreTokens()) {
                String binrootS = tok.nextToken();
                File f = FileUtil.normalizeFile(new File(binrootS));
                URL binroot = FileUtil.urlForArchiveOrDir(f);
                if (binroot == null) {
                    continue;
                }
                FileObject[] someRoots = SourceForBinaryQuery.findSourceRoots(binroot).getRoots();
                result.addAll(Arrays.asList(someRoots));
            }
            if (data.platformSources != null) {
                result.addAll(Arrays.asList(data.platformSources.getRoots()));
            } else {
                // no platform found. use default one:
                JavaPlatform plat = JavaPlatform.getDefault();
                // in unit tests the default platform may be null:
                if (plat != null) {
                    result.addAll(Arrays.asList(plat.getSourceFolders().getRoots()));
                }
            }
            result = Collections.unmodifiableCollection(result);
            synchronized (data) {
                data.classpathSourceRoots = result;
            }
        }
        return result;
    }
    
    private static String guessExceptionMessage(SessionData data) {
        final String pet = data.possibleExceptionText;
        String lem = data.lastExceptionMessage;
        if (pet != null && pet.length() < LOGGER_MAX_LINE_LENGTH) { // not too long message, so check for exception
            if (lem == null) {
                Matcher m = EXCEPTION_MESSAGE.matcher(pet);
                if (m.matches()) {
                    data.lastExceptionMessage = lem = m.group(1);
                } else {
                    data.possibleExceptionText = null;
                }
            }
            return lem;
        }
        return null;
    }
}
