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

package org.pustefixframework.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Generates a Pustefix webapp, but without the parts generated by Maven's war plugin. Executes in the generate-sources phase because 
 * some tests need files generated by this plugin.
 *
 * @goal generate
 * @phase generate-sources
 * @requiresDependencyResolution compile
 */
@Deprecated
public class PustefixWebappMojo extends AbstractMojo {
    
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    
    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException {

        // allow plugin declaration in parent pom
        if ("pom".equals(project.getPackaging())) {
            return;
        }
        
        getLog().warn("************************************************************************");
        getLog().warn("* You're calling a deprecated Maven plugin goal. You should remove     *");
        getLog().warn("* the 'generate' goal from the 'pustefix-webapp-plugin' configuration. *");
        getLog().warn("************************************************************************");
        
    }

}
