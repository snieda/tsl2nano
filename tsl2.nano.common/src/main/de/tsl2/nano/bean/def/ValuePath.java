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

import java.lang.reflect.Method;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;

import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.BeanClass;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.messaging.EventController;

/**
 * resolves relations through a path to several beans/attributes
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
@Default(value = DefaultType.FIELD, required = false)
public class ValuePath<B, T> extends PathExpression<T> implements IValueAccess<T> {
    transient B instance;
    transient EventController eventController;

    /** attribute relation separator (like 'myattr1.relationattr.nextrelationattr' */
    public static final String PATH_SEPARATOR = ".";

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
        if (attributePath.length > 1)
            lastInstance =
                BeanClass.getValue(instance,
                    CollectionUtil.copyOfRange(attributePath, 0, attributePath.length - 2, String[].class));
        else
            lastInstance = instance;
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
        if (eventController == null)
            eventController = new EventController();
        return eventController;
    }

    static Method getReadAccessMethod() {
        try {
            return ValuePath.class.getMethod(ATTR_VALUE, new Class[0]);
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }
}
