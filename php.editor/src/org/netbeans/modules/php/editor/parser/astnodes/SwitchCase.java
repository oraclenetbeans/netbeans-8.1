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
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.parser.astnodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a case statement.
 * A case statement is part of switch statement
 * <pre>e.g.<pre>
 * case expr:
 *   statement1;
 *   break;,
 *
 * default:
 *   statement2;
 */
public class SwitchCase extends Statement {

    private Expression value;
    private ArrayList<Statement> actions = new ArrayList<>();
    private boolean isDefault;

    public SwitchCase(int start, int end, Expression value, Statement[] actions, boolean isDefault) {
        super(start, end);

        if (actions == null) {
            throw new IllegalArgumentException();
        }

        this.value = value;
        this.actions.addAll(Arrays.asList(actions));
        this.isDefault = isDefault;
    }

    public SwitchCase(int start, int end, Expression value, List<Statement> actions, boolean isDefault) {
        this(start, end, value,
                actions == null ? null : (Statement[]) actions.toArray(new Statement[actions.size()]),
                isDefault);
    }

    /**
     * The actions of this case statement
     * @return List of actions of this case statement
     */
    public List<Statement> getActions() {
        return this.actions;
    }

    /**
     * True if this is a default case statement
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * The value (expression) of this case statement
     * @return value (expression) of this case statement
     */
    public Expression getValue() {
        return value;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Statement statement : getActions()) {
            sb.append(statement).append(";"); //NOI18N
        }
        return "case " + getValue() + ":" + sb.toString(); //NOI18N
    }

}
