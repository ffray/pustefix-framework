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

package de.schlund.pfixcore.webservice.config;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Configuration.java 
 * 
 * Created: 22.07.2004
 * 
 * @author mleidig@schlund.de
 */
public class Configuration implements Serializable {

    private GlobalServiceConfig globConf;
    private HashMap<String,ServiceConfig> srvsConf;
    
    public Configuration() {
        srvsConf=new HashMap<String,ServiceConfig>();
    }
     
    public GlobalServiceConfig getGlobalServiceConfig() {
        return globConf;
    }
    
    public void setGlobalServiceConfig(GlobalServiceConfig globConf) {
        this.globConf=globConf;
    }
    
    public void addServiceConfig(ServiceConfig srvConf) {
        srvsConf.put(srvConf.getName(),srvConf);
    }
    
    public ServiceConfig getServiceConfig(String name) {
        return (ServiceConfig)srvsConf.get(name);
    }
    
    public Collection<ServiceConfig> getServiceConfig() {
        return Collections.unmodifiableCollection(srvsConf.values());
    }
    
    @Override
    public boolean equals(Object obj) {
    	if(obj instanceof Configuration) {
    		Configuration ref=(Configuration)obj;
    		if(!getGlobalServiceConfig().equals(ref.getGlobalServiceConfig())) {
    			System.out.println("Global service not equal");
    			return false;
    		}
    		Iterator<ServiceConfig> it=srvsConf.values().iterator();
    		while(it.hasNext()) {
    			ServiceConfig sc=it.next();
    			ServiceConfig refSc=ref.getServiceConfig(sc.getName());
    			if(refSc==null) {
    				System.out.println("Service not found: "+sc.getName());
    				return false;
    			}
    			if(!sc.equals(refSc)) {
    				System.out.println("Service not equal: "+sc.getName());
    				return false;
    			}
    		}
    		it=ref.srvsConf.values().iterator();
    		while(it.hasNext()) {
    			ServiceConfig refSc=it.next();
    			ServiceConfig sc=getServiceConfig(refSc.getName());
    			if(sc==null) {
    				System.out.println("Service not found: "+refSc.getName());
    				return false;
    			}
    		}
    		return true;
    	}
    	return false;
    }
    
}
