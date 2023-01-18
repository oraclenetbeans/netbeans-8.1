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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.lib.profiler.results.cpu.cct.nodes;

import org.netbeans.lib.profiler.results.RuntimeCCTNode;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class BaseCPUCCTNode implements RuntimeCPUCCTNode {
    
    private static final RuntimeCCTNode[] EMPTY_CHILDREN = new RuntimeCCTNode[0];    
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    /** Children nodes in the RuntimeCPUCCTNode tree. This field can have three different values depending on the
     * number of children:
     *   null if there are no children
     *   instance of RuntimeCPUCCTNode if there is exactly one child
     *   instance of RuntimeCPUCCTNode[] if there are multiple children
     * This is purely a memory consumption optimization, which typically saves about 50% of memory, since a lot of
     * RuntimeCPUCCTNode trees are a sequence of single-child nodes, and in such case we remove the need to 
     * create a one-item array.
     */
    private Object children;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of BaseCPUCCTNode */
    public BaseCPUCCTNode() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public RuntimeCCTNode[] getChildren() {
        if (children == null) {
            return EMPTY_CHILDREN;
        } else if (children instanceof RuntimeCPUCCTNode) {
            return new RuntimeCPUCCTNode[]{(RuntimeCPUCCTNode)children};
        }
        return (RuntimeCPUCCTNode[])children;
    }

    public void attachNodeAsChild(RuntimeCPUCCTNode node) {
        if (children == null) {
            children = node;
        } else if (children instanceof RuntimeCPUCCTNode) {
            children = new RuntimeCPUCCTNode[]{(RuntimeCPUCCTNode)children,node};
        } else {
            RuntimeCPUCCTNode[] ch = (RuntimeCPUCCTNode[]) children;
            RuntimeCPUCCTNode[] newChildren = new RuntimeCPUCCTNode[ch.length+1];
            System.arraycopy(ch, 0, newChildren, 0, ch.length);
            newChildren[newChildren.length-1] = node;
            children = newChildren;
        }
    }
}
