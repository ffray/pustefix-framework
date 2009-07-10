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

package org.pustefixframework.config.contextxmlservice.parser;

import org.pustefixframework.config.contextxmlservice.parser.internal.ContextXMLServletConfigImpl;
import org.pustefixframework.config.generic.ParsingUtils;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.w3c.dom.Element;

import com.marsching.flexiparse.parser.HandlerContext;
import com.marsching.flexiparse.parser.ParsingHandler;
import com.marsching.flexiparse.parser.exception.ParserException;

import de.schlund.pfixcore.auth.RoleProvider;

/**
 * 
 * @author mleidig
 *
 */
public class RoleProviderParsingHandler implements ParsingHandler {

    public void handleNode(HandlerContext context) throws ParserException {
       
        Element element = (Element)context.getNode();
        ParsingUtils.checkAttributes(element, new String[] {"class"}, null);
        
        ConfigurableOsgiBundleApplicationContext appContext = ParsingUtils.getSingleTopObject(ConfigurableOsgiBundleApplicationContext.class, context);

        ContextXMLServletConfigImpl config = ParsingUtils.getSingleTopObject(ContextXMLServletConfigImpl.class, context);     
        
        RoleProvider roleProvider;
        String className = element.getAttribute("class").trim();
        if (className == null || className.trim().equals("")) throw new ParserException("Element 'roleprovider' requires 'class' attribute.");
        try {
            Class<?> clazz = Class.forName(className, true, appContext.getClassLoader());
            if (!RoleProvider.class.isAssignableFrom(clazz))
                throw new ParserException("Class '" + className + "' doesn't implement the RoleProvider interface.");
            roleProvider = (RoleProvider) clazz.newInstance();
            config.getContextConfig().setCustomRoleProvider(roleProvider);
            context.getObjectTreeElement().addObject(roleProvider);
        } catch (ClassNotFoundException x) {
            throw new ParserException("RoleProvider class not found: " + className);
        } catch (InstantiationException x) {
            throw new ParserException("RoleProvider class can't be instantiated: " + className, x);
        } catch (IllegalAccessException x) {
            throw new ParserException("RoleProvider class can't be instantiated: " + className, x);
        }
        
        PropertyParsingUtils.setProperties(roleProvider, element);
    }

}