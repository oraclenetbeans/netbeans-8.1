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
package org.netbeans.modules.javacard.project.deps.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.javacard.project.JCProject;
import org.netbeans.modules.javacard.project.deps.ArtifactKind;
import org.netbeans.modules.javacard.project.deps.Dependency;
import org.netbeans.modules.javacard.project.deps.DependencyKind;
import org.netbeans.modules.javacard.project.deps.DeploymentStrategy;
import org.netbeans.modules.javacard.project.deps.ResolvedDependencies;
import org.netbeans.modules.javacard.project.deps.ResolvedDependency;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

public final class AddDependencyWizardIterator implements WizardDescriptor.Iterator<Map<String, Object>> {

    static final String PROP_RESOLVED_DEPS = "_resolvedDependencies"; //NOI18N
    static final String PROP_TARGET_PROJECT = "_targetProject"; //NOI18N

    public static ResolvedDependency show(ResolvedDependencies deps, JCProject project) {
        AddDependencyWizardIterator iter = new AddDependencyWizardIterator();
        Map<String, Object> settings = Collections.synchronizedMap(new HashMap<String, Object>());
        settings.put(PROP_RESOLVED_DEPS, deps);
        settings.put(PROP_TARGET_PROJECT, project);
        WizardDescriptor desc = new WizardDescriptor(iter, settings);
        iter.setWizardDescriptor(desc);
        desc.setTitleFormat(new MessageFormat("{0} ({1})")); //NOI18N
        desc.setTitle(NbBundle.getMessage(AddDependencyWizardIterator.class,
                "TTL_ADD_LIBRARY")); //NOI18N
        Dialog dialog = DialogDisplayer.getDefault().createDialog(desc);
        dialog.setVisible(true);
        dialog.toFront();
        boolean cancelled = desc.getValue() != WizardDescriptor.FINISH_OPTION;
        if (!cancelled) {
            DependencyKind kind = (DependencyKind) settings.get(ChooseOriginWizardPanel.PROP_ACTUAL_DEP_KIND);
            DeploymentStrategy depStrat = (DeploymentStrategy) settings.get(ChooseDeploymentStrategyWizardPanel.PROP_DEPLOYMENT_STRATEGY);
            File origin = (File) settings.get(ChooseOriginWizardPanel.PROP_ORIGIN_FILE);
            File expFile = (File) settings.get(ChooseOriginWizardPanel.PROP_EXP_FILE);
            File sigFile = (File) settings.get(ChooseOriginWizardPanel.PROP_SIG_FILE);
            File sourceRoot = (File) settings.get(ChooseOriginWizardPanel.PROP_SOURCE_ROOT);
            assert origin != null;
            assert depStrat != null;
            assert kind != null;
            String nm = origin.getName();
            String id = nm;
            int ix = 0;
            while (deps.get(id) != null) {
                id = nm + "_" + (ix++);
            }
            Map<ArtifactKind, String> paths = new HashMap<ArtifactKind, String>();
            paths.put(ArtifactKind.ORIGIN, origin.getAbsolutePath());
            if (expFile != null && kind.supportedArtifacts().contains(ArtifactKind.EXP_FILE)) {
                paths.put(ArtifactKind.EXP_FILE, expFile.getAbsolutePath());
            }
            if (sigFile != null && kind.supportedArtifacts().contains(ArtifactKind.SIG_FILE)) {
                paths.put(ArtifactKind.SIG_FILE, sigFile.getAbsolutePath());
            }
            if (sourceRoot != null && kind.supportedArtifacts().contains(ArtifactKind.SOURCES_PATH)) {
                paths.put(ArtifactKind.SOURCES_PATH, sourceRoot.getAbsolutePath());
            }
            return deps.add(new Dependency(id, kind, depStrat), paths);
        }
        return null;
    }
    private int index;
    private WizardDescriptor.Panel<Map<String,Object>>[] panels;
    private WizardDescriptor wiz;
    private IntermediatePanelKind intermediatePanelKind;

    private void setWizardDescriptor(WizardDescriptor w) {
        this.wiz = w;
    }

    synchronized IntermediatePanelKind getIntermediatePanelKind() {
        return intermediatePanelKind;
    }

    void setIntermediatePanelKind(IntermediatePanelKind kind) {
        assert !EventQueue.isDispatchThread();
        synchronized (this) {
            this.intermediatePanelKind = kind;
        }
        try {
            //Need to trigger a re-get of the panels before next button pressing
            //completes.  This needs to happen on the event thread, or we will
            //get the previously computed set of panels
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    supp.fireChange();
                }
            });
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @SuppressWarnings("unchecked") //NOI18N
    private WizardDescriptor.Panel<Map<String,Object>>[] createPanels() {
        return new WizardDescriptor.Panel[]{
                    new ChooseDependencyKindWizardPanel(wiz),
                    new ChooseOriginWizardPanel(wiz, this),
                    new ChooseSigOrExpFilePanel(wiz),
                    new ChooseDeploymentStrategyWizardPanel(wiz)
                };
    }

    @SuppressWarnings("unchecked") //NOI18N
    private WizardDescriptor.Panel<Map<String,Object>>[] panels() {
        WizardDescriptor.Panel<Map<String,Object>>[] pnls = panels == null ? panels = createPanels() : panels;
        IntermediatePanelKind pk = getIntermediatePanelKind();
        WizardDescriptor.Panel<Map<String,Object>>[] result = new
                WizardDescriptor.Panel[pk == null ? pnls.length - 1 : pnls.length];
        for (int i = 0; i < result.length; i++) {
            if (i <= 1) {
                result[i] = pnls[i];
            } else if (pk != null) {
                result[i] = pnls[i];
            } else if (i > 1) {
                result[i] = pnls[i+1];
            }
        }
        return result;
    }

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel<Map<String,Object>>[] getPanels() {
        WizardDescriptor.Panel<Map<String,Object>>[] pnls = panels();
        //Always update the steps
        String[] steps = new String[pnls.length];
        for (int i = 0; i < pnls.length; i++) {
            Component c = pnls[i].getComponent();
            // Default step name to component name of panel.
            steps[i] = c.getName();
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent) c;
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(i)); //NOII18N
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps); //NOII18N
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, Boolean.TRUE); //NOII18N
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, Boolean.TRUE); //NOII18N
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, Boolean.TRUE); //NOII18N
            }
        }
        return pnls;
    }

    public WizardDescriptor.Panel<Map<String, Object>> current() {
        return getPanels()[index];
    }

    public String name() {
        return NbBundle.getMessage(AddDependencyWizardIterator.class,
                "LOCATION_IN_WIZARD", index + 1, getPanels().length); //NOI18N
    }

    public boolean hasNext() {
        return index < getPanels().length - 1;
    }

    public boolean hasPrevious() {
        return index > 0;
    }

    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        index++;
    }

    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        index--;
    }
    private final ChangeSupport supp = new ChangeSupport(this);
    // If nothing unusual changes in the middle of the wizard, simply:

    public void addChangeListener(ChangeListener l) {
        supp.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        supp.removeChangeListener(l);
    }
}
