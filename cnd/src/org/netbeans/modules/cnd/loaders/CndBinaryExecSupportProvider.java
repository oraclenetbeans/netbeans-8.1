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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.loaders;

import org.netbeans.modules.cnd.execution.BinaryExecSupport;
import org.netbeans.modules.cnd.source.spi.CndCookieProvider;
import org.netbeans.modules.cnd.utils.MIMENames;
import org.openide.loaders.DataObject;
import org.openide.loaders.MultiDataObject;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.InstanceContent.Convertor;

/**
 * TODO: Is it needed class? All binaries data objects create binary support in constructors.
 * Source data objects do not need binary support.
 * Remove class or migrate binaries data objects from CookieSet to Lookup.
 *
 * @author Alexey Vladykin
 */
public final class CndBinaryExecSupportProvider extends CndCookieProvider {
    static final BinaryExecSupportFactory staticFactory = new BinaryExecSupportFactory();

    @Override
    public void addLookup(DataObject dao, InstanceContent ic) {
        MultiDataObject mdao = (MultiDataObject) dao;
        if (!MIMENames.isFortranOrHeaderOrCppOrC(dao.getPrimaryFile().getMIMEType())){
            ic.add(mdao, staticFactory);
        }
    }

    private static class BinaryExecSupportFactory implements Convertor<MultiDataObject, BinaryExecSupport> {

        public BinaryExecSupportFactory() {
        }

        @Override
        public BinaryExecSupport convert(MultiDataObject obj) {
            return new BinaryExecSupport(obj.getPrimaryEntry());
        }

        @Override
        public Class<? extends BinaryExecSupport> type(MultiDataObject obj) {
            return BinaryExecSupport.class;
        }

        @Override
        public String id(MultiDataObject obj) {
            return BinaryExecSupport.class.getName()+obj.getPrimaryFile().getPath();
        }

        @Override
        public String displayName(MultiDataObject obj) {
            return id(obj);
        }
    }
}
