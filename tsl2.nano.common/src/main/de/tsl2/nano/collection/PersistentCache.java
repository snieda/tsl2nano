/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: May 8, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.collection;

import java.io.File;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.XmlUtil;

/**
 * application cache to store simple data. uses an own {@link ReferenceMap} with weak keys to avoid out-of-memory errors.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class PersistentCache {
    transient static PersistentCache self = null;
    transient static String cacheFilePath;

    ReferenceMap cache = null;

    private static final Log LOG = LogFactory.getLog(PersistentCache.class);

    private PersistentCache() {
        assert self == null : "don't call this constructor. only for internal use!";
        cache = new ReferenceMap();
    }

    /**
     * instance (call createInstance(.) once before!)
     * 
     * @return singelton instance
     */
    public static final PersistentCache instance() {
        checkInstance();
        return self;
    }

    /**
     * createInstance
     * 
     * @param cachePath path without file name
     * @return new created instance
     */
    @SuppressWarnings("static-access")
    public static PersistentCache createInstance(String cachePath) {
        assert self == null : "cache already initialized!";
        self = new PersistentCache();
        cacheFilePath = cachePath + "/tsl2nano-persistentcache.xml";
        try {
            if (new File(cacheFilePath).exists()) {
                self.cache = ENV.get(XmlUtil.class).loadXml(cacheFilePath, ReferenceMap.class);
            }
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
        return self;
    }

    /**
     * isCreated
     * 
     * @return true, if createInstance(.) was called already.
     */
    public static boolean isCreated() {
        return self != null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "static-access" })
    public Object put(Object key, Object value) {
        final Object result = cache.put(key, value);
        try {
            ENV.get(XmlUtil.class).saveXml(cacheFilePath, cache);
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Object remove(Object key) {
        final Object result = cache.remove(key);
        try {
            ENV.get(XmlUtil.class).saveXml(cacheFilePath, cache);
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
        return result;
    }

    /**
     * get
     * 
     * @param key key of value
     * @return cached value
     */
    public Object get(Object key) {
        checkInstance();
        return cache.get(key);
    }

    /**
     * deleteCache
     * 
     * @return true, if cache file could be deleted
     */
    public boolean deleteCache() {
        checkInstance();
        cache.clear();
        LOG.info("deleting cache file '" + cacheFilePath + "'");
        return new File(cacheFilePath).delete();
    }

    private static final void checkInstance() {
        assert self != null : "please call createInstance(.) before using it!";

        /*
         * on working with different classloaders, no instance may be available
         * on current bundle - we create a soft instance with standard path
         * to avoid nullpointers - the temp-caching should not stop the application.
         * the assertion is ignored no product systems.
         */
        if (self == null) {
            LOG.warn("No instance initialized for caching - perhaps loaded through new classloader");
            createInstance(System.getProperty("user.home"));
        }
    }

    /**
     * clearCache
     */
    public static void clearCache() {
        self = null;
    }
}
