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
package org.pustefixframework.resource.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pustefixframework.config.Constants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * 
 * @author mleidig@schlund.de
 *
 */
public class DynamicIncludeInfoImpl implements DynamicIncludeInfo {
    
    private String name;
    private int dynamicSearchLevel;
    private Map<String,Set<String>> overrideMap = new HashMap<String,Set<String>>();
    
    public DynamicIncludeInfoImpl(String name, int dynamicSearchLevel) {
        this.name = name;
        this.dynamicSearchLevel = dynamicSearchLevel;
    }
    
    public String getModuleName() {
        return name;
    }
    
    public int getDynamicSearchLevel() {
    	return dynamicSearchLevel;
    }
    
    public void addOverridedResource(String module, String resourcePath) {
        Set<String> resList = overrideMap.get(module);
        if(resList == null) {
            resList = new HashSet<String>();
            overrideMap.put(module,resList);
        }
        resList.add(resourcePath);
    }
    
    public Set<String> getOverridedResources(String module) {
        return overrideMap.get(module);
    }
    
    public boolean overridesResource(String module, String path) {
        Set<String> overrides = overrideMap.get(module);
        if(overrides != null) {
        	return overrides.contains(path);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "MODULE " + name;
    }
        
    public static DynamicIncludeInfoImpl create(String moduleName, Element element) {
    	DynamicIncludeInfoImpl dynInfo;
        if(element.getNamespaceURI().equals(Constants.NS_MODULE) && element.getLocalName().equals("dynamic-includes")) {
        	int level = -1;
        	Element searchElem = getSingleChildElement(element, Constants.NS_MODULE, "auto-search", false);
        	if(searchElem != null) {
        		String levelStr = searchElem.getAttribute("level").trim();
        		if(levelStr.length() > 0) level = Integer.parseInt(levelStr);
        	}
        	dynInfo = new DynamicIncludeInfoImpl(moduleName, level);
        	Element overrideElem = getSingleChildElement(element, Constants.NS_MODULE, "override-modules", false);
        	if(overrideElem != null) {	
	        	List<Element> modElems = getChildElements(overrideElem, Constants.NS_MODULE, "module");
	        	for(Element modElem:modElems) {
	        		String modName = modElem.getAttribute("name").trim();
	        		if(modName.equals("")) throw new IllegalArgumentException("Element 'module' requires 'name' attribute!");
	        		List<Element> resElems = getChildElements(modElem, Constants.NS_MODULE, "resource");
	        		for(Element resElem:resElems) {
	        			String resPath = resElem.getAttribute("path").trim();
	        			if(resPath.equals("")) throw new IllegalArgumentException("Element 'resource' requires 'path' attribute!");
	        			if(!resPath.startsWith("/")) resPath = "/" + resPath;
	        			if(!resPath.startsWith("/PUSTEFIX-INF")) resPath = "/PUSTEFIX-INF" + resPath;
	        			dynInfo.addOverridedResource(modName, resPath);
	        		}
	        	}
        	}
        } else throw new IllegalArgumentException("Unexpected module descriptor element: " + element.getNodeName());
        return dynInfo;
    }
    
    private static Element getSingleChildElement(Element parent, String nsuri, String localName, boolean mandatory) {
        Element elem = null;
        NodeList nodes = parent.getChildNodes();
        for(int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE && node.getNamespaceURI().equals(nsuri) 
                    && node.getLocalName().equals(localName)) {
                if(elem != null) throw new IllegalArgumentException("Multiple '" + localName + "' child elements aren't allowed."); 
                elem = (Element)node;
            }
        }
        if(mandatory && elem == null) throw new IllegalArgumentException("Missing '" + localName + "' child element.");
        return elem;
    }

    
    private static List<Element> getChildElements(Element parent, String nsuri, String localName) {
        List<Element> elems = new ArrayList<Element>();
        NodeList nodes = parent.getChildNodes();
        for(int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE && node.getNamespaceURI().equals(nsuri) 
                    && node.getLocalName().equals(localName)) {
                elems.add((Element)node);
            }
        }
        return elems;
    }
    
}