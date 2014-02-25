/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 25.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import java.io.Serializable;
import java.util.Map;

import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.util.StringUtil;

/**
 * Usable as attribute getting it's value through a give sql-query.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class SQLExpression<T extends Serializable> extends RunnableExpression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 4797735771490194926L;

    public SQLExpression() {
    }

    @SuppressWarnings("unchecked")
    public SQLExpression(Class<?> argumentHolderClass, String query) {
        this(argumentHolderClass, query, (Class<T>) Object.class);
    }

    /**
     * constructor
     */
    public SQLExpression(Class<?> argumentHolderClass, String query, Class<T> type) {
        super(argumentHolderClass, query, type);
    }

    @Override
    public String getExpressionPattern() {
        return "select.*";
    }

    @Override
    protected IPRunnable<T, Map<String, Object>> createRunnable() {
        return new SQLRunner<T>(getName(), getExpression(), true, null);
    }

    @Override
    public String getName() {
        return StringUtil.substring(expression, "select ", "from");
    }
}
