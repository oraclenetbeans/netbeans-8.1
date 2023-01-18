/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javascript2.editor.api;

import org.netbeans.modules.javascript2.editor.model.DeclarationScope;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.model.impl.ModelUtils;

/**
 *
 * @author Petr Pisl
 */
public class FrameworksUtils {
    
    public static final String CATEGORY = "jsframeworks";       //NOI18N
    
    public static final String HTML5_CLIENT_PROJECT = "org.netbeans.modules.web.clientproject"; //NOI18N
    public static final String PHP_PROJECT = "org-netbeans-modules-php-project"; //NOI18N
    public static final String MAVEN_PROJECT = "org-netbeans-modules-maven";    //NOI18N

    /**
     * It change the declaration scope of the input object to the new scope. 
     * If the where object is not a function (Declaration Scope), then it's all the properties are
     * scanned recursively to change the declaration scope to the new one. It doesn't change the parents
     * of the objects, just the declaration scope. Usually is used, when you need wrap the object to the
     * new virtual function. 
     * @param where the object which is moved from one declaration scope to another one. 
     * @param newScope new declaration scope 
     */
    public static void changeDeclarationScope(JsObject where, DeclarationScope newScope) {
        ModelUtils.changeDeclarationScope(where, newScope);
    }
}
