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

package de.schlund.pfixxml.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Provides access to resources on the filesystem.
 * For most of the functionality, {@link de.schlund.pfixxml.config.GlobalConfig}
 * is required and has to be configured through {@link de.schlund.pfixxml.config.GlobalConfigurator}
 * before this class is used.
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class ResourceUtil {
    /**
     * Creates a resource object using the given uri.
     * At the moment only absolute URIs with the "file" or
     * "pfixroot" scheme are supported.
     * 
     * @param uri URI denoting the resource to access
     * @return Resource object for the given URI
     * @throws IllegalArgumentException if given URI
     *         uses a scheme that is not supported or is
     *         not absolute
     * @see #getFileResource(String)
     */
    public static FileResource getFileResource(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("")) {
            throw new IllegalArgumentException("Cannot handle URIs without a scheme");
        }
        
        String path = uri.getPath();
        if (path == null || path.equals("")) {
            throw new IllegalArgumentException("Cannot handle URIs without a path");
        }
        
        if (scheme.equals("pfixroot")) {
            return new DocrootResourceImpl(uri);
        } else if (scheme.equals("file")) {
            return new FileSystemResourceImpl(uri);
        } else {
            throw new IllegalArgumentException("Cannot handle URI with scheme '" + scheme + "'");
        }
    }
    
    /**
     * Returns a resource interpreting the supplied string as
     * an URI. Special characters in the URI have to be escaped
     * as the supplied string is directly passed to the constructor
     * {@link URI#URI(java.lang.String)}.
     * 
     * @param uri String specifying a full URI
     * @return Resource for the specified URI
     * @throws IllegalArgumentException if supplied does not
     *         represent a valid URI
     * @see #getFileResource(URI)
     */
    public static FileResource getFileResource(String uri) {
        try {
            return getFileResource(new URI(uri));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URI \"" + uri + "\" is not well-formed", e);
        }
    }
    
    /**
     * Returns a resource resolving the given name relative to the
     * given parent. The parent is always treated as a directory
     * even if its URI does not end with a "/"
     * 
     * @param parent directory to look for resource in
     * @param name filename or relative path
     * @return resource specified by the concatenation of the given parent
     *         and name
     * @throws IllegalArgumentException if name is not relative
     */
    public static FileResource getFileResource(FileResource parent, String name) {
        if (name.startsWith("/")) {
            throw new IllegalArgumentException("Relative name may not start with a '/'");
        }
        
        URI parent_uri = parent.toURI();
        String parent_path = parent_uri.getPath();
        if (!parent_path.endsWith("/")) {
            parent_path = parent_path + "/";
        }
        
        try {
            return getFileResource(new URI(parent_uri.getScheme(), parent_uri.getAuthority(), parent_path + name, parent_uri.getQuery(), parent_uri.getFragment()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns a resource interpreting the given path relative
     * to Pustefix's docroot.
     * 
     * @param path path of the resource relative to the docroot 
     * @return Resource from the docroot
     */
    public static DocrootResource getFileResourceFromDocroot(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        try {
            return (DocrootResource) getFileResource(new URI("pfixroot", null, path, null, null));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Path \"" + path + "\" is not well-formed", e);
        }
    }
    
    /**
     * Copies a resource's content to another resource.
     * 
     * @param source resource to read from
     * @param target resource to write to
     * @throws IOException if content cannot be copied
     */
    public static void copy(FileResource source, FileResource target) throws IOException {
        if (!source.isFile() || target.isDirectory()) {
            throw new FileNotFoundException("Either source is not existing or target or source is a directory");
        }
        InputStream in = source.getInputStream();
        OutputStream out = target.getOutputStream();
        int len = 0;
        byte[] buffer = new byte[512];
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
    }
}
