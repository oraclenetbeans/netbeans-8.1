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
package org.netbeans.modules.javascript2.editor.model.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.javascript2.editor.model.DeclarationScope;
import org.netbeans.modules.javascript2.editor.model.Identifier;
import org.netbeans.modules.javascript2.editor.model.JsFunction;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.model.TypeUsage;

/**
 *
 * @author Petr Pisl
 */
public class JsFunctionReference extends JsObjectReference implements JsFunction {
    
    private final JsFunction original;
    
    public JsFunctionReference(JsObject parent, Identifier declarationName,
            JsFunction original, boolean isDeclared, Set<Modifier> modifiers) {
        super(parent, declarationName, original, isDeclared, modifiers);
        this.original = original;
    }

    @Override
    public JsFunction getOriginal() {
        return this.original;
    }
    
    @Override
    public Collection<? extends JsObject> getParameters() {
        return original.getParameters();
    }

    @Override
    public JsObject getParameter(String name) {
        return original.getParameter(name);
    }

    @Override
    public void addReturnType(TypeUsage type) {
        original.addReturnType(type);
    }

    @Override
    public Collection<? extends TypeUsage> getReturnTypes() {
        return original.getReturnTypes();
    }

    @Override
    public Collection<? extends DeclarationScope> getChildrenScopes() {
        return original.getChildrenScopes();
    }

    @Override
    public DeclarationScope getParentScope() {
        return original.getParentScope();
    }

    @Override
    public void addDeclaredScope(DeclarationScope scope) {
        original.addDeclaredScope(scope);
    }
    
}
