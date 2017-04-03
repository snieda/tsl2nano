/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.util;

/**
 * describes a fuzzy finder algorithm
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IFuzzyDescriptor<T> {
    /** provides all available data to use the filter on */
    Iterable<T> getAvailables();

    /** calculates the distance between an available item and the given expression/filter */
    double distance(T item, String expression);
}
