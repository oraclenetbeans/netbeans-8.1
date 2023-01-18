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
 * Software is Sun Microsystems, Inc.
 *
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */

package org.netbeans.api.templates;

import java.util.Map;

/** This is an interface for <q>smart templating</q>.
 * Implementations of this class can be registered in the global {@link org.openide.util.Lookup}
 * and allows anyone provide additional parameters to each {@link CreateFromTemplateHandler}s
 * when a template is instantiating.
 * <p/>
 * Implementations are called in the order of appearance in Lookup. The positions less than 0 are
 * reserved for the platform. Implementations called later can see and override
 * values defined by earlier CreateFromTemplateAttributes.
 * <p/>
 * Read more in the <a href="@TOP@/architecture-summary.html#script">howto document</a>.
 * <p/>
 * This interface supersedes {@code CreateFromTemplateAttributesProvider} in {@code openide.loaders} module.
 * 
 * @author Svata Dedic
 */
public interface CreateFromTemplateAttributes {
    /** Called when a template is about to be instantiated to provide additional
     * values to the {@link CreateFromTemplateHandler} that will handle the 
     * template instantiation.
     * <p/>
     * If the returned Map defines the same value as some {@link CreateFromTemplateAttributes} registered
     * earlier, the Map's value takes precedence. Parameters supplied by the {@link FileBuilder} cannot be
     * overriden.
     * 
     * @param desc the creation request
     * @return map of named objects, or null
     */
    Map<String,?> attributesFor(CreateDescriptor desc);
}
