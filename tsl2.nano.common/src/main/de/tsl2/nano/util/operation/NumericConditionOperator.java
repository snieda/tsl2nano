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

/**
 * Combination of conditioning (including booleans) and numeric operator. result has to be of type {@link BigDecimal}.
 * This implementation disables type generics of super classes.
 * 
 * @author Tom
 * @version $Revision$
 */
public class NumericConditionOperator extends ConditionOperator<Object> {

    @Override
    public BigDecimal eval(CharSequence expression, Map<CharSequence, Object> v) {
        Object result = super.eval(expression, v);
        return (BigDecimal) (result instanceof BigDecimal ? result : result != null ? new BigDecimal(result.toString().trim()) : null);
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