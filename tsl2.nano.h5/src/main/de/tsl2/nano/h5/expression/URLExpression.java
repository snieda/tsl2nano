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

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.incubation.specification.Pool;

/**
 * Usable as attribute getting it's value through a given restful service. the expression is here an url query path. The
 * default method is GET. If method is PUT or POST, the parent bean (this expression is part of an attribute!) will be
 * transfered as JSON object.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
public class URLExpression<T extends Serializable> extends RunnableExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -107140100937166501L;

    static {
        registerExpression(URLExpression.class);
    }

    public URLExpression() {
    }

    public URLExpression(String expression) {
        this(null, expression, null);
    }

    /**
     * constructor
     */
    public URLExpression(Class<?> declaringClass, String query, Class<T> type) {
        super(declaringClass, query, type);
    }

    @Override
    public String getExpressionPattern() {
        return "@.*";//"@http[s]?:[/][/].*(\\:\\d{1,8}/)?.*";
    }

    /**
     * sets the new Expression and evaluates new values for: {@link #method}, {@link #contentType} {@link #valuesOnly}, {@link #urlRESTSeparators}, {@link #handleResponse}.
     * @param expression new expression
     */
    @Override
    public void setExpression(String expression) {
        super.setExpression(expression);
        //create a new web-client
        IPRunnable<T, Map<String, Object>> runner = null;
        try {
            runner = createRunnable();
        } catch (Exception e) {
            //Ok, no runner found - we create a new one
        }
        if (runner == null) {
            WebClient client = WebClient.create(expression.substring(1), declaringClass);
            super.setExpression("@" + client.getName());
            ENV.get(Pool.class).add(client);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected IPRunnable<T, Map<String, Object>> createRunnable() {
        return (WebClient<T>) ENV.get(Pool.class).get(WebClient.getName(expression.substring(1)));
    }

    @Override
    public String getName() {
        if (name == null)
            name = createRunnable().getName();
        return name;
    }
}
