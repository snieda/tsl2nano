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
 * Standard operand providing operations through {@link IOperable}. it wraps the {@link Comparable} interface to become
 * readable/usable. in other words: wraps any java-bean to be a comparable number.
 * <p/>
 * if null-converter was given , only a constraint selection of operations are available.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class Operable<T extends Comparable<T>> implements IOperable<T> {
    /** to-long converter to be used by numeric operations */
    IConverter<T, Number> converter;
    /** operand */
    T value;

    /**
     * constructor
     * 
     * @param value operands value
     * @param converter (optional) converter to provide operations like substract() and add()
     */
    public Operable(T value, IConverter<T, Number> converter) {
        super();
        this.value = value;
        this.converter = converter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLower(T operand) {
        return value.compareTo(operand) < 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGreater(T operand) {
        return value.compareTo(operand) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T min(T operand) {
        return isLower(operand) ? value : operand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T max(T operand) {
        return isGreater(operand) ? value : operand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getDiff(T operand) {
        checkConverter();
        return converter.from(getDelta(operand));
    }

    protected double getDelta(T operand) {
        return converter.to(value).doubleValue() - converter.to(operand).doubleValue();
    }

    protected double getSum(T operand) {
        return converter.to(value).doubleValue() + converter.to(operand).doubleValue();
    }

    protected double getMultiplication(T operand) {
        return converter.to(value).doubleValue() * converter.to(operand).doubleValue();
    }

    protected double getDivision(T operand) {
        return converter.to(value).doubleValue() / converter.to(operand).doubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T subtract(T operand) {
        checkConverter();
        return converter.from(getDelta(operand));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T add(T operand) {
        checkConverter();
        return converter.from(getSum(operand));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T multiply(T operand) {
        checkConverter();
        return converter.from(getMultiplication(operand));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T divide(T operand) {
        checkConverter();
        return converter.from(getDivision(operand));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue() {
        return value;
    }

    public Number getConversion() {
        checkConverter();
        return converter.to(value);
    }
    /**
     * throws an exception, if converter is null.
     */
    protected void checkConverter() {
        if (converter == null) {
            throw new UnsupportedOperationException("without converter, this operation is undefined!");
        }
    }
}
