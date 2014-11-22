/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 21.11.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.repeat;

/**
 * any change to be done on the item. if the item is null, the complete item will change from 'old' to 'new'.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public interface IChange {
    /**
     * @return item to change from old to new
     */
    Object getItem();

    /**
     * @return item state before change
     */
    Object getOld();

    /**
     * @return item state after change
     */
    Object getNew();

    /**
     * @return new reversed change object
     */
    IChange revert();
}
