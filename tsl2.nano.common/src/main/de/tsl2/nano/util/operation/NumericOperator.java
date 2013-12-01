/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 22.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util.operation;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;

/**
 * Numeric Operator as a sample implementation of {@link Operator}. Is able to do math operations on decimals (including
 * currency symbols). Override {@link #createOperations()} to have more operation definitions.
 * 
 * @author Tom
 * @version $Revision$
 */
public class NumericOperator extends SOperator<BigDecimal> {
    
    /**
     * constructor
     */
    public NumericOperator() {
        this(null);
    }

    /**
     * constructor
     * @param values
     */
    public NumericOperator(
            Map<CharSequence, BigDecimal> values) {
        super(CharSequence.class, createConverter(), values);
    }

    protected static IConverter<CharSequence, BigDecimal> createConverter() {
        DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance();
        fmt.setParseBigDecimal(true);
        return new FromCharSequenceConverter<BigDecimal>(fmt);
    }
    
    /**
     * define all possible operations. see {@link #operationDefs}
     */
    @SuppressWarnings("serial")
    protected void createOperations() {
        syntax.put(KEY_OPERATION, "[-+*/%^]");
//        syntax.put(KEY_DEFAULT_OPERAND, "0");
//        syntax.put(KEY_DEFAULT_OPERATOR, "+");
        operationDefs = new HashMap<CharSequence, IAction<BigDecimal>>();
        addOperation("+", new CommonAction<BigDecimal>() {
            @Override
            public BigDecimal action() throws Exception {
                return ((BigDecimal) parameter[0]).add(((BigDecimal) parameter[1]));
            }
        });
        addOperation("-", new CommonAction<BigDecimal>() {
            @Override
            public BigDecimal action() throws Exception {
                return ((BigDecimal) parameter[0]).subtract(((BigDecimal) parameter[1]));
            }
        });
        addOperation("*", new CommonAction<BigDecimal>() {
            @Override
            public BigDecimal action() throws Exception {
                return ((BigDecimal) parameter[0]).multiply(((BigDecimal) parameter[1]));
            }
        });
        addOperation("/", new CommonAction<BigDecimal>() {
            @Override
            public BigDecimal action() throws Exception {
                return ((BigDecimal) parameter[0]).divide(((BigDecimal) parameter[1]));
            }
        });
        addOperation("%", new CommonAction<BigDecimal>() {
            @Override
            public BigDecimal action() throws Exception {
                return new BigDecimal(((BigDecimal)parameter[0]).intValueExact() % ((BigDecimal)parameter[1]).intValueExact());
            }
        });
        addOperation("^", new CommonAction<BigDecimal>() {
            @Override
            public BigDecimal action() throws Exception {
                return ((BigDecimal) parameter[0]).pow(((BigDecimal)parameter[1]).intValueExact());
            }
        });
    }
}
