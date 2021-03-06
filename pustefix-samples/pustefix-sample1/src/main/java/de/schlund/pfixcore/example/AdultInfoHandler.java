/*
 * This file is part of Pustefix.
 *
 * Pustefix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Pustefix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Pustefix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.schlund.pfixcore.example;

import org.pustefixframework.web.mvc.InputHandler;
import org.springframework.beans.factory.annotation.Autowired;

import de.schlund.pfixcore.example.iwrapper.AdultInfo;

/**
 * AdultInfoHandler.java
 *
 *
 * Created: Thu Oct 18 18:53:20 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class AdultInfoHandler implements InputHandler<AdultInfo> {

    private ContextAdultInfo cai;

    /**
    @Autowired
    private ServletContext servletContext;
    @Autowired
    private PustefixContextXMLRequestHandler handler;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private Context context;
    */
    
    public void handleSubmittedData(AdultInfo info) {
        cai.setAdult(info.getAdult());
        cai.setDate(info.getDate());
        
        /**
        VirtualHttpServletRequest req = new VirtualHttpServletRequest(servletContext);
        req.setSession(request.getSession(false));
        req.setPathInfo("/encodingpage");
        req.setMethod("GET");
        VirtualHttpServletResponse res = new VirtualHttpServletResponse();
        RequestAttributes originalRequestAttributes = RequestContextHolder.getRequestAttributes();
        RequestContextImpl originalRequestContext = ((ContextImpl)context).getRequestContextForCurrentThread();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        try {
            handler.handleRequest(req, res);
            System.out.println(new String(res.getOutput()));
        } catch(Exception x) {
            //TODO: handle exception
            x.printStackTrace();
        } finally {
            RequestContextHolder.setRequestAttributes(originalRequestAttributes);
            ((ContextImpl)context).setRequestContextForCurrentThread(originalRequestContext);
        }
        */
    }
    
    public void retrieveCurrentStatus(AdultInfo info) {
        if (!cai.needsData()) {
            info.setAdult(cai.getAdult());
        }
        info.setDate(cai.getDate());
    }
    
    public boolean needsData() {
        return cai.needsData();
    }
    
    public boolean isActive() {
        return true;
    }
    
    public boolean prerequisitesMet() {
        return true;
    }

    @Autowired
    public void setContextAdultInfo(ContextAdultInfo cai) {
        this.cai = cai;
    }

}
