/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 07.04.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.action;

import java.io.Serializable;

import org.simpleframework.xml.Attribute;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public class Parameter<T> implements Serializable {
    private static final long serialVersionUID = -7323462415048503940L;

    @Attribute
    String name;
    IConstraint<T> constraint;
    transient T value;
    
    /**
     * constructor
     */
    public Parameter() {
    }

    /**
     * constructor
     * @param name
     * @param constraint
     * @param value
     */
    public Parameter(String name, IConstraint<T> constraint, T value) {
        super();
        this.name = name;
        this.constraint = constraint;
        this.value = value;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the constraint.
     */
    public IConstraint<T> getConstraint() {
        return constraint;
    }

    /**
     * @param constraint The constraint to set.
     */
    public void setConstraint(IConstraint<T> constraint) {
        this.constraint = constraint;
    }

    /**
     * @return Returns the value.
     */
    public T getValue() {
        return value;
    }

    /**
     * @param value The value to set.
     */
    public void setValue(T value) {
        if (constraint != null) {
            constraint.check(name, value);
            if (value == null && constraint.getDefault() != null) {
                this.value = constraint.getDefault();
                return;
            }
        }
        this.value = value;
    }
    
    @Override
    public String toString() {
        return name + ": " + (value != null ? value.toString() : String.valueOf(constraint));
    }
}
