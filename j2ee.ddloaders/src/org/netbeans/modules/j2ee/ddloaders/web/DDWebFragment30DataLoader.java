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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.j2ee.ddloaders.web;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;

/**
 * A data loader for web-fragment.xml version 3.0. Required for providing
 * a different action context than for older versions - see #85570.
 *
 * @author Petr Slechta
 */
public class DDWebFragment30DataLoader extends DDDataLoader {

    private static final long serialVersionUID = 1L;

    public static final String REQUIRED_MIME = "text/x-dd-servlet-fragment3.0"; // NOI18N
    public static final String REQUIRED_MIME_31 = "text/x-dd-servlet-fragment3.1"; // NOI18N

    public DDWebFragment30DataLoader() {
        super("org.netbeans.modules.j2ee.ddloaders.web.DDFragmentDataObject");  // NOI18N
    }

    @Override
    protected String actionsContext() {
        return "Loaders/text/x-dd-web-fragment3.0/Actions/"; // NOI18N
    }

    @Override
    protected String[] getSupportedMimeTypes() {
        return new String[]{REQUIRED_MIME, REQUIRED_MIME_31};
    }

    @Override
    protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException {
        return new DDFragmentDataObject(primaryFile, this, REQUIRED_MIME);
    }

}
