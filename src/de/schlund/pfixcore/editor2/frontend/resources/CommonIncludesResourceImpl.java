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

package de.schlund.pfixcore.editor2.frontend.resources;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.schlund.pfixcore.editor2.core.dom.Image;
import de.schlund.pfixcore.editor2.core.dom.IncludePart;
import de.schlund.pfixcore.editor2.core.dom.IncludePartThemeVariant;
import de.schlund.pfixcore.editor2.core.dom.Page;
import de.schlund.pfixcore.editor2.core.dom.Project;
import de.schlund.pfixcore.editor2.core.dom.Theme;
import de.schlund.pfixcore.editor2.core.exception.EditorException;
import de.schlund.pfixcore.editor2.core.exception.EditorIOException;
import de.schlund.pfixcore.editor2.core.exception.EditorIncludeHasChangedException;
import de.schlund.pfixcore.editor2.core.exception.EditorParsingException;
import de.schlund.pfixcore.editor2.core.exception.EditorSecurityException;
import de.schlund.pfixcore.editor2.frontend.util.DiffUtil;
import de.schlund.pfixcore.editor2.frontend.util.EditorResourceLocator;
import de.schlund.pfixcore.editor2.frontend.util.SpringBeanLocator;
import de.schlund.pfixcore.workflow.Context;
import de.schlund.pfixxml.ResultDocument;
import de.schlund.pfixxml.util.Xml;

public abstract class CommonIncludesResourceImpl implements
        CommonIncludesResource {

    private Context context;

    private IncludePartThemeVariant selectedIncludePart;

    protected abstract boolean securityMayCreateIncludePartThemeVariant(
            IncludePart includePart, Theme theme);

    protected abstract IncludePartThemeVariant internalSelectIncludePart(
            Project project, String path, String part, String theme);

    protected abstract Collection getPossibleThemes(
            IncludePartThemeVariant selectedIncludePart, Project project,
            Collection pages);

    protected abstract boolean securityMayEditIncludePartThemeVariant(
            IncludePartThemeVariant variant);

    protected abstract void renderAllIncludes(ResultDocument resdoc,
            Element elem, Project project);

    public void init(Context context) throws Exception {
        this.context = context;
    }

    public void insertStatus(ResultDocument resdoc, Element elem)
            throws Exception {
        // System.out.println("In IS");
        Project project = EditorResourceLocator.getProjectsResource(context)
                .getSelectedProject();
        if (project != null) {
            this.renderAllIncludes(resdoc, elem, project);
        }

        if (this.selectedIncludePart != null) {
            // System.out.println("In IS: HASH: " + this.selectedIncludePart.getMD5());
            Element currentInclude = resdoc.createSubNode(elem,
                    "currentinclude");
            currentInclude.setAttribute("path", this.selectedIncludePart
                    .getIncludePart().getIncludeFile().getPath());
            currentInclude.setAttribute("part", this.selectedIncludePart
                    .getIncludePart().getName());
            currentInclude.setAttribute("theme", this.selectedIncludePart
                    .getTheme().getName());
            currentInclude.setAttribute("hash", this.selectedIncludePart
                    .getMD5());
            if (this
                    .securityMayEditIncludePartThemeVariant(this.selectedIncludePart)) {
                currentInclude.setAttribute("mayEdit", "true");
            } else {
                currentInclude.setAttribute("mayEdit", "false");
            }

            // Render possible new branches
            Collection pages = this.selectedIncludePart.getAffectedPages();
            Collection themes = this.getPossibleThemes(
                    this.selectedIncludePart, project, pages);
            if (!themes.isEmpty()) {
                Element possibleThemes = resdoc.createSubNode(currentInclude,
                        "possiblethemes");
                for (Iterator i = themes.iterator(); i.hasNext();) {
                    Theme theme = (Theme) i.next();
                    ResultDocument.addTextChild(possibleThemes, "option", theme
                            .getName());
                }
            }

            // Render backups
            Collection backups = SpringBeanLocator.getBackupService()
                    .listIncludeVersions(this.selectedIncludePart);
            if (!backups.isEmpty()) {
                Element backupsNode = resdoc.createSubNode(currentInclude,
                        "backups");
                for (Iterator i = backups.iterator(); i.hasNext();) {
                    String version = (String) i.next();
                    ResultDocument.addTextChild(backupsNode, "option", version);
                }
            }

            // Render affected pages

            if (!pages.isEmpty()) {
                Element pagesNode = resdoc.createSubNode(currentInclude,
                        "pages");
                HashMap projectNodes = new HashMap();
                for (Iterator i = pages.iterator(); i.hasNext();) {
                    Page page = (Page) i.next();
                    Element projectNode;
                    if (projectNodes.containsKey(page.getProject())) {
                        projectNode = (Element) projectNodes.get(page
                                .getProject());
                    } else {
                        projectNode = resdoc
                                .createSubNode(pagesNode, "project");
                        projectNode.setAttribute("name", page.getProject()
                                .getName());
                        projectNodes.put(page.getProject(), projectNode);
                    }
                    Element pageNode = resdoc
                            .createSubNode(projectNode, "page");
                    pageNode.setAttribute("name", page.getName());
                    if (page.getVariant() != null) {
                        pageNode.setAttribute("variant", page.getVariant()
                                .getName());
                    }
                }
            }

            // Render includes
            TreeSet includes = new TreeSet(this.selectedIncludePart
                    .getIncludeDependencies(false));
            if (!includes.isEmpty()) {
                Element includesNode = resdoc.createSubNode(currentInclude,
                        "includes");
                for (Iterator i = includes.iterator(); i.hasNext();) {
                    IncludePartThemeVariant variant = (IncludePartThemeVariant) i
                            .next();
                    this.renderInclude(variant, includesNode);
                }
            }

            // Render images
            // Get images for current target and all include parts
            TreeSet images = new TreeSet(this.selectedIncludePart
                    .getImageDependencies(true));
            if (!images.isEmpty()) {
                Element imagesNode = resdoc.createSubNode(currentInclude,
                        "images");
                for (Iterator i = images.iterator(); i.hasNext();) {
                    Image image = (Image) i.next();
                    Element imageNode = resdoc.createSubNode(imagesNode,
                            "image");
                    imageNode.setAttribute("path", image.getPath());
                    imageNode.setAttribute("modtime", Long.toString(image
                            .getLastModTime()));
                }
            }

            // Render content of include part
            Node content = this.selectedIncludePart.getIncludePart()
                    .getContentXML();
            if (content != null) {
                Element contentNode = resdoc.createSubNode(currentInclude,
                        "content");
                contentNode.appendChild(contentNode.getOwnerDocument()
                        .importNode(content, true));
            }
        }
    }

    private void renderInclude(IncludePartThemeVariant variant, Element parent) {
        Document doc = parent.getOwnerDocument();
        Element includeNode = doc.createElement("include");
        parent.appendChild(includeNode);
        includeNode.setAttribute("part", variant.getIncludePart().getName());
        includeNode.setAttribute("path", variant.getIncludePart()
                .getIncludeFile().getPath());
        includeNode.setAttribute("theme", variant.getTheme().getName());

        try {
            TreeSet variants = new TreeSet(variant
                    .getIncludeDependencies(false));
            for (Iterator i = variants.iterator(); i.hasNext();) {
                IncludePartThemeVariant variant2 = (IncludePartThemeVariant) i
                        .next();
                this.renderInclude(variant2, includeNode);
            }
        } catch (EditorParsingException e) {
            // Omit dependencies for this part
        }
    }

    public void reset() throws Exception {
        this.selectedIncludePart = null;
    }

    public boolean selectIncludePart(String path, String part, String theme) {
        Project project = EditorResourceLocator.getProjectsResource(context)
                .getSelectedProject();
        if (project == null) {
            return false;
        }
        IncludePartThemeVariant variant = this.internalSelectIncludePart(
                project, path, part, theme);
        if (variant == null) {
            return false;
        } else {
            this.selectedIncludePart = variant;
            return true;
        }
    }

    public void unselectIncludePart() {
        this.selectedIncludePart = null;
    }

    public IncludePartThemeVariant getSelectedIncludePart() {
        return this.selectedIncludePart;
    }

    public int restoreBackup(String version, String hash) {
        if (this.selectedIncludePart == null) {
            return 1;
        }

        if (!this.selectedIncludePart.getMD5().equals(hash)) {
            return 2;
        }

        try {
            if (SpringBeanLocator.getBackupService().restoreInclude(
                    this.selectedIncludePart, version)) {
                return 0;
            } else {
                return 1;
            }
        } catch (EditorSecurityException e) {
            return 1;
        }
    }

    public String getMD5() {
        if (this.selectedIncludePart == null) {
            return "";
        }
        return this.selectedIncludePart.getMD5();
    }

    public String getContent() {
        if (this.selectedIncludePart == null) {
            return "";
        }
        Node xml = this.selectedIncludePart.getXML();
        if (xml == null) {
            return "<lang name=\"default\">\n  \n</lang>";
        }

        // Make sure xmlns declarations are present in top-level element
        if (xml instanceof Element) {
            Map xmlnsMappings = SpringBeanLocator.getConfigurationService()
                    .getPrefixToNamespaceMappings();
            Element elem = (Element) xml;
            for (Iterator i = xmlnsMappings.keySet().iterator(); i.hasNext();) {
                String prefix = (String) i.next();
                String url = (String) xmlnsMappings.get(prefix);
                elem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:"
                        + prefix, url);
            }
        }

        String serialized = Xml.serialize(xml, false, false);
        serialized = Xml.stripElement(serialized);
        StringBuffer output = new StringBuffer();

        for (int i = 0; i < serialized.length(); i++) {
            char c = serialized.charAt(i);
            output.append(this.unicodeTranslate(c));
        }

        // Remove empty lines
        StringBuffer lineBuffer = new StringBuffer();
        StringBuffer output2 = new StringBuffer();
        for (int i = 0; i < output.length(); i++) {
            if (output.charAt(i) == '\r') {
                if (output.charAt(i + 1) == '\n') {
                    i++;
                }
                if (lineBuffer.length() != 0) {
                    output2.append(lineBuffer);
                    output2.append('\n');
                }
                lineBuffer.delete(0, lineBuffer.length());
            } else if (output.charAt(i) == '\n') {
                if (lineBuffer.length() != 0) {
                    output2.append(lineBuffer);
                    output2.append('\n');
                }
                lineBuffer.delete(0, lineBuffer.length());
            } else {
                lineBuffer.append(output.charAt(i));
            }
        }

        // Get minimum number of leading spaces and remove the same
        // number of leading spaces in each line
        int spaceCount = -1;
        int temporaryCount = 0;
        for (int i = 0; i < output2.length(); i++) {
            char c = output2.charAt(i);
            if (c == ' ') {
                temporaryCount++;
            } else if (c == '\n') {
                if (spaceCount == -1) {
                    spaceCount = temporaryCount;
                } else if (temporaryCount < spaceCount) {
                    spaceCount = temporaryCount;
                }
                temporaryCount = 0;
            }
        }
        if (spaceCount > 0) {
            Pattern pattern = Pattern.compile("^ {" + spaceCount + "}",
                    Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(output2);
            return matcher.replaceAll("");
        } else {
            return output2.toString();
        }
    }

    private String unicodeTranslate(char c) {
        if (c < 128 || (c >= 0xc0 && c <= 0xcf) || (c >= 0xd1 && c <= 0xd6)
                || (c >= 0xd9 && c <= 0xdd) || (c >= 0xdf && c <= 0xef)
                || (c >= 0xf1 && c <= 0xf6) || (c >= 0xf9 & c <= 0xfd)
                || c == 0xff) {
            return Character.toString(c);
        } else {
            return "&#" + Integer.toString(c) + ";";
        }
    }

    public void setContent(String content, String hash) throws SAXException,
            EditorException {
        // Check whether hashcode has changed
        if (!this.selectedIncludePart.getMD5().equals(hash)) {
            String newHash = this.getMD5();
            String newContent = this.getContent();
            String merged = DiffUtil.merge(content, newContent);
            throw new EditorIncludeHasChangedException(merged, newHash);
        }

        StringBuffer xmlcode = new StringBuffer();
        xmlcode.append("<part");

        // Add predefined prefixes
        Map xmlnsMappings = SpringBeanLocator.getConfigurationService()
                .getPrefixToNamespaceMappings();
        for (Iterator i = xmlnsMappings.keySet().iterator(); i.hasNext();) {
            String prefix = (String) i.next();
            String url = (String) xmlnsMappings.get(prefix);
            xmlcode.append(" xmlns:" + prefix + "=\"" + url + "\"");
        }

        xmlcode.append(">\n");
        xmlcode.append(content);
        xmlcode.append("\n</part>");
        Document doc = Xml.parseStringMutable(xmlcode.toString());

        this.selectedIncludePart.setXML(doc.getDocumentElement());
    }

    public boolean createAndSelectBranch(String themeName) {
        Theme theme = null;
        for (Iterator i = this.getPossibleThemes(
                this.selectedIncludePart,
                EditorResourceLocator.getProjectsResource(context)
                        .getSelectedProject(),
                this.selectedIncludePart.getAffectedPages()).iterator(); i
                .hasNext();) {
            Theme theme2 = (Theme) i.next();
            if (theme2.getName().equals(themeName)) {
                theme = theme2;
            }
        }

        if (theme == null) {
            return false;
        }

        if (!this.securityMayCreateIncludePartThemeVariant(
                this.selectedIncludePart.getIncludePart(), theme)) {
            return false;
        }

        this.selectedIncludePart = this.selectedIncludePart.getIncludePart()
                .createThemeVariant(theme);

        return true;
    }

    public boolean deleteSelectedBranch() {
        try {
            this.selectedIncludePart.getIncludePart().deleteThemeVariant(
                    this.selectedIncludePart);
        } catch (EditorSecurityException e) {
            return false;
        } catch (EditorIOException e) {
            return false;
        } catch (EditorParsingException e) {
            return false;
        }
        this.selectedIncludePart = null;
        return true;
    }
}
