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

package org.netbeans.modules.java.source.builder;

import org.netbeans.api.java.source.Comment;
import org.netbeans.modules.java.source.query.CommentHandler;

import com.sun.source.tree.Tree;

import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.Context;

import java.util.*;
import org.netbeans.modules.java.source.query.CommentSet.RelativePosition;


/**
 * Generate Comments during scanning.
 */
public class CommentHandlerService implements CommentHandler {
    private static final Context.Key<CommentHandlerService> commentHandlerKey = 
        new Context.Key<CommentHandlerService>();
    
    /** Get the CommentMaker instance for this context. */
    public static CommentHandlerService instance(Context context) {
	CommentHandlerService instance = context.get(commentHandlerKey);
	if (instance == null) {
	    instance = new CommentHandlerService(context);
            setCommentHandler(context, instance);
        }
	return instance;
    }
    
    /**
     * Called from reattributor.
     */
    public static void setCommentHandler(Context context, CommentHandlerService instance) {
        assert context.get(commentHandlerKey) == null;
        context.put(commentHandlerKey, instance);
    }

    private final Map<Tree, CommentSetImpl> map = new HashMap<Tree, CommentSetImpl>();
    
    private CommentHandlerService(Context context) {
    }
    
    Map<Tree, CommentSetImpl> getCommentMap() {
        Map<Tree, CommentSetImpl> m = new HashMap<>(map);
        for (Iterator<Map.Entry<Tree, CommentSetImpl>> it = m.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Tree, CommentSetImpl> e = it.next();
            if (!e.getValue().hasComments()) {
                it.remove();
            }
        }
        return m;
    }
    
    public boolean hasComments(Tree tree) {
        synchronized (map) {
            return map.containsKey(tree);
        }
    }
    
    public CommentSetImpl getComments(Tree tree) {
        synchronized (map) {
            CommentSetImpl cs = map.get(tree);
            if (cs == null) {
                // note - subsequent change to the CommentSetImpl will clone the old (empty) set of comments into CommentSetImpl
                // optimization NOT to retain empty CSImpls is not possible; the caller may modify the return value.
                cs = new CommentSetImpl();
                map.put(tree, cs);
            }
            return cs;
        }
    }

    /**
     * Copies preceding and trailing comments from one tree to another,
     * appending the new entries to the existing comment lists.
     */
    public void copyComments(Tree fromTree, Tree toTree) {
        copyComments(fromTree, toTree, null, null, false);
    }
        
    /**
     * Copies comments from one Tree to another.
     * If non-empty is true, the contents of 'relative position' is only copied if it contains non-whitespaces. This is used
     * when moving comments to an unrelated Tree, often changing RelativePosition (copyToPos != null) - whitespaces at the start
     * or end only mess up the source.
     */
    public void copyComments(Tree fromTree, Tree toTree, RelativePosition copyToPos, Collection<Comment> copied, boolean nonEmpty) {
        if (fromTree == toTree) {
            return;
        }
        synchronized (map) {
            CommentSetImpl from = map.get(fromTree);
            if (from != null) {
                CommentSetImpl to = map.get(toTree);
                if (to == null) {
                    map.put(toTree, to = new CommentSetImpl());
                }
                for (RelativePosition pos : RelativePosition.values()) {
                    int index = 0;
                    int last = -1;
                    int first = 0;
                    List<Comment> l = from.getComments(pos);
                    if (nonEmpty) {
                        boolean nonWs = false;
                        for (Comment c : l) {
                            if (c.style()  != Comment.Style.WHITESPACE) {
                                last = index;
                                if (!nonWs) {
                                    first = index;
                                    nonWs = true;
                                }
                            }
                            index++;
                        }
                        if (!nonWs) {
                            continue;
                        }
                    } 
                    if (last == -1) {
                        last = l.size() - 1;
                    }
                    for (index = first; index <= last; index++) {
                        Comment c = l.get(index);
                        if (copied != null && !copied.add(c)) {
                            continue;
                        }
                        to.addComment(copyToPos == null ? pos : copyToPos, c);
                    }
                }
            }
        }
    }
    
    /**
     * Add a comment to a tree's comment set.  If a comment set
     * for the tree doesn't exist, one will be created.
     */
    public void addComment(Tree tree, Comment c) {
        synchronized (map) {
            CommentSetImpl set = map.get(tree);
            if (set == null) {
                set = new CommentSetImpl();
                map.put(tree, set);
            }
            set.addPrecedingComment(c);
        }
    }


    public String toString() {
        return "CommentHandlerService[" +
                "map=" + map +
                ']';
    }
}
