/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 16.01.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.collection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Combines the interfaces of {@link List} and {@link Map}. The underlying instance is a map - the list will hold the
 * maps entries {@link Map#entrySet()}.
 * <p/>
 * Problem: It is not possible to create a class implementing both interfaces of {@link List} and {@link Map}. There is
 * an interface name-clash on the remote(Object) method.<br/>
 * Errormessages: The return types are incompatible for the inherited methods Map<K,V>.remove(Object),
 * LinkedList<Map.Entry<K,V>>.remove(Object)
 * <p/>
 * Map.Entry is not serializable!<br/>
 * 
 * @author Tom
 * @version $Revision$
 */
public class MapEntrySet<K, V> extends LinkedHashSet<Entry<K, V>> {
    /** serialVersionUID */
    private static final long serialVersionUID = -7564082533237090900L;

    Map<K, V> map;
    boolean sync;
    private boolean internal;

    public MapEntrySet(Map<K, V> map) {
        this.map = map;
        Set<Map.Entry<K, V>> entrySet = map.entrySet();
        internal = true;
        for (Map.Entry<K, V> entry : entrySet) {
            add(new Entry(entry, sync, map));
        }
        internal = false;
    }

    /**
     * map
     * 
     * @return refreshed map - having all entries of this instance.
     */
    public Map<K, V> map() {
        map.clear();
        for (Map.Entry<K, V> e : this) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    @Override
    public boolean add(Entry<K, V> e) {
        boolean result = super.add(e);
        if (result && !internal && sync) {
            map.put(e.getKey(), e.getValue());
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        boolean result = super.remove(o);
        if (result && !internal && sync)
            map.remove(((Map.Entry<K, V>) o).getKey());
        return result;
    }

    /**
     * setDirectSynchronization
     * 
     * @param sync if true, setting an entries value will be mapped to the map
     */
    public void setDirectSynchronization(boolean sync) {
        this.sync = sync;
    }

}
