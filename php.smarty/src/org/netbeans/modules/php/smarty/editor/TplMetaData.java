/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
package org.netbeans.modules.php.smarty.editor;

import org.netbeans.modules.php.smarty.SmartyFramework;

/** Holds data relevant to the TPL coloring for one TPL page.
 *
 * @author Martin Fousek
 */
public final class TplMetaData {

    private String openDelimiter, closeDelimiter;
    private SmartyFramework.Version version;
    private boolean initialized;

    public TplMetaData(String openDelimiter, String closeDelimiter, SmartyFramework.Version version) {
        this.openDelimiter = openDelimiter;
        this.closeDelimiter = closeDelimiter;
        this.version = version;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean initialized() {
        boolean oldVal = initialized;
        this.initialized = true;
        return oldVal;
    }

    /** Updates coloring data. The update is initiated by saving project properties. */
    public void updateMetaData(String openDelimiter, String closeDelimiter) {
        this.openDelimiter = openDelimiter;
        this.closeDelimiter = closeDelimiter;
    }

    public String getOpenDelimiter() {
        return openDelimiter;
    }

    public String getCloseDelimiter() {
        return closeDelimiter;
    }

    public SmartyFramework.Version getSmartyVersion() {
        return version;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("TplMetaData:");
        buf.append(" openDelim=");
        buf.append(getOpenDelimiter());
        buf.append("; closeDelim=");
        buf.append(getCloseDelimiter());
        buf.append(')');

        return buf.toString();
    }
}
