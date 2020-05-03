/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 28.01.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.expression;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.rules.AbstractRule;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RuleDecisionTable;
import de.tsl2.nano.incubation.specification.rules.RuleScript;

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

    static {
        registerExpression(RuleExpression.class);
    }

    /**
     * constructor
     */
    public RuleExpression() {
        super();
    }

    public RuleExpression(Class<?> declaringClass, String ruleName) {
        this(declaringClass, ruleName, (Class<T>) Util.untyped(Object.class));
    }

    /**
     * constructor
     */
    public RuleExpression(Class<?> argumentHolderClass, String ruleName, Class<T> type) {
        super(argumentHolderClass, ruleName, type);
    }

    @Override
    protected IPRunnable<T, Map<String, Object>> createRunnable() {
        return (AbstractRule<T>) ENV.get(Pool.class).get(expression.substring(1), getRunnableType(expression));
    }

    @Override
    protected Map<String, Object> refreshArguments(Object beanInstance) {
        super.refreshArguments(beanInstance);
        //transform dates to numbers
        //TODO it's dirty - implement generic for different types
        Set<String> keySet = arguments.keySet();
        for (String charSequence : keySet) {
            Object v = arguments.get(charSequence);
            if (v instanceof Date) {
                arguments.put(charSequence, (T) new BigDecimal(((Date) v).getTime()));
            }
        }
        return (Map<String, Object>) Util.untyped(arguments);
    }
    
    @Override
    public String getExpressionPattern() {
        return expressionPattern();
    }

    public static String expressionPattern() {
        return "[" + AbstractRule.PREFIX + RuleScript.PREFIX + RuleDecisionTable.PREFIX + "].*";
    }

    @Override
    public String getName() {
        if (name == null)
            name = expression.substring(1);
        return name;
    }
    public static Class<? extends AbstractRule> getRunnableType(String ruleName) {
        return ruleName.charAt(0) == RuleScript.PREFIX ? RuleScript.class : ruleName.charAt(0) == RuleDecisionTable.PREFIX ? RuleDecisionTable.class : Rule.class;
    }
}
