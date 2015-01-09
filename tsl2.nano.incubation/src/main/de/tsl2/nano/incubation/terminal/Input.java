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
import java.util.Properties;

import de.tsl2.nano.bean.def.IConstraint;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

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

    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
        if (Util.isEmpty(input))
            input = StringUtil.toString(getValue());
        return super.react(caller, input, in, out, env);
    }
    
    @Override
    public String ask() {
        return StringUtil.substring(super.ask(), null, POSTFIX_QUESTION) + " (" + getValue() + ")" + POSTFIX_QUESTION;
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
