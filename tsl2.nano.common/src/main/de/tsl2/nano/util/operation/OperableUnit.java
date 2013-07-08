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
 * usable to compare values with defined units. f.e., a bigdecimal working as currency.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class OperableUnit<T extends Comparable<T>, U> extends Operable<T> implements IUnit<U> {

    /**
     * constructor
     * 
     * @param value
     * @param converter
     */
    public OperableUnit(T value, IConvertableUnit<T, U> converter) {
        super(value, converter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public U getUnit() {
        return ((IConvertableUnit<T, U>)converter).getUnit();
    }

    /**
     * converts the own value to the given operableunit.
     * 
     * @param convertableUnit new unit, to convert the own value to
     * @return converted value
     */
    public <T1 extends Comparable<T1>, U1> OperableUnit<T1, U1> convert(IConvertableUnit<T1, U1> convertableUnit) {
        return new OperableUnit<T1, U1>(convertableUnit.from(converter.to(value)), convertableUnit);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getConversion() + " " + getUnit();
    }
}
