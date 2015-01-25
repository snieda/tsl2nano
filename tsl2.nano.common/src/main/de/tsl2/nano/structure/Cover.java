/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Dec 7, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.structure;

import java.io.Serializable;

/**
 * Technical base definition to enhance a {@link #content} through this cover. Extending properties are defined by the
 * {@link #descriptor}. May be used as base for Wrappers, Pointers or Connections.
 * <p/>
 * Implementing all java-standard interfaces like {@link Serializable}, {@link Comparable}, {@link Cloneable} and the
 * methods {@link Object#hashCode()}, {@link Object#equals(Object)}, you may use this class to extend your class with
 * some properties (defined by the descriptor).
 * <p/>
 * An example use case would be to add a weight to your class, using that weight to be sorted and compared.
 * <p/>
 * TODO: create interfaces for 'Wrapper+Source' or 'Pointer+Destination', 'Delegator+Source', 'Extender+Base',
 * 'Hull+Core', 'Cover+Core'
 * <p/>
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Cover<T extends Serializable & Comparable<? super T>, D extends Comparable<? super D>> implements
        Comparable<Object>,
        Serializable,
        Cloneable {
    /** serialVersionUID */
    private static final long serialVersionUID = -2576925075573766918L;

    /** content */
    protected T content;
    /** description or extension */
    protected D descriptor;

    /**
     * constructor
     * 
     * @param core {@link #content}
     * @param descriptor {@link #descriptor}
     */
    public Cover(T core, D descriptor) {
        assert core != null;
        this.content = core;
        this.descriptor = descriptor;
    }

    /**
     * getContent
     * 
     * @return {@link #content}
     */
    public T getContent() {
        return content;
    }

    /**
     * @return Returns the {@link #descriptor}.
     */
    public D getDescriptor() {
        return descriptor;
    }

    /**
     * @param weight The {@link #descriptor} to set.
     */
    public void setDescriptor(D descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Object o) {
        return descriptor != null ? descriptor.compareTo(((Cover<T,D>)o).descriptor) : -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return content.hashCode() + 31 * (descriptor == null ? 0 : descriptor.hashCode());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        Cover<T, D> o2 = (Cover<T, D>) obj;
        return content.equals(o2.content) && compareTo(o2) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return content + ":" + descriptor;
    }
}
