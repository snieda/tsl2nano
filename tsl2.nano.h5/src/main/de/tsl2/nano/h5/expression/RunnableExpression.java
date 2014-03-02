/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 25.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.expression;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.AbstractExpression;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.exception.ManagedException;
import de.tsl2.nano.execution.IPRunnable;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public abstract class RunnableExpression<T extends Serializable> extends AbstractExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 8147165150625339935L;

    /** runnable to be executed with expression */
    transient IPRunnable<T, Map<String, Object>> runnable;

    /** arguments for rule execution */
    transient Map<String, T> arguments;

    /** optional real attribute to set the value through this rule */
    String connectedAttribute;

    /**
     * constructor
     */
    public RunnableExpression() {
    }

    /**
     * constructor
     * 
     * @param declaringClass
     * @param expression
     * @param type
     */
    public RunnableExpression(Class<?> declaringClass, String expression, Class<T> type) {
        super(declaringClass, expression, type);
    }

    public IPRunnable<T, Map<String, Object>> getRunnable() {
        if (runnable == null)
            runnable = createRunnable();
        return runnable;
    }

    /**
     * createRunnable
     * 
     * @return new instance of desired {@link IPRunnable} implementation
     */
    protected abstract IPRunnable<T, Map<String, Object>> createRunnable();

    @Override
    public T getValue(Object beanInstance) {
        try {
            T result = (T) getRunnable().run(refreshArguments(beanInstance));
            if (connectedAttribute != null)
                Bean.getBean((Serializable) beanInstance).getAttribute(connectedAttribute).setValue(result);
            return result;
        } catch (final Exception e) {
            ManagedException.forward(new IllegalStateException("Execution of '" + getName()
                + "' with current arguments failed!", e));
            return null;
        }
    }

    protected Map<String, Object> refreshArguments(Object beanInstance) {
        if (arguments == null)
            arguments = new HashMap<String, T>();
        arguments.putAll((Map<String, ? extends T>) BeanUtil.toValueMap(beanInstance, false,
            true, true, getRunnable().getParameter().keySet().toArray(new String[0])));
        return (Map<String, Object>) arguments;
    }

    /**
     * @return Returns the arguments.
     */
    protected Map<String, T> getArguments() {
        return arguments;
    }

    /**
     * @param arguments The arguments to set.
     */
    protected void setArguments(Map<String, T> arguments) {
        this.arguments = arguments;
    }

    public void connectTo(String attributeOfParent) {
        connectedAttribute = attributeOfParent;
    }

    @Override
    public void setValue(Object instance, T value) {
        throw new UnsupportedOperationException();
    }
}
