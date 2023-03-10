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

package org.netbeans.modules.cnd.editor.reformat;

import org.netbeans.api.lexer.Token;
import org.netbeans.cnd.api.lexer.CppTokenId;
import static org.netbeans.cnd.api.lexer.CppTokenId.*;

/**
 *
 * @author Alexander Simon
 */
class StackEntry {

    private int index;
    private CppTokenId kind;
    private CppTokenId importantKind;
    private boolean likeToFunction = false;
    private boolean likeToArrayInitialization = false;
    private String text;
    private int indent;
    private int selfIndent;
    private int lambdaIndent = 0;
    private int lambdaParen = 0;

    StackEntry(ExtendedTokenSequence ts) {
        super();
        index = ts.index();
        kind = ts.token().id();
        text = ts.token().text().toString();
        switch (kind) {
            case IF: //("if", "keyword-directive"),
            case ELSE: //("else", "keyword-directive"),
            case TRY: //("try", "keyword-directive"), // C++
            case CATCH: //("catch", "keyword-directive"), //C++
            case WHILE: //("while", "keyword-directive"),
            case FOR: //("for", "keyword-directive"),
            case DO: //("do", "keyword-directive"),
            case ASM: //("asm", "keyword-directive"), // gcc and C++
            case SWITCH: //("switch", "keyword-directive"),
                importantKind = kind;
                break;
            default:
                initImportant(ts);
        }
    }

    private void initImportant(ExtendedTokenSequence ts) {
        int i = ts.index();
        try {
            int bracket = 0;
            int paren = 0;
            int triangle = 0;
            boolean hasID = false;
            Token<CppTokenId> id = ts.lookPreviousImportant();
            boolean prevID = id != null && id.id() == IDENTIFIER;
            while (true) {
                if (!ts.movePrevious()) {
                    return;
                }
                Token<CppTokenId> current = ts.token();
                switch (current.id()) {
                    case TEMPLATE:
                    {
                        if (paren == 0 && triangle == 0) {
                            likeToArrayInitialization = false;
                            likeToFunction = true;
                        }
                        break;
                    }
                    case IDENTIFIER:
                    {
                        if (paren == 0 && triangle == 0) {
                            hasID = true;
                        }
                        break;
                    }
                    case RPAREN: //(")", "separator"),
                    {
                        if (paren == 0 && triangle == 0) {
                            likeToFunction = true;
                            Token<CppTokenId> next = ts.lookNextImportant();
                            if (next != null) {
                                if (next.id() == COLON && prevID) {
                                    likeToArrayInitialization = true;
                                    likeToFunction = false;
                                    return;
                                }
                            }
                        }
                        paren++;
                        break;
                    }
                    case LPAREN: //("(", "separator"),
                    {
                        if (paren == 0) {
                            Token<CppTokenId> prev = ts.lookPreviousImportant();
                            if (prev != null) {
                                if (prev.id() == OPERATOR) {
                                    likeToArrayInitialization = false;
                                    likeToFunction = true;
                                    return;
                                }
                            }
                            likeToArrayInitialization = true;
                            return;
                        }
                        paren--;
                        break;
                    }
                        
                    case LBRACKET: //[
                    {
                        bracket--;
                        if (paren == 0 && triangle == 0 && bracket == 0) {
                            Token<CppTokenId> prev = ts.lookPreviousImportant();
                            if (prev != null) {
                                if (prev.id() == OPERATOR) {
                                    likeToArrayInitialization = false;
                                    likeToFunction = true;
                                    return;
                                }
                                if (prev.id() == IDENTIFIER) {
                                    likeToArrayInitialization = true;
                                    return;
                                }
                                if (prev.id() == IDENTIFIER || prev.id() == RBRACKET || prev.id() == LBRACKET) {
                                    break;
                                }
                            }
                            likeToArrayInitialization = false;
                            likeToFunction = false;
                            importantKind = ARROW;
                            lambdaIndent = lambdaIndent(ts);
                            return;
                        }
                        break;
                    }
                    case RBRACKET: //]
                    {
                        bracket++;
                        break;
                    }
                        
                    case CASE:
                    case DEFAULT:
                    {
                        if (paren == 0 && triangle == 0) {
                            likeToArrayInitialization = false;
                            likeToFunction = false;
                            return;
                        }
                        break;
                    }
                    case RBRACE: //("}", "separator"),
                    case LBRACE: //("{", "separator"),
                    case SEMICOLON: //(";", "separator"),
                    {
                        if (paren == 0 && triangle == 0) {
                            if (hasID && !likeToFunction) {
                                likeToArrayInitialization = true;
                            }
                            // undefined
                            return;
                        }
                        break;
                    }
                    case EQ: //("=", "operator"),
                    {
                        if (paren == 0 && triangle == 0) {
                            Token<CppTokenId> prev = ts.lookPreviousImportant();
                            if (prev != null && prev.id() == OPERATOR) {
                                likeToArrayInitialization = false;
                                likeToFunction = true;
                                return;
                            }
                            likeToArrayInitialization = true;
                            likeToFunction = false;
                            return;
                        }
                        break;
                    }
                    case GT: //(">", "operator"),
                    {
                        if (paren == 0) {
                            Token<CppTokenId> prev = ts.lookPreviousImportant();
                            if (prev != null && prev.id() == OPERATOR) {
                                likeToArrayInitialization = false;
                                likeToFunction = true;
                                return;
                            }
                            triangle++;
                        }
                        break;
                    }
                    case LT: //("<", "operator"),
                    {
                        if (paren == 0) {
                            if (triangle == 0) {
                            Token<CppTokenId> prev = ts.lookPreviousImportant();
                                if (prev != null && prev.id() == OPERATOR) {
                                    likeToArrayInitialization = false;
                                    likeToFunction = true;
                                    return;
                                }
                                // undefined
                                return;
                            }
                            triangle--;
                        }
                        break;
                    }
                    case NAMESPACE: //("namespace", "keyword"), //C++
                    {
                        if (paren == 0 && triangle == 0) {
                            importantKind = current.id();
                            likeToFunction = false;
                            return;
                        }
                        break;
                    }
                    case CLASS: //("class", "keyword"), //C++
                    {
                        if (paren == 0 && triangle == 0) {
                            Token<CppTokenId> isEnum = ts.lookPreviousImportant();
                            if (isEnum != null && isEnum.id() == ENUM) {
                                importantKind = isEnum.id();
                            } else {
                                importantKind = current.id();
                            }
                            likeToFunction = false;
                            return;
                        }
                        break;
                    }
                    case STRUCT: //("struct", "keyword"),
                    case ENUM: //("enum", "keyword"),
                    case UNION: //("union", "keyword"),
                    {
                        if (paren == 0 && triangle == 0) {
                            if (!likeToFunction) {
                                importantKind = current.id();
                                return;
                            }
                        }
                        break;
                    }
                    case EXTERN: //EXTERN("extern", "keyword"),
                    {
                        if (paren == 0 && triangle == 0) {
                            if (!likeToFunction) {
                                importantKind = CppTokenId.NAMESPACE;
                                return;
                            }
                        }
                        break;
                    }
                    case IF: //("if", "keyword-directive"),
                    case ELSE: //("else", "keyword-directive"),
                    case SWITCH: //("switch", "keyword-directive"),
                    case WHILE: //("while", "keyword-directive"),
                    case DO: //("do", "keyword-directive"),
                    case FOR: //("for", "keyword-directive"),
                    case TRY: //("try", "keyword-directive"), // C++
                    case CATCH: //("catch", "keyword-directive"), //C++
                    {
                        if (paren == 0 && triangle == 0) {
                            importantKind = current.id();
                            likeToFunction = false;
                            return;
                        }
                        break;
                    }
                    case ARROW: // ->
                    { 
                        if (paren == 0 && triangle == 0) {
                            Token<CppTokenId> prev = ts.lookPreviousImportant();
                            if (prev != null && prev.id() == OPERATOR) {
                                likeToArrayInitialization = false;
                                likeToFunction = true;
                                return;
                            }
                            importantKind = current.id();
                            likeToFunction = false;
                            lambdaIndent = lambdaIndent(ts);
                            return;
                        }
                        break;
                    }
                }
            }
        } finally {
            ts.moveIndex(i);
            ts.moveNext();
        }
    }

    private int lambdaIndent(ExtendedTokenSequence ts) {
        int i = ts.index();
        try {
            while(true) {
                if (!ts.movePrevious()){
                    return 0;
                }
                if (ts.token().id() == NEW_LINE){
                    while(true) {
                        if (!ts.moveNext()) {
                            return 0;
                        }
                        switch(ts.token().id()) {
                            case WHITESPACE:
                                break;
                            default:
                                int d = ts.getTokenPosition();
                                return d;
                        }
                    }
                }
            }
        } finally {
            ts.moveIndex(i);
            ts.moveNext();
        }
    }

    public int getLambdaIndent(){
        return lambdaIndent;
    }

    public int getLambdaParen(){
        return lambdaParen;
    }

    public void setLambdaParen(int lambdaParen){
        this.lambdaParen = lambdaParen;
    }

    public int getIndent(){
        return indent;
    }

    public void setIndent(int indent){
        this.indent = indent;
    }

    public int getSelfIndent(){
        return selfIndent;
    }

    public void setSelfIndent(int selfIndent){
        this.selfIndent = selfIndent;
    }
    
    public int getIndex() {
        return index;
    }
    
    public String getText() {
        return text;
    }

    public CppTokenId getKind() {
        return kind;
    }

    public CppTokenId getImportantKind() {
        return importantKind;
    }

    public boolean isLikeToFunction() {
        return likeToFunction;
    }

    public void setLikeToFunction(boolean likeToFunction) {
        this.likeToFunction = likeToFunction;
    }

    public boolean isLikeToArrayInitialization() {
        return likeToArrayInitialization;
    }

    public void setLikeToArrayInitialization(boolean likeToArrayInitialization) {
        this.likeToArrayInitialization = likeToArrayInitialization;
    }

    @Override
    public String toString(){
        StringBuilder buf = new StringBuilder(kind.name());
        if (importantKind != null && kind != importantKind){
            buf.append("(").append(importantKind.name()).append(")"); // NOI18N
        } else if (likeToFunction) {
            buf.append("(FUNCTION)"); // NOI18N
        } else if (likeToArrayInitialization) {
            buf.append("(ARRAY_INITIALIZATION)"); // NOI18N
        }
        return buf.toString();
    }
}
