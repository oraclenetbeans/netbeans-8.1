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

package org.netbeans.modules.cnd.debugger.common2.utils.options;

import java.lang.IllegalArgumentException;

import java.beans.PropertyEditorSupport;

import org.openide.ErrorManager;

/**
 * Editor for enums, but also plain text or enums+text.
 */

class OptionEnumEditor extends PropertyEditorSupport {
    OptionPropertySupport ops;
    public OptionEnumEditor(OptionPropertySupport ops) {
	this.ops = ops;
    }

    @Override
    public void setAsText(String text) {

	// text = text.trim();	Not all text should be blindly trimmed!

	Validity v = ops.getValidity(text);
	if (!v.isValid()) {
	    IllegalArgumentException e = new IllegalArgumentException(v.why());
	    // THe following will make it appear as a nice error dialog.
	    ErrorManager.getDefault().annotate(e,
					       ErrorManager.USER,
					       v.why(),
					       v.why(),
					       null,
					       null);
	    throw e;
	}

	setValue(text); // from PropertyEditorSupport
    }

    @Override
    public String getAsText() {
	return (String) getValue();
        //return ops.getValue();
    }

    @Override
    public String[] getTags() {
	/* example of what to return.
	String[] tags = new String[4];
	tags[0] = "8";
	tags[1] = "16";
	tags[2] = "32";
	tags[3] = "automatic";
	return tags;
	*/
	return ops.getValues();
    }
}
