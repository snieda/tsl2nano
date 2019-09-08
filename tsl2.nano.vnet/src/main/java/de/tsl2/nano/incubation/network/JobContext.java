/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 29.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.network;

import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class JobContext<V> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 8480081516558257792L;
    
    String name;
    Callable<V> callable;
    ClassLoader classLoader;
    URL[] urlsForClassLoading;
    Properties properties;
    public JobContext(String name, Callable<V> callable, Properties properties, URL...urlsForClassLoading) {
        this(name, callable, null, properties);
        this.urlsForClassLoading = urlsForClassLoading;
    }
    public JobContext(String name, Callable<V> callable, ClassLoader classLoader, Properties properties) {
        super();
        this.name = name;
        this.callable = callable;
        this.classLoader = classLoader;
        this.properties = properties;
    }
    /**
     * @return Returns the callable.
     */
    public Callable<V> getCallable() {
        return callable;
    }
    /**
     * @return Returns the classLoader.
     */
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = new URLClassLoader(urlsForClassLoading, Thread.currentThread().getContextClassLoader());
        }
        return classLoader;
    }
    /**
     * @return Returns the properties.
     */
    public Properties getProperties() {
        return properties;
    }
    
    @Override
    public String toString() {
        return name + ": " + callable + ", " + classLoader + ", " + properties;
    }
}
