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
package org.netbeans.modules.html.editor.lib.api.foreign;

import java.io.IOException;
import java.io.Reader;


public class CharSequenceReader extends Reader {

    protected CharSequence source;
    protected int length;
    protected int next = 0;
    private int mark = 0;
    
    public CharSequenceReader(CharSequence immutableCharSequence) {
        this.source = immutableCharSequence;
        this.length = source.length();
    }

    @Override
    public int read() throws IOException {
        synchronized (lock) {
            if (next >= length) {
                return -1;
            }
            char c = source.charAt(next++);
            return processReadChar(c);
        }
    }
    
    protected char processReadChar(char c) throws IOException {
        return c;
    }

     public int read(char cbuf[], int off, int len) throws IOException {
	synchronized (lock) {
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
	    if (next >= length)
		return -1;
	    int n = Math.min(length - next, len);
            for(int i = 0; i < n; i++) {
//                cbuf[i + off] = source.charAt(next + i);
                cbuf[i + off] = (char)read();
            }
//	    next += n;
	    return n;
	}
    }

    @Override
    public long skip(long ns) throws IOException {
        synchronized (lock) {
            if (next >= length) {
                return 0;
            }
            // Bound skip by beginning and end of the source
            long n = Math.min(length - next, ns);
            n = Math.max(-next, n);
            next += n;
                        
            return n;
        }
    }

    @Override
    public boolean ready() throws IOException {
        synchronized (lock) {
            return true;
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }
    
    protected void markedAt(int mark) {
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        if (readAheadLimit < 0) {
            throw new IllegalArgumentException("Read-ahead limit < 0");
        }
        synchronized (lock) {
            mark = next;
            markedAt(mark);
        }
    }

    protected void inputReset() {
    }
    
    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            next = mark;
            inputReset();
        }
    }

    @Override
    public void close() {
    }
    
}