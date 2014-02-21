/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 21.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;

import de.tsl2.nano.bean.BeanClass;

/**
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
@Default(value = DefaultType.FIELD, required = false)
public class AttributeExpression<T> extends AttributeDefinition<T> implements IValueExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8384604323167279662L;

    protected IValueExpression<T> valueExpression;
    /**
     * framework extensions can register own value-expression extensions. this extensions will be used on calling
     * {@link #setExpression(String)}. the extensions must have a constuctor with two arguments: Class declaringClass,
     * String expression.
     */
    @SuppressWarnings("rawtypes")
    protected static final Map<String, Class<? extends IValueExpression>> registeredExtensions =
        new HashMap<String, Class<? extends IValueExpression>>();

    static {
        //TODO: how to use IValueExpression.getExpressionPattern
        registerExpression(".*\\.*", PathExpression.class);
    }

    /**
     * constructor
     */
    protected AttributeExpression() {
        super();
    }

    /**
     * constructor
     * 
     * @param readAccessMethod
     */
    protected AttributeExpression(Method readAccessMethod) {
        super(readAccessMethod);
    }

    @Override
    public String getId() {
        return valueExpression != null ? valueExpression.toString() : super.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getType() {
        //once, the readAccessMethod should be initialized through getType()
        if (readAccessMethod == null)
            super.getType();
        return (Class<T>) (valueExpression != null ? valueExpression.getType() : super.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue(Object beanInstance) {
        return (T) (valueExpression != null ? valueExpression.getValue(beanInstance) : super.getValue(beanInstance));
    }

    @Override
    public String getExpressionPattern() {
//        return valueExpression != null ? valueExpression.getExpressionPattern() : null;
        //to avoid miss-understandings we don't return the value
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExpression() {
        return valueExpression != null ? valueExpression.getExpression() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExpression(String valueExpression) {
        /*
         * is it a special value like a value-path or a rule?
         */
        Class<? extends IValueExpression<T>> extensionClass =
            (Class<? extends IValueExpression<T>>) getExtension(valueExpression);
        IValueExpression<T> extension;
        if (extensionClass != null) {
            extension = BeanClass.createInstance(extensionClass, valueExpression);
        } else {
            extension = new ValueExpression<T>(valueExpression, getType());
        }
        setValueExpression(extension);
    }

    /**
     * setValueExpression
     * 
     * @param valueExpression new value expression
     */
    public void setValueExpression(IValueExpression<T> valueExpression) {
        this.valueExpression = valueExpression;
    }

    /**
     * see {@link #registeredExtensions}, {@link #getExtension(String)} and {@link #getBeanValue(Object, String)}.
     * 
     * @param attributeRegEx regular expression, defining a specialized attribute
     * @param extension type to handle the specialized attribute.
     */
    public static final void registerExpression(String attributeRegEx, Class<? extends IValueExpression> extension) {
        registeredExtensions.put(attributeRegEx, extension);
    }

    /**
     * getExtension
     * 
     * @param attributeName attribute name to check
     * @return registered extension or null - if standard
     */
    protected static Class<? extends IValueExpression> getExtension(String attributeName) {
        Set<String> regExs = registeredExtensions.keySet();
        for (String attrRegEx : regExs) {
            if (attributeName.matches(attrRegEx))
                return registeredExtensions.get(attrRegEx);
        }
        return null;
    }

    @Override
    public String toString() {
        return valueExpression != null ? valueExpression.toString() : super.toString();
    }
}
