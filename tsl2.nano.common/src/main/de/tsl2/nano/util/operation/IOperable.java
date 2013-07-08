/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 19, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.operation;

/**
 * base for comparable objects like numbers, dates, strings - but for any other objects, too - to provide some standard
 * operations.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IOperable<T> {
    /** returns the current operand */
    T getValue();

    /** returns true, if the current value is lower than the given operand value */
    boolean isLower(T operand);

    /** returns true, if the current value is greater than the given operand value */
    boolean isGreater(T operand);

    /** returns the mimimum of current value and given operand */
    T min(T operand);

    /** returns the maximum of current value and given operand */
    T max(T operand);

    /** returns the difference between current value and given operand */
    T getDiff(T operand);

    /** returns the subtraction of current value and given operand */
    T subtract(T operand);

    /** returns the addition of current value and given operand */
    T add(T operand);

    /** returns the multiplication of current value and given operand */
    T multiply(T operand);

    /** returns the division of current value and given operand */
    T divide(T operand);

}
