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
package org.netbeans.modules.cnd.makeproject.ui.options;

import org.netbeans.modules.cnd.utils.ui.NamedOption;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Alexander Simon
 */
@ServiceProvider(path=NamedOption.MAKE_PROJECT_CATEGORY, service=NamedOption.class, position=2000)
public class ResolveSymbolicLinks extends NamedOption {
    // Reuse
    public static final String RESOLVE_SYMBOLIC_LINKS = "resolveSymbolicLinks";  // NOI18N
    
    @Override
    public String getName() {
        return RESOLVE_SYMBOLIC_LINKS;
    }

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(ReuseOutputTab.class, "RESOLVE_SYMBOLIC_LINKS_CHECKBOX_TXT"); //NOI18N
    }

    @Override
    public String getDescription() {
        return NbBundle.getMessage(ReuseOutputTab.class, "RESOLVE_SYMBOLIC_LINKS_CHECKBOX_AD"); //NOI18N
    }

    @Override
    public NamedOption.OptionKind getKind() {
        return NamedOption.OptionKind.Boolean;
    }

    @Override
    public Object getDefaultValue() {
        return Boolean.getBoolean("cnd.project.resolve.symlinks");
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
