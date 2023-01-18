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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.cnd.debugger.gdb2;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 *
 * @author Egor Ushakov
 */
public class GdbUtils {
    private GdbUtils() {
    }
    
    public static String gdbToUserEncoding(String string, final String encoding) {
        // The first part transforms string to byte array
        char[] chars = string.toCharArray();
        char next;
        boolean escape = false;
        ArrayList<Byte> _bytes = new ArrayList<Byte>();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            next = (i + 1) < chars.length ? chars[i + 1] : 0;
            if (escape) {
                // skip escaped char
                escape = false;
            } else if (ch == '\\') {
                if (Character.isDigit(next)) {
                    char[] charVal = {chars[++i], chars[++i], chars[++i]};
                    ch = (char) Integer.parseInt(String.valueOf(charVal), 8);
                } else {
                    escape = true;
                }
            }
            _bytes.add((byte) ch);
        }
        byte[] bytes = new byte[_bytes.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = _bytes.get(i);
        }

        // The second part performs encoding to current coding system
        try {
            string = new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
        }
        return string;
    }
    
    public static double parseVersionString(String msg) throws NumberFormatException {
        int dot = msg.indexOf('.');

        int first = dot - 1;
        while (first > 0 && Character.isDigit(msg.charAt(first))) {
            first--;
        }

        int last = dot + 1;
        while (last < msg.length() && Character.isDigit(msg.charAt(last))) {
            last++;
        }
        return Double.parseDouble(msg.substring(first+1, last));
    }
}
