package org.netbeans.modules.cnd.antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.cs.usfca.edu
 * Software rights: http://www.antlr.org/license.html
 */

public class CommonHiddenStreamToken extends CommonToken {
    protected CommonHiddenStreamToken hiddenBefore;
    protected CommonHiddenStreamToken hiddenAfter;

    public CommonHiddenStreamToken() {
        super();
    }

    public CommonHiddenStreamToken(int t, String txt) {
        super(t, txt);
    }

    public CommonHiddenStreamToken(String s) {
        super(s);
    }

    public CommonHiddenStreamToken getHiddenAfter() {
        return hiddenAfter;
    }

    public CommonHiddenStreamToken getHiddenBefore() {
        return hiddenBefore;
    }

    protected void setHiddenAfter(CommonHiddenStreamToken t) {
        hiddenAfter = t;
    }

    protected void setHiddenBefore(CommonHiddenStreamToken t) {
        hiddenBefore = t;
    }
}
