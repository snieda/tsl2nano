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

import de.tsl2.nano.bean.IAttribute;
import de.tsl2.nano.bean.ValueHolder;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public class Value<T> extends ValueHolder<T> implements IAttribute<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1460468414949211876L;

    @Attribute
    String name;
    
    public Value(String name) {
        this(name, null, null);
    }
    
    /**
     * constructor
     * @param object
     * @param type
     */
    public Value(String name, Class<T> type, T object) {
        super(object, type);
        this.name = name;
    }
    
    @Override
    public int compareTo(IAttribute<T> o) {
        return getId().compareTo(o.getId());
    }

    @Override
    public Class getDeclaringClass() {
        return getType();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T getValue(Object instance) {
        return getValue();
    }

    @Override
    public void setValue(Object instance, T value) {
        setValue(value);
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
}
