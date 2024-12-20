/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.terminal.item;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.simpleframework.xml.Element;

import de.tsl2.nano.action.IConstraint;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.terminal.IContainer;
import de.tsl2.nano.terminal.IItem;

/**
 * 
 * @author Tom
 * @version $Revision$
 */
public class Option<T> extends AItem<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 7608860141435727313L;

    @Element
    T defaultValue;

    /**
     * constructor
     */
    public Option() {
        super();
        type = Type.Option;
    }

    public Option(IContainer<?> parent, String name, IConstraint<T> constraints, T defaultValue, String description) {
        this(parent, name, constraints, defaultValue, true, description);
    }

    /**
     * constructor
     * 
     * @param name
     * @param constraints
     * @param type
     * @param value
     */
    public Option(IContainer<?> parent, String name, IConstraint<T> constraints, T defaultValue, boolean active, String description) {
        super(name, constraints, Type.Option, defaultValue, description);
        this.parent = parent;
        this.defaultValue = defaultValue;
        if (!active) {
            this.value = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
        if (!Util.isEmpty(input)) {
            env.put(getName(), getValue());
        } else {
            env.remove(getName());
        }
        changed = !changed;
        return caller == this ? getParent().next(in, out, env) : caller;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPresentationPrefix() {
        prefix.setCharAt(PREFIX, changed ? 'v' : 'o');
        return super.getPresentationPrefix();
    }
}
