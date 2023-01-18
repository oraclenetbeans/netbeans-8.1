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
package org.netbeans.modules.php.nette2;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.netbeans.modules.php.api.framework.BadgeIcon;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.phpmodule.PhpModuleProperties;
import org.netbeans.modules.php.nette2.annotations.Nette2AnnotationsProvider;
import org.netbeans.modules.php.nette2.preferences.Nette2Preferences;
import org.netbeans.modules.php.nette2.ui.actions.Nette2PhpModuleActionsExtender;
import org.netbeans.modules.php.nette2.ui.customizer.Nette2CustomizerExtender;
import org.netbeans.modules.php.nette2.utils.Constants;
import org.netbeans.modules.php.spi.annotation.AnnotationCompletionTagProvider;
import org.netbeans.modules.php.spi.framework.PhpFrameworkProvider;
import org.netbeans.modules.php.spi.framework.PhpModuleActionsExtender;
import org.netbeans.modules.php.spi.framework.PhpModuleCustomizerExtender;
import org.netbeans.modules.php.spi.framework.PhpModuleExtender;
import org.netbeans.modules.php.spi.framework.PhpModuleIgnoredFilesExtender;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommandSupport;
import org.netbeans.modules.php.spi.phpmodule.ImportantFilesImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class Nette2FrameworkProvider extends PhpFrameworkProvider {
    private static final Nette2FrameworkProvider INSTANCE = new Nette2FrameworkProvider();
    private final BadgeIcon badgeIcon;

    @PhpFrameworkProvider.Registration(position = 190)
    public static Nette2FrameworkProvider getInstance() {
        return INSTANCE;
    }

    @NbBundle.Messages({
        "LBL_FrameworkName=Nette2 PHP Web Framework",
        "LBL_FrameworkDescription=Nette2 PHP Web Framework"
    })
    private Nette2FrameworkProvider() {
        super("Nette2 PHP Web Framework", Bundle.LBL_FrameworkName(), Bundle.LBL_FrameworkDescription()); //NOI18N
        badgeIcon = new BadgeIcon(
                ImageUtilities.loadImage(Constants.ICON_PATH),
                Nette2FrameworkProvider.class.getResource("/" + Constants.ICON_PATH)); //NOI18N
    }

    @Override
    public boolean isInPhpModule(PhpModule phpModule) {
        boolean result = Nette2Preferences.isManuallyEnabled(phpModule);
        if (!result) {
            FileObject sourceDirectory = phpModule.getSourceDirectory();
            if (sourceDirectory != null) {
                FileObject bootstrap = getFileObject(sourceDirectory, Constants.COMMON_BOOTSTRAP_PATH);
                result = bootstrap != null && !bootstrap.isFolder() && bootstrap.isValid();
                FileObject config = getFileObject(sourceDirectory, Constants.COMMON_CONFIG_PATH);
                result = result && config != null && config.isFolder() && config.isValid();
            }
        }
        return result;
    }

    @Override
    public ImportantFilesImplementation getConfigurationFiles2(PhpModule phpModule) {
        return new ConfigurationFiles(phpModule);
    }

    @Override
    public PhpModuleExtender createPhpModuleExtender(PhpModule phpModule) {
        return new Nette2PhpModuleExtender();
    }

    @Override
    public PhpModuleProperties getPhpModuleProperties(PhpModule phpModule) {
        return new PhpModuleProperties();
    }

    @Override
    public PhpModuleActionsExtender getActionsExtender(PhpModule phpModule) {
        return new Nette2PhpModuleActionsExtender();
    }

    @Override
    public PhpModuleIgnoredFilesExtender getIgnoredFilesExtender(PhpModule phpModule) {
        return new PhpModuleIgnoredFilesExtender() {
            @Override
            public Set<File> getIgnoredFiles() {
                return Collections.<File>emptySet();
            }
        };
    }

    @Override
    public FrameworkCommandSupport getFrameworkCommandSupport(PhpModule phpModule) {
        return null;
    }

    @Override
    public BadgeIcon getBadgeIcon() {
        return badgeIcon;
    }

    @Override
    public List<AnnotationCompletionTagProvider> getAnnotationsCompletionTagProviders(PhpModule phpModule) {
        return Collections.<AnnotationCompletionTagProvider>singletonList(new Nette2AnnotationsProvider());
    }

    @Override
    public PhpModuleCustomizerExtender createPhpModuleCustomizerExtender(PhpModule phpModule) {
        return new Nette2CustomizerExtender(phpModule);
    }

    /**
     * Try to get a FileObject with correct filename case. See bug 238679.
     *
     * @param parent Parent FileObject.
     * @param relPath Relative path, separated by slashes.
     */
    private FileObject getFileObject(FileObject parent, String relPath) {
        File parentFile = FileUtil.toFile(parent);
        if (parentFile != null) {
            String nativePath = relPath.replace('/', File.separatorChar);
            File file = new File(parentFile, nativePath);
            return FileUtil.toFileObject(FileUtil.normalizeFile(file));
        } else {
            return null;
        }
    }
}
