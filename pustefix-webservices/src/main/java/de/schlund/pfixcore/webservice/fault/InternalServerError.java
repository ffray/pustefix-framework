/*
 * de.schlund.pfixcore.webservice.fault.InternalServerError
 */
package de.schlund.pfixcore.webservice.fault;

public class InternalServerError extends Exception {
    
    /**
     * 
     */
    private static final long serialVersionUID = -8624059164450583256L;

    public InternalServerError() {
        super("Internal server error");
    }

}
