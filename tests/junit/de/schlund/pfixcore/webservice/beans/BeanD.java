package de.schlund.pfixcore.webservice.beans;

@ExcludeByDefault
public class BeanD {

    int foo;
    int bar;
    int baz;
    
    @Include
    public int getFoo() {
        return foo;
    }
    
    public void setFoo(int foo) {
        this.foo=foo;
    }
                                                                                                                                   
    public int getBar() {
        return bar;
    }
    
    public void setBar(int bar) {
        this.bar=bar;
    }
                        
    @Include
    public int getBaz() {
        return baz;
    }
    
    public void setBaz(int baz) {
        this.baz=baz;
    }

}
