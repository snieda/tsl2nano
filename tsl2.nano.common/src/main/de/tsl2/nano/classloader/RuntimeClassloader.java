/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 14, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.classloader;

import java.io.File;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.exception.Message;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.StringUtil;

/**
 * provides dynamic classloading through extending classpath on runtime. use {@link #addURL(URL)} to enhance the
 * classpath.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class RuntimeClassloader extends URLClassLoader {
    private static final Log LOG = LogFactory.getLog(RuntimeClassloader.class);

    /**
     * constructor
     * 
     * @param urls
     */
    public RuntimeClassloader(URL[] urls) {
        super(urls);
    }

    /**
     * constructor
     * 
     * @param urls
     * @param parent
     */
    public RuntimeClassloader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * constructor
     * 
     * @param urls
     * @param parent
     * @param factory
     */
    public RuntimeClassloader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addURL(URL url) {
        LOG.info("adding '" + url + " to classpath");
        super.addURL(url);
    }

    /**
     * addFile. see {@link #addURL(URL)} - using localhost.
     * 
     * @param fileName file name
     */
    public void addFile(String fileName) {
        addURL(getFileURL(fileName));
    }

    /**
     * creates the desired url
     * 
     * @param fileName file name
     * @return url with file name
     */
    public static URL getFileURL(String fileName) {
        try {
//            return new URL("file", "localhost", fileName);
            return new File(fileName).toURI().toURL();
        } catch (final MalformedURLException e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * adds the given file-path to the classloader and evaluates all bean classes from given jar
     * 
     * @param beanjar bean jar file
     * @return bean types
     */
    @SuppressWarnings({ "rawtypes" })
    public List<Class> loadBeanClasses(String beanjar, StringBuilder messages) {
        if (beanjar == null) {
            return new LinkedList<Class>();
        }
        if (messages == null)
            messages = new StringBuilder();

        LOG.info("loading bean classes from: " + beanjar);
        addFile(beanjar);
        List<Class> beanClasses;
        String[] classNames;
        String p = null;
        classNames = FileUtil.readFileNamesFromZip(beanjar, "*.class");
        if (classNames == null) {
            throw new FormattedException("The given jar-file '" + beanjar + "' doesn't exist!");
        }

        beanClasses = new ArrayList<Class>(classNames.length);
        int loaderrors = 0;
        for (int i = 0; i < classNames.length; i++) {
            if (classNames[i].endsWith(".class")) {
                String className = StringUtil.substring(classNames[i], null, ".class");
                if (p != null) {
                    className = p + "." + className;
                }
                className = className.replace('/', '.');
                LOG.info("loading class: " + className);
                try {
                    Class<?> clazz = loadClass(className);
                    /*
                     * check, if serializable
                     */
                    if (!Serializable.class.isAssignableFrom(clazz)) {
                        LOG.info("ignoring not serializable class: " + clazz);
                        continue;
                    }
                    /*
                     * don't show enums. interfaces may have no superclass!
                     */
                    if (clazz.isEnum() || (clazz.getSuperclass() != null && clazz.getSuperclass().isEnum())) {
                        LOG.info("ignoring enum class: " + clazz);
                        continue;
                    }
                    /*
                     * check, if instances can be done and serialized!
                     */
                    BeanUtil.serializeBean(clazz.newInstance());
                    /*
                     * ok, add it
                     */
                    beanClasses.add(clazz);
                } catch (Throwable e) {
                    LOG.error(e.toString());
                    loaderrors++;
//                    ForwardedException.forward(e);
                }
            }
        }
        if (beanClasses.size() == 0) {
            LOG.error("No classes were load. Please select another jar-file with bean types!");
            //TODO remove URL runtimeClassloader.
            return beanClasses;
        } else if (loaderrors > 0) {
            messages.append("Loaded bean types: " + beanClasses.size()
                + " of "
                + classNames.length
                + " (errors: "
                + loaderrors
                + ", ignored: "
                + (classNames.length - (beanClasses.size() + loaderrors))
                + ") of '"
                + beanjar
                + "'\nHave a look at the log file to see the failed class loadings");
            LOG.warn(messages);
        }
        return beanClasses;
    }

    /**
     * startPathChecker NOT IMPLEMENTED YET!
     * 
     * @param path path to be checked for new jars to load on runtime.
     * @param waitMillis milliseconds to wait between checks.
     */
    public void startPathChecker(final String path, final long waitMillis) {
        Runnable pathChecker = new Runnable() {
            File fPath = new File(path);
            File[] lastFiles;

            @Override
            public void run() {
                LOG.info("running patch checker in thread " + Thread.currentThread());
                while (true) {
                    try {
                        Thread.sleep(waitMillis);

                        List<File> changedFiles = lastModifiedFile();
                        for (File file : changedFiles) {
                            // TODO: implement unloading existing classes
                            if (file.getPath().endsWith(".jar")) {
                                Message.send("New jar-file loaded: " + file.getAbsolutePath());
                                addFile(file.getAbsolutePath());
                            }
                        }
                    } catch (InterruptedException e) {
                        LOG.error(e);
//                        ForwardedException.forward(e);
                    }
                }
            }

            List<File> lastModifiedFile() {
                File[] files = fPath.listFiles();
                File last = null;
                List<File> fileList;
                if (lastFiles != null) {
                    fileList = new ArrayList<File>(Arrays.asList(files));
                    boolean changed = fileList.removeAll(Arrays.asList(lastFiles));
                    if (changed && fileList.size() > 0) {
                        lastFiles = files;
                        return fileList;
                    }
                }
                for (int i = 0; i < files.length; i++) {
                    if (last == null || files[i].lastModified() > last.lastModified())
                        last = files[i];
                }
                lastFiles = files;
                fileList = new ArrayList<File>();
                if (last != null && last.lastModified() > System.currentTimeMillis() - waitMillis)
                    fileList.add(last);
                return fileList;
            }
        };
        ThreadUtil.startDaemon("classloader-environment-path-checker", pathChecker);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[urls: " + getURLs().length + "]";
    }
}
