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

package org.netbeans.modules.websvc.metro.samples;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.JComponent;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.openide.WizardDescriptor;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.TemplateWizard;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Martin Grebac
 */
public class WebSampleProjectIterator implements TemplateWizard.Iterator {
    
    private static final long serialVersionUID = 4L;
    
    int currentIndex;
    transient PanelConfigureProject basicPanel;
    private transient WizardDescriptor wiz;
    private transient FileChangeListener fcl = new NbprojectFileChangeListener();

    static Object create() {
        return new WebSampleProjectIterator();
    }
    
    public WebSampleProjectIterator () {
    }
    
    public void addChangeListener (javax.swing.event.ChangeListener changeListener) {
    }
    
    public void removeChangeListener (javax.swing.event.ChangeListener changeListener) {
    }
    
    public org.openide.WizardDescriptor.Panel current () {
        return basicPanel;
    }
    
    public boolean hasNext () {
        return false;
    }
    
    public boolean hasPrevious () {
        return false;
    }
    
    public void initialize (org.openide.loaders.TemplateWizard templateWizard) {
        this.wiz = templateWizard;
        String name = templateWizard.getTemplate().getName();
        templateWizard.putProperty (WizardProperties.NAME, name);
        basicPanel = new PanelConfigureProject();
        currentIndex = 0;
        updateStepsList ();
    }
    
    public void uninitialize (org.openide.loaders.TemplateWizard templateWizard) {
        basicPanel = null;
        this.wiz.putProperty(WizardProperties.PROJECT_DIR,null);
        this.wiz.putProperty(WizardProperties.NAME,null);
        currentIndex = -1;
    }
    
    public java.util.Set instantiate (org.openide.loaders.TemplateWizard templateWizard) throws java.io.IOException {
        File projectLocation = (File) wiz.getProperty(WizardProperties.PROJECT_DIR);
        String name = (String) wiz.getProperty(WizardProperties.NAME);
        String serverID = (String) wiz.getProperty(WizardProperties.SERVER);
                        
        Collection<FileObject> prjLocs = null;
        prjLocs = WebSampleProjectGenerator.createProjectFromTemplate(
                templateWizard.getTemplate().getPrimaryFile(), 
                projectLocation, 
                name, serverID);
        
        Set hset = new HashSet();
        for (FileObject prj : prjLocs) {
            FileObject webRoot = prj.getFileObject("web");    //NOI18N
            FileObject index = getIndexFile(webRoot);
            if (webRoot != null) hset.add(DataObject.find(prj));
            if (index != null) hset.add(DataObject.find(index));
            
            // run wsimport-client-generate target when jaxws-build.xml is created
            if (prj.getName().contains("Client")) { //NOI18N
                FileObject nbprojectDir = prj.getFileObject("nbproject"); //NOI18N
                if (nbprojectDir != null) {
                    hset.add(nbprojectDir);
                    FileChangeListener weakListener = FileUtil.weakFileChangeListener(fcl,nbprojectDir);
                    nbprojectDir.addFileChangeListener(weakListener);
                }                
            }
        }

        return hset;
    }
    
    public String name() {
        return current().getComponent().getName();
    }
    
    public void nextPanel() {
        throw new NoSuchElementException ();
    }
    
    public void previousPanel() {
        throw new NoSuchElementException ();
    }
    
    void updateStepsList() {
        JComponent component = (JComponent) current ().getComponent ();
        if (component == null) {
            return;
        }
        String[] list;
        list = new String[] {
            NbBundle.getMessage(PanelConfigureProject.class, "LBL_NWP1_ProjectTitleName"), // NOI18N
        };
        component.putClientProperty (WizardDescriptor.PROP_CONTENT_DATA, list); // NOI18N
        component.putClientProperty (WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, Integer.valueOf(currentIndex)); // NOI18N
    }
    
    private FileObject getIndexFile(FileObject webRoot) {
        FileObject file = null;
        file = webRoot.getFileObject("index", "jsp");
        if (file == null) {
            file = webRoot.getFileObject("index", "html");
        }
        return file;
    }

    private static class NbprojectFileChangeListener extends FileChangeAdapter {

        @Override
        public void fileDataCreated(FileEvent fe) {
            if ("jaxws-build.xml".equals(fe.getFile().getNameExt())) { //NOI18N
                FileObject nbprojectDir = (FileObject) fe.getSource();
                final FileObject buildImplFo = nbprojectDir.getFileObject("build-impl.xml");
                if (buildImplFo != null) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            try {
                                ExecutorTask wsimportTask =
                                        ActionUtils.runTarget(buildImplFo,
                                        new String[]{"wsimport-client-generate"}, null); //NOI18N
                                wsimportTask.waitFinished();
                            } catch (IllegalArgumentException ex) {
                                // do nothing if there is no wsimport-client-generate target
                            } catch (java.io.IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    },1000);
                }
            }
        }
    }
    
}
