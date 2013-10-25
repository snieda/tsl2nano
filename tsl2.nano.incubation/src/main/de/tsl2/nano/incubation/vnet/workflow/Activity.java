/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 20.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.vnet.workflow;

import java.util.Map;

import de.tsl2.nano.action.CommonAction;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public abstract class Activity<S, T> extends CommonAction<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -4293912643879238202L;

    protected S condition;
    protected S expression;
    
    public Activity() {
    }

    /**
     * constructor
     * @param condition
     * @param expression
     */
    public Activity(String name, S condition, S expression) {
        super(name, name, name);
        this.condition = condition;
        this.expression = expression;
    }
    
    public abstract boolean canActivate(Map parameter);
    
    public T activate(Map parameter) {
        //TODO: check for natural order of values
        setParameter(parameter.values().toArray());
        return activate();
    }
}
