/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 06.07.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.h5.configuration;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.AbstractExpression;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.NetUtil;

/**
 * describes an expression through its expression-pattern. tries to identifiy the type of expression and creates an
 * instance of {@link AbstractExpression} through {@link #toInstance()}. provides some helpers to identify a string
 * value (see {@link #isHtml(String)}, {@link #isURL(String)}, {@link #isJSON(String)}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class ExpressionDescriptor<T> extends AbstractExpression<T> {

    public ExpressionDescriptor() {
    }

    /**
     * constructor
     */
    public ExpressionDescriptor(Class declaringClass) {
        this.declaringClass = declaringClass;
    }

    @Override
    public String getExpressionPattern() {
        return null;
    }

    @Override
    public T getValue(Object instance) {
        return null;
    }

    @Override
    public void setValue(Object instance, T value) {

    }

    public void setDeclaringClass(Class cls) {
        this.declaringClass = cls;
    }

    @SuppressWarnings("rawtypes")
    AbstractExpression toInstance() {
        Class<? extends AbstractExpression> impl = AbstractExpression.getImplementation(getExpression());
        if (impl == null)
            throw new IllegalStateException("no implementation found for pattern: " + getExpression());
        return BeanUtil.copyValues(this, BeanClass.createInstance(impl));
    }

    public static boolean isHtml(String response) {
        return response != null && response.contains("<html");
    }

    public static boolean isJSON(String response) {
        return response != null && response.matches("\\{\\s*\\w+\\s*\\:\\w+\\,");
    }

    public static boolean isURL(String response) {
        return response != null && NetUtil.isURL(response);
    }
}
