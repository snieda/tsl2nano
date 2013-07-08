/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Nov 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet;

/**
 * Provides a full path to an object. This is more than an identifiable object - to be able to work on
 * place-holders/expressions. Using the {@link #hashCode()} of an object an Object is identifiable (so you don't need an
 * extra interface for that).
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public interface ILocatable {
    /**
     * full path to current object. the end of the path should be the id of that object.
     * 
     * @return path of current object
     */
    String getPath();
}
