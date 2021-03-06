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
package org.pustefixframework.live;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Provides information about Pustefix live resources. Also handles read and write operations of live.xml.
 */
public class LiveJarInfo {

    private static Logger LOG = Logger.getLogger(LiveJarInfo.class);
    
    public static final String[] DEFAULT_DOCROOT_LIVE_EXCLUSIONS = { 
            File.separator + "WEB-INF" + File.separator + "web.xml",
            File.separator + ".cache", 
            File.separator + "wsscript",
            File.separator + "wsdl", 
            File.separator + ".editorbackup", 
            File.separator + "htdocs" + File.separator + "sitemap.xml"};

    public static String PROP_LIVEROOT = "pustefix.liveroot";
    public static String PROP_LIVEROOT_MAXDEPTH = "pustefix.liveroot.maxdepth";
    public static String PROP_LIVE_FORCE_VERSION = "pustefix.live.forceversion";
    
    private static int DEFAULT_LIVEROOT_MAXDEPTH = 4;
    
    /** The live.xml file */
    private File file;

    private File liveRootDir;
    private int liveRootMaxDepth = DEFAULT_LIVEROOT_MAXDEPTH;
    
    private long lastReadTimestamp;
    
    private boolean forceVersion = true;

    /** The jar entries */
    private Map<String, Entry> jarEntries;
    private Map<String, Entry> jarEntriesNoVersion;

    /** The war entry */
    private Map<String, Entry> warEntries;
    private Map<String, Entry> warEntriesNoVersion;

    private Map<String, File> rootToLocation;
    private Set<String> rootsWithNoLocation;

    /**
     * Creates a new instance of LiveJarInfo, tries to detect the live.xml and parses that live.xml.
     */
    public LiveJarInfo() {

        String forceVersionProp = System.getProperty(PROP_LIVE_FORCE_VERSION);
        if(forceVersionProp != null) {
            forceVersion = Boolean.parseBoolean(forceVersionProp);
        }
        
        String liveRoot = System.getProperty(PROP_LIVEROOT);
        if(liveRoot != null) {
            
            liveRootDir = new File(liveRoot);
            try {
                liveRootDir = liveRootDir.getCanonicalFile();
            } catch (IOException e) {
                throw new RuntimeException("Can't read liveroot dir '" + liveRootDir.getPath() + "'.");
            }
            if(!liveRootDir.exists()) throw new RuntimeException("Liveroot dir '" + liveRootDir.getPath() + "' doesn't exist.");
            
            String value = System.getProperty(PROP_LIVEROOT_MAXDEPTH);
            if(value != null) {
                liveRootMaxDepth = Integer.parseInt(value);
            }
            
            LOG.info("Using maven projects found in '" + liveRootDir.getPath() + "' to look up live resources."); 
            
        } else {
        
            // search live.xml in workspace
            URL classpathUrl = getClass().getResource("/");
            if (classpathUrl != null) {
                String classpathDir = classpathUrl.getFile();
                if (classpathDir != null && classpathDir.length() > 0) {
                    File dir = new File(classpathDir);
                    do {
                        File test = new File(dir, "live.xml");
                        if (test.exists()) {
                            file = test;
                            LOG.info("Detected live.xml: " + file);
                            break;
                        }
                        dir = dir.getParentFile();
                    } while (dir != null);
                }
            }
    
            // fallback: use live.xml/life.xml from old location in ~/.m2
            if (file == null) {
                String homeDir = System.getProperty("user.home");
                File oldFile = new File(homeDir + File.separator + ".m2" + File.separator + "live.xml");
                if (!oldFile.exists()) {
                    oldFile = new File(homeDir + File.separator + ".m2" + File.separator + "life.xml"); // support old misspelled name
                }
                if (oldFile.exists()) {
                    file = oldFile;
                    LOG.warn("Using live.xml from old location: " + file);
                }
            }
            if (file == null) {
                LOG.info("No live.xml detected, default settings for live resources may be used!");
            }
    
        }
        
        init();
    }

    public LiveJarInfo(File file) {
        this.file = file;
        init();
    }

    private void init() {
        jarEntries = new HashMap<String, Entry>();
        jarEntriesNoVersion = new HashMap<String, Entry>();
        warEntries = new HashMap<String, Entry>();
        warEntriesNoVersion = new HashMap<String, Entry>();
        rootToLocation = new HashMap<String, File>();
        rootsWithNoLocation = new HashSet<String>();

        if (file != null) {
            try {
                read();
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        } else if(liveRootDir != null) {
            long t1 = System.currentTimeMillis();
            findMavenProjects(liveRootDir, 0, liveRootMaxDepth);
            long t2 = System.currentTimeMillis();
            LOG.info("Finding Maven projects took " + (t2-t1) + "ms.");
        }
        
        LOG.info(toString());
    }

    private void read() throws Exception {
        if (file.exists()) {
            lastReadTimestamp = file.lastModified();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            Element root = document.getDocumentElement();
            if (root.getLocalName().equals("live") || root.getLocalName().equals("life")) { // support old misspelled
                // name
                for (Element jarElem : LiveUtils.getChildElements(root, "jar")) {
                    Entry entry = readEntry(jarElem);
                    jarEntries.put(entry.getId(), entry);
                    jarEntriesNoVersion.put(entry.getGroupId() + "+" + entry.getArtifactId(), entry);
                }
                for (Element warElem : LiveUtils.getChildElements(root, "war")) {
                    Entry entry = readEntry(warElem);
                    warEntries.put(entry.getId(), entry);
                    warEntriesNoVersion.put(entry.getGroupId() + "+" + entry.getArtifactId(), entry);
                }
            }
        } else {
            LOG.info("Live jar configuration " + file + " not found.");
        }
    }

    private Entry readEntry(Element jarElem) throws Exception {
        Entry entry = new Entry();
        Element idElem = LiveUtils.getSingleChildElement(jarElem, "id", true);
        Element groupElem = LiveUtils.getSingleChildElement(idElem, "group", true);
        entry.groupId = groupElem.getTextContent().trim();
        Element artifactElem = LiveUtils.getSingleChildElement(idElem, "artifact", true);
        entry.artifactId = artifactElem.getTextContent().trim();
        Element versionElem = LiveUtils.getSingleChildElement(idElem, "version", true);
        entry.version = versionElem.getTextContent().trim();
        List<Element> dirElems = LiveUtils.getChildElements(jarElem, "directory");
        if (dirElems.size() == 0)
            dirElems = LiveUtils.getChildElements(jarElem, "directorie"); // support old misspelled name
        for (Element dirElem : dirElems) {
            File dir = new File(dirElem.getTextContent().trim());
            entry.directories.add(dir);
        }
        return entry;
    }

    public void checkFileModified() {
        if (file != null && file.exists()) {
            if (file.lastModified() > lastReadTimestamp) {
                init();
            }
        }
    }

    /**
     * Writes the live information to the live.xml file.
     * @throws Exception
     *             the exception
     */
    public void write() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        Element live = document.createElement("live");
        document.appendChild(live);

        for (Entry entry : jarEntries.values()) {
            Node jar = live.appendChild(document.createElement("jar"));
            writeEntry(document, jar, entry);
        }
        for (Entry entry : warEntries.values()) {
            Node war = live.appendChild(document.createElement("war"));
            writeEntry(document, war, entry);
        }

        // Write the DOM document to the file
        Source source = new DOMSource(document);
        Result result = new StreamResult(file);
        Transformer serializer = TransformerFactory.newInstance().newTransformer();
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        serializer.transform(source, result);
    }

    private void writeEntry(Document document, Node jar, Entry entry) {
        Node id = jar.appendChild(document.createElement("id"));
        Node group = id.appendChild(document.createElement("group"));
        group.setTextContent(entry.groupId);
        Node artifact = id.appendChild(document.createElement("artifact"));
        artifact.setTextContent(entry.artifactId);
        Node version = id.appendChild(document.createElement("version"));
        version.setTextContent(entry.version);
        for (File directory : entry.directories) {
            Node directoryNode = jar.appendChild(document.createElement("directory"));
            directoryNode.setTextContent(directory.getAbsolutePath());
        }
    }

    public File getLiveFile() {
        return file;
    }

    public Map<String, Entry> getJarEntries() {
        checkFileModified();
        return jarEntries;
    }

    public boolean hasJarEntries() {
        checkFileModified();
        return jarEntries != null && jarEntries.size() > 0;
    }

    public Map<String, Entry> getWarEntries() {
        checkFileModified();
        return warEntries;
    }

    public boolean hasWarEntries() {
        checkFileModified();
        return warEntries != null && warEntries.size() > 0;
    }
    
    public static boolean isDefaultDocrootLiveExclusion(String path) {
        for (String s : DEFAULT_DOCROOT_LIVE_EXCLUSIONS) {
            if(path.startsWith(s) && 
                    (path.equals(s) || path.substring(s.length()).startsWith(File.separator))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the live docroot.
     * @param docroot
     *            the original docroot
     * @param path
     *            the path of the resource, relative to docroot, used to determine includes/excludes
     * @return the live location for the docroot resource, or null if no live location is available
     * @throws Exception
     */
    public File getLiveDocroot(String docroot, String path) throws Exception {
        checkFileModified();

        // TODO: excludes and includes, depending on path, per directory
        if(isDefaultDocrootLiveExclusion(path)) return null;

        File location = rootToLocation.get(docroot);
        if (location != null || rootsWithNoLocation.contains(docroot)) {
            return location;
        }

        // find pom.xml, retrieve groupId, artifactId, version from pom.xml
        File pomFile = LiveUtils.guessPom(docroot);
        if (pomFile != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found pom.xml: " + pomFile);
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(pomFile);
            Element root = document.getDocumentElement();
            Element groupElem = LiveUtils.getSingleChildElement(root, "groupId", true);
            String groupId = groupElem.getTextContent().trim();
            Element artifactElem = LiveUtils.getSingleChildElement(root, "artifactId", true);
            String artifactId = artifactElem.getTextContent().trim();
            Element versionElem = LiveUtils.getSingleChildElement(root, "version", true);
            String version = versionElem.getTextContent().trim();
            String entryKey = groupId + "+" + artifactId + "+" + version;

            Entry warEntry = warEntries.get(entryKey);
            if (warEntry == null && !forceVersion) warEntry = warEntriesNoVersion.get(groupId + "+" + artifactId);
            if (warEntry != null) {
                for (File dir : warEntry.directories) {
                    // if (dir.getName().equals("webapp")) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found live docroot location by pom.xml: " + entryKey);
                    }
                    rootToLocation.put(docroot.toString(), dir);
                    return dir;
                    // }
                }   
            }
        }

        rootsWithNoLocation.add(docroot);
        return null;
    }

    /**
     * Gets the live module root.
     * @param url
     *            the URL to the module JAR or file target URL
     * @param path
     *            the resource path, relative to the URL
     * @return the live location for the module resource, or null if no live location is available
     */
    public File getLiveModuleRoot(URL url, String path) {
        checkFileModified();

        // TODO: excludes and includes, depending on path, per directory

        File location = rootToLocation.get(url.toString());
        if (location != null || rootsWithNoLocation.contains(url.toString())) {
            return location;
        }

        if (url.getProtocol().equals("jar")) {
            String jarFileName = url.getPath();
            int ind = jarFileName.indexOf('!');
            jarFileName = jarFileName.substring(0, ind);
            ind = jarFileName.lastIndexOf('/');
            jarFileName = jarFileName.substring(ind + 1);
            ind = jarFileName.lastIndexOf('.');
            jarFileName = jarFileName.substring(0, ind);
            // look for entry by jar file name
            Entry entry = jarEntries.get(jarFileName);
            if (entry != null) {
                for (File dir : entry.directories) {
                    if (dir.getName().equals("resources")) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Found live module location by jar file name: " + jarFileName);
                        }
                        rootToLocation.put(url.toString(), dir);
                        return dir;
                    }
                }
            }
            // look for entry by artifact name and MANIFEST attributes
            try {
                URL manifestUrl = new URL(url, "/META-INF/MANIFEST.MF");
                URLConnection con = manifestUrl.openConnection();
                if (con != null) {
                    InputStream in = con.getInputStream();
                    if (in != null) {
                        Manifest manifest = new Manifest(in);
                        Attributes attrs = manifest.getMainAttributes();
                        String groupId = attrs.getValue("Implementation-Vendor-Id");
                        String version = attrs.getValue("Implementation-Version");
                        if (groupId != null && version != null) {
                            int endInd = jarFileName.indexOf(version);
                            if (endInd > 2) {
                                String artifactId = jarFileName.substring(0, endInd - 1);
                                String entryKey = groupId + "+" + artifactId + "+" + version;
                                entry = jarEntries.get(entryKey);
                                if(entry == null && !forceVersion) entry = jarEntriesNoVersion.get(groupId + "+" + artifactId);
                                if (entry != null) {
                                    for (File dir : entry.directories) {
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("Found live module location by artifact name and MANIFEST attributes: "
                                                    + entryKey);
                                        }
                                        rootToLocation.put(url.toString(), dir);
                                        return dir;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (FileNotFoundException x) {
                LOG.warn("Module contains no MANIFEST.MF: " + url.toString());
            } catch (MalformedURLException x) {
                LOG.warn("Illegal module URL: " + url.toString(), x);
            } catch (IOException x) {
                LOG.warn("IO error reading module data: " + url.toString(), x);
            }
        } else if (url.getProtocol().equals("file")) {
            // find pom.xml, retrieve groupId, artifactId, version from pom.xml
            try {
                File pomFile = LiveUtils.guessPom(url.getFile());
                if (pomFile != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found pom.xml: " + pomFile);
                    }
                    String entryKey = LiveUtils.getKeyFromPom(pomFile);
                    Entry jarEntry = jarEntries.get(entryKey);
                    if(jarEntry == null && !forceVersion) jarEntry = jarEntriesNoVersion.get(entryKey.substring(0, entryKey.lastIndexOf('+')));
                    if (jarEntry != null) {
                        for (File dir : jarEntry.directories) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found live module location by pom.xml: " + entryKey);
                            }
                            rootToLocation.put(url.toString(), dir);
                            return dir;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Exceptions reading module live POM: " + url.toString(), e);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found no live location: " + url.toString());
        }
        rootsWithNoLocation.add(url.toString());
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Live jar information - ");
        int noJar = (jarEntries == null ? 0 : jarEntries.size());
        sb.append("Detected " + noJar + " live jar entr" + (noJar == 1 ? "y" : "ies"));
        int noWar = (warEntries == null ? 0 : warEntries.size());
        sb.append(" and " + noWar + " live war entr" + (noJar == 1 ? "y" : "ies"));
        return sb.toString();
    }
    
    private void findMavenProjects(File dir, int level, int maxDepth) {
        File[] files = dir.listFiles();
        for(File file: files) {
            if(file.isDirectory() && !file.isHidden() && !file.getName().equals("CVS") 
                    && !file.getName().equals("target") && !file.getName().equals("src")) {
                if(level < maxDepth) findMavenProjects(file, level + 1, maxDepth);
            } else if(file.isFile() && file.getName().equals("pom.xml") && file.canRead()) {
                Entry entry = null;
                try {
                    entry = LiveUtils.getEntryFromPom(file);
                } catch (Exception e) {
                    LOG.warn("Error reading POM: " + file.getAbsolutePath(), e);
                }
                if(entry != null) {
                    File resDir = new File(file.getParentFile(), "src/main/resources");
                    if(resDir.exists()) {
                        List<File> resDirs = new ArrayList<File>();
                        resDirs.add(resDir);
                        entry.setDirectories(resDirs);
                        jarEntries.put(entry.getId(), entry);
                        jarEntriesNoVersion.put(entry.getGroupId() + "+" + entry.getArtifactId(), entry);
                    } else {
                        resDir = new File(file.getParentFile(), "src/main/webapp");
                        if(resDir.exists()) {
                            List<File> resDirs = new ArrayList<File>();
                            resDirs.add(resDir);
                            entry.setDirectories(resDirs);
                            warEntries.put(entry.getId(), entry);
                            warEntriesNoVersion.put(entry.getGroupId() + "+" + entry.getArtifactId(), entry);
                        }
                    }
                }
            }
        }
    }
    

    public static class Entry {

        private String groupId;
        private String artifactId;
        private String version;
        private List<File> directories = new ArrayList<File>();

        public String getId() {
            return groupId + "+" + artifactId + "+" + version;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public List<File> getDirectories() {
            return directories;
        }

        public void setDirectories(List<File> directories) {
            this.directories = directories;
        }

    }
    
}
