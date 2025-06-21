/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 26, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.core.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import de.tsl2.nano.core.ITransformer;

/**
 * It is a {@link RuntimeClassloader}, manipulating search parameter for a new class or resource.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class TransformingClassLoader extends NetworkClassLoader {

    protected ITransformer<String, String> transformer;
    
    /**
     * constructor
     * 
     * @param urls
     */
    public TransformingClassLoader(URL[] urls) {
        super(urls);
    }

    /**
     * constructor
     * 
     * @param urls
     * @param parent
     */
    public TransformingClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    
    /**
     * @param transformer The transformer to set.
     */
    public void setTransformer(ITransformer<String, String> transformer) {
        this.transformer = transformer;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(transformer.transform(name));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return super.getResources(transformer.transform(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getResource(String name) {
        return super.getResource(transformer.transform(name));
    }

}
