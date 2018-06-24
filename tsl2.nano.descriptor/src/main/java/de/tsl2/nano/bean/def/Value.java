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

import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.IDefaultAttribute;
import de.tsl2.nano.core.cls.IValueAccess;

/**
 * Simple implementation of {@link IValueAccess} and {@link IAttribute}
 * @author Tom
 * @version $Revision$ 
 */
public class Value<T> extends ValueHolder<T> implements IDefaultAttribute<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1460468414949211876L;

    @Attribute
    String name;

    protected Value() {
        
    }
    
    public Value(String name) {
        this(name, null, null);
    }
    
    public Value(String name, T object) {
        this(name, null, object);
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
    public Class getDeclaringClass() {
        return getType();
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
        return getValue();
    }

    @Override
    public void setValue(Object instance, T value) {
        setValue(value);
    }

    @Override
    public String toString() {
        return getId();
    }
}
