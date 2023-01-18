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

package org.netbeans.modules.xml.text.completion;

import java.util.*;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.xml.lexer.XMLTokenId;

import org.w3c.dom.*;

import org.netbeans.editor.*;
import org.openide.ErrorManager;

import org.netbeans.modules.xml.text.syntax.*;
import org.netbeans.modules.xml.api.model.*;
import org.netbeans.modules.xml.spi.dom.UOException;
import org.netbeans.modules.xml.text.bracematch.XMLBraceMatcher;
import org.netbeans.modules.xml.text.syntax.dom.SyntaxNode;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.openide.util.NbBundle;

/**
 * Consults grammar and presents list of possible choices
 * in particular document context.
 * <p>
 * <b>Warning:</b> It is public for unit test purposes only!
 *
 * @author Petr Nejedly
 * @author Sandeep Randhawa
 * @author Petr Kuzel
 * @author asgeir@dimonsoftware.com
 * @version 1.01
 */

public class XMLCompletionQuery implements XMLTokenIDs {
    
    // the name of a property indentifing cached query
    public static final String DOCUMENT_GRAMMAR_BINDING_PROP = "doc-bind-query";
    
    /**
     * Perform the query on the given component. The query usually
     * gets the component's document, the caret position and searches back
     * to examine surrounding context. Then it returns the result.
     * <p>
     * It is also called after every keystroke while opened completion
     * popup. So some result cache could be used. It is not easy at
     * this level because of BACKSPACE that can extend result.
     *
     * @param component the component to use in this query.
     * @param offset position in the component's document to which the query will
     *   be performed. Usually it's a caret position.
     * @param support syntax-support that will be used during resolving of the query.
     * @return result of the query or null if there's no result.
     */
    public List<CompletionItem> query(JTextComponent component, int offset, SyntaxSupport support) {        
        BaseDocument doc = (BaseDocument)component.getDocument();
        if (doc == null) return null;
        XMLSyntaxSupport sup = (XMLSyntaxSupport)support.get(XMLSyntaxSupport.class);
        if( sup == null ) return null;// No SyntaxSupport for us, no hint for user        
        try {
            SyntaxQueryHelper helper = new SyntaxQueryHelper(sup, offset);
            
            //supposing that there wont be more cc items result sets in one moment hence
            //using the static field, otherwise the substitute offset would have to be set to all CC items
            XMLResultItem.substituteOffset = helper.getOffset() - helper.getEraseCount();
            
            // completion request originates from area covered by DOM,
            if (helper.getCompletionType() != SyntaxQueryHelper.COMPLETION_TYPE_DTD) {
                List<CompletionItem> all = new ArrayList<CompletionItem>();
                List<CompletionItem> list = null;
                switch (helper.getCompletionType()) {
                    case SyntaxQueryHelper.COMPLETION_TYPE_ATTRIBUTE:
                        list = queryAttributes(helper, doc, sup);
                        break;
                    case SyntaxQueryHelper.COMPLETION_TYPE_VALUE:
                        list = queryValues(helper, doc, sup);
                        break;
                    case SyntaxQueryHelper.COMPLETION_TYPE_ELEMENT:
                        list = queryElements(helper, doc, sup);
                        break;
                    case SyntaxQueryHelper.COMPLETION_TYPE_ENTITY:
                        list = queryEntities(helper, doc, sup);
                        break;
                    case SyntaxQueryHelper.COMPLETION_TYPE_NOTATION:
                        list = queryNotations(helper, doc, sup);
                    case SyntaxQueryHelper.COMPLETION_TYPE_UNKNOWN:
                        return null; //do not show the CC
                }
                
                if(list == null) {
                    return null;
                }

                /***************************************************************
                 * The functionality "Inserting of an appropriate closing end tag"
                 * has been moved to the module "xml.schema.completion". The method
                 * "org.netbeans.modules.xml.schema.completion.CompletionQuery.query(...)"
                 * creates (if it's necessary) an instance of the class CompletionResultItem,
                 * related to a closing end tag.
                 * (CompletionResultItem endTagResultItem = CompletionUtil.getEndTagCompletionItem(...))
                 ***************************************************************
                if (helper.getCompletionType() == SyntaxQueryHelper.COMPLETION_TYPE_VALUE) {
                    //might be the end tag autocompletion
                    if (helper.getToken().getTokenID() == XMLDefaultTokenContext.TAG) {
                    SyntaxElement se = helper.getSyntaxElement();
                        if (se instanceof StartTag) {
                            String tagName = ((StartTag)se).getNodeName();
                            if (tagName != null && !XMLBraceMatcher.hasEndTag(doc,
                                offset, tagName))
                                list.add(new EndTagAutocompletionResultItem(tagName));
                        }
                    }
                }
                ***************************************************************/
                
                if (list.isEmpty() && helper.getPreText().startsWith("</")) { // NOI18N
                    List stlist = findStartTag((SyntaxNode)helper.getSyntaxElement(), !helper.getPreText().endsWith("/") ? "/" : "");
                    if (stlist != null && !stlist.isEmpty()) {
                        ElementResultItem item = (ElementResultItem)stlist.get(0); //we always get just one item
                        if(!XMLBraceMatcher.hasEndTag(doc, offset, item.getItemText()) &&
                           (!item.getItemText().startsWith("/") ||
                            item.getItemText().startsWith(helper.getPreText().substring(1)))) {
                            String title = NbBundle.getMessage(XMLCompletionQuery.class, "MSG_result", helper.getPreText());
                             //TODOxxxreturn new XMLCompletionResult(component, title,stlist, helper.getOffset(), 0);
                        }
                    }
                }
                
                String debugMsg = Boolean.getBoolean("netbeans.debug.xml") ? " " + helper.getOffset() + "-" + helper.getEraseCount() : "";
                String title = NbBundle.getMessage(XMLCompletionQuery.class, "MSG_result", helper.getPreText()) + debugMsg;

                /***************************************************************
                 * The functionality "Inserting of an appropriate closing end tag"
                 * has been moved to the module "xml.schema.completion". The method
                 * "org.netbeans.modules.xml.schema.completion.CompletionQuery.query(...)"
                 * creates (if it's necessary) an instance of the class CompletionResultItem,
                 * related to a closing end tag.
                 * (CompletionResultItem endTagResultItem = CompletionUtil.getEndTagCompletionItem(...))
                 ***************************************************************
                // add to the list end tag if detected '<'
                // unless following end tag is of the same name
                if (helper.getPreText().endsWith("<") && helper.getToken().getTokenID() == TEXT) { // NOI18N
                    List startTags = findStartTag((SyntaxNode)helper.getSyntaxElement(), "/"); // NOI18N
                    boolean addEndTag = true;
                    SyntaxNode ctx = (SyntaxNode) helper.getSyntaxElement();
                    SyntaxElement nextElement = ctx != null ? ctx.getNext() : null;
                    if (nextElement instanceof EndTag) {
                        EndTag endtag = (EndTag) nextElement;
                        String nodename = endtag.getNodeName();
                        if (nodename != null && startTags.isEmpty() == false) {
                            ElementResultItem item = (ElementResultItem) startTags.get(0);
                            if (("/" + nodename).equals(item.getItemText())) {  // NOI18N
                                addEndTag = false;
                            }
                        }
                    }
                    if (addEndTag) {
                        all.addAll(startTags);
                    }
                }
                ***************************************************************/

                all.addAll(list);
                if(!all.isEmpty())
                    return all;
                //TODOxxxreturn noSuggestion(component, sup.requestedAutoCompletion());
                    //TODOxxx
//                    return new XMLCompletionResult(
//                            component,
//                            title,
//                            all,
//                            helper.getOffset() - helper.getEraseCount(),
//                            helper.getEraseCount()
//                            );                
            } else {
                // prolog, internal DTD no completition yet
                if (helper.getToken().getTokenID() == PI_CONTENT) {
                    if (helper.getPreText().endsWith("encoding=")) {                        // NOI18N
//                        List encodings = new ArrayList(2);
//                        encodings.add(new XMLResultItem(0, "\"UTF-8\""));          // NOI18N
//                        encodings.add(new XMLResultItem(1, "\"UTF-16\""));         // NOI18N
                        //TODOxxx
//                        return new XMLCompletionResult(
//                                component,
//                                NbBundle.getMessage(XMLCompletionQuery.class, "MSG_encoding_comp"),
//                                encodings,
//                                helper.getOffset(),
//                                0
//                                );
                    }
                }
                //TODOxxxreturn noSuggestion(component, sup.requestedAutoCompletion());
            }
            
        } catch (BadLocationException e) {
            //Util.THIS.debug(e);
        }
        return null;
        // nobody knows what happened...
         //TODOxxxreturn noSuggestion(component, sup.requestedAutoCompletion());
    }
        
    /**
     * Contruct result indicating that grammar is not able to give
     * a hint because document is too broken or invalid. Grammar
     * knows that it's broken.
     */
//    private static Result cannotSuggest(JTextComponent component, boolean auto) {
//        if (auto) return null;
//        return new XMLCompletionResult(
//                component,
//                NbBundle.getMessage(XMLCompletionQuery.class, "BK0002"),
//                Collections.EMPTY_LIST,
//                0,
//                0
//                );
//    }
    
    /**
     * Contruct result indicating that grammar is not able to give
     * a hint because in given context is not nothing allowed what
     * the grammar know of. May grammar is missing at all.
     */
//    private static Result noSuggestion(JTextComponent component, boolean auto) {
//        if (auto) return null;
//        return new XMLCompletionResult(
//                component,
//                NbBundle.getMessage(XMLCompletionQuery.class, "BK0003"),
//                Collections.EMPTY_LIST,
//                0,
//                0
//                );
//    }
    
    // Grammar binding ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * Obtain grammar manager, cache results in document property
     * <code>PROP_DOCUMENT_QUERY</code>. It is always called from single
     * thread.
     */
    public static GrammarQuery getPerformer(Document doc, XMLSyntaxSupport sup) {
        
        Object grammarBindingObj = doc.getProperty(DOCUMENT_GRAMMAR_BINDING_PROP);
        
        if (grammarBindingObj == null) {
            grammarBindingObj = new GrammarManager(doc, sup);
            doc.putProperty(DOCUMENT_GRAMMAR_BINDING_PROP, grammarBindingObj);
        }
        
        return ((GrammarManager)grammarBindingObj).getGrammar();
    }
    
    // Delegate queriing to performer ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    private List queryEntities(SyntaxQueryHelper helper, Document doc, XMLSyntaxSupport sup) {
        Enumeration res = getPerformer(doc, sup).queryEntities(helper.getContext().getCurrentPrefix());
        return translateEntityRefs(res);
    }
    
    private List queryElements(SyntaxQueryHelper helper, Document doc, XMLSyntaxSupport sup) {
        try {
            GrammarQuery performer = getPerformer(doc, sup);
            HintContext ctx = helper.getContext();
            //66607 hacky fix - see the issue comment
            if(helper.getPreText().startsWith("</")) {
                return Collections.EMPTY_LIST;
            }
            String typedPrefix = ctx.getCurrentPrefix();
            Enumeration res = performer.queryElements(ctx);
            return translateElements(res, typedPrefix, performer);
        } catch(UOException e){
            ErrorManager.getDefault().notify(e);
            return null;
        }
    }
    
    private List<CompletionItem> queryAttributes(SyntaxQueryHelper helper, Document doc, XMLSyntaxSupport sup) {
        Enumeration res = getPerformer(doc, sup).queryAttributes(helper.getContext());
        return translateAttributes(res, helper.isBoundary());
    }
    
    private List<CompletionItem> queryValues(SyntaxQueryHelper helper, Document doc, XMLSyntaxSupport sup) {
        try {
            Enumeration res = getPerformer(doc, sup).queryValues(helper.getContext());
            int delLen = 0;
            String suffix = null;
            if (helper.getToken() != null) {
                if (helper.getToken().getTokenID() == XMLTokenIDs.TEXT) {
                    String c = helper.getToken().getImage();
                    String p = helper.getPreText();
                    // special case: do not remove text after newline to
                    // preserve formatting / indentation
                    int nlIndex = c.indexOf('\n');
                    if (nlIndex > 0) {
                        c = c.substring(0, nlIndex);
                    }
                    delLen = c.length() - p.length();
                    // possibly append closing end tag
                    String tagName = shouldCloseTag(helper, doc, sup);
                    if (tagName != null) {
                        suffix = "</" + tagName + ">";
                    }
                } else if (helper.getToken().getTokenID() == XMLTokenIDs.VALUE) {
                    String c = helper.getToken().getImage();
                    delLen = c.length();
                    if (c.charAt(0) == '"' ||
                        c.charAt(0) == '\'') {
                        delLen--;
                    }
                    int l = c.length() - 1;
                    if (c.charAt(l) == '"' || c.charAt(l) == '\'') {
                        delLen--;
                    }
                } else if (helper.getToken().getTokenID() == XMLTokenIDs.TAG) {
                    String tagName = shouldCloseTag(helper, doc, sup);
                    if (tagName != null) {
                        suffix = "</" + tagName + ">";
                    }
                }
            }
//            String curValue = helper.getContext().getNodeValue();
//            int curLen = 0;
//            if (curValue != null) {
//                curValue = curValue.trim();
//                curLen = curValue.length();
//            }
            return translateValues(res, delLen, suffix);
        } catch (Exception ex) {
            Logger.getLogger(XMLCompletionQuery.class.getName()).log(Level.INFO, "cf. #118136", ex);
            return null;
        }
    }
    
    private List<CompletionItem> queryNotations(SyntaxQueryHelper helper, Document doc, XMLSyntaxSupport sup) {  //!!! to be implemented
        Enumeration res = getPerformer(doc, sup).queryNotations(helper.getContext().getCurrentPrefix());
        return null;
    }
    
    // Translate general results to editor ones ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    private List<CompletionItem> translateEntityRefs(Enumeration refs ) {
        List<CompletionItem> result = new ArrayList<CompletionItem>(133);
        int i = 0;
        while ( refs.hasMoreElements() ) {
            GrammarResult next = (GrammarResult) refs.nextElement();
            if(next != null && next.getNodeName() != null) {
                EntityRefResultItem ref = new EntityRefResultItem(i++, next);
                result.add( ref );
            }
        }
        return result;
    }
    
    /** Translate results perfromer (DOM nodes) format to CompletionQuery.ResultItems format. */
    private List<CompletionItem> translateElements(Enumeration els, String prefix, GrammarQuery perfomer) {
        List<CompletionItem> result = new ArrayList<CompletionItem>(13);
        int i = 0;
        while (els.hasMoreElements()) {
            GrammarResult next = (GrammarResult) els.nextElement();
            if (prefix.equals(next.getNodeName())) {
// XXX It's probably OK that perfomer has returned it, we just do not want to visualize it
//                ErrorManager err =ErrorManager.getDefault();
//                err.log(ErrorManager.WARNING, "Grammar " + perfomer.getClass().getName() + " result '"  + prefix + "' eliminated to avoid #28224.");  // NOi18N
                continue;
            }
            if(next != null && next.getNodeName() != null) {
                ElementResultItem ei = new ElementResultItem(i++, next);
                result.add( ei );
            }
        }
        return result;
    }
    
    
    private List<CompletionItem> translateAttributes(Enumeration attrs, boolean boundary) {
        List<CompletionItem> result = new ArrayList<CompletionItem>(13);
        int i = 0;
        while (attrs.hasMoreElements()) {
            GrammarResult next = (GrammarResult) attrs.nextElement();
            if(next != null && next.getNodeName() != null) {
                AttributeResultItem attr = new AttributeResultItem(i++, next, false);
                result.add( attr );
            }
        }
        return result;
    }
    
    private String shouldCloseTag(SyntaxQueryHelper helper, Document doc, XMLSyntaxSupport sup) {
        TokenItem ti = helper.getToken();
        TokenItem previous = ti.getPrevious();
        if (previous == null || previous.getTokenID() != XMLTokenIDs.TAG) {
            // preceded by something else than tag name, i.e. argument, operator...
            return null;
        }
        String tagName  = previous.getImage();
        if (tagName.equals(">")) { // NOI18N
            // closing brace of a tag, iterate towards tag's begin, skip attributes and their values.
             previous = previous.getPrevious();
            while (previous != null && previous.getTokenID() != XMLTokenIDs.TAG) {
                previous = previous.getPrevious();
            }
            if (previous == null) {
                return null;
            }
            // got tagname.
            tagName = previous.getImage();
        }
        if (tagName.startsWith(END_TAG_PREFIX)) {
            // traversal through preceding tags, counting end-start pairs not implemented.
            return null;
        } else if (!tagName.startsWith(TAG_FIRST_CHAR)) {
            return null;
        } 
        // tag name does not include end sharp brace
        tagName = tagName.substring(1, tagName.length()).trim();
        TokenSequence<XMLTokenId> s = (TokenSequence<XMLTokenId>)TokenHierarchy.get(doc).tokenSequence();
        if (isClosingEndTagFoundAfter(ti.getOffset(), s, tagName)) {
            // I know, there may be multiple levels of the same tag name, and the innermost may
            // be missing...
            return null;
        }
        return tagName;
    }
    
    public static final String
        TAG_FIRST_CHAR = "<", //NOI18N
        TAG_LAST_CHAR  = ">", //NOI18N
        END_TAG_PREFIX = "</", //NOI18N
        END_TAG_SUFFIX = "/>"; //NOI18N

    public static boolean isEndTagPrefix(Token token) {
        if (token == null) return false;

        TokenId tokenID = token.id();
        if (tokenID.equals(XMLTokenId.TAG) || tokenID.equals(XMLTokenId.TEXT)) {
            String tokenText = token.text().toString();
            if (tokenText.startsWith(END_TAG_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTagFirstChar(Token token) {
        if (token == null) return false;

        TokenId tokenID = token.id();
        if (tokenID.equals(XMLTokenId.TAG) || tokenID.equals(XMLTokenId.TEXT)) {
            String tokenText = token.text().toString();
            if ((! isEndTagPrefix(token)) && tokenText.startsWith(TAG_FIRST_CHAR)) {
                return true;
            }
        }
        return false;
    }
    
    public static String getTokenTagName(Token token) {
        if (token == null) return null;

        int index = -1;
        if (isTagFirstChar(token)) {
            index = TAG_FIRST_CHAR.length();
        } else if (isEndTagPrefix(token)) {
            index = END_TAG_PREFIX.length();
        } else {
            return null;
        }
        String tokenText = token.text().toString(),
               tagName = (tokenText == null ? null : tokenText.substring(index));
        return tagName;
    }
    
    private static boolean isClosingEndTagFoundAfter(int caretPos,
        TokenSequence tokenSequence, String startTagName) {
        if ((tokenSequence == null) || (startTagName == null)) return false;
        tokenSequence.move(caretPos);
        
        int unclosedTagCount = 1;
        while (tokenSequence.moveNext()) {
            Token token = tokenSequence.token();
            String nextTagName = getTokenTagName(token);
            // fix for issue #185048
            // (http://netbeans.org/bugzilla/show_bug.cgi?id=185048)
            // also: must not count ends of nested tags, just the
            // same level
            if (isEndTagPrefix(token)) {
                if (unclosedTagCount-- == 0) {
                    return false;
                }
            } else if (isTagFirstChar(token)) {
                unclosedTagCount++;
            } else {
                continue;
            }
            if (unclosedTagCount == 0) {
                if (isEndTagPrefix(token)) {
                    return startTagName.equals(nextTagName);
                }
            }
        }
        return false;
    }

    
    private List<CompletionItem> translateValues(Enumeration values, int delLen, String suffix) {
        List<CompletionItem> result = new ArrayList<CompletionItem>(3);
        int i = 0;
        while (values.hasMoreElements()) {
            GrammarResult next = (GrammarResult) values.nextElement();
            int dl = delLen;
            if (next instanceof Text) {
                // the node provides delete information, use it.
                dl = ((Text)next).getLength();
                if (dl < 0) {
                    dl = delLen;
                }
            }
            if(next != null && next.getDisplayName() != null) {
                ValueResultItem val = new ValueResultItem(i++, next, dl, suffix);
                result.add( val );
            }
        }
        return result;
    }
    
    
    /**
     * User just typed <sample>&lt;/</sample> so we must locate
     * paing start tag.
     * @param text pointer to starting context
     * @param prefix that is prepended to created ElementResult e.g. '</'
     * @return list with one ElementResult or empty.
     */
    private static List<CompletionItem> findStartTag(SyntaxNode text, String prefix) {
        //if ( Util.THIS.isLoggable() ) /* then */ Util.THIS.debug("XMLCompletionQuery.findStartTag: text=" + text);
        
        Node parent = text.getParentNode();
        if (parent == null) {
            return Collections.EMPTY_LIST;
        }
        
        String name = parent.getNodeName();
        //if ( Util.THIS.isLoggable() ) Util.THIS.debug("    name=" + name);
        if ( name == null ) {
            return Collections.EMPTY_LIST;
        }
        
        XMLResultItem res = new ElementResultItem(0, prefix + name);
        //if ( Util.THIS.isLoggable() ) Util.THIS.debug("    result=" + res);
        
        List<CompletionItem> list = new ArrayList<CompletionItem>(1);
        list.add(res);
        
        return list;
    }
    
    private static List<CompletionItem> findStartTag(SyntaxNode text) {
        return findStartTag(text, "");
    }
    
//    public static class XMLCompletionResult extends CompletionQuery.DefaultResult {
//        private int substituteOffset;
//        public XMLCompletionResult(JTextComponent component, String title, List data, int offset, int len ) {
//            super(component, title, data, offset, len);
//            substituteOffset = offset;
//        }
//
//        public int getSubstituteOffset() {
//            return substituteOffset;
//        }
//    }
    
}
