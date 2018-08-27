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
import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.AbstractExpression;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.execution.VolatileResult;

/**
 * base for all expression runners
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public abstract class RunnableExpression<T extends Serializable> extends AbstractExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 8147165150625339935L;

    /** arguments for rule execution */
    transient Map<String, Object> arguments;

    /**
     * runnable to be executed with expression. if the run() method is currently running, access to the run() method
     * should be blocked
     */
    transient VolatileResult<T> result;
    /** optional real attribute (result-attribute) to set the value through this rule result */
    String connectedAttribute;

    private transient boolean isRunning;

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

    public VolatileResult<T> getResult() {
        if (result == null) {
            result = new VolatileResult<T>(ENV.get("cache.expire.milliseconds.pathexpression", 300), createRunnable());
        }
        return result;
    }

    @Override
    public String getName() {
        if (name == null)
            name = getResult().getName();
        return name;
    }

    /**
     * createRunnable
     * 
     * @return new instance of desired {@link IPRunnable} implementation
     */
    protected abstract IPRunnable<T, Map<String, Object>> createRunnable();

    @Override
    public synchronized T getValue(Object beanInstance) {
        if (!isRunning) {
            isRunning = true;
            try {
                T result = getResult().get(refreshArguments(beanInstance));
                if (connectedAttribute != null) {
                    Bean.getBean((Serializable) beanInstance).getAttribute(connectedAttribute).setValue(result);
                }
                return result;
            } catch (final Exception e) {
                ManagedException.forward(new IllegalStateException("Execution of '" + getName()
                    + "' with current arguments failed: " + e.getLocalizedMessage(), e));
                return null;
            } finally {
                isRunning = false;
            }
        } else {
            return getResult().get();
        }

    }

    /**
     * calls {@link BeanUtil#toValueMap(Object)} to create a map holding all values of the given bean instance
     * 
     * @param beanInstance to be 'value'-mapped
     * @return map holding all values of the given bean instance
     */
    protected Map<String, Object> refreshArguments(Object beanInstance) {
        if (arguments == null) {
            arguments = new HashMap<String, Object>();
        }
        arguments.putAll((Map)System.getProperties());
        arguments.putAll(ENV.getProperties());
        if (beanInstance == null) {
            return (Map<String, Object>) Util.untyped(arguments);
        }
        Map<String, ? extends Serializable> p = getResult().getParameter();
        if (beanInstance instanceof Map) {
            arguments.putAll((Map<? extends String, ? extends T>) beanInstance);
        } else {
            //put in all attributes
            arguments.putAll((Map<String, ? extends T>) Util.untyped(BeanUtil.toValueMap(beanInstance, false,
                true, p != null, (p != null ? p.keySet().toArray(new String[0]) : null))));
            //and now the instance itself
            arguments.put(BeanAttribute.toFirstLower(Bean.getBean(beanInstance).getName()), (T) beanInstance);
        }

        //TODO: not performance-optimized: do the filtering before
        if (p != null)
            MapUtil.retainAll(arguments, p.keySet());

        return (Map<String, Object>) Util.untyped(arguments);
    }

    /**
     * @return Returns the arguments.
     */
    protected Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * @param arguments The arguments to set.
     */
    protected void setArguments(Map<String, Object> arguments) {
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
