/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */

package de.tsl2.nano.modelkit;

public interface TriFunction<T, U, V, R> {
    R apply(T t, U u, V v);
}
