/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 09.04.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.bean.def;

/**
 * provides an instance for a definition.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IConstructable<T> {
    /**
     * @return instance of this definition
     */
    T getInstance();

    /**
     * sets the instance for this definition
     * @param instance new instance
     */
    void setInstance(T instance);
}
