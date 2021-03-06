package org.pustefixframework.http;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.schlund.pfixxml.LanguageInfo;
import de.schlund.pfixxml.TenantInfo;
import de.schlund.pfixxml.serverutil.SessionAdmin;

public class SessionHandlingTestServlet extends HttpServlet {

    private static final long serialVersionUID = -6733916353769534326L;

    private SessionHandlingTestHandler handler;
    
    private Class<? extends SessionTrackingStrategy> sessionTrackingStrategy;
    private Properties properties;
    
    public SessionHandlingTestServlet(Class<? extends SessionTrackingStrategy> sessionTrackingStrategy, Properties properties) {
        this.properties = properties;
        this.sessionTrackingStrategy = sessionTrackingStrategy;
    }
    
    @Override
    public void init() throws ServletException {
        handler = new SessionHandlingTestHandler(properties);
        SessionTrackingStrategy strategy;
        try {
            strategy = sessionTrackingStrategy.newInstance();
        } catch (Exception x) {
            throw new ServletException("Can't instantiate SessionTrackingStrategy", x);
        }
        handler.setSessionTrackingStrategy(strategy);
        handler.setServletContext(getServletContext());
        handler.setTenantInfo(new TenantInfo());
        handler.setLanguageInfo(new LanguageInfo());
        try {
            handler.init();
        } catch(ServletException x) {
            throw new RuntimeException("Error initializing SessionHandlingTestHandler", x);
        }
        SessionAdmin sessionAdmin = new SessionAdmin();
        handler.setSessionAdmin(sessionAdmin);
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        handler.handleRequest(req, res);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    	doGet(req, res);
    }
    
}
