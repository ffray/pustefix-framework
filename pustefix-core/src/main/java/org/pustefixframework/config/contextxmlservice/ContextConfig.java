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

package org.pustefixframework.config.contextxmlservice;

import java.util.List;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.schlund.pfixcore.auth.AuthConstraint;
import de.schlund.pfixcore.auth.Condition;
import de.schlund.pfixcore.auth.RoleProvider;
import de.schlund.pfixcore.workflow.ContextInterceptor;
import de.schlund.pfixcore.workflow.State;
import de.schlund.pfixcore.workflow.context.PageFlow;
import de.schlund.pfixxml.Variant;

/**
 * Provides configuration for a context instance.
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public interface ContextConfig {

    /**
     * Returns the type of the default state. This is the type of the state
     * returned by {@link #getDefaultState()}.
     * @return type of default state
     */
    Class<? extends State> getDefaultStateType();
    
    /**
     * Returns the default state to use if no more specific information is available.
     * @return the default state to use for a page where no other information is given. 
     */
    State getDefaultState();
    
    String getDefaultStateParentBeanName();
    
     /**
     * Returns name of the page to use when the user enters the site without
     * specifying a specific page.
     * 
     * @return name of default page
     */
    String getDefaultPage(Variant variant);
    
    /**
     * Returns a list of the configuration for all context resources that should
     * be created by the context.
     * 
     * @return list of configuration for alle context resources
     */
    List<? extends ContextResourceConfig> getContextResourceConfigs();

    /**
     * Returns the configuration for the context resource of the specified class
     * 
     * @param clazz class of the context resource
     * @return configuration object for the context resource
     */
    ContextResourceConfig getContextResourceConfig(Class<?> clazz);
    ContextResourceConfig getContextResourceConfig(String name);

    /**
     * Returns a list of all pageflows.
     * 
     * @return list of pageflows
     */
    List<? extends PageFlow> getPageFlows();

    /**
     * Returns the pageflow specified by <code>name</code>.
     * 
     * @param name name of the pageflow
     * @return pageflow instance
     */
    PageFlow getPageFlow(String name);

    boolean getPageFlowPassThrough();
    
    /**
     * Returns a list of configurations for all pagerequests.
     * 
     * @return list of all pagerequest configurations.
     */
    List<? extends PageRequestConfig> getPageRequestConfigs();

    /**
     * Returns the configuration for the pagerequest specified by <code>name</code>.
     * 
     * @param name name of the pagerequest
     * @return pagerequest configuration
     */
    PageRequestConfig getPageRequestConfig(String name);

    /**
     * Returns a list of all start interceptors.
     * 
     * @return list of start interceptors
     */
    List<? extends ContextInterceptor> getStartInterceptors();

    /**
     * Returns a list of all end interceptors.
     * 
     * @return list of end interceptors
     */
    List<? extends ContextInterceptor> getEndInterceptors();
    
    /**
     * Returns a list of all postrender interceptors.
     * 
     * @return list of end interceptors
     */
    List<? extends ContextInterceptor> getPostRenderInterceptors();

    /**
     * Returns configuration properties for the context instance.
     * 
     * @return properties specifying additional configuration options for
     * the context
     */
    Properties getProperties();

    /**
     * Returns true if the context shall synchronize on session objects to ensure
     * that only one request is concurrently processed per session. 
     * 
     * @return flag indicating whether to synchronize on sessions
     */
    boolean isSynchronized();

    RoleProvider getRoleProvider();
    AuthConstraint getDefaultAuthConstraint();
    AuthConstraint getAuthConstraint(String id);
    Condition getCondition(String id);
    Element getAuthConstraintAsXML(Document doc, AuthConstraint authConstraint);
    
    PreserveParams getPreserveParams();
    
}