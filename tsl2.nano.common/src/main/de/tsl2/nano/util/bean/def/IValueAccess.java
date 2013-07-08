package de.tsl2.nano.util.bean.def;

import de.tsl2.nano.messaging.EventController;

public interface IValueAccess<T> {

    public static final String ATTR_VALUE = "value";
    public static final String KEY_VALUE = "valueHolder." + ATTR_VALUE;

    public static final String ATTR_TYPE = "type";
    public static final String KEY_TYPE = "valueHolder." + ATTR_TYPE;
    
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