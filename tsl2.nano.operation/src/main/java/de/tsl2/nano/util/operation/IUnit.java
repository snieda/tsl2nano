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
 * see {@link #getUnit()}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IUnit<U> {
    /** defines the unit of a value */
    U getUnit();
}
