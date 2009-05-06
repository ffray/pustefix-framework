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

package de.schlund.pfixxml.targets;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * PageTargetTree.java
 *
 *
 * Created: Fri Jul 20 13:38:09 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 *
 *
 */

public class PageTargetTree {
    TreeMap toplevels = new TreeMap();
    TreeMap pageinfos = new TreeMap();

    protected void addEntry(PageInfo pageinfo, Target target) {
        synchronized (toplevels) {
            if (toplevels.get(pageinfo) == null) {
                toplevels.put(pageinfo, target);
                String name = pageinfo.getName();
                if (pageinfos.get(name) == null) {
                    pageinfos.put(name, new TreeSet());
                }
                TreeSet pinfos = (TreeSet) pageinfos.get(name);
                pinfos.add(pageinfo);
            } else {
                throw new RuntimeException("Can't have another top-level target '" +
                                           target.getTargetKey() + "' for the same page '" +
                                           pageinfo.getName() + "' variant: '" + pageinfo.getVariant() + "'");
            }
        }
    }

    public TreeSet getPageInfoForPageName(String name) {
        synchronized (pageinfos) {
            return (TreeSet) pageinfos.get(name);
        }
    }

    
    public TreeSet getPageInfos() {
        synchronized (toplevels) {
            return new TreeSet(toplevels.keySet());
        }
    }

    public TreeSet getToplevelTargets() {
        synchronized (toplevels) {
            return new TreeSet(toplevels.values());
        }
    }

    public Target getTargetForPageInfo(PageInfo pinfo) {
        synchronized (toplevels) {
            return (Target) toplevels.get(pinfo);
        }
    }
    
    public void initTargets() {
        synchronized (toplevels) {
            for (Iterator i = toplevels.keySet().iterator(); i.hasNext(); ) {
                PageInfo pageinfo = (PageInfo) i.next();
                Target   top      = (Target) toplevels.get(pageinfo);
                addPageInfoToTarget(pageinfo, top);
            }
        }
    }
    
    private void addPageInfoToTarget(PageInfo page, Target target) {
        ((TargetImpl) target).addPageInfo(page);
        Target xmlsource = target.getXMLSource();
        if (xmlsource != null) {
            addPageInfoToTarget(page, xmlsource);
        }
        
        Target xslsource = target.getXSLSource();
        if (xslsource != null) {
            addPageInfoToTarget(page, xslsource);
        }
    }

}// PageTargetTree