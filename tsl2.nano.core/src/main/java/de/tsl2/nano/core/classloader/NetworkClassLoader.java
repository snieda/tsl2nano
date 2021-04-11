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
import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

import org.apache.commons.logging.Log;

//import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

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
     * environment config path. as it is not possible to use the type ENV.class itself (import class is loaded
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

    protected NetworkClassLoader() {
        this((ClassLoader)null);
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

    public NetworkClassLoader(URL[] urls) {
        super(urls);
    }

    public NetworkClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public NetworkClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	@Override
    public void addLibraryPath(String path) {
        super.addLibraryPath(path);

        //only for the first time we load the persisted ignore list
        if (unresolveables.size() == 0) {
            path = path != null ? path : ".";
            File persistedList = new File(path + "/" + FILENAME_UNRESOLVEABLES);
            if (persistedList.canRead()) {
                unresolveables.addAll(ListSet.load(persistedList.getPath()));
                LOG.info("unresolvable class-packages are:\n\t" + unresolveables);
            }
        }
    	if (environment == null)
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
            if (!pckName.startsWith(Util.FRAMEWORK_PACKAGE + ".core") && !unresolveables.contains(pckName)) {
                try {
                    if (BeanClass.isPublicClassName(name) && loadDependencies(name) != null) {
                        //reload jar-files from environment
                        downloadedjars++;
                        addLibraryPath(environment);
                        return super.findClass(name);
                    } else {
                    	addToUnresolvables(name, pckName, e);
                    }
                } catch (Exception e2) {
                    addToUnresolvables(name, pckName, e2);
                }
            }
            //throw the origin exception!
            throw e;
        }
    }

	private void addToUnresolvables(String name, String pckName, Exception e2) {
		if (LOG.isDebugEnabled()) {
		    LOG.warn("couldn't load class " + name, e2);
		} else {
		    LOG.warn("couldn't load class " + name);
		}
		unresolveables.add(pckName);
		if (environment == null)
		    environment = "."; 
		unresolveables.save(environment + "/" + FILENAME_UNRESOLVEABLES);
	}

    //TODO: check refactoring to move loadDependencies from ENV to this class
    protected Object loadDependencies(String name) {
        //we don't want to have a direct dependency to the environment class!
        return BeanClass.createBeanClass("de.tsl2.nano.core.ENV").callMethod(null,
            "loadDependencies",
            new Class[] { String[].class },
            new Object[] {new String[]{name}});
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
    public void close() throws IOException {
    	super.close();
    	unresolveables.clear();
    }
    
//    @Override
//    public Object clone() throws CloneNotSupportedException {
//    	NetworkClassLoader clone = new NetworkClassLoader(getURLs(), getParent());
//    	//for performance issue, we copy the loaded classes to the new one
//    	//TODO: that's not the right way...
//    	new PrivateAccessor<>(clone).member("classes", Collection.class).addAll(new PrivateAccessor<>(this).member("classes", Collection.class));
//    	return clone;
//    }
    @Override
    public String toString() {
        return super.toString() + "[downloaded-jars:" + downloadedjars + ", unresolved-classes:"
            + unresolveables.size() + "]";
    }
}
