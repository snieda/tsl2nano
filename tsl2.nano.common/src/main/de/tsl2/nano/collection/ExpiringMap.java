/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.02.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.collection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * map holding entries with a lifecycle defined by a {@link #timeout}. on adding elements, old elements may be removed.
 * useful for in-memory caches. if you define a timeout of -1, the expiration functionality will be switched off.
 * <p/>
 * Note-1: the {@link LinkedHashMap} implementation provides an accessOrder (see it's constructors) and an unimplemented
 * {@link #removeEldestEntry(java.util.Map.Entry)} - these are not usable for our cache.
 * <p/>
 * Note-2: As the capacity() evaluation is only be accessible through pure-performance reflection, we may use our
 * timeout variable as {@link #shrink()}ing frequency.
 * 
 * @author Tom
 * @version $Revision$
 */
public class ExpiringMap<K, V> extends LinkedHashMap<K, V> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(ExpiringMap.class);
    
    /** holding touch-times of all entries */
    Map<K, Long> touches;
    /** maps current capacity (as it is not visible from here, we get it through reflection!) */
    int capacity;
    /** life cycle of elements */
    long timeout;

    /**
     * constructor
     * 
     * @param timeout
     */
    public ExpiringMap(long timeout) {
        super();
        this.timeout = timeout;
    }

    /**
     * constructor
     * 
     * @param initialCapacity
     * @param loadFactor
     * @param accessOrder
     */
    public ExpiringMap(int initialCapacity, float loadFactor, boolean accessOrder, long timeout) {
        super(initialCapacity, loadFactor, accessOrder);
        init(timeout);
    }

    /**
     * constructor
     * 
     * @param initialCapacity
     * @param loadFactor
     */
    public ExpiringMap(int initialCapacity, float loadFactor, long timeout) {
        super(initialCapacity, loadFactor);
        init(timeout);
    }

    /**
     * constructor
     * 
     * @param initialCapacity
     */
    public ExpiringMap(int initialCapacity, long timeout) {
        super(initialCapacity);
        init(timeout);
    }

    /**
     * constructor
     * 
     * @param m
     */
    public ExpiringMap(Map<? extends K, ? extends V> m, long timeout) {
        super(m);
        init(timeout);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        if (timeout != -1) {
            Long access = touches.get(key);
            if (access != null && System.currentTimeMillis() - access > timeout) {
                remove(key);
                return null;
            }
            touch((K) key);
        }
        return super.get(key);
    }

    private void touch(K key) {
        touches.put(key, System.currentTimeMillis());
    }

    @Override
    public V put(K key, V value) {
        if (timeout != -1) {
            touch(key);
            shrink();
        }
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        if (timeout != -1) {
            touches.remove(key);
        }
        return super.remove(key);
    }

    @Override
    public void clear() {
        if (timeout != -1) {
            touches.clear();
        }
        super.clear();
    }

    /**
     * does the thing and removes expired objects.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void shrink() {
        if (size() >= capacity) {
            long now = System.currentTimeMillis();
            Set<java.util.Map.Entry<K, V>> entries = entrySet();
            for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
                Entry<K, V> entry = (Entry<K, V>) iterator.next();
                if (now - touches.get(entry.key) > timeout) {
                    LOG.info("removing expired entry " + entry);
                    iterator.remove();
                }
            }
            refreshCapacity();
        }
    }

    /**
     * creates the touch-time table
     * 
     * @param timeout The timeout to set.
     */
    protected void init(long timeout) {
        if (timeout != -1) {
            touches = new HashMap<K, Long>();
            Set<K> ks = keySet();
            long now = System.currentTimeMillis();
            for (K k : ks) {
                touches.put(k, now);
            }
            refreshCapacity();
        }
        this.timeout = timeout;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void refreshCapacity() {
        capacity = (Integer) new PrivateAccessor(this).call("capacity", Integer.class);
    }
}
