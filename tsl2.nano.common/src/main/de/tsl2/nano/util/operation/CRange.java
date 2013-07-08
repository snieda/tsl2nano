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

import java.io.Serializable;

/**
 * generic range object. usable for comparable values like numbers, dates, strings. but it is usable too for any
 * objects, implementing {@link Comparable}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class CRange<T extends Comparable<T>> extends Range<T> implements Comparable<CRange<T>>, Serializable, IRange<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 8470377216770952614L;

    /** used to transform the current value object into a number of type long and vice versa.  */
    IConverter<T, Number> converter;
    /**
     * constructor
     * 
     * @param from minimum value. if null, the converters minimum will be used
     * @param to maximum value. if null, the converters maximum will be used
     * @param converter (optional) object to long converter to represent an object as number value.
     */
    public <C extends IConverter<T, Number>> CRange(T from, T to, C converter) {
        super(from, to);
        this.converter = converter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(CRange<T> o) {
        if (this.getFrom().compareTo(o.getFrom()) == 0) {
            return this.getTo().compareTo(o.getTo());
        } else {
            return this.getFrom().compareTo(o.getFrom());
        }

    }

    /**
     * Calculates the span between from and to
     * 
     * @return the span between from and to.
     */
    public Number getDelta() {
        return converter.to(to).doubleValue() - converter.to(from).doubleValue();
    }

    /**
     * checks, if given value is inside range
     * @param another value
     * @return true, if another value is inside range
     */
    public boolean contains(T another) {
        return contains(new CRange<T>(another, another, converter));
    }
    
    /**
     * Check if this period includes another one.
     * 
     * @param period the period that is checked as included
     * @return true if included
     */
    public boolean contains(IRange<T> another) {
        if (another == null) {
            return false;
        }
        final T start = getFrom() != null ? getFrom() : converter.from(Long.MIN_VALUE);
        final T end = getTo() != null ? getTo() : converter.from(Long.MAX_VALUE);
        final T astart = another.getFrom() != null ? another.getFrom() : converter.from(Long.MIN_VALUE);
        final T aend = another.getTo() != null ? another.getTo() : converter.from(Long.MAX_VALUE);
        return start.compareTo(aend) <= 0 && end.compareTo(astart) >= 0;
    }

    /**
     * Determines if a given period intersects the range.
     * 
     * @param range range
     * @return true if the given range intersects the current range
     */
    public boolean intersects(IRange<T> range) {
        return intersects(range.getFrom(), range.getTo());
    }

    /**
     * Another variant of intersection check (save range construction).
     * 
     * @param from from value of other range
     * @param to to value of other range
     * @return true if there is an intersection
     */
    public boolean intersects(T from, T to) {
        if (getTo().compareTo(from) >= 0 && getFrom().compareTo(to) <= 0) {
            return true;
        }
        return false;
    }
}
