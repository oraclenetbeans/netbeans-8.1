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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.hudson.ant;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ChangeListener;
import javax.xml.xpath.XPathFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.hudson.ant.AntBasedJobCreator.ArchivePattern;
import org.netbeans.modules.hudson.ant.AntBasedJobCreator.Configuration;
import org.netbeans.modules.hudson.ant.AntBasedJobCreator.Target;
import static org.netbeans.modules.hudson.ant.Bundle.*;
import org.netbeans.modules.hudson.ui.spi.ProjectHudsonJobCreatorFactory;
import org.netbeans.modules.hudson.ui.spi.ProjectHudsonJobCreatorFactory.Helper;
import org.netbeans.modules.hudson.ui.spi.ProjectHudsonJobCreatorFactory.ProjectHudsonJobCreator;
import org.netbeans.modules.hudson.spi.HudsonSCM;
import org.netbeans.modules.hudson.spi.HudsonSCM.ConfigurationStatus;
import org.netbeans.modules.java.api.common.project.ui.customizer.ProjectSharability;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class JobCreator extends JPanel implements ProjectHudsonJobCreator {

    @ServiceProvider(service=ProjectHudsonJobCreatorFactory.class, position=200)
    public static class Factory implements ProjectHudsonJobCreatorFactory {
        public ProjectHudsonJobCreator forProject(Project project) {
            FileObject projectXml = project.getProjectDirectory().getFileObject("nbproject/project.xml"); // NOI18N
            if (projectXml != null && projectXml.isData()) {
                try {
                    Document doc = XMLUtil.parse(new InputSource(projectXml.getURL().toString()), false, true, null, null);
                    String type = XPathFactory.newInstance().newXPath().evaluate(
                            "/*/*[local-name(.)='type']", doc.getDocumentElement()); // NOI18N
                    for (AntBasedJobCreator handler : Lookup.getDefault().lookupAll(AntBasedJobCreator.class)) {
                        if (handler.type().equals(type)) {
                            return new JobCreator(project, handler.forProject(project));
                        }
                    }
                } catch (Exception x) {
                    Logger.getLogger(JobCreator.class.getName()).log(Level.FINE, "Could not check type of " + projectXml, x);
                }
            }
            return null;
        }
    }

    private final Project project;
    private final Configuration config;
    private final ProjectSharability shar;
    private final HudsonSCM.Configuration scm;
    private final Map<Target,JCheckBox> checkboxen;

    public JobCreator(Project project, Configuration config) {
        this.project = project;
        this.config = config;
        this.shar = project.getLookup().lookup(ProjectSharability.class);
        scm = Helper.prepareSCM(FileUtil.toFile(project.getProjectDirectory()));
        checkboxen = initComponents();
    }

    public String jobName() {
        return ProjectUtils.getInformation(project).getName();
    }

    public JComponent customizer() {
        return this;
    }

    @Messages({
        "JobCreator.copy_message=Global libraries should be copied to a dedicated libraries folder.",
        "JobCreator.copy_label=&Copy Libraries..."
    })
    public ConfigurationStatus status() {
        if (scm == null) {
            return Helper.noSCMError();
        }
        if (shar != null && !shar.isSharable()) {
            String msg = JobCreator_copy_message();
            JButton button = new JButton();
            Mnemonics.setLocalizedText(button, JobCreator_copy_label());
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    shar.makeSharable();
                }
            });
            return ConfigurationStatus.withWarning(msg).withExtraButton(button);
        }
        ConfigurationStatus scmStatus = scm.problems();
        if (scmStatus != null) {
            return scmStatus;
        } else {
            return ConfigurationStatus.valid();
        }
    }

    public void addChangeListener(ChangeListener listener) {}
    public void removeChangeListener(ChangeListener listener) {}

    public Document configure() throws IOException {
        Document doc = XMLUtil.createDocument("project", null, null, null); // NOI18N
        Element projectE = doc.getDocumentElement();
        StringBuilder targetsS = new StringBuilder();
        for (Map.Entry<Target,JCheckBox> entry : checkboxen.entrySet()) {
            if (entry.getValue().isSelected()) {
                if (targetsS.length() > 0) {
                    targetsS.append(' ');
                }
                targetsS.append(entry.getKey().antName());
            }
        }
        Element ant = (Element) projectE.appendChild(doc.createElement("builders")). // NOI18N
                appendChild(doc.createElement("hudson.tasks.Ant")); // NOI18N
        ant.appendChild(doc.createElement("targets")). // NOI18N
                appendChild(doc.createTextNode(targetsS.toString()));
        StringBuilder properties = null;
        for (Map.Entry<Target,JCheckBox> entry : checkboxen.entrySet()) {
            if (entry.getValue().isSelected()) {
                String props = entry.getKey().properties();
                if (props != null) {
                    if (properties == null) {
                        properties = new StringBuilder();
                    } else {
                        properties.append('\n');
                    }
                    properties.append(props);
                }
            }
        }
        if (properties != null) {
            ant.appendChild(doc.createElement("properties")). // NOI18N
                    appendChild(doc.createTextNode(properties.toString()));
        }
        Element publishers = (Element) projectE.appendChild(doc.createElement("publishers")); // NOI18N
        for (Map.Entry<Target,JCheckBox> entry : checkboxen.entrySet()) {
            if (!entry.getValue().isSelected()) {
                continue;
            }
            Target t = entry.getKey();
            ArchivePattern archive = t.artifactArchival();
            if (archive != null) {
                Element aa = (Element) publishers.appendChild(doc.createElement("hudson.tasks.ArtifactArchiver")); // NOI18N
                aa.appendChild(doc.createElement("artifacts")). // NOI18N
                        appendChild(doc.createTextNode(archive.includes()));
                String excl = archive.excludes();
                if (excl != null) {
                    aa.appendChild(doc.createElement("excludes")). // NOI18N
                            appendChild(doc.createTextNode(excl));
                }
                aa.appendChild(doc.createElement("latestOnly")). // NOI18N
                        appendChild(doc.createTextNode("true")); // NOI18N
            }
            String javadoc = t.javadocDir();
            if (javadoc != null) {
                publishers.appendChild(doc.createElement("hudson.tasks.JavadocArchiver")). // NOI18N
                        appendChild(doc.createElement("javadocDir")). // NOI18N
                        appendChild(doc.createTextNode(javadoc));
            }
            String tests = t.testResults();
            if (tests != null) {
                publishers.appendChild(doc.createElement("hudson.tasks.junit.JUnitResultArchiver")). // NOI18N
                        appendChild(doc.createElement("testResults")). // NOI18N
                        appendChild(doc.createTextNode(tests));
            }
        }
        for (String dummy : new String[] {"actions", "buildWrappers"}) { // NOI18N
            projectE.appendChild(doc.createElement(dummy));
        }
        scm.configure(doc);
        Helper.addLogRotator(doc);
        return doc;
    }

    @Messages({"# {0} - name of Ant target which be run if selected", "JobCreator.checkbox.a11y=Run Ant target: {0}"})
    private Map<Target,JCheckBox> initComponents() {
        Map<Target,JCheckBox> boxen = new LinkedHashMap<Target,JCheckBox>();
        for (Target t : config.targets()) {
            JCheckBox box = new JCheckBox();
            boxen.put(t, box);
            box.setSelected(t.selected());
            box.setEnabled(t.enabled());
            Mnemonics.setLocalizedText(box, t.labelWithMnemonic());
            box.getAccessibleContext().setAccessibleDescription(JobCreator_checkbox_a11y(t.antName()));
        }
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        ParallelGroup parallelGroup = layout.createParallelGroup(Alignment.LEADING);
        for (JCheckBox box : boxen.values()) {
            parallelGroup.addComponent(box);
        }
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(parallelGroup)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        SequentialGroup sequentialGroup = layout.createSequentialGroup();
        boolean first = false;
        for (JCheckBox box : boxen.values()) {
            if (first) {
                first = false;
                sequentialGroup.addContainerGap();
            } else {
                sequentialGroup.addPreferredGap(ComponentPlacement.RELATED);
            }
            sequentialGroup.addComponent(box);
        }
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(sequentialGroup
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        return boxen;
    }

}
