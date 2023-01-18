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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.cnd.script.loaders;


import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.filesystems.FileObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.loaders.DataObjectExistsException;
import org.openide.nodes.Node;

import org.netbeans.modules.cnd.execution.ShellExecSupport;
import org.netbeans.modules.cnd.utils.MIMENames;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.MultiDataObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/**
 *  Represents a Shell object in the Repository.
 */
@Messages("ShellResolver=Common Shell Extensions")
@MIMEResolver.Registration(displayName="#ShellResolver", position=139, resource="../resources/mime-resolver.xml")
public class ShellDataObject extends MultiDataObject {

    /** Serial version number */
    static final long serialVersionUID = -5853234372530618782L;

    /** Constructor for this class */
    public ShellDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException {
        super(pf, loader);

        registerEditor(MIMENames.SHELL_MIME_TYPE, true);
        getCookieSet().add(new ShellExecSupport(getPrimaryEntry()));
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @Messages("Source=&Source")
    @MultiViewElement.Registration(
        displayName="#Source", // NOI18N
        iconBase="org/netbeans/modules/cnd/script/resources/ShellDataIcon.gif", // NOI18N
        persistenceType=TopComponent.PERSISTENCE_ONLY_OPENED,
        mimeType=MIMENames.SHELL_MIME_TYPE,
        preferredID="shell.source", // NOI18N
        position=1
    )
    public static MultiViewEditorElement createMultiViewEditorElement(Lookup context) {
        return new MultiViewEditorElement(context);
    }

    @Override
    protected Node createNodeDelegate() {
        return new ShellDataNode(this);
    }
}
