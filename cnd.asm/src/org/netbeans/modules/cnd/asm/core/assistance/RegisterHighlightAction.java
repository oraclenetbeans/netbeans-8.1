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


package org.netbeans.modules.cnd.asm.core.assistance;

import org.netbeans.modules.cnd.asm.model.AsmState;
import org.netbeans.modules.cnd.asm.model.lang.AsmElement;
import org.netbeans.modules.cnd.asm.model.lang.AsmOffsetable;
import org.netbeans.modules.cnd.asm.model.lang.OperandElement;
import org.netbeans.modules.cnd.asm.model.lang.Register;
import org.netbeans.modules.cnd.asm.model.lang.RegisterElement;
import org.netbeans.modules.cnd.asm.model.util.DefaultOffsetable;
import org.netbeans.modules.cnd.asm.model.util.AsmModelUtilities;
import org.netbeans.modules.cnd.asm.model.util.IntervalSet;

public class RegisterHighlightAction {
                 
    public IntervalSet<HighlightEntry> getHighlight(AsmState state, int pos) {
        final AsmElement root = state.getElements();
        final AsmElement res = AsmModelUtilities.findAtRecursive(root, pos);
        final IntervalSet<HighlightEntry> list = new IntervalSet<HighlightEntry>();
                
        if (res != null && res instanceof RegisterElement) {
            final Register reg  = ((RegisterElement) res).getRegister();                         
                                    
            AsmModelUtilities.AsmVisitor vis = new AsmModelUtilities.AsmVisitor() {
                 public boolean visit(AsmElement comp) {
                     if (comp instanceof RegisterElement && comp != res) {
                         Register t_reg = ((RegisterElement) comp).getRegister();
                         if (reg.getChildren().contains(t_reg) || reg == t_reg || 
                             t_reg.getChildren().contains(reg)) {
                             list.add(new HighlightEntry(comp, ((OperandElement) comp).getUsage()));                                                       
                         }
                     }
                     return true;
                 }
            };
            
            AsmModelUtilities.walkCompound(root, vis);                                                   
        }
        
        return list;
    }
    
    public static class HighlightEntry extends DefaultOffsetable {
        private final OperandElement.Usage usage;

        HighlightEntry(AsmOffsetable off, OperandElement.Usage usage) {
            super(off);
            this.usage = usage;
        }
        
        public OperandElement.Usage getUsage() {
            return usage;
        }
    }
}
