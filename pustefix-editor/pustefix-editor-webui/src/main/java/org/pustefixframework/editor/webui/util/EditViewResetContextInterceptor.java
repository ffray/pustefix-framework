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
 */

package org.pustefixframework.editor.webui.util;

import org.pustefixframework.container.annotations.Inject;
import org.pustefixframework.editor.webui.resources.SessionResource;

import de.schlund.pfixcore.generator.IWrapper;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.pfixcore.workflow.ContextInterceptor;
import de.schlund.pfixxml.PfixServletRequest;

/**
 * This interceptor is used to reset the flag, which defines wheter
 * a user is currently editing an include part.
 * The flag is stored in {@link org.pustefixframework.editor.webui.resources.SessionResource}
 * and set by {@link org.pustefixframework.editor.webui.handlers.CommonUploadIncludePartHandler#retrieveCurrentStatus(Context, IWrapper)},
 * which effectively means, it is set to <code>false</code> when any page requested and back
 * to <code>true</code> if a page containing the editor form is loaded (and actually shows some
 * content).
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class EditViewResetContextInterceptor implements ContextInterceptor {
    
    private SessionResource sessionResource;
    
    @Inject
    public void setSessionResource(SessionResource sessionResource) {
        this.sessionResource = sessionResource;
    }
    
    public void process(Context context, PfixServletRequest preq) {
        sessionResource.setInIncludeEditView(false);
    }
    
}