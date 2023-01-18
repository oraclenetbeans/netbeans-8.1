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

package org.netbeans.modules.maven.apisupport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.modules.java.api.common.ant.PackageModifierImplementation;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.model.ModelOperation;
import org.netbeans.modules.maven.model.Utilities;
import org.netbeans.modules.maven.model.pom.Build;
import org.netbeans.modules.maven.model.pom.Configuration;
import org.netbeans.modules.maven.model.pom.POMExtensibilityElement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.POMQName;
import org.netbeans.modules.maven.model.pom.Plugin;
import org.netbeans.modules.maven.model.pom.PluginContainer;
import org.netbeans.modules.maven.model.pom.PluginManagement;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.explorer.ExplorerManager;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author mkleint
 */
@ProjectServiceProvider(service = PackageModifierImplementation.class, projectType="org-netbeans-modules-maven/" + NbMavenProject.TYPE_NBM)
public class MavenPackageModifierImpl implements PackageModifierImplementation {
    private final Project project;

    public MavenPackageModifierImpl(Project p) {
        this.project = p;
    }

    static final String ID_LOGICAL = "projectTabLogical_tc"; // NOI18N    
    @Override
    public void exportPackageAction(final Collection<String> packagesToExport, final boolean export) {
        
        final PropertyChangeListener prop = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!NbMavenProject.PROP_PROJECT.equals(evt.getPropertyName())) {
                    return;
                }
                try {
                    FileObject srcdir = org.netbeans.modules.maven.api.FileUtilities.convertStringToFileObject(project.getLookup().lookup(NbMavenProject.class).getMavenProject().getBuild().getSourceDirectory());
                    if (srcdir != null) {
                        LogicalViewProvider lvp = project.getLookup().lookup(LogicalViewProvider.class);
                        assert lvp != null;
                        Node root = findProjectTCRootNode();
                        if (root != null) {
                            for (Node prjNode : root.getChildren().getNodes(true)) {
                                Project p = prjNode.getLookup().lookup(Project.class);
                                if (project.equals(p)) {
                                    Node found = lvp.findPath(prjNode, srcdir);
                                    if (found != null) {
                                        iterateNodesAndFireIconChange(found);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } finally {
                    project.getLookup().lookup(NbMavenProject.class).removePropertyChangeListener(this);
                }
            }

            //ugly reflection to force a change of icon badge
            private void iterateNodesAndFireIconChange(Node found) {
                for (Node nd : found.getChildren().getNodes(true)) {
                    FileObject fo = nd.getLookup().lookup(FileObject.class);
                    if (fo != null && fo.isFolder() && fo.isFolder()) {
                        try {
                            Method m1 = Node.class.getDeclaredMethod("fireIconChange");
                            m1.setAccessible(true);
                            Method m2 = Node.class.getDeclaredMethod("fireOpenedIconChange");
                            m2.setAccessible(true);
                            m2.invoke(nd);
                            m1.invoke(nd);
                        } catch (NoSuchMethodException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (SecurityException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (IllegalAccessException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (IllegalArgumentException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (InvocationTargetException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        iterateNodesAndFireIconChange(nd);
                    }
                }
            }

            //ugly way of finding the shown root package node to fire iconChange events
            private Node findProjectTCRootNode() {
                final Node[] toRet = new Node[1];
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            TopComponent projecttc = WindowManager.getDefault().findTopComponent(ID_LOGICAL);
                            if (projecttc != null && projecttc instanceof ExplorerManager.Provider) {
                                ExplorerManager.Provider em = (ExplorerManager.Provider) projecttc;
                                Node root = em.getExplorerManager().getRootContext();
                                toRet[0] = root;
                            }
                        }
                    });
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }
                return toRet[0];
            }
        };
        Utilities.performPOMModelOperations(project.getProjectDirectory().getFileObject("pom.xml"),
                Collections.singletonList(new ModelOperation<POMModel>() {

                    @Override
                    public void performOperation(POMModel model) {
                        org.netbeans.modules.maven.model.pom.Project p = model.getProject();
                        Build bld = p.getBuild();
                        POMExtensibilityElement publicPackages = null;
                        if (bld != null) {
                            publicPackages = findPublicPackagesElement(bld);
                            if (publicPackages != null) {
                                //find in pluginmanagement
                                PluginManagement pm = bld.getPluginManagement();
                                if (pm != null) {
                                    publicPackages = findPublicPackagesElement(pm);
                                }
                            }
                        }

                        if (publicPackages == null && export) {
                            publicPackages = createPublicPackagesElement(p);
                            assert publicPackages != null;
                        }
                        if (export) {
                            for (String exp : packagesToExport) {
                                POMExtensibilityElement pack = model.getFactory().createPOMExtensibilityElement(POMQName.createQName("publicPackage"));
                                publicPackages.addExtensibilityElement(pack);
                                pack.setElementText(exp);
                            }
                        } else {
                            //find the right publicPackage element and remove it.
                            //TODO what to do with subpackages?
                            Set<POMExtensibilityElement> toRemove = new HashSet<POMExtensibilityElement>();
                            LBL : for (String exp : packagesToExport) {
                                for (POMExtensibilityElement el : publicPackages.getExtensibilityElements()) {
                                    if ("publicPackage".equals(el.getQName().getLocalPart()) && el.getElementText() != null && el.getElementText().equals(exp)) {
                                        toRemove.add(el);
                                        continue LBL;
                                    }
                                }
                                //TODO not in the current file's list or is covered by subpackage?
                                
                            }
                            if (!toRemove.isEmpty()) {
                                for (POMExtensibilityElement el : toRemove) {
                                    publicPackages.removeExtensibilityElement(el);
                                }
                                if (publicPackages.getChildren() == null || publicPackages.getChildren().isEmpty()) {
                                    publicPackages.getParent().removeExtensibilityElement(publicPackages);
                                }
                            }
                        }
                        project.getLookup().lookup(NbMavenProject.class).addPropertyChangeListener(prop);
                    }

                    private POMExtensibilityElement createPublicPackagesElement(org.netbeans.modules.maven.model.pom.Project p) {
                        Build bld = p.getBuild();
                        if (bld == null) {
                            bld = p.getModel().getFactory().createBuild();
                            p.setBuild(bld);
                        }
                        Plugin plug = bld.findPluginById("org.codehaus.mojo", "nbm-maven-plugin");
                        if (plug == null) {
                            //mostly should not happen with nb modules..
                            plug = bld.getModel().getFactory().createPlugin();
                            plug.setGroupId("org.codehaus.mojo");
                            plug.setArtifactId("nbm-maven-plugin");
                            plug.setExtensions(Boolean.TRUE);
                            //set version? let's hope it's managed..
                            bld.addPlugin(plug);
                        }
                        Configuration conf = plug.getConfiguration();
                        if (conf == null) {
                            conf = plug.getModel().getFactory().createConfiguration();
                            plug.setConfiguration(conf);
                        }
                        List<POMExtensibilityElement> elems = conf.getConfigurationElements();
                        if (elems != null) {
                            for (POMExtensibilityElement el : elems) {
                                if ("publicPackages".equals(el.getQName().getLocalPart())) {
                                    return el;
                                }
                            }
                        }
                        POMExtensibilityElement toRet = conf.getModel().getFactory().createPOMExtensibilityElement(POMQName.createQName("publicPackages"));
                        conf.addExtensibilityElement(toRet);
                        return toRet;
                    }

                    private POMExtensibilityElement findPublicPackagesElement(PluginContainer bld) {
                        POMExtensibilityElement publicPackages = null;
                        Plugin plug = bld.findPluginById("org.codehaus.mojo", "nbm-maven-plugin");
                        if (plug != null) {
                            Configuration conf = plug.getConfiguration();
                            if (conf != null) {
                                List<POMExtensibilityElement> elems = conf.getConfigurationElements();
                                if (elems != null) {
                                    for (POMExtensibilityElement el : elems) {
                                        if ("publicPackages".equals(el.getQName().getLocalPart())) {
                                            publicPackages = el;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        return publicPackages;
                    }
                }));
    }
    
}
