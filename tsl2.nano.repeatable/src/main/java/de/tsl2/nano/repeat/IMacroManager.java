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
 * simple macro manager definition
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public interface IMacroManager<CONTEXT> {
    /**
     * @param id macro identifier to be used later for {@link #play(String, Object)}. to start recording, the id must
     *            not be null! internally, the invoking will be done without an id (=null) - using the id from first
     *            call.
     * @param macros one or more macro to record.
     */
    void record(String id, ICommand<CONTEXT>... macros);

    /**
     * @return true, if {@link #record(String)} was called and {@link #stop()} was not called yet after.
     */
    boolean isRecording();

    /**
     * stops the current recording. can only be called, if {@link #record(String, ICommand...)} was called before
     * 
     * @return count of recorded items
     */
    int stop();

    /**
     * @param context context to do the play() on
     * @return count of items changed.
     */
    int play(String id, CONTEXT context);
}
