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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.source.parsing;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.api.java.source.JavaParserResultTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Parameters;
import org.openide.util.WeakListeners;

/**
 * Class file parser.
 * Threading: Not thread safe, concurrency handled by caller (TaskProcessor).
 * @author Tomas Zezula
 */
//@NotThreadSafe
public class ClassParser extends Parser {
    
    public static final String MIME_TYPE = "application/x-class-file";  //NOI18N
    private static final ClassPath EMPTY_PATH = ClassPathSupport.createClassPath(new URL[0]);
    private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());
    
    private final ChangeSupport changeSupport;
    private final ClasspathInfoListener cpInfoListener;
    private CompilationInfoImpl ciImpl;
    private Snapshot lastSnapshot;
    private ClasspathInfo info;
    private ChangeListener wl;

    ClassParser() {
        this.changeSupport = new ChangeSupport(this);
        this.cpInfoListener = new ClasspathInfoListener(this.changeSupport, null);
    }

    @Override
    public void parse(final Snapshot snapshot, Task task, final SourceModificationEvent event) throws ParseException {
        assert snapshot != null;
        lastSnapshot = snapshot;
        final Source source = snapshot.getSource();
        assert source != null;
        final FileObject file = source.getFileObject();
        assert file != null;
        if (info == null) {
            if ((task instanceof ClasspathInfoProvider)) {
                info =((ClasspathInfoProvider)task).getClasspathInfo();
            }
            if (info == null) {
                ClassPath bootPath = ClassPath.getClassPath(file, ClassPath.BOOT);
                if (bootPath == null) {
                    //javac requires at least java.lang
                    bootPath = JavaPlatformManager.getDefault().getDefaultPlatform().getBootstrapLibraries();
                }
                ClassPath compilePath = ClassPath.getClassPath(file, ClassPath.COMPILE);
                if (compilePath == null) {
                    compilePath = EMPTY_PATH;
                }
                ClassPath executePath = ClassPath.getClassPath(file, ClassPath.EXECUTE);
                if (executePath == null) {
                    executePath = EMPTY_PATH;
                }
                ClassPath srcPath = ClassPath.getClassPath(file, ClassPath.SOURCE);
                if (srcPath == null) {
                    srcPath = EMPTY_PATH;
                }
                info = ClasspathInfo.create(
                    bootPath,
                    ClassPathSupport.createProxyClassPath(compilePath,executePath),
                    srcPath);
            }
            assert info != null;
            info.addChangeListener(wl=WeakListeners.change(this.cpInfoListener, info));
        }
        final ClassPath bootPath = info.getClassPath(ClasspathInfo.PathKind.BOOT);
        final ClassPath compilePath = info.getClassPath(ClasspathInfo.PathKind.COMPILE);
        final ClassPath srcPath = info.getClassPath(ClasspathInfo.PathKind.SOURCE);
        final FileObject root = ClassPathSupport.createProxyClassPath(
                ClassPathSupport.createClassPath(CachingArchiveProvider.getDefault().ctSymRootsFor(bootPath)),
                bootPath,
                compilePath,
                srcPath).findOwnerRoot(file);
        if (root == null) {
            throw new ParseException(
                String.format("The file %s is not owned by provided classpaths, boot: %s, compile: %s, src: %s",    //NOI18N
                    FileUtil.getFileDisplayName(file),
                    bootPath.toString(),
                    compilePath.toString(),
                    srcPath.toString()));
        }
        try {
            this.ciImpl = new CompilationInfoImpl(info,file,root);
        } catch (final IOException ioe) {
            throw new ParseException ("ClassParser failure", ioe);            //NOI18N
        }
    }

    @Override
    public Result getResult(Task task) throws ParseException {
        assert ciImpl != null;
        final boolean isParserResultTask = task instanceof ParserResultTask;
        final boolean isJavaParserResultTask = task instanceof JavaParserResultTask;
        final boolean isUserTask = task instanceof UserTask;
        JavacParserResult result = null;
        if (isParserResultTask) {
            final JavaSource.Phase currentPhase = ciImpl.getPhase();
            JavaSource.Phase requiredPhase;
            if (isJavaParserResultTask) {
                requiredPhase = ((JavaParserResultTask)task).getPhase();
            } else {
                requiredPhase = JavaSource.Phase.RESOLVED;
            }
            if (task instanceof ClasspathInfoProvider) {
                final ClasspathInfo taskProvidedCpInfo = ((ClasspathInfoProvider)task).getClasspathInfo();
                if (taskProvidedCpInfo != null && !taskProvidedCpInfo.equals(info)) {
                    assert info != null;
                    assert wl != null;
                    info.removeChangeListener(wl);
                    info = null;
                    parse(lastSnapshot, task, null);
                }
            }
            if (currentPhase.compareTo(requiredPhase)<0) {
                ciImpl.setPhase(requiredPhase);
            }
            result = new JavacParserResult(JavaSourceAccessor.getINSTANCE().createCompilationInfo(ciImpl));
        }
        else if (isUserTask) {
            result = new JavacParserResult(JavaSourceAccessor.getINSTANCE().createCompilationController(ciImpl));
        }
        else {
            LOGGER.warning("Ignoring unknown task: " + task);                   //NOI18N
        }
        return result;
    }

    @Override
    public void cancel() {
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
        Parameters.notNull("changeListener", changeListener);   //NOI18N
        this.changeSupport.addChangeListener(changeListener);
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
        Parameters.notNull("changeListener", changeListener);   //NOI18N
        this.changeSupport.removeChangeListener(changeListener);
    }

}
