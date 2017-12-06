/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 13, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.tsl2.nano.bean.ValueHolder;


/**
 * works like a property with a key-value-pair. usable as simple bean in a property container. using a
 * {@link Collection} of {@link NamedValue} is similar to {@link Map} with key and values in their
 * {@link Map#entrySet()}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class NamedValue<T> extends ValueHolder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 4072634237789434505L;

    String name;

    public static final String ATTR_NAME = "name";
    public static final String KEY_NAME = "namedValue.name";

    /**
     * constructor
     * 
     * @param object
     */
    public NamedValue(String name, T object) {
        super(object);
        this.name = name;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /*
     * some Collection<NamedValue> Utilities (perhaps we put that into an own class)
     */

    public Set<String> keySet(Collection<NamedValue<T>> namedValues) {
        return null;
    }

    public Collection<T> values(Collection<NamedValue<T>> namedValues) {
        return null;
    }

    public T get(Collection<NamedValue<T>> namedValues, String key) {
        return null;
    }

    /**
     * transform a {@link Collection} of {@link NamedValue} to a map like {@link Properties} or {@link HashMap}.
     * 
     * @param src to get the pairs from
     * @param dest to put the pairs to
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void putAll(Collection<NamedValue> src, Map dest) {
        for (final NamedValue namedValue : src) {
            dest.put(namedValue.getName(), namedValue.getValue());
        }
    }

    /**
     * transform a map like {@link Properties} or {@link HashMap} to a {@link Collection} of {@link NamedValue}.
     * 
     * @param src to get the pairs from
     * @param dest to put the pairs to
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void putAll(Map src, Collection<NamedValue> dest) {
        final Set<?> keySet = src.keySet();
        for (final Object k : keySet) {
            dest.add(new NamedValue(k.toString(), src.get(k)));
        }
    }
}
