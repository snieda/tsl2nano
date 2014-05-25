/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 17.05.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import java.io.Serializable;

import de.tsl2.nano.bean.def.BeanDefinition;

/**
 * is able to create an object that holds all needed information to connect to a back-end.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public interface IConnector<PERSISTENCE extends Serializable> {
    /**
     * getAuthentificationBean
     * 
     * @return authentification bean
     */
    PERSISTENCE createConnectionInfo();

    /**
     * connect to backend through a persistence provider
     * 
     * @return result - normally a list of available entity beans
     */
    BeanDefinition<?> connect(PERSISTENCE connectionInfo);
}
