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

package de.schlund.pfixcore.editor;
import java.io.*;
import java.util.*;
import javax.xml.transform.TransformerException;

import de.schlund.pfixcore.editor.resources.*;
import de.schlund.pfixxml.*;
import de.schlund.pfixxml.targets.*;
import de.schlund.pfixxml.util.XPath;
import de.schlund.pfixxml.util.Xml;
import org.apache.log4j.*;
import org.w3c.dom.*;

/**
 * EditorHelper.java
 *
 *
 * Created: Fri Nov 30 16:40:07 2001
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 * @version
 *
 *
 */

public class EditorHelper {
    private static Category CAT = Category.getInstance(EditorHelper.class.getName());
    private static String EDITOR_PERF = "EDITOR_PERF";
    private static Category PERF_LOGGER = Category.getInstance(EDITOR_PERF);
    
    
    public static void doUpdateForAuxDependency(AuxDependency currinc, TargetGenerator tgen) throws Exception {
        //Path path = currinc.getPath();
        TreeSet targets = currinc.getAffectedTargets();
        HashSet pages = new HashSet();
        for (Iterator i = targets.iterator(); i.hasNext();) {
            Target target = (Target) i.next();
            pages.addAll(target.getPageInfos());
        }

        Iterator j = pages.iterator();
        Target toplevel;
        if (j.hasNext()) {
            PageInfo pinfo = (PageInfo) j.next();
            if (pinfo.getTargetGenerator() == tgen) {
                toplevel = tgen.getPageTargetTree().getTargetForPageInfo(pinfo);
                if (toplevel == null) {
                    CAT.error("\n **************** Got 'null' target for PageInfo " + pinfo.getName() + "!! *****************");
                } else {
                    try {
                        toplevel.getValue();
                    } catch (Exception e) {
                        CAT.warn("*** CAUTION: Exception on updating of " + toplevel.getTargetKey());
                    }
                }
            }
            for (; j.hasNext();) {
                pinfo = (PageInfo) j.next();
                if (pinfo.getTargetGenerator() != tgen)
                    continue;
                toplevel = tgen.getPageTargetTree().getTargetForPageInfo(pinfo);
                if (toplevel == null) {
                    CAT.error("\n **************** Got 'null' target for PageInfo " + pinfo.getName() + "!! *****************");
                } else {
                    EditorPageUpdater.getInstance().addTarget(toplevel);
                }
            }
        }
    }

    public static Node createEmptyPart(Document doc, AuxDependency include) {
        String name = include.getPart();
        Element root = doc.getDocumentElement();
        Element part = doc.createElement("part");
        part.setAttribute("name", name);
        root.appendChild(doc.createTextNode("\n  "));
        root.appendChild(part);
        root.appendChild(doc.createTextNode("\n"));
        return part;
    }

    public static void checkForFile(Path path, PfixcoreNamespace[] nspaces) throws Exception {
        File incfile = path.resolve();
        if (incfile.exists() && (!incfile.canRead() || !incfile.canWrite() || !incfile.isFile())) {
            throw new XMLException("File " + path.getRelative() + " is not accessible!");
        }
        if (!incfile.exists()) {
            CAT.debug("===> Going to create " + path.getRelative());
            if (incfile.createNewFile()) {
                Document skel = Xml.createDocument();
                Element root = skel.createElement("include_parts");
                if (nspaces != null) {
                    for (int i = 0; i < nspaces.length; i++) {
                        root.setAttribute("xmlns:" + nspaces[i].getPrefix(), nspaces[i].getUri());
                    }
                }
                skel.appendChild(root);
                root.appendChild(skel.createComment("Append include parts here..."));
                Xml.serialize(skel, incfile, false, true);
            } else {
                throw new XMLException("Couldn't generate new file " + path.getRelative());
            }
        }
    }

    private static String constructBackupDir(EditorSessionStatus ess, AuxDependency inc) {
        Path path = inc.getPath();
        if (inc.getType().equals(DependencyType.TEXT)) {
            String part = inc.getPart();
            String prod = inc.getProduct();
            return ess.getBackupDir() + "/" + path.getRelative() + "/" + part + "/" + prod;
        } else {
            return ess.getBackupDir() + "/" + path.getRelative();
        }
    }

    private static File constructBackupFile(EditorSessionStatus ess, AuxDependency inc) {
        String user = ess.getUser().getId();
        String time = new Date().toString();
        String dir = constructBackupDir(ess, inc);
        String name = time + " [" + user + "]";
        File tmp = new File(dir);
        if (tmp.exists() || tmp.mkdirs()) {
            return new File(dir, name);
        } else {
            CAT.warn("Couldn't create backup dir " + dir);
            return null;
        }
    }

    public static void createBackupImage(EditorSessionStatus ess, File from) {
        AuxDependency inc = ess.getCurrentImage();
        File file = constructBackupFile(ess, inc);
        if (file != null) {
            try {
                FileInputStream fin = new FileInputStream(from);
                FileOutputStream fout = new FileOutputStream(file);
                byte[] b = new byte[4096];
                int num = 0;
                while ((num = fin.read(b)) != -1) {
                    fout.write(b, 0, num);
                }
            } catch (Exception e) {
                CAT.warn("Couldn't serialize into backup file " + file.getAbsolutePath());
            }
        }
    }

    public static void createBackup(EditorSessionStatus ess, AuxDependency inc, Node tosave) {
        // CAUTION: Don't do it like below, the inc has to be supplied by the caller!!!
        // AuxDependency inc  = ess.getCurrentInclude();
        File file = constructBackupFile(ess, inc);
        if (file != null) {
            try {
                Xml.serialize((Element) tosave, file, false, true);
            } catch (Exception e) {
                CAT.warn("Couldn't serialize into backup file " + file.getAbsolutePath());
            }
        }
    }

    public static String[] getBackupNames(EditorSessionStatus ess, AuxDependency inc) {
        String dir = constructBackupDir(ess, inc);
        File file = new File(dir);
        File[] entries = file.listFiles();
        if (entries != null && entries.length > 0) {
            Comparator comp = new ReverseCreationComp();
            Arrays.sort(entries, comp);
            String[] names = new String[entries.length];
            for (int i = 0; i < entries.length; i++) {
                names[i] = entries[i].getName();
            }
            return names;
        } else {
            return null;
        }
    }

    public static File getBackupImageFile(EditorSessionStatus ess, AuxDependency aux, String filename) throws Exception {
        String name = constructBackupDir(ess, aux) + "/" + filename;
        File file = new File(name);
        if (file.exists() && file.canRead() && file.isFile()) {
            return file;
        } else {
            return null;
        }
    }

    public static Node getBackupContent(EditorSessionStatus ess, AuxDependency inc, String filename, boolean kill) throws Exception {
        String name = constructBackupDir(ess, inc) + "/" + filename;
        File file = new File(name);
        if (file.exists() && file.canRead() && file.isFile()) {
            Document doc = Xml.parse(file);
            if (kill) {
                file.delete();
            }
            return doc.getDocumentElement();
        } else {
            return null;
        }
    }

    public static void renderBackupOptions(EditorSessionStatus ess, AuxDependency inc, ResultDocument resdoc, Element root) {
        String[] names = getBackupNames(ess, inc);
        if (names != null && names.length > 0) {
            for (int i = 0; i < names.length; i++) {
                ResultDocument.addTextChild(root, "option", names[i]);
            }
        }
    }

    public static void renderSingleTarget(Target target, ResultDocument resdoc, Element root) {
        Element elem = resdoc.createSubNode(root, "target");
        elem.setAttribute("type", target.getType().toString());
        elem.setAttribute("name", target.getTargetKey());

        if (target.getType() == TargetType.XML_VIRTUAL || target.getType() == TargetType.XSL_VIRTUAL) {
            renderSingleTarget(target.getXMLSource(), resdoc, elem);
            renderSingleTarget(target.getXSLSource(), resdoc, elem);
        }
    }

    public static void renderAffectedPages(EditorSessionStatus ess, AuxDependency aux, ResultDocument resdoc, Element root) throws Exception {
        TreeSet targets = aux.getAffectedTargets();
        HashSet prods = new HashSet();
        TreeSet pages = new TreeSet();

        prods.addAll(Arrays.asList(EditorProductFactory.getInstance().getAllEditorProducts()));

        for (Iterator i = targets.iterator(); i.hasNext();) {
            Target target = (Target) i.next();
            pages.addAll(target.getPageInfos());
        }
        renderAffectedPages(ess, pages, prods, resdoc, root);
    }

    public static void renderAffectedPages(EditorSessionStatus ess, Target target, ResultDocument resdoc, Element root) throws Exception {
        TargetType type = target.getType();
        HashSet prods = new HashSet();
        TreeSet pages = target.getPageInfos();

        if (type.equals(TargetType.XML_LEAF) || type.equals(TargetType.XSL_LEAF)) {
            prods.addAll(Arrays.asList(EditorProductFactory.getInstance().getAllEditorProducts()));
        } else {
            prods.add(ess.getProduct());
        }
        renderAffectedPages(ess, pages, prods, resdoc, root);
    }

    private static void renderAffectedPages(EditorSessionStatus ess, TreeSet pages, HashSet prods, ResultDocument resdoc, Element root) {

        for (Iterator i = prods.iterator(); i.hasNext();) {
            EditorProduct prod = (EditorProduct) i.next();
            TargetGenerator gen = prod.getTargetGenerator();

            Element elem = resdoc.createSubNode(root, "product");
            elem.setAttribute("name", prod.getName());
            elem.setAttribute("comment", prod.getComment());

            for (Iterator j = pages.iterator(); j.hasNext();) {
                PageInfo pinfo = (PageInfo) j.next();
                TargetGenerator pgen = pinfo.getTargetGenerator();
                if (pgen == gen) {
                    Element newnode = resdoc.createSubNode(elem, "page");
                    newnode.setAttribute("name", pinfo.getName());
                    Target toplevel = pgen.getPageTargetTree().getTargetForPageInfo(pinfo);
                    try {
                        if (toplevel.needsUpdate()) {
                            EditorPageUpdater.getInstance().addTarget(toplevel);
                            newnode.setAttribute("uptodate", "false");
                        } else {
                            newnode.setAttribute("uptodate", "true");
                        }
                    } catch (Exception e) {
                        newnode.setAttribute("uptodate", "???");
                    }
                }
            }
        }
    }

    public static void resetIncludeDocumentTarget(TargetGenerator tgen, AuxDependency include) throws Exception {
        if (include.getType() != DependencyType.TEXT) {
            throw new XMLException("Dependency is not of Type TEXT");
        }
        Path path = include.getPath();

        //String name = path.substring(docroot.length());
        /* Target target  = tgen.createXMLLeafTarget(name);
         target.resetModTime();*/
        IncludeDocument includeDocument = IncludeDocumentFactory.getInstance().getIncludeDocument(path, true);
        includeDocument.resetModTime();

        CAT.debug("==========> After reset: Modtime for IncludeDocument: " + includeDocument.getModTime());
    }

    public static Document getIncludeDocument(TargetGenerator tgen, AuxDependency include) throws Exception {
        if (include.getType() != DependencyType.TEXT) {
            throw new XMLException("Dependency is not of Type TEXT");
        }
        Path path = include.getPath();
        File file = path.resolve();
        if (!file.exists()) {
            return null;
        }
        CAT.debug("**************** getIncludeDocument: " + path.getRelative());
        Object LOCK = FileLockFactory.getInstance().getLockObj(path);
        synchronized (LOCK) {
            Document doc = null;
            IncludeDocument iDoc = IncludeDocumentFactory.getInstance().getIncludeDocument(path, false);
            doc = iDoc.getDocument();
            return doc;
        }
    }

    public static Element getIncludePart(Document doc, AuxDependency include) throws Exception {
        String part = include.getPart();
        Path path = include.getPath();
        return (Element) XPath.selectNode(doc, "/include_parts/part[@name = '" + part + "']");
    }

    public static Element getIncludePart(TargetGenerator tgen, AuxDependency include) throws Exception {
        Document doc = getIncludeDocument(tgen, include);
        if (doc == null) {
            return null;
        } else {
            return getIncludePart(doc, include);
        }
    }

    public static void renderBranchOptions(EditorSessionStatus esess, AuxDependency inc, ResultDocument resdoc, Element root) throws Exception {

        Path path = inc.getPath();
        String part = inc.getPart();
        TargetGenerator tgen = esess.getProduct().getTargetGenerator();
        Element elem = getIncludePart(tgen, inc);
        if (elem == null)
            return;

        List nl = XPath.select(elem, "./product");
        HashSet affedprod = new HashSet();

        for (int i = 0; i < nl.size(); i++) {
            Element tmp = (Element) nl.get(i);
            String prod = tmp.getAttribute("name");
            if (prod == null)
                continue;

            AuxDependency test = AuxDependencyFactory.getInstance().getAuxDependency(DependencyType.TEXT, path, part, prod);

            EditorProduct[] eprods = null;
            if (prod.equals("default")) {
                eprods = EditorProductFactory.getInstance().getAllEditorProducts();
            } else {
                EditorProduct tmpprod = EditorProductFactory.getInstance().getEditorProduct(prod);
                if (tmpprod != null) {
                    eprods = new EditorProduct[] { tmpprod };
                }
            }

            if (eprods != null) {
                for (int j = 0; j < eprods.length; j++) {
                    EditorProduct epr = eprods[j];
                    TreeSet allinc = epr.getTargetGenerator().getDependencyRefCounter().getDependenciesOfType(DependencyType.TEXT);
                    if (allinc.contains(test)) {
                        affedprod.add(epr);
                    }
                }
            }
        }

        for (Iterator k = affedprod.iterator(); k.hasNext();) {
            EditorProduct tmp = (EditorProduct) k.next();
            Element usedby = resdoc.createSubNode(root, "usedbyproduct");
            usedby.setAttribute("name", tmp.getName());
            usedby.setAttribute("comment", tmp.getComment());
        }
    }

    public static void renderIncludeContent(TargetGenerator tgen, AuxDependency include, ResultDocument resdoc, Element root) throws Exception {
        Element part = getIncludePart(tgen, include);
        if (part != null) {
            Node cinfo = resdoc.getSPDocument().getDocument().importNode(part, true);
            root.appendChild(cinfo);
        }
    }

    public static void renderTargetContent(Target target, ResultDocument resdoc, Element root) {
        String key = target.getTargetKey();
        File cache = target.getTargetGenerator().getDisccachedir();
        File docroot = target.getTargetGenerator().getDocroot();
        TargetType type = target.getType();
        File file;

        if (type == TargetType.XML_LEAF || type == TargetType.XSL_LEAF) {
            file = new File(docroot, key);
        } else {
            file = new File(cache, key);
        }

        try {
            Document cdoc = Xml.parse(file);
            Node cinfo = resdoc.getSPDocument().getDocument().importNode(cdoc.getDocumentElement(), true);
            root.appendChild(cinfo);
        } catch (Exception e) {
            // 
        }
    }

    public static void renderAuxfiles(Target target, ResultDocument resdoc, Element root) {
        TreeSet allaux = new TreeSet();
        int count = 0;
        getAuxdepsForTarget(allaux, target, false, DependencyType.FILE);
        for (Iterator i = allaux.iterator(); i.hasNext();) {
            AuxDependency aux = (AuxDependency) i.next();
            Element elem = resdoc.createSubNode(root, "auxfile");
            elem.setAttribute("dir", aux.getPath().getDir());
            elem.setAttribute("path", aux.getPath().getRelative());
            elem.setAttribute("count", "" + count++);
        }
    }

    public static void renderImages(Target target, ResultDocument resdoc, Element root) {
        TreeSet allimgs = new TreeSet();
        TreeSet allinc = new TreeSet();
        getAuxdepsForTarget(allimgs, target, false, DependencyType.IMAGE);
        // get the includes for this target (so recurse has to be "false" here!
        getAuxdepsForTarget(allinc, target, false, DependencyType.TEXT);
        // we want to iterate over these, but still add more to allinc.
        TreeSet tmp = new TreeSet(allinc);
        for (Iterator i = tmp.iterator(); i.hasNext();) {
            AuxDependency aux = (AuxDependency) i.next();
            // Now get all includes recursively
            getAuxdepsForInclude(allinc, aux, true, DependencyType.TEXT);
        }
        // now for all includes, get the images they use
        for (Iterator i = allinc.iterator(); i.hasNext();) {
            AuxDependency aux = (AuxDependency) i.next();
            // we put what we find into allimgs
            getAuxdepsForInclude(allimgs, aux, false, DependencyType.IMAGE);
        }
        int j = 0;
        for (Iterator i = allimgs.iterator(); i.hasNext();) {
            AuxDependency aux = (AuxDependency) i.next();
            Element elem = resdoc.createSubNode(root, "image");
            elem.setAttribute("dir", aux.getPath().getDir());
            elem.setAttribute("path", aux.getPath().getRelative());
            elem.setAttribute("modtime", "" + aux.getModTime());
            elem.setAttribute("count", "" + j++);
        }
    }

    public static void renderIncludes(AuxDependency aux, ResultDocument resdoc, Element root) {
        TreeSet allaux = aux.getChildren();
        ArrayList count = new ArrayList();
        count.add(new Integer(0));
        if (allaux != null) {
            renderIncludesRec(allaux, resdoc, root, count);
        }
    }

    public static void renderIncludes(Target target, ResultDocument resdoc, Element root) {
        TreeSet allaux = new TreeSet();
        ArrayList count = new ArrayList();
        count.add(new Integer(0));
        getAuxdepsForTarget(allaux, target, false, DependencyType.TEXT);
        renderIncludesRec(allaux, resdoc, root, count);
    }

    private static void renderIncludesRec(TreeSet allaux, ResultDocument resdoc, Element root, ArrayList count) {
        for (Iterator i = allaux.iterator(); i.hasNext();) {
            AuxDependency aux = (AuxDependency) i.next();
            if (aux.getType() == DependencyType.TEXT) {
                Integer cnt = (Integer) count.get(0);
                TreeSet sub = new TreeSet();
                getAuxdepsForInclude(sub, aux, false, DependencyType.TEXT);
                Element elem = resdoc.createSubNode(root, "include");
                elem.setAttribute("dir", aux.getPath().getDir());
                elem.setAttribute("path", aux.getPath().getRelative());
                elem.setAttribute("part", aux.getPart());
                elem.setAttribute("count", cnt.toString());
                count.set(0, new Integer(cnt.intValue() + 1));
                if (sub != null && !sub.isEmpty()) {
                    renderIncludesRec(sub, resdoc, elem, count);
                }
            }
        }
    }

    public static void renderIncludesFlatRecursive(Target target, ResultDocument resdoc, Element root) {
        TreeSet allaux = new TreeSet();
        getAuxdepsForTarget(allaux, target, true, DependencyType.TEXT);
        int j = 0;
        for (Iterator i = allaux.iterator(); i.hasNext();) {
            AuxDependency aux = (AuxDependency) i.next();
            Element elem = resdoc.createSubNode(root, "include");
            elem.setAttribute("dir", aux.getPath().getDir());
            elem.setAttribute("path", aux.getPath().getRelative());
            elem.setAttribute("part", aux.getPart());
            elem.setAttribute("product", aux.getProduct());
            elem.setAttribute("count", "" + j++);
        }
    }

    public static void renderImagesFlatRecursive(Target target, ResultDocument resdoc, Element root) {
        TreeSet allaux = new TreeSet();
        getAuxdepsForTarget(allaux, target, true, DependencyType.IMAGE);
        int j = 0;
        for (Iterator i = allaux.iterator(); i.hasNext();) {
            AuxDependency aux = (AuxDependency) i.next();
            Element elem = resdoc.createSubNode(root, "image");
            elem.setAttribute("dir", aux.getPath().getDir());
            elem.setAttribute("path", aux.getPath().getRelative());
            elem.setAttribute("modtime", "" + aux.getModTime());
            elem.setAttribute("count", "" + j++);
        }
    }

    public static void renderImagesFlatRecursive(AuxDependency auxin, ResultDocument resdoc, Element root) {
        TreeSet allaux = new TreeSet();
        getAuxdepsForInclude(allaux, auxin, true, DependencyType.IMAGE);
        int j = 0;
        for (Iterator i = allaux.iterator(); i.hasNext();) {
            AuxDependency aux = (AuxDependency) i.next();
            Element elem = resdoc.createSubNode(root, "image");
            elem.setAttribute("dir", aux.getPath().getDir());
            elem.setAttribute("path", aux.getPath().getRelative());
            elem.setAttribute("modtime", "" + aux.getModTime());
            elem.setAttribute("count", "" + j++);
        }
    }

    private static void getAuxdepsForTarget(TreeSet bucket, Target target, boolean recurse, DependencyType type) {
        if (!(target instanceof VirtualTarget)) {
            return;
        }
        AuxDependencyManager manager = ((VirtualTarget) target).getAuxDependencyManager();
        if (manager != null) {
            TreeSet topaux = manager.getChildren();
            for (Iterator i = topaux.iterator(); i.hasNext();) {
                AuxDependency aux = (AuxDependency) i.next();
                if (aux.getType() == type) {
                    bucket.add(aux);
                }
                if (recurse) {
                    getAuxdepsForInclude(bucket, aux, recurse, type);
                }
            }
            if (recurse) {
                if (target.getType() == TargetType.XML_VIRTUAL || target.getType() == TargetType.XSL_VIRTUAL) {
                    getAuxdepsForTarget(bucket, target.getXMLSource(), true, type);
                    getAuxdepsForTarget(bucket, target.getXSLSource(), true, type);
                }
            }
        }
    }

    private static void getAuxdepsForInclude(TreeSet bucket, AuxDependency aux, boolean recurse, DependencyType type) {
        TreeSet children = aux.getChildren();
        if (children != null) {
            for (Iterator i = children.iterator(); i.hasNext();) {
                AuxDependency child = (AuxDependency) i.next();
                if (child.getType() == type) {
                    bucket.add(child);
                }
                if (recurse) {
                    getAuxdepsForInclude(bucket, child, recurse, type);
                }
            }
        }
    }


    public static void renderIncludesForAppletInfo(TargetGenerator tgen, TreeSet includes, ResultDocument resdoc, Element root) {
        for (Iterator i = includes.iterator(); i.hasNext();) {
            AuxDependency curr = (AuxDependency) i.next();
            Path path = curr.getPath();
            String dir = path.getDir();
            String part = curr.getPart();
            String product = curr.getProduct();

            Element inc = resdoc.createSubNode(root, "include");
            inc.setAttribute("path", path.getRelative());
            inc.setAttribute("part", part);
            inc.setAttribute("product", product);
        }
            
    }

    
    public static void renderAllIncludesForNavigation(TreeSet includes, ResultDocument resdoc, Element root) {
        String olddir = "";
        Path oldpath = null;
        Element direlem = null;
        Element pathelem = null;
        for (Iterator i = includes.iterator(); i.hasNext();) {
            AuxDependency curr = (AuxDependency) i.next();
            Path path = curr.getPath();
            String dir = path.getDir();
            String part = curr.getPart();
            String product = curr.getProduct();
            if (!olddir.equals(dir) || olddir.equals("")) {
                direlem = resdoc.createSubNode(root, "directory");
                direlem.setAttribute("name", dir);
                olddir = dir;
            }
            if (!path.equals(oldpath) || olddir.equals("")) {
                pathelem = resdoc.createSubNode(direlem, "path");
                pathelem.setAttribute("name", path.getRelative());
                oldpath = path;
            }
            Element inc = resdoc.createSubNode(pathelem, "include");
            inc.setAttribute("path", path.getRelative());
            inc.setAttribute("part", part);
            inc.setAttribute("product", product);
        }
    }

    public static void renderAllPatternMatchingIncludes(EditorSearch es, ResultDocument resdoc, Element root, String type) {
        if (es.getStatus() == EditorSearch.SCODE_OK) {
            TreeSet includes = type.equals(EditorSearch.INCLUDE) ? es.getResultSet() : es.getDynResultSet();
            String olddir = "";
            Path oldpath = null;
            Element direlem = null;
            Element pathelem = null;
            for (Iterator i = includes.iterator(); i.hasNext();) {
                AuxDependency curr = (AuxDependency) i.next();
                Path path = curr.getPath();
                String dir = path.getDir();
                String part = curr.getPart();
                String product = curr.getProduct();
                if (!olddir.equals(dir) || olddir.equals("")) {
                    direlem = resdoc.createSubNode(root, "directory");
                    direlem.setAttribute("name", dir);
                    olddir = dir;
                }
                if (!path.equals(oldpath) || olddir.equals("")) {
                    pathelem = resdoc.createSubNode(direlem, "path");
                    pathelem.setAttribute("name", path.getRelative());
                    oldpath = path;
                }
                Element inc = resdoc.createSubNode(pathelem, "include");
                inc.setAttribute("path", path.getRelative());
                inc.setAttribute("part", part);
                inc.setAttribute("product", product);
                EditorSearchContext[] matches = type.equals(EditorSearch.INCLUDE) ? es.getSearchContexts(curr) : es.getDynSearchContexts(curr);
                for (int j = 0; j < matches.length; j++) {
                    Element match = resdoc.createSubNode(inc, "match");
                    EditorSearchContext tmp = matches[j];
                    match.setAttribute("match", tmp.getMatch());
                    match.setAttribute("pre", tmp.getPre());
                    match.setAttribute("post", tmp.getPost());
                }
            }
        }
    }

    public static HashSet getAffectedProductsForInclude(AuxDependency current_include)
        throws Exception, XMLException, TransformerException {
      
      
        String include_prod = current_include.getProduct();      
            
        long start_time = 0;
        if(PERF_LOGGER.isInfoEnabled()) {
            start_time = System.currentTimeMillis();     
        }
        
        
        HashSet affedprod = new HashSet();


    
        EditorProduct[] eprods = null;
        if (include_prod.equals("default")) {
            eprods = EditorProductFactory.getInstance().getAllEditorProducts();
        } else {
            EditorProduct tmpprod = EditorProductFactory.getInstance().getEditorProduct(include_prod);
            if (tmpprod != null) {
                eprods = new EditorProduct[] { tmpprod };
            }
        }

        if (eprods != null) {
            PERF_LOGGER.info("1:"+(System.currentTimeMillis() - start_time));
            for (int j = 0; j < eprods.length; j++) {
                EditorProduct epr = eprods[j];
                TreeSet allinc = epr.getTargetGenerator().getDependencyRefCounter().getDependenciesOfType(DependencyType.TEXT);
                PERF_LOGGER.info("getDependenciesOfType/" + epr.getName() + "=>" + (System.currentTimeMillis() - start_time));
                if (allinc.contains(current_include)) {
                    affedprod.add(epr);
                }
            }
        }
        
        if(PERF_LOGGER.isInfoEnabled()) {
            long length = System.currentTimeMillis() - start_time;
            PERF_LOGGER.info(EditorHelper.class.getName()+"#getAffectedProductsForInclude: "+length);
        }

        return affedprod;
    }

    /** Get all products which use the image specified by its path */
    public static HashSet getAffectedProductsForImage(Path path_to_image) throws Exception {
        
        long start_time = 0;
        if(PERF_LOGGER.isInfoEnabled()) {
            start_time = System.currentTimeMillis();
        }
        HashSet affected_products = new HashSet();

        AuxDependency auxdep = AuxDependencyFactory.getInstance().getAuxDependency(DependencyType.IMAGE, path_to_image, null, null);
        TreeSet afftected_targets = auxdep.getAffectedTargets();

        // get all targetgenerators from the affected targets
        HashSet tgens_affected_targtes = new HashSet();
        for (Iterator iter = afftected_targets.iterator(); iter.hasNext();) {
            Target current_target = (Target) iter.next();
            TargetGenerator tgen = current_target.getTargetGenerator();
            tgens_affected_targtes.add(tgen);
        }

        // get all targetgenerators from all products
        EditorProduct[] all_products = EditorProductFactory.getInstance().getAllEditorProducts();
        for (int i = 0; i < all_products.length; i++) {
            TargetGenerator tgen = all_products[i].getTargetGenerator();
            if (tgens_affected_targtes.contains(tgen)) {
                affected_products.add(all_products[i]);
            }
        }
        
        if(PERF_LOGGER.isInfoEnabled()) {
            long length = System.currentTimeMillis() - start_time;
            PERF_LOGGER.info(EditorHelper.class.getName()+"#getAffectedProductsForImage: "+length);
        }
        
        return affected_products;
    }

} // EditorHelper
