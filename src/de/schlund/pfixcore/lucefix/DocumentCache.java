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

package de.schlund.pfixcore.lucefix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.log4j.Category;
import org.apache.lucene.document.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.schlund.pfixcore.lucefix.Tripel.Type;
import de.schlund.pfixxml.PathFactory;

/**
 * @author schuppi
 * @date Jun 14, 2005
 */
public class DocumentCache {
    private Map cache;
    private static Category LOG = Category.getInstance(DocumentCache.class);

    public DocumentCache() {
        cache = new HashMap();
    }

    public Document getDocument(Tripel tripel) throws FileNotFoundException, IOException, SAXException {
        return getDocument(tripel.getPath(), tripel.getType());
    }

    public Document getDocument(String path, Type type) throws FileNotFoundException, IOException, SAXException {
        // look in cache
//        LOG.debug("looking for " + path);
        Document retval = lookup(path);
        if (retval != null) {
            found++;
        }else{
            missed++;
            String filename = stripAddition(path);
            // file was not scanned
            flush(); 
            Collection newest = DocumentCache.getDocumentsFromFileAsCollection(PathFactory.getInstance().createPath(
                    filename).resolve());
            for (Iterator iter = newest.iterator(); iter.hasNext();) {
                Document element = (Document) iter.next();
                if (type != Type.EDITORUPDATE){
                    cache.put(element.get("path"), element);
                }
                if (path.equals(element.get("path"))) {
                    retval = element;
                    break;
                }
            }
        }
        return retval;
    }

    /**
     * @param path
     * @return
     */
    private static String stripAddition(String path) {
        int letzterSlash = path.lastIndexOf("/");
        int vorletzterSlash = path.lastIndexOf("/", letzterSlash - 1);
        return path.substring(0, vorletzterSlash);
    }

    /**
     * @param path
     * @return
     */
    private Document lookup(String path) {
        return (Document) cache.get(path);
    }

    public boolean remove(Document doc) {
        if (doc == null) return false;
        return cache.remove(doc.get("path")) != null;
    }

    public Collection getRest() {
        return cache.values();
    }

    public void flush() {
        cache.clear();
    }

    private static Collection getDocumentsFromFileAsCollection(File f) throws FileNotFoundException, IOException,
            SAXException {
        XMLReader xmlreader = XMLReaderFactory.createXMLReader();
        IncludeFileHandler handler = new IncludeFileHandler(f.getAbsolutePath(), f.lastModified());
        xmlreader.setContentHandler(handler);
        xmlreader.setDTDHandler(handler);
        xmlreader.setEntityResolver(handler);
        xmlreader.setErrorHandler(handler);
        xmlreader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
        try {
            xmlreader.parse(new InputSource(new FileReader(f)));
        } catch (Exception e) {
//            org.apache.log4j.Logger.getLogger(DocumentCache.class).warn("bad xml: " + f);
            return new Vector();
        }
        return handler.getScannedDocumentsAsVector();
    }
    
    // statistic stuff
    private int found = 0, missed = 0;
    protected void resetStatistic(){
        found = missed = 0;
    }

    protected int getFound() {
        return found;
    }

    protected int getMissed() {
        return missed;
    }
    
}