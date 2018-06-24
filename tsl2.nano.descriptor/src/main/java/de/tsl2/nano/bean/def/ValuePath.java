/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 16.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;

import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IValueAccess;
import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.util.CollectionUtil;

/**
 * resolves relations through a path to several beans/attributes
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
@Default(value = DefaultType.FIELD, required = false)
public class ValuePath<B, T> extends PathExpression<T> implements IValueAccess<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    transient B instance;
    transient EventController eventController;

    protected ValuePath() {
    }

    public ValuePath(B instance, String attributeChain) {
        this((Class<B>) instance.getClass(), instance,
            (Class<T>) Object.class, splitChain(attributeChain));
    }

    /**
     * constructor
     * 
     * @param declaringClass
     * @param attributePath
     * @param instance
     */
    protected ValuePath(Class<B> declaringClass, B instance, Class<T> type, String... attributeChain) {
        super(declaringClass, type, attributeChain);
        this.instance = instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue() {
        return (T) BeanClass.getValue(instance, attributePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(T object) {
        Object lastInstance;
        if (attributePath.length > 1) {
            lastInstance =
                BeanClass.getValue(instance,
                    CollectionUtil.copyOfRange(attributePath, 0, attributePath.length - 2, String[].class));
        } else {
            lastInstance = instance;
        }
        BeanAttribute.getBeanAttribute(BeanClass.getDefiningClass(lastInstance.getClass()),
            attributePath[attributePath.length - 1]).setValue(lastInstance, object);
    }

    /**
     * @param instance The instance to set.
     */
    protected void setInstance(B instance) {
        this.instance = instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventController changeHandler() {
        if (eventController == null) {
            eventController = new EventController();
        }
        return eventController;
    }
}
