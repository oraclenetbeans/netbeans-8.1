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
package org.netbeans.modules.selenium2.webclient.mocha.preferences;

import java.io.File;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.web.common.api.ExternalExecutableValidator;
import org.netbeans.modules.web.common.api.ValidationResult;
import org.openide.util.NbBundle;

/**
 *
 * @author Theofanis Oikonomou
 */
public class MochaPreferencesValidator {

    private final ValidationResult result = new ValidationResult();


    public ValidationResult getResult() {
        return result;
    }

    @NbBundle.Messages("MochaPreferencesValidator.mocha.name=Mocha")
    public MochaPreferencesValidator validateMochaInstallFolder(String mochaInstallFolder) {
        String warning = validateMochaExec(Bundle.MochaPreferencesValidator_mocha_name(), mochaInstallFolder, false);
        if (warning != null) {
            result.addWarning(new ValidationResult.Message("path", warning)); // NOI18N
        }
        return this;
    }
    
    @NbBundle.Messages({
        "# {0} - source",
        "ExternalExecutableValidator.validateFile.missing={0} install location must be specified.",
        "# {0} - source",
        "ExternalExecutableValidator.validateFile.notAbsolute={0} install location must be an absolute path.",
        "# {0} - source",
        "ExternalExecutableValidator.validateFile.notFile={0} install location is invalid (\"./bin/mocha\" executable could not be located).",
        "# {0} - source",
        "ExternalExecutableValidator.validateFile.notReadable={0} is not readable.",
        "# {0} - source",
        "ExternalExecutableValidator.validateFile.notWritable={0} is not writable."
    })
    @CheckForNull
    private static String validateMochaExec(String source, String filePath, boolean writable) {
        if (filePath == null
                || filePath.trim().isEmpty()) {
            return Bundle.ExternalExecutableValidator_validateFile_missing(source);
        }

        File file = new File(filePath + "/bin/mocha");
        if (!file.isAbsolute()) {
            return Bundle.ExternalExecutableValidator_validateFile_notAbsolute(source);
        } else if (!file.isFile()) {
            return Bundle.ExternalExecutableValidator_validateFile_notFile(source);
        } else if (!file.canRead()) {
            return Bundle.ExternalExecutableValidator_validateFile_notReadable(source);
        } else if (writable && !file.canWrite()) {
            return Bundle.ExternalExecutableValidator_validateFile_notWritable(source);
        }
        return null;
    }
    
}
