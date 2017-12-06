/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 21.11.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.repeat.impl;

import java.io.Serializable;

import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.repeat.IChange;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class AChange implements IChange, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -8864405104786450647L;
    Object item;
    Object old;
    Object neW;

    /**
     * constructor
     * 
     * @param item
     * @param old
     * @param neW
     */
    public AChange(Object item, Object old, Object neW) {
        super();
        this.item = item;
        this.old = old;
        this.neW = neW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getItem() {
        return item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getOld() {
        return old;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getNew() {
        return neW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IChange revert() {
        return new AChange(item, neW, old);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Util.asString(item) + ": " + Util.asString(old) + " --> " + Util.asString(neW);
    }
}
