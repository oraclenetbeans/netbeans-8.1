/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.cnd.debugger.common2.debugger;

import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.cnd.api.lexer.CndTokenUtilities;
import org.netbeans.cnd.api.lexer.CppTokenId;
import org.netbeans.cnd.api.lexer.TokenItem;
import org.netbeans.modules.cnd.debugger.common2.debugger.options.DebuggerOption;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;

import org.openide.ErrorManager;
import org.openide.loaders.DataObject;
import org.openide.cookies.EditorCookie;
import org.openide.text.Annotation;
import org.openide.text.Line.Part;
import org.openide.text.Line;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 * This class helps with Balloon evaluation and is associated with editor
 * mime types in
 * "../resources/mf-layer.xml"
 *
 * See also NB'
 * ant/src/org/netbeans/modules/debugger/projects/ToolTipAnnotation.java
 * in the debuggerjpda module.
 */
public final class EvalAnnotation extends Annotation {
    private final RequestProcessor RP = new RequestProcessor("Debugger tooltip evaluation", 2); //NOI18N

    // interface Annotation
    @Override
    public String getAnnotationType() {
        // By returning null we announce that we're a tooltip as opposed
        // to an annotation.
        return null;
    }

    /**
     * Called to get the contents of the tooltip.
     *
     * Despite appearances this works asynchronously too.
     * We return null and the caller listens on a property change for
     * PROP_SHORT_DESCRIPTION.
     */

    // interface Annotation
    @Override
    public String getShortDescription() {
        final Part lp = (Part) getAttachedAnnotatable();

        if (lp == null) {
            return null;
        }
        
        RP.post(new Runnable() {
            @Override
            public void run() {
                evalExpression(lp);
            }
        });

        return null;
    }

    private void evalExpression(final Part lp) {
        try {
            Line line = lp.getLine();
            Lookup lineLookup = line.getLookup();
            DataObject dobj = lineLookup.lookup(DataObject.class);

            final EditorCookie ec = dobj.getCookie(EditorCookie.class);
            final StyledDocument doc = ec.openDocument();

            JEditorPane ep = EditorContextDispatcher.getDefault().getCurrentEditor();
            if (ep == null) {
                return;
            }

            int pos = lp.getColumn();
            final int offset = NbDocument.findLineOffset(doc, line.getLineNumber()) + pos;
            
            // 6630840
            String expr = getSelectedExpr(ep, offset);
            if (expr == null && DebuggerOption.BALLOON_EVAL.isEnabled(NativeDebuggerManager.get().globalOptions())) {
                Element lineElem =
                    NbDocument.findLineRootElement(doc).
                    getElement(line.getLineNumber());

                if (lineElem == null) {
                    return;
                }
                // not selected case
                int lineStartOffset = lineElem.getStartOffset();
                int lineLen = lineElem.getEndOffset() - lineStartOffset;
                expr = doc.getText(lineStartOffset, lineLen);
                
                // do not evaluate comments etc. (see 166207)
                final AtomicBoolean skip = new AtomicBoolean(false);

                doc.render(new Runnable() {
                    @Override
                    public void run() {
                        TokenItem<TokenId> token = CndTokenUtilities.getToken(doc, offset, true);
                        if (token != null) {
                            String category = token.id().primaryCategory();
                            if (!CppTokenId.IDENTIFIER_CATEGORY.equals(category)) {
                                skip.set(true);
                            }
                        }
                    }
                });

                if (skip.get()) {
                    return;
                }
            } else {
                // selected expression
                // expr has the expression that user wants to evaluate
                // "-1" is a hint to the engine to skip it's own parsing which
		// I may do by calling back to EvalAnnotation.extractExpression()
                pos = -1;
            }

            NativeDebugger debugger = NativeDebuggerManager.get().currentNativeDebugger();
            if (debugger != null) {
                lastAnnotation = this;
                debugger.balloonEvaluate(pos, expr);
            }
        } catch (IOException e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
        } catch (BadLocationException e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
        }
    }

    /*
     *  return 
     *	    expr : user has selected expression specifically
     *      null : user has hovered cursor above expression. Engine can take
     *             care of parsing on it's own (dbx can handle C/C++/Fortran) or
     *             call back to EvalAnnotation.extractExpression().
     */
    private static String getSelectedExpr(JEditorPane ep, int offset) {
        //see bz#248407, looks like editorpane is not displayable and no caret is set which will lead to NPE here
        if ((ep.getCaret() != null) && (ep.getSelectionStart() <= offset) && (offset <= ep.getSelectionEnd())) {
            return ep.getSelectedText();
        }
	return null;
    }


    /*
     *  text is the context of whole line
     *  pos is the int pointer of the cursor relative to the 
     *      beginning of the line
     *  e.g.
     *      text =  "   aa = a.b.c;"
     *                         ^
     *      pos = 11              
     *
     *  called from GdbDebuggerImpl
     *
     */
    public static String extractExpr(int pos, String text) {
        int bp = pos; // beginning of the exp
        int ep = pos; // end of the expr
        int len = text.length();

        if (pos >= len) {
            return null; // pointed outside the current line
        }
        char[] str = new char[len + 1];   // leave room for '\0'
        text.getChars(0, len, str, 0);
        str[len] = 0;

        // Search forwards. Accept all alpha numeric characters, _, and
        // array indexing (provided it's balanced)
        int bbalance = 0; // Balance of brackets []
        int pbalance = 0; // Balance of parentheses ()
        boolean foundEnd = false; // Found the boundary of the expression
        for (; !foundEnd && str[ep] != 0; ep++) {
            if (Character.isLetterOrDigit(str[ep]) || str[ep] == '_') {
                continue; // C token
            }
            switch (str[ep]) {
                case '[':
                    bbalance++;
                    break;
                case ']':
                    if (str[ep] == ']') {
                        bbalance--;
                        if (bbalance < 0) {
                            foundEnd = true;
                            break;
                        }
                    }
                    break;
                case '*': // Allow *'s: for example in "foo[*bar]"
                    break;
                case ':': // Foo::bar
                    // pass only scope members, see IZ 206740
                    if (str[ep+1] != ':') {
                        foundEnd = true;
                    } else {
                        ep++;
                    }
                    break;
                default:
                    foundEnd = true;
                    break;
            }
            if (foundEnd) {
                break;      // To prevent e++ in loop iteration
            }
        }
        // Search backwards. Just like forwards, but also accept function calls ()
        // and dereferencing ->, and look for a cast in front of the expression
        bbalance = 0;
        pbalance = 0;
        foundEnd = false;
        // for (; !foundEnd && (bp <= pos); bp--) {
        for (; !foundEnd && (bp >= 0); bp--) {
            if (Character.isLetterOrDigit(str[bp]) || str[bp] == '_') {
                continue; // C identifier
            }
            switch (str[bp]) {
                case ')':
                    // Special case: I need to see if this is a
                    // function call (in which case I proceed as
                    // usual) or a cast.  If it's a cast I want to
                    // find the matching parenthesis and stop
                    // (since a cast can contain stuff that I
                    // don't otherwise allow, like *, whitespace,
                    // etc.)  This allows me to point at "(char
                    if ((bp < ep) && (Character.isLetterOrDigit(str[bp + 1]) || str[bp + 1] == '_')) {
                        // It's a cast
                        pbalance = 0;
                        while (bp >= pos) {
                            if (str[bp] == ')') {
                                pbalance++;
                            } else if (str[bp] == '(') {
                                pbalance--;
                                if (pbalance == 0) {
                                    // found beginning of cast
                                    // compensate for b++ below
                                    bp--;
                                    break;
                                }
                            }
                            bp--;
                        }
                        foundEnd = true;
                        break;
                    } else {
                        // It's a function call
                        pbalance++;
                    }
                    break;
                case ']':
                    bbalance++;
                    break;
                case '(':
                    pbalance--;
                    if (pbalance < 0) {
                        foundEnd = true;
                        break;
                    }
                    break;
                case '[':
                    bbalance--;
                    if (bbalance < 0) {
                        foundEnd = true;
                        break;
                    }
                    break;
                case '>':
                    // for example "foo->bar"
                    if ((bp == pos) || (str[bp - 1] != '-')) {
                        foundEnd = true;
                        break;
                    } else {
                        bp--; // skip over whole ->
                    }
                    break;
                case '.': // for example "foo.bar"
                case 0:   // empty string: when you point at the end of a expr
                case ':': // for example "Foo::bar"
                    break;
                case '&':
                case '*':
                    // What do we do about an expression like
                    // foo = &bar; ? Does the user want to evaluate
                    // "bar" or "&bar" ???
                    // For now let's assume the user wants "bar"
                    // For *, we have the same issue.
                    // It's not easy to decide which is right.
                    // (1) char *foo = "hello"
                    // (2) bar = *llist
                    // In (1) you want "foo", in (2) you want "*llist".
                    // But since we have a gesture for *eval (hit control),
                    // let's go with behavior 1 for now.
                    foundEnd = true;
                    break;
                default:
                    foundEnd = true;
                    break;
            }
            if (foundEnd) {
                break;
            } // To prevent bp-- in loop iteration
        }

        bp++; // Skip the delimiter we just found
        String result = "";
        if (bp >= ep) {
            return null;
        }

        while (bp < ep) {
            result += str[bp++];
        }

        return result;
    }
    private static EvalAnnotation lastAnnotation = null;

    public static void postResult(int rt1, int rt2, int flags,
            String lhs, String rhs,
            String rhs2, String rhs3) {
        if (lhs == null) {
            postResult(rhs);
        } else if (rhs2 == null && rhs3 == null) {
            // Plain lhs=rhs tooltip
            postResult(lhs + " = " + rhs); // NOI18N

        /*
        Disabled because the editor's tooltip support doesn't
        position multiline tooltips correctly. (Tor, can you
        fix that?)
         */
        } else {
            StringBuffer sb = new StringBuffer(200);

            sb.append(lhs);
            sb.append(' ');
            sb.append('=');
            sb.append(' ');
            sb.append(rhs);

            if (rhs2 != null) {
                sb.append("\n");	 // NOI18N
                sb.append("type: "); // NOI18N
                sb.append(rhs2);
            }

            /* the tooltip doesn't honor HTML
            sb.append("<HTML>"); // NOI18N
            sb.append("<FONT SIZE=-2>");
            sb.append(lhs);
            sb.append(' ');
            sb.append('=');
            sb.append(' ');
            sb.append(rhs);
            if (rhs2 != null) {
            sb.append("<br>"); // NOI18N
            sb.append("type: ");
            sb.append(rhs2);
            }
            if ((rhs3 != null) && (rhs3.length() > 0)) {
            sb.append("<br>"); // NOI18N
            sb.append('*');
            sb.append(lhs);
            sb.append(' ');
            sb.append('=');
            sb.append(' ');
            sb.append(rhs3);
            }
            sb.append("</HTML>"); // NOI18N
             */
            postResult(sb.toString());
        }
    }

    public static void postResult(String tipText) {
        if (lastAnnotation != null) {
            lastAnnotation.firePropertyChange(PROP_SHORT_DESCRIPTION,
                    null, tipText);
            // See bug 207390
            // If we make it null futher notifications will not appear
            //lastAnnotation = null;
        }
    }
}
