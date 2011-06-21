package org.pustefixframework.util.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DOMUtils {
    
    public static List<Element> getChildElementsByTagName(Element parent, String tagName) {
        List<Element> elems = new ArrayList<Element>();
        NodeList nodes = parent.getChildNodes();
        for(int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(tagName)) {
                elems.add((Element)node);
            }
        }
        return elems;
    }
    
    public static List<Element> getChildElementsByTagNameNS(Element parent, String namespaceURI, String localName) {
        List<Element> elems = new ArrayList<Element>();
        NodeList nodes = parent.getChildNodes();
        for(int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE && 
                    node.getNamespaceURI().equals(namespaceURI) && node.getLocalName().equals(localName)) {
                elems.add((Element)node);
            }
        }
        return elems;
    }

}
