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
     * the attributes property name to be changed. these names vary on the target system. on an html target system it
     * would be an input-tags attribute like its 'value'. the extension of this class will handle this value and its
     * default, if null.
     */
    protected String propertyName;

    /**
     * constructor
     */
    public AbstractDependencyListener() {
    }

    public AbstractDependencyListener(AttributeDefinition<T> attribute) {
        this(attribute, null);
    }

    /**
     * constructor
     * 
     * @param attribute
     */
    public AbstractDependencyListener(AttributeDefinition<T> attribute, String propertyName) {
        super();
        this.attribute = attribute;
        this.attributeID = attribute.getId();
        this.propertyName = propertyName;
    }

    /**
     * setAttribute
     * 
     * @param attribute
     */
    void setAttribute(AttributeDefinition<T> attribute) {
        this.attribute = attribute;
    }
}
