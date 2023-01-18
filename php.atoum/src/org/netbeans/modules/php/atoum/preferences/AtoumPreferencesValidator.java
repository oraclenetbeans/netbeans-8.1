/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.atoum.preferences;

import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.api.validation.ValidationResult;
import org.netbeans.modules.php.atoum.util.AtoumUtils;
import org.openide.util.NbBundle;

public final class AtoumPreferencesValidator {

    private final ValidationResult result = new ValidationResult();


    public ValidationResult getResult() {
        return result;
    }

    public AtoumPreferencesValidator validate(PhpModule phpModule) {
        validateBootstrap(AtoumPreferences.isBootstrapEnabled(phpModule), AtoumPreferences.getBootstrapPath(phpModule));
        validateConfiguration(AtoumPreferences.isConfigurationEnabled(phpModule), AtoumPreferences.getConfigurationPath(phpModule));
        validateAtoum(AtoumPreferences.isAtoumEnabled(phpModule), AtoumPreferences.getAtoumPath(phpModule));
        return this;
    }

    @NbBundle.Messages("AtoumPreferencesValidator.bootstrap.label=Bootstrap")
    public AtoumPreferencesValidator validateBootstrap(boolean bootstrapEnabled, String bootstrapPath) {
        validatePath(bootstrapEnabled, bootstrapPath, Bundle.AtoumPreferencesValidator_bootstrap_label(), "bootstrap.path"); // NOI18N
        return this;
    }

    @NbBundle.Messages("AtoumPreferencesValidator.configuration.label=Configuration")
    public AtoumPreferencesValidator validateConfiguration(boolean configurationEnabled, String configurationPath) {
        validatePath(configurationEnabled, configurationPath, Bundle.AtoumPreferencesValidator_configuration_label(), "configuration.path"); // NOI18N
        return this;
    }

    public AtoumPreferencesValidator validateAtoum(boolean atoumEnabled, String atoumPath) {
        if (atoumEnabled) {
            String warning = AtoumUtils.validateAtoumPath(atoumPath);
            if (warning != null) {
                result.addWarning(new ValidationResult.Message("atoum.path", warning)); // NOI18N
            }
        }
        return this;
    }

    private void validatePath(boolean pathEnabled, String path, String label, String source) {
        if (!pathEnabled) {
            return;
        }
        String warning = FileUtils.validateFile(label, path, false);
        if (warning != null) {
            result.addWarning(new ValidationResult.Message(source, warning));
        }
    }

}
