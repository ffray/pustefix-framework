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
 */

package de.schlund.pfixxml.config;

import java.util.Map;
import java.util.Properties;

import de.schlund.pfixcore.auth.AuthConstraint;
import de.schlund.pfixcore.workflow.State;
import de.schlund.pfixcore.workflow.app.ResdocFinalizer;

/**
 * Provides configuration for a specific page.  
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public interface PageRequestConfig {

    /**
     * Enum type for activity handling policy. The policy will not be applied
     * on handlers with activeignore set to true.
     */
    public enum Policy {
        /**
         * Signal isActive() for the state if any handler is active.
         */
        ANY,
        /**
         * Signal isActive() for the state only if all handlers are active.
         */
        ALL,
        /**
         * Signal isActive() even if none of the handlers is active.
         */
        NONE
    }
    
    /**
     * Returns the name of the page. This name includes the variant
     * string if applicable. The page name has to be unique for a servlet
     * and should be unique for the whole project.
     * 
     * @return name of the page
     */
    String getPageName();

    /**
     * If <code>true</code> a SSL connection is forced when this page
     * is requested.
     * 
     * @return flag indicating whether to use a secure connection for this page
     */
    boolean isSSL();

    /**
     * Returns the class that is used to construct the state that
     * serves this page.
     * 
     * @return class of the state for this page
     */
    Class<? extends State> getState();

    /**
     * Returns the policy for the <code>isActive()</code> method.
     * 
     * @return policy for isActive() check
     */
    Policy getIWrapperPolicy();

    /**
     * Returns the class of the finalizer for the page (use with caution).
     * 
     * @return finalizer class or <code>null</code> if there is no finalizer
     */
    Class<? extends ResdocFinalizer> getFinalizer();

    /**
     * Returns the list of IWrappers for this page. IWrappers are used
     * for input handling. The map returned has the form prefix => IWrapperConfig.
     * 
     * @return list of IWrappers
     */
    Map<String, ? extends IWrapperConfig> getIWrappers();

    /**
     * Returns context resources defined for this page. The map has the form
     * tagname => context resource class. Each context resource specified here
     * will be included in the result XML tree.
     * 
     * @return mapping of tagname to context resource class
     */
    Map<String, Class<?>> getContextResources();

    /**
     * Returns properties defined for this page.
     * 
     * @return properties for this page
     */
    Properties getProperties();
    
    boolean requiresToken();
    
    AuthConstraint getAuthConstraint();
    
    public String getDefaultFlow();
    
    public Map<String, ? extends ProcessActionConfig> getProcessActions();
}