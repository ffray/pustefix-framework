package de.schlund.pfixxml;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Category;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.schlund.pfixxml.targets.Path;
import de.schlund.pfixxml.targets.TraxXSLTProcessor;


/**
 * IncludeDocument.java
 * 
 * 
 * Created: 20021031
 * 
 * @author <a href="mailto:haecker@schlund.de">Joerg Haecker</a>
 * 
 * 
 * This class encapsulates an include-module of the PUSTEFIX-system.
 * A IncludeDocument stores a Document created from a file. Currently
 * there are two types of Documents: mutable and immutable. The user
 * of this class must know which type he wants. 
 * Anymore various administrative data like modification time
 * of the file from which it is created from and more are stored.  
 */
public class IncludeDocument {

    //~ Instance/static variables ..................................................................

    private Document                          doc;
    private long                              modTime           = 0;
    private static final String               INCPATH           = "incpath";
    private static final Category             CAT               = Category.getInstance(IncludeDocument.class.getName());
    private static final DocumentBuilder      docBuilder;
    
    //~ Constructors ...............................................................................
    static {
        // NOTE: here we want a XERCES-DocumentBuilderFactory
        DocumentBuilderFactory docBuilderFactory = new DocumentBuilderFactoryImpl();

        if (! docBuilderFactory.isNamespaceAware())
            docBuilderFactory.setNamespaceAware(true);
        if (docBuilderFactory.isValidating())
            docBuilderFactory.setValidating(false);
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            CAT.error(e.getMessage());
            throw new RuntimeException(e);
        }  
    }
    /**
     * Constructor
     */
    public IncludeDocument()  {
    }

    //~ Methods ....................................................................................

    /**
     * Create the internal document.
     * @param path the path in the filesystem to create the document from.
     * @param mutable determine if the document is mutable or not. Any attempts
     * to modify an immutable document will cause an exception.
     */
    public void createDocument(Path path, boolean mutable) throws SAXException, IOException, TransformerException {
        File tmp = path.resolve();
        modTime = tmp.lastModified();
        
        try {
            doc = docBuilder.parse(tmp);
        } catch (SAXParseException ex) {
            StringBuffer      buf = new StringBuffer(100);
            buf.append("Caught SAXParseException!\n");
            buf.append("  Message  : ").append(ex.getMessage()).append("\n");
            buf.append("  SystemID : ").append(ex.getSystemId()).append("\n");
            buf.append("  Line     : ").append(ex.getLineNumber()).append("\n");
            buf.append("  Column   : ").append(ex.getColumnNumber()).append("\n");
            CAT.error(buf.toString());
            throw ex;
        }
        
        Element rootElement = doc.getDocumentElement();
        rootElement.setAttribute(INCPATH, path.getRelative());
        if (! mutable) {
            doc = TraxXSLTProcessor.getInstance().xmlObjectFromDocument(doc);
        }
    }

    public Document getDocument() {
        return doc;
    }

    public long getModTime() {
        return modTime;
    }

    public void resetModTime() {
        modTime -= 1l;
    }
}