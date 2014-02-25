/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 29.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.execution;

import java.io.Serializable;

/**
 * combines the two interfaces {@link ICRunnable} and {@link Runnable} to implement {@link ICRunnable}s usable by
 * {@link Thread}s.
 * 
 * @author Tom
 * @version $Revision$
 */
public abstract class ARunnable<CONTEXT extends Serializable> implements ICRunnable<CONTEXT>, Runnable {
    CONTEXT context;
    Object[] args;
    
    /**
     * constructor
     * @param context
     * @param args
     */
    public ARunnable(CONTEXT context, Object[] args) {
        super();
        this.context = context;
        this.args = args;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        context = run(context, args);
    }
    public void setArgs(Object[] args) {
        this.args = args;
    }
    public CONTEXT getResult() {
        return context;
    }
}
