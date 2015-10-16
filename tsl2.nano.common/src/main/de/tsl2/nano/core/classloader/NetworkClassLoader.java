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
import java.util.Collection;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Extends the {@link NestedJarClassLoader} to load unresolved classes from network-connection through a maven
 * repository. All classes that cannot be found through maven are cached in {@link #unresolveables}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class NetworkClassLoader extends NestedJarClassLoader {
    private static final Log LOG = LogFactory.getLog(NetworkClassLoader.class);

    /** persistent cache for classes that couldn't be loaded through network. */
    static final ListSet<String> unresolveables = new ListSet<String>();

    /** filename for persistent cache of {@link #unresolveables}. */
    static final String FILENAME_UNRESOLVEABLES = "network.classloader.unresolvables";

    /** standard exclude expresion */
    public static final String REGEX_EXCLUDE = "standalone";
    
    /**
     * environment config path. as it is not possible to use the type Environment.class itself (import class is loaded
     * by AppLoader, but a new Environment was created), we use this variable instead.
     */
    String environment;

    private transient int downloadedjars;

    /**
     * convenience to create an network-url-classloader on given directories and registering it to the current thread.
     * 
     * @param directories to search jar files from
     * @return new classloader, searching classes in the nested jar, on the given directories and on the network using
     *         maven...
     */
    public static NetworkClassLoader createAndRegister(String... directories) {
        NetworkClassLoader cl = new NetworkClassLoader(Thread.currentThread().getContextClassLoader());
        for (int i = 0; i < directories.length; i++) {
            cl.addLibraryPath(directories[i]);
        }
        Thread.currentThread().setContextClassLoader(cl);
        return cl;
    }

    /**
     * constructor
     * 
     * @param parent parent class loader
     */
    public NetworkClassLoader(ClassLoader parent) {
        this(parent, null);
    }
    
    /**
     * constructor
     * 
     * @param parent parent class loader
     * @param exclude regular expression for nested jars to be excluded from classpath. see {@link NestedJarClassLoader#NestedJarClassLoader(ClassLoader, String)}
     */
    public NetworkClassLoader(ClassLoader parent, String exclude) {
        super(parent, exclude);
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
                unresolveables.addAll(ListSet.load(persistedList.getPath()));
                LOG.info("unresolvable class-packages are:\n\t" + unresolveables);
            }
        }
        environment = path;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            //if a root classpath contains a classes directory, extract the class name
            if (name.startsWith(DEFAULT_BIN_DIR))
                name = StringUtil.substring(name, ".", null);
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            //try it again after loading it from network
            String pckName = BeanClass.getPackageName(name);
            if (!unresolveables.contains(pckName)) {
                try {
                    if (BeanClass.isPublicClassName(name) && ENV.loadDependencies(name) != null) {
                        //reload jar-files from environment
                        downloadedjars++;
                        addLibraryPath(environment);
                        return super.findClass(name);
                    }
                } catch (Exception e2) {
                    if (LOG.isDebugEnabled()) {
                        LOG.warn("couldn't load class " + name, e2);
                    } else {
                        LOG.warn("couldn't load class " + name);
                    }
                    unresolveables.add(pckName);
                    unresolveables.save(environment + "/" + FILENAME_UNRESOLVEABLES);
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

    @Override
    public String toString() {
        return super.toString() + "[downloaded-jars:" + downloadedjars + ", unresolved-classes:"
            + unresolveables.size() + "]";
    }
}
