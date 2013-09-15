/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Apr 26, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.persistence;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import de.tsl2.nano.log.LogFactory;

import de.tsl2.nano.classloader.LibClassLoader;
import de.tsl2.nano.classloader.RuntimeClassloader;
import de.tsl2.nano.classloader.TransformingClassLoader;
import de.tsl2.nano.collection.ITransformer;

/**
 * It is a {@link RuntimeClassloader}, manipulating the found persistence.xml
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class PersistenceClassLoader extends TransformingClassLoader {
    private static final Log LOG = LogFactory.getLog(PersistenceClassLoader.class);

    /**
     * constructor
     * 
     * @param urls
     */
    public PersistenceClassLoader(URL[] urls) {
        super(urls);
        setTransformer(getPersistenceTransform());
    }

    /**
     * constructor
     * 
     * @param urls
     * @param parent
     */
    public PersistenceClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        setTransformer(getPersistenceTransform());
    }

    /**
     * transforms the standard persistence file path {@link Persistence#FILE_PERSISTENCE_XML} to
     * {@link Persistence#FILE_MYPERSISTENCE}.
     * 
     * @return path to transformed persistence-file: {@link Persistence#FILE_MYPERSISTENCE}
     */
    ITransformer<String, String> getPersistenceTransform() {
        return new ITransformer<String, String>() {
            @Override
            public String transform(String name) {
                if (name.contains(Persistence.FILE_PERSISTENCE_XML)) {
                    LOG.info("manipulating url of " + name
                        + " ==> using file: "
                        + Persistence.getPath(Persistence.FILE_MYPERSISTENCE));
                    name = Persistence.FILE_MYPERSISTENCE;
                }
                return name;
            }
        };
    }
}
