/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 26, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.collection;

/**
 * Transforms an object into another one.
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public interface ITransformer<S, T> {
    S transform(T toTransform);
}
