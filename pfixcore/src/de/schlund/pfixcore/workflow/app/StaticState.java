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

package de.schlund.pfixcore.workflow.app;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import de.schlund.pfixcore.util.PropertiesUtils;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.pfixcore.workflow.ContextResource;
import de.schlund.pfixcore.workflow.ContextResourceManager;
import de.schlund.pfixcore.workflow.StateImpl;
import de.schlund.pfixxml.PfixServletRequest;
import de.schlund.pfixxml.ResultDocument;
import de.schlund.pfixxml.XMLException;

/**
 * StaticState.java
 *
 *
 * Created: Wed Oct 10 09:50:19 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class StaticState extends StateImpl {
    public static final String PROP_INSERTCR = "insertcr";

    /**
     * @see de.schlund.pfixcore.workflow.State#getDocument(Context, PfixServletRequest)
     */
    public ResultDocument getDocument(Context context, PfixServletRequest preq) throws Exception {
        ResultDocument  resdoc = new ResultDocument();
        renderContextResources(context, resdoc);
        return resdoc;
    }

    
    /**
     * Method renderContextResources.
     * @param context
     * @param resdoc
     * @throws Exception
     */
    public void renderContextResources(Context context, ResultDocument resdoc) throws Exception {
        Properties props  = context.getPropertiesForCurrentPageRequest();
        if (props != null) {
            ContextResourceManager crm = context.getContextResourceManager();
            HashMap                crs = PropertiesUtils.selectProperties(props, PROP_INSERTCR);
            if (crs != null) {
                for (Iterator i = crs.keySet().iterator(); i.hasNext();) {
                    String nodename  = (String) i.next();
                    String classname = (String) crs.get(nodename);
                    if(CAT.isDebugEnabled()) {
                        CAT.debug("*** Auto appending status for " + classname + " at node " + nodename);
                    }
                    ContextResource cr = crm.getResource(classname);
                    
                    if (cr == null) {
                        throw new XMLException("ContextResource not found: " + classname);
                    }
                    
                    cr.insertStatus(resdoc, resdoc.createNode(nodename));
                }
            }
        }
    }

}// StaticState
