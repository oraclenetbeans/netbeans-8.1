/*******************************************************************************
 * Copyright (c) 2006 Zend Corporation and IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zend and IBM - Initial implementation
 *******************************************************************************/
package org.netbeans.modules.php.editor.parser.astnodes;

import java.util.Arrays;

public class StateStack {

    private byte[] stack;
    private int lastIn = -1;

    /**
     * Creates new StateStack
     */
    public StateStack() {
        this(5);
    }

    public StateStack(int stackSize) {
        stack = new byte[stackSize];
        lastIn = -1;
    }

    public boolean isEmpty() {
        return lastIn == -1;
    }

    public int popStack() {
        int result = stack[lastIn];
        lastIn--;
        return result;
    }

    public void pushStack(int state) {
        lastIn++;
        if (lastIn == stack.length) {
            multiplySize();
        }
        stack[lastIn] = (byte) state;
    }

    private void multiplySize() {
        int length = stack.length;
        byte[] temp = new byte[length * 2];
        System.arraycopy(stack, 0, temp, 0, length);
        stack = temp;
    }

    public int clear() {
        return lastIn = -1;
    }

    public int size() {
        return lastIn + 1;
    }

    public StateStack createClone() {
        StateStack rv = new StateStack(this.size());
        rv.copyFrom(this);
        return rv;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof StateStack)) {
            return false;
        }

        StateStack s2 = (StateStack) obj;
        if (this.lastIn != s2.lastIn) {
            return false;
        }

        for (int i = lastIn; i >= 0; i--) {
            if (this.stack[i] != s2.stack[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Arrays.hashCode(this.stack);
        hash = 53 * hash + this.lastIn;
        return hash;
    }

    public void copyFrom(StateStack s) {
        while (s.lastIn >= this.stack.length) {
            this.multiplySize();
        }
        this.lastIn = s.lastIn;
        for (int i = 0; i <= s.lastIn; i++) {
            this.stack[i] = s.stack[i];
        }
    }

    public boolean contains(int state) {
        for (int i = 0; i <= lastIn; i++) {
            if (stack[i] == state) {
                return true;
            }
        }
        return false;
    }

    public int get(int index) {
        return stack[index];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(50);
        for (int i = 0; i <= lastIn; i++) {
            sb.append(" stack[").append(i).append("]= ").append(stack[i]); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return sb.toString();
    }
}
