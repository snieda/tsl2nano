/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Nov 16, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.log;

import java.text.MessageFormat;

/**
 * fast messsage format using {@link MessageFormat} with static arguments
 * @author Thomas Schneider
 * @version $Revision$ 
 */
public class MsgFormat {
    /** used as temporarily buffer for {@link #msgFormat} calls (--> performance) */
    final StringBuffer msgBuffer = new StringBuffer();
    /**
     * used for formatted logging using outputformat as pattern. see {@link #log(Class, State, Object, Throwable)}. (-->
     * performance)
     */
    MessageFormat msgFormat;

    /**
     * constructor
     * @param pattern
     * @param argCount
     */
    public MsgFormat(String pattern) {
        msgFormat = new MessageFormat(pattern);
    }
    
    public void applyPattern(String pattern) {
        msgFormat.applyPattern(pattern);
    }
    
    /**
     * format
     * @param args
     * @return
     */
    public String format(Object... args) {
        msgBuffer.setLength(0);
        return msgFormat.format(args, msgBuffer, null).toString();
    }
}
