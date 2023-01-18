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

package org.netbeans.modules.php.twig.editor.lexer;

import java.util.Objects;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;

@org.netbeans.api.annotations.common.SuppressWarnings({"SF_SWITCH_FALLTHROUGH", "URF_UNREAD_FIELD", "DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING"})
%%

%public
%class TwigTopColoringLexer
%type TwigTopTokenId
%function findNextToken
%unicode
%caseless
%char

%eofval{
        if(input.readLength() > 0) {
            // backup eof
            input.backup(1);
            //and return the text as error token
            if (zzLexicalState == ST_BLOCK) {
                return TwigTopTokenId.T_TWIG_BLOCK;
            } else if (zzLexicalState == ST_VAR) {
                return TwigTopTokenId.T_TWIG_VAR;
            } else {
                return TwigTopTokenId.T_HTML;
            }
        } else {
            return null;
        }
%eofval}

%{

    private TwigStateStack stack = new TwigStateStack();
    private LexerInput input;
    private Lexing lexing;
    private boolean probablyInDString;
    private boolean probablyInSString;

    public TwigTopColoringLexer(LexerRestartInfo info) {
        this.input = info.input();
        if(info.state() != null) {
            //reset state
            setState((LexerState) info.state());
            this.lexing = ((LexerState) info.state()).lexing;
            probablyInDString = ((LexerState) info.state()).probablyInDString;
            probablyInSString = ((LexerState) info.state()).probablyInSString;
        } else {
            zzState = zzLexicalState = YYINITIAL;
            this.lexing = Lexing.NORMAL;
            probablyInDString = false;
            probablyInSString = false;
            stack.clear();
        }

    }

    private enum Lexing {
        NORMAL,
        RAW,
        VERBATIM;
    }

    public static final class LexerState  {
        final TwigStateStack stack;
        /** the current state of the DFA */
        final int zzState;
        /** the current lexical state */
        final int zzLexicalState;
        private final Lexing lexing;
        private final boolean probablyInDString;
        private final boolean probablyInSString;

        LexerState(TwigStateStack stack, int zzState, int zzLexicalState, Lexing lexing, boolean probablyInDString, boolean probablyInSString) {
            this.stack = stack;
            this.zzState = zzState;
            this.zzLexicalState = zzLexicalState;
            this.lexing = lexing;
            this.probablyInDString = probablyInDString;
            this.probablyInSString = probablyInSString;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.stack);
            hash = 71 * hash + this.zzState;
            hash = 71 * hash + this.zzLexicalState;
            hash = 71 * hash + Objects.hashCode(this.lexing);
            hash = 71 * hash + (this.probablyInDString ? 1 : 0);
            hash = 71 * hash + (this.probablyInSString ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LexerState other = (LexerState) obj;
            if (!Objects.equals(this.stack, other.stack)) {
                return false;
            }
            if (this.zzState != other.zzState) {
                return false;
            }
            if (this.zzLexicalState != other.zzLexicalState) {
                return false;
            }
            if (this.lexing != other.lexing) {
                return false;
            }
            if (this.probablyInDString != other.probablyInDString) {
                return false;
            }
            if (this.probablyInSString != other.probablyInSString) {
                return false;
            }
            return true;
        }
    }

    public LexerState getState() {
        return new LexerState(stack.createClone(), zzState, zzLexicalState, lexing, probablyInDString, probablyInSString);
    }

    public void setState(LexerState state) {
        this.stack.copyFrom(state.stack);
        this.zzState = state.zzState;
        this.zzLexicalState = state.zzLexicalState;
        this.lexing = state.lexing;
        this.probablyInDString = state.probablyInDString;
        this.probablyInSString = state.probablyInSString;
    }

    protected int getZZLexicalState() {
        return zzLexicalState;
    }

    protected void popState() {
        yybegin(stack.popStack());
    }

    protected void pushState(final int state) {
        stack.pushStack(getZZLexicalState());
        yybegin(state);
    }


 // End user code

%}

WHITESPACE=[ \t\r\n]+
BLOCK_START="{%"
BLOCK_END="%}"
BLOCK_RAW_START="{%"[ \t]*"raw"[ \t]*"%}"
BLOCK_RAW_END="{%"[ \t]*"endraw"[ \t]*"%}"
BLOCK_VERBATIM_START="{%"[ \t]*"verbatim"[ \t]*"%}"
BLOCK_VERBATIM_END="{%"[ \t]*"endverbatim"[ \t]*"%}"
VAR_START="{{"
VAR_END="}}"
COMMENT_START="{#"
COMMENT_END=([^#] | #[^}])*"#}"
D_STRING_DELIM=\"
S_STRING_DELIM='

%state ST_RAW_START
%state ST_RAW_END
%state ST_VERBATIM_START
%state ST_VERBATIM_END
%state ST_BLOCK
%state ST_VAR
%state ST_COMMENT
%state ST_HIGHLIGHTING_ERROR

%%

<YYINITIAL, ST_RAW_START, ST_RAW_END, ST_VERBATIM_START, ST_VERBATIM_END, ST_BLOCK, ST_VAR, ST_COMMENT>{WHITESPACE}+ {
}

<YYINITIAL> {
    {BLOCK_RAW_START} {
        if (lexing == Lexing.NORMAL) {
            yypushback(yylength());
            pushState(ST_RAW_START);
        }
    }
    {BLOCK_RAW_END} {
        if (lexing != Lexing.VERBATIM) {
            int indexOfRawBlockStart = yytext().lastIndexOf("{%"); //NOI18N
            yypushback(yylength() - indexOfRawBlockStart);
            pushState(ST_RAW_END);
        }
    }
    {BLOCK_VERBATIM_START} {
        if (lexing == Lexing.NORMAL) {
            yypushback(yylength());
            pushState(ST_VERBATIM_START);
        }
    }
    {BLOCK_VERBATIM_END} {
        if (lexing != Lexing.RAW) {
            int indexOfVerbatimBlockStart = yytext().lastIndexOf("{%"); //NOI18N
            yypushback(yylength() - indexOfVerbatimBlockStart);
            pushState(ST_VERBATIM_END);
        }
    }
    {BLOCK_START} {
        if (lexing == Lexing.NORMAL) {
            if (yylength() > 2) {
                yypushback(2);
                return TwigTopTokenId.T_HTML;
            }
            pushState(ST_BLOCK);
            return TwigTopTokenId.T_TWIG_BLOCK_START;
        }
    }
    {COMMENT_START} {
        if (lexing == Lexing.NORMAL) {
            int textLength = yylength();
            yypushback(2);
            pushState(ST_COMMENT);
            if (textLength > 2) {
                return TwigTopTokenId.T_HTML;
            }
        }
    }
    {VAR_START} {
        if (lexing == Lexing.NORMAL) {
            if (yylength() > 2) {
                yypushback(2);
                return TwigTopTokenId.T_HTML;
            }
            pushState(ST_VAR);
            return TwigTopTokenId.T_TWIG_VAR_START;
        }
    }
    . {}
}

<ST_RAW_START> {
    {BLOCK_START} {
        if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_HTML;
        }
        lexing = Lexing.RAW;
        return TwigTopTokenId.T_TWIG_BLOCK_START;
    }
}

<ST_VERBATIM_START> {
    {BLOCK_START} {
        if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_HTML;
        }
        lexing = Lexing.VERBATIM;
        return TwigTopTokenId.T_TWIG_BLOCK_START;
    }
}

<ST_RAW_START, ST_VERBATIM_START> {
    {BLOCK_END} {
        if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_TWIG_BLOCK;
        }
        popState();
        return TwigTopTokenId.T_TWIG_BLOCK_END;
    }
    . {}
}

<ST_RAW_END, ST_VERBATIM_END> {
    {BLOCK_START} {
        if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_HTML;
        }
        lexing = Lexing.NORMAL;
        return TwigTopTokenId.T_TWIG_BLOCK_START;
    }
    {BLOCK_END} {
        if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_TWIG_BLOCK;
        }
        popState();
        return TwigTopTokenId.T_TWIG_BLOCK_END;
    }
    . {}
}

<ST_COMMENT> {
    {COMMENT_END} {
        popState();
        return TwigTopTokenId.T_TWIG_COMMENT;
    }
    . {}
}

<ST_BLOCK> {
    {BLOCK_END} {
        if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_TWIG_BLOCK;
        }
        popState();
        return TwigTopTokenId.T_TWIG_BLOCK_END;
    }
    . {}
}

<ST_VAR> {
    {D_STRING_DELIM} {
        if (!probablyInSString) {
            probablyInDString = !probablyInDString;
        }
    }
    {S_STRING_DELIM} {
        if (!probablyInDString) {
            probablyInSString = !probablyInSString;
        }
    }
    {VAR_END} {
        if (!probablyInDString && !probablyInSString) {
            if (yylength() > 2) {
                yypushback(2);
                return TwigTopTokenId.T_TWIG_VAR;
            }
            popState();
            return TwigTopTokenId.T_TWIG_VAR_END;
        }
    }
    . {}
}

/* ============================================
   Stay in this state until we find a whitespace.
   After we find a whitespace we go the the prev state and try again from the next token.
   ============================================ */
<ST_HIGHLIGHTING_ERROR> {
    . {
        return TwigTopTokenId.T_TWIG_OTHER;
    }
}

/* ============================================
   This rule must be the last in the section!!
   it should contain all the states.
   ============================================ */
<YYINITIAL, ST_RAW_START, ST_RAW_END, ST_VERBATIM_START, ST_VERBATIM_END, ST_BLOCK, ST_VAR, ST_COMMENT> {
    . {
        yypushback(yylength());
        pushState(ST_HIGHLIGHTING_ERROR);
    }
}
