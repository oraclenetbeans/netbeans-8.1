/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */


package org.netbeans.modules.xml.text.dom;


import java.lang.ref.WeakReference;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.xml.lexer.XMLTokenId;


/**
 *
 * Instances are produced by {@link XMLSyntaxSupport}.
 * <p>
 * <b>Warning:</b> class is public only for private purposes!
 *
 * @author  Petr Nejedly - original HTML design
 * @author  Sandeep Randhawa - XML port
 * @author  Petr Kuzel - DOM Nodes
 *
 * @version 1.0
 */
public abstract class SyntaxElement {
    
    protected XMLSyntaxSupport support;
    private WeakReference<Token> first; //a weak reference to the fist TokenItem of this SE
    private WeakReference<SyntaxElement> previous;    // WR to the cached previous element
    private WeakReference<SyntaxElement> next;        // WR to the cached next element
    
    // let it be visible by static inner classes extending us
    protected int offset;     // original position in document
    protected int length;     // original lenght in document
    
    /** Creates new SyntaxElement */
    SyntaxElement(XMLSyntaxSupport support, Token<XMLTokenId> token, int start, int end)  {
        this.support = support;
        this.offset = start;
        this.length = end-start;
        this.first = new WeakReference(token);
    }
    
    /** returns an instance of first TokenItem of this SyntaxElement.
     * The instance is weakly held by this SyntaxElement instance, once
     * it is GC'ed, a new one is created using the offset of the original one.
     *
     * The WeakReference is used here because of a huge deep memory consumption of
     * a TokenItem-s under some circumstances (The SyntaxSupport chains the tokens
     * so it happens that each SyntaxElement instance holds it's own long
     * TokenItem-s chain.
     *
     * The current implementation lowers the CPU performance slightly, but
     * allows to GC the TokenItem-s chains if necessary.
     */
    protected Token first() {
        Token cached_first = first.get();
        if(cached_first != null)
            return cached_first;
        try {
            //it is a first token offset, so we shouldn't overlap the document length
            Token new_first = support.getNextToken(offset);
            first = new WeakReference(new_first);
            return new_first;
        } catch(BadLocationException e) {
            //fall through null
        }
        return null;
    }
    
    public int getElementOffset() {
        return offset;
    }
    
    public int getElementLength() {
        return length;
    }
    
    void setNext(SyntaxElement se) {
        next = new WeakReference(se);
    }
    
    void setPrevious(SyntaxElement se) {
        previous = new WeakReference(se);
    }
    
    /**
     * Get previous SyntaxElement. Weakly cache results.
     * @return previous SyntaxElement or <code>null</code> at document begining
     * or illegal location.
     */
    public SyntaxElement getPrevious() {
        SyntaxElement cached_previous = (previous == null) ? null : previous.get();
        if( cached_previous != null )
            return cached_previous;
        try {
            //we are on the beginning - no previous
            if (offset == 0) {
                return null;
            }
            //data not inialized yet or GC'ed already - we need to parse again
            SyntaxElement new_previous = support.getElementChain( getElementOffset() - 1 );
            if( new_previous != null ) {
                setPrevious(new_previous); //weakly cache the element
                new_previous.setNext(this);
                if (new_previous.offset == offset) {
                    return null;
                }
            }
            return new_previous;
        } catch (BadLocationException ex) {
            return null;
        }
    }
    
    /**
     * Get next SyntaxElement. Cache results.
     * @return next SyntaxElement or <code>null</code> at document end
     * or illegal location.
     */
    public SyntaxElement getNext() {
        SyntaxElement cached_next = next == null ? null : next.get();
        if( cached_next != null )
            return cached_next;
        try {
            //data not inialized yet or GC'ed already - we need to parse again
            SyntaxElement new_next = support.getElementChain( offset+length);
            if( new_next != null ) {
                setNext(new_next); //weakly cache the element
                new_next.setPrevious(this);
                if (new_next.offset == offset) {
                    return null;
                }
            }
            return new_next;
        } catch (BadLocationException ex) {
            return null;
        }
    }
    
    /**
     * Print element content for debug purposes.
     */
    public String toString() {
        String content = "?";
        return "SyntaxElement [offset=" + offset + "; length=" + length + " ;type = " + this.getClass().getName() + "; content:" + content +"]";
    }
    
    /**
     *
     */
    public int hashCode() {
        return super.hashCode() ^ offset ^ length;
    }
    
    /**
     * DOM Node equals. It's not compatible with Object's equals specs!
     */
    public boolean equals(Object obj) {
        if (obj instanceof SyntaxElement) {
            if (((SyntaxElement)obj).offset == offset) return true;
        }
        return false;
    }
        
    /**
     * It may stop some DOM traversing.  //!!!
     */
    public static class Error extends SyntaxElement {
        public Error( XMLSyntaxSupport support, Token first, int start, int end ) {
            super( support, first, start, end);
        }

        public String toString() {
            return "Error" + super.toString();                                  // NOI18N
        }
    }
    
}
