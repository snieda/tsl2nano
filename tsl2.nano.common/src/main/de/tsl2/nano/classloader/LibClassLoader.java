/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 27, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.classloader;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;

import de.tsl2.nano.exception.FormattedException;

/**
 * Is able to add all jars of a given path to the classpath (see {@link #addLibraryPath(String)}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class LibClassLoader extends RuntimeClassloader {

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
        final File fPath = new File(path);
        if (!fPath.isDirectory()) {
            throw FormattedException.implementationError("path must be a directory!", path);
        }

        final File[] jarFiles = fPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        final URL pathURL = getFileURL(path);
        if (!Arrays.asList(getURLs()).contains(pathURL)) {
            addFile(path);
        }

        for (final File file : jarFiles) {
            addFile(file.getAbsolutePath());
        }
    }
}
