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
package org.netbeans.modules.xml.axi.visitor;

import java.util.Stack;
import org.netbeans.modules.xml.axi.AXIComponent;
import org.netbeans.modules.xml.axi.AXIComponent.ComponentType;
import org.netbeans.modules.xml.axi.AXIDocument;
import org.netbeans.modules.xml.axi.AnyAttribute;
import org.netbeans.modules.xml.axi.AnyElement;
import org.netbeans.modules.xml.axi.Attribute;
import org.netbeans.modules.xml.axi.Compositor;
import org.netbeans.modules.xml.axi.Element;
import org.netbeans.modules.xml.axi.ContentModel;
import org.netbeans.modules.xml.axi.impl.ElementRef;

/**
 *
 * @author Samaresh (Samaresh.Panda@Sun.Com)
 */
public class DeepAXITreeVisitor extends DefaultVisitor {
        
    Stack<AXIComponent> pathToRoot = new Stack<AXIComponent>();
    
    /**
     * Creates a new instance of DeepAXITreeVisitor
     */
    public DeepAXITreeVisitor() {
        super();
    }
    
    public void visit(AXIDocument root) {
        visitChildren(root);
    }
    
    public void visit(Element element) {        
        visitChildren(element);
    }
    
    public void visit(AnyElement element) {
        visitChildren(element);
    }
    
    public void visit(Attribute attribute) {        
        visitChildren(attribute);
    }
        
    public void visit(AnyAttribute attribute) {
        visitChildren(attribute);
    }
    
    public void visit(Compositor compositor) {        
        visitChildren(compositor);
    }
            
    public void visit(ContentModel element) {
        visitChildren(element);
    }
        
    protected void visitChildren(AXIComponent component) {
        if( !canVisit(component) )
            return;
                
        pathToRoot.push(component.getOriginal());
        for(AXIComponent child: component.getChildren()) {
            child.accept(this);
        }
        pathToRoot.pop();
    }
        
    protected boolean canVisit(AXIComponent component) {        
        if(pathToRoot.contains(component))
            return false;
        
        if(component.getComponentType() == ComponentType.PROXY)
            return canVisit(component.getOriginal());

        if(component.getComponentType() == ComponentType.REFERENCE &&
           component instanceof ElementRef) {
            ElementRef ref = (ElementRef)component;
            Element e = ref.getReferent();
            if(pathToRoot.contains(e))
                return false;
        }
        
        return true;
    }
    
}
