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

/**
 * functions of an application session.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface ISession {
    /** @return identifier of this session - perhaps the inetadress */
    Object getId();

    /** @return user authorization implementation */
    Object getUserAuthorization();
    
    /** @return sessions classloader */
    ClassLoader getSessionClassLoader();

    /** @return sessions exception handler */
    UncaughtExceptionHandler getExceptionHandler();

    /** @return current life duration */
    long getDuration();

    /** @return main application instance/server */
    Main getApplication();

    /** closes this session */
    void close();

    /** sets the user authorization */
    void setUserAuthorization(Object authorization);
}
