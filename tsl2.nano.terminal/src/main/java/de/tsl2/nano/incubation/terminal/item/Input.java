/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal.item;

import java.io.PrintStream;
import java.io.InputStream;
import java.util.Properties;

import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.IConstraint;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.terminal.IItem;

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
    public Input(String name, T value, String description, boolean nullable) {
        this(name, null, value, description);
        getConstraints().setNullable(nullable);
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
        initDeserialization();
    }

    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
        if (Util.isEmpty(input)) {
            input = StringUtil.toString(getValue());
        }
        return super.react(caller, input, in, out, env);
    }
    
    @Override
    public String ask(Properties env) {
        return StringUtil.substring(super.ask(env), null, POSTFIX_QUESTION) + " (" + getValue() + ")" + POSTFIX_QUESTION;
    }

    @Override
    @Commit
    protected void initDeserialization() {
        super.initDeserialization();
        if (!getConstraints().isNullable()) {
            prefix.setCharAt(PREFIX, '§');
        } else {
            prefix.setCharAt(PREFIX, '*');
        }
    }
}
