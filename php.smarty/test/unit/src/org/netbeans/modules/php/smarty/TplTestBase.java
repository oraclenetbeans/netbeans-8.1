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
package org.netbeans.modules.php.smarty;

import org.netbeans.api.html.lexer.HTMLTokenId;
import org.netbeans.junit.MockServices;
import org.netbeans.lib.lexer.test.TestLanguageProvider;
import org.netbeans.modules.csl.api.Formatter;
import org.netbeans.modules.csl.api.test.CslTestBase;
import org.netbeans.modules.csl.spi.DefaultLanguageConfig;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.masterfs.filebasedfs.FileBasedURLMapper;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.smarty.editor.TplDataLoader;
import org.netbeans.modules.php.smarty.editor.gsf.TplLanguage;
import org.netbeans.modules.php.smarty.editor.lexer.TplTopTokenId;
import org.netbeans.modules.php.smarty.ui.options.SmartyOptions;
import org.netbeans.modules.projectapi.SimpleFileOwnerQueryImplementation;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.test.MockLookup;

/**
 *
 * @author Martin Fousek <marfous@netbeans.org>
 */
public abstract class TplTestBase extends CslTestBase {

    public TplTestBase(String testName) {
        super(testName);
        MockLookup.setLookup(Lookups.singleton(new TestLanguageProvider()));
    }

    @Override
    protected void setUp() throws Exception {
        MockLookup.init();
        MockServices.setServices(new Class[] {FileBasedURLMapper.class});
        MockLookup.setInstances(
                new SimpleFileOwnerQueryImplementation(),
                new TestLanguageProvider());

        assert Lookup.getDefault().lookup(TestLanguageProvider.class) != null;

        try {
            TestLanguageProvider.register(HTMLTokenId.language());
            TestLanguageProvider.register(PHPTokenId.language());
            TestLanguageProvider.register(JsTokenId.javascriptLanguage());
            TestLanguageProvider.register(TplTopTokenId.language());
        } catch (IllegalStateException ise) {
            // Ignore -- we've already registered this either via layers or other means
        }
        resetSmartyOptions();
        super.setUp();
    }

    @Override
    protected DefaultLanguageConfig getPreferredLanguage() {
        return new TplLanguage();
    }

    @Override
    protected String getPreferredMimeType() {
        return TplDataLoader.MIME_TYPE;
    }

    @Override
    public Formatter getFormatter(IndentPrefs preferences) {
        return null;
    }

    protected void setupSmartyOptions(String openDelimiter, String closeDelimiter, SmartyFramework.Version version) {
        SmartyOptions.getInstance().setDefaultOpenDelimiter(openDelimiter);
        SmartyOptions.getInstance().setDefaultCloseDelimiter(closeDelimiter);
        SmartyOptions.getInstance().setSmartyVersion(version);
    }

    protected void resetSmartyOptions() {
        SmartyOptions.getInstance().setDefaultOpenDelimiter("{");
        SmartyOptions.getInstance().setDefaultCloseDelimiter("}");
        SmartyOptions.getInstance().setSmartyVersion(SmartyFramework.Version.SMARTY3);
    }

}
