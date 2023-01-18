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
package org.netbeans.modules.ide.ergonomics.fod;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import org.junit.Assert;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.SubprojectProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service=ProjectFactory.class, position=29998)
public final class TestFactory extends ProjectOpenedHook
implements ProjectFactory, Project, ProjectInformation, SubprojectProvider, LogicalViewProvider {

    static Set<FileObject> recognize = new HashSet<FileObject>();
    static Set<Project> subprojects = new HashSet<Project>();
    static IOException ex;
    int closed;
    int opened;
    int listenerCount;
    final FileObject dir;

    public TestFactory() {
        dir = null;
    }

    private TestFactory(FileObject dir) {
        this.dir = dir;
    }

    public boolean isProject(FileObject projectDirectory) {
        return recognize.contains(projectDirectory);
    }

    public Project loadProject(FileObject pd, ProjectState state) throws IOException {
        IOException e = ex;
        if (e != null) {
            ex = null;
            throw e;
        }
        return isProject(pd) ? new TestFactory(pd) : null;
    }

    public void saveProject(Project project) throws IOException, ClassCastException {
    }

    public FileObject getProjectDirectory() {
        return dir;
    }

    public Lookup getLookup() {
        return Lookups.singleton(this);
    }

    public String getName() {
        return "x";
    }

    public String getDisplayName() {
        return "y";
    }

    public Icon getIcon() {
        return null;
    }

    public Project getProject() {
        return this;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listenerCount++;
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listenerCount--;
    }

    @Override
    protected void projectOpened() {
        opened++;
    }

    @Override
    protected void projectClosed() {
        closed++;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestFactory) {
            return super.equals(obj);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public Set<? extends Project> getSubprojects() {
        return subprojects;
    }

    public void addChangeListener(ChangeListener listener) {
    }

    public void removeChangeListener(ChangeListener listener) {
    }

    public Node createLogicalView() {
        AbstractNode an = new AbstractNode(new Children.Array());
        an.setName("xyz");
        an.setDisplayName("Name xyz");

        an.getChildren().add(new Node[]{ new AbstractNode(Children.LEAF), new AbstractNode(Children.LEAF) });
        an.getChildren().getNodeAt(0).setName("a");
        an.getChildren().getNodeAt(1).setName("b");
        return an;
    }

    public Node findPath(Node root, Object target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void assertListeners(String msg) {
        if (listenerCount == 0) {
            Assert.fail(msg);
        }
    }


}
