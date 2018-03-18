/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 23.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.core;

/**
 * it's not really an exception but the information, that a process was successful finished. use this to control a
 * process inside a call stack.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Finished extends ManagedException {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public Finished(String message, Object... args) {
        super(null, false);
        localizedMessage = Messages.getFormattedString(message, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return localizedMessage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    /**
     * delegates to {@link #apply(String, Object...)}.
     */
    public static void apply() {
        apply("tsl2nano.process.finished", "tsl2nano.process", "tsl2nano.successful");
    }

    /**
     * apply a finished 'exception' to go through the call stack. on junit tests, you should embed this call into a
     * try-catch block.
     * 
     * @param msg message holding e.g. the result or the process id.
     * @param args optional arguments for the message msg.
     */
    public static void apply(String msg, Object... args) {
        throw new Finished(msg, args);
    }

}
