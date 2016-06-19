/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 08.04.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.core;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;

/**
 * functions of an application session.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface ISession<WORK> {
    /** @return identifier of this session - perhaps the inetadress + user */
    Object getId();

    /** @return non-unique context data */
    Object getContext();
    
    /** @return user authorization implementation */
    Object getUserAuthorization();
    
    /** @return sessions classloader */
    ClassLoader getSessionClassLoader();

    /** @return sessions exception handler */
    UncaughtExceptionHandler getExceptionHandler();

    /** @return current life duration */
    long getDuration();

    /** @return last session access */
    long getLastAccess();

    /** checks for closed, expired, done, empty, etc...if check fails, it throws an exception or returns false. */
    boolean check(long timeout, boolean throwException);
    
    /** @return main application instance/server */
    Main getApplication();

    /** closes this session */
    void close();

    /** sets the user authorization */
    void setUserAuthorization(Object authorization);
    
    /** return the websocket-server port for this session */
    int getWebsocketPort();
    
    /**
     * getNavigationStack
     * @return array of working objects in the sessions stack.
     */
    WORK[] getNavigationStack();
    
    /** return the current working object */
    WORK getWorkingObject();
}
