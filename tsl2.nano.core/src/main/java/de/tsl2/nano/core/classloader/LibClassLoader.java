/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 27, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.core.classloader;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.FileUtil;

/**
 * Extends the {@link RuntimeClassloader} to add all jars of a given path to the classpath (see
 * {@link #addLibraryPath(String)}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class LibClassLoader extends RuntimeClassloader {

    protected static final String EXT_LIBRARY = ".jar";

    public LibClassLoader(URL[] urls) {
        super(urls);
    }

    public LibClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public LibClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * addLibraryPath
     * 
     * @param path path added to the classpath adding all jars, too
     */
    public void addLibraryPath(String path) {
        final File fPath = FileUtil.getURIFile(path);
        if (!fPath.isDirectory()) {
            throw ManagedException.illegalArgument(path, "path must be a directory!");
        }

        File[] jarFiles = fPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(EXT_LIBRARY);
            }
        });

        final URL pathURL = getFileURL(path);
        if (!Arrays.asList(getURLs()).contains(pathURL)) {
            addFile(path);
        }

        jarFiles = sortByVersion(jarFiles);
        for (final File file : jarFiles) {
            addFile(file.getAbsolutePath());
        }
    }

    /**
     * to avoid classloading conflicts, do a reverse sorting by name - higher versions come first.
     * @param jarFiles list of jar files
     * @return sorted jars
     */
    private File[] sortByVersion(File[] jarFiles) {
        //IMPROVE: performance!
        Arrays.sort(jarFiles);
        List<File> list = Arrays.asList(jarFiles);
        Collections.reverse(list);
        return list.toArray(new File[0]);
    }
}
