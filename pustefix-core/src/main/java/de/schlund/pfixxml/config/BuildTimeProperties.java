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

package de.schlund.pfixxml.config;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.schlund.pfixxml.resources.ResourceUtil;

/**
 * Provides easy access to properties stored at buildtime 
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class BuildTimeProperties {
    private static final String PATH = "WEB-INF/buildtime.prop";
    
    private static Properties props = null;

    public static Properties getProperties() {
        if (BuildTimeProperties.props == null) {
            Properties props = new Properties();
            try {
                props.load(ResourceUtil.getFileResourceFromDocroot(PATH).getInputStream());
                BuildTimeProperties.props = props;
            } catch (IOException e) {
                Logger.getLogger(BuildTimeProperties.class).error(
                        "Could not load buildtime properties!");
            }
            return props;
        }
        return BuildTimeProperties.props;
    }

    public static void setProperties(Properties p) {
        props = p; 
    }
    
    public static void generate(Properties props, String mode, String machine, String fqdn, String uid) throws IOException {
        props.setProperty("mode", mode);
        props.setProperty("machine", machine);
        props.setProperty("fqdn", fqdn);
        props.setProperty("uid", uid);
        props.store(ResourceUtil.getFileResourceFromDocroot(PATH).getOutputStream(), "Properties used at buildtime");
    }
}