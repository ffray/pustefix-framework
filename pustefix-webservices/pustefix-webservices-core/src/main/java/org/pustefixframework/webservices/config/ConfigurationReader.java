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

package org.pustefixframework.webservices.config;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.pustefixframework.webservices.Constants;
import org.pustefixframework.webservices.fault.FaultHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.schlund.pfixxml.config.CustomizationHandler;
import de.schlund.pfixxml.resources.FileResource;
import de.schlund.pfixxml.resources.Resource;
import de.schlund.pfixxml.resources.ResourceUtil;

/**
 * @author mleidig@schlund.de
 */
public class ConfigurationReader extends DefaultHandler {

    Resource configFile;
    Configuration config;
    Stack<Object> contextStack=new Stack<Object>();
    Object context;
    CharArrayWriter content=new CharArrayWriter();
    List<FileResource> importedFiles=new ArrayList<FileResource>();
    boolean isRootFile;
    ApplicationContext appContext;
    
    public static Configuration read(Resource file, ApplicationContext appContext) throws Exception {
        return read(file, true, appContext);
    }
    
    public static Configuration read(Resource file) throws Exception {
        return read(file, true, null);
    }
    
    private static Configuration read(Resource file,boolean isRootFile, ApplicationContext appContext) throws Exception {
        ConfigurationReader reader=new ConfigurationReader(file, isRootFile, appContext);
        reader.read();
        return reader.getConfiguration();
    }
    
    private ConfigurationReader(Resource configFile, boolean isRootFile, ApplicationContext appContext) {
        this.configFile = configFile;
        this.isRootFile = isRootFile;
        this.appContext = appContext;
    }
    
    public Configuration getConfiguration() {
        return config;
    }
    
    public void read() throws Exception {
        CustomizationHandler cushandler=new CustomizationHandler(this,Constants.WS_CONF_NS,Constants.CUS_NS);
        SAXParserFactory spf=SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser=spf.newSAXParser();
        parser.parse(configFile.getInputStream(),cushandler);
    }
    
    private void setContext(Object obj) {
        contextStack.add(obj);
        context=obj;
    }
    
    private void resetContext() {
        contextStack.pop();
        if(contextStack.empty()) context=null;
        else context=contextStack.peek();
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        content.reset();
        if(context==null) {
            if(localName.equals("webservice-config")) {
                config=new Configuration();
                setContext(config);
            }
        } else if(context instanceof Configuration) {
            if(localName.equals("webservice-global")) {
                GlobalServiceConfig globSrvConf=new GlobalServiceConfig();
                config.setGlobalServiceConfig(globSrvConf);
                setContext(globSrvConf);
            } else if(localName.equals("webservice")) {
                ServiceConfig srvConf=new ServiceConfig(config.getGlobalServiceConfig());
                String name=getStringAttribute(atts,"name",true);
                srvConf.setName(name);
                config.addServiceConfig(srvConf);
                setContext(srvConf);
            } else if(localName.equals("import")) {
                String name=getStringAttribute(atts,"href",true);
                FileResource impFile=ResourceUtil.getFileResourceFromDocroot(name);
                importedFiles.add(impFile);
            }
        } else if(context instanceof GlobalServiceConfig) {
            GlobalServiceConfig globSrvConf=(GlobalServiceConfig)context;
            if(localName.equals("wsdlsupport")) {
                Boolean wsdlSupport=getBooleanAttribute(atts,"enabled");
                if(wsdlSupport!=null) globSrvConf.setWSDLSupportEnabled(wsdlSupport);
                String wsdlRepo=getStringAttribute(atts,"repository");
                if(wsdlRepo!=null) globSrvConf.setWSDLRepository(wsdlRepo);
            } else if(localName.equals("stubgeneration")) {
                 Boolean stubGeneration=getBooleanAttribute(atts,"enabled");
                 if(stubGeneration!=null) globSrvConf.setStubGenerationEnabled(stubGeneration);
                 String stubRepo=getStringAttribute(atts,"repository");
                 if(stubRepo!=null)     globSrvConf.setStubRepository(stubRepo);
                 String jsNamespace=getStringAttribute(atts,"jsnamespace");
                 if(jsNamespace!=null) globSrvConf.setStubJSNamespace(jsNamespace);
            } else if(localName.equals("protocol")) {
                String proto=getStringAttribute(atts,"type",Constants.PROTOCOL_TYPES);
                if(proto!=null) globSrvConf.setProtocolType(proto);
            } else if(localName.equals("encoding")) {
                String encStyle=getStringAttribute(atts,"style",Constants.ENCODING_STYLES);
                if(encStyle!=null) globSrvConf.setEncodingStyle(encStyle);
                String encUse=getStringAttribute(atts,"use",Constants.ENCODING_USES);
                if(encUse!=null) globSrvConf.setEncodingUse(encUse);
            } else if(localName.equals("json")) {
                Boolean hinting=getBooleanAttribute(atts,"classhinting");
                if(hinting!=null) globSrvConf.setJSONClassHinting(hinting);
            } else if(localName.equals("session")) {
                String sessType=getStringAttribute(atts,"type",Constants.SESSION_TYPES);
                if(sessType!=null) globSrvConf.setSessionType(sessType);
            } else if(localName.equals("scope")) {
                String scopeType=getStringAttribute(atts,"type",Constants.SERVICE_SCOPES);
                if(scopeType!=null) globSrvConf.setScopeType(scopeType);
            } else if(localName.equals("ssl")) {
                Boolean sslForce=getBooleanAttribute(atts,"force");
                if(sslForce!=null) globSrvConf.setSSLForce(sslForce);
            } else if(localName.equals("context")) {
                String ctxName=getStringAttribute(atts,"name",true);
                globSrvConf.setContextName(ctxName);
                Boolean ctxSync=getBooleanAttribute(atts,"synchronize");
                if(ctxSync!=null) globSrvConf.setSynchronizeOnContext(ctxSync);
            } else if(localName.equals("admin")) {
                Boolean admin=getBooleanAttribute(atts,"enabled");
                if(admin!=null) globSrvConf.setAdminEnabled(admin);
            } else if(localName.equals("monitoring")) {
                Boolean monitoring=getBooleanAttribute(atts,"enabled");
                if(monitoring!=null) globSrvConf.setMonitoringEnabled(monitoring);
                String monitorScope=getStringAttribute(atts,"scope",Constants.MONITOR_SCOPES);
                if(monitorScope!=null) globSrvConf.setMonitoringScope(monitorScope);
                Integer monitorSize=getIntegerAttribute(atts,"historysize");
                if(monitorSize!=null) globSrvConf.setMonitoringHistorySize(monitorSize);
            } else if(localName.equals("logging")) {
                Boolean logging=getBooleanAttribute(atts,"enabled");
                if(logging!=null) globSrvConf.setLoggingEnabled(logging);
            } else if(localName.equals("faulthandler")) {
                 FaultHandler faultHandler=(FaultHandler)getObjectAttribute(atts,"class",FaultHandler.class,false);
                 globSrvConf.setFaultHandler(faultHandler);
                 setContext(faultHandler);
            } else if(localName.equals("authconstraint")) {
                String ref=getStringAttribute(atts,"ref",true);
                if(ref!=null) globSrvConf.setAuthConstraintRef(ref);
            }
        } else if(context instanceof ServiceConfig) {
            ServiceConfig srvConf=(ServiceConfig)context;
            if(localName.equals("interface")) {
                String name=getStringAttribute(atts,"name",true);
                srvConf.setInterfaceName(name);
            } else if(localName.equals("implementation")) {
                String name=getStringAttribute(atts,"name",true);
                srvConf.setImplementationName(name);
            } else if(localName.equals("protocol")) {
                String proto=getStringAttribute(atts,"type",Constants.PROTOCOL_TYPES);
                if(proto!=null) srvConf.setProtocolType(proto);
            } else if(localName.equals("stubgeneration")) {
                 String jsNamespace=getStringAttribute(atts,"jsnamespace");
                 if(jsNamespace!=null) srvConf.setStubJSNamespace(jsNamespace);
            } else if(localName.equals("encoding")) {
                String encStyle=getStringAttribute(atts,"style",Constants.ENCODING_STYLES);
                if(encStyle!=null) srvConf.setEncodingStyle(encStyle);
                String encUse=getStringAttribute(atts,"use",Constants.ENCODING_USES);
                if(encUse!=null) srvConf.setEncodingUse(encUse);
            } else if(localName.equals("json")) {
                Boolean hinting=getBooleanAttribute(atts,"classhinting");
                if(hinting!=null) srvConf.setJSONClassHinting(hinting);
            } else if(localName.equals("session")) {
                String sessType=getStringAttribute(atts,"type",Constants.SESSION_TYPES);
                if(sessType!=null) srvConf.setSessionType(sessType);
            } else if(localName.equals("scope")) {
                String scopeType=getStringAttribute(atts,"type",Constants.SERVICE_SCOPES);
                if(scopeType!=null) srvConf.setScopeType(scopeType);
            } else if(localName.equals("ssl")) {
                Boolean sslForce=getBooleanAttribute(atts,"force");
                if(sslForce!=null) srvConf.setSSLForce(sslForce);
            } else if(localName.equals("context")) {
                Boolean ctxSync=getBooleanAttribute(atts,"synchronize");
                if(ctxSync!=null) srvConf.setSynchronizeOnContext(ctxSync);
            } else if(localName.equals("faulthandler")) {
                 FaultHandler faultHandler=(FaultHandler)getObjectAttribute(atts,"class",FaultHandler.class,false);
                 srvConf.setFaultHandler(faultHandler);
                 setContext(faultHandler);
            } else if(localName.equals("authconstraint")) {
                String ref=getStringAttribute(atts,"ref",true);
                if(ref!=null) srvConf.setAuthConstraintRef(ref);
            }
        } else if(context instanceof FaultHandler) {
            FaultHandler faultHandler=(FaultHandler)context;
            if(localName.equals("param")) {
                String name=getStringAttribute(atts,"name",true);
                String value=getStringAttribute(atts,"value",true);
                faultHandler.addParam(name,value);
            }
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(context instanceof Configuration) {
            if(localName.equals("webservice-config")) {
                resetContext();
            }
        } else if(context instanceof GlobalServiceConfig) {
            GlobalServiceConfig globSrvConf=(GlobalServiceConfig)context;
            if(localName.equals("webservice-global")) {
                resetContext();
            } else if(localName.equals("requestpath")) {
                String path=getContent();
                if(path!=null&&!path.equals("")) globSrvConf.setRequestPath(path);
            }
        } else if(context instanceof ServiceConfig) {
            ServiceConfig srvConf = (ServiceConfig)context;
            if(localName.equals("webservice")) {
                resetContext();
            } else if(localName.equals("whitelist")) {
                String text = getContent();
                String[] values = text.split("[,\\s]+");
                List<Pattern> patterns = new ArrayList<>();
                for(String value: values) {
                    patterns.add(Pattern.compile(value));
                }
                srvConf.setDeserializationWhiteList(patterns);
            }
        } else if(context instanceof FaultHandler) {
            if(localName.equals("faulthandler")) {
                FaultHandler faultHandler=(FaultHandler)context;
                if(faultHandler instanceof ApplicationContextAware) {
                    ((ApplicationContextAware)faultHandler).setApplicationContext(appContext);
                }
                faultHandler.init();
                resetContext();
            }
        }
    }
    
    @Override
    public void endDocument() throws SAXException {
        processImports();
        if(isRootFile) {
            GlobalServiceConfig globSrvConf=config.getGlobalServiceConfig();
            if(globSrvConf==null) {
                globSrvConf=new GlobalServiceConfig();
                config.setGlobalServiceConfig(globSrvConf);
            }
            Collection<ServiceConfig> wsList=config.getServiceConfig();
            for(ServiceConfig ws:wsList) {
                ws.setGlobalServiceConfig(globSrvConf);
            }
        }
        try {
            Resource metaFile= (Resource)configFile.createRelative("beanmetadata.xml");
            if(config.getGlobalServiceConfig()!=null && metaFile.exists()) 
                config.getGlobalServiceConfig().setDefaultBeanMetaDataURL(metaFile.getFile().toURI().toURL());
        } catch(MalformedURLException x) {
            throw new SAXException("Can't get default bean metadata URL.",x);
        } catch(IOException x) {
            throw new SAXException("Can't read bean metadata.", x);
        } 
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        content.write(ch,start,length);
    }
    
    public String getContent() {
        return content.toString().trim();
    }
    
    private void processImports() throws SAXException {
        for(int i=importedFiles.size()-1;i>-1;i--) {
            try {
                FileResource impFile=importedFiles.get(i);
                Configuration impConf=ConfigurationReader.read(impFile, false, appContext);
                if(config.getGlobalServiceConfig()==null && impConf.getGlobalServiceConfig()!=null) {
                    config.setGlobalServiceConfig(impConf.getGlobalServiceConfig());
                }
                Collection<ServiceConfig> wsList=impConf.getServiceConfig();
                for(ServiceConfig ws:wsList) {
                    if(config.getServiceConfig(ws.getName())==null) {
                        config.addServiceConfig(ws);
                    }
                }
            } catch(Exception x) {
                throw new SAXException("Error processing imported file: "+importedFiles.get(i),x);
            }
        }
    }
    
    private String getStringAttribute(Attributes attributes,String attrName) throws ConfigException {
        String val=attributes.getValue(attrName);
        if(val!=null) val=val.trim();
        return val;
    }
    
    private String getStringAttribute(Attributes attributes,String attrName,boolean mandatory) throws ConfigException {
        String val=attributes.getValue(attrName);
        if(val==null && mandatory) throw new ConfigException(ConfigException.MISSING_ATTRIBUTE,attrName);
        return val.trim();
    }
    
    private String getStringAttribute(Attributes attributes,String attrName,String[] allowedValues) throws ConfigException {
        String val=attributes.getValue(attrName);
        if(val==null) return null;
        for(int i=0;i<allowedValues.length;i++) {
            if(val.equals(allowedValues[i])) return val;
        }
        throw new ConfigException(ConfigException.ILLEGAL_ATTRIBUTE_VALUE,attrName,val);
    }
    
    private Boolean getBooleanAttribute(Attributes attributes,String attrName) throws ConfigException {
        String val=attributes.getValue(attrName);
        if(val==null) return null;
        if(val.equalsIgnoreCase("true")) return true;
        if(val.equalsIgnoreCase("false")) return false;
        throw new ConfigException(ConfigException.ILLEGAL_ATTRIBUTE_VALUE,attrName,val);
    }
    
    private Integer getIntegerAttribute(Attributes attributes,String attrName) throws ConfigException {
        String val=attributes.getValue(attrName);
        if(val==null) return null;
        try {
            int intVal=Integer.parseInt(val);
            return intVal;
        } catch(NumberFormatException x) {
            throw new ConfigException(ConfigException.ILLEGAL_ATTRIBUTE_VALUE,attrName,val);
        }
    }
    
    private Object getObjectAttribute(Attributes attributes,String attrName,Class<?> superClazz,boolean mandatory) throws ConfigException {
        String val=attributes.getValue(attrName);
        if(val==null) {
            if(mandatory) throw new ConfigException(ConfigException.MISSING_ATTRIBUTE,attrName);
            else return null;
        }
        try {
            Class<?> clazz=Class.forName(val);
            Object obj=clazz.newInstance();
            if(!superClazz.isInstance(obj)) throw new ClassCastException("Class '"+val+"' can't be casted to '"+superClazz.getName()+"'.");
            return obj;
        } catch(Exception x) {
            throw new ConfigException(ConfigException.ILLEGAL_ATTRIBUTE_VALUE,attrName,val,x);
        } 
    }

    
    public static void serialize(Configuration config,FileResource file) throws Exception {
        OutputStream out=file.getOutputStream();
        serialize(config,out);
    }
    
    public static void serialize(Configuration config,OutputStream out) throws Exception {
        ObjectOutputStream serializer=new ObjectOutputStream(out);
        serializer.writeObject(config);
        serializer.flush( );
    }
    
    public static Configuration deserialize(FileResource file) throws Exception {
        InputStream in=file.getInputStream();
        return deserialize(in);
    }
    
    public static Configuration deserialize(InputStream in) throws Exception {
        ObjectInputStream deserializer=new ObjectInputStream(in);
        return (Configuration)deserializer.readObject( );
    }
    
}
