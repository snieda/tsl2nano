/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.h5.navigation;

import de.tsl2.nano.bean.def.BeanDefinition;

/**
 * base for NanoH5-Bean-Navigation
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public interface IBeanNavigator {
    /**
     * getName
     * @return navigators name
     */
    String getName();
    /**
     * adds new bean
     * @param bean to add
     */
    void add(BeanDefinition<?> bean);
    
    /**
     * isEmtpy
     * @return true, if no items available
     */
    boolean isEmpty();
    
    /**
     * current
     * 
     * @return current navigation bean
     */
    BeanDefinition<?> current();

    /**
     * evaluates the next bean to work on
     * 
     * @param userResponseObject
     * @return next bean
     */
    BeanDefinition<?> next(Object userResponseObject);

    /**
     * evaluates, if a navigation item (bean) was clicked.
     * 
     * @param uri uri to analyze
     * @return navigation bean or null
     */
    BeanDefinition<?> fromUrl(String uri);

    /**
     * puts the current navigation stack to an array
     * 
     * @return new array
     */
    BeanDefinition<?>[] toArray();
    
    /**
     * done
     * @return true, if no more navigation objects are available
     */
    boolean done();
}
