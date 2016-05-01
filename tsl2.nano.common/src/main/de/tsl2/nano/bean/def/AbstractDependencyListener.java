/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 11.07.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Transient;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.messaging.IStatefulListener;

/**
 * standard dependency {@link IListener} to be added to the {@link EventController} of an {@link AttributeDefinition}.
 * 
 * @author Tom
 * @version $Revision$
 */
public abstract class AbstractDependencyListener<T, E> implements IStatefulListener<E>, Serializable, Cloneable {
    /** serialVersionUID */
    private static final long serialVersionUID = -4105265981459378940L;
    /** full attribute-definition object - transient to be serializable */
    @Transient
    transient protected IAttributeDefinition<T> attribute;
    /** attributes id - to be serialized/deserialized - to evaluate the full object after de-serialization */
    @Attribute(required=false)
    protected String attributeID;
    /**
     * the attributes property name to be changed. these names vary on the target system. on an html target system it
     * would be an input-tags attribute like its 'value'. the extension of this class will handle this value and its
     * default, if null.
     */
    @Attribute(required=false)
    protected String propertyName;

    /** copy of base instance holding all further changes */
    transient protected Object changes;

    /**
     * constructor
     */
    public AbstractDependencyListener() {
    }

    public AbstractDependencyListener(IAttributeDefinition<T> attribute) {
        this(attribute, null);
    }

    /**
     * constructor
     * 
     * @param attribute
     */
    public AbstractDependencyListener(IAttributeDefinition<T> attribute, String propertyName) {
        super();
        setAttribute(attribute);
        this.propertyName = propertyName;
    }

    protected IAttributeDefinition<T> getAttribute() {
        return attribute;
    }
    
    /**
     * setAttribute
     * 
     * @param attribute
     */
    protected void setAttribute(IAttributeDefinition<T> attribute) {
        this.attribute = attribute;
        this.attributeID = attribute.getId();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getStateObject() {
        return changes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStateObject(Object changeObject) {
        changes = changeObject;
    }

    @Override
    public void reset() {
        attribute = null;
        setStateObject(null);
    }
    
    @Override
    public String toString() {
        return Util.toString(getClass(), "attributeID=" + attributeID);
    }
}
