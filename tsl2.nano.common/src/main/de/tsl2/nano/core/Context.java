/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Dec 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.core;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.collection.FilteringIterator;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.XmlUtil;

/**
 * persistable context
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@Default(value = DefaultType.FIELD, required = false)
public class Context implements Serializable, Map {

    /** serialVersionUID */
    private static final long serialVersionUID = -5738577461481485970L;

    @Attribute
    String name;

    @ElementMap(entry = "property", key = "name", attribute = true, inline = true, required = false, keyType = String.class, valueType = Object.class)
    private TreeMap properties;

    /** if true, this context will be persisted on any change. */
    transient boolean autopersist = false;

    /**
     * creates a new instance from file or through constructor
     * @param name context name, used as filename on persisting to file system
     * @param autoPersist whether to persist this context on each change. if name is null, no persisting will be done.
     * @return new context instance
     */
    public static Context create(String name, boolean autoPersist) {
        Context context = null;
        if (name == null) {
            context = new Context();
            autoPersist = false;
        } else {
            String fname = getFileName(name);
            File file = new File(fname);
            if (file.exists()) {
                context = XmlUtil.loadXml(file.getPath(), Context.class);
            } else {
                context = new Context(name);
            }
        }
        context.init(autoPersist);
        return context;
    }

    /**
     * init
     * 
     * @param context
     * @param autoPersist
     */
    protected void init(boolean autoPersist) {
        this.properties = new TreeMap();
        this.autopersist = autoPersist;
    }

    protected Context() {
        this(null);
    }

    protected Context(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return properties.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        return properties.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object key) {
        return properties.get(key);
    }

    public Object add(Object obj) {
        String k = String.valueOf(size());
        put(k, obj);
        return k;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object put(Object key, Object value) {
        Object result = properties.put(key, value);
        if (autopersist) {
            save();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object remove(Object key) {
        Object result = properties.remove(key);
        if (autopersist) {
            save();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map m) {
        properties.putAll(m);
        if (autopersist) {
            save();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set keySet() {
        return properties.keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection values() {
        return properties.values();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set entrySet() {
        return properties.entrySet();
    }

    /**
     * resets the current environment to be empty
     */
    public void clear() {
        properties.clear();
        if (autopersist) {
            save();
        }
    }

    /**
     * if no value was found for given key, the defaultValue will be put to the map and returned
     * 
     * @param key
     * @param defaultValue
     * @return map value or defaultValue
     */
    public <T> T get(String key, T defaultValue) {
        T value = (T) properties.get(key);
        if (value == null && defaultValue != null) {
            value = defaultValue;
            put(key, value);
        }
        return value;
    }

    /**
     * filters only values of valueType to be returned in a filtered iterator
     * 
     * @param valueType
     * @return filtered map values
     */
    public <T> Iterator<T> get(final Class<T> valueType) {
        return (Iterator<T>) new FilteringIterator(properties.values(), new IPredicate() {
            @Override
            public boolean eval(Object arg0) {
                return arg0 != null && valueType.isAssignableFrom(arg0.getClass());
            }
        });
    }

    /**
     * isPersisted
     * 
     * @return true, if this context was created on file system
     */
    public boolean isPersisted() {
        return new File(getFileName(getName())).exists();
    }

    /**
     * persists the current context
     */
    public void save() {
        XmlUtil.saveXml(getFileName(getName()), this);
    }

    public String getName() {
        if (name == null)
            name = FileUtil.getUniqueFileName("context");
        return name;
    }

    public static String getFileName(String name) {
        return ENV.getTempPathRel() + "context-" + name + ".xml";
    }

    @Override
    public String toString() {
        return getName();
    }
}