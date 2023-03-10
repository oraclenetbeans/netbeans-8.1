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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.languages.ini.lexer;

import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;

@org.netbeans.api.annotations.common.SuppressWarnings({"SF_SWITCH_FALLTHROUGH", "URF_UNREAD_FIELD", "DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING"})
%%

%public
%class IniColoringLexer
%type IniTokenId
%function nextToken
%unicode
%caseless
%char

%eofval{
        if(input.readLength() > 0) {
            // backup eof
            input.backup(1);
            //and return the text as error token
            return IniTokenId.INI_ERROR;
        } else {
            return null;
        }
%eofval}

%{

    private StateStack stack = new StateStack();

    private LexerInput input;

    public IniColoringLexer(LexerRestartInfo info) {
        this.input = info.input();
        if(info.state() != null) {
            //reset state
            setState((LexerState) info.state());
        } else {
            zzState = zzLexicalState = YYINITIAL;
            stack.clear();
        }

    }

    public static final class LexerState  {
        final StateStack stack;
        /** the current state of the DFA */
        final int zzState;
        /** the current lexical state */
        final int zzLexicalState;

        LexerState(StateStack stack, int zzState, int zzLexicalState) {
            this.stack = stack;
            this.zzState = zzState;
            this.zzLexicalState = zzLexicalState;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            LexerState state = (LexerState) obj;
            return (this.stack.equals(state.stack)
                && (this.zzState == state.zzState)
                && (this.zzLexicalState == state.zzLexicalState));
        }

        @Override
        public int hashCode() {
            int hash = 11;
            hash = 31 * hash + this.zzState;
            hash = 31 * hash + this.zzLexicalState;
            if (stack != null) {
                hash = 31 * hash + this.stack.hashCode();
            }
            return hash;
        }
    }

    public LexerState getState() {
        return new LexerState(stack.createClone(), zzState, zzLexicalState);
    }

    public void setState(LexerState state) {
        this.stack.copyFrom(state.stack);
        this.zzState = state.zzState;
        this.zzLexicalState = state.zzLexicalState;
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

WHITESPACE=[ \t]+
NEWLINE=("\r"|"\n"|"\r\n")
D_STRING="\""([^"\""]|"\\\"")*"\""
S_STRING="'"([^"'"]|"\\'")*"'"
EQUALS="="
COMMENT=";"[^"\r""\n""\r\n"]*
SECTION="["[^"]""\r""\n""\r\n"]*"]"
KEY=[^"=""\r""\n""\r\n"";"]+
QUOTED_VALUE={D_STRING} | {S_STRING}
UNQUOTED_VALUE=[^"\r""\n""\r\n"";"]+
VALUE={WHITESPACE}*{QUOTED_VALUE}{UNQUOTED_VALUE}? | {UNQUOTED_VALUE}
ERROR=[^"\r""\n""\r\n"";"]+

%state ST_IN_BLOCK
%state ST_IN_SECTION
%state ST_AFTER_SECTION
%state ST_IN_KEY
%state ST_IN_VALUE
%state ST_HIGHLIGHTING_ERROR


%%
<YYINITIAL>.|{NEWLINE} {
    yypushback(yylength());
    pushState(ST_IN_BLOCK);
}

<ST_IN_BLOCK, ST_AFTER_SECTION, ST_IN_KEY, ST_IN_VALUE>{WHITESPACE} {
    return IniTokenId.INI_WHITESPACE;
}

<ST_IN_BLOCK, ST_AFTER_SECTION, ST_IN_KEY>{COMMENT} {
    return IniTokenId.INI_COMMENT;
}

<ST_IN_BLOCK> {
    {SECTION} {
        yypushback(yylength());
        pushState(ST_IN_SECTION);
    }
    {NEWLINE} {
        return IniTokenId.INI_WHITESPACE;
    }
    . {
        pushState(ST_IN_KEY);
        yypushback(yylength());
    }
}

<ST_IN_SECTION> {
    "[" {
        return IniTokenId.INI_SECTION_DELIM;
    }
    "]" {
        popState();
        pushState(ST_AFTER_SECTION);
        return IniTokenId.INI_SECTION_DELIM;
    }
    [^\[\]]+ {
        return IniTokenId.INI_SECTION;
    }
}

<ST_AFTER_SECTION> {
    {WHITESPACE} {
        return IniTokenId.INI_WHITESPACE;
    }
    {COMMENT} {
        return IniTokenId.INI_COMMENT;
    }
    {ERROR} {
        return IniTokenId.INI_ERROR;
    }
    {NEWLINE} {
        popState();
        return IniTokenId.INI_WHITESPACE;
    }
}

<ST_IN_KEY> {
    {EQUALS} {
        popState();
        pushState(ST_IN_VALUE);
        return IniTokenId.INI_EQUALS;
    }
    {KEY} {
        return IniTokenId.INI_KEY;
    }
    {NEWLINE} {
        popState();
        return IniTokenId.INI_WHITESPACE;
    }
}

<ST_IN_VALUE> {
    {VALUE} {
        return IniTokenId.INI_VALUE;
    }
    {COMMENT} {
        popState();
        return IniTokenId.INI_COMMENT;
    }
    {NEWLINE} {
        popState();
        return IniTokenId.INI_WHITESPACE;
    }
}

/* ============================================
   Stay in this state until we find a whitespace.
   After we find a whitespace we go the the prev state and try again from the next token.
   ============================================ */
<ST_HIGHLIGHTING_ERROR> {
	{WHITESPACE} {
        popState();
        return IniTokenId.INI_WHITESPACE;
    }
    . | {NEWLINE} {
        return IniTokenId.INI_ERROR;
    }
}

// not needed for ini files...
/* ============================================
   This rule must be the last in the section!!
   it should contain all the states.
   ============================================ */
/*<YYINITIAL, ST_IN_BLOCK, ST_IN_SECTION, ST_AFTER_SECTION, ST_IN_KEY, ST_IN_VALUE> {
    . {
        yypushback(yylength());
        pushState(ST_HIGHLIGHTING_ERROR);
    }
    {NEWLINE} {
        yypushback(yylength());
        pushState(ST_HIGHLIGHTING_ERROR);
    }
}*/
