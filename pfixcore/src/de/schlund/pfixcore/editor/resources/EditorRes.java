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

package de.schlund.pfixcore.editor.resources;

import de.schlund.pfixcore.workflow.*;


/**
 * EditorRes.java
 *
 *
 * Created: Thu Nov 29 23:45:23 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public abstract class EditorRes {
    public final static String ESESSION  = "de.schlund.pfixcore.editor.resources.EditorSessionStatus";
    public final static String ESEARCH   = "de.schlund.pfixcore.editor.resources.EditorSearch";
    public static final String ETESTCASE = "de.schlund.pfixcore.editor.resources.CRTestcase";
    
    public static EditorSearch getEditorSearch(ContextResourceManager crm) {
        return (EditorSearch) crm.getResource(ESEARCH);
    }
    
    public static EditorSessionStatus getEditorSessionStatus(ContextResourceManager crm) {
        return (EditorSessionStatus) crm.getResource(ESESSION);
    }
    
    public static CRTestcase getCRTestcase(ContextResourceManager crm) {
        return (CRTestcase) crm.getResource(ETESTCASE);
    }
    
}// EditorRes
