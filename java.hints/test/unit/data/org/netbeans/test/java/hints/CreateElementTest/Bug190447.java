package org.netbeans.test.java.hints;

public class Bug190447<E> {
    
    private <T> void t() {
        t = test();
        t2 = this.<T>test2();
        t3 = this.<E>test2();
    }

    public Iterable<? extends String> test() {
        return null;
    }

    public <T> Iterable<? extends T> test2() {
        return null;
    }
    
}
