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
package org.pustefixframework.tutorial.firstapp.contextresources;

import org.pustefixframework.tutorial.firstapp.User;
import org.w3c.dom.Element;

import de.schlund.pfixcore.beans.InsertStatus;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.pfixxml.ResultDocument;

public class ContextUserImpl implements ContextUser {

    private User user;
    
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void init(Context context) throws Exception {
        // nothing to do here

    }

    @InsertStatus
    public void insertStatus(ResultDocument document, Element element)
            throws Exception {
        if (this.user == null) {
            return;
        }
        ResultDocument.addTextChild(element, "sex", this.user.getSex());
        ResultDocument.addTextChild(element, "name", this.user.getName());
        ResultDocument.addTextChild(element, "email", this.user.getEmail());
        ResultDocument.addTextChild(element, "homepage", String.valueOf(this.user.getHomepage()));
        ResultDocument.addTextChild(element, "birthday", String.valueOf(this.user.getBirthday()));
        ResultDocument.addTextChild(element, "admin", String.valueOf(this.user.getAdmin()));
    }
}