/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 08.05.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.h5.websocket;

import de.tsl2.nano.messaging.ChangeEvent;

/**
 * Websocket event holding a mouse click position
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class WSEvent extends ChangeEvent {
    /** serialVersionUID */
    private static final long serialVersionUID = 6151520283676866400L;
    long timeStamp;
    int clickX, clickY;

    public WSEvent(Object source, Object oldValue, Object newValue, int clickX, int clickY) {
        super(source, true, false, oldValue, newValue);
        this.clickX = clickX;
        this.clickY = clickY;
        timeStamp = System.currentTimeMillis();
    }

    /**
     * @return Returns the clickX.
     */
    public int getClickX() {
        return clickX;
    }

    /**
     * @return Returns the clickY.
     */
    public int getClickY() {
        return clickY;
    }

    /**
     * getTimeStamp
     * @return
     */
    public long getTimeStamp() {
        return timeStamp;
    }
}
