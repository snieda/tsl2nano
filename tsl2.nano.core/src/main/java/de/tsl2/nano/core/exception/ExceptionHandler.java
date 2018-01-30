/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 28.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core.exception;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * simple exception handler to be used by several threads.
 * 
 * @author Tom
 * @version $Revision$
 */
public class ExceptionHandler implements UncaughtExceptionHandler {
    protected List<Throwable> exceptions;

    /**
     * constructor
     */
    public ExceptionHandler() {
        this(true);
    }

    /**
     * constructor
     * 
     * @param concurrent true to be useable through concurrent threads
     */
    public ExceptionHandler(boolean concurrent) {
        super();
        exceptions = new LinkedList<Throwable>();
        if (concurrent) {
            exceptions = Collections.synchronizedList(exceptions);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        //TODO: eval the sessions thread
        exceptions.add(e);
    }

    /**
     * getExceptions
     * 
     * @return last exceptions
     */
    public List<Throwable> getExceptions() {
        return new ArrayList<Throwable>(exceptions);
    }

    /**
     * clears all exceptions and returns a copy of the last exception list.
     * @return
     */
    public List<Throwable> clearExceptions() {
        List<Throwable> e = getExceptions();
        exceptions.clear();
        return e;
    }

    public boolean hasExceptions() {
        return exceptions.size() > 0;
    }
}
