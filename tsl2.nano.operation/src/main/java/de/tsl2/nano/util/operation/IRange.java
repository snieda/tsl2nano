package de.tsl2.nano.util.operation;

/**
 * @param <T> the object type for the range definition.
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IRange<T> {

    /**
     * @return the range minimum value
     */
    public abstract T getFrom();

    /**
     * @return Returns the range maximum value
     */
    public abstract T getTo();

}