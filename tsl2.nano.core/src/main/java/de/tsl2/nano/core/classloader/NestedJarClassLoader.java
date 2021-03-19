/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 13.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.AppLoader;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Extends the {@link LibClassLoader} to load classes from nesting jar-files. tries to access its owning jar file (see
 * {@link #getRootJarPath()} to evaluate the jar-file names in the root of its owning jar. If this owning jar isn't
 * accessable, it reads META-INF/MANIFEST.MF/Class-Path attributes and tries to load all given jars inside this root
 * jar. see {@link #getManifestClassPath()}.
 * <p/>
 * WARN: here it isn't possible to use utility classes, because they aren't loaded yet. so we use simple outputs.
 * 
 * @author ts
 * @version $Revision$
 */
public class NestedJarClassLoader extends LibClassLoader implements Cloneable {
    /** class loading extension */
    protected static final String EXT_CLASS = ".class";

    private static final Log LOG = LogFactory.getLog(NestedJarClassLoader.class);

    /** hasRootJar, initial true to start evaluation! */
    boolean hasRootJar = true;

    /** class variable to enhance performance of classloading on nested jar files. */
    Map<String, ZipStream> jarFileStreams;
    /** all nestedJars of it's main jar */
    String[] nestedJars;
    /** regular expression to exclude nesting jars from classpath */
    String exclude;

    public NestedJarClassLoader(ClassLoader parent) {
        this(parent, null);
    }

    /**
     * constructor
     * 
     * @param parent parent class loader
     * @param exclude regular expression for nested jars to be excluded from classpath.
     */
    public NestedJarClassLoader(ClassLoader parent, String exclude) {
        super(new URL[0], parent);
        this.exclude = exclude;
    }

    public NestedJarClassLoader(URL[] urls) {
        super(urls);
    }

    public NestedJarClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public NestedJarClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

//    @Override
//    public URL findResource(final String name) {
//        URL res = super.findResource(name);
//
//        if (res == null) {
//            String nestedJarName = findNested(name, true);
//            if (nestedJarName != null) {
//                try {
//                    String url = "jar:" + getRootJarPath() + "!/" + nestedJarName + "!/" + name;
//                    LOG.debug("resolving resource " + url);
//                    return new URL(url);
//                } catch (MalformedURLException e) {
//                    LOG.error(e);
//                    ManagedException.forward(e);
//                }
//            }
//        }
//        return res;
//    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            return findClassInNestedJar(name);
        } catch (NoClassDefFoundError e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }

    Class<?> findClassInNestedJar(String name) throws ClassNotFoundException {
        byte[] bytes = findInNestedJar(name);
        // perhaps create package
        int i = name.lastIndexOf('.');
        if (i != -1) {
            String pkgname = name.substring(0, i);
            Package pkg = getPackage(pkgname);
            if (pkg == null) {
                URL pkgUrl = getResource(pkgname.replaceAll("\\.", "/").concat("/"));
                //TODO: load nested manifests
                Manifest manifest = null;//this.manifests.get(nestedJarPath);
                if (manifest != null) {
                    definePackage(pkgname, manifest, pkgUrl);
                } else {
                    definePackage(pkgname, null, null, null, null, null, null, pkgUrl);
                }
            }
        }

        return defineClass(name, bytes, 0, bytes.length);

    }

    byte[] findInNestedJar(String name) throws ClassNotFoundException {
        LOG.debug("loading " + name);
        long startTime = System.currentTimeMillis();
        String[] nestedJars = getNestedJars();
        if (nestedJars != null) {
            final String path = getFileName(name);
            for (int i = 0; i < nestedJars.length; i++) {
                try {
                    ZipStream zipStream = getJarInputStream(nestedJars[i]);
                    byte[] bytes = zipStream.getFile(path);
                    if (bytes != null) {
                        LOG.debug("loaded " + nestedJars[i]
                            + " -> "
                            + name
                            + " with "
                            + bytes.length
                            + " bytes in "
                            + (System.currentTimeMillis() - startTime)
                            + "msecs");
                        //for performance: put jar-file to top
                        shiftToTop(nestedJars, i);
                        return bytes;
                    }
                } catch (Throwable e) {
                    LOG.error(e);
                    ManagedException.forward(e);
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

    private void shiftToTop(Object[] arr, int i) {
        Object obj = arr[i];
        //system.arraycopy not possible
        for (int j = i; j > 0; j--) {
            arr[j] = arr[j - 1];
        }
        arr[0] = obj;
    }

//    String findNested(String name, boolean resource) {
//        LOG.debug("searching for " + name);
//        String[] nestedJars = getNestedJars();
//        if (nestedJars != null) {
//            final String path = resource ? name : getFileName(name);
//            for (int i = 0; i < nestedJars.length; i++) {
//                ZipInputStream zipStream = getJarInputStream(nestedJars[i]);
//                String[] result = FileUtil.readFileNamesFromZip(zipStream, path, true);
//                if (result != null && result.length > 0)
//                    return result[0];
//            }
//        }
//        return null;
//    }

    /**
     * evaluates the root jar file name through the system property 'java.class.path'.
     * 
     * @return root/owing jar file name
     */
    protected String getRootJarPath() {
        String rootPath = System.getProperty("java.class.path");
        return rootPath.contains(";") ? null : rootPath;
    }

    /**
     * evaluates all nested jar files of the owning jar.
     * 
     * @return jar file names
     */
    public String[] getNestedJars() {
        if (hasRootJar && nestedJars == null) {
            String rootPath = getRootJarPath();
            if (rootPath != null && new File(rootPath).isFile()) {
//                hasRootJar = true;
                nestedJars = getNestedJars(rootPath);
                LOG.info("current jar: " + rootPath
                    + "\nnesting jars:\n"
                    + StringUtil.toFormattedString(nestedJars, -1, true));
                if (LOG.isDebugEnabled()) {
                    readManifest(this);
                }
                this.jarFileStreams = new HashMap<String, ZipStream>(nestedJars.length);
//            } else {
//                hasRootJar = false;
//                LOG.info("application not launched through jar-file. No nested jar files to load.");
            } else {//try to load the jars through Class-Path attribute in manfest.mf
                nestedJars = getManifestClassPath();
            }
        }
        return nestedJars;
    }

    /**
     * reads META-INF/MANIFEST.MF/Class-Path attributes and tries to load all given jars inside this root jar.
     * getManifestClassPath
     */
    protected String[] getManifestClassPath() {
        Attributes attributes = readManifest(this);
        String classPath = attributes.getValue("Class-Path");
        if (classPath != null) {
            LOG.info("reading nested jars through META-INF/MANIFEST.MF/Class-Path:\n\t" + classPath);
            String[] jars = classPath.split("\\s");
            //check, if jar as nested available
            this.jarFileStreams = new HashMap<String, ZipStream>(jars.length);
            List<String> nestedJars = new ArrayList<String>(jars.length);
            for (int i = 0; i < jars.length; i++) {
                if (getResource(jars[i]) != null)
                    nestedJars.add(jars[i]);
                else
                    LOG.warn(jars[i]
                        + " couldn't be loaded as nested content of this root jar!");
            }
            LOG.info(StringUtil.toFormattedString(nestedJars, -1, true));
            return nestedJars.toArray(new String[0]);
        }
        return null;
    }

    /**
     * reads all jar file names in the root directory of the owning jar.
     * 
     * @param rootPath
     * @return jar file names
     */
    private String[] getNestedJars(String rootPath) {
        return FileUtil.readFileNamesFromZip(rootPath, (exclude != null ? "(?!" + exclude + ")" : "") + "*"
            + EXT_LIBRARY.substring(1));
    }

    /**
     * getJarInputStream
     * 
     * @param jarName
     * @return
     */
    protected ZipStream getJarInputStream(String jarName) {
        ZipStream zipStream = jarFileStreams.get(jarName);
        if (zipStream == null) {
            System.out.println("loading nested jar:" + jarName + " ...");
            zipStream = new ZipStream(this, jarName);
            jarFileStreams.put(jarName, zipStream);
        }
        return zipStream;
    }

    /**
     * getClassName
     * 
     * @param name file name
     * @return class name
     */
    private String getFileName(String name) {
        return name.replace('.', '/') + EXT_CLASS;
    }

    @Override
    public String toString() {
        return super.toString() + "[nested: " + (getNestedJars() != null ? nestedJars.length : 0) + "]";
    }

    public void reset() {
    	if (!AppLoader.isJdkOracle())
    		new PrivateAccessor<>(this).member("classes", Collection.class).clear();
	}
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    
    @Override
    public void close() throws IOException {
    	super.close();
    	nestedJars = null;
    	Collection<ZipStream> streams = jarFileStreams.values();
    	for (ZipStream s : streams) {
    		if (s.zipStream != null)
    			s.zipStream.close();
		}
    }
}

/**
 * encapsulated caching zip-reader to enhance performance on preloading file bytes
 * 
 * @author Tom
 * @version $Revision$
 */
class ZipStream {
    ZipInputStream zipStream;
    Map<String, byte[]> zipEntryBytes;

    ZipStream(ClassLoader cl, String jarName) {
        super();
        zipEntryBytes = new HashMap<String, byte[]>();
        InputStream jarStream = cl.getResourceAsStream(jarName);
        if (jarStream != null)
        	zipStream = new ZipInputStream(jarStream);
        else
        	throw new IllegalStateException("resource '" + jarName + "' not found through classloader " + cl);
    }

    /**
     * @return Returns the zipStream.
     */
    ZipInputStream getZipStream() {
        return zipStream;
    }

    byte[] getFile(String fileName) {
        byte[] entryBytes = zipEntryBytes.get(fileName);
        if (entryBytes == null) {
            if (zipStream == null) {
                return null;
            } else {
                return readFromZip(fileName);
            }
        }
        zipEntryBytes.remove(entryBytes);
        return entryBytes;
    }

    //as we don't close the streams, we can't use FileUtil
    public byte[] readFromZip(String file) {
        //open a zip-file
        ZipEntry zipEntry = null;
        try {
            //search source 
            while ((zipEntry = zipStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals(file)) {
                    break;
                } else {
                    zipEntryBytes.put(zipEntry.getName(), FileUtil.readBytes(zipStream));
                }
            }
            if (zipEntry == null) {
                if (zipStream != null) {
                    try {
                        zipStream.close();
                        zipStream = null;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }

            //read source
            return FileUtil.readBytes(zipStream);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}