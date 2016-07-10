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
 * Usable as attribute getting it's value through a given restful service. the expression is here an url query path. The
 * default method is GET. If method is PUT or POST, the parent bean (this expression is part of an attribute!) will be
 * transfered as JSON object.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class RestfulExpression<T extends Serializable> extends RunnableExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -107140100937166501L;

    transient Object lastResult;
    /** REST method type */
    String method = "GET";
    /**
     * if true, the url query will be created like: http://.../search?city=Munich&code=80000. otherwise, the parameter
     * will be appended through '/'.
     */
    boolean urlQuerySeparators;
    /**
     * if true, the response-data will be embedded into the html (e.g. iframe-srcdoc) to be handled by a
     * dependency-listener etc. - if false, the response is handled as link (like iframe-src)
     */
    boolean handleResponse;
    
    static {
        registerExpression(RestfulExpression.class);
    }

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
        return "http[s]?:[/]{2,2}.*(\\:\\d{1,8}/)?.*";
    }

    @Override
    protected IPRunnable<T, Map<String, Object>> createRunnable() {
        return new IPRunnable<T, Map<String, Object>>() {
            @Override
            public T run(Map<String, Object> context, Object... extArgs) {
//                if (!NetUtil.isOnline())
//                    return (T) lastResult;
                return (T) NetUtil.getRest(expression, extArgs);
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
