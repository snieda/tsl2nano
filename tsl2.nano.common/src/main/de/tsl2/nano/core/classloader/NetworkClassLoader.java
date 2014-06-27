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

import java.io.File;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.util.FileUtil;

/**
 * Loads unresolved classes from network-connection through a maven repository.
 * 
 * @author Tom
 * @version $Revision$
 */
public class NetworkClassLoader extends NestedJarClassLoader {
    static final List<String> unresolveables = new ArrayList<String>();
    
    static final String FILENAME_UNRESOLVEABLES = "network.classloader.unresolvables";
    
    /**
     * environment config path. as it is not possible to use the type Environment.class itself (import class is loaded
     * by AppLoader, but a new Environment was created), we use this variable instead.
     */
    String environment;

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

    @SuppressWarnings("unchecked")
    @Override
    public void addLibraryPath(String path) {
        super.addLibraryPath(path);

        //only for the first time we load the persisted ignore list
        if (environment == null || !environment.equals(path)) {
            File persistedList = new File(path + "/" + FILENAME_UNRESOLVEABLES);
            if (persistedList.canRead()) {
                unresolveables.addAll((Collection<? extends String>) FileUtil.load(persistedList.getPath()));
            }
        }
        environment = path;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            //try it again after loading it from network
            if (!unresolveables.contains(name)) {
                try {
                    if (Environment.loadDependencies(name) != null) {
                        //reload jar-files from environment
                        addLibraryPath(environment);
                    }
                    return super.findClass(name);
                } catch (Exception e2) {
                    unresolveables.add(name);
                    FileUtil.save(environment + "/" + FILENAME_UNRESOLVEABLES, unresolveables);
                    //throw the origin exception!
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }
}
