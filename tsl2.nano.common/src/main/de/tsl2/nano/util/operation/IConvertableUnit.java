/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.operation;

/**
 * combination of a convertable value and its unit
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IConvertableUnit<T, U> extends IConverter<T, Number>, IUnit<U> {
}
