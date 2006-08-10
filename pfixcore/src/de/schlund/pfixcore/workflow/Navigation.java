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

package de.schlund.pfixcore.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.schlund.pfixxml.resources.FileResource;
import de.schlund.pfixxml.util.XPath;
import de.schlund.pfixxml.util.Xml;

public class Navigation {
    private Category CAT = Category.getInstance(Navigation.class.getName());

    private NavigationElement                   pageroot = new NavigationElement("__NONE__", "__NONE__");
    private Map<String, NavigationElement> pagetonavi;
    
    public Navigation(FileResource navifile) throws Exception {
        Document navitree = Xml.parseMutable(navifile);
        List     nl       = XPath.select(navitree, "/make/navigation/page");
        pagetonavi        = new HashMap<String, NavigationElement>();
        recursePagetree(pageroot, nl);
    }

    private void recursePagetree(NavigationElement parent, List nl) throws Exception {
        for (int i = 0; i < nl.size(); i++) {
            Element page    = (Element) nl.get(i);
            String  name    = page.getAttribute("name");
            String  handler = page.getAttribute("handler");
            
            NavigationElement elem = new NavigationElement(name, handler);
            pagetonavi.put(name, elem);
            parent.addChild(elem);
            List tmp = XPath.select(page, "./page");
            if (tmp.size() > 0) {
                recursePagetree(elem, tmp);
            }
        }
    }
    
    public NavigationElement[] getNavigationElements() {
        return pageroot.getChildren();
    }

    public NavigationElement getNavigationElementForPageRequest(PageRequest page) {
        return pagetonavi.get(page.getRootName());
    }

    public class NavigationElement {
        private ArrayList children = new ArrayList();
        private String    name;
        private String    handler;
        
        public NavigationElement (String name, String handler) {
            this.name = name;
            this.handler = handler;
        }
        
        public void addChild(NavigationElement elem) {
            children.add(elem);  
        }
        
        public String getName() {
            return name;
        }
        
        public String getHandler() {
            return handler;
        }
        
        public boolean hasChildren() {
            return !children.isEmpty();
        }
        
        public NavigationElement[] getChildren() {
            return (NavigationElement[]) children.toArray(new NavigationElement[] {});
        }
    } // NavigationElement
}
