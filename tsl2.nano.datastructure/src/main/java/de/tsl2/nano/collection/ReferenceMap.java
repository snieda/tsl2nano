/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 26.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.collection;

/**
 * Simple HashMap holding it's values as Soft- or Weak References. To have weak references on keys, use java's WeakHashMap.
 * @author Tom
 * @version $Revision$ 
 */
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ReferenceMap<K, V> extends AbstractMap<K, V>
        implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** internal HashMap holding the Reference. */
    private final Map<K, Reference<V>> hash =
        new HashMap<K, Reference<V>>();

    private final Map<Reference<V>, K> reverseLookup =
        new HashMap<Reference<V>, K>();

    /** Reference queue for cleared Reference objects. */
    transient private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

    private boolean weak = true;

    /**
     * hashmap as weak map
     */
    public ReferenceMap() {
        this(true);
    }

    /**
     * constructor
     * @param weak if false, {@link SoftReference}s will be used on it's values.
     */
    public ReferenceMap(boolean weak) {
        super();
        this.weak = weak;
    }
    
    /**
     * creates a reference using {@link #weak} to instantiate {@link WeakReference} or {@link SoftReference}.
     * 
     * @param value value
     * @param queue queue
     * @return new {@link Reference} instance
     */
    protected Reference<V> createReference(V value, ReferenceQueue<V> queue) {
        return weak ? new WeakReference<V>(value, queue) : new SoftReference<V>(value, queue);
    }

    @Override
    public V get(Object key) {
        expungeStaleEntries();
        V result = null;
        // We get the Reference represented by that key
        Reference<V> soft_ref = hash.get(key);
        if (soft_ref != null) {
            // From the Reference we get the value, which can be
            // null if it has been garbage collected
            result = soft_ref.get();
            if (result == null) {
                // If the value has been garbage collected, remove the
                // entry from the HashMap.
                hash.remove(key);
                reverseLookup.remove(soft_ref);
            }
        }
        return result;
    }

    private void expungeStaleEntries() {
        Reference<? extends V> sv;
        while ((sv = queue.poll()) != null) {
            hash.remove(reverseLookup.remove(sv));
        }
    }

    @Override
    public V put(K key, V value) {
        expungeStaleEntries();
        Reference<V> soft_ref = createReference(value, queue);
        reverseLookup.put(soft_ref, key);
        Reference<V> result = hash.put(key, soft_ref);
        if (result == null) {
            return null;
        }
        reverseLookup.remove(result);
        return result.get();
    }

    @Override
    public V remove(Object key) {
        expungeStaleEntries();
        Reference<V> result = hash.remove(key);
        if (result == null) {
            return null;
        }
        return result.get();
    }

    @Override
    public void clear() {
        hash.clear();
        reverseLookup.clear();
    }

    @Override
    public int size() {
        expungeStaleEntries();
        return hash.size();
    }

    /**
     * Returns a copy of the key/values in the map at the point of calling. However, setValue still sets the value in
     * the actual HashMap.
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        expungeStaleEntries();
        Set<Entry<K, V>> result = new LinkedHashSet<Entry<K, V>>();
        for (final Entry<K, Reference<V>> entry : hash.entrySet()) {
            final V value = entry.getValue().get();
            if (value != null) {
                result.add(new Entry<K, V>() {
                    @Override
                    public K getKey() {
                        return entry.getKey();
                    }

                    @Override
                    public V getValue() {
                        return value;
                    }

                    @Override
                    public V setValue(V v) {
                        entry.setValue(createReference(v, queue));
                        return value;
                    }
                });
            }
        }
        return result;
    }
}
