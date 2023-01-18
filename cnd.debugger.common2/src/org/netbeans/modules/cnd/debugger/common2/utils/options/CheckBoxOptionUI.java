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


/**
 * This class represents an option containing a check box.
 */

package org.netbeans.modules.cnd.debugger.common2.utils.options;

import java.awt.*;
import javax.swing.*;


class CheckBoxOptionUI extends OptionUI {

    JCheckBox   checkBox    = null;     // the cheak box holding the values

    public CheckBoxOptionUI(Option inOption) {
	super(inOption, false);
    }

 
    /**returns the value displayed on the UI
     */
    @Override
    protected String getValueFromUI() {
	return (checkBox.isSelected()) ? "on" : "off";    // NOI18N
    }


    /** update UI with currValue
     */
    @Override
    protected void updateUI(){
	if (currValue == null)
	    return;
	checkBox.setSelected(currValue.equals("on"));    // NOI18N
    }

    /** Add this option into the given panel */
    @Override
    public void addTo(JPanel parent) {
	GridBagConstraints gridBagConstraints = new GridBagConstraints();
	if (hasLabel) {
	    gridBagConstraints.gridwidth = 1;
	    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	    gridBagConstraints.insets = new java.awt.Insets(0, 12, 5, 12);

	    label = createLabel();
	    parent.add(label, gridBagConstraints);

	    gridBagConstraints = new GridBagConstraints();
	}

	gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
	gridBagConstraints.insets = new java.awt.Insets(0, 12, 5, 12);

	checkBox = new JCheckBox();
	checkBox.setText(optionType.getLabel()); // NOI18N
	checkBox.getAccessibleContext()
		.setAccessibleDescription(optionType.getOptionDescription());
	checkBox.setMnemonic(optionType.getMnemonic());
	
	updateUI();
	parent.add(checkBox, gridBagConstraints);

	if (hasLabel) {
	    label.setLabelFor(checkBox);
	}
    }
}
