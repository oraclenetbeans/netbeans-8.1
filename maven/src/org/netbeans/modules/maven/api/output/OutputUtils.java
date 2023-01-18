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

package org.netbeans.modules.maven.api.output;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.classpath.ProjectSourcesClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

/**
 *
 * @author mkleint
 */
public final class OutputUtils {
    public static final Pattern linePattern = Pattern.compile("(?:\\[catch\\])?\\sat (.*)\\((?:Native Method|(.*)\\.java\\:(\\d+))\\)"); //NOI18N
 
    private static final Map<Project, StacktraceOutputListener> projectStacktraceListeners = new WeakHashMap<>();
    private static final Map<FileObject, StacktraceOutputListener> fileStacktraceListeners = new WeakHashMap<>();
    
    /** Creates a new instance of OutputUtils */
    private OutputUtils() {
    }
    
    /**
     * 
     * @param line
     * @param classPath
     * @return 
     * @deprecated use {@link #matchStackTraceLine(java.lang.String, org.openide.filesystems.FileObject)}  
     *              or {@link #matchStackTraceLine(java.lang.String, org.netbeans.api.project.Project)} instead.
     */
    public static OutputListener matchStackTraceLine(String line, ClassPath classPath) {
        StacktraceAttributes sa = matchStackTraceLine(line);
        return sa != null ? new ClassPathStacktraceOutputListener(classPath, sa) : null;
    }
    
    /**
     * 
     * @param line
     * @param project
     * @return 
     */
    public static OutputListener matchStackTraceLine(String line, Project project) {
        StacktraceAttributes sa = matchStackTraceLine(line);
        if(sa != null) {
            synchronized(projectStacktraceListeners) {
                StacktraceOutputListener list = projectStacktraceListeners.get(project);
                if(list == null) {
                    list = new ProjectStacktraceOutputListener(project);
                    projectStacktraceListeners.put(project, list);
                }
                return list;
            }
        }
        return null;
    }
    
    /**
     * 
     * @param line
     * @param fileObject
     * @return 
     */
    public static OutputListener matchStackTraceLine(String line, FileObject fileObject) {
        StacktraceAttributes sa = matchStackTraceLine(line);
        if(sa != null) {
            synchronized(fileStacktraceListeners) {
                StacktraceOutputListener list = fileStacktraceListeners.get(fileObject);
                if(list == null) {
                    list = new FileObjectStacktraceOutputListener(fileObject);
                    fileStacktraceListeners.put(fileObject, list);
                }
                return list;
            }
        }
        return null;
    }
    
    /**
     * 
     * @param line
     * @param classPath
     * @return 
     */
    private static StacktraceAttributes matchStackTraceLine(String line) {
        Matcher match = linePattern.matcher(line);
        if (match.matches() && match.groupCount() == 3) {
            String method = match.group(1);
            String file = match.group(2);
            String lineNum = match.group(3);
            int index = file == null || file.isEmpty() ? -1 : method.indexOf(file);
            if (index > -1) {
                return new StacktraceAttributes(method, file, lineNum);
            }
        }
        return null;
    }
    
    private static class StacktraceAttributes {
        private final String method;
        private final String file;
        private final String lineNum;
        public StacktraceAttributes(String method, String file, String lineNum) {
            this.method = method;
            this.file = file;
            this.lineNum = lineNum;
        }
    }
    
    private static abstract class StacktraceOutputListener implements OutputListener {
        
        protected abstract ClassPath getClassPath();
        
        protected  StacktraceAttributes getStacktraceAttributes(String line) {
            return matchStackTraceLine(line);
        }

        @Override
        public void outputLineSelected(OutputEvent ev) {
    //            cookie.getLineSet().getCurrent(line).show(Line.SHOW_SHOW);
        }

        /** Called when some sort of action is performed on a line.
         * @param ev the event describing the line
         */
        @Override
        @NbBundle.Messages({
            "# {0} - class name",
            "NotFound=Class \"{0}\" not found on classpath", 
            "# {0} - file name",
            "NoSource=Source file not found for \"{0}\""
        })
        public void outputLineAction(OutputEvent ev) {
            StacktraceAttributes sa = matchStackTraceLine(ev.getLine());
            ClassPath classPath = getClassPath();

            int index = sa.method.indexOf(sa.file);
            String packageName = sa.method.substring(0, index).replace('.', '/'); //NOI18N
            String resourceName = packageName + sa.file + ".class"; //NOI18N
            FileObject resource = classPath.findResource(resourceName);
            if (resource != null) {
                FileObject root = classPath.findOwnerRoot(resource);
                if (root != null) {
                    URL url = URLMapper.findURL(root, URLMapper.INTERNAL);
                    SourceForBinaryQuery.Result res = SourceForBinaryQuery.findSourceRoots(url);
                    FileObject[] rootz = res.getRoots();
                    for (int i = 0; i < rootz.length; i++) {
                        String path = packageName + sa.file + ".java"; //NOI18N
                        FileObject javaFo = rootz[i].getFileObject(path);
                        if (javaFo != null) {
                            try {
                                DataObject obj = DataObject.find(javaFo);
                                EditorCookie cookie = obj.getLookup().lookup(EditorCookie.class);
                                int lineInt = Integer.parseInt(sa.lineNum);
                                try {
                                    cookie.getLineSet().getCurrent(lineInt - 1).show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
                                } catch (IndexOutOfBoundsException x) { // #155880
                                    cookie.open();
                                }
                                return;
                            } catch (DataObjectNotFoundException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    }
                    StatusDisplayer.getDefault().setStatusText(Bundle.NoSource(sa.file));
                }
            } else {
                StatusDisplayer.getDefault().setStatusText(Bundle.NotFound(sa.file));
            }
        }

        /** Called when a line is cleared from the buffer of known lines.
         * @param ev the event describing the line
         */
        @Override
        public void outputLineCleared(OutputEvent ev) {
        }
    }
    
    private static class ProjectStacktraceOutputListener extends StacktraceOutputListener {
        private final WeakReference<Project> ref;

        public ProjectStacktraceOutputListener(Project project) {
            this.ref = new WeakReference<>(project);
        }

        @Override
        protected ClassPath getClassPath() {
            Project prj = ref.get();
            if(prj != null) {
                ClassPath[] cp = prj.getLookup().lookup(ProjectSourcesClassPathProvider.class).getProjectClassPaths(ClassPath.EXECUTE);
                return ClassPathSupport.createProxyClassPath(cp);
            }
            return null;
        }        
    }
    
    private static class FileObjectStacktraceOutputListener extends StacktraceOutputListener {
        private final WeakReference<FileObject> ref;

        public FileObjectStacktraceOutputListener(FileObject file) {
            this.ref = new WeakReference<>(file);
        }

        @Override
        protected ClassPath getClassPath() {
            FileObject fileObject = ref.get();
            if(fileObject != null) {
                return ClassPath.getClassPath(fileObject, ClassPath.EXECUTE);
            }
            return null;
        }        
    }
    
    /**
     * Legacy
     */
    private static class ClassPathStacktraceOutputListener extends StacktraceOutputListener {
        private final ClassPath classPath;
        private final StacktraceAttributes sa;

        public ClassPathStacktraceOutputListener(ClassPath classPath, StacktraceAttributes sa) {
            this.classPath = classPath;
            this.sa = sa;
        }

        @Override
        protected ClassPath getClassPath() {
            return classPath;
        }        

        @Override
        protected StacktraceAttributes getStacktraceAttributes(String line) {
            return sa;
        }
    }
    
}
