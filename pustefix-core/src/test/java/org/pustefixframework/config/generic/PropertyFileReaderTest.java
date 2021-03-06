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
package org.pustefixframework.config.generic;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;
import org.pustefixframework.container.spring.beans.TenantAwareProperties;
import org.pustefixframework.container.spring.beans.TenantScope;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import de.schlund.pfixxml.Tenant;
import de.schlund.pfixxml.config.EnvironmentProperties;


public class PropertyFileReaderTest {

    @Before
    public void setUp() {
        ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%p: %m\n"));
        Logger logger=Logger.getRootLogger();
        logger.setLevel((Level)Level.WARN);
        logger.removeAllAppenders();
        logger.addAppender(appender);
    }
    
    @Test
    public void test() throws Exception {
        Properties btp = new Properties();
        btp.put("mode", "test");
        btp.put("fqdn", "fqdn");
        EnvironmentProperties.setProperties(btp);
        InputStream in = getClass().getResourceAsStream("/properties.xml");
        Properties props = new Properties();
        PropertyFileReader.read(in, props);
        Properties refProps = new Properties();
        refProps.setProperty("foo", "bar");
        refProps.setProperty("hey", "ho");
        
        String mode = EnvironmentProperties.getProperties().getProperty("mode");
        refProps.setProperty("mode", mode);
        String fqdn = EnvironmentProperties.getProperties().getProperty("fqdn");
        refProps.setProperty("fqdn", fqdn);
        
        assertEquals(props, refProps);
    }
    
    @Test
    public void testTenants() throws Exception {
        
        Properties btp = new Properties();
        btp.put("mode", "test");
        EnvironmentProperties.setProperties(btp);
        
        InputStream in = getClass().getResourceAsStream("/properties-tenants.xml");
        Properties props = new TenantAwareProperties();
        PropertyFileReader.read(in, props);
        
        assertEquals("ho", props.getProperty("hey"));
        setTenant("DE");
        assertEquals("ho-DE", props.getProperty("hey"));
        
        btp = new Properties();
        btp.put("mode", "prod");
        EnvironmentProperties.setProperties(btp);
        
        in = getClass().getResourceAsStream("/properties-tenants.xml");
        props = new TenantAwareProperties();
        PropertyFileReader.read(in, props);
        
        assertEquals("ho-prod", props.getProperty("hey"));
        setTenant("UK");
        assertEquals("ho-prod-UK", props.getProperty("hey"));
      
    }
    
    private void setTenant(String name) {
        
        Tenant tenant = new Tenant(name);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(TenantScope.REQUEST_ATTRIBUTE_TENANT, tenant);
        RequestAttributes attributes = new ServletRequestAttributes(req);
        RequestContextHolder.setRequestAttributes(attributes);
    }
    
}
