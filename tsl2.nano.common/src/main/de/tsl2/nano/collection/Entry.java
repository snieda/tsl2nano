package de.tsl2.nano.collection;

import java.io.Serializable;
import java.util.Map;

import de.tsl2.nano.core.util.Util;

/**
 * part of {@link MapEntrySet}. as the hashmap implementation doesn't make Map.Entry serializable, we have to write our
 * own entry class.
 * 
 * @param <K>
 * @param <V>
 * @author Tom
 * @version $Revision$
 */
public class Entry<K, V> implements Map.Entry<K, V>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    K key;
    V value;

    boolean sync;
    Map<K, V> map;

//    /**
//     * constructor no map connection available. {@link MapEntrySet} has to collect this entry on adding.
//     */
//    public Entry() {
//    }

    Entry(Map.Entry<K, V> src, boolean sync, Map<K, V> map) {
        key = src.getKey();
        value = src.getValue();
        this.sync = sync;
        this.map = map;
    }

    /**
     * constructor
     * 
     * @param key
     * @param value
     */
    public Entry(K key, V value) {
        super();
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        this.value = value;
        if (sync) {
            map.put(key, value);
        }
        return value;
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), key, value);
    }
}
