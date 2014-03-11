/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 2, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;

import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.util.operation.IConverter;

/**
 * is able to translate a string to a boolean - through a given regular expression. may be useful to translate a
 * character like 'y' and 'n' to true and false.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ValueMatcher implements IValueAccess<Boolean>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -4686551991738977281L;

    ValueBinder<String, Boolean> vb;
    
    protected static final String DEFAULT_TRUE = "[1xXjJyYsSoOtT]";
    protected static final String DEFAULT_FALSE = "[0  nNfF]";

    public ValueMatcher(IValueAccess<String> value) {
        this(value, DEFAULT_TRUE, DEFAULT_TRUE.substring(0, 1), DEFAULT_FALSE.substring(0, 1));
    }

    public ValueMatcher(IValueAccess<String> value, final String defaultTrueValue, final String defaultFalseValue) {
        this(value, DEFAULT_TRUE, defaultTrueValue, defaultFalseValue);
    }

    /**
     * constructor to be serializable
     */
    protected ValueMatcher() {
        super();
    }

    /**
     * constructor
     * @param value
     * @param trueExpression
     */
    public ValueMatcher(IValueAccess<String> value, final String trueExpression, final String defaultTrueValue, final String defaultFalseValue) {
        super();
        IConverter<String, Boolean> c = new IConverter<String, Boolean>() {
            @Override
            public String from(Boolean toValue) {
                return toValue ? defaultTrueValue : defaultFalseValue;
            }

            @Override
            public Boolean to(String fromValue) {
                return fromValue.matches(trueExpression);
            }
        };
        vb = new ValueBinder<String, Boolean>(value, new ValueHolder<Boolean>(c.to(value.getValue())), c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getValue() {
        return vb.getSecondValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Boolean newValue) {
        vb.setSecondValue(newValue);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventController changeHandler() {
        throw new UnsupportedOperationException();
    }
}
