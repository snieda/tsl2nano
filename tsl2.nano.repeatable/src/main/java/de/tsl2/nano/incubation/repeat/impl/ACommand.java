/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 21.11.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.repeat.impl;

import java.io.Serializable;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.repeat.IChange;
import de.tsl2.nano.incubation.repeat.ICommand;

/**
 * simple command implementation to be used by a command manager
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public abstract class ACommand<CONTEXT> implements ICommand<CONTEXT>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1588956642653074758L;

    private static final Log LOG = LogFactory.getLog(ACommand.class);

    CONTEXT context;
    IChange[] changes;

	String name;

    /**
     * constructor
     * @param context
     * @param changes
     */
    public ACommand(String name, CONTEXT context, IChange... changes) {
        super();
		this.name = name;
        this.context = context;
        this.changes = changes;
    }

    @Override
    public void run() {
        StringBuilder info = new StringBuilder("\nRUN COMMAND ("
            + context + ")\n"
            + StringUtil.toFormattedString(changes, Integer.MAX_VALUE, true));
        LOG.info(info);
        runWith(changes);
    }

    @Override
    public String getName() {
    	return name;
    }
    
    @Override
    public CONTEXT getContext() {
        return context;
    }

    @Override
    public void setContext(CONTEXT context) {
        this.context = context;
    }
    
    @Override
    public void undo() {
        IChange[] reversChanges = new IChange[changes.length];
        for (int i = 0; i < reversChanges.length; i++) {
            reversChanges[i] = changes[i].revert();
        }
        runWith(reversChanges);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Util.toString(getClass(), context, changes);
    }
}
