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

package org.netbeans.modules.profiler.ppoints;

import javax.swing.Icon;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.ui.components.table.EnhancedTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.modules.profiler.api.*;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.java.JavaProfilerSource;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourceMethodInfo;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "Utils_CannotOpenSourceMsg=Cannot show profiling point in source.\nCheck profiling point location.",
    "Utils_InvalidPPLocationMsg=<html><b>Invalid location of {0}.</b><br><br>Location of the profiling point does not seem to be valid.<br>Make sure it points inside method definition, otherwise<br>the profiling point will not be hit during profiling.</html>"
})
public class Utils {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class JavaEditorContext {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Document document;
        private FileObject fileObject;
        private JTextComponent textComponent;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public JavaEditorContext(JTextComponent textComponent, Document document, FileObject fileObject) {
            this.textComponent = textComponent;
            this.document = document;
            this.fileObject = fileObject;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Document getDocument() {
            return document;
        }

        public FileObject getFileObject() {
            return fileObject;
        }

        public JTextComponent getTextComponent() {
            return textComponent;
        }
    }

    private static class ProfilingPointPresenterListRenderer extends DefaultListCellRenderer {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            renderer.setBorder(BorderFactory.createEmptyBorder(1, 7, 1, 5));

            if (value instanceof ProfilingPoint) {
                boolean enabled = ((ProfilingPoint) value).isEnabled();
                renderer.setText(((ProfilingPoint) value).getName());
                renderer.setIcon(enabled ? ((ProfilingPoint) value).getFactory().getIcon() :
                                           ((ProfilingPoint) value).getFactory().getDisabledIcon());
                renderer.setEnabled(enabled);
            } else if (value instanceof ProfilingPointFactory) {
                renderer.setText(((ProfilingPointFactory) value).getType());
                renderer.setIcon(((ProfilingPointFactory) value).getIcon());
                renderer.setEnabled(true);
            } else {
                renderer.setIcon(null);
                renderer.setEnabled(true);
            }

            return renderer;
        }
    }

    private static class ProfilingPointPresenterRenderer extends LabelTableCellRenderer {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProfilingPointPresenterRenderer() {
            super(SwingConstants.LEADING);
            //      setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 5)); // TODO: enable once Scope is implemented
            setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 5));
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            return new ProfilingPointPresenterRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                                                                                       column);
        }

        protected void setValue(JTable table, Object value, int row, int column) {
            if (table != null) {
                setFont(table.getFont());
            }

            if (value instanceof ProfilingPoint) {
                boolean enabled = ((ProfilingPoint) value).isEnabled();
                label.setText(((ProfilingPoint) value).getName());
                label.setIcon(enabled ? ((ProfilingPoint) value).getFactory().getIcon() :
                                        ((ProfilingPoint) value).getFactory().getDisabledIcon());
                label.setEnabled(enabled);
            } else if (value instanceof ProfilingPointFactory) {
                label.setText(((ProfilingPointFactory) value).getType());
                label.setIcon(((ProfilingPointFactory) value).getIcon());
                label.setEnabled(true);
            } else {
                label.setText(""); //NOI18N
                label.setIcon(null);
                label.setEnabled(true);
            }
        }
    }

    private static class ProfilingPointScopeRenderer extends LabelTableCellRenderer {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProfilingPointScopeRenderer() {
            super(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            return new ProfilingPointScopeRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        protected void setValue(JTable table, Object value, int row, int column) {
            label.setText(""); //NOI18N

            if (value instanceof ProfilingPoint) {
                label.setIcon(((ProfilingPoint) value).getFactory().getScopeIcon());
                label.setEnabled(((ProfilingPoint) value).isEnabled());
            } else if (value instanceof ProfilingPointFactory) {
                label.setIcon(((ProfilingPointFactory) value).getScopeIcon());
                label.setEnabled(true);
            } else {
                label.setIcon(Icons.getIcon(GeneralIcons.EMPTY));
                label.setEnabled(true);
            }
        }
    }

    private static class ProjectPresenterListRenderer extends DefaultListCellRenderer {
        //~ Inner Classes --------------------------------------------------------------------------------------------------------

        private static class Renderer extends DefaultListCellRenderer {
            //~ Methods ----------------------------------------------------------------------------------------------------------

            public void setFont(Font font) {
            }

            public void setFontEx(Font font) {
                super.setFont(font);
            }
        }

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Renderer renderer = new Renderer();
        private boolean firstFontSet = false;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel rendererOrig = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            renderer.setComponentOrientation(rendererOrig.getComponentOrientation());
            renderer.setFontEx(rendererOrig.getFont());
            renderer.setOpaque(rendererOrig.isOpaque());
            renderer.setForeground(rendererOrig.getForeground());
            renderer.setBackground(rendererOrig.getBackground());
            renderer.setEnabled(rendererOrig.isEnabled());
            renderer.setBorder(rendererOrig.getBorder());

            if ((value != null) && value instanceof Lookup.Provider) {
                renderer.setText(ProjectUtilities.getDisplayName((Lookup.Provider)value));
                renderer.setIcon(ProjectUtilities.getIcon((Lookup.Provider)value));

                if (ProjectUtilities.getMainProject() == value) {
                    renderer.setFontEx(renderer.getFont().deriveFont(Font.BOLD)); // bold for main project
                } else {
                    renderer.setFontEx(renderer.getFont().deriveFont(Font.PLAIN));
                }
            } else {
                renderer.setText(rendererOrig.getText());
                renderer.setIcon(EMPTY_ICON);
            }

            return renderer;
        }
    }

    private static class ProjectPresenterRenderer extends LabelTableCellRenderer {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Font font;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProjectPresenterRenderer() {
            super(SwingConstants.LEADING);
            setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
            font = label.getFont();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            return new ProjectPresenterRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        protected void setValue(JTable table, Object value, int row, int column) {
            if ((value != null) && (value instanceof Lookup.Provider || value instanceof ProfilingPoint)) {
                if (table != null) {
                    setFont(table.getFont());
                }

                if (value instanceof ProfilingPoint) {
                    label.setEnabled(((ProfilingPoint) value).isEnabled());
                    value = ((ProfilingPoint) value).getProject();
                } else {
                    label.setEnabled(true);
                }

                final Icon icon = ProjectUtilities.getIcon((Lookup.Provider)value);
                label.setText(ProjectUtilities.getDisplayName((Lookup.Provider)value));
                label.setIcon(table.isEnabled() ? icon
                                                : new ImageIcon(GrayFilter.createDisabledImage(((ImageIcon) icon).getImage())));
                label.setFont((ProjectUtilities.getMainProject() == value) ? font.deriveFont(Font.BOLD) : font); // bold for main project
            } else {
                label.setText(""); //NOI18N
                label.setIcon(null);
                label.setEnabled(true);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String PROJECT_DIRECTORY_MARK = "{$projectDirectory}"; // NOI18N

    // TODO: Move to more "universal" location
    public static final ImageIcon EMPTY_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/empty16.gif", false); // NOI18N
    private static final ProjectPresenterRenderer projectRenderer = new ProjectPresenterRenderer();
    private static final ProjectPresenterListRenderer projectListRenderer = new ProjectPresenterListRenderer();
    private static final EnhancedTableCellRenderer scopeRenderer = new ProfilingPointScopeRenderer();
    private static final ProfilingPointPresenterRenderer presenterRenderer = new ProfilingPointPresenterRenderer();
    private static final ProfilingPointPresenterListRenderer presenterListRenderer = new ProfilingPointPresenterListRenderer();
    private static final DateFormat fullDateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
    private static final DateFormat todayDateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
    private static final DateFormat todayDateFormatHiRes = new SimpleDateFormat("HH:mm:ss.SSS"); // NOI118N
    private static final DateFormat dayDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String getAbsolutePath(Lookup.Provider project, String sourceFileRelativePath) {
        if (project == null) { // no project context for file

            File file = new File(sourceFileRelativePath);

            return file.exists() ? sourceFileRelativePath : null; // return sourceFileRelativePath if absolute path, null otherwise
        }

        return new File(sourceFileRelativePath.replace(PROJECT_DIRECTORY_MARK,
                                                       FileUtil.toFile(ProjectUtilities.getProjectDirectory(project)).getAbsolutePath()))
                                                                                                                                                                                                                                                                                                                                                                       .getAbsolutePath(); // expand relative path to absolute
    }

    public static String getClassName(CodeProfilingPoint.Location location) {
        File file = FileUtil.normalizeFile(new File(location.getFile()));
        FileObject fileObject = FileUtil.toFileObject(file);

        if ((fileObject == null) || !fileObject.isValid()) {
            return null;
        }

        int documentOffset = getDocumentOffset(location);

        if (documentOffset == -1) {
            return null;
        }
        // FIXME - optimize
        JavaProfilerSource src = JavaProfilerSource.createFrom(fileObject);
        if (src == null) return null;
        SourceClassInfo sci = src.getEnclosingClass(documentOffset);
        if (sci == null) return null;
        return sci.getQualifiedName();
    }
    
    public static String getMethodName(CodeProfilingPoint.Location location) {
        File file = FileUtil.normalizeFile(new File(location.getFile()));
        FileObject fileObject = FileUtil.toFileObject(file);

        if ((fileObject == null) || !fileObject.isValid()) {
            return null;
        }

        int documentOffset = getDocumentOffset(location);

        if (documentOffset == -1) {
            return null;
        }
        // FIXME - optimize
        JavaProfilerSource src = JavaProfilerSource.createFrom(fileObject);
        if (src == null) return null;
        SourceMethodInfo smi = src.getEnclosingMethod(documentOffset);
        if (smi == null) return null;
        return smi.getName();
    }

    public static CodeProfilingPoint.Location getCurrentLocation(int lineOffset) {
        EditorContext mostActiveContext = EditorSupport.getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        FileObject mostActiveJavaSource = mostActiveContext.getFileObject();

        if (mostActiveJavaSource == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        File currentFile = FileUtil.toFile(mostActiveJavaSource);

        if (currentFile == null) {
            return CodeProfilingPoint.Location.EMPTY; // Happens for AbstractFileObject, for example JDK classes
        }

        String fileName = currentFile.getAbsolutePath();

        int lineNumber = EditorSupport.getLineForOffset(mostActiveJavaSource, mostActiveContext.getTextComponent().getCaret().getDot()) + 1;

        if (lineNumber == -1) {
            lineNumber = 1;
        }

        return new CodeProfilingPoint.Location(fileName, lineNumber,
                                               lineOffset /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */);
    }

    public static Lookup.Provider getCurrentProject() {
        Lookup.Provider currentProject = getMostActiveJavaProject();

        if (currentProject == null) {
            currentProject = ProjectUtilities.getMainProject();
        }

        return currentProject;
    }

    public static CodeProfilingPoint.Location getCurrentSelectionEndLocation(int lineOffset) {
        EditorContext mostActiveContext = EditorSupport.getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        FileObject mostActiveJavaSource = mostActiveContext.getFileObject();

        if (mostActiveJavaSource == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        JTextComponent mostActiveTextComponent = mostActiveContext.getTextComponent();

        if (mostActiveTextComponent.getSelectedText() == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        String fileName = FileUtil.toFile(mostActiveJavaSource).getAbsolutePath();
        int lineNumber = EditorSupport.getLineForOffset(mostActiveJavaSource, mostActiveTextComponent.getSelectionEnd()) + 1;

        if (lineNumber == -1) {
            lineNumber = 1;
        }

        return new CodeProfilingPoint.Location(fileName, lineNumber,
                                               lineOffset /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */);
    }

    public static CodeProfilingPoint.Location[] getCurrentSelectionLocations() {
        EditorContext mostActiveContext = EditorSupport.getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return new CodeProfilingPoint.Location[0];
        }

        FileObject mostActiveJavaSource = mostActiveContext.getFileObject();

        if (mostActiveJavaSource == null) {
            return new CodeProfilingPoint.Location[0];
        }

        JTextComponent mostActiveTextComponent = mostActiveContext.getTextComponent();

        if (mostActiveTextComponent.getSelectedText() == null) {
            return new CodeProfilingPoint.Location[0];
        }

        File file = FileUtil.toFile(mostActiveJavaSource);

        if (file == null) {
            return new CodeProfilingPoint.Location[0]; // Most likely Java source
        }

        String fileName = file.getAbsolutePath();

        int startLineNumber = EditorSupport.getLineForOffset(mostActiveJavaSource, mostActiveTextComponent.getSelectionStart()) + 1;

        if (startLineNumber == -1) {
            startLineNumber = 1;
        }

        // #211681
        int endLineNumber = EditorSupport.getLineForOffset(mostActiveJavaSource, mostActiveTextComponent.getSelectionEnd() - 1) + 1;
        endLineNumber = Math.max(startLineNumber, endLineNumber);

        return new CodeProfilingPoint.Location[] {
                   new CodeProfilingPoint.Location(fileName, startLineNumber,
                                                   CodeProfilingPoint.Location.OFFSET_START /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */),
                   new CodeProfilingPoint.Location(fileName, endLineNumber,
                                                   CodeProfilingPoint.Location.OFFSET_END /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */)
               };
    }

    public static CodeProfilingPoint.Location getCurrentSelectionStartLocation(int lineOffset) {
        EditorContext mostActiveContext = EditorSupport.getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        FileObject mostActiveJavaSource = mostActiveContext.getFileObject();

        if (mostActiveJavaSource == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        JTextComponent mostActiveTextComponent = mostActiveContext.getTextComponent();

        if (mostActiveTextComponent.getSelectedText() == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        String fileName = FileUtil.toFile(mostActiveJavaSource).getAbsolutePath();
        int lineNumber = EditorSupport.getLineForOffset(mostActiveJavaSource, mostActiveTextComponent.getSelectionStart()) + 1;

        if (lineNumber == -1) {
            lineNumber = 1;
        }

        return new CodeProfilingPoint.Location(fileName, lineNumber,
                                               lineOffset /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */);
    }

    public static int getDocumentOffset(CodeProfilingPoint.Location location) {
        File file = FileUtil.normalizeFile(new File(location.getFile()));
        FileObject fileObject = FileUtil.toFileObject(file);

        if ((fileObject == null) || !fileObject.isValid()) return -1;

        int linePosition = EditorSupport.getOffsetForLine(fileObject, location.getLine() - 1); // Line is 1-based, needs to be 0-based
        if (linePosition == -1) return -1;
        
        int lineOffset;
        if (location.isLineStart()) {
            lineOffset = 0;
        } else if (location.isLineEnd()) {
            lineOffset = EditorSupport.getOffsetForLine(fileObject, location.getLine()) - linePosition - 1; // TODO: workaround to get line length, could fail at the end of last line!!!
            if (lineOffset == -1) return -1;
        } else {
            lineOffset = location.getOffset();
        }

        return linePosition + lineOffset;
    }

    public static double getDurationInMicroSec(long startTimestamp, long endTimestamp) {
        ProfilingSessionStatus session = Profiler.getDefault().getTargetAppRunner().getProfilingSessionStatus();
        double countsInMicroSec = session.timerCountsInSecond[0] / 1000000D;

        return (endTimestamp - startTimestamp) / countsInMicroSec;
    }

    //  public static DataObject getDataObject(CodeProfilingPoint.Location location) {
    //    // URL
    //    String url = location.getFile();
    //
    //    // FileObject
    //    FileObject file = null;
    //    try {
    //      file = URLMapper.findFileObject(new File(url).toURI().toURL());
    //    } catch (MalformedURLException e) {}
    //    if (file == null) return null;
    //
    //    // DataObject
    //    DataObject dao = null;
    //    try {
    //      dao = DataObject.find(file);
    //    } catch (DataObjectNotFoundException ex) {}
    //
    //    return dao;
    //  }
    public static Line getEditorLine(CodeProfilingPoint profilingPoint, CodeProfilingPoint.Annotation annotation) {
        return getEditorLine(profilingPoint.getLocation(annotation));
    }    
        
    public static Line getEditorLine(CodeProfilingPoint.Location location) {
        if (location == null) {
            return null;
        }
        
        // URL
        String url = location.getFile();
        if (url == null) {
            return null;
        }

        // FileObject
        FileObject file = null;

        try {
            file = URLMapper.findFileObject(new File(url).toURI().toURL());
        } catch (MalformedURLException e) {
        }

        if (file == null) {
            return null;
        }

        // DataObject
        DataObject dao = null;

        try {
            dao = DataObject.find(file);
        } catch (DataObjectNotFoundException ex) {
            return null;
        }

        // LineCookie of pp
        LineCookie lineCookie = (LineCookie) dao.getCookie(LineCookie.class);

        if (lineCookie == null) {
            return null;
        }

        // Line.Set of pp - real line where pp is defined
        Line.Set lineSet = lineCookie.getLineSet();

        if (lineSet == null) {
            return null;
        }

        try {
            return lineSet.getCurrent(location.getLine() - 1); // Line is 1-based, needs to be 0-based for Line.Set
        } catch (Exception e) {
        }

        return null;
    }
    
    public static boolean isValidLocation(CodeProfilingPoint.Location location) {
        // Fail if location not in method
        String methodName = Utils.getMethodName(location);
        if (methodName == null) return false;
        
        // Succeed if location in method body
        if (location.isLineStart()) return true;
        else if (location.isLineEnd()) {
            CodeProfilingPoint.Location startLocation = new CodeProfilingPoint.Location(
                    location.getFile(), location.getLine(), CodeProfilingPoint.Location.OFFSET_START);
            if (methodName.equals(Utils.getMethodName(startLocation))) return true;
        }

        Line line = getEditorLine(location); 
        if (line == null) return false;
        
        // #211135, line.getText() returns null for closed documents
        String lineText = line.getText();
        if (lineText == null) return false;
        
        // Fail if location immediately after method declaration - JUST A BEST GUESS!
        lineText = lineText.trim();
        if (lineText.endsWith("{") && lineText.indexOf('{') == lineText.lastIndexOf('{')) return false; // NOI18N
        
        return true;
    }
    
    public static void checkLocation(final CodeProfilingPoint.Single ppoint) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                if (!isValidLocation(ppoint.getLocation()))
                    ProfilerDialogs.displayWarning(
                            Bundle.Utils_InvalidPPLocationMsg(ppoint.getName()));
            }
        });
    }
    
    public static void checkLocation(final CodeProfilingPoint.Paired ppoint) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                if (!isValidLocation(ppoint.getStartLocation()))
                    ProfilerDialogs.displayWarning(
                            Bundle.Utils_InvalidPPLocationMsg(ppoint.getName()));
                else if (ppoint.usesEndLocation() && !isValidLocation(ppoint.getEndLocation()))
                    ProfilerDialogs.displayWarning(
                            Bundle.Utils_InvalidPPLocationMsg(ppoint.getName()));
            }
        });
    }

    public static Lookup.Provider getMostActiveJavaProject() {
        EditorContext mostActiveContext = EditorSupport.getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return null;
        }

        FileObject mostActiveFileObject = mostActiveContext.getFileObject();

        if (mostActiveFileObject == null) {
            return null;
        }

        return ProjectUtilities.getProject(mostActiveFileObject);
    }

    public static ListCellRenderer getPresenterListRenderer() {
        return presenterListRenderer;
    }

    public static EnhancedTableCellRenderer getPresenterRenderer() {
        return presenterRenderer;
    }

    public static CodeProfilingPoint[] getProfilingPointsOnLine(CodeProfilingPoint.Location location) {
        if ((location == null) || (location == CodeProfilingPoint.Location.EMPTY)) {
            return new CodeProfilingPoint[0];
        }

        File file = new File(location.getFile());
        int lineNumber = location.getLine();

        List<CodeProfilingPoint> lineProfilingPoints = new ArrayList();
        List<CodeProfilingPoint> profilingPoints = ProfilingPointsManager.getDefault()
                                                                         .getProfilingPoints(CodeProfilingPoint.class, null, false);

        for (CodeProfilingPoint profilingPoint : profilingPoints) {
            for (CodeProfilingPoint.Annotation annotation : profilingPoint.getAnnotations()) {
                CodeProfilingPoint.Location loc = profilingPoint.getLocation(annotation);

                if ((loc.getLine() == lineNumber) && new File(loc.getFile()).equals(file)) {
                    lineProfilingPoints.add(profilingPoint);

                    break;
                }
            }
        }

        return lineProfilingPoints.toArray(new CodeProfilingPoint[lineProfilingPoints.size()]);
    }

    // TODO: should be moved to ProjectUtilities
    public static ListCellRenderer getProjectListRenderer() {
        return projectListRenderer;
    }

    // TODO: should be moved to ProjectUtilities
    public static EnhancedTableCellRenderer getProjectRenderer() {
        return projectRenderer;
    }

    public static String getRelativePath(Lookup.Provider project, String sourceFileAbsolutePath) {
        if (project == null) {
            return sourceFileAbsolutePath; // no project context for file
        }
        final FileObject projectDirectory = ProjectUtilities.getProjectDirectory(project);
        String projectDirectoryAbsolutePath = FileUtil.toFile(projectDirectory).getAbsolutePath();

        if (!sourceFileAbsolutePath.startsWith(projectDirectoryAbsolutePath)) {
            return sourceFileAbsolutePath; // file not placed in project directory
        }

        File file = FileUtil.normalizeFile(new File(sourceFileAbsolutePath));

        return PROJECT_DIRECTORY_MARK + "/" // NOI18N
               + FileUtil.getRelativePath(projectDirectory, FileUtil.toFileObject(file)); // file placed in project directory => relative path used
    }

    public static EnhancedTableCellRenderer getScopeRenderer() {
        return scopeRenderer;
    }

    public static String getThreadClassName(int threadID) {
        // TODO: get the thread class name for RuntimeProfilingPoint.HitEvent.threadId
        return null;
    }

    public static String getThreadName(int threadID) {
        // TODO: get the thread name for RuntimeProfilingPoint.HitEvent.threadId
        return "&lt;unknown thread, id=" + threadID + "&gt;"; // NOI18N (not used)
    }

    public static long getTimeInMillis(final long hiResTimeStamp) {
        ProfilingSessionStatus session = Profiler.getDefault().getTargetAppRunner().getProfilingSessionStatus();
        long statupInCounts = session.startupTimeInCounts;
        long startupMillis = session.startupTimeMillis;
        long countsInMillis = session.timerCountsInSecond[0] / 1000L;

        return startupMillis + ((hiResTimeStamp - statupInCounts) / countsInMillis);
    }

    public static String getUniqueName(String name, String nameSuffix, Lookup.Provider project) {
        List<ProfilingPoint> projectProfilingPoints = ProfilingPointsManager.getDefault().getProfilingPoints(project, false, true);
        Set<String> projectProfilingPointsNames = new HashSet();

        for (ProfilingPoint projectProfilingPoint : projectProfilingPoints) {
            projectProfilingPointsNames.add(projectProfilingPoint.getName());
        }

        int index = 0;
        String indexStr = ""; // NOI18N

        while (projectProfilingPointsNames.contains(name + indexStr + nameSuffix)) {
            indexStr = " " + Integer.toString(++index); // NOI18N
        }

        return name + indexStr + nameSuffix;
    }

    public static String formatLocalProfilingPointTime(long timestamp) {
        Date now = new Date();
        Date date = new Date(timestamp);

        if (dayDateFormat.format(now).equals(dayDateFormat.format(date))) {
            return todayDateFormat.format(date);
        } else {
            return fullDateFormat.format(date);
        }
    }

    public static String formatProfilingPointTime(long timestamp) {
        long timestampInMillis = getTimeInMillis(timestamp);
        Date now = new Date();
        Date date = new Date(timestampInMillis);

        if (dayDateFormat.format(now).equals(dayDateFormat.format(date))) {
            return todayDateFormat.format(date);
        } else {
            return fullDateFormat.format(date);
        }
    }

    public static String formatProfilingPointTimeHiRes(long timestamp) {
        long timestampInMillis = getTimeInMillis(timestamp);
        Date now = new Date();
        Date date = new Date(timestampInMillis);

        if (dayDateFormat.format(now).equals(dayDateFormat.format(date))) {
            return todayDateFormatHiRes.format(date);
        } else {
            return todayDateFormatHiRes.format(date)+" "+dayDateFormat.format(date);  // NOI18N
        }
    }
    
    public static Font getTitledBorderFont(TitledBorder tb) {
        Font font = tb.getTitleFont();
        if (font == null) font = UIManager.getFont("TitledBorder.font"); // NOI18N
        if (font == null) font = new JLabel().getFont();
        if (font == null) font = UIManager.getFont("Label.font"); // NOI18N
        return font;
    }

    public static void openLocation(CodeProfilingPoint.Location location) {
        File file = FileUtil.normalizeFile(new File(location.getFile()));
        final FileObject fileObject = FileUtil.toFileObject(file);

        if ((fileObject == null) || !fileObject.isValid()) {
            return;
        }

        final int documentOffset = getDocumentOffset(location);

        if (documentOffset == -1) {
            ProfilerDialogs.displayError(Bundle.Utils_CannotOpenSourceMsg());
            return;
        }

        GoToSource.openFile(fileObject, documentOffset);
    }
    
}
