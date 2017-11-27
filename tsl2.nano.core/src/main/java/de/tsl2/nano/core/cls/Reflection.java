/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 07.12.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.core.cls;

import java.util.LinkedHashMap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;

/**
 * reflects an object through its class and member informations. may be useful on serialization of not-owned classes.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Reflection<T> {
    /** the class to reflect objects from */
    @Attribute
    Class<T> type;
    /** some members in the order of an existing constructor parameters */
    @ElementMap(entry = "member", attribute = true, key="name", inline = true)
    LinkedHashMap<String, Object> members;

    /** the real object instance */
    transient T object;

    /**
     * reads the values of the given objects fields and creates an reflection object through delegating to
     * {@link #reflect(Object, LinkedHashMap)}.
     * 
     * @param obj real object instance
     * @param names field names to evaluate the values from
     * @return new reflection object
     */
    public static <T> Reflection<T> reflectFields(T obj, String... names) {
        PrivateAccessor<T> acc = new PrivateAccessor<T>(obj);
        return reflect(obj, (LinkedHashMap<String, Object>) acc.members(names));
    }

    /**
     * creates a map through given arguments and delegates to {@link #reflect(Object, LinkedHashMap)}
     */
    public static <T> Reflection<T> reflect(T obj, Object... args) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        int i = 0;
        for (Object arg : args) {
            map.put(String.valueOf(i++), arg);
        }
        return reflect(obj, map);
    }

    /**
     * delegates to {@link #reflect(Class, LinkedHashMap)}
     */
    @SuppressWarnings("unchecked")
    public static <T> Reflection<T> reflect(T obj, LinkedHashMap<String, Object> members) {
        Reflection<T> ref = (Reflection<T>) reflect(obj.getClass(), members);
        ref.object = obj;
        return ref;
    }

    /**
     * reflects an object through its class name and some members
     * @param cls class to reflect
     * @param members field-names/values of the instance to reflect
     * @return new reflection object
     */
    public static <T> Reflection<T> reflect(Class<T> cls, LinkedHashMap<String, Object> members) {
        Reflection<T> ref = new Reflection<T>();
        ref.type = cls;
        ref.setChecked(members);
        return ref;
    }

    /**
     * creates a new object through reflection.
     * 
     * @return
     */
    public T object() {
        if (object == null)
            object = BeanClass.createInstance(type, members.values().toArray());
        return object;
    }

    public Class<T> type() {
        return type;
    }

    public Object member(String name) {
        return members.get(name);
    }

    /**
     * setMember
     * 
     * @param name
     * @param value
     * @return old value or null
     */
    public Object setMember(String name, Object value) {
        Object old = members.get(name);
        members.put(name, value);
        object = null;
        return old;
    }
    
    protected void setChecked(LinkedHashMap<String, Object> members) {
        //TODO: check if constructor exists
        this.members = members;
    }
}
