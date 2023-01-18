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
package org.netbeans.modules.maven.model.settings.impl;

import javax.xml.namespace.QName;
import org.netbeans.modules.maven.model.pom.POMQName;
import org.netbeans.modules.xml.xam.dom.Attribute;

/**
 *
 * @author mkleint
 */
public enum ProfilesAttribute implements Attribute {
        NS("xmlns"),            // NOI18N // TMP
        ;
    
    private String name;
    private Class type;
    private Class subtype;
    
    /**
     * Creates a new instance of POMAttribute
     */
    ProfilesAttribute(String name) {
        this(name, String.class);
    }
    
    ProfilesAttribute(String name, Class type) {
        this(name, type, null);
    }
    
    ProfilesAttribute(String name, Class type, Class subtype) {
        this.name = name;
        this.type = type;
        this.subtype = subtype;
    }
    
    @Override
    public String toString() { 
        return name; 
    }

    @Override
    public Class getType() {
        return type;
    }

    @Override
    public String getName() { 
        return name; 
    }

    @Override
    public Class getMemberType() { 
        return subtype; 
    }
    
    public QName getQName() {
        return new QName(POMQName.NS_URI, name);
    }
}
