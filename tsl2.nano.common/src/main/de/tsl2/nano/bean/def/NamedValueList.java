/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 15, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.collection.CollectionUtil;

/**
 * A list of NamedValues - providing a map view. It's like the opposite to the {@link Map} with its collection view
 * through {@link Map#entrySet()}.
 * <p>
 * 
 * UNUSED AND UNTESTED YET!
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */

/*
 * TODO: it's not possible to implement collection and map interface? remove(Object) has different return types!
 */
public class NamedValueList<T> extends LinkedList<NamedValue<T>>/* implements Map<String, T>*/{
    Collection<String> keys = null;
    Collection<T> values = null;

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * constructor
     */
    public NamedValueList() {
        super();
    }

    /**
     * constructor
     * 
     * @param c
     */
    public NamedValueList(Collection<NamedValue<T>> c) {
        super(c);
    }

    protected Collection<String> keys() {
        if (keys == null) {
            keys = CollectionUtil.getTransformedCollection(this, "name");
        }
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return keys.contains(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    /**
     * {@inheritDoc}
     */
    public T get(Object key) {
        //TODO: implement
        return null;//CollectionUtil.find(this, NamedValue.class, NamedValue.ATTR_NAME, key);
    }

    /**
     * {@inheritDoc}
     */
    public Object put(Object key, Object value) {
        return add(new NamedValue(key.toString(), value));
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map m) {
        final Set keySet = m.keySet();
        for (final Object k : keySet) {
            add(new NamedValue<T>((String) k, (T) m.get(k)));
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set keySet() {
        return new LinkedHashSet(keys());
    }

    /**
     * {@inheritDoc}
     */
    public Collection values() {
        if (values == null) {
            values = CollectionUtil.getTransformedCollection(this, IValueAccess.ATTR_VALUE);
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public Set entrySet() {
        return new LinkedHashSet(this);
    }

}
