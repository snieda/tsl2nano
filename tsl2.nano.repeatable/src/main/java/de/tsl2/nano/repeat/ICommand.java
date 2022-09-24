/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 21.11.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.repeat;

/**
 * command holding it's context and optional changes done by execution - to be undo-able or to be used by macros.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public interface ICommand<CONTEXT> extends Runnable {
	String getName();
    /**
     * @return the context on which the command will be executed
     */
    CONTEXT getContext();

    /**
     * @param context new context to be set.should only be called inside the framework. if a change-item is null, the
     *            whole context will be set.
     */
    void setContext(CONTEXT context);

    /**
     * undo this command, if already done (through {@link Runnable#run()}).
     */
    void undo();

    /**
     * if the implementation provides an undo-mechanism, the {@link #undo()} should call this method with a stored
     * 'change' information.
     * 
     * @param change to be done on CONTEXT while execution.
     */
    void runWith(IChange... changes);
}
