/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 01.06.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.core.classloader;

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.LinkedList;
import java.util.List;

import de.tsl2.nano.core.Environment;

/**
 * Loads unresolved classes from network-connection through a maven repository.
 * 
 * @author Tom
 * @version $Revision$
 */
public class NetworkClassLoader extends NestedJarClassLoader {
    static final List<String> unresolveables = new LinkedList<String>();

    /**
     * constructor
     * 
     * @param parent
     */
    public NetworkClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * constructor
     * 
     * @param urls
     */
    public NetworkClassLoader(URL[] urls) {
        super(urls);
    }

    /**
     * constructor
     * 
     * @param urls
     * @param parent
     * @param factory
     */
    public NetworkClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    /**
     * constructor
     * 
     * @param urls
     * @param parent
     */
    public NetworkClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            //try it again after loading it from network
            if (!unresolveables.contains(name)) {
                try {
                    Environment.loadDependencies(name);
                    return super.findClass(name);
                } catch (Exception e2) {
                    unresolveables.add(name);
                    //throw the origin exception!
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }
}
