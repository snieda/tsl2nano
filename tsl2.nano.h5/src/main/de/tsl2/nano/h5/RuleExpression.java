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
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.Environment;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RulePool;

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
        return (Rule<T>) Environment.get(RulePool.class).get(expression.substring(1));
    }

    @Override
    protected Map<String, Object> refreshArguments(Object beanInstance) {
        Map<String, Object> args = super.refreshArguments(beanInstance);
        //transform dates to numbers
        //TODO it's dirty - implement generic for different types
        Set<String> keySet = arguments.keySet();
        for (String charSequence : keySet) {
            Object v = arguments.get(charSequence);
            if (v instanceof Date)
                arguments.put(charSequence, (T) new BigDecimal(((Date) v).getTime()));
        }
        return (Map<String, Object>) arguments;
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
