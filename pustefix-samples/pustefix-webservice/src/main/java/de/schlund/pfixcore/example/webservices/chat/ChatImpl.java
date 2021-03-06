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
package de.schlund.pfixcore.example.webservices.chat;

import javax.jws.WebService;

import org.pustefixframework.webservices.AbstractService;

/**
 * @author ml
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
@WebService
public class ChatImpl extends AbstractService implements Chat {

    public void login(String nickName) throws Exception {
        ContextChat cc=(ContextChat)this.getContextResource(ContextChat.class.getName());
        cc.login(nickName);
    }
    
    public void logout() throws Exception {
        ContextChat cc=(ContextChat)this.getContextResource(ContextChat.class.getName());
        cc.logout();
    }
    
    public void sendMessage(String txt,String[] recipients) throws Exception {
        ContextChat cc=(ContextChat)this.getContextResource(ContextChat.class.getName());
        cc.sendMessage(txt,recipients);
    }
    
    public Message[] getMessages() {
        ContextChat cc=(ContextChat)this.getContextResource(ContextChat.class.getName());
        return cc.getMessages();
    }
    
    public Message[] getLastMessages() {
        ContextChat cc=(ContextChat)this.getContextResource(ContextChat.class.getName());
        return cc.getLastMessages();
    }
    
    public String[] getNickNames() {
        ContextChat cc=(ContextChat)this.getContextResource(ContextChat.class.getName());
        return cc.getNickNames();
    }

}
