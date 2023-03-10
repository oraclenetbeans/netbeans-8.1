/* The following code was generated by JFlex 1.4.3 on 22.7.14 11:16 */

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

/**
 * This class is a scanner generated by
 * <a href="http://www.jflex.de/">JFlex</a> 1.4.3
 * on 22.7.14 11:16 from the specification file
 * <tt>/home/ondrej/Projects/web-main/php.twig/tools/TwigTopColoringLexer.flex</tt>
 */
public class TwigTopColoringLexer {
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

  /** This character denotes the end of file */
  public static final int YYEOF = LexerInput.EOF;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int ST_VERBATIM_START = 6;
  public static final int ST_RAW_END = 4;
  public static final int ST_VERBATIM_END = 8;
  public static final int ST_COMMENT = 14;
  public static final int ST_HIGHLIGHTING_ERROR = 16;
  public static final int ST_RAW_START = 2;
  public static final int YYINITIAL = 0;
  public static final int ST_BLOCK = 10;
  public static final int ST_VAR = 12;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = {
     0,  0,  1,  1,  2,  2,  3,  3,  2,  2,  4,  4,  5,  5,  6,  6,
     7, 7
  };

  /**
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED =
    "\11\0\1\5\1\24\2\0\1\1\22\0\1\5\1\0\1\22\1\21"+
    "\1\0\1\3\1\0\1\23\31\0\1\7\1\15\1\0\1\13\1\11"+
    "\3\0\1\17\3\0\1\20\1\12\3\0\1\6\1\0\1\16\1\0"+
    "\1\14\1\10\11\0\1\7\1\15\1\0\1\13\1\11\3\0\1\17"+
    "\3\0\1\20\1\12\3\0\1\6\1\0\1\16\1\0\1\14\1\10"+
    "\3\0\1\2\1\0\1\4\uff82\0";

  /**
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /**
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\10\0\10\1\1\2\1\3\3\1\1\4\1\5\1\6"+
    "\1\7\1\10\1\11\1\12\1\13\1\14\2\0\1\15"+
    "\16\0\1\16\11\0\1\17\5\0\1\20\2\0\1\21";

  private static int [] zzUnpackAction() {
    int [] result = new int[67];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\25\0\52\0\77\0\124\0\151\0\176\0\223"+
    "\0\250\0\275\0\322\0\347\0\374\0\u0111\0\u0126\0\u013b"+
    "\0\250\0\250\0\u0150\0\u0165\0\u017a\0\250\0\250\0\u018f"+
    "\0\250\0\250\0\250\0\250\0\250\0\250\0\u0150\0\u017a"+
    "\0\250\0\u018f\0\u01a4\0\u01b9\0\u01ce\0\u01e3\0\u01f8\0\u020d"+
    "\0\u0222\0\u0237\0\u024c\0\u0261\0\u0276\0\u028b\0\u02a0\0\250"+
    "\0\u02b5\0\u02ca\0\u02df\0\u02f4\0\u0309\0\u031e\0\u0333\0\u0348"+
    "\0\u035d\0\250\0\u0372\0\u0387\0\u039c\0\u03b1\0\u03c6\0\250"+
    "\0\u03db\0\u03f0\0\250";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[67];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /**
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\11\1\12\1\13\2\11\1\12\16\11\1\12\1\11"+
    "\1\12\1\14\1\15\1\11\1\12\16\11\1\12\1\11"+
    "\1\12\1\16\1\15\1\11\1\12\16\11\1\12\1\11"+
    "\1\12\1\17\1\15\1\11\1\12\16\11\1\12\1\11"+
    "\1\12\1\11\1\15\1\11\1\12\16\11\1\12\1\11"+
    "\1\12\2\11\1\20\1\12\14\11\1\21\1\22\1\12"+
    "\1\23\1\24\3\23\1\24\13\23\1\25\2\23\1\24"+
    "\24\26\27\0\1\12\3\0\1\12\16\0\1\12\2\0"+
    "\1\27\1\30\15\0\1\31\6\0\1\32\25\0\1\33"+
    "\23\0\1\34\24\0\1\35\25\0\1\36\20\0\21\37"+
    "\1\40\4\37\1\24\3\37\1\24\13\37\1\40\2\37"+
    "\1\24\4\37\1\41\20\37\5\0\1\42\1\43\2\0"+
    "\1\44\2\0\1\45\17\0\1\46\27\0\1\47\23\0"+
    "\1\50\23\0\1\51\27\0\1\52\17\0\1\53\21\0"+
    "\1\54\1\0\1\51\25\0\1\55\5\0\1\56\25\0"+
    "\1\57\13\0\1\60\27\0\1\61\26\0\1\62\22\0"+
    "\1\63\25\0\1\64\22\0\1\65\34\0\1\66\11\0"+
    "\1\67\1\0\1\64\34\0\1\70\26\0\1\71\11\0"+
    "\1\72\27\0\1\73\35\0\1\74\22\0\1\75\11\0"+
    "\1\76\1\0\1\74\36\0\1\77\11\0\1\100\40\0"+
    "\1\101\7\0\1\102\1\0\1\101\23\0\1\103\20\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[1029];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\10\0\1\11\7\1\2\11\3\1\2\11\1\1\6\11"+
    "\2\0\1\11\16\0\1\11\11\0\1\11\5\0\1\11"+
    "\2\0\1\11";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[67];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the textposition at the last state to be included in yytext */
  private int zzPushbackPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** number of newlines encountered up to the start of the matched text */
  private int yyline;

  /** the number of characters up to the start of the matched text */
  private int yychar;

  /**
   * the number of characters from the last newline up to the start of the
   * matched text
   */
  private int yycolumn;

  /**
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF = false;

  /* user code: */

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



  /**
   * Creates a new scanner
   * There is also a java.io.InputStream version of this constructor.
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public TwigTopColoringLexer(java.io.Reader in) {
    this.zzReader = in;
  }

  /**
   * Creates a new scanner.
   * There is also java.io.Reader version of this constructor.
   *
   * @param   in  the java.io.Inputstream to read input from.
   */
  public TwigTopColoringLexer(java.io.InputStream in) {
    this(new java.io.InputStreamReader(in));
  }

  /**
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x10000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 110) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }



  /**
   * Closes the input stream.
   */
  public final void yyclose() throws java.io.IOException {
    zzAtEOF = true;            /* indicate end of file */
    zzEndRead = zzStartRead;  /* invalidate buffer    */

    if (zzReader != null)
      zzReader.close();
  }


  /**
   * Resets the scanner to read from a new input stream.
   * Does not close the old reader.
   *
   * All internal variables are reset, the old input stream
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <tt>ZZ_INITIAL</tt>.
   *
   * @param reader   the new input stream
   */
  public final void yyreset(java.io.Reader reader) {
    zzReader = reader;
    zzAtBOL  = true;
    zzAtEOF  = false;
    zzEndRead = zzStartRead = 0;
    zzCurrentPos = zzMarkedPos = zzPushbackPos = 0;
    yyline = yychar = yycolumn = 0;
    zzLexicalState = YYINITIAL;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final String yytext() {
    return input.readText().toString();
  }


  /**
   * Returns the character at position <tt>pos</tt> from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
     return input.readText().charAt(pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return input.readLength();
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    input.backup(number);
    //zzMarkedPos -= number;
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public TwigTopTokenId findNextToken() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    //int zzCurrentPosL;
    //int zzMarkedPosL;
    //int zzEndReadL = zzEndRead;
    //char [] zzBufferL = zzBuffer;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      //zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      //zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
      int tokenLength = 0;

      zzState = ZZ_LEXSTATE[zzLexicalState];


      zzForAction: {
        while (true) {
            zzInput = input.read();

            if(zzInput == LexerInput.EOF) {
                //end of input reached
                zzInput = YYEOF;
                break zzForAction;
                //notice: currently LexerInput.EOF == YYEOF
            }

          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          int zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            tokenLength = input.readLength();
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      if(zzInput != YYEOF) {
         input.backup(input.readLength() - tokenLength);
      }

      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
        case 11:
          { if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_HTML;
        }
        lexing = Lexing.VERBATIM;
        return TwigTopTokenId.T_TWIG_BLOCK_START;
          }
        case 18: break;
        case 14:
          { if (lexing == Lexing.NORMAL) {
            yypushback(yylength());
            pushState(ST_RAW_START);
        }
          }
        case 19: break;
        case 12:
          { if (!probablyInDString && !probablyInSString) {
            if (yylength() > 2) {
                yypushback(2);
                return TwigTopTokenId.T_TWIG_VAR;
            }
            popState();
            return TwigTopTokenId.T_TWIG_VAR_END;
        }
          }
        case 20: break;
        case 7:
          { if (lexing == Lexing.NORMAL) {
            int textLength = yylength();
            yypushback(2);
            pushState(ST_COMMENT);
            if (textLength > 2) {
                return TwigTopTokenId.T_HTML;
            }
        }
          }
        case 21: break;
        case 13:
          { popState();
        return TwigTopTokenId.T_TWIG_COMMENT;
          }
        case 22: break;
        case 4:
          { return TwigTopTokenId.T_TWIG_OTHER;
          }
        case 23: break;
        case 3:
          { if (!probablyInDString) {
            probablyInSString = !probablyInSString;
        }
          }
        case 24: break;
        case 15:
          { if (lexing != Lexing.VERBATIM) {
            int indexOfRawBlockStart = yytext().lastIndexOf("{%"); //NOI18N
            yypushback(yylength() - indexOfRawBlockStart);
            pushState(ST_RAW_END);
        }
          }
        case 25: break;
        case 10:
          { if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_HTML;
        }
        lexing = Lexing.NORMAL;
        return TwigTopTokenId.T_TWIG_BLOCK_START;
          }
        case 26: break;
        case 5:
          { if (lexing == Lexing.NORMAL) {
            if (yylength() > 2) {
                yypushback(2);
                return TwigTopTokenId.T_HTML;
            }
            pushState(ST_VAR);
            return TwigTopTokenId.T_TWIG_VAR_START;
        }
          }
        case 27: break;
        case 2:
          { if (!probablyInSString) {
            probablyInDString = !probablyInDString;
        }
          }
        case 28: break;
        case 16:
          { if (lexing == Lexing.NORMAL) {
            yypushback(yylength());
            pushState(ST_VERBATIM_START);
        }
          }
        case 29: break;
        case 6:
          { if (lexing == Lexing.NORMAL) {
            if (yylength() > 2) {
                yypushback(2);
                return TwigTopTokenId.T_HTML;
            }
            pushState(ST_BLOCK);
            return TwigTopTokenId.T_TWIG_BLOCK_START;
        }
          }
        case 30: break;
        case 17:
          { if (lexing != Lexing.RAW) {
            int indexOfVerbatimBlockStart = yytext().lastIndexOf("{%"); //NOI18N
            yypushback(yylength() - indexOfVerbatimBlockStart);
            pushState(ST_VERBATIM_END);
        }
          }
        case 31: break;
        case 8:
          { if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_HTML;
        }
        lexing = Lexing.RAW;
        return TwigTopTokenId.T_TWIG_BLOCK_START;
          }
        case 32: break;
        case 9:
          { if (yylength() > 2) {
            yypushback(2);
            return TwigTopTokenId.T_TWIG_BLOCK;
        }
        popState();
        return TwigTopTokenId.T_TWIG_BLOCK_END;
          }
        case 33: break;
        case 1:
          {
          }
        case 34: break;
        default:
          if (zzInput == YYEOF)
            //zzAtEOF = true;
              {         if(input.readLength() > 0) {
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
 }

          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
