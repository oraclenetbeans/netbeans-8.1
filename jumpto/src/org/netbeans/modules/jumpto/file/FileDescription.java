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
 * The Original Software is the Accelerators module.
 * The Initial Developer of the Original Software is Andrei Badea.
 * Portions Copyright 2005-2006 Andrei Badea.
 * All Rights Reserved.
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
 * Contributor(s): Petr Hrebejk
  *                markiewb@netbeans.org
 */

package org.netbeans.modules.jumpto.file;

import java.awt.Image;
import java.beans.BeanInfo;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.api.actions.Editable;
import org.netbeans.api.actions.Openable;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.jumpto.file.FileDescriptor;
import org.openide.cookies.EditCookie;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.ImageUtilities;
import org.openide.util.Parameters;

/** Contains interesting information about file found in the search.
 *
 * @author Petr Hrebejk
 * @author Tomas Zezula
 * todo: SPI interface if needed. Now FileObject added to result seems to be enough
 * and everyone will use the default impl anyway.
 */
public class FileDescription extends FileDescriptor {

    private static final Logger LOG = Logger.getLogger(FileDescription.class.getName());
    /**
     * The icon used if unknown project, i.e. {@code project == null}.
     * In such case, we use {@code find.png} - "a file belongs to the find".
     */
    public static ImageIcon UNKNOWN_PROJECT_ICON = ImageUtilities.loadImageIcon(
             "org/netbeans/modules/jumpto/resources/find.gif", false); // NOI18N
    private final FileObject fileObject;
    private final String ownerPath;
    private final Project project; // Project the file belongs to
    private volatile Icon icon;
    private volatile String projectName;
    private volatile Icon projectIcon;
    private volatile ProjectInformation projectInfo;

    public FileDescription(
            @NonNull final FileObject file,
            @NonNull final String ownerPath,
            @NullAllowed final Project project) {
        Parameters.notNull("file", file);   //NOI18N
        Parameters.notNull("ownerPath", ownerPath); //NOI18N
        this.fileObject = file;
        this.ownerPath = ownerPath;
        this.project = project;
    }

    @Override
    public String getFileName() {
        return fileObject.getNameExt(); // NOI18N
    }

    @Override
    @NonNull
    public Icon getIcon() {
        Icon res = icon;
        if (res == null) {
            final DataObject od = getDataObject();
            final Image img = od == null ? // #187973
                UNKNOWN_PROJECT_ICON.getImage() :
                od.getNodeDelegate().getIcon(BeanInfo.ICON_COLOR_16x16);
            res = icon = new ImageIcon(img);
        }
        return res;
    }

    @Override
    public String getOwnerPath() {
        return ownerPath;
    }

    @Override
    @NonNull
    public String getProjectName() {
        String res = projectName;
        if (res == null) {
            final ProjectInformation pi = getProjectInfo();
            res = projectName = pi == null ?
                "" :    //NOI18N
                pi.getDisplayName();
        }
        return res;
    }

    @Override
    @NonNull
    public Icon getProjectIcon() {
        Icon res = projectIcon;
        if ( res == null ) {
            final ProjectInformation pi = getProjectInfo();
            res = projectIcon = pi == null ?
                UNKNOWN_PROJECT_ICON :
                pi.getIcon();
        }
        return res;
    }


    @Override
    public void open() {
        final DataObject od = getDataObject();
        if (od != null) {
            final FileObject fo = getFileObject();
            if (fo != od.getPrimaryFile()) {
                open(od);
            } else {
                edit(od, getLineNumber());
            }
        }
    }

    @Override
    public FileObject getFileObject() {
        return fileObject;
    }

    private DataObject getDataObject() {
        try     {
            org.openide.filesystems.FileObject fo = getFileObject();
            return org.openide.loaders.DataObject.find(fo);
        }
        catch (DataObjectNotFoundException ex) {
            return null;
        }
    }

    @CheckForNull
    private ProjectInformation getProjectInfo() {
        // Issue #167198: A file may not belong to any project.
        // Hence, FileOwnerQuery.getOwner(file) can return null as a project,
        // and fileDescription.project will be null too.
        // But! We should not call ProjectUtils.getInformation(null).
        if(project == null) {
            return null;
        }
        ProjectInformation res = projectInfo;
        if (res == null) {
            //Don't use slow ProjectUtils.getInformation
            res = projectInfo = project.getLookup().lookup(ProjectInformation.class);
        }
        return res;
    }

    private static void open(@NonNull final DataObject dobj) {
        final Openable openable = dobj.getLookup().lookup(Openable.class);
        if (openable != null) {
            openable.open();
        }
        //Workaround of non functional org.openide.util.Lookup class hierarchy cache.
        final OpenCookie oc = dobj.getLookup().lookup(OpenCookie.class);
        if (oc != null) {
            oc.open();
        }
    }

    private static void edit(@NonNull final DataObject dobj, final int lineNo) {
        // if linenumber is given then try to open file at this line
        // code taken from org.netbeans.modules.java.stackanalyzer.StackLineAnalyser.Link.show()
        LineCookie lineCookie = dobj.getLookup().lookup(LineCookie.class);
        if (lineCookie == null) {
            //Workaround of non functional org.openide.util.Lookup class hierarchy cache.
            lineCookie = dobj.getLookup().lookup(EditorCookie.class);
        }
        if (lineCookie != null && lineNo != -1) {
            try {
                final Line l = lineCookie.getLineSet().getCurrent(lineNo - 1);
                if (l != null) {
                    // open file at the given line
                    l.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS, -1);
                    return;
                }
            } catch (IndexOutOfBoundsException oob) {
                LOG.log(Level.FINE, "Line no more exists.", oob);   //NOI18N
            }
        }
        Editable editable = dobj.getLookup().lookup(Editable.class);
        if (editable == null) {
            //Workaround of non functional org.openide.util.Lookup class hierarchy cache.
            editable = dobj.getLookup().lookup(EditCookie.class);
        }
        if (editable != null) {
            editable.edit();
            return;
        }
        open(dobj);
    }
}
