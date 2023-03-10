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
 * Contributor(s): theanuradha@netbeans.org
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven.repository;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.modules.maven.indexer.api.RepositoryIndexer;
import org.netbeans.modules.maven.indexer.api.RepositoryInfo;
import org.netbeans.modules.maven.indexer.api.RepositoryPreferences;
import static org.netbeans.modules.maven.repository.Bundle.*;
import org.netbeans.modules.maven.repository.register.RepositoryRegisterUI;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.actions.DeleteAction;
import org.openide.actions.PropertiesAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Anuradha G
 */
public class RepositoryNode extends AbstractNode {

    private static final @StaticResource String LOCAL_REPO = "org/netbeans/modules/maven/repository/localrepo.png";
    private static final @StaticResource String REMOTE_REPO = "org/netbeans/modules/maven/repository/remoterepo.png";

    private RepositoryInfo info;
    private final GroupListChildren children;

    private static final RequestProcessor RPrefreshindex = new RequestProcessor(RefreshIndexAction.class.getName(),1);

    public RepositoryNode(RepositoryInfo info) {
        this(info, new GroupListChildren(info));
    }
    private RepositoryNode(RepositoryInfo info, GroupListChildren children) {
        super(Children.create(children, true));
        this.info = info;
        this.children = children;
        setName(info.getId());
        setDisplayName(info.getName());
    }

    @Override
    public Image getIcon(int arg0) {
        if (info.isRemoteDownloadable()) {
            return ImageUtilities.loadImage(REMOTE_REPO, true);
        }
        return ImageUtilities.loadImage(LOCAL_REPO, true);
    }

    @Override
    public Image getOpenedIcon(int arg0) {
        return getIcon(arg0);
    }

    @Override
    public String getHtmlDisplayName() {
        StringBuilder base = new StringBuilder().append(getDisplayName());
        if (info.isMirror()) {
            base.append(" <font color='!controlShadow'>[");
            for (RepositoryInfo nf : info.getMirroredRepositories()) {
                base.append(nf.getName()).append(",");
            }
            base.setLength(base.length() - 1);
            base.append("]</font>");
        }
        return base.toString();
    }

    @Messages({
        "#{0} - repository id",
        "LBL_REPO_ID=Repository ID:<b> {0} </b><p>",
        "#{0} - repository name",
        "LBL_REPO_Name=Repository Name: <b> {0} </b><p>",
        "#{0} - repository url",
        "LBL_REPO_Url=Repository URL:<b> {0} </b><p>",
        "LBL_ORIGIN=Repository Origin:<b> {0} </b><p>",
        "LBL_ORIGIN_PERSISTENT=User defined",
        "LBL_ORIGIN_TRANSIENT=Added by the IDE",
        "LBL_Mirrors=Mirrors repositories: <b> {0} </b><p>"
    })
    @Override public String getShortDescription() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("<html>");//NOI18N

        buffer.append(LBL_REPO_ID(info.getId()));

        buffer.append(LBL_REPO_Name(info.getName()));

        //show repo url if available
        if (info.getRepositoryUrl() != null) {
            buffer.append(LBL_REPO_Url(info.getRepositoryUrl()));
        }
        buffer.append(LBL_ORIGIN(
                RepositoryPreferences.getInstance().isPersistent(info.getId()) ? 
                LBL_ORIGIN_PERSISTENT() : 
                LBL_ORIGIN_TRANSIENT()));
        if (info.isMirror()) {
            StringBuilder s = new StringBuilder();
            for (RepositoryInfo nf : info.getMirroredRepositories()) {
                s.append(nf.getName()).append(",");
            }
            s.setLength(s.length() - 1);
            buffer.append(LBL_Mirrors(s.toString()));
        }
        
        buffer.append("</html>");//NOI18N

        return buffer.toString();
    }

    @Override
    public void destroy() throws IOException {
        RepositoryPreferences.getInstance().removeRepositoryInfo(info);
        super.destroy();
    }

    @Override
    public boolean canDestroy() {
        return RepositoryPreferences.getInstance().isPersistent(info.getId());
    }

    @Override
    public Action[] getActions(boolean arg0) {
        return new Action[]{
            // XXX Find
            new RefreshIndexAction(), // XXX allow multiselections
            new EditAction(),
            DeleteAction.get(DeleteAction.class),
            null,
            PropertiesAction.get(PropertiesAction.class)
        };
    }
    
    @Messages({
        "LBL_Id=ID",
        "LBL_Name=Name",
        "LBL_Local=Local",
        "LBL_Local_repository_path=Local repository path",
        "LBL_Remote_Index=Remote Index Downloadable",
        "LBL_Remote_URL=Remote Repository URL",
        "LBL_Remote_Index_URL=Remote Index URL",
        "LBL_last_indexed=Last Indexed"
    })
    @Override protected Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set basicProps = sheet.get(Sheet.PROPERTIES);
        try {
            Node.Property<?> id = new PropertySupport.Reflection<String>(info, String.class, "getId", null); //NOI18N
            id.setDisplayName(LBL_Id());
            id.setShortDescription(""); //NOI18N
            Node.Property<?> name = new PropertySupport.Reflection<String>(info, String.class, "getName", null); //NOI18N
            name.setDisplayName(LBL_Name());
            name.setShortDescription(""); //NOI18N
            Node.Property<?> local = new PropertySupport.Reflection<Boolean>(info, Boolean.TYPE, "isLocal", null); //NOI18N
            local.setName("local"); //NOI18N
            local.setDisplayName(LBL_Local());
            local.setShortDescription("");
            Node.Property<?> localRepoLocation = new PropertySupport.Reflection<String>(info, String.class, "getRepositoryPath", null); //NOI18N
            localRepoLocation.setDisplayName(LBL_Local_repository_path());
            Node.Property<?> remoteDownloadable = new PropertySupport.Reflection<Boolean>(info, Boolean.TYPE, "isRemoteDownloadable", null); //NOI18N
            remoteDownloadable.setDisplayName(LBL_Remote_Index());
            Node.Property<?> repoURL = new PropertySupport.Reflection<String>(info, String.class, "getRepositoryUrl", null); //NOI18N
            repoURL.setDisplayName(LBL_Remote_URL());
            Node.Property<?> indexURL = new PropertySupport.Reflection<String>(info, String.class, "getIndexUpdateUrl", null); //NOI18N
            indexURL.setDisplayName(LBL_Remote_Index_URL());
            Node.Property<?> lastIndexed = new PropertySupport.ReadOnly<Date>("lastIndexed", Date.class, LBL_last_indexed(), null) {
                @Override public Date getValue() throws IllegalAccessException, InvocationTargetException {
                    return RepositoryPreferences.getLastIndexUpdate(info.getId());
                }
            };
            basicProps.put(new Node.Property<?>[] {
                id, name, local, localRepoLocation, remoteDownloadable, repoURL, indexURL, lastIndexed
            });
        } catch (NoSuchMethodException exc) {
            exc.printStackTrace();
        }
        return sheet;
    }

    public class RefreshIndexAction extends AbstractAction {
        
        @Messages("LBL_REPO_Update_Index=Update Index")
        public RefreshIndexAction() {
            putValue(NAME, LBL_REPO_Update_Index());
        }

        public @Override void actionPerformed(ActionEvent e) {
            setEnabled(false); // XXX ineffective if in context menu
            RPrefreshindex.post(new Runnable() {
                public @Override void run() {
                    RepositoryIndexer.indexRepo(info);
                }
            });
        }
    }
    
    private class EditAction extends AbstractAction {

        @Messages("ACT_Edit=Edit...")
        EditAction() {
            putValue(NAME, ACT_Edit());
        }

        @Override public boolean isEnabled() {
            return RepositoryPreferences.getInstance().isPersistent(info.getId()) && info.isRemoteDownloadable();
        }

        @Messages("LBL_Edit_Repo=Edit Repository")
        @Override public void actionPerformed(ActionEvent e) {
            final RepositoryRegisterUI rrui = new RepositoryRegisterUI(RepositoryNode.this.info);
            rrui.getAccessibleContext().setAccessibleDescription(LBL_Edit_Repo());
            DialogDescriptor dd = new DialogDescriptor(rrui, LBL_Edit_Repo());
            dd.setClosingOptions(new Object[]{
                        rrui.getButton(),
                        DialogDescriptor.CANCEL_OPTION
                    });
            dd.setOptions(new Object[]{
                        rrui.getButton(),
                        DialogDescriptor.CANCEL_OPTION
                    });
            Object ret = DialogDisplayer.getDefault().notify(dd);
            if (rrui.getButton() == ret) {
                RepositoryInfo info;
                try {
                    info = rrui.getRepositoryInfo();
                } catch (URISyntaxException x) {
                    DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(x.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE));
                    return;
                }
                RepositoryPreferences.getInstance().addOrModifyRepositoryInfo(info);
                RepositoryNode.this.info = info;
                setDisplayName(info.getName());
                fireIconChange();
                fireOpenedIconChange();
                children.setInfo(info);
            }

        }
    }
}
