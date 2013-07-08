/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 29, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.util.bean.ValueHolder;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public class ObservedValue<T> extends ValueHolder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -4037625774779685277L;

    EventController eventController;
    
    public ObservedValue(T object) {
        super(object);
        eventController = new EventController();
    }

    /**
     * @return Returns the eventController.
     */
    public EventController getChangeController() {
        return eventController;
    }

}
