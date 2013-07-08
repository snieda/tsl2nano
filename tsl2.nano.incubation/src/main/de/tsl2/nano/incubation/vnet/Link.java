/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Dec 7, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet;

import java.io.Serializable;

/**
 * Technical base definition to link an item to a {@link #getDestination()}. The link properties are defined by the
 * {@link #descriptor}. May be used as base for Wrappers, Pointers or Connections.
 * <p/>
 * A source item will hold this link to the destination. Implementing all java-standard interfaces like
 * {@link Serializable}, {@link Comparable}, {@link Cloneable} and the methods {@link Object#hashCode()},
 * {@link Object#equals(Object)}, you may use this class to extend your class with some properties (defined by the
 * descriptor).
 * <p/>
 * An example use case would be to add a weight to your class, using that weight to be sorted and compared.
 * <p/>
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Link<T extends Serializable & Comparable<? super T>, D extends Comparable<? super D>> extends Cover<T, D> {
    /** serialVersionUID */
    private static final long serialVersionUID = -2576925075573766918L;

    /**
     * constructor
     * 
     * @param destination {@link #destination}
     * @param descriptor {@link #descriptor}
     */
    public Link(T destination, D descriptor) {
        super(destination, descriptor);
    }

    /**
     * @return Returns the destination.
     */
    public T getDestination() {
        return content;
    }

    /**
     * @param destination The destination to set.
     */
    public void setDestination(T destination) {
        assert destination != null;
        this.content = destination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "={" + descriptor + "}=> " + content;
    }
}
