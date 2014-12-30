/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import de.tsl2.nano.bean.def.IConstraint;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public class Input<T> extends AItem<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3385405996942538232L;

    /**
     * constructor
     */
    public Input() {
        super();
        type = Type.Input;
    }

    public Input(String name, T value, String description) {
        this(name, null, value, description);
    }
    
    public Input(String name, IConstraint<T> constraints, T value, String description) {
        this(name, constraints, value, description, true);
    }
    
    /**
     * constructor
     * @param name
     * @param constraints
     * @param type
     * @param value
     */
    public Input(String name, IConstraint<T> constraints, T value, String description, boolean nullable) {
        super(name, constraints, Type.Input, value, description);
        getConstraints().setNullable(nullable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (getConstraints() != null && !constraints.isNullable())
            prefix.setCharAt(PREFIX, '§');
        return super.toString();
    }
}
