/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 15, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.core.messaging;

import java.util.EventObject;

/**
 * standard change event to be fired on value changes. used by {@link EventController} and handled by {@link IListener}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ChangeEvent extends EventObject {
    /** serialVersionUID */
    private static final long serialVersionUID = -3010284121353463402L;
    /** all listeners will be called before value change - then it is false - and after value change - then it is true */
    public boolean hasChanged;
    /**
     * default: false. if set to true, the call of listeners will be stopped, if the value was not already changed, the
     * change will be canceled
     */
    public boolean breakEvent;
    /** previous value */
    protected Object oldValue;
    /** new value */
    public Object newValue;

    /**
     * constructor
     * 
     * @param source {@link #getSource()}
     * @param hasChanged {@link #hasChanged}
     * @param breakEvent {@link #breakEvent}
     * @param oldValue {@link #oldValue}
     * @param newValue {@link #newValue}
     */
    public ChangeEvent(Object source, boolean hasChanged, boolean breakEvent, Object oldValue, Object newValue) {
        super(source);
        this.hasChanged = hasChanged;
        this.breakEvent = breakEvent;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ChangeEvent[" + source
            + ", "
            + (hasChanged ? "changed, " : "")
            + (breakEvent ? "stopped, " : "")
            + oldValue
            + " ==> "
            + newValue
            + "]";
    }

    /**
     * convenience to create the event itself
     * 
     * @param source {@link ChangeEvent#getSource()}
     * @param oldValue {@link ChangeEvent#oldValue}
     * @param newValue {@link ChangeEvent#newValue}
     * @param changed {@link ChangeEvent#hasChanged}
     * @return new event instance
     */
    public static ChangeEvent createEvent(Object source, Object oldValue, Object newValue, boolean changed) {
        return new ChangeEvent(source, changed, false, oldValue, newValue);
    }
}
