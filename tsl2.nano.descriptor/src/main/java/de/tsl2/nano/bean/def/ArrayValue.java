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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;

import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.core.cls.IDefaultAttribute;
import de.tsl2.nano.core.messaging.EventController;

/**
 * Object[] value.
 * 
 * @author Tom
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
public class ArrayValue<T> implements IValueAccess<T>, IDefaultAttribute<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1460468414949211876L;

    transient Object[] instance;
    @Attribute
    Class<T> type;
    @Attribute
    String name;
    @Attribute
    int index;

    @Element(required=false)
    EventController eventController;

    public ArrayValue() {
    }
    
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
    public Class getDeclaringClass() {
        return Object[].class;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public T getValue(Object instance) {
        return (T) (instance instanceof Object[] ? ((Object[]) instance)[index] : instance != null && index == 0
            ? instance : null/*throw new IllegalArgumentException(this + " can't evaluate value for instance " + instance)*/);
    }

    @Override
    public void setValue(Object instance, T value) {
        ((Object[]) instance)[index] = value;
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
        if (eventController == null) {
            eventController = new EventController();
        }
        return eventController;
    }
}
