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
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

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

    public ExpressionDescriptor(Class declaringClass) {
        this(declaringClass, null);
    }
    
    /**
     * constructor
     */
    public ExpressionDescriptor(Class declaringClass, String expression) {
        this.declaringClass = declaringClass;
        this.expression = expression;
    }

    @Override
    public String getExpressionPattern() {
        return null;
    }

    public static String getName(String url) {
        return FileUtil.getValidFileName(!Util.isEmpty(url) ? StringUtil.substring(url, "://", "/") : "[undefined]");
    }
    @Override
    public String getName() {
        if (name == null && expression != null) {
            name = getName(super.getName());
        }
        return name;
    }

    @Override
    public T getValue(Object instance) {
        return null;
    }

    @Override
    public void setValue(Object instance, T value) {

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

    public static boolean isSVG(String response) {
        return response != null && response.contains("<svg");
    }

    public static boolean isAudio(String response) {
        return response != null && response.contains("<audio");
    }

    public static boolean isVideo(String response) {
        return response != null && response.contains("<video");
    }
}
