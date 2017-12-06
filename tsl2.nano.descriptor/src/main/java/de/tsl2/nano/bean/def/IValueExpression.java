/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 21.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import de.tsl2.nano.core.cls.BeanAttribute;

/**
 * base definition for all calculation attributes like rules or sql-statements.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IValueExpression<T> {

    String getName();
    /**
     * optional expression description
     * 
     * @return the expression pattern
     */
    String getExpressionPattern();

    /**
     * getExpression
     * 
     * @return the defined expression or null
     */
    String getExpression();

    /**
     * if you set a value expression (see {@link ValueExpression}, the value returned by {@link #getValue(Object)} will
     * be evaluated through call of {@link ValueExpression#from(String)} - not through standard mechanism
     * {@link BeanAttribute#getValue(Object)}.
     * 
     * @param valueExpression string representation of a new {@link ValueExpression}.
     */
    void setExpression(String valueExpression);

    /**
     * getType
     * 
     * @return type of value evaluated through expression
     */
    Class<T> getType();

    /**
     * getValue
     * 
     * @param instance
     * @return value evaluated through expression using given instance
     */
    T getValue(Object instance);
}
