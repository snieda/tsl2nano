/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 30.12.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.messaging;

/**
 * a stateful listener holding temporary data to be reseted.
 * @author Tom
 * @version $Revision$ 
 */
public interface IStatefulListener<T> extends IListener<T> {
    /**
     * reset stateful data
     */
    void reset();
}