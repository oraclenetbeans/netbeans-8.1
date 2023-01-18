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
package org.netbeans.lib.v8debug.commands;

import org.netbeans.lib.v8debug.PropertyBoolean;
import org.netbeans.lib.v8debug.PropertyLong;
import org.netbeans.lib.v8debug.V8Arguments;
import org.netbeans.lib.v8debug.V8Body;
import org.netbeans.lib.v8debug.V8Command;
import org.netbeans.lib.v8debug.V8Request;
import org.netbeans.lib.v8debug.V8Script;

/**
 *
 * @author Martin Entlicher
 */
public final class Scripts {
    
    private Scripts() {}
    
    public static V8Request createRequest(long sequence) {
        return new V8Request(sequence, V8Command.Scripts, null);
    }
    
    public static V8Request createRequest(long sequence, V8Script.Types types, Boolean includeSource) {
        return new V8Request(sequence, V8Command.Scripts, new Arguments(types, null, includeSource, null));
    }
    
    public static V8Request createRequest(long sequence, V8Script.Types types,
                                          long[] ids, Boolean includeSource,
                                          String nameFilter) {
        return new V8Request(sequence, V8Command.Scripts, new Arguments(types, ids, includeSource, nameFilter));
    }
    
    public static V8Request createRequest(long sequence, V8Script.Types types,
                                          long[] ids, Boolean includeSource,
                                          long idFilter) {
        return new V8Request(sequence, V8Command.Scripts, new Arguments(types, ids, includeSource, idFilter));
    }
    
    public static final class Arguments extends V8Arguments {
        
        private final V8Script.Types types;
        private final long[] ids;
        private final PropertyBoolean includeSource;
        private final String nameFilter;
        private final PropertyLong idFilter;
        
        public Arguments(V8Script.Types types, long[] ids, Boolean includeSource,
                         String nameFilter) {
            this.types = types;
            this.ids = ids;
            this.includeSource = new PropertyBoolean(includeSource);
            this.nameFilter = nameFilter;
            this.idFilter = new PropertyLong(null);
        }
        
        public Arguments(V8Script.Types types, long[] ids, Boolean includeSource,
                         long idFilter) {
            this.types = types;
            this.ids = ids;
            this.includeSource = new PropertyBoolean(includeSource);
            this.nameFilter = null;
            this.idFilter = new PropertyLong(idFilter);
        }

        public V8Script.Types getTypes() {
            return types;
        }

        public long[] getIds() {
            return ids;
        }

        public PropertyBoolean isIncludeSource() {
            return includeSource;
        }

        public String getNameFilter() {
            return nameFilter;
        }

        public PropertyLong getIdFilter() {
            return idFilter;
        }
    }
    
    public static final class ResponseBody extends V8Body {
        
        private final V8Script[] scripts;
        
        public ResponseBody(V8Script[] scripts) {
            this.scripts = scripts;
        }

        public V8Script[] getScripts() {
            return scripts;
        }
    }
    
}
