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

package org.pustefixframework.container.spring.beans;

import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.pustefixframework.container.spring.util.PustefixPropertiesPersister;
import org.pustefixframework.http.internal.PustefixInit;
import org.pustefixframework.http.internal.PustefixTempDirs;
import org.pustefixframework.web.mvc.internal.ControllerStateAdapter;
import org.pustefixframework.web.mvc.internal.InputHandlerProcessor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.schlund.pfixcore.exception.PustefixCoreException;
import de.schlund.pfixcore.exception.PustefixRuntimeException;
import de.schlund.pfixxml.config.EnvironmentProperties;
import de.schlund.pfixxml.resources.ResourceUtil;

public class PustefixWebApplicationContext extends AbstractRefreshableWebApplicationContext {
    
    private Logger LOG = Logger.getLogger(PustefixWebApplicationContext.class);

    private PustefixInit pustefixInit;

    public PustefixWebApplicationContext() {
        super();
    }

    public PustefixWebApplicationContext(PustefixInit pustefixInit) {
        super();
        this.pustefixInit = pustefixInit;
    }

    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException, BeansException {
        
        //disable bean definition overriding as it turned out to be more cumbersome
        //finding errors caused by this feature, than that it's bringing real benefit
        //beanFactory.setAllowBeanDefinitionOverriding(false);

        if(pustefixInit == null) {
            pustefixInit = (PustefixInit)getServletContext().getAttribute(PustefixInit.SERVLET_CONTEXT_ATTRIBUTE_NAME);
            if(pustefixInit == null) {
                try {
                    pustefixInit = new PustefixInit(getServletContext());
                } catch(PustefixCoreException x) {
                    throw new PustefixRuntimeException("Pustefix initialization failed", x);
                }
            }
        }

        //activate mode profile (using Pustefix execution environment mode as profile name)
        getEnvironment().addActiveProfile(EnvironmentProperties.getProperties().getProperty("mode"));
        
        if(LOG.isInfoEnabled()) {
            Properties props = EnvironmentProperties.getProperties();
            LOG.info("Initializing Pustefix with runtime properties: " +
                "fqdn=" + props.getProperty("fqdn") +
                ", machine=" + props.getProperty("machine") +
                ", mode=" + props.getProperty("mode") +
                ", uid=" + props.getProperty("uid"));
        }

        String configLocations[] = getConfigLocations();
        if (configLocations == null) {
            configLocations = getDefaultConfigLocations();
            if (configLocations == null) {
                return;
            }
        }

        XmlBeanDefinitionReader springReader = new XmlBeanDefinitionReader(beanFactory);
        springReader.setResourceLoader(this);
        springReader.setEntityResolver(new ResourceEntityResolver(this));
        springReader.setEnvironment(getEnvironment());
        

        PustefixProjectBeanDefinitionReader pustefixReader = new PustefixProjectBeanDefinitionReader(beanFactory);
        pustefixReader.setResourceLoader(this);

        for (int i = 0; i < configLocations.length; i++) {
            String configLocation = configLocations[i];
            Resource configResource = this.getResource(configLocation);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setXIncludeAware(true);
            DocumentBuilder db;
            Document doc;
            try {
                db = dbf.newDocumentBuilder();
                doc = db.parse(configResource.getInputStream());
            } catch (ParserConfigurationException e) {
                throw new ApplicationContextException("Error while reading " + configResource + ": " + e.getMessage(), e);
            } catch (SAXException e) {
                throw new ApplicationContextException("Error while reading " + configResource + ": " + e.getMessage(), e);
            }
            if (doc.getDocumentElement().getNamespaceURI() != null && doc.getDocumentElement().getNamespaceURI().equals("http://www.pustefix-framework.org/2008/namespace/project-config")) {
                pustefixReader.loadBeanDefinitions(configResource);
            } else {
                tryAddPropertyConfigurer(configLocation, beanFactory);
                springReader.loadBeanDefinitions(configResource);
            }

        }

        beanFactory.registerScope("tenant", new TenantScope());
        addAnnotationMethodHandlerAdapter(beanFactory);
        addBeanFactoryPostProcessor(AnnotationBeanDefinitionPostProcessor.class, beanFactory);
        addBeanFactoryPostProcessor(InputHandlerProcessor.class, beanFactory);
    }
    
    private void addBeanFactoryPostProcessor(Class<? extends BeanFactoryPostProcessor> processorClass, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.genericBeanDefinition(processorClass);
        beanBuilder.setScope("singleton");
        BeanDefinition definition = beanBuilder.getBeanDefinition();
        DefaultBeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();
        String name = beanNameGenerator.generateBeanName(definition, registry);
        registry.registerBeanDefinition(name, definition);
    }

    private void tryAddPropertyConfigurer(String configLocation, BeanDefinitionRegistry registry) {
        int ind = configLocation.lastIndexOf(".");
        if(ind > -1) {
            String propConfigLocation = configLocation.substring(0,ind) + "-properties" +
                configLocation.substring(ind);
            Resource propConfigResource = getResource(propConfigLocation);
            if(propConfigResource.exists()) {
                addPropertyConfigurer(PropertyPlaceholderConfigurer.class, propConfigResource, registry);
            }
            propConfigLocation = configLocation.substring(0,ind) + "-properties-override" +
                configLocation.substring(ind);
            propConfigResource = getResource(propConfigLocation);
            if(propConfigResource.exists()) {
                addPropertyConfigurer(PropertyOverrideConfigurer.class, propConfigResource, registry);
            }
        }
    }

    private void addPropertyConfigurer(Class<? extends PropertyResourceConfigurer> clazz, Resource location, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        beanBuilder.setScope("singleton");
        beanBuilder.addPropertyValue("location", location);
        PustefixPropertiesPersister persister = new PustefixPropertiesPersister();
        try {
            persister.setLocation(location.getURI());
        } catch(IOException x) {
            //ignore
        }
        beanBuilder.addPropertyValue("propertiesPersister", persister);
        BeanDefinition definition = beanBuilder.getBeanDefinition();
        DefaultBeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();
        String name = beanNameGenerator.generateBeanName(definition, registry);
        registry.registerBeanDefinition(name, definition);
    }

    private void addAnnotationMethodHandlerAdapter(BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.genericBeanDefinition(ControllerStateAdapter.class);
        beanBuilder.setScope("singleton");
        BeanDefinition definition = beanBuilder.getBeanDefinition();
        String beanName = ControllerStateAdapter.class.getName();
        registry.registerBeanDefinition(beanName, definition);
    }
    
    @Override
    public Resource getResource(String location) {
        if(location.startsWith("module:") || location.startsWith("dynamic:")) {
            return ResourceUtil.getResource(location);
        } else {
            return super.getResource(location);
        }
    }
    
    @Override
    protected void doClose() {
        super.doClose();
        PustefixTempDirs.getInstance(getServletContext()).dispose();
    }
    
}
