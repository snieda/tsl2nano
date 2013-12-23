/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 25, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.collection;

/**
 * evaluates a true or false for a given object - useful for filtering.
 * 
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public interface IPredicate<T> {
    public boolean eval(T arg0);
}
