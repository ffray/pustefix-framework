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

package de.schlund.pfixcore.lucefix;

import java.io.IOException;

import org.apache.lucene.queryParser.ParseException;

import de.schlund.pfixcore.generator.IHandler;
import de.schlund.pfixcore.generator.IWrapper;
import de.schlund.pfixcore.lucefix.wrappers.Search;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.util.statuscodes.StatusCodeLib;

public class SearchHandler implements IHandler {

    private static final String CSEARCH = "de.schlund.pfixcore.lucefix.ContextSearch";

    public void handleSubmittedData(Context context, IWrapper wrapper) throws ParseException{


        ContextSearch csearch = (ContextSearch) context.getContextResourceManager().getResource(CSEARCH);
        Search search = (Search) wrapper;


        if (search.getDoit() != null && search.getDoit().booleanValue()) {
            String content = search.getContents();
            String tags = search.getTags();
            if (tags != null){
            	// replace ":" with "\:"
            	tags = tags.replace(":", "\\:");
            }
            String attribKey = search.getAttribkeys();
            String attribValue = search.getAttribvalues();
            String comments = search.getComments();
            try {
				csearch.search(content, tags, attribKey, attribValue, comments);
			} catch (IOException e) {
				search.addSCodeDoit(StatusCodeLib.PFIXCORE_LUCEFIX_INDEX_NOT_INITED);
			} 
        }
    }

    public void retrieveCurrentStatus(Context context, IWrapper wrapper) throws Exception {
        // TODO Auto-generated method stub

    }

    public boolean isActive(Context context) throws Exception {
        return true;
    }

    public boolean needsData(Context context) throws Exception {
        return false;
    }

    public boolean prerequisitesMet(Context context) throws Exception {
        return true;
    }
}
