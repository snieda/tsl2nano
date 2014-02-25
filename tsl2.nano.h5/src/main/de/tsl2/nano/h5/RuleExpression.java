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
import java.util.Map;

import de.tsl2.nano.Environment;
import de.tsl2.nano.execution.IPRunnable;
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
public class RuleExpression<T extends Serializable> extends RunnableExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 5898135631911176804L;

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

    protected IPRunnable<T, Map<String, Object>> createRunnable() {
        return (Rule<T>) Environment.get(RulePool.class).getRule(expression.substring(1));
    }


    @Override
    public String getExpressionPattern() {
        return "\\§.*";
    }

    @Override
    public String getName() {
        return expression.substring(1);
    }
}
