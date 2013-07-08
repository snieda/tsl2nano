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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.util.FileUtil;

/**
 * application cache to store simple data. uses apache common {@link ReferenceMap} to avoid out-of-memory errors.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class PersistentCache {
    transient static PersistentCache self = null;
    transient static String cacheFilePath;

    @SuppressWarnings("unchecked")
    ReferenceMap cache = null;

    private static final Log LOG = LogFactory.getLog(PersistentCache.class);

    @SuppressWarnings("unchecked")
    private PersistentCache() {
        assert self == null : "don't call this constructor. only for internal use!";
        cache = new ReferenceMap(ReferenceMap.SOFT, ReferenceMap.SOFT);
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
    public static PersistentCache createInstance(String cachePath) {
        assert self == null : "cache already initialized!";
        self = new PersistentCache();
        cacheFilePath = cachePath + "/swartifex-persistentcache.xml";
        try {
            if (new File(cacheFilePath).exists()) {
                self.cache = (ReferenceMap) FileUtil.loadXml(cacheFilePath);
            }
        } catch (final Exception e) {
            ForwardedException.forward(e);
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
    public Object put(Object key, Object value) {
        final Object result = cache.put(key, value);
        try {
            FileUtil.saveXml(cache, cacheFilePath);
        } catch (final Exception e) {
            ForwardedException.forward(e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Object remove(Object key) {
        final Object result = cache.remove(key);
        try {
            FileUtil.saveXml(cache, cacheFilePath);
        } catch (final Exception e) {
            ForwardedException.forward(e);
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
}
