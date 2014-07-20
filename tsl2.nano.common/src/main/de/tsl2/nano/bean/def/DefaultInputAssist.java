/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 01.07.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.Util;

/**
 * Default implementation of {@link IInputAssist}. tries to get the available values from jpa. will be used for all
 * persistable bean-attributes.
 * 
 * @author Tom
 * @version $Revision$
 */
public class DefaultInputAssist<T> implements IInputAssist<T> {
    private static final Log LOG = LogFactory.getLog(DefaultInputAssist.class);

    /** (parent) attribute using this input assist */
    transient IAttributeDefinition<T> attribute;
    
    /** as simple-xml seems to have problems to save a bean without properties, we set an empty 'workaround' property. */
    @Attribute
    boolean emptyWorkaround = true;
    /**
     * constructor
     */
    protected DefaultInputAssist() {
        super();
    }

    /**
     * constructor
     */
    public DefaultInputAssist(IAttributeDefinition<T> attribute) {
        this.attribute = attribute;
    }

    /**
     * @param attribute The attribute to set.
     */
    public void setAttribute(IAttributeDefinition<T> attribute) {
        this.attribute = attribute;
    }

    @Override
    public Collection<?> availableValues(Object prefix) {
        String input = Util.asString(prefix) + "*";

        if (attribute.getFormat() instanceof ValueExpressionFormat) {
            ValueExpression<T> ve = ((ValueExpressionFormat) attribute.getFormat()).getValueExpression();
            T exampleBean = ve.createExampleBean(input);
            Collection<T> values = BeanContainer.instance().getBeansByExample(exampleBean, true);
            //fire value change event to all dependent listeners to refresh their values
            Collection<String> result = new ArrayList<String>(values.size());
            if (values.size() > 0) {
                for (T t : values) {
                    result.add(ve.to(t));
                }
            }
            return result;
        }
        return new ArrayList<T>(0);
    }

//    @Override
//    public EventController changeHandler() {
//        return changeHandler;
//    }
}
