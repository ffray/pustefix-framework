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
package de.schlund.pfixxml.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.pustefixframework.xml.tools.XSLTracing;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import de.schlund.pfixxml.resources.FileResource;
import de.schlund.pfixxml.resources.Resource;
import de.schlund.pfixxml.resources.ResourceUtil;
import de.schlund.pfixxml.targets.LeafTarget;
import de.schlund.pfixxml.targets.Target;
import de.schlund.pfixxml.targets.TargetGenerationException;
import de.schlund.pfixxml.targets.TargetImpl;
import de.schlund.pfixxml.util.xsltimpl.ErrorListenerBase;

public class Xslt {
    
    private static final Logger LOG = Logger.getLogger(Xslt.class);
    
    //-- load documents

    public synchronized static Transformer createIdentityTransformer(XsltVersion xsltVersion) {
        try {
            TransformerFactory trfFact=XsltProvider.getXsltSupport(xsltVersion).getSharedTransformerFactory();
            return trfFact.newTransformer();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static synchronized Transformer createPrettyPrinter(XsltVersion xsltVersion) {
        try {
            return XsltProvider.getXsltSupport(xsltVersion).getPrettyPrinterTemplates().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @deprecated Use {@link #loadTemplates(FileResource)} instead
     */
    @Deprecated
    public static Templates loadTemplates(Path path) throws TransformerConfigurationException {
        return loadTemplates(XsltVersion.XSLT1, new InputSource("file://" + path.resolve().getAbsolutePath()), null);
    }
    
    public static Templates loadTemplates(XsltVersion xsltVersion, Resource path) throws TransformerConfigurationException {
        return loadTemplates(xsltVersion, path, null);
    }

    public static Templates loadTemplates(XsltVersion xsltVersion, FileResource path) throws TransformerConfigurationException {
        return loadTemplates(xsltVersion, path, null);
    }

    public static Templates loadTemplates(XsltVersion xsltVersion, Resource path, TargetImpl parent) throws TransformerConfigurationException {
        return loadTemplates(xsltVersion, path, parent, false);
    }
    
    public static Templates loadTemplates(XsltVersion xsltVersion, Resource path, TargetImpl parent, boolean debug) throws TransformerConfigurationException {
    	try {
            InputSource is = new InputSource();
            is.setSystemId(path.toURI().toString());
            is.setByteStream(path.getInputStream());
            return loadTemplates(xsltVersion, is, parent, debug);
        } catch(IOException x) {
            throw new TransformerConfigurationException("Can't load template", x);
        }
    }
    
    public static Templates loadTemplates(XsltVersion xsltVersion, FileResource path, TargetImpl parent) throws TransformerConfigurationException {
        InputSource input;
        try {
            input = new InputSource(path.toURL().toString());
        } catch (MalformedURLException e) {
            throw new TransformerConfigurationException("\"" + path.toString() + "\" does not respresent a valid file", e);
        }
        return loadTemplates(xsltVersion, input, parent);
    }
    
    private static Templates loadTemplates(XsltVersion xsltVersion, InputSource input, TargetImpl parent) throws TransformerConfigurationException {
        try {
            return loadTemplates(xsltVersion, input, parent, false);
        } catch (TransformerConfigurationException e) {
            return loadTemplates(xsltVersion, input, parent, true);
        }
    }
    
    private static Templates loadTemplates(XsltVersion xsltVersion, InputSource input, TargetImpl parent, boolean debug) throws TransformerConfigurationException {
        Source src = new SAXSource(Xml.createXMLReader(), input);    
        TransformerFactory factory = XsltProvider.getXsltSupport(xsltVersion).getThreadTransformerFactory();
        ErrorListenerBase errorListener = new ErrorListenerBase();
        factory.setErrorListener(errorListener);
        factory.setURIResolver(new ResourceResolver(parent,xsltVersion,debug));
        try {
            Templates retval = factory.newTemplates(src);
            return retval;
        } catch (TransformerConfigurationException e) {
            StringBuffer sb = new StringBuffer();
            sb.append("TransformerConfigurationException in doLoadTemplates!\n");
            sb.append("Path: ").append(input.getSystemId()).append("\n");
            sb.append("Message and Location: ").append(e.getMessageAndLocation()).append("\n");
          
            
            List<TransformerException> errors = errorListener.getErrors();
            if(e.getException() == null && e.getCause() == null && errors.size() > 0) {
                if(e != errors.get(0)) {
                    TransformerException last = e;
                    final int maxDepth = 10;
                    for(int i=errors.size()-1; i>-1 && (errors.size()-i)<=maxDepth && last.getCause()==null; i--) {
                        if(last != errors.get(i)) last.initCause(errors.get(i));
                        last=errors.get(i);
                    }
                }
            }
            
            Throwable cause = e.getException();
            if (cause == null) cause = e.getCause();
            sb.append("Cause: ").append((cause != null) ? cause.getMessage() : "none").append("\n");
            LOG.error(sb.toString());
            throw e;
        }
    }
    
    //-- apply transformation
    public static void transform(Document xml, Templates templates, Map<String, Object> params, Result result) throws TransformerException {
        transform(xml, templates, params, result, null);
    }
    
    public static void transform(Document xml, Templates templates, Map<String, Object> params, Result result, String encoding) throws TransformerException {
    	try {
            doTransform(xml, templates, params, result, encoding, false);
    	} catch(TransformerException x) {
    		LOG.error(x);
    		XsltVersion xsltVersion = getXsltVersion(templates);
    		XsltSupport xsltSupport = XsltProvider.getXsltSupport(xsltVersion);
    		String systemId = xsltSupport.getSystemId(templates);
    		if(systemId != null) {
	    		Resource res = ResourceUtil.getResource(systemId);
	    		templates = loadTemplates(xsltVersion, res, null, true);
	    		doTransform(xml, templates, params, result, encoding, true);
    		}
    		throw x;
    	}
    }
    
    private static void doTransform(Document xml, Templates templates, Map<String, Object> params, Result result, String encoding, 
            boolean traceLocation) throws TransformerException {
        try {
            doTransform(xml,templates,params,result,encoding,traceLocation,false);
        } catch(UnsupportedOperationException x) {
            //workaround for the following sporadically occurring error, which can't be reproduced and normally doesn't occur again after retry:
            //java.lang.UnsupportedOperationException: Cannot create intensional node-set with context dependencies: class com.icl.saxon.expr.PathExpression
            if(result instanceof StreamResult) {
                OutputStream out=((StreamResult)result).getOutputStream();
                if(out instanceof ByteArrayOutputStream) {
                    LOG.error("Try to transform again after UnsupportedOperationException",x);
                    ByteArrayOutputStream baos=(ByteArrayOutputStream)out;
                    baos.reset();
                    try {
                        doTransform(xml,templates,params,result,encoding,traceLocation,true);
                    } catch(UnsupportedOperationException ex) {
                        LOG.error("Try to transform and trace after UnsupportedOperationException",ex);
                        baos.reset();
                        doTransform(xml,templates,params,result,encoding,traceLocation,true);
                    }
                }
            }
        }
    }
    
    private static void doTransform(Document xml, Templates templates, Map<String, Object> params, Result result, String encoding, 
            boolean traceLocation, boolean traceInstructions) throws TransformerException {

        XsltVersion xsltVersion=getXsltVersion(templates);
        Transformer trafo = templates.newTransformer();
        if (encoding != null) {
            trafo.setOutputProperty(OutputKeys.ENCODING, encoding);
        }
        StringWriter traceWriter=null;
        if(traceInstructions) {
           traceWriter=new StringWriter();
           XsltProvider.getXsltSupport(xsltVersion).doTracing(trafo,traceWriter);
        }

        if(XSLTracing.getInstance().isEnabled() && params.containsKey("__spdoc__")) {
            XsltProvider.getXsltSupport(xsltVersion).doPerformanceTracing(trafo, templates);
        }
        XsltMessageWriter msgWriter = XsltProvider.getXsltSupport(xsltVersion).recordMessages(trafo);
        long start = 0;
        if (params != null) {
            for (Iterator<String> e = params.keySet().iterator(); e.hasNext();) {
                String name  = e.next();
                Object value = params.get(name);
                if (name != null && value != null) {
                    trafo.setParameter(name, value);
                }
            }
        }
        if (LOG.isDebugEnabled())
            start = System.currentTimeMillis();
        try {
            ExtensionFunctionUtils.resetExtensionFunctionError();
            XsltProvider.getXsltSupport(xsltVersion).doErrorListening(trafo, traceLocation);
            trafo.transform(new DOMSource(Xml.parse(xsltVersion,xml)), result);
        } catch(TransformerException x) {
            String messages = msgWriter.getMessages();
            if(!messages.isEmpty()) {
                XsltMessageTempStore.setMessages(x, messages);
            }
        	String msg = x.getMessage();
        	Throwable extFuncError = ExtensionFunctionUtils.getExtensionFunctionError();
        	if(extFuncError != null || (msg != null && msg.contains("Exception in extension function"))) {
        		if(extFuncError == null) {
        			extFuncError = x;
        		}
        		String extFuncMsg = x.getMessageAndLocation();
        		if(extFuncMsg != null && extFuncMsg.contains("Exception in extension function") 
        				&& x.getLocator() != null && x.getLocator() instanceof Element) {
        			Element element = (Element)x.getLocator();
            		String val = element.getAttribute("select");
            		if(val.length() > 0) {
            			extFuncMsg += "; Expression: \"" + val + "\"";
            		}
        		}
        		TransformerException xsltEx = new XsltExtensionFunctionException(extFuncMsg, extFuncError);
        		xsltEx.setLocator(x.getLocator());
        		throw xsltEx;
        	} else {
        		throw x;
        	}
            
        } finally {
           ExtensionFunctionUtils.resetExtensionFunctionError();
           if(traceInstructions) {
              String traceStr=traceWriter.toString();
              int maxSize=10000;
              if(traceStr.length()>maxSize) {
                 traceStr=traceStr.substring(traceStr.length()-maxSize);
                 int ind=traceStr.indexOf('\n');
                 if(ind>-1) traceStr=traceStr.substring(ind); 
              }
              LOG.error("Last trace steps:\n"+traceStr);
           }
        }
        if (LOG.isDebugEnabled()) {
            long stop = System.currentTimeMillis();
            LOG.debug("      ===========> Transforming and serializing took " + (stop - start) + " ms.");
        }
    }
    
    //--

    private static XsltVersion getXsltVersion(Templates templates) {
        Iterator<Map.Entry<XsltVersion,XsltSupport>> it=XsltProvider.getXsltSupport().entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<XsltVersion,XsltSupport> entry=it.next();
            if(entry.getValue().isInternalTemplate(templates)) return entry.getKey();
        }
        return null;
    }
    
    
    public static class ResourceResolver implements URIResolver {
        
        private TargetImpl parent;
        private XsltVersion xsltVersion;
        private boolean debug;
        
        public ResourceResolver(TargetImpl parent, XsltVersion xsltVersion, boolean debug) {
            this.parent = parent;
            this.xsltVersion = xsltVersion;
            this.debug = debug;
        }

        public Target getParentTarget() {
            return parent;
        }
        
        /**
         * Resolve file url relative to root. Before searching the file system, check
         * if there is a XML Target defined and use this instead.
         * @param base ignored, always relative to root 
         * */
        public Source resolve(String href, String base) throws TransformerException {
            URI uri;
            String path;
            Resource resource;
            
            //Rewrite include href to xslt version specific file, that's necessary cause
            //the XSLT1 and XSLT2 extension functions are incompatible and we want
            //to support using XSLT1 (Saxon 6.5.x) or XSLT2 (Saxon 8.x) without the
            //need to have both versions installed, thus the extension functions can't be
            //referenced within the same stylesheet and we rewrite to the according 
            //version specific stylesheet here
            if(href.equals("module://pustefix-core/xsl/include.xsl")) {
                if(xsltVersion==XsltVersion.XSLT2) href="module://pustefix-core/xsl/include_xslt2.xsl";
            } else if(href.equals("module://pustefix-core/xsl/render.xsl")) {
                if(xsltVersion==XsltVersion.XSLT2) href="module://pustefix-core/xsl/render_xslt2.xsl";
            }
                
            try {
                uri = new URI(href);
            } catch (URISyntaxException e) {
                return new StreamSource(href);
            }

            if (uri.getScheme() != null && !uri.getScheme().equals("docroot") 
                    && !uri.getScheme().equals("module") && !uri.getScheme().equals("dynamic")) {
                // we don't handle uris with an explicit scheme
                return new StreamSource(href);
            }
            path = uri.getPath();
            if (uri.getScheme() != null && uri.getScheme().equals("docroot")) {
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
            }
            if("module".equals(uri.getScheme()) || "dynamic".equals(uri.getScheme())) {
                path = uri.toString();
            }
            
            if (parent != null) {
                Target target = parent.getTargetGenerator().getTarget(path);
                if (target == null) {
                    target = parent.getTargetGenerator().createXMLLeafTarget(path);
                }
                
                if(! ( debug && target instanceof LeafTarget)) {
                
                    Source source;
                    try {
                        source = target.getSource();
                    } catch (TargetGenerationException e) {
                        throw new TransformerException("Could not retrieve target '"
                                                   + target.getTargetKey() + "' included by stylesheet!", e);
                    }
                
                    // If Document object is null, the file could not be found or read
                    // so return null to tell the parser the URI could not be resolved
                    if (source == null) {
                        return null;
                    }

                    parent.getAuxDependencyManager().addDependencyTarget(target.getTargetKey());
                    return source;
                }
            }
            resource = ResourceUtil.getResource(path);
            if(!resource.exists()) {
                throw new TransformerException("Resource can't be found: " + uri.toString());
            }
            try {
            	Source source = new StreamSource(resource.getInputStream(), resource.toURI().toString());
            	return source;
            } catch(IOException x) {
            	throw new TransformerException("Can't read resource: " + path);
            }
        }
    }

}
