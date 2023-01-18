//
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
//
// Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
//
// Oracle and Java are registered trademarks of Oracle and/or its affiliates.
// Other names may be trademarks of their respective owners.
//
// The contents of this file are subject to the terms of either the GNU
// General Public License Version 2 only ("GPL") or the Common
// Development and Distribution License("CDDL") (collectively, the
// "License"). You may not use this file except in compliance with the
// License. You can obtain a copy of the License at
// http://www.netbeans.org/cddl-gplv2.html
// or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
// specific language governing permissions and limitations under the
// License.  When distributing the software, include this License Header
// Notice in each file and include the License file at
// nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
// particular file as subject to the "Classpath" exception as provided
// by Oracle in the GPL Version 2 section of the License file that
// accompanied this code. If applicable, add the following below the
// License Header, with the fields enclosed by brackets [] replaced by
// your own identifying information:
// "Portions Copyrighted [year] [name of copyright owner]"
//
// Contributor(s):
//
// The Original Software is NetBeans. The Initial Developer of the Original
// Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
// Microsystems, Inc. All Rights Reserved.
//
// If you wish your version of this file to be governed by only the CDDL
// or only the GPL Version 2, indicate your decision by adding
// "[Contributor] elects to include this software in this distribution
// under the [CDDL or GPL Version 2] license." If you do not indicate a
// single choice of license, a recipient has the option to distribute
// your version of this file under either the CDDL, the GPL Version 2 or
// to extend the choice of license to its licensees as provided above.
// However, if you add GPL Version 2 code and therefore, elected the GPL
// Version 2 license, then the option applies only if the new code is
// made subject to such option by the copyright holder.
//

/*
 *
 * Parser for preprocessor expressions based on Big Integers
 */
header {

package org.netbeans.modules.cnd.apt.impl.support.generated;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.math.BigInteger;

import org.netbeans.modules.cnd.antlr.*;
import org.netbeans.modules.cnd.antlr.collections.*;
import org.netbeans.modules.cnd.antlr.debug.misc.*;
import org.netbeans.modules.cnd.apt.support.APTMacroCallback;
import org.netbeans.modules.cnd.apt.support.APTBaseToken;
import org.netbeans.modules.cnd.apt.support.APTToken;
import org.netbeans.modules.cnd.apt.utils.APTUtils;
}

options {
	language = "Java"; // NOI18N
} 

{
@org.netbeans.api.annotations.common.SuppressWarnings({"UCF", "MS"})
@SuppressWarnings({"unchecked", "cast", "fallthrough"})
final /*final class attribute gives us performance */
}
class APTBigIntegerExprParser extends Parser;

options {
//	k = 2;
	importVocab = APTGenerated;
	codeGenMakeSwitchThreshold = 2;
	codeGenBitsetTestThreshold = 3;
//	noConstructors = true;
	buildAST = false;
}

{
    // value for ID with text "true" (i.e. it is 1 in C++ and 0 in C langs)
    private BigInteger trueIDValue = BigInteger.ZERO;
    private final static String TRUE = "true";// NOI18N
    private APTMacroCallback callback = null;
    private boolean bigValuesInUse = false;

    public APTBigIntegerExprParser(TokenStream lexer, APTMacroCallback callback, BigInteger trueIDValue) {
        super(lexer, 1, 16);
        tokenNames = _tokenNames;
        this.callback = callback;
        this.trueIDValue = trueIDValue;
    }

    private boolean isDefined(Token id) {
        if (id != null && callback != null) {
            return callback.isDefined((APTToken)id);
        }
        return false;
    }

    private boolean toBoolean(BigInteger r) {
        return BigInteger.ZERO.equals(r) ? false : true;
    }

    // Fixup: workaround is added due to bug in jdk6 Update 10 (see IZ#150693)
    private static BigInteger one = BigInteger.ONE;
    private static BigInteger zero = BigInteger.ZERO;
    private BigInteger toBigInteger(boolean b) {
        return b ? one : zero;
    }

    private BigInteger toBigInteger(String str) {
        BigInteger val = BigInteger.ZERO;
        try {
            str = remSuffix(str);
            if (str.length() > 1 && str.charAt(0) == '0') {
                char secondChar = str.charAt(1);
                if (secondChar == 'x' || secondChar == 'X') {
                    // hex
                    val = new BigInteger(str.substring(2), 16);
                } else {
                    // octal
                    val = new BigInteger(str.substring(1), 8);
                }
            } else {
                // decimal
                val = new BigInteger(str);
            }
        } catch (NumberFormatException ex) {
            //ex.printStackTrace(System.err);
        }
        return val;
    }

    private String remSuffix(String num) {
        int len = num.length();
        boolean stop;
        do {
            stop = true;
            if (len > 0) {
                char last = num.charAt(len - 1);
                // remove postfix like u, U, l, L
                if (last == 'u' || last == 'U' || last == 'l' || last == 'L') {
                    num = num.substring(0, len - 1);
                    len--;
                    stop = false;
                }
            }
        } while (!stop);
        return num;
    }

    private BigInteger binaryToBigInteger(String str) {
        str = remSuffix(str);
        BigInteger val = BigInteger.ZERO;
        try {
            val = new BigInteger(str, 2);
        } catch (NumberFormatException ex) {
            ex.printStackTrace(System.err);
        }
        return val;
    }
    
    private BigInteger charToBigInteger(CharSequence str) {
        int val;
        int len = str.length();
        if (len <= 2) {
            // empty '' or incorrect char
            val = 0;
        } else if (len == 3) {
            val = str.charAt(1);
        } else if (len == 4 && str.charAt(1) == '\\') {
            switch (str.charAt(2)) {
                case 'b':
                    val = '\b';
                    break;
                case 'f':
                    val = '\f';
                    break;
                case 'n':
                    val = '\n';
                    break;
                case 'r':
                    val = '\r';
                    break;
                case 't':
                    val = '\t';
                    break;
//                case '"':
//                    val = '\"';
//                    break;
//                case '\'':
//                    val = '\'';
//                    break;
//                case '\\':
//                    val = '\\';
//                    break;
                default:
                    // what to do with '\e'; '\v'; '\?'; ?
                    // for other like '\'' '\\' '\"' use symbol as is
                    val = str.charAt(2);
                    break;
            }
        } else {
            // for now just use the first char as well
            APTUtils.LOG.log(Level.INFO, "use fallback when convert character [{0}] to BigInteger\n", str); // NOI18N
            val = str.charAt(1);
        }
        if (APTUtils.LOG.isLoggable(Level.FINE)) {
            APTUtils.LOG.log(Level.FINE, "convert char [{0}] to BigInteger {1}\n", new Object[] { str, val}); // NOI18N
        }
        return BigInteger.valueOf(val);
    }

    private BigInteger evalID(Token id) {
        // each not expanded ID in expression is '0' by specification
        // but 'true' can be treated differently:
        //  in C++ as 1
        //  in C as 0
        if (id != null && TRUE.equals(id.getText())) {
            return trueIDValue;
        }
        return BigInteger.ZERO;
    }

}

imaginaryTokenDefinitions :
   SIGN_MINUS
   SIGN_PLUS
;

expr      returns [BigInteger r] {r=BigInteger.ZERO;} : r=ternCondExpr | EOF;
// ternCondExpr uses * because ? generates incorrect code in ANTLR 2.7.5
// don't want to use guessing, because it slows down code
ternCondExpr returns [BigInteger r] : {BigInteger b,c;}   r=orExpr
                (options{generateAmbigWarnings = false;}:
                    QUESTIONMARK^ b=ternCondExpr COLON! c=ternCondExpr { r = toBoolean(r)?b:c;}
                )*
        ;
//rule        :   QUESTIONMARK^ ternCondExpr COLON! ternCondExpr;
orExpr    returns [BigInteger r] : {BigInteger b;}  r=andExpr (OR^ b=andExpr {r=toBigInteger(toBoolean(r) || toBoolean(b));})*;
andExpr   returns [BigInteger r] : {BigInteger b;}  r=borExpr (AND^ b=borExpr {r=toBigInteger(toBoolean(r) && toBoolean(b));})*;
borExpr   returns [BigInteger r] : {BigInteger b;}  r=xorExpr (BITWISEOR^ b=xorExpr {r=r.or(b);})*;
xorExpr   returns [BigInteger r] : {BigInteger b;}  r=bandExpr (BITWISEXOR^ b=bandExpr {r=r.xor(b);})*;
bandExpr  returns [BigInteger r] : {BigInteger b;}  r=eqExpr  (AMPERSAND^ b=eqExpr {r=r.and(b);})*;
eqExpr    returns [BigInteger r] : {BigInteger b;}  r=relExpr (EQUAL^ b=relExpr {r= toBigInteger(r.equals(b));}
                                                 | NOTEQUAL^ b=relExpr {r= toBigInteger(!r.equals(b));})*;
relExpr   returns [BigInteger r] : {BigInteger b;}  r=shiftExpr (LESSTHAN^ b=shiftExpr { r= toBigInteger(r.compareTo(b) < 0); }
                                                    |LESSTHANOREQUALTO^ b=shiftExpr { r= toBigInteger(r.compareTo(b) <= 0); }
                                                    |GREATERTHAN^ b=shiftExpr { r= toBigInteger(r.compareTo(b) > 0); }
                                                    |GREATERTHANOREQUALTO^ b=shiftExpr { r= toBigInteger(r.compareTo(b) >= 0); })*;
shiftExpr returns [BigInteger r] : {BigInteger b;}  r=sumExpr (SHIFTLEFT^ b=sumExpr { r= r.shiftLeft(b.intValue()); }
                                                  |SHIFTRIGHT^ b=sumExpr { r= r.shiftRight(b.intValue()); })*;
sumExpr   returns [BigInteger r] : {BigInteger b;}  r=prodExpr (PLUS^ b=prodExpr { r= r.add(b); }
                                                   |MINUS^ b=prodExpr { r= r.subtract(b); })* ;
prodExpr  returns [BigInteger r] : {BigInteger b;}  r=signExpr (STAR^ b=signExpr { r=r.multiply(b); }
                                                   |DIVIDE^ b=signExpr { r=r.divide(b); }
                                                   |MOD^ b=signExpr { r=r.mod(b); } )* ;
signExpr  returns [BigInteger r] {r=BigInteger.ZERO;}:
                      MINUS^ r=atom { r=r.negate(); }
                    | PLUS^  r=atom { r= (r.signum()<0) ? BigInteger.ZERO.subtract(r) : r; }
                    | NOT^ r=atom { r=toBigInteger(!toBoolean(r)); }
                    | TILDE^ r=atom { r=r.not(); }
                | r=atom ;
atom returns [BigInteger r]  {r=BigInteger.ZERO;}     : r=constant | r=defined | (LPAREN^ r=expr RPAREN!) ;
//atom        : constant | NUMBER | defined | ID | (LPAREN^ expr RPAREN!) ;

defined returns [BigInteger r] {r=BigInteger.ZERO;} :
        DEFINED^
        (
            (LPAREN! id_1:ID_DEFINED RPAREN!) { r = toBigInteger(isDefined(id_1)); }
            | id_2:ID_DEFINED { r = toBigInteger(isDefined(id_2)); }
        )
;

constant returns [BigInteger r] {r=BigInteger.ZERO;}
            :	
            (
                LITERAL_true { r=toBigInteger(true);}
            |	LITERAL_false { r=toBigInteger(false);}
            |   n:NUMBER {r=toBigInteger(n.getText());}
            |   id:IDENT {r=evalID(id);}
            | o:OCTALINT {r=toBigInteger(o.getText());}
            | d:DECIMALINT {r=toBigInteger(d.getText());}
            | x:HEXADECIMALINT {r=toBigInteger(x.
                getText());}
            | b:BINARYINT {r=binaryToBigInteger(b.getText());}
            | c: CHAR_LITERAL {r=charToBigInteger(c.getText());; }
//          | f1: FLOATONE {r=toBigInteger(f1.getText());}
//          | f2: FLOATTWO {r=toBigInteger(f2.getText());}
            )
	;
