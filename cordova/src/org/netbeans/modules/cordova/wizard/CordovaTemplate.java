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
package org.netbeans.modules.cordova.wizard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.modules.cordova.CordovaPerformer;
import org.netbeans.modules.cordova.CordovaPlatform;
import org.netbeans.modules.cordova.project.CordovaPanel;
import org.netbeans.modules.cordova.updatetask.SourceConfig;
import org.netbeans.modules.web.browser.api.BrowserFamilyId;
import org.netbeans.modules.web.browser.api.WebBrowser;
import org.netbeans.modules.web.browser.spi.ProjectBrowserProvider;
import org.netbeans.modules.web.clientproject.api.ClientProjectWizardProvider;
import org.netbeans.modules.web.clientproject.spi.ClientProjectExtender;
import org.netbeans.modules.web.clientproject.spi.SiteTemplateImplementation;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.Panel;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 */
@NbBundle.Messages({"LBL_Name=Cordova Hello World"})
@ServiceProvider(service = SiteTemplateImplementation.class, position = 1000)
public class CordovaTemplate implements SiteTemplateImplementation {

    @Override
    public String getName() {
        return Bundle.LBL_Name();
    }

    @Override
    public void apply(FileObject projectDir, ProjectProperties projectProperties, ProgressHandle handle) throws IOException {
        CordovaPerformer.getDefault().perform("create-hello-world", FileOwnerQuery.getOwner(projectDir)).waitFinished();
    }


    @Override
    public String getDescription() {
        return Bundle.LBL_Name();
    }

    @Override
    public boolean isPrepared() {
        return CordovaPlatform.getDefault().isReady();
    }

    @NbBundle.Messages(
        "ERR_NO_Cordova=NetBeans cannot find cordova on your PATH."
    )
    @Override
    public void prepare() throws IOException {
        if (!CordovaPlatform.getDefault().isReady()) {
            throw new IllegalStateException(Bundle.ERR_NO_Cordova(), null);
        }
    }

    @Override
    public void configure(ProjectProperties projectProperties) {
        projectProperties.setSiteRootFolder("www");//NOI18N
        projectProperties.setTestFolder("test");//NOI18N
    }

    @Override
    public String getId() {
        return "CORDOVA"; // NOI18N
    }

    @Override
    public void cleanup() {
        // noop
    }

    @ServiceProvider(service=ClientProjectExtender.class)
    public static class CordovaExtender implements ClientProjectExtender {

        private CordovaWizardPanel panel;
        private CordovaSetupPanel initPanel;

        @Override
        public Panel<WizardDescriptor>[] createWizardPanels() {
            return new Panel[]{panel=new CordovaWizardPanel(this)};
        }
        
        @Override
        public Panel<WizardDescriptor>[] createInitPanels() {
            if (CordovaPlatform.getDefault().isReady()) {
                return new Panel[0];
            }
            return new Panel[]{initPanel=new CordovaSetupPanel(null)};
        }

        @Override
        @NbBundle.Messages({
            "LBL_iPhoneSimulator=iPhone Simulator",
            "LBL_AndroidEmulator=Android Emulator",
            "LBL_AndroidDevice=Android Device"
        })
        public void apply(FileObject projectRoot, FileObject siteRoot, String librariesPath) {
            try {
                final Project project = FileOwnerQuery.getOwner(projectRoot);
                
                CordovaPerformer.getDefault().createPlatforms(project).waitFinished();
                
                Preferences preferences = ProjectUtils.getPreferences(project, CordovaPlatform.class, true);
                preferences.put("phonegap", "true"); // NOI18N
                setPhoneGapBrowser(project);
                
                if (panel != null) {
                    
                    try {
                        final SourceConfig config = CordovaPerformer.getConfig(project);
                        panel.save(config);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    CordovaPerformer.createScript(project, "plugins.properties", "nbproject/plugins.properties", true);
                   panel = null;
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (Throwable ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public void initialize(WizardDescriptor wizardDescriptor) {
            wizardDescriptor.putProperty("SITE_TEMPLATE", Lookup.getDefault().lookup(CordovaTemplate.class)); // NOI18N
            wizardDescriptor.putProperty("SITE_ROOT", "www"); // NOI18N
        }

        public static void setPhoneGapBrowser(final Project project) throws IOException, IllegalArgumentException {
            ProjectBrowserProvider browserProvider = project.getLookup().lookup(ProjectBrowserProvider.class);
            for (WebBrowser browser:browserProvider.getBrowsers()) {
                if (browser.getBrowserFamily() == BrowserFamilyId.PHONEGAP) {
                    if (Utilities.isMac()) {
                        if (browser.getId().equals("ios")) { // NOI18N
                            browserProvider.setActiveBrowser(browser);
                            break;
                        }
                    } else {
                        if (browser.getId().equals("android_1")) { // NOI18N
                            browserProvider.setActiveBrowser(browser);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static class CordovaWizardPanel implements Panel<WizardDescriptor>, PropertyChangeListener  {

        private CordovaExtender ext;
        private WizardDescriptor wizardDescriptor;
        public CordovaWizardPanel(CordovaExtender ext) {
            CordovaPlatform.getDefault().addPropertyChangeListener(this);
            this.ext = ext;
        }

        private CordovaPanel panel;
        private transient final ChangeSupport changeSupport = new ChangeSupport(this);

        @Override
        public void addChangeListener(ChangeListener listener) {
            changeSupport.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            changeSupport.removeChangeListener(listener);
        }

        @Override
        public JComponent getComponent() {
            if (panel == null) {
                panel = new CordovaPanel(ext);
                panel.addPropertyChangeListener(this);
            }
            return panel;
        }


        @Override
        public HelpCtx getHelp() {
            return new HelpCtx("org.netbeans.modules.cordova.template.CordovaTemplate$CordovaWizardPanel");//NOI18N
        }

        @Override
        public void readSettings(WizardDescriptor wizardDescriptor) {
            this.wizardDescriptor = wizardDescriptor;
            SiteTemplateImplementation template = (SiteTemplateImplementation) wizardDescriptor.getProperty("SITE_TEMPLATE");//NOI18N
            panel.setPanelEnabled(template instanceof CordovaTemplate);
            panel.setProjectName((String) wizardDescriptor.getProperty("NAME"));
            panel.setVersion(CordovaPerformer.DEFAULT_VERSION);
        }

        @Override
        public void storeSettings(WizardDescriptor settings) {
        }

        @Override
        @NbBundle.Messages({
            "ERR_MobilePlatforms=Mobile Platforms are not configured",
            "ERR_InvalidAppId={0} is not a valid Application ID"
        })
        public boolean isValid() {
//            final String sdkLocation = CordovaPlatform.getDefault().getSdkLocation();
//            if (sdkLocation == null) {
//                setErrorMessage(Bundle.ERR_MobilePlatforms());
//                return false;
//            }
//            
//            if (!SourceConfig.isValidId(panel.getPackageName())) {
//                setErrorMessage(Bundle.ERR_InvalidAppId(panel.getPackageName()));
//                return false;
//            }
//
//            setErrorMessage("");
            return true;
        }

        private void setErrorMessage(String message) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, message);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            changeSupport.fireChange();
            panel.update();
        }

        private void save(SourceConfig c) throws IOException {
            panel.save(c);
        }
    }
    
    @NbBundle.Messages({
        "LBL_CordovaApp=Cordova Application"
    })
    @TemplateRegistration(folder = "Project/ClientSide",
            displayName = "#LBL_CordovaApp",
            description = "../resources/CordovaProjectDescription.html", // NOI18N
            iconBase = "org/netbeans/modules/cordova/resources/project.png", // NOI18N
            position = 400)
    public static WizardDescriptor.InstantiatingIterator newProjectWithExtender() {
        return ClientProjectWizardProvider.newProjectWithExtender();
    }

}
