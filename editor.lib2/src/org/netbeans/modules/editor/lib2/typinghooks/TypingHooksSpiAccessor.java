/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.editor.lib2.typinghooks;

import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import org.netbeans.spi.editor.typinghooks.CamelCaseInterceptor;
import org.netbeans.spi.editor.typinghooks.DeletedTextInterceptor;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;
import org.netbeans.spi.editor.typinghooks.TypedTextInterceptor;

/**
 *
 * @author vita
 */
public abstract class TypingHooksSpiAccessor {

    private static TypingHooksSpiAccessor ACCESSOR = null;

    public static synchronized void register(TypingHooksSpiAccessor accessor) {
        assert ACCESSOR == null : "Can't register two package accessors!"; //NOI18N
        ACCESSOR = accessor;
    }

    public static synchronized TypingHooksSpiAccessor get() {
        // Trying to wake up HighlightsLayer ...
        try {
            Class clazz = Class.forName(TypedTextInterceptor.MutableContext.class.getName());
        } catch (ClassNotFoundException e) {
            // ignore
        }

        assert ACCESSOR != null : "There is no package accessor available!"; //NOI18N
        return ACCESSOR;
    }

    /** Creates a new instance of HighlightingSpiPackageAccessor */
    protected TypingHooksSpiAccessor() {
    }

    public abstract TypedTextInterceptor.MutableContext createTtiContext(JTextComponent c, Position offset, String typedText, String replacedText);
    public abstract Object [] getTtiContextData(TypedTextInterceptor.MutableContext context);
    public abstract void resetTtiContextData(TypedTextInterceptor.MutableContext context);
    
    public abstract DeletedTextInterceptor.Context createDtiContext(JTextComponent c, int offset, String removedText, boolean backwardDelete);
    public abstract Object [] getDwiContextData(CamelCaseInterceptor.MutableContext context);
    public abstract CamelCaseInterceptor.MutableContext createDwiContext(JTextComponent c, int offset, boolean backwardDelete);
    
    public abstract TypedBreakInterceptor.MutableContext createTbiContext(JTextComponent c, int caretOffset, int insertBreakOffset);
    public abstract Object [] getTbiContextData(TypedBreakInterceptor.MutableContext context);
    public abstract void resetTbiContextData(TypedBreakInterceptor.MutableContext context);
}
