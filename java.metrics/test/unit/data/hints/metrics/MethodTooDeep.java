/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.Collection;

/**
 *
 * @author sdedic
 */
public class MethodTooDeep {
    private Collection col;
    
    public int m() {
        int a = 0;
        OUTER: for (int i = 0; i < 10; i++) {
            int j = 0;
            try {
                do {
                    if (i % 2 == 0) {
                        a *= 2;
                        if (j == i / 2) {
                            if (j % 2 == 0) {
                                a = a + Math.random() > 0.5 ? 1 : 2;
                            }
                            break;
                        } else if (j == i / 3) {
                            for (Object o : col) {
                                int x = 0;
                                while (x < 10) {
                                    x++;
                                    switch (x) {
                                        case 1:
                                    }
                                }
                            }
                            continue OUTER;
                        }
                    }
                    j++;
                } while (j < i);
            } catch (IllegalArgumentException ex) {
                a--;
            } catch (NullPointerException ex) {
                a -= this.m();
            }
        }
        return a;
    }
}
