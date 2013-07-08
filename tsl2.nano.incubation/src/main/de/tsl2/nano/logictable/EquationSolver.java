/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Dec 8, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.logictable;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Map;

import de.tsl2.nano.util.StringUtil;

/**
 * 
 * TODO: optimize performance (StringBuilder)
 * TODO: more abstract: variable open,close, operation
 * TODO: interface for math-exteded-functions
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class EquationSolver {
    static final String B_OPEN = "(";
    static final String B_CLOSE = ")";
    static final String BETWEEN = ":";
    static final String CONCAT = ";";
    static final String OPERATION = "([+-/*%])";
    static final String EQUATION = "=";
    static final String TERM = "\\" + B_OPEN + "[^)(]*\\" + B_CLOSE;
    static final String OPERAND = "([a-zA-Z0-9]+)";
    static final String SET1 = OPERAND + BETWEEN + OPERAND;
    static final String SET2 = OPERAND + "(" + CONCAT + OPERAND + ")+";
    static final String FUNC = OPERAND + "\\" + B_OPEN + "(" + SET1 + "|" + SET2 + "\\" + B_CLOSE;

    /** a map containing any values. values found by this solver must be of right type */
    Map<String, Object> values;
    NumberFormat formatter;

    /**
     * constructor
     */
    public EquationSolver() {
        this(null, null);
    }

    /**
     * constructor
     * 
     * @param values
     */
    public EquationSolver(NumberFormat formatter, Map<String, Object> values) {
        super();
        if (formatter == null)
            this.formatter = NumberFormat.getInstance();
        else
            this.formatter = formatter;
        if (values == null)
            this.values = new Hashtable<String, Object>();
        else
            this.values = values;
    }

    public Object eval(String expression) {
        return eval(new StringBuilder(expression));
    }
    
    public Object eval(StringBuilder expression) {
        //extract all functions
        evalFunctions(expression);
        //extract all terms
        String term = expression.toString();
        StringBuilder t;
        while (true) {
            term = extract(expression, TERM);
            if (term.isEmpty())
                break;
            t = new StringBuilder(term.substring(1, term.length() - 1));
            StringUtil.replace(expression, term, String.valueOf(operate(t, values)));
        }
        return operate(expression, values);
    }

    protected void evalFunctions(StringBuilder expression) {
        //TODO implementieren
    }

    private BigDecimal operate(StringBuilder term, Map<String, Object> values) {
        String o1 = extract(term, OPERAND, "");
        String op = extract(term, OPERATION, "");
        String o2 = extract(term, OPERAND, "");

        String rest = StringUtil.substring(term, o2, null).trim();

        /*
         * the value map may contain any values - but the found value must have the right type!
         */
        BigDecimal n1 = (BigDecimal) values.get(o1);
        n1 = n1 != null ? n1 : new BigDecimal(o1);
        BigDecimal n2 = (BigDecimal) values.get(o2);
        n2 = n2 != null ? n2 : new BigDecimal(o2);

        BigDecimal result;
        switch (op.charAt(0)) {
        case '+':
            result = n1.add(n2);
            break;
        case '-':
            result = n1.subtract(n2);
            break;
        case '*':
            result = n1.multiply(n2);
            break;
        case '/':
            result = n1.divide(n2);
            break;
        case '%':
            result = new BigDecimal(n1.intValueExact() % n2.intValueExact());
            break;
        case '^':
            result = n1.pow(n2.intValueExact());
            break;
        default:
            throw new IllegalArgumentException(term.toString());
        }
        if (!rest.isEmpty()) {
            term = new StringBuilder(result + rest);
            result = operate(term, values);
        }
        return result;
    }

    private String extract(CharSequence source, String regexp) {
        return extract(source, regexp, null);
    }
    private String extract(CharSequence source, String regexp, String replacement) {
        return StringUtil.extract(source, regexp, replacement);
    }

    private Object func(String name, Number... values) {
        return null;
    }
}
