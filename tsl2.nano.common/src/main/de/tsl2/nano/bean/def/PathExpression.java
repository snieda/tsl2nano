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
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.BeanClass;
import de.tsl2.nano.util.StringUtil;

/**
 * resolves relations through a path to several beans/attributes
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
@Default(value = DefaultType.FIELD, required = false)
public class PathExpression<T> extends AbstractExpression<T> implements IValueExpression<T> {
    transient String[] attributePath;

    /** attribute relation separator (like 'myattr1.relationattr.nextrelationattr' */
    public static final String PATH_SEPARATOR = ".";

    protected PathExpression() {
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
        return (T) BeanClass.getValue(instance, attributePath);
    }

    @Commit
    private void initDeserializing() {
        attributePath = splitChain(expression);
    }

    public static boolean isPath(String name) {
        return name.contains(PATH_SEPARATOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExpressionPattern() {
        return ".*\\.*";
    }
}
