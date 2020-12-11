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

import java.io.Serializable;

import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.historize.Volatile;

/**
 * resolves relations through a path to several beans/attributes
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class PathExpression<T> extends AbstractExpression<T> implements IValueExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -2355489779688683413L;

    transient String[] attributePath;

    /** performance optimizing */
    transient Volatile<T> v;

    /** attribute relation separator (like 'myattr1.relationattr.nextrelationattr' */
    public static final String PATH_SEPARATOR = ".";

    static {
        registerExpression(PathExpression.class);
    }
    public PathExpression() {
    }

    /**
     * constructor
     * 
     * @param declaringClass declaringClass of first attribute in chain
     * @param attributeChain attribute path separated by {@link #PATH_SEPARATOR}.
     */
    public PathExpression(Class<?> declaringClass, String attributeChain) {
        super(declaringClass, attributeChain, (Class<T>) Object.class);
        initDeserializing();
    }

    /**
     * constructor
     * 
     * @param declaringClass declaringClass of first attribute in chain
     * @param attributeChain attribute path separated by {@link #PATH_SEPARATOR}.
     * @param type
     */
    protected PathExpression(Class<?> declaringClass, Class<T> type, String... attributeChain) {
        super(declaringClass, StringUtil.concat(PATH_SEPARATOR.toCharArray(), attributeChain), type);
        this.attributePath = attributeChain;
    }

    /**
     * splitChain
     * 
     * @param attributePath
     * @return
     */
    public static String[] splitChain(String attributeChain) {
        return attributeChain.split("\\" + PATH_SEPARATOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue(Object instance) {
        if (v.expired()) {
            v.set((T) BeanClass.getValue(instance, attributePath));
        }
        return v.get();
    }

    @Override
    public void setValue(Object instance, T value) {
        Object v = value;
        for (int i = 0; i < attributePath.length - 1; i++) {
            v = BeanClass.getValue(v, attributePath[i]);
            if (v == null) {
                throw new IllegalStateException("couldn't set value " + value + " for attribute " + this
                    + ". please set a value for " + attributePath[i] + " first!");
            }
        }
        Bean.getBean(v).setValue(attributePath[attributePath.length - 1], value);
    }

    @Commit
    private void initDeserializing() {
        v = new Volatile<T>(ENV.get("cache.expire.milliseconds.pathexpression", 500));
        attributePath = splitChain(expression);
    }

    public static boolean isPath(String name) {
        return name.contains(PATH_SEPARATOR);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getExpressionPattern() {
        return "([\\w\\d]+[\\.\\(\\)\\[\\]]?)+";
    }

    @Override
    public String getName() {
        if (name == null) {
            name = expression.substring(expression.lastIndexOf(PATH_SEPARATOR) + 1);
        }
        return name;
    }

    /**
     * setName
     * 
     * @param name see {@link #name}
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }
}
