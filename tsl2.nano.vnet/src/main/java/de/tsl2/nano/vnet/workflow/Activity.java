/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 20.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.vnet.workflow;

import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.core.cls.PrivateAccessor;

/**
 * 
 * @param <S> input type for condition and expression
 * @param <T> output type for action result
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
@SuppressWarnings("rawtypes")
@Default(value = DefaultType.FIELD, required = false)
public abstract class Activity<S, T> extends CommonAction<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -4293912643879238202L;

    @Element(required=true, type=String.class)
    protected S condition;
    @Element(required=true, type=String.class)
    protected S expression;
    
    protected transient Map parameter;
    
    /**
     * constructor for xml-deserialization
     */
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
        this.parameter = parameter;
        //TODO: check for natural order of values
        setParameter(parameter.values().toArray());
        return activate();
    }
    
    @Attribute
    public String getName() {
        return getShortDescription();
    }
    
    @SuppressWarnings("unchecked")
    @Attribute
    protected void setName(String name) {
        new PrivateAccessor(this).set("shortDescription", name);
    }

    public S getCondition() {
		return condition;
	}
    
    @Override
    public String toString() {
        return getShortDescription();
    }
}
