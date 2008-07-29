/*
 * This file is part of PFIXCORE.
 *
 * PFIXCORE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * PFIXCORE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PFIXCORE; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.pustefixframework.http.dereferer;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.pustefixframework.http.AbstractPustefixRequestHandler;

import de.schlund.pfixxml.PfixServletRequest;
import de.schlund.pfixxml.RequestParam;
import de.schlund.pfixxml.config.ServletManagerConfig;
import de.schlund.pfixxml.config.impl.ServletManagerConfigImpl;
import de.schlund.pfixxml.resources.FileResource;
import de.schlund.pfixxml.serverutil.SessionHelper;
import de.schlund.pfixxml.util.Base64Utils;
import de.schlund.pfixxml.util.MD5Utils;

/**
 * This class implements a "Dereferer" servlet to get rid of Referer
 * headers. <b>ALL LINKS THAT GO TO AN OUTSIDE DOMAIN MUST USE THIS SERVLET!</b>
 * If this servlet is bound to e.g. /xml/deref than every outside link
 * (say to http://www.gimp.org) must be written like <a href="/xml/deref?link=http://www.gimp.org">Gimp</a>
 *
 */

public class DerefRequestHandler extends AbstractPustefixRequestHandler {
   
    private static final long serialVersionUID = 4003807093421866709L;
    
    protected final static Logger   DEREFLOG        = Logger.getLogger("LOGGER_DEREF");
    protected final static Logger   LOG             = Logger.getLogger(DerefRequestHandler.class);
    public final static String PROP_DEREFKEY = "derefserver.signkey";
    public final static String PROP_IGNORESIGN = "derefserver.ignoresign";
    private ServletManagerConfig config;
    
    protected boolean allowSessionCreate() {
        return (false);
    }

    protected boolean needsSession() {
        return (false);
    }

    private String signString(String input, String key) {
        return SignUtil.signString(input, key);
    }

    private boolean checkSign(String input, String key, String sign) {
        if (input == null || sign == null) {
            return false;
        }
        return MD5Utils.hex_md5(input+key, "utf8").equals(sign);
    }
    
    protected void process(PfixServletRequest preq, HttpServletResponse res) throws Exception {
        RequestParam linkparam    = preq.getRequestParam("link");
        RequestParam enclinkparam = preq.getRequestParam("__enclink");
        RequestParam signparam    = preq.getRequestParam("__sign");
        String       key          = config.getProperties().getProperty(PROP_DEREFKEY);
        String       ign          = config.getProperties().getProperty(PROP_IGNORESIGN);

        // This is currently set to true by default for backward compatibility. 
        boolean ignore_nosign = true;
        if (ign != null && ign.equals("false")) {
            ignore_nosign = false;
        }

        HttpServletRequest req     = preq.getRequest();
        String             referer = req.getHeader("Referer");

        LOG.debug("===> sign key: " + key);
        LOG.debug("===> Referer: " + referer);
        
        if (linkparam != null && linkparam.getValue() != null) {
            LOG.debug(" ==> Handle link: " + linkparam.getValue());
            if (signparam != null && signparam.getValue() != null) {
                LOG.debug("     with sign: " + signparam.getValue());
            }
            handleLink(linkparam.getValue(), signparam, ignore_nosign, preq, res, key);
            return;
        } else if (enclinkparam != null && enclinkparam.getValue() != null &&
                   signparam != null && signparam.getValue() != null) {
            LOG.debug(" ==> Handle enclink: " + enclinkparam.getValue());
            LOG.debug("     with sign: " + signparam.getValue());
            handleEnclink(enclinkparam.getValue(), signparam.getValue(), preq, res, key);
            return;
        } else {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
    }


    private void handleLink(String link, RequestParam signparam, boolean ignore_nosign,
                            PfixServletRequest preq, HttpServletResponse res, String key) throws Exception {
        boolean checked = false;
        boolean signed  = false;
        boolean addallparams = false;
        
        if (link.startsWith("/") || link.startsWith("addallparams:/")) {
            // This is a "relative absolute" link, no other domain.
            // It doesn't matter if any JS tricks or other stuff is played here, because
            // the link will only be used in the second stage when we do relocate via 302
            ignore_nosign = true;
        }

        if  (signparam != null && signparam.getValue() != null) {
            signed = true;
        }
        if (signed && checkSign(link, key, signparam.getValue())) {
            checked = true;
        }

        if (link.startsWith("addallparams:")) {
            addallparams = true;
            link = link.substring("addallparams:".length());
        }        
        
        // We don't currently enforce the signing at this stage. We may change this to enforcing mode,
        // or maybe we will use some clear warning pages in the case of a not signed request.
        if (checked || (!signed && ignore_nosign)) {
            OutputStream       out    = res.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out, res.getCharacterEncoding());
            if (addallparams) {
                String[] allparamnames = preq.getRequestParamNames();
                StringBuffer urlextension = new StringBuffer();
                for (String tmpname : allparamnames) {
                    if (tmpname.equals("link") || tmpname.equals("__sign") || tmpname.equals("__enclink")) {
                        continue;
                    }
                    RequestParam tmpparam = preq.getRequestParam(tmpname);
                    if (tmpparam.getValue() != null) {
                        urlextension.append("&" + URLEncoder.encode(tmpname, preq.getRequest().getCharacterEncoding()) + "=" + 
                                URLEncoder.encode(tmpparam.getValue(), preq.getRequest().getCharacterEncoding()));
                    }
                }
                String urltail = urlextension.toString();
                if (urltail != null && urltail.length() > 0) {
                    if (link.contains("?")) {
                        link = link + urlextension;
                    } else {
                        link = link + "?" + urlextension.substring(1);
                    }
                }
            }
            String             enclink  = Base64Utils.encode(link.getBytes("utf8"),false);
            String             reallink = getServerURL(preq) +
                SessionHelper.getClearedURI(preq) + "?__enclink=" + URLEncoder.encode(enclink, "utf8") +
                "&__sign=" + signString(enclink, key);
            
            LOG.debug("===> Meta refresh to link: " + reallink);
            DEREFLOG.info(preq.getServerName() + "|" + link + "|" + preq.getRequest().getHeader("Referer"));
            
            writer.write("<html><head>");
            writer.write("<meta http-equiv=\"refresh\" content=\"0; URL=" + reallink +  "\">");

            writer.write("<script language=\"JavaScript\" type=\"text/javascript\">\n");
            writer.write("<!--\n");
            writer.write("function redirect() { setTimeout(\"window.location.replace('" + reallink + "')\", 10); }\n");
            writer.write("-->\n");
            writer.write("</script>\n");
            
            writer.write("</head><body onload=\"redirect()\" bgcolor=\"#ffffff\"><center>");
            writer.write("<a style=\"color:#cccccc;\" href=\"" + reallink + "\">" + "-> Redirect ->" + "</a>");
            writer.write("</center></body></html>");
            writer.flush();
        } else {
            LOG.warn("===> No meta refresh because signature is wrong.");
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
    }
    
    private String getServerURL(PfixServletRequest preq) {
        String url = preq.getScheme() + "://" + preq.getServerName();
        //only add port if non-default
        if (!((preq.getScheme().equals("http") && preq.getServerPort() == 80) || (preq.getScheme().equals("https") && preq.getServerPort() == 443))) {
            url += ":" + preq.getServerPort();
        }
        return url;
    }

    private void handleEnclink(String enclink, String sign, PfixServletRequest preq, HttpServletResponse res, String key) throws Exception {
        if (checkSign(enclink, key, sign)) {
            String link = new String( Base64Utils.decode(enclink), "utf8");
            if (link.startsWith("/")) {
                link = getServerURL(preq) + link;
            }
            LOG.debug("===> Relocate to link: " + link);

            res.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
            res.setHeader("Pragma", "no-cache");
            res.setHeader("Cache-Control", "no-cache, no-store, private, must-revalidate");
            res.setHeader("Location", link);
            res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        } else {
            LOG.warn("===> Won't relocate because signature is wrong.");
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
    }
    
    protected ServletManagerConfig getServletManagerConfig() {
        return this.config;
    }

    protected void reloadServletConfig(FileResource configFile, Properties globalProperties) throws ServletException {
        // Deref server does not use a servlet specific configuration
        // So simply initialize configuration with global properties
        ServletManagerConfigImpl sConf = new ServletManagerConfigImpl();
        sConf.setProperties(globalProperties);
        sConf.setSSL(false);
        this.config = sConf;
    }
    
}