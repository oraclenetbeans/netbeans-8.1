package inspectAndTransform;

public class Class_A_G {

    public boolean m1(int value) {
        if(value > 10){
            return true;
        }else{
            return false;
        }
    }

    public boolean m2(String value) {
        if(value == ""){
            return true;
        }else{
            return false;
        }
    }

    public void m3(String value) {
        if(value != ""){
            System.out.println("...");
        }
    }
}
