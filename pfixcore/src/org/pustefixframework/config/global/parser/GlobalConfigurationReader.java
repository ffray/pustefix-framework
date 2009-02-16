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

package org.pustefixframework.config.global.parser;

import java.io.IOException;

import org.pustefixframework.config.customization.PropertiesBasedCustomizationInfo;
import org.pustefixframework.config.global.GlobalConfigurationHolder;

import com.marsching.flexiparse.parser.ClasspathConfiguredParser;
import com.marsching.flexiparse.parser.exception.ParserException;

import de.schlund.pfixxml.config.BuildTimeProperties;
import de.schlund.pfixxml.resources.FileResource;
import de.schlund.pfixxml.resources.ResourceUtil;

public class GlobalConfigurationReader {
    
    public GlobalConfigurationHolder readGlobalConfiguration() throws ParserException {
        // FIXME: Inject ResourceUtil instead of using singleton
        FileResource resource = ResourceUtil.getFileResourceFromDocroot("common/conf/projects.xml");
        PropertiesBasedCustomizationInfo customizationInfo = new PropertiesBasedCustomizationInfo(BuildTimeProperties.getProperties());
        ClasspathConfiguredParser parser = new ClasspathConfiguredParser("META-INF/org/pustefixframework/config/global/parser/global-projects-config.xml");
        try {
            return new GlobalConfigurationHolder(parser.parse(resource.getInputStream(), customizationInfo));
        } catch (IOException e) {
            throw new ParserException("Could not read file " + resource.toString());
        }
    }
    
}
