package org.pustefixframework.http;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.schlund.pfixxml.PfixServletRequest;
import de.schlund.pfixxml.PfixServletRequestImpl;
import de.schlund.pfixxml.serverutil.SessionAdmin;
import de.schlund.pfixxml.serverutil.SessionHelper;
import de.schlund.pfixxml.serverutil.SessionInfoStruct;
import de.schlund.pfixxml.serverutil.SessionInfoStruct.TrailElement;
import de.schlund.pfixxml.util.CookieUtils;

public class CookieSessionTrackingStrategy implements SessionTrackingStrategy {

    private Logger LOG = Logger.getLogger(CookieSessionTrackingStrategy.class);
    private Logger LOGGER_SESSION = Logger.getLogger("LOGGER_SESSION");
    
    private static final String CHECK_FOR_RUNNING_SSL_SESSION = "__CHECK_FOR_RUNNING_SSL_SESSION__";
    private static final String PARAM_FORCELOCAL = "__forcelocal";
    private static final String STORED_REQUEST = "__STORED_PFIXSERVLETREQUEST__";
    private static final String INITIAL_SESSION_CHECK = "__INITIAL_SESSION_CHECK__";
    private static final String COOKIE_SESSION_RESET = "__PFIX_RST_";
    //Cookie indicates that session under HTTPS exists. That's necessary because HTTPS sessions have secure flag set
    //and going back in the browser to a non-HTTPS session won't send the session cookie
    private static final String COOKIE_SESSION_SSL = "__PFIX_SSL_";
    private static final String COOKIE_TEST = "__PFIX_TST_";
    
    private SessionTrackingStrategyContext context;
    
    public void init(SessionTrackingStrategyContext context) {
        this.context = context;
    }
    
    public void handleRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        
        HttpSession session = null;
        boolean has_session = false;
        boolean has_ssl_session_insecure = false;
        boolean has_ssl_session_secure = false;
        boolean force_jump_back_to_ssl = false;
        boolean force_reuse_visit_id = false;
        boolean does_cookies = false;
        boolean used_ssl = false;
        
        Cookie[] cookies = CookieUtils.getCookies(req);
        if(cookies == null) {
            addTestCookie(req, res);
        }
        
        used_ssl = (getCookie(cookies, COOKIE_SESSION_SSL) != null);
        
        if (req.isRequestedSessionIdValid()) {
            session = req.getSession(false);
            has_session = true;
            LOG.debug("*** Found valid session with ID " + session.getId());

            if(session.getAttribute(INITIAL_SESSION_CHECK) != null && req.isRequestedSessionIdFromCookie()) {
                session.removeAttribute(INITIAL_SESSION_CHECK);
                session.setAttribute(AbstractPustefixRequestHandler.SESSION_ATTR_COOKIE_SESSION, true);
                String redirect_uri = SessionHelper.getClearedURL(req.getScheme(), AbstractPustefixRequestHandler.getServerName(req), req, context.getServletManagerConfig().getProperties());
                AbstractPustefixRequestHandler.relocate(res, HttpServletResponse.SC_MOVED_TEMPORARILY, redirect_uri);
                return;
            }
            
            Boolean secure = (Boolean) session.getAttribute(SessionAdmin.SESSION_IS_SECURE);

            if (has_session) {
                if (req.isSecure()) {
                    LOG.debug("*** Found running under SSL");
                    if (secure != null && secure.booleanValue()) {
                        if(req.isRequestedSessionIdFromURL()) {
                            if(cookies != null) {
                                LOG.debug("*** Found secure session in URL but cookies enabled => Destroying session.");
                                LOGGER_SESSION.info("Invalidate session V: " + session.getId());
                                if(LOGGER_SESSION.isDebugEnabled()) LOGGER_SESSION.debug(dumpRequest(req));
                                SessionUtils.invalidate(session);
                                has_session = false;
                            } else {
                                boolean ok = AbstractPustefixRequestHandler.checkClientIdentity(req);
                                if(!ok) {
                                    LOG.warn("Invalidate session " + session.getId() + " because client identity changed!");
                                    LOGGER_SESSION.info("Invalidate session VI: " + session.getId() + dumpRequest(req));
                                    SessionUtils.invalidate(session);
                                    has_session = false;
                                } else {
                                    has_ssl_session_secure = true;
                                }
                            }
                        } else {
                            has_ssl_session_secure = true;
                        }
                    } else {
                        LOG.debug("    ... but session is insecure!");
                        has_ssl_session_insecure = true;
                    }
                } else if (secure != null && secure.booleanValue()) {
                    LOG.debug("*** Found secure session but NOT running under SSL => Destroying session.");
                    LOGGER_SESSION.info("Invalidate session I: " + session.getId());
                    if(LOGGER_SESSION.isDebugEnabled()) LOGGER_SESSION.debug(dumpRequest(req));
                    SessionUtils.invalidate(session);
                    has_session = false;
                }
            }
        } else if (req.getRequestedSessionId() != null && context.wantsCheckSessionIdValid()) {
            LOG.debug("*** Found old and invalid session in request: " + req.getRequestedSessionId());
            if(LOG.isDebugEnabled()) LOG.debug(dumpRequest(req));
            // We have no valid session, but the request contained an invalid session id.
            // case a) This may be an invalid id because we invalidated the session when jumping
            // into the secure SSL session (see redirectToSecureSSLSession below). by using the back button
            // of the browser, the user may have come back to a (non-ssl) page (in his browser history) that contains
            // links with the old "parent" session id embedded. We need to check for this and create a
            // new session but reuse the visit id of the currently running SSL session.
            if (!req.isSecure() && context.getSessionAdmin().idWasParentSession(req.getRequestedSessionId())) {
                LOG.debug("    ... but this session was the parent of a currently running secure session.");
                HttpSession secure_session = context.getSessionAdmin().getChildSessionForParentId(req.getRequestedSessionId());
                if (secure_session != null) {
                    does_cookies = (cookies != null);
                }
                // We'll try to get back there securely by first jumping back to a new (insecure) SSL session,
                // and after that the the jump to the secure SSL session will not create a new one, but reuse
                // the already running secure session instead (but only if a secure cookie can identify the request as
                // coming from the browser that made the initial jump http->https).
                if (does_cookies) {
                    LOG.debug("    ... client handles cookies, so we'll check if we can reuse the parent session.");
                    force_jump_back_to_ssl = true;
                } else {
                    // OK, it seems as if we will not be able to identify the peer by comparing cookies.
                    // So the only thing we can do is to reuse the VISIT_ID.
                    LOG.debug("    ... but can't reuse the secure session because the client doesn't handle cookies.");
                    force_reuse_visit_id = true;
                }
            } else {
                // Normally the balancer (or, more accurate: mod_jk) has a chance to choose the right server for a 
                // new session, but with a session id in the URL it wasn't able to. So we redirect to a "fresh" request 
                // without _any_ id, giving the balancer the possibility to choose a different server. (this can be 
                // overridden by supplying the parameter __forcelocal=1 to the request). All this makes only sense of 
                // course if we are running in a cluster of servers behind a balancer that chooses the right server
                // based on the session id included in the URL.
                String forcelocal = req.getParameter(PARAM_FORCELOCAL);
                String active = (String)req.getAttribute("JK_LB_ACTIVATION");
                if (forcelocal != null && (forcelocal.equals("1") || forcelocal.equals("true") || forcelocal.equals("yes"))) {
                    LOG.debug("    ... but found __forcelocal parameter to be set.");
                } else if(req.getMethod().equals("POST") && (active == null || active.equals("ACT"))) {
                    LOG.debug("    ... but is POST.");
                } else {
                    boolean resetTry = false;
                    if(cookies != null) {
                        Cookie cookie = getCookie(cookies, COOKIE_SESSION_RESET);
                        if(cookie != null && cookie.getValue().equals(req.getRequestedSessionId())) resetTry = true;
                    }
                    if(!resetTry) {
                        LOG.debug("    ... and __forcelocal is NOT set.");
                        redirectToClearedRequest(req, res, used_ssl);
                        return;
                    }
                    // End of request cycle.
                }
            }
        }

        PfixServletRequest preq = null;
        try {

        if (has_session) {
            preq = (PfixServletRequest) session.getAttribute(STORED_REQUEST);
            if (preq != null) {
                LOG.debug("*** Found old PfixServletRequest object in session");
                session.removeAttribute(STORED_REQUEST);
                preq.updateRequest(req);
            }
        }
        if (preq == null) {
            LOG.debug("*** Creating PfixServletRequest object.");
            preq = new PfixServletRequestImpl(req, context.getServletManagerConfig().getProperties(), context);
        }

        //TODO: call it later
        //PustefixInit.tryReloadLog4j();
        
        // End of initialization. Now we handle all cases where we need to redirect.

        if (force_jump_back_to_ssl && context.allowSessionCreate()) {
            LOG.debug("=> I");
            forceRedirectBackToInsecureSSL(preq, req, res);
            return;
            // End of request cycle.
        }
        if (force_reuse_visit_id && context.allowSessionCreate()) {
            LOG.debug("=> II");
            forceNewSessionSameVisit(preq, req, res);
            return;
            // End of request cycle.
        }
        if (has_ssl_session_insecure) {
            LOG.debug("=> III");
            redirectToSecureSSLSession(preq, req, res);
            return;
            // End of request cycle.
        }
        if (!has_session && used_ssl && !req.isSecure()) {
            LOG.debug("=> VII");
            redirectToSSL(req, res);
            return;
        }
        if (context.needsSession() && context.allowSessionCreate() && context.needsSSL(preq) && !has_ssl_session_secure) {  
            if(req.isSecure()) {
                LOG.debug("=> IVa");
                redirectToSSLSession(preq, req, res);
            } else {
                LOG.debug("=> IVb");
                redirectToInsecureSSLSession(preq, req, res);
            }
            return;
            // End of request cycle.
        }
        if (!has_session && context.needsSession() && context.allowSessionCreate() && !context.needsSSL(preq)) {
            LOG.debug("=> V");
            if(req.isSecure()) {
            	redirectToSSLSession(preq, req, res);
            } else {
            	redirectToSession(preq, req, res);
            }
            return;
            // End of request cycle.
        }
        if (!has_session && !context.needsSession() && context.needsSSL(preq) && !req.isSecure()) {
            LOG.debug("=> VI");
            redirectToSSL(req, res);
            return;
            // End of request cycle.
        }

        LOG.debug("*** >>> End of redirection management, handling request now.... <<< ***\n");

        if(session != null) {
            if(session.getAttribute(AbstractPustefixRequestHandler.VISIT_ID) == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Found session without visit_id: ");
                sb.append(req.getRemoteAddr()).append("|");
                sb.append(req.getRequestURI()).append("|");
                sb.append(session.getId()).append("|");
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
                sb.append(format.format(new Date(session.getCreationTime()))).append("|");
                sb.append(format.format(new Date(session.getLastAccessedTime()))).append("|");
                Enumeration<?> e = session.getAttributeNames();
                while(e.hasMoreElements()) sb.append(e.nextElement()).append("|");
                e = req.getHeaderNames();
                while(e.hasMoreElements()) {
                    String name = (String)e.nextElement();
                    Enumeration<?> v = req.getHeaders(name);
                    while(v.hasMoreElements()) {
                        String value = (String)v.nextElement();
                        sb.append(name).append(":").append(value).append("|");
                    }
                }
                LOG.warn(sb.toString());
                LOGGER_SESSION.info("Invalidate session II: " + session.getId());
                if(LOGGER_SESSION.isDebugEnabled()) LOGGER_SESSION.debug(dumpRequest(req));
                SessionUtils.invalidate(session);
                redirectToClearedRequest(req, res, used_ssl);
                return;
            }
        }
        
        context.callProcess(preq, req, res);
        
        } finally {
            if(preq != null) {
                preq.resetRequest();
            }
        }
    }

    private void redirectToClearedRequest(HttpServletRequest req, HttpServletResponse res, boolean usedSSL) {
        LOG.debug("===> Redirecting to cleared Request URL");
        String redirect_uri = SessionHelper.getClearedURL(req.getScheme(), AbstractPustefixRequestHandler.getServerName(req), req, context.getServletManagerConfig().getProperties());
        if(req.isRequestedSessionIdFromCookie()) {
            Cookie cookie = new Cookie(AbstractPustefixRequestHandler.getSessionCookieName(req), "");
            cookie.setMaxAge(0);
            cookie.setPath((req.getContextPath().equals("")) ? "/" : req.getContextPath());
            res.addCookie(cookie);
            Cookie resetCookie = new Cookie(COOKIE_SESSION_RESET, req.getRequestedSessionId());
            resetCookie.setMaxAge(60);
            resetCookie.setPath((req.getContextPath().equals("")) ? "/" : req.getContextPath());
            res.addCookie(resetCookie);
            if(usedSSL) {
	            Cookie sslCookie = new Cookie(COOKIE_SESSION_SSL, "");
	            sslCookie.setMaxAge(0);
	            sslCookie.setPath((req.getContextPath().equals("")) ? "/" : req.getContextPath());
	            res.addCookie(sslCookie);
            }
        }
        AbstractPustefixRequestHandler.relocate(res, HttpServletResponse.SC_MOVED_PERMANENTLY, redirect_uri);
    }
    
    private void redirectToSSL(HttpServletRequest req, HttpServletResponse res) {
        LOG.debug("===> Redirecting to session-less request URL under SSL");
        String redirect_uri = SessionHelper.getClearedURL("https", AbstractPustefixRequestHandler.getServerName(req), req, context.getServletManagerConfig().getProperties());
        AbstractPustefixRequestHandler.relocate(res, redirect_uri);
    }
    
    private void redirectToInsecureSSLSession(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        boolean reuse_session = false;
        if (req.isRequestedSessionIdValid()) {
            reuse_session = true;
            LOG.debug("*** reusing existing session for jump http=>https");
        }
        HttpSession session = req.getSession(true);
        AbstractPustefixRequestHandler.storeClientIdentity(req);
        LOGGER_SESSION.info("Get session I: " + session.getId());
        if (!reuse_session) {
            context.registerSession(req, session);
        }

        LOG.debug("*** Setting INSECURE flag in session (Id: " + session.getId() + ")");
        session.setAttribute(SessionAdmin.SESSION_IS_SECURE, Boolean.FALSE);
        session.setAttribute(STORED_REQUEST, preq);

        LOG.debug("===> Redirecting to insecure SSL URL with session (Id: " + session.getId() + ")");
        String redirect_uri = SessionHelper.encodeURL("https", AbstractPustefixRequestHandler.getServerName(req), req, context.getServletManagerConfig().getProperties());
        AbstractPustefixRequestHandler.relocate(res, redirect_uri);
    }
    
    private void redirectToSession(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        HttpSession session = req.getSession(true);
        AbstractPustefixRequestHandler.storeClientIdentity(req);
        LOGGER_SESSION.info("Get session II: " + session.getId());
        context.registerSession(req, session);
        LOG.debug("===> Redirecting to URL with session (Id: " + session.getId() + ")");
        session.setAttribute(STORED_REQUEST, preq);
        session.setAttribute(INITIAL_SESSION_CHECK, session.getId());
        String redirect_uri = SessionHelper.encodeURL(req.getScheme(), AbstractPustefixRequestHandler.getServerName(req), req, context.getServletManagerConfig().getProperties());
        AbstractPustefixRequestHandler.relocate(res, redirect_uri);
    }
    
    private void redirectToSSLSession(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        HttpSession session = req.getSession(true);
        AbstractPustefixRequestHandler.storeClientIdentity(req);
        LOGGER_SESSION.info("Get session III: " + session.getId());
        context.registerSession(req, session);

        LOG.debug("===> Redirecting to URL with session (Id: " + session.getId() + ")");
        session.setAttribute(STORED_REQUEST, preq);
        session.setAttribute(INITIAL_SESSION_CHECK, session.getId());
        session.setAttribute(SessionAdmin.SESSION_IS_SECURE, Boolean.TRUE);
        addSSLCookie(req, res);
        String redirect_uri = SessionHelper.encodeURL("https", AbstractPustefixRequestHandler.getServerName(req), req, context.getServletManagerConfig().getProperties());
        AbstractPustefixRequestHandler.relocate(res, redirect_uri);
    }
    
    private void redirectToSecureSSLSession(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        HttpSession session = req.getSession(false);
        String visit_id = (String) session.getAttribute(AbstractPustefixRequestHandler.VISIT_ID);
        
        LOG.debug("*** Saving session data...");
        HashMap<String, Object> map = new HashMap<String, Object>();
        SessionHelper.saveSessionData(map, session);
        // Before we invalidate the current session we save the traillog
        SessionInfoStruct infostruct = context.getSessionAdmin().getInfo(session);
        LinkedList<TrailElement> traillog = new LinkedList<TrailElement>();
        String old_id = session.getId();
        if (infostruct != null) {
            traillog = context.getSessionAdmin().getInfo(session).getTraillog();
        } else {
            LOG.warn("*** Infostruct == NULL ***");
        }

        LOG.debug("*** Invalidation old session (Id: " + old_id + ")");
        LOGGER_SESSION.info("Invalidate session III: " + session.getId());
        if(LOGGER_SESSION.isDebugEnabled()) LOGGER_SESSION.debug(dumpRequest(req));
        SessionUtils.invalidate(session);
        session = req.getSession(true);
        AbstractPustefixRequestHandler.storeClientIdentity(req);
        LOGGER_SESSION.info("Get session IV: " + session.getId());

        // First of all we put the old session id into the new session (__PARENT_SESSION_ID__)
        session.setAttribute(SessionAdmin.PARENT_SESS_ID, old_id);
        if (visit_id != null) {
            // Don't call this.registerSession(...) here. We don't want to log this as a different visit.
            // Now we register the new session with saved traillog
            context.getSessionAdmin().registerSession(session, traillog, infostruct.getData().getServerName(), infostruct.getData().getRemoteAddr());
        } else {
            // Register a new session now.
            context.registerSession(req, session);
        }
        LOG.debug("*** Got new Session (Id: " + session.getId() + ")");
        LOG.debug("*** Copying data back to new session");
        SessionHelper.copySessionData(map, session);
        LOG.debug("*** Setting SECURE flag");
        session.setAttribute(SessionAdmin.SESSION_IS_SECURE, Boolean.TRUE);
        addSSLCookie(req, res);
        session.setAttribute(STORED_REQUEST, preq);
        session.setAttribute(INITIAL_SESSION_CHECK, session.getId());

        LOG.debug("===> Redirecting to secure SSL URL with session (Id: " + session.getId() + ")");
        String redirect_uri = SessionHelper.encodeURL("https", AbstractPustefixRequestHandler.getServerName(req), req, context.getServletManagerConfig().getProperties());
        AbstractPustefixRequestHandler.relocate(res, redirect_uri);
    }
        
    private void forceRedirectBackToInsecureSSL(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        // When we come here, we KNOW that there's a secure SSL session already running, so this session here is
        // only used for the jump to SSL so we can get the cookie to check the identity of the caller.
        String parentid = req.getRequestedSessionId();
        HttpSession session = req.getSession(true);
        AbstractPustefixRequestHandler.storeClientIdentity(req);
        LOGGER_SESSION.info("Get session V: " + session.getId());
        session.setAttribute(CHECK_FOR_RUNNING_SSL_SESSION, parentid);
        LOG.debug("*** Setting INSECURE flag in session (Id: " + session.getId() + ")");
        session.setAttribute(SessionAdmin.SESSION_IS_SECURE, Boolean.FALSE);
        session.setAttribute(STORED_REQUEST, preq);

        LOG.debug("===> Redirecting to SSL URL with session (Id: " + session.getId() + ")");
        String redirect_uri = SessionHelper.encodeURL("https", AbstractPustefixRequestHandler.getServerName(req), req, context.getServletManagerConfig().getProperties());
        AbstractPustefixRequestHandler.relocate(res, redirect_uri);
    }
    
    private void forceNewSessionSameVisit(PfixServletRequest preq, HttpServletRequest req, HttpServletResponse res) {
        // When we come here, we KNOW that there's a secure SSL session already running, but unfortunately
        // it seems that the browser doesn't send cookies. So we will not be able to know for sure that the request comes
        // from the legitimate user. The only thing we can do is to copy the VISIT_ID, which helps to keep the
        // statistic clean :-)
        String parentid = req.getRequestedSessionId();
        HttpSession child = context.getSessionAdmin().getChildSessionForParentId(parentid);
        String curr_visit_id = (String) child.getAttribute(AbstractPustefixRequestHandler.VISIT_ID);
        HttpSession session = req.getSession(true);
        AbstractPustefixRequestHandler.storeClientIdentity(req);
        LOGGER_SESSION.info("Get session VI: " + session.getId());

        LinkedList<TrailElement> traillog = context.getSessionAdmin().getInfo(child).getTraillog();
        session.setAttribute(AbstractPustefixRequestHandler.VISIT_ID, curr_visit_id);
        context.getSessionAdmin().registerSession(session, traillog, AbstractPustefixRequestHandler.getServerName(req), req.getRemoteAddr());
        LOG.debug("===> Redirecting with session (Id: " + session.getId() + ") using OLD VISIT_ID: " + curr_visit_id);
        session.setAttribute(STORED_REQUEST, preq);
        String redirect_uri = SessionHelper.encodeURL(req.getScheme(), AbstractPustefixRequestHandler.getServerName(req), req, context.getServletManagerConfig().getProperties());
        AbstractPustefixRequestHandler.relocate(res, redirect_uri);
    }
    
    private static void addSSLCookie(HttpServletRequest req, HttpServletResponse res) {
        Cookie resetCookie = new Cookie(COOKIE_SESSION_SSL, "true");
        resetCookie.setMaxAge(-1);
        resetCookie.setPath((req.getContextPath().equals("")) ? "/" : req.getContextPath());
        res.addCookie(resetCookie);
    }
    
    private static void addTestCookie(HttpServletRequest req, HttpServletResponse res) {
        Cookie resetCookie = new Cookie(COOKIE_TEST, "true");
        resetCookie.setMaxAge(-1);
        resetCookie.setPath((req.getContextPath().equals("")) ? "/" : req.getContextPath());
        res.addCookie(resetCookie);
    }
    
    private static Cookie getCookie(Cookie[] cookies, String name) {
        if(cookies != null) {
            for(Cookie cookie: cookies) {
                if(cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }
    
    private static String dumpRequest(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(req.getMethod()).append("|").append(req.getRequestURI()).append("|");
        sb.append(req.getQueryString() == null?"-":req.getQueryString()).append("|");
        sb.append(req.getRequestedSessionId()).append("|").append(req.getProtocol()).append("|");
        sb.append(req.getScheme()).append("|").append(req.getRemoteAddr()).append("|");
        sb.append(req.getServerName()).append("\n");
        Enumeration<?> headers = req.getHeaderNames();
        while(headers.hasMoreElements()) {
            String header = (String)headers.nextElement();
            Enumeration<?> headerValues = req.getHeaders(header);
            while(headerValues.hasMoreElements()) {
                String value = (String)headerValues.nextElement();
                sb.append(header).append(": ").append(value).append("\n");
            }
        }
        return sb.toString();
    }
    
}
