/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 28.01.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.Environment;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.incubation.rules.Rule;
import de.tsl2.nano.incubation.rules.RulePool;

/**
 * Attribute providing the calculation of a {@link Rule}. This attribute can be connected to a 'real' bean-attribute to
 * transfer the calculation value.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class RuleAttribute<T> extends BeanValue<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 5898135631911176804L;

    Class<T> type;
    Map<CharSequence, T> arguments;
    /** optional real attribute to set the value through this rule */
    AttributeDefinition<T> connectedAttribute;

    /**
     * constructor
     */
    public RuleAttribute() {
        super();
    }

    public RuleAttribute(Bean<?> parent, String ruleName) {
        this(parent, ruleName, (Class<T>) Object.class);
    }

    /**
     * constructor
     */
    public RuleAttribute(Bean<?> parent, String ruleName, Class<T> type) {
        super(Environment.get(RulePool.class).getRule(ruleName), executionMethod());
        this.parent = parent;
        this.arguments = new HashMap<CharSequence, T>();
        this.type = type;
    }

    private static Method executionMethod() {
        try {
            return Rule.class.getMethod("execute", Map.class);
        } catch (NoSuchMethodException e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    public Rule<T> getRule() {
        return (Rule<T>) getInstance();
    }

    @Override
    public Object getValue(Object beanInstance) {
        try {
            Object result = readAccessMethod.invoke(beanInstance, refreshArguments());
            if (connectedAttribute != null)
                connectedAttribute.setValue(beanInstance, result);
            return result;
        } catch (final Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    private Object refreshArguments() {
        arguments.putAll((Map<? extends CharSequence, ? extends T>) parent.toValueMap());
        return arguments;
    }

    /**
     * @return Returns the arguments.
     */
    protected Map<CharSequence, T> getArguments() {
        return arguments;
    }

    /**
     * @param arguments The arguments to set.
     */
    protected void setArguments(Map<CharSequence, T> arguments) {
        this.arguments = arguments;
    }

    public void connectTo(AttributeDefinition<T> attribute) {
        connectedAttribute = attribute;
    }
}
