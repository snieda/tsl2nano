/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Sep 14, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.bean;

import java.io.Serializable;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.def.IValueAccess;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.ChangeEvent;

/**
 * simple object holder to be used as synthetic bean attribute.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ValueHolder<T> implements Serializable, IValueAccess<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 7453785034902931979L;

    T value;
    @Attribute
    Class<T> type;
    transient EventController changeHandler;

    public ValueHolder(T object) {
        this(object, null);
    }

    /**
     * constructor
     * 
     * @param object initial value
     */
    @SuppressWarnings("unchecked")
    public ValueHolder(T object, Class<T> type) {
        super();
        this.value = object;
        this.type = type != null ? type : (Class<T>) (object != null ? object.getClass() : Object.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(T object) {
        if (changeHandler == null || !changeHandler.hasListeners()) {
            this.value = object;
        } else {
            final T oldValue = getValue();
            final ChangeEvent event = new ChangeEvent(this, false, false, oldValue, object);
            changeHandler.fireEvent(event);
            if (!event.breakEvent) {
                this.value = object;
                event.hasChanged = true;
                changeHandler.fireEvent(event);
            }
        }
    }

    /**
     * type
     * 
     * @return type of value
     */
    @Override
    public Class<T> getType() {
        return type;
    }

    /**
     * it is no getter to avoid being a bean attribute.
     * 
     * @return Returns the changeHandler.
     */
    public EventController changeHandler() {
        if (changeHandler == null)
            changeHandler = new EventController();
        return changeHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Value[" + value + "]";
    }
}
