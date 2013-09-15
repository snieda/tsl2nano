/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 13.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package org.apache.commons.logging;

/**
 * To avoid any dependencies to the apache commons log library (not usable on android), we put the logging interface
 * inside the nano framework.
 * 
 * @author ts
 * @version $Revision$
 */
public interface Log {
    boolean isDebugEnabled();
    boolean isErrorEnabled();
    boolean isFatalEnabled();
    boolean isInfoEnabled();
    boolean isTraceEnabled();
    boolean isWarnEnabled();
    void trace(java.lang.Object arg0);
    void trace(java.lang.Object arg0, java.lang.Throwable arg1);
    void debug(java.lang.Object arg0);
    void debug(java.lang.Object arg0, java.lang.Throwable arg1);
    void info(java.lang.Object arg0);
    void info(java.lang.Object arg0, java.lang.Throwable arg1);
    void warn(java.lang.Object arg0);
    void warn(java.lang.Object arg0, java.lang.Throwable arg1);
    void error(java.lang.Object arg0);
    void error(java.lang.Object arg0, java.lang.Throwable arg1);
    void fatal(java.lang.Object arg0);
    void fatal(java.lang.Object arg0, java.lang.Throwable arg1);
}
