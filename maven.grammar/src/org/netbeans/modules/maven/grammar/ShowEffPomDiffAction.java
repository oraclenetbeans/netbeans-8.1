/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.maven.grammar;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.diff.Diff;
import org.netbeans.api.diff.DiffView;
import org.netbeans.api.diff.Difference;
import org.netbeans.api.diff.StreamSource;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.maven.api.MavenConfiguration;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.embedder.EmbedderFactory;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 *
 * @author mkleint
 */
class ShowEffPomDiffAction extends AbstractAction {

    private static final Logger LOG = Logger.getLogger(ShowEffPomDiffAction.class.getName());
    private static final @StaticResource
    String ICON = "org/netbeans/modules/maven/grammar/diffSettingsIcon.gif";
    private final Lookup lookup;

    ShowEffPomDiffAction(Lookup lookup) {
        putValue(NAME, "Show diff");
        putValue(Action.SMALL_ICON, ImageUtilities.loadImage(ICON));
        putValue(SHORT_DESCRIPTION, "Show diff between the current effective pom and alternate ones");
        this.lookup = lookup;
    }

    void calculateEnabledState() {
        FileObject pom = lookup.lookup(FileObject.class);
        if (pom != null) {
            Project p = null;

            try {
                p = ProjectManager.getDefault().findProject(pom.getParent());
                if (p != null) {
                    NbMavenProject nbmp = p.getLookup().lookup(NbMavenProject.class);
                    if (nbmp == null || nbmp.isUnloadable()) {
                        p = null;
                    }
                }
            } catch (IOException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
            } catch (IllegalArgumentException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
            }
            setEnabled(p != null);
        } else {
            setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FileObject pom = lookup.lookup(FileObject.class);
        if (pom != null) {
            Project p = null;

            try {
                p = ProjectManager.getDefault().findProject(pom.getParent());
            } catch (IOException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
            } catch (IllegalArgumentException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
            }
            if (p != null) {
                final ProjectConfigurationProvider<MavenConfiguration> conf = p.getLookup().lookup(ProjectConfigurationProvider.class);
                ShowEffPomDiffPanel pnl = new ShowEffPomDiffPanel(conf);
                DialogDescriptor dd = new DialogDescriptor(pnl, "Select configurations/profiles to diff");
                if (DialogDisplayer.getDefault().notify(dd) == DialogDescriptor.OK_OPTION) {
                    NbMavenProject nbmp = p.getLookup().lookup(NbMavenProject.class);
                    final Model model;
                    final Model model2;
                    final MavenConfiguration sel = pnl.isConfigurationSelected() ? pnl.getSelectedConfig() : null;
                    final List<String> profiles = sel != null ? sel.getActivatedProfiles() : pnl.getSelectedProfiles();
                    final Properties props = new Properties();
                    if (sel != null) {
                        props.putAll(sel.getProperties());
                    } else {
                        props.putAll(pnl.getSelectedProperties());
                    }
                    if (nbmp != null) {
                        model = nbmp.getMavenProject().getModel();
                        if (model == null || nbmp.isUnloadable()) {
                            //  errorMessage = ERR_Unloadable();
                        }
                        model2 = nbmp.loadAlternateMavenProject(EmbedderFactory.getProjectEmbedder(), profiles, props).getModel();
                    } else {
                        //not project based.
                        model = null;
                        model2 = null;
                    }

                    StreamSource ss = new StreamSource() {

                        @Override
                        public String getName() {
                            return "Current";
                        }

                        @Override
                        public String getTitle() {
                            return "Current Project Effective POM (configuration: " + conf.getActiveConfiguration().getDisplayName() + ")";
                        }

                        @Override
                        public String getMIMEType() {
                            return "text/x-maven-pom+xml";
                        }

                        @Override
                        public Reader createReader() throws IOException {
                            final StringWriter sw = new StringWriter();
                            new MavenXpp3Writer().write(sw, model);
                            return new StringReader(sw.toString());
                        }

                        @Override
                        public Writer createWriter(Difference[] conflicts) throws IOException {
                            return null;
                        }

                    };
                    StreamSource ss2 = new StreamSource() {

                        @Override
                        public String getName() {
                            return "Alternate";
                        }

                        @Override
                        public String getTitle() {
                            return "Alternate Effective POM (configuration: " + (sel != null ? sel.getDisplayName() : "<custom>") + ")";
                        }

                        @Override
                        public String getMIMEType() {
                            return "text/x-maven-pom+xml";
                        }

                        @Override
                        public Reader createReader() throws IOException {
                            final StringWriter sw = new StringWriter();
                            new MavenXpp3Writer().write(sw, model2);
                            return new StringReader(sw.toString());
                        }

                        @Override
                        public Writer createWriter(Difference[] conflicts) throws IOException {
                            return null;
                        }
                    };

                    if (model != null && model2 != null) {
                        showDiff(ss, ss2, "Effective Diff - " + pom.getNameExt() + POMDataObject.annotateWithProjectName(pom));
                    }
                }
            }
        }
    }

    @NbBundle.Messages({"# {0} - file basename", "Title.diffing={0}"})
    public static void showDiff(final StreamSource before, final StreamSource after, final String tcName) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    DiffView view = Diff.getDefault().createDiff(before, after);
                    // XXX reuse the same TC
                    DiffTopComponent tc = new DiffTopComponent(view);
                    tc.setName(tcName);
                    tc.setDisplayName(tcName); //NOI18N
                    tc.open();
                    tc.requestActive();
                } catch (IOException x) {
                    LOG.log(Level.INFO, null, x);
                }
            }
        });
    }

    private static class DiffTopComponent extends TopComponent {

        DiffTopComponent(DiffView view) {
            setLayout(new BorderLayout());
            add(view.getComponent(), BorderLayout.CENTER);
        }

        public @Override
        int getPersistenceType() {
            return TopComponent.PERSISTENCE_NEVER;
        }

        protected @Override
        String preferredID() {
            return "DiffTopComponent"; // NOI18N
        }
    }
}
