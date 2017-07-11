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
import java.util.LinkedHashMap;
import java.util.Map;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.incubation.specification.AbstractRunnable;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.util.operation.NumericOperator;

/**
 * 
 * TODO: optimize performance (StringBuilder) TODO: use NumericOperator
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class EquationSolver extends NumericOperator {
    static final String B_OPEN = "\\(";
    static final String B_CLOSE = "\\)";
    static final String BETWEEN = ":";
    static final String SEPARATOR = ",";
    static final String CONCAT = "\\s*[" + SEPARATOR + BETWEEN + "]\\s*";
    static final String OPERATION = "([+-/*%])";
    static final String EQUATION = "=";
    static final String TERM = B_OPEN + "[^)(]*" + B_CLOSE;
    static final String OPERAND = "([a-zA-Z0-9_]+)";
    static final String SET = "(" + OPERAND + "(" + CONCAT + OPERAND + ")*)*";
    static final String FUNC = OPERAND + B_OPEN + SET + B_CLOSE;

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
        if (formatter == null) {
            this.formatter = NumberFormat.getInstance();
        } else {
            this.formatter = formatter;
        }
        if (values == null) {
            this.values = new Hashtable<String, Object>();
        } else {
            this.values = values;
        }
    }

    public Object eval(String expression) {
        return eval(new StringBuilder(expression));
    }

    public Object eval(StringBuilder expression) {
        //extract all functions
        evalFunctions(expression);
        //extract all terms
        String term;
        StringBuilder t;
        while (true) {
            term = extract(expression, TERM);
            if (isEmpty(term)) {
                break;
            }
            t = new StringBuilder(term.substring(1, term.length() - 1));
            StringUtil.replace(expression, term, String.valueOf(operate(t, values)));
        }
        return !isEmpty(expression) && expression.toString().matches(".*" + OPERATION + ".*") 
                ? operate(expression, values) : expression.toString();
    }

    /**
     * isEmpty
     * @param term
     * @return
     */
    boolean isEmpty(String term) {
        return term.isEmpty() || term.equals("()");
    }

    /**
     * replaces all found functions/rule inside the expression with their calculation results.
     * 
     * @param expression
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void evalFunctions(StringBuilder expression) {
        String regex = FUNC;//AbstractExpression.createRegExpOnAllRegistered();
        String expr = expression.toString();
        Object result;
        Map args; //TODO: use extra args?
        StringBuilder func, tempFunc;
        String sfunc, fname;
        int i = 0;
        do {
            sfunc = StringUtil.findRegExp(expr, regex, i);
            if (Util.isEmpty(sfunc))
                break;
            func = new StringBuilder(sfunc);
            fname = StringUtil.extract(tempFunc=new StringBuilder(func), OPERAND, "");
            args = getFunctionArgs(tempFunc, values);
            IPRunnable runner = ENV.get(Pool.class).find(fname);
            result = runner.run(args);
            StringUtil.replace(expression, func.toString(), StringUtil.toString(result), i);
            i += func.length();
        } while (true);
    }

    private Map getFunctionArgs(StringBuilder func, Map<String, Object> values) {
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        AbstractRunnable.markArgumentsAsSequence(args);
        String a;
        do {
            a = StringUtil.extract(func, OPERAND, "");
            if (Util.isEmpty(a))
                break;
            args.put(a, values.get(a));
        } while (true);
        return args;
    }

    private BigDecimal operate(StringBuilder term, Map<String, Object> values) {
        String o1 = extract(term, OPERAND, "");
        String op = extract(term, OPERATION, "");
        String o2 = extract(term, OPERAND, "");

        String rest = StringUtil.substring(term, o2, null).trim();

        /*
         * the value map may contain any values - but the found value must have the right type!
         */
        Object v1 = values.containsKey(o1) ? values.get(o1) : o1;
        BigDecimal n1 = (BigDecimal) (v1 instanceof BigDecimal ? v1 : new BigDecimal(String.valueOf(v1)));
        Object v2 = values.containsKey(o2) ? values.get(o2) : o2;
        BigDecimal n2 = (BigDecimal) (v2 instanceof BigDecimal ? v2 : new BigDecimal(String.valueOf(v2)));

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
