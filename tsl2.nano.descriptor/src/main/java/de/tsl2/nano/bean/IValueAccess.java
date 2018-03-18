package de.tsl2.nano.bean;

import de.tsl2.nano.core.messaging.EventController;

public interface IValueAccess<T> {

    static final String ATTR_VALUE = "value";
    static final String KEY_VALUE = "valueHolder." + ATTR_VALUE;

    static final String ATTR_TYPE = "type";
    static final String KEY_TYPE = "valueHolder." + ATTR_TYPE;
    
    /**
     * @return Returns the object.
     */
    T getValue();

    /**
     * @param object The object to set.
     */
    void setValue(T object);

    /**
     * getType
     * @return type of value or null
     */
    Class<T> getType();
    
    /** returns the event controller to add and remove value change listeners */
    EventController changeHandler();
}