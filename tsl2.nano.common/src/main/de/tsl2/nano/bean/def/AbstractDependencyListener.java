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

import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;

/**
 * standard dependency {@link IListener} to be added to the {@link EventController} of an {@link AttributeDefinition}.
 * 
 * @author Tom
 * @version $Revision$
 */
public abstract class AbstractDependencyListener<T> implements IListener<T> {
    /** full attribute-definition object - transient to be serializable */
    transient protected AttributeDefinition<T> attribute;
    /** attributes id - to be serialized/deserialized - to evaluate the full object after de-serialization */
    protected String attributeID;

    /**
     * constructor
     */
    public AbstractDependencyListener() {
    }

    /**
     * constructor
     * @param attribute
     */
    public AbstractDependencyListener(AttributeDefinition<T> attribute) {
        super();
        this.attribute = attribute;
        this.attributeID = attribute.getId();
    }
    
    /**
     * setAttribute
     * @param attribute
     */
    void setAttribute(AttributeDefinition<T> attribute) {
        this.attribute = attribute;
    }
}
