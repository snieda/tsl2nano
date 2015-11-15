/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 26, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core;

/**
 * Transforms an object from S to T.
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public interface ITransformer<S, T> {
    /** Transforms an object from S to T. */
    T transform(S toTransform);
}
