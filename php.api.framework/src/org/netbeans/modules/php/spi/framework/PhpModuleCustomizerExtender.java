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

package org.netbeans.modules.php.spi.framework;

import java.util.EnumSet;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.openide.util.HelpCtx;

/**
 * Provides support for extending a PHP module properties (via Project Properties dialog).
 * For Reading and storing properties, {@link PhpModule#getPreferences(Class, boolean)} can be used.
 *
 * @author Tomas Mysik
 */
public abstract class PhpModuleCustomizerExtender {

    /**
     * This class is used to notify about changes in the direction from frameworks to PHP module.
     * @see PhpModuleCustomizerExtender#save(PhpModule)
     * @since 0.3
     */
    public enum Change {
        /**
         * Directory with source files changed.
         */
        SOURCES_CHANGE,
        /**
         * Directory with test files changed.
         */
        TESTS_CHANGE,
        /**
         * Directory with Selenium files changed.
         */
        SELENIUM_CHANGE,
        /**
         * Ignored files changed.
         * @see org.netbeans.modules.php.spi.phpmodule.PhpModuleIgnoredFilesExtender
         */
        IGNORED_FILES_CHANGE,
        /**
         * Framework has been added or removed.
         */
        FRAMEWORK_CHANGE,
    }

    /**
     * Returns the display name of this extender. This method
     * is meant to return a shorter name then
     * {@link PhpFramewor Provider#getName()} (which is used if {@code null} is returned).
     * @return display name of the category, can be {@code null}.
     */
    public abstract String getDisplayName();

    /**
     * Attaches a change listener that is to be notified of changes
     * in the extender (e.g., the result of the {@link #isValid} method
     * has changed.
     *
     * @param  listener a listener.
     */
    public abstract void addChangeListener(ChangeListener listener);

    /**
     * Removes a change listener.
     *
     * @param  listener a listener.
     */
    public abstract void removeChangeListener(ChangeListener listener);

    /**
     * Returns a UI component used to allow the user to customize this extender.
     *
     * @return a component that provides configuration UI.
     *         This method might be called more than once and it is expected to always
     *         return the same instance.
     */
    public abstract JComponent getComponent();

    /**
     * Returns a help context for {@link #getComponent}.
     *
     * @return a help context; can be <code>null</code>.
     */
    public abstract HelpCtx getHelp();

    /**
     * Checks if this extender is valid (e.g., if the configuration set
     * using the UI component returned by {@link #getComponent} is valid).
     * <p>
     * If it returns <code>false</code>, check {@link #getErrorMessage() error message}, it
     * should not be <code>null</code>.
     *
     * @return <code>true</code> if the configuration is valid, <code>false</code> otherwise.
     * @see #getErrorMessage()
     * @see #getWarningMessage()
     */
    public abstract boolean isValid();

    /**
     * Get error message or <code>null</code> if the {@link #getComponent component} is {@link #isValid() valid}.
     * @return error message or <code>null</code> if the {@link #getComponent component} is {@link #isValid() valid}
     * @see #isValid()
     * @see #getWarningMessage()
     */
    public abstract String getErrorMessage();

    /**
     * Get warning message that can be not <code>null</code> even for {@link #isValid() valid} extender.
     * In other words, it is safe to customize PHP module even if this method returns a message.
     * @return warning message or <code>null</code>
     * @see #isValid()
     * @see #getErrorMessage()
     */
    public String getWarningMessage() {
        return null;
    }

    /**
     * Called to extend properties of the given PHP module. This method
     * is called in a background thread and only if user clicks on the OK button;
     * also, it cannot be called if {@link #isValid()} is <code>false</code>.
     * <p>
     * <b>Please notice that this method is called under project write lock
     * so it should finish as fast as possible.</b>
     * <p>
     * There are 2 situations that can happen (also simultaneously):
     * <ol>
     *  <li><i>a {@link Change}</i> - in such case the code must run in the method itself (it
     *      must finish before the {@link Change} is returned), or</li>
     *  <li><i>not a {@link Change}</i> - in this case, if it is a long-running task (e.g. sending e-mail, connecting to a remote server),
     *      it is recommended to create {@link org.openide.util.RequestProcessor} and run the code in it; the return value is {@code null}.</li>
     * </ol>
     *
     * @param phpModule the PHP module which properties are to be extended; never <code>null</code>
     * @return set of {@link Change changes} that happened in the PHP module; can be {@code null} if no such change happened (more information in method description)
     * @see #isValid()
     * @see Change
     * @see PhpModule#getPreferences(Class, boolean)
     */
    public abstract EnumSet<Change> save(PhpModule phpModule);
}
