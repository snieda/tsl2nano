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
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * class to enable nesting jar-files into a jar-file. here it isn't possible to use utility classes, because they aren't
 * loaded yet. so we use simple outputs.
 * 
 * @author ts
 * @version $Revision$
 */
public class NestedJarClassLoader extends LibClassLoader implements Cloneable {
    private static final Log LOG = LogFactory.getLog(NestedJarClassLoader.class);

    /** hasRootJar, initial true to start evaluation! */
    boolean hasRootJar = true;

    Map<String, ZipStream> jarFileStreams;
    String[] nestedJars;

    public NestedJarClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public NestedJarClassLoader(URL[] urls) {
        super(urls);
    }

    /**
     * constructor
     * 
     * @param urls
     * @param parent
     * @param factory
     */
    public NestedJarClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    /**
     * constructor
     * 
     * @param urls
     * @param parent
     */
    public NestedJarClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
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

    protected String getRootJarPath() {
        String rootPath = System.getProperty("java.class.path");
        return rootPath.contains(";") ? null : rootPath;
    }

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
            }
        }
        return nestedJars;
    }

    private String[] getNestedJars(String rootPath) {
        return FileUtil.readFileNamesFromZip(rootPath, "*jar");
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
        return name.replace('.', '/') + ".class";
    }

    @Override
    public String toString() {
        return super.toString() + "[nested: " + (getNestedJars() != null ? nestedJars.length : 0) + "]";
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
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
        zipStream = new ZipInputStream(jarStream);
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