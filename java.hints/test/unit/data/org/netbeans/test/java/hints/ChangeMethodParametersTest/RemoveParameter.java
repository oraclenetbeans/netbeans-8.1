package org.netbeans.test.java.hints;

public class RemoveParameter {
    
    private void method(int i, String a, Object o, boolean b) {
        System.out.println(i);
        System.out.println(a);
        System.out.println(o);
        System.out.println(b);
    }
    
    public void test() {
        method(3, "H", this);
        method("H");
    }
}
