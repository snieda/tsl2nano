/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 27.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.lang.reflect.Method;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.messaging.EventController;

/**
 * Object[] value.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class MapValue<T> implements IValueAccess<T>, IAttribute<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1460468414949211876L;

    transient Map map;
    @Attribute
    Class<T> type;
    @Element
    Object name;
    transient EventController eventController;

    protected MapValue() {
    }

    public MapValue(Object name) {
        this(name, null, null);
    }

    /**
     * constructor
     * 
     * @param object
     * @param type
     */
    public MapValue(Object name, Class<T> type, Map map) {
        this.name = name;
        this.type = (Class<T>) (type != null ? type : map != null && map.get(name) != null ? map.get(name).getClass() : Object.class);
        this.map = map;
    }

    @Override
    public int compareTo(IAttribute<T> o) {
        return getId().compareTo(o.getId());
    }

    @Override
    public Class<?> getDeclaringClass() {
        return map.getClass();
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public T getValue(Object instance) {
        return (T) (instance != null ? ((Map) instance).get(name) : map.get(name));
    }

    @Override
    public void setValue(Object instance, T value) {
        if (instance != null) {
            ((Map) instance).put(name, value);
        } else {
            map.put(name, value);
        }
    }

    @Override
    public String getId() {
        return getType().getSimpleName() + "." + name;
    }

    @Override
    public boolean hasWriteAccess() {
        return true;
    }

    @Override
    public Method getAccessMethod() {
        return null;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public T getValue() {
        return getValue(map);
    }

    @Override
    public void setValue(T value) {
        setValue(map, value);
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    public void setMap(Map map) {
        this.map = map;
    }
    
    @Override
    public EventController changeHandler() {
        if (eventController == null) {
            eventController = new EventController();
        }
        return eventController;
    }
}
