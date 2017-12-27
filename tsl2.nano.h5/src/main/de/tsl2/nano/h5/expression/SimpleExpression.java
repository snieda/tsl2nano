/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 15.07.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.h5.expression;

import java.io.Serializable;
import java.util.Map;

import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.incubation.specification.AbstractRunnable;

/**
 * simple runnable expression. useful to show an url in an iframe. all context properties are filled into expression if
 * there are ant-like variables.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class SimpleExpression extends RunnableExpression<String> {
    /** serialVersionUID */
    private static final long serialVersionUID = 8038825503409780745L;

    static {
        registerExpression(SimpleExpression.class);
    }

    /**
     * constructor
     */
    public SimpleExpression() {
    }

    @Override
    public String getExpressionPattern() {
        //no other expression type...
        return "[^@!§$%&].*";
    }

    @SuppressWarnings("serial")
    @Override
    protected IPRunnable<String, Map<String, Object>> createRunnable() {
        return new AbstractRunnable<String>() {
            @Override
            public String run(Map<String, Object> context, Object... extArgs) {
                return StringUtil.insertProperties(expression, context);
            }

            @Override
            public String getName() {
                return WebClient.getName(expression);
            }
        };
    }

}
