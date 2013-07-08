package de.tsl2.nano.util.operation;

import java.io.Serializable;


/**
 * Simplest implementation of {@link IRange}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class Range<T> implements IRange<T>, Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = -8480523674748971225L;
    
    /** the range minimum value */
    protected T from;
    /** the range maximum value */
    protected T to;

    /**
     * constructor
     * @param from
     * @param to
     */
    public Range(T from, T to) {
        super();
        this.from = from;
        this.to = to;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getFrom() {
        return from;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getTo() {
        return to;
    }

}