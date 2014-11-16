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

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.XmlUtil;

/**
 * Loads unresolved classes from network-connection through a maven repository. All classes that cannot be found through
 * maven are cached in {@link #unresolveables}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class NetworkClassLoader extends NestedJarClassLoader {
    private static final Log LOG = LogFactory.getLog(NetworkClassLoader.class);

    /** persistent cache for classes that couldn't be loaded through network. */
    static final List<String> unresolveables = new ArrayList<String>();

    /** filename for persistent cache of {@link #unresolveables}. */
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
            if (Environment.isAvailable() && Environment.get("networkclassloader.reload.unresolved", false)
                && persistedList.canRead()) {
                unresolveables.addAll((Collection<? extends String>) Environment.load(FILENAME_UNRESOLVEABLES,
                    ArrayList.class));
                LOG.info("unresolvable class-packages are:\n\t" + unresolveables);
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
            String pckName = BeanClass.getPackageName(name);
            if (!unresolveables.contains(pckName)) {
                try {
                    if (BeanClass.isPublicClassName(name) && Environment.isAvailable()
                        && Environment.loadDependencies(name) != null) {
                        //reload jar-files from environment
                        addLibraryPath(environment);
                        return super.findClass(name);
                    }

                } catch (Exception e2) {
                    LOG.warn("couldn't load class " + name, e);
                    unresolveables.add(pckName);
                    Environment.persist(unresolveables);
                }
            }
            //throw the origin exception!
            throw e;
        }
    }

    /**
     * resetUnresolvedClasses
     * 
     * @return true, if file could be deleted
     */
    public static boolean resetUnresolvedClasses(String path) {
        unresolveables.clear();
        return new File(path + FILENAME_UNRESOLVEABLES).delete();
    }
}
