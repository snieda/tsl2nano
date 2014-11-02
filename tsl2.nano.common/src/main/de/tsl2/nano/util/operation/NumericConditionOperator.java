/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 30.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util.operation;

import java.math.BigDecimal;
import java.util.Map;

import de.tsl2.nano.core.Environment;

/**
 * Combination of conditioning (including booleans) and numeric operator. result has to be of type {@link BigDecimal}.
 * This implementation disables type generics of super classes. uses {@link Function} to calculate inside functions
 * before.
 * 
 * @author Tom
 * @version $Revision$
 */
public class NumericConditionOperator extends ConditionOperator<Object> {
    @SuppressWarnings("unchecked")
    transient Function<Object> funcop = Environment.get(Function.class);

    /**
     * calculate functions
     */
    @Override
    protected CharSequence precalc(CharSequence expression) {
        //first, calculate functions and replace their expression with their results
        Object funcResult = funcop.eval(expression, (Map<CharSequence, Object>) getValues());
        if (funcResult != null)
            expression = funcResult.toString();

        return super.precalc(expression);
    }
    
    @Override
    public BigDecimal eval(CharSequence expression, Map<CharSequence, Object> v) {
        Object result = super.eval(expression, v);
        return (BigDecimal) (result instanceof BigDecimal ? result : result != null ? new BigDecimal(result.toString()
            .trim()) : null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void createOperations() {
        super.createOperations();
        syntax.put(KEY_OPERATION, "[!&|?:\\-+*/%^]");
        syntax.put(KEY_HIGH_OPERATION, "[*/%^]");
        /*
         * re-use numeric operations
         */
        NumericOperator nop = new NumericOperator();
        addOperation("+", new TypeOP(nop, BigDecimal.class, "+"));
        addOperation("-", new TypeOP(nop, BigDecimal.class, "-"));
        addOperation("*", new TypeOP(nop, BigDecimal.class, "*"));
        addOperation("/", new TypeOP(nop, BigDecimal.class, "/"));
        addOperation("%", new TypeOP(nop, BigDecimal.class, "%"));
        addOperation("^", new TypeOP(nop, BigDecimal.class, "^"));
    }
}
