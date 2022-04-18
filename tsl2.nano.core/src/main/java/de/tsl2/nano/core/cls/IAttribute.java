/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 22.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.core.cls;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * basic attribute properties
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IAttribute<T> extends Comparable<IAttribute<T>>, Serializable {

    /**
     * @return type of parent instance holding this attribute and/or used by {@link #getValue(Object)} and
     *         {@link #setValue(Object, Object)}
     */
    Class getDeclaringClass();

    /**
     * @return attribute name
     */
    String getName();

    /** the values class type */
    Class<T> getType();

    /**
     * evaluates the attribute value for the given instance. no generic type is given because the implementing base
     * class BeanAttribute doesn't define that
     */
    T getValue(Object instance);

    /** sets a new value for the given instance */
    void setValue(Object instance, T value);

    /**
     * @return unique attribute id
     */
    String getId();

    /**
     * @return true, if new value can be set through {@link #setValue(Object, Object)}
     */
    boolean hasWriteAccess();

    /**
     * @return for internal use only! getter method of current attribute or null, if not available (then it is virtual).
     */
    Method getAccessMethod();

    /**
     * @return true, if no special, physical bean-attribute is defined. perhaps on expressions or using the generic
     *         IValueAccess.getValue().
     */
    boolean isVirtual();

    /**
     * usable on virtual attributes to be defined by a parent.
     * @param name name that is defined by a parent.
     */
    void setName(String name);

    /**
     * convenience to recurse into the given sibling attribute
     * @param name name of this attributes attribute (evaluated through return type)
     * @return sibling attribute
     */
    default IAttribute<?> getAttribute(String name) { throw new UnsupportedOperationException();}
}
