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

package de.schlund.pfixcore.editor.handlers;
import de.schlund.pfixcore.editor.*;
import de.schlund.pfixcore.editor.resources.*;
import de.schlund.pfixcore.workflow.*;
import de.schlund.pfixcore.workflow.app.*;
import de.schlund.pfixxml.*;
import de.schlund.pfixxml.targets.*;
import java.util.*;
import org.apache.log4j.Category;
import org.w3c.dom.*;


/**
 * AppletInfoFinalizer.java
 *
 *
 * Created: Fri Jul 04 16:40:07 2003
 *
 * @author <a href="mailto:zaich@schlund.de">Volker Zaich</a>
 * @version
 *
 * AppletInfoFinalizer gets all Includes and Images from the actually project
 * and put em into the resultDocument. Maybe directorys will be added later.
 *
 *
 *
 *
 *
 *
 */







public class AppletInfoFinalizer extends ResdocSimpleFinalizer {
    
    private String currdoc = null;
    private static Category LOG = Category.getInstance(ResdocSimpleFinalizer.class.getName());
    
    protected void renderDefault(IWrapperContainer container) throws Exception {
        
        Context                context     = container.getAssociatedContext();
        ContextResourceManager crm         = context.getContextResourceManager();
        EditorSessionStatus    esess       = EditorRes.getEditorSessionStatus(crm);
        EditorSearch           esearch     = EditorRes.getEditorSearch(crm);
        ResultDocument         resdoc      = container.getAssociatedResultDocument();
        TargetGenerator        tgen        = esess.getProduct().getTargetGenerator();
        AuxDependency          currinclude = esess.getCurrentInclude();
        PfixcoreNamespace[]    nspaces     = esess.getProduct().getPfixcoreNamespace();
        TreeSet images = tgen.getDependencyRefCounter().getDependenciesOfType(DependencyType.IMAGE);
        Element iroot   = resdoc.createNode("allimages"); 
        renderAllImages(tgen, images, resdoc, iroot);
        
        for (int i = 0; i < nspaces.length; i++) {              
            PfixcoreNamespace nsp = nspaces[i];
            resdoc.addUsedNamespace(nsp.getPrefix(), nsp.getUri());
        }
        
        // Render the current status of the editor session
        esess.insertStatus(resdoc, resdoc.createNode("cr_editorsession"));
        
        // Render all includes
        TreeSet includes = tgen.getDependencyRefCounter().getDependenciesOfType(DependencyType.TEXT);
        Element root     = resdoc.createNode("allincludes"); 
        EditorHelper.renderIncludesForAppletInfo(tgen, includes, resdoc, root);                                        
        
    }
    
    
    
    
    public void onSuccess(IWrapperContainer container) throws Exception {
        renderDefault(container);
    }
    
    
    
    private void renderAllImages(TargetGenerator tgen, TreeSet images, ResultDocument resdoc, Element root) {
        String  olddir  = "";
        Element elem    = null;
        for (Iterator i = images.iterator(); i.hasNext(); ) {
            AuxDependency curr = (AuxDependency) i.next();
            String docroot = tgen.getDocroot();
            Path path = curr.getPath();
            String dir  = path.getDir();
            String name = path.getName();
            try {
                Element img = resdoc.createSubNode(root, "image");
                img.setAttribute("path", path.getRelative());
                img.setAttribute("name", name);
                if (curr.getModTime() == 0) {
                    img.setAttribute("missing", "true");
                }
                 
            } catch (Exception e) {
                LOG.debug("** Error while getting Image ** - Check your Path \n" + e.getMessage());
                
            }
            

        }
        }

    

    
}
