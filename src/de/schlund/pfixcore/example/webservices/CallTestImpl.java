package de.schlund.pfixcore.example.webservices;

public class CallTestImpl implements CallTest {

    public String test(String str) {
        return str;
    }
    
    public String testError(String str) {
        if(str.equalsIgnoreCase("error")) throw new IllegalArgumentException("Illegal value: "+str);
        return str;
    }
    
}