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

import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Properties;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.bean.def.IConstraint;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * 
 * @author Tom
 * @version $Revision$
 */
public class AItem<T> implements IItem<T>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 7142058494650831052L;

    @Attribute
    String name;
    @Attribute
    Type type;
    @Element(type = Constraint.class, required = false)
    IConstraint<T> constraints;

    T value;
    transient IItem<?> parent;
    transient byte[] origin;
    transient StringBuilder prefix = new StringBuilder("( ) ");
    transient boolean changed;
    private String description;

    static final int PREFIX = 1;
    static final String POSTFIX_QUESTION = ": ";
    
    /**
     * constructor
     */
    public AItem() {
    }

    /**
     * constructor
     * 
     * @param name
     * @param constraints
     * @param type
     * @param value
     */
    public AItem(String name, IConstraint<T> constraints, Type type, T value, String description) {
        super();
        this.name = name;
        this.type = type;
        this.value = value;
        this.description = description;

        initConstraints(constraints);
    }

    @SuppressWarnings("unchecked")
    protected void initConstraints(IConstraint<T> constraints) {
        if (constraints != null) {
            this.constraints = constraints;
        } else {
            Class<T> t = (Class<T>) (value != null ? value.getClass() : String.class);
            this.constraints = new Constraint<T>((Class<T>) t);
            this.constraints.setFormat(Constraint.createFormat(t));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IConstraint<T> getConstraints() {
        return constraints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Element(required=false)
    public T getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Element(required=false)
    public void setValue(T value) {
        if (constraints != null)
            constraints.check(name, value);
        this.value = value;
    }

    @Override
    public String ask() {
        String ask = "Please enter a " + constraints.getType().getSimpleName();
        if (constraints.getMinimum() != null || constraints.getMaximum() != null) {
            ask += " between " + constraints.getMinimum() != null ? constraints.getMinimum() : "<any>"
                + constraints.getMaximum() != null ? constraints.getMaximum() : "<any>";
        } else if (constraints.getAllowedValues() != null) {
            ask += " as one of: " + StringUtil.toString(constraints.getAllowedValues(), 60);
        }
        return ask + POSTFIX_QUESTION;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
        try {
            setValue((T) getConstraints().getFormat().parseObject(input));
            if (!Util.isEmpty(input))
                env.put(getName(), getValue());
            else
                env.remove(getName());
            changed = true;
        } catch (ParseException e) {
            ManagedException.forward(e);
        }
        return caller == this ? getParent() : this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChanged() {
        return changed;//ByteUtil.equals(ByteUtil.serialize(value), origin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEditable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPresentationPrefix() {
        if (isChanged())
            prefix.setCharAt(PREFIX, 'x');
        return prefix.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription(boolean full) {
        if (description == null)
            description = getConstraints() != null ? getConstraints().toString() : name;
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IItem getParent() {
        return parent;
    }

    /**
     * @param parent The {@link #parent} to set.
     */
    @Override
    public void setParent(IItem parent) {
        this.parent = parent;
    }

    public static String translate(String name) {
        return Terminal.translate(name);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getPresentationPrefix() + translate(name) + POSTFIX_QUESTION + value + "\n";
    }
}
