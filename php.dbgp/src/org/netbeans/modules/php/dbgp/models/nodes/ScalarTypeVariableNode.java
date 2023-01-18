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
package org.netbeans.modules.php.dbgp.models.nodes;

import java.util.Set;
import org.netbeans.modules.php.dbgp.models.VariablesModelFilter.FilterType;
import org.netbeans.modules.php.dbgp.packets.Property;
import org.openide.util.NbBundle;

/**
 * @author ads
 *
 */
class ScalarTypeVariableNode extends org.netbeans.modules.php.dbgp.models.VariablesModel.AbstractVariableNode {
    private static final String TYPE_FLOAT = "TYPE_Float"; // NOI18N
    private static final String TYPE_INT = "TYPE_Int"; // NOI18N
    private static final String TYPE_BOOLEAN = "TYPE_Boolean"; // NOI18N
    private static final String TYPE_STRING = "TYPE_String"; // NOI18N
    private static final String TYPE_NULL = "TYPE_Null"; // NOI18N
    public static final String BOOLEAN = "boolean"; // NOI18N
    public static final String BOOL = "bool"; // NOI18N
    public static final String INTEGER = "integer"; // NOI18N
    public static final String INT = "int"; // NOI18N
    public static final String FLOAT = "float"; // NOI18N
    public static final String STRING = "string"; // NOI18N

    ScalarTypeVariableNode(Property property, AbstractModelNode parent) {
        super(property, parent);
    }

    @Override
    public String getType() {
        String type = super.getType();
        String bundleKey;
        switch (type) {
            case BOOLEAN:
            case BOOL:
                bundleKey = TYPE_BOOLEAN;
                break;
            case INTEGER:
            case INT:
                bundleKey = TYPE_INT;
                break;
            case FLOAT:
                bundleKey = TYPE_FLOAT;
                break;
            case STRING:
                bundleKey = TYPE_STRING;
                break;
            default:
                bundleKey = TYPE_NULL;
                break;
        }
        return NbBundle.getMessage(ScalarTypeVariableNode.class, bundleKey);
    }

    @Override
    protected boolean isTypeApplied(Set<FilterType> filters) {
        return filters.contains(FilterType.SCALARS);
    }

}
