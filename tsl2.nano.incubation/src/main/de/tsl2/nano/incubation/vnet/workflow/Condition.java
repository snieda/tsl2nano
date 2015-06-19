/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 09.02.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.vnet.workflow;

import java.util.Map;

import org.simpleframework.xml.Element;

import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.util.operation.ConditionOperator;

/**
 * evaluates a condition through an expression, see {@link ConditionOperator}. use {@link #isTrue(Map)} to do the
 * evaluation.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Condition extends ConditionOperator<Object> {
    @Element(required = true, type = String.class)
    protected String expression;

    /**
     * constructor
     */
    public Condition() {
        this(new ComparableMap<CharSequence, Object>());
    }

    /**
     * constructor
     * 
     * @param expression
     */
    public Condition(String expression) {
        this();
        this.expression = expression;
    }

    /**
     * constructor
     * 
     * @param values
     */
    public Condition(Map<CharSequence, Object> values) {
        super(values);
    }

    /**
     * setExpression
     * 
     * @param expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean isTrue(Map parameter) {
        //sometimes we get a string out of eval, but sometimes a boolean. so we generalize it ;-)
        return Boolean.valueOf(StringUtil.toString(eval(expression, parameter)));
    }

    @Override
    public String toString() {
        return expression;
    }
}
