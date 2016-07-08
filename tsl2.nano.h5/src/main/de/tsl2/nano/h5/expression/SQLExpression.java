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
import java.util.Collection;
import java.util.Map;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.execution.IPRunnable;

/**
 * Usable as attribute getting it's value through a given sql-query.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class SQLExpression<T extends Serializable> extends RunnableExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 4797735771490194926L;

    static {
        registerExpression(SQLExpression.class);
    }

    public SQLExpression() {
    }

    public SQLExpression(Class<?> attributeHolderClass, String query) {
        this(attributeHolderClass, query, null);
    }

    /**
     * constructor
     */
    public SQLExpression(Class<?> attributeHolderClass, String query, Class<T> type) {
        super(attributeHolderClass, query, type);
    }

    @Override
    public Class<T> getType() {
        if (type == null) {
            type = (Class<T>) getTypeFromQuery(((Query)createRunnable()).getQuery());
        }
        return type;
    }
    /**
     * evaluates count of query-columns. if only one, return type Object.class, else return Collection.class
     * 
     * @param query to analyze
     * @return query-result type.
     */
    private static Class<?> getTypeFromQuery(String query) {
        //TODO: how to match that with 'from' at the end? (not working yet!)
        return query.toLowerCase().matches("(?m)[^\\(']+[,][^\\)']+") ? Collection.class : Object.class;
    }

    @Override
    public String getExpressionPattern() {
        return "\\?.*";
    }

    @Override
    protected IPRunnable<T, Map<String, Object>> createRunnable() {
        return (IPRunnable<T, Map<String, Object>>) ENV.get(QueryPool.class).get(expression.substring(1));
    }

    @Override
    public String getName() {
        return expression.substring(1);
    }
}
