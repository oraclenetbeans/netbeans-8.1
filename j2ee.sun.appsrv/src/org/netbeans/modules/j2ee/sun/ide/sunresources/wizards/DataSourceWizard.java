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
/*
 * DataSourceWizard.java
 *
 * Created on September 30, 2003, 10:05 AM
 */

package org.netbeans.modules.j2ee.sun.ide.sunresources.wizards;

/**
 *
 * @author  nityad
 */

import java.awt.Component;
import java.util.Set;
import javax.swing.JComponent;
import java.io.InputStream;
import javax.swing.event.ChangeListener;

import org.openide.util.NbBundle;
import org.openide.WizardDescriptor;
import org.netbeans.api.project.Project;
import org.netbeans.modules.j2ee.sun.api.restricted.ResourceUtils;
import org.openide.filesystems.FileObject;
import org.netbeans.spi.project.ui.templates.support.Templates;

import org.netbeans.modules.j2ee.sun.sunresources.beans.Wizard;
import org.netbeans.modules.j2ee.sun.sunresources.beans.WizardConstants;
import org.openide.ErrorManager;

public final class DataSourceWizard implements WizardDescriptor.InstantiatingIterator, ChangeListener, WizardConstants{

    private Project project;
    
    /** An array of all wizard panels */
       
    private static final String DATAFILE = "org/netbeans/modules/j2ee/sun/sunresources/beans/DSWizard.xml";  //NOI18N
    private static final String CP_DATAFILE = "org/netbeans/modules/j2ee/sun/sunresources/beans/CPWizard.xml";  //NOI18N
    
    private transient int index;
    private transient WizardDescriptor.Panel[] panels;
//    private transient WizardDescriptor wiz;
    private transient String[] steps;
    
    private ResourceConfigHelper helper;
    private Wizard wizardInfo;
        
    private boolean addSteps = false;
//    private boolean firstTime = true;
    
    private ResourceConfigHelper cphelper;
    private ResourceConfigHelperHolder holder;
    private Wizard cpWizardInfo;
     
    private transient WizardDescriptor.Panel[] morePanels = null;
    private transient WizardDescriptor.Panel[] dsPanels = null;
    
    private transient String[] dsSteps = null;
    private transient String[] moreSteps = null;
    
    /** Creates a new instance of DataSourceWizard */
    public static DataSourceWizard create () {
        return new DataSourceWizard ();
    }
    
    private WizardDescriptor.Panel[] createPanels() {
        morePanels = null;
        WizardDescriptor.Panel panel = new CommonAttributePanel(helper, wizardInfo, new String[] {"general"});   //NOI18N
        panel.addChangeListener(this);
        return new WizardDescriptor.Panel[] {
            panel,
            new CommonPropertyPanel(this.helper, this.wizardInfo)
        };
    }
    
    private String[] createSteps() {
        return new String[] {
            NbBundle.getMessage(DataSourceWizard.class,__FirstStepChoose),
            NbBundle.getMessage(DataSourceWizard.class, "LBL_GeneralAttributes_DS"), // NOI18N
            NbBundle.getMessage(DataSourceWizard.class, "LBL_AddProperty") // NOI18N
        };
    }
    
    @Override
    public Set instantiate(){
        try{
            if(this.holder.hasCPHelper()){
                String poolName = this.cphelper.getData().getString(__Name);
                this.helper.getData().setString(__PoolName, poolName);
                this.cphelper.getData().setTargetFile(poolName);
                this.cphelper.getData().setTargetFileObject(this.helper.getData().getTargetFileObject());
                ResourceUtils.saveJDBCResourceDatatoXml(this.helper.getData(), this.cphelper.getData(),Util.getBaseName(project));
            }else{
                ResourceUtils.saveJDBCResourceDatatoXml(this.helper.getData(), null,Util.getBaseName(project));
            }    
        }catch (Exception ex){
                                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL,
                                        ex);
        }
        return java.util.Collections.EMPTY_SET;
    }
    
    @Override
    public void initialize(WizardDescriptor wiz){
        this.wizardInfo = getWizardInfo(DATAFILE);
        this.holder = new ResourceConfigHelperHolder();
        this.helper = holder.getDataSourceHelper();
        
        //this.wiz = wiz;
        wiz.putProperty("NewFileWizard_Title", NbBundle.getMessage(ConnPoolWizard.class, "Templates/SunResources/JDBC_Resource")); //NOI18N
        index = 0;
                
        project = Templates.getProject(wiz);
        
        panels = createPanels();
        // Make sure list of steps is accurate.
        steps = createSteps();
        
        try{
            FileObject pkgLocation = project.getProjectDirectory();
            if (pkgLocation != null) {
                this.helper.getData().setTargetFileObject(pkgLocation);
            }
        }catch (Exception ex){
                                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL,
                                        ex);
        }
        
        for (int i = 0; i < panels.length; i++) {
            Component c = panels[i].getComponent();
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent)c;
                // Step #.
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(i)); // NOI18N
                // Step name (actually the whole list for reference).
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps); // NOI18N
            }
        }
        
    }
    
    @Override
    public void uninitialize(WizardDescriptor wiz){
        //this.wiz = null;
        panels = null;
    }
    
    public Wizard getWizardInfo(String filePath){
        try{
            InputStream in = Wizard.class.getClassLoader().getResourceAsStream(filePath);
            this.wizardInfo = Wizard.createGraph(in);
        }catch(Exception ex){
                                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL,
                                        ex);
        }
        return this.wizardInfo;
    }
    
    @Override
    public String name(){
        return NbBundle.getMessage(DataSourceWizard.class, "Templates/SunResources/JDBC_Resource"); //NOI18N
    }
    
    @Override
    public boolean hasNext(){
        return index < panels.length - 1;
    }
    
    @Override
    public boolean hasPrevious(){
        return index > 0;
    }
    
    @Override
    public synchronized void nextPanel(){
        if (index + 1 == panels.length) {
            throw new java.util.NoSuchElementException();
        }
        
        if (index == 0) {
            ((CommonPropertyPanel) panels[1]).setInitialFocus();
        }else if (index == 1) {
            ((CPVendor) panels[2]).setInitialFocus();
        }else if (index == 2){
            ((CPPropertiesPanelPanel) panels[3]).refreshFields();
        }else if (index == 3){
            ((CommonAttributePanel) panels[4]).setPropInitialFocus();
        }
        
        index ++;
    }
    
    @Override
    public synchronized void previousPanel(){
        if (index == 0) {
            throw new java.util.NoSuchElementException();
        }
        
        index--;
    }
    
    @Override
    public WizardDescriptor.Panel current(){
        return (WizardDescriptor.Panel)panels[index];
    }
    
    @Override
    public final void addChangeListener(ChangeListener l) {
    }
    @Override
    public final void removeChangeListener(ChangeListener l) {
    }

    public void setResourceConfigHelper(ResourceConfigHelper helper){
        this.helper = helper;
    }
    
    public ResourceConfigHelper getResourceConfigHelper(){
        return this.helper;
    }
    
    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {
        if( (e.getSource().getClass() == CommonAttributePanel.class) || (e.getSource().getClass() == CommonAttributeVisualPanel.class) ) {
            CommonAttributePanel commonPane = (CommonAttributePanel)this.current();
            CommonAttributeVisualPanel visPane = (CommonAttributeVisualPanel)commonPane.getComponent();
            boolean oldVal = addSteps;
            this.addSteps = visPane.isNewResourceSelected();
            
            if((!oldVal && addSteps) || (oldVal && !addSteps)){
                this.holder.setHasCPHelper(this.addSteps);
                if (addSteps && morePanels == null) {
                    addPanels();
                    addSteps();
                    for (int i = 0; i < panels.length; i++) {
                        Component c = panels[i].getComponent();
                        if (steps[i] == null) {
                            steps[i] = c.getName();
                        }
                        if (c instanceof JComponent) {
                            JComponent jc = (JComponent)c;
                            jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(i)); // NOI18N
                            jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps); // NOI18N
                        }
                    }
                }else if((!addSteps) && (morePanels != null)){
                    if(dsPanels != null){
                        panels = dsPanels;
                        morePanels = null;
                    }else
                        panels = createPanels();
                    if(dsSteps != null)
                        steps = dsSteps;
                    else
                        steps = createSteps();
                    
                    for (int i = 0; i < panels.length; i++) {
                        Component c = panels[i].getComponent();
                        if (steps[i] == null) {
                            steps[i] = c.getName();
                        }
                        if (c instanceof JComponent) {
                            JComponent jc = (JComponent)c;
                            jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(i)); // NOI18N
                            jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps); // NOI18N
                        }
                    }
                    ((CommonAttributePanel)panels[0]).setInitialFocus();
                }
            }
        }
    }
    
    protected void addPanels() {
        if (panels != null && morePanels == null) {
            this.cphelper = this.holder.addAssociatedHelper();
            this.cphelper.getData().setResourceName(__JdbcConnectionPool);
            this.holder.setHasCPHelper(true);
            this.cpWizardInfo = getWizardInfo(CP_DATAFILE);
            this.cphelper.getData().setTargetFileObject(this.helper.getData().getTargetFileObject());
            this.cphelper.getData().setString(__DynamicWizPanel, "true"); //NOI18N
            
            morePanels = new WizardDescriptor.Panel[] {
                panels[0],
                panels[1],
                new CPVendor(this.cphelper, this.cpWizardInfo),
                new CPPropertiesPanelPanel(this.cphelper, this.cpWizardInfo),
                new CommonAttributePanel(this.cphelper, this.cpWizardInfo,  new String[] {"pool-setting", "pool-setting-2", "pool-setting-3"}), //NOI18N
            };
        }
        dsPanels = panels;
        panels = morePanels;
    }
        
    protected void addSteps() {
        if (steps != null && moreSteps == null) {
            moreSteps = new String[] {
                steps[0],
                steps[1],
                steps[2],
                NbBundle.getMessage(DataSourceWizard.class, "TITLE_ConnPoolWizardPanel_dbConn"), // NOI18N
                NbBundle.getMessage(DataSourceWizard.class, "TITLE_ConnPoolWizardPanel_properties"), // NOI18N
                NbBundle.getMessage(DataSourceWizard.class, "TITLE_ConnPoolWizardPanel_optionalProps") // NOI18N
            };
        }
        dsSteps = steps;
        steps = moreSteps;
    }     
} 
