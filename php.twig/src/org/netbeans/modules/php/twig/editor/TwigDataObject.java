/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s): Sebastian H??rl
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.twig.editor;

import java.io.IOException;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import static org.netbeans.modules.php.twig.editor.TwigDataObject.ACTIONS;
import org.netbeans.modules.php.twig.editor.gsf.TwigLanguage;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject.Registration;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@Messages("TwigResolver=Twig Files")
@MIMEResolver.ExtensionRegistration(displayName = "#TwigResolver", position = 125, extension = "twig", mimeType = TwigLanguage.TWIG_MIME_TYPE)
@Registration(displayName = "TWIG", iconBase = "org/netbeans/modules/php/twig/resources/icon.png", mimeType = TwigLanguage.TWIG_MIME_TYPE)
@ActionReferences(value = {
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.OpenAction"), path = ACTIONS, position = 100, separatorAfter = 200),
    @ActionReference(id =
    @ActionID(category = "Edit", id = "org.openide.actions.CutAction"), path = ACTIONS, position = 300),
    @ActionReference(id =
    @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"), path = ACTIONS, position = 400, separatorAfter = 500),
    @ActionReference(id =
    @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"), path = ACTIONS, position = 600),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.RenameAction"), path = ACTIONS, position = 700, separatorAfter = 800),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"), path = ACTIONS, position = 900, separatorAfter = 1000),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"), path = ACTIONS, position = 1100, separatorAfter = 1200),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.ToolsAction"), path = ACTIONS, position = 1300),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"), path = ACTIONS, position = 1400)})
public class TwigDataObject extends MultiDataObject {

    static final String ACTIONS = "Loaders/" + TwigLanguage.TWIG_MIME_TYPE + "/Actions";

    public TwigDataObject(FileObject pf, MultiFileLoader loader) throws IOException {
        super(pf, loader);
        registerEditor(TwigLanguage.TWIG_MIME_TYPE, true);
    }

    @Override
    protected Node createNodeDelegate() {
        return new DataNode(this, Children.LEAF, getLookup());
    }

    @Messages("Source=&Source")
    @MultiViewElement.Registration(
            displayName = "#Source",
    iconBase = "org/netbeans/modules/php/twig/resources/icon.png",
    persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
    mimeType = TwigLanguage.TWIG_MIME_TYPE,
    preferredID = "php.twig.source",
    position = 1)
    public static MultiViewEditorElement createMultiViewEditorElement(Lookup context) {
        return new MultiViewEditorElement(context);
    }

    @Override
    protected int associateLookup() {
        return 1;
    }
}
