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

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.messaging.EventController;

/**
 * Object[] value.
 * 
 * @author Tom
 * @version $Revision$
 */
public class ArrayValue<T> implements IValueAccess<T>, IAttribute<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1460468414949211876L;

    transient Object[] instance;
    @Attribute
    Class<T> type;
    @Attribute
    String name;
    @Attribute
    int index;
    transient EventController eventController;

    public ArrayValue(String name, int index) {
        this(name, index, null, null);
    }

    /**
     * constructor
     * 
     * @param object
     * @param type
     */
    public ArrayValue(String name, int index, Class<T> type, Object[] instance) {
        this.name = name;
        this.index = index;
        this.type = (Class<T>) (type != null ? type : instance != null ? instance[index].getClass() : Object.class);
        this.instance = instance;
    }

    @Override
    public int compareTo(IAttribute<T> o) {
        return getId().compareTo(o.getId());
    }

    @Override
    public Class getDeclaringClass() {
        return Object[].class;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T getValue(Object instance) {
        return (T) ((Object[]) instance)[index];
    }

    @Override
    public void setValue(Object instance, T value) {
        ((Object[]) instance)[index] = value;
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
        return getValue(instance);
    }

    @Override
    public void setValue(T value) {
        setValue(instance, value);
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public EventController changeHandler() {
        if (eventController == null)
            eventController = new EventController();
        return eventController;
    }
}
