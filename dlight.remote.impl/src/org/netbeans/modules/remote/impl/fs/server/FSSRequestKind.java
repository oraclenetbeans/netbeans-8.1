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

package org.netbeans.modules.remote.impl.fs.server;

/** 
 * Request and response kind 
 */
 public enum FSSRequestKind {
    FS_REQ_LS('l'), 
    FS_REQ_RECURSIVE_LS('r'), 
    FS_REQ_STAT('S'), 
    FS_REQ_LSTAT('s'),
    FS_REQ_QUIT('q'),
    FS_REQ_SLEEP('P'),
    FS_REQ_ADD_WATCH('W'),
    FS_REQ_REMOVE_WATCH('w'),
    FS_REQ_REFRESH('R'),
    FS_REQ_DELETE('d'),
    FS_REQ_COPY('C'),
    FS_REQ_MOVE('m'),
    FS_REQ_SERVER_INFO('i'),
    FS_REQ_OPTION('o');

    private final char letter;

    private FSSRequestKind(char letter) {
        this.letter = letter;
    }

    public char getChar() {
        return letter;
    }
    
}
