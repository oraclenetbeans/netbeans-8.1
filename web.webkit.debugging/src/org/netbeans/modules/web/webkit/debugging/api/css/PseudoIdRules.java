/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.webkit.debugging.api.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * CSS rule collection for a single pseudo style.
 *
 * @author Jan Stola
 */
public class PseudoIdRules {
    /** Pseudo style identifier. */
    private final int pseudoId;
    /** Rules applicable to the pseudo style. */
    private final List<Rule> rules;

    /**
     * Creates a new {@code PseudoIdRules} that corresponds to the given JSONObject.
     *
     * @param pseudoRules JSONObject describing the rulese.
     */
    PseudoIdRules(JSONObject pseudoRules) {
        pseudoId = ((Number)pseudoRules.get("pseudoId")).intValue(); // NOI18N
        JSONArray array = (JSONArray)pseudoRules.get("rules"); // NOI18N
        if (array == null) {
            // "rules" attribute has been renamed to "matches" recently
            array = (JSONArray)pseudoRules.get("matches"); // NOI18N
        }
        rules = new ArrayList<Rule>(array.size());
        for (Object o : array) {
            Rule rule = new Rule((JSONObject)o);
            rules.add(rule);
        }
    }

    /**
     * Returns the identifier of the pseudo style.
     *
     * @return identifier of the pseudo style.
     */
    public int getPseudoId() {
        return pseudoId;
    }

    /**
     * Returns the rules applicable to the pseudo style.
     *
     * @return rules applicable to the pseudo style.
     */
    public List<Rule> getRules() {
        return Collections.unmodifiableList(rules);
    }

}
