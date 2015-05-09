/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 25.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.expression;

import java.io.Serializable;
import java.util.Map;

import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.execution.IPRunnable;

/**
 * Usable as attribute getting it's value through a given restful service.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class RestfulExpression<T extends Serializable> extends RunnableExpression<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = -107140100937166501L;

    public RestfulExpression() {
    }

    public RestfulExpression(Class<?> declaringClass, String query) {
        this(declaringClass, query, null);
    }

    /**
     * constructor
     */
    public RestfulExpression(Class<?> declaringClass, String query, Class<T> type) {
        super(declaringClass, query, type);
    }

    @Override
    public String getExpressionPattern() {
        return "http[s]?:[/]{2,2}.*\\:\\d{1,8}/.*";
    }

    @Override
    protected IPRunnable<T, Map<String, Object>> createRunnable() {
        return new IPRunnable<T, Map<String,Object>>() {
            @Override
            public T run(Map<String, Object> context, Object... extArgs) {
                return (T) NetUtil.getRestful(expression, extArgs);
            }
            @Override
            public String getName() {
                return expression;
            }
            @Override
            public Map<String, ? extends Serializable> getParameter() {
                // TODO Auto-generated method stub
                return null;
            }
            @Override
            public Map<String, Object> checkedArguments(Map<String, Object> args, boolean strict) {
                // TODO Auto-generated method stub
                return args;
            }
        };
    }

    @Override
    public String getName() {
        return expression.substring(1);
    }
}
