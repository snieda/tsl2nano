/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Dec 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.vnet;

import de.tsl2.nano.core.messaging.IListener;

/**
 * On notifying all listeners about a new {@link Notification}, the listener will first be checked, if its
 * {@link ILocatable#getPath()} fits the notifcations path expression.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class NotificationController extends ThreadingEventController {
    
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public void notify(Notification n) {
            fireEvent(n);
    }

    @Override
    public void handle(IListener l, Object e) {
        if (((Notification) e).notify((ILocatable) l)) {
            super.handle(l, e);
        }
    }
}
