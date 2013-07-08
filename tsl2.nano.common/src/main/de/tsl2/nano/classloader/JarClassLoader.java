/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 15, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.classloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.tsl2.nano.collection.SegmentList;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanClass;

/**
 * class to enable nesting jar-files into a jar-file. here it isn't possible to use utility classes, because they aren't
 * loaded yet. so we use simple outputs.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class JarClassLoader extends URLClassLoader {
    Map<String, byte[]> unresolvedClassDefintions;
    Map<String, byte[]> definitionErrors;

    public JarClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        unresolvedClassDefintions = new Hashtable<String, byte[]>();
        definitionErrors = new Hashtable<String, byte[]>();
        loadClassesFromNestedJars();
    }

    /**
     * loadClassesFromNestedJars
     * @return count of defined classes
     */
    private int loadClassesFromNestedJars() {
        long startTime = System.currentTimeMillis();
        String rootPath = getRootJarPath();
        log("current jar: " + rootPath);
        String[] nestedJars = getNestedJars(rootPath);
        if (nestedJars == null) {
            log("no nested jars available");
            return 0;
        }
        log("loading " + nestedJars.length + " nested jars into classpath");
        int i = 0;
        for (String jarName : nestedJars) {
            int ni = 0;
            log("loading classes of nesting jar " + jarName + "...");
            ZipInputStream zipStream = getJarInputStream(jarName);
//            InputStreamReader inputStreamReader = new InputStreamReader(zipStream);
            try {
                ZipEntry zipEntry = null;
                int size = 0;
                byte[] b;
                while ((zipEntry = zipStream.getNextEntry()) != null) {
                    SegmentList<byte[], Byte> byteBuffer = new SegmentList<byte[], Byte>(byte.class);
                    String name = zipEntry.getName();
                    if (name.endsWith("class")) {
                        size = (int) (zipEntry.getSize() == -1 ? 1 : zipEntry.getSize());
                        debug("loading class " + name + " with " + zipEntry.getSize() + " bytes");
                        while (size > 0) {
                            b = new byte[size];
                            size = zipStream.read(b, 0, size);
                            if (size > 0)
                                byteBuffer.add(b);
                        }
                        name = name.replace('/', '.');
                        name = StringUtil.substring(name, null, ".class");
                        byte[] bytes = byteBuffer.toSegmentArray();
                        debug(bytes.length + " bytes read, defining class " + name);
//                        debug(Arrays.toString(bytes));
                        if (defineClass(name, bytes))
                            ni++;
                    }
                }
                log(ni + " loaded");
            } catch (IOException e) {
                ForwardedException.forward(e);
            } finally {
                if (zipStream != null)
                    try {
                        zipStream.close();
                    } catch (IOException e) {
                        ForwardedException.forward(e);
                    }
            }
            i += ni;
        }
        /*
         * now, load classes again, that couldn't be loaded previously
         * do that many times, shuffling the order
         */
        int tries = 0;
        while (unresolvedClassDefintions.size() > 0 && tries++ < 10000) {
            log("trying to load " + unresolvedClassDefintions.size() + " classes again...");
            List<String> names = new ArrayList<String>(unresolvedClassDefintions.keySet());
            Collections.shuffle(names);
            for (String name : names) {
                byte[] bytes = unresolvedClassDefintions.remove(name);
                if (defineClass(name, bytes)) {
                    i++;
                }
            }
        }
        log(i + " classes were loaded");
        log(definitionErrors.size() + " classes had unresolved class errors!");
        log(definitionErrors.size() + " classes had linkage errors!");
        log("loading time: " + (int) ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
        unresolvedClassDefintions.clear();
        return i;
    }

    /**
     * getJarInputStream
     * @param jarName
     * @return
     */
    protected ZipInputStream getJarInputStream(String jarName) {
        InputStream jarStream = this.getResourceAsStream(jarName);
        ZipInputStream zipStream = new ZipInputStream(jarStream);
        return zipStream;
    }

    protected ZipInputStream getExternalJarInputStream(String jarName) {
        try {
            FileInputStream fis = new FileInputStream(new File(jarName));
            ZipInputStream zipStream = new ZipInputStream(fis);
            return zipStream;
        } catch (FileNotFoundException e) {
            new RuntimeException(e);
            return null;
        }
    }
    
    /**
     * defineClass
     * 
     * @param name
     * @param bytes
     * @throws ClassFormatError
     */
    boolean defineClass(String name, byte[] bytes) {
        try {
            Class<?> definedClass = defineClass(name, bytes, 0, bytes.length);
            resolveClass(definedClass);
            return true;
        } catch (NoClassDefFoundError error) {
            //no problem, we put it to a map to load it later
            debug(error.toString() + " ==> we define the class later again...");
            unresolvedClassDefintions.put(name, bytes);
        } catch (LinkageError error) {
            debug("fatal error on defining class: " + error.toString());
//            try {
//                String utf8 = new String(bytes, "UTF-8");
//                bytes = utf8.getBytes();
//                Class<?> definedClass = defineClass(name, bytes, 0, bytes.length);
//            } catch (UnsupportedEncodingException e) {
//                new RuntimeException(e);
//            }
            definitionErrors.put(name, bytes);
        }
        return false;
    }

    private static final void debug(String text) {
        System.out.println(text);
    }

    private static final void pre(String text) {
        System.out.print(text);
    }

    private static final void log(String text) {
        System.out.println(text);
    }

    protected String getRootJarPath() {
//        String root = "../swartifex.architect/packages/plugins/de.tsl2.nano.common_0.0.2.B.jar";
//        log(new File(root).getAbsolutePath());
//        return root;
        return System.getProperty("java.class.path");
    }

    private String[] getNestedJars(String rootPath) {
        return readFileNamesFromZip(rootPath, "*jar");
    }

    /**
     * Returns a file array containing all filenames inside the given jar/zip file.
     * 
     * @param zipfile
     * @param filter e.g. *.txt
     * @return
     */
    public static String[] readFileNamesFromZip(String zipfile, String filter) {
        filter = filter.replace("*", ".*");
        //open a zip-file
        ZipInputStream sourceStream = null;
        try {
            final File zip = new File(zipfile);
            if (!zip.exists()) {
                return null;
            }
            //open the source data file
            final FileInputStream fis = new FileInputStream(zip);
            sourceStream = new ZipInputStream(fis);
            //search sources
            final List files = new LinkedList();
            ZipEntry zipEntry = null;
            while ((zipEntry = sourceStream.getNextEntry()) != null) {
                if (zipEntry.getName().matches(filter)) {
                    files.add(zipEntry.getName());
                }
                sourceStream.closeEntry();
            }
            return (String[]) files.toArray(new String[0]);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (sourceStream != null) {
                try {
                    sourceStream.close();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected Attributes readManifest() {
        Attributes attributes = new Attributes();
        try {
            for (Enumeration<URL> manifests = getResources("META-INF/MANIFEST.MF"); manifests.hasMoreElements();) {
                URL manifestURL = manifests.nextElement();
                InputStream in = manifestURL.openStream();
                try {
                    Manifest manifest = new Manifest(in);

                    attributes.putAll(manifest.getMainAttributes());
//                String arguments = mainAttributes.getValue("Arguments");
//                if (arguments != null)
//                    log("Found arguments: " + arguments);
                } finally {
                    in.close();
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        log("manifest:\n" + StringUtil.toFormattedString(attributes, 80));
        return attributes;
    }

    /**
     * main
     * @param args
     */
    public static final void main(String[] args) {
        debug(System.getProperties().toString());
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        log("resetting current thread classloader " + contextClassLoader);
        JarClassLoader jarClassLoader = new JarClassLoader(contextClassLoader);
        Thread.currentThread().setContextClassLoader(jarClassLoader);
        //test it
        try {
            jarClassLoader.loadClass("org.apache.commons.logging.LogFactory");
        } catch (ClassNotFoundException e) {
            ForwardedException.forward(e);
        }
        
        jarClassLoader.readManifest();
        /*
         * The Manifest entry 'Main-Arguments' will be given as args.
         * We have to split this single argument to have start arguments
         */
        if (args.length > 0) {
            log("Main-Arguments: \n" + StringUtil.toFormattedString(args, 80));
            String[] splittedMainClass = args[0].split(" ");
            BeanClass bc = BeanClass.createBeanClass(splittedMainClass[0]);
            String[] nargs = new String[args.length - 1];
            System.arraycopy(args, 1, nargs, 0, nargs.length);
            bc.callMethod(null, "main", new Class[] { String[].class }, new Object[]{nargs});
        } else {
            log("no main arguments given --> nothing to do ...");
        }
    }
}
