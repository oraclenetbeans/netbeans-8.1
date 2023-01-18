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
package org.netbeans.lib.terminalemulator;

/**
 * Stuff common to InterpANSI, InterpDtTerm and InterpXTerm.
 * @author ivan
 */
class InterpProtoANSI extends InterpDumb {

    protected static class Ascii {
        public static final char ESC = 27;
        public static final char CTRL_N = 14;   // ASCII SO/ShiftOut
        public static final char CTRL_O = 15;   // ASCII SI/ShiftIn
    }
    
    protected static class InterpTypeProtoANSI extends InterpTypeDumb {
	protected final Actor act_reset_number = new ACT_RESET_NUMBER();
	protected final Actor act_remember_digit = new ACT_REMEMBER_DIGIT();
        protected final Actor act_push_number = new ACT_PUSH_NUMBER();
	protected final Actor act_remember1 = new ACT_REMEMBER1();
	protected final Actor act_setg = new ACT_SETG();

	protected final State st_esc = new State("esc");	// NOI18N
	protected final State st_esc_lb = new State("esc_lb");	// NOI18N

	protected final State st_esc_setg = new State("esc_setg");// NOI18N

	protected InterpTypeProtoANSI() {
	    st_base.setAction((char) 27, st_esc, new ACT_TO_ESC());
	    st_base.setAction(Ascii.CTRL_N, st_base, new ACT_AS());
	    st_base.setAction(Ascii.CTRL_O, st_base, new ACT_AE());

	    st_esc.setRegular(st_esc, act_regular);
	    st_esc.setAction('M', st_base, new ACT_M());
	    st_esc.setAction('c', st_base, new ACT_FULL_RESET());

	    st_esc.setAction('n', st_base, new ACT_LS2());
	    st_esc.setAction('o', st_base, new ACT_LS3());

	    st_esc.setAction('(', st_esc_setg, act_remember1);
	    st_esc.setAction(')', st_esc_setg, act_remember1);
	    st_esc.setAction('*', st_esc_setg, act_remember1);
	    st_esc.setAction('+', st_esc_setg, act_remember1);
                st_esc_setg.setAction('B', st_base, act_setg);
                st_esc_setg.setAction('0', st_base, act_setg);

	    st_esc.setAction('[', st_esc_lb, act_reset_number);

	    st_esc_lb.setRegular(st_esc_lb, act_regular);
	    for (char c = '0'; c <= '9'; c++)
		st_esc_lb.setAction(c, st_esc_lb, act_remember_digit);
	    st_esc_lb.setAction(';', st_esc_lb, act_push_number);
	    st_esc_lb.setAction('A', st_base, new ACT_UP());
	    st_esc_lb.setAction('B', st_base, new ACT_CUD());
	    st_esc_lb.setAction('C', st_base, new ACT_ND());
	    st_esc_lb.setAction('D', st_base, new ACT_BC());
	    st_esc_lb.setAction('G', st_base, new ACT_CHA());
	    st_esc_lb.setAction('H', st_base, new ACT_HO());
	    st_esc_lb.setAction('I', st_base, new ACT_CHT());
	    st_esc_lb.setAction('J', st_base, new ACT_J());
	    st_esc_lb.setAction('K', st_base, new ACT_K());
	    st_esc_lb.setAction('L', st_base, new ACT_AL());
	    st_esc_lb.setAction('M', st_base, new ACT_DL());
	    st_esc_lb.setAction('P', st_base, new ACT_DC());
	    st_esc_lb.setAction('X', st_base, new ACT_ECH());
	    st_esc_lb.setAction('Z', st_base, new ACT_CBT());
	    st_esc_lb.setAction('@', st_base, new ACT_IC());
	    st_esc_lb.setAction('d', st_base, new ACT_VPA());
	    st_esc_lb.setAction('h', st_base, new ACT_SM());
	    st_esc_lb.setAction('l', st_base, new ACT_RM());
	    st_esc_lb.setAction('m', st_base, new ACT_ATTR());
	    st_esc_lb.setAction('n', st_base, new ACT_DSR());
	    st_esc_lb.setAction('r', st_base, new ACT_MARGIN());
        }

	static final class ACT_CHA implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_cha(1);
		else
		    ai.ops.op_cha(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_ECH implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_ech(1);
		else
		    ai.ops.op_ech(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_CBT implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_cbt(1);
		else
		    ai.ops.op_cbt(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_CHT implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_cht(1);
		else
		    ai.ops.op_cht(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_VPA implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_vpa(1);
		else
		    ai.ops.op_vpa(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_TO_ESC implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		return null;
	    }
	}

        static final class ACT_AS implements Actor {
	    @Override
            public String action(AbstractInterp ai, char c) {
                ai.ops.op_as();
                return null;
            }
        }
        
        static final class ACT_AE implements Actor {
	    @Override
            public String action(AbstractInterp ai, char c) {
                ai.ops.op_ae();
                return null;
            }
        }

	static class ACT_RESET_NUMBER implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		ai.resetNumber();
		return null;
	    }
	};

	static final class ACT_REMEMBER_DIGIT implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		ai.remember_digit(c);
		return null;
	    }
	};

	static final class ACT_M implements Actor {
            @Override
	    public String action(AbstractInterp ai, char c) {
		ai.ops.op_ri(1);
		return null;
	    }
	}

	static final class ACT_FULL_RESET implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		ai.ops.op_full_reset();
		return null;
	    }
	}

	static final class ACT_LS2 implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		ai.ops.op_selectGL(2);
		return null;
	    }
	}

	static final class ACT_LS3 implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		ai.ops.op_selectGL(3);
		return null;
	    }
	}

	static final class ACT_REMEMBER1 implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
                InterpProtoANSI i = (InterpProtoANSI) ai;
                i.rememberedChar = c;
		return null;
	    }
	}

	static final class ACT_SETG implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
                int gx = 0;
                InterpProtoANSI i = (InterpProtoANSI) ai;
                switch (i.rememberedChar) {
                    case '(':           gx = 0;  break;
                    case ')':           gx = 1;  break;
                    case '*':           gx = 2;  break;
                    case '+':           gx = 3;  break;
                }
                int fx = 0;
                switch (c) {
                    case 'B': fx = 0;   break;
                    case '0': fx = 1;   break;
                }
                ai.ops.op_setG(gx, fx);
		return null;
	    }
	}

	static final class ACT_PUSH_NUMBER implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (!ai.pushNumber())
		    return "ACT PUSH_NUMBER";	// NOI18N
		return null;
	    }
	}

	static final class ACT_UP implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_cuu(1);
		else
		    ai.ops.op_cuu(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_CUD implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
                // no scroll
		if (ai.noNumber())
		    ai.ops.op_cud(1);
		else
		    ai.ops.op_cud(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_ND implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_nd(1);
		else
		    ai.ops.op_nd(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_BC implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_bc(1);
		else
		    ai.ops.op_bc(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_HO implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber()) {
		    ai.ops.op_ho();
		} else {
		    ai.ops.op_cm(ai.numberAt(0), ai.numberAt(1));// row, col
		} 
		return null;
	    }
	}

	static final class ACT_J implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber()) {
		    ai.ops.op_ed(0);
		} else {
		    final int code = ai.numberAt(0);
                    switch (code) {
                        case 0:
                        case 1:
                        case 2:
                            ai.ops.op_ed(code);
                            break;
                        default:
                            return "ACT J: count of > 2 not supported";	// NOI18N
                    }
		} 
		return null;
	    }
	}

	static final class ACT_K implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber()) {
                    ai.ops.op_el(0);
		} else {
                    final int code = ai.numberAt(0);
                    switch (code) {
                        case 0:
                        case 1:
                        case 2:
                            ai.ops.op_el(code);
                            break;
                        default:
                            return "ACT K: count of > 2 not supported";	// NOI18N
                    }
		} 
		return null;
	    }
	}

	static final class ACT_AL implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber()) {
		    ai.ops.op_al(1);
		} else {
		    ai.ops.op_al(ai.numberAt(0));
		} 
		return null;
	    }
	}

	static final class ACT_DL implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber()) {
		    ai.ops.op_dl(1);
		} else {
		    ai.ops.op_dl(ai.numberAt(0));
		} 
		return null;
	    }
	}

	static final class ACT_DC implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_dc(1);
		else
		    ai.ops.op_dc(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_IC implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_ic(1);
		else
		    ai.ops.op_ic(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_SM implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_set_mode(1);
		else
		    ai.ops.op_set_mode(ai.numberAt(0));
		return null;
	    }
	}

	static final class ACT_RM implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_reset_mode(1);
		else
		    ai.ops.op_reset_mode(ai.numberAt(0));
		return null;
	    }
	}


	static final class ACT_ATTR implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		// set graphics modes (bold, reverse video etc)
		if (ai.noNumber()) {
		    ai.ops.op_attr(0);	// reset everything
		} else {
		    for (int n = 0; n <= ai.nNumbers(); n++) {
                        final int attr = ai.numberAt(n);
                        if (!((InterpProtoANSI) ai).dispatchAttr(ai, attr))
                            return "ACT_ATTR: unrecognized attribute " + attr;  // NOI18N
                    }
		}
		return null;
	    }
	}

	static final class ACT_DSR implements Actor {
	    // Device Status Report
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber()) {
		    ai.ops.op_status_report(5);	// reset everything
		} else {
		    ai.ops.op_status_report(ai.numberAt(0));
		}
		return null;
	    }
	}

	static final class ACT_MARGIN implements Actor {
	    @Override
	    public String action(AbstractInterp ai, char c) {
		if (ai.noNumber())
		    ai.ops.op_margin(0, 0);
		else
		    ai.ops.op_margin(ai.numberAt(0), ai.numberAt(1));
		return null;
	    }
	}

    }

    private InterpTypeProtoANSI type;

    private static final InterpTypeProtoANSI type_singleton = new InterpTypeProtoANSI();

    private char rememberedChar;

    public InterpProtoANSI(Ops ops) {
	super(ops, type_singleton);
	this.type = type_singleton;
	setup();
    } 

    protected InterpProtoANSI(Ops ops, InterpTypeProtoANSI type) {
	super(ops, type);
	this.type = type;
	setup();
    } 

    @Override
    public String name() {
	return "proto-ansi";	// NOI18N
    } 

    @Override
    public void reset() {
	super.reset();
    }

    /**
     * Process an attrib code in an Interp-specific manner.
     * @param ai
     * @param n The attribute code.
     * @return Whether the attribute code is valid for the Interp.
     */
    protected boolean dispatchAttr(AbstractInterp ai, int n) {
        return false;
    }

    private void setup() {
        state = type.st_base;
    }
}
