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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.Environment;
import de.tsl2.nano.bean.def.AbstractExpression;
import de.tsl2.nano.bean.def.Bean;
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
public class RuleExpression<T> extends AbstractExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 5898135631911176804L;

    /** rule to be executed with expression as ruleName */
    Rule<T> rule;

    /** arguments for rule execution */
    transient Map<CharSequence, T> arguments;

    /** optional real attribute to set the value through this rule */
    String connectedAttribute;

    /** used by {@link #refreshArguments(Object)} to avoid a stackoverflow */
    private transient boolean onRefresh;

    /**
     * constructor
     */
    public RuleExpression() {
        super();
    }

    public RuleExpression(Class<?> argumentHolderClass, String ruleName) {
        this(argumentHolderClass, ruleName, (Class<T>) Object.class);
    }

    /**
     * constructor
     */
    public RuleExpression(Class<?> argumentHolderClass, String ruleName, Class<T> type) {
        super(argumentHolderClass, ruleName, type);
    }

    public Rule<T> getRule() {
        if (rule == null)
            rule = (Rule<T>) Environment.get(RulePool.class).getRule(expression.substring(1));
        return rule;
    }

    @Override
    public T getValue(Object beanInstance) {
        try {
            T result = (T) getRule().execute((Map<CharSequence, T>) refreshArguments(beanInstance));
            if (connectedAttribute != null)
                Bean.getBean((Serializable) beanInstance).getAttribute(connectedAttribute).setValue(result);
            return result;
        } catch (final Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    private Map<? extends CharSequence, ? extends T> refreshArguments(Object beanInstance) {
        if (!onRefresh) {
            onRefresh = true;
            if (arguments == null)
                arguments = new HashMap<CharSequence, T>();
            arguments.putAll((Map<? extends CharSequence, ? extends T>) Bean.getBean((Serializable) beanInstance)
                .toValueMap());
            //transform dates to numbers
            //TODO it's dirty - implement generic for different types
            Set<CharSequence> keySet = arguments.keySet();
            for (CharSequence charSequence : keySet) {
                Object v = arguments.get(charSequence);
                if (v instanceof Date)
                    arguments.put(charSequence, (T) new BigDecimal(((Date)v).getTime()));
            }
            onRefresh = false;
        }
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

    public void connectTo(String attributeOfParent) {
        connectedAttribute = attributeOfParent;
    }

    @Override
    public String getExpressionPattern() {
        return "\\§.*";
    }
}
