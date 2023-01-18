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

import org.openide.ErrorManager;

import org.xml.sax.Attributes;
import org.netbeans.modules.cnd.api.xml.*;

public class OptionSetXMLCodec extends XMLDecoder implements XMLEncoder {

    static private final String TAG_OPTION = "option"; // NOI18N

    static private final String ATTR_OPTION_NAME = "name"; // NOI18N
    static private final String ATTR_OPTION_VALUE = "value"; // NOI18N


    private OptionSet optionSet;

    public OptionSetXMLCodec(OptionSet optionSet) {
	this.optionSet = optionSet;
    }

    // interface XMLDecoder
    @Override
    public String tag() {
	return optionSet.tag();
    }

    // interface XMLDecoder
    @Override
    public void start(Attributes atts) throws VersionException {
	String what = optionSet.description();
	int maxVersion = 1;
	checkVersion(atts, what, maxVersion);
    }

    // interface XMLDecoder
    @Override
    public void end() {
    }

    // interface XMLDecoder
    @Override
    public void startElement(String element, Attributes atts) {
	if (TAG_OPTION.equals(element)) {
	    String name = atts.getValue(ATTR_OPTION_NAME);
	    OptionValue o = optionSet.byName(name);
	    if (o != null) {
		String value = atts.getValue(ATTR_OPTION_VALUE);
		o.setInitialValue(value);
	    } else {
		ErrorManager.getDefault().log("Warning: unknown option " + name); // NOI18N
	    }
	} 
    }

    // interface XMLDecoder
    @Override
    public void endElement(String element, String currentText) {
    }

    // interface XMLEncoder
    @Override
    public void encode(XMLEncoderStream xes) {
	xes.elementOpen(tag());
	    for (int ox = 0; ox < optionSet.values().size(); ox++) {
		OptionValue o = optionSet.values().get(ox);
		writeOption(xes, o);
	    }
	xes.elementClose(tag());
    }

    private void writeOption(XMLEncoderStream xes, OptionValue o) {
	// was: OptionValue.perhapsToXML()
	if (!o.type().persist(o))
	    return;
	String xmlval = o.get();
	String defaultval = o.getDefaultValue();
        String option_name = o.type().getName();
        if (!option_name.equals("stack_max_size") && !xmlval.equals(defaultval)) { // NOI18N
	    AttrValuePair optionAttrs[] = new AttrValuePair[] {
		new AttrValuePair(ATTR_OPTION_NAME, option_name),
		new AttrValuePair(ATTR_OPTION_VALUE, xmlval)
	    };
	    xes.element(TAG_OPTION, optionAttrs);
	}
    }
}
