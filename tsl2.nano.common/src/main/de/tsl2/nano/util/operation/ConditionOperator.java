/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 23.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util.operation;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.util.StringUtil;

/**
 * Input: string holding boolean expressions. conditional execution
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "serial" })
public class ConditionOperator<T> extends SOperator<T> {
    BooleanOperator op;

    /**
     * constructor
     */
    public ConditionOperator() {
        super();
    }

    /**
     * constructor
     * 
     * @param inputClass
     * @param converter
     * @param values
     */
    public ConditionOperator(Map<CharSequence, T> values) {
        super(String.class, null, values);
        op = new BooleanOperator((Map<CharSequence, Boolean>) values);
        operationDefs.putAll((Map<? extends CharSequence, ? extends IAction<T>>) op.operationDefs);
        converter = createConverter();
    }

    protected IConverter<CharSequence, T> createConverter() {
        Format fmt = new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(StringUtil.toString(obj));
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                pos.setIndex(StringUtil.isEmpty(source) ? 1 : source.length());
                return Boolean.valueOf(source);
            }

        };
        return new FromCharSequenceConverter<T>(fmt);
    }

    @Override
    protected void createOperations() {
        syntax.put(KEY_OPERATION, "[!&|?:]");
        operationDefs = new HashMap<CharSequence, IAction<T>>();
        addOperation("?", new CommonAction<T>() {
            @Override
            public T action() throws Exception {
                boolean result = parameter[0] instanceof Boolean ? (Boolean) parameter[0]
                    : op.eval((String) parameter[0]);
                /*
                 * if expression is true, we start an action or simply return a stored value.
                 */
                return executeIf((T) parameter[1], result);
            }
        });
        addOperation(":", new CommonAction<T>() {
            @Override
            public T action() throws Exception {
                boolean result = parameter[0] instanceof Boolean ? (Boolean) parameter[0]
                    : op.eval((String) parameter[0]);
                /*
                 * if expression is false, we start an action or simply return a stored value.
                 */
                return executeIf((T) parameter[1], !result);
            }
        });
    }

    /**
     * execute
     * 
     * @param result
     * @return
     */
    private T executeIf(T p, boolean iF) {
        Object result;
        if (iF) {
            if (p instanceof IAction)
                result = ((IAction<T>) p).activate();
            else if (p instanceof CharSequence && ((CharSequence) p).toString().matches(".+" + syntax(KEY_OPERATION)
                + ".+"))
                result = op.eval((CharSequence) p);
            else
                result = p;
            values.put(KEY_RESULT, (T) result);
        } else {
            //trick to overrule the compiler check
            result = Boolean.FALSE;
        }
        return (T) result;
    }
}
