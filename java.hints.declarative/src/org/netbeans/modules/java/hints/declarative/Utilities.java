/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2012 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2012 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints.declarative;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.java.hints.declarative.Condition.Instanceof;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jan Lahoda
 */
public class Utilities {

    public static String readFile(FileObject file) {
        StringBuilder sb = new StringBuilder();

        Reader r = null;

        try {
            r = new InputStreamReader(file.getInputStream(), "UTF-8");

            int read;

            while ((read = r.read()) != (-1)) {
                sb.append((char) read);
            }

            return sb.toString();
        } catch (IOException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.FINE, null, ex);
            return null;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Logger.getLogger(Utilities.class.getName()).log(Level.FINE, null, ex);
                }
            }
        }
    }
    
    public static Map<String, String> conditions2Constraints(List<Condition> conditions) {
        Map<String, String> constraints = new HashMap<String, String>();

        for (Condition c : conditions) {
            if (!(c instanceof Instanceof) || c.not)
                continue;

            Instanceof i = (Instanceof) c;

            constraints.put(i.variable, i.constraint.trim()); //TODO: may i.constraint contain comments? if so, they need to be removed
        }
        
        return constraints;
    }

}
