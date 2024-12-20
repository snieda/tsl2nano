/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.terminal;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Properties;

import de.tsl2.nano.action.IConstraint;
import static de.tsl2.nano.core.util.CLI.*;

import de.tsl2.nano.terminal.TextTerminal.Frame;
import de.tsl2.nano.vnet.workflow.Condition;

/**
 * root definition for the terminal application. all items are extensions of it. only the types {@link Type} are
 * defined. each item can have the following states:
 * 
 * <pre>
 * 
 * - unchangeable (Option)
 * - duty and changed
 * - duty and not changed
 * - is running
 * - was executed
 * </pre>
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public interface IItem<T> {
    /** the items name */
    String getName();

    /** one of {@link Type} */
    Type getType();

    /** input constraints. defines a value range and perhaps an input mask. */
    IConstraint<T> getConstraints();

    /** optional workflow condition. if condition evaluates 'false', this item will be invisible in it's parent list. */
    Condition getCondition();

    /** the items current value */
    T getValue();

    /** will be called after user input on this item */
    void setValue(T value);

    /** provides a question text for the user */
    String ask(Properties env);

    /** will be called after user input. @returns the next item to provide */
    IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env);

    /** @return true, if item was edited */
    boolean isChanged();

    /**
     * @return true, if this item should be editable by user input. {@link Type#Action} and {@link Type#Option} are not
     *         editable. isEditable
     * @return
     */
    boolean isEditable();

    /** items parent */
    IContainer getParent();

    /** used by framework */
    void setParent(IContainer parent);

    /** shows informations like edited, duty, type of input. */
    String getPresentationPrefix();

    /** item description to be used as user help */
    String getDescription(Properties env, boolean full);
    
    /** optional, defines an own frame style */
    Frame getStyle();
    
    /** optional, defines an own frame foreground color */
    Color getFgColor();
    
    /** optional, defines an own frame background color */
    Color getBgColor();
    
    enum Type implements Serializable {
        Input, Option, Container, Selector, Action;
    }
}

