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

import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.Util;

/**
 * Input: string holding boolean expressions. conditional execution
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "serial" })
public class ConditionOperator<T> extends SOperator<T> {
    private BooleanOperator op;

    public static final String KEY_THEN = "?";
    public static final String KEY_ELSE = ":";

    public static final String KEY_ANY = ".+";

    /**
     * constructor
     */
    public ConditionOperator() {
        this(null);
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
//        operationDefs.putAll((Map<? extends CharSequence, ? extends IAction<T>>) op.operationDefs);
        converter = createConverter();
    }

    protected BooleanOperator getOp() {
        if (op == null)
            op = new BooleanOperator((Map<CharSequence, Boolean>) values);
        return op;
    }
    protected IConverter<CharSequence, T> createConverter() {
        Format fmt = new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(StringUtil.toString(obj));
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                pos.setIndex(Util.isEmpty(source) ? 1 : source.length());
                Boolean b = Boolean.valueOf(source);
                //check, if it's really a boolean. if not, return it without conversion.
                return b.toString().equals(source) ? b : source;
            }

        };
        return new FromCharSequenceConverter<T>(fmt);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void createOperations() {
        syntax.put(KEY_OPERATION, "[!&|?:]");
        operationDefs = new HashMap<CharSequence, IAction<T>>();
        addOperation(KEY_THEN, new CommonAction<T>() {
            @Override
            public T action() throws Exception {
                boolean result =
                    parameter[0] instanceof Boolean ? (Boolean) parameter[0] : op.eval((String) parameter[0]);
                /*
                 * if expression is true, we start an action or simply return a stored value.
                 */
                return executeIf((T) parameter[1], result);
            }
        });
        addOperation(KEY_ELSE, new CommonAction<T>() {
            @Override
            public T action() throws Exception {
                boolean result =
                    parameter[0] instanceof Boolean ? (Boolean) parameter[0] : op.eval((String) parameter[0]);
                /*
                 * if expression is false, we start an action or simply return a stored value.
                 */
                return executeIf((T) parameter[1], !result);
            }
        });

        addOperation("&", new TypeOP(getOp(), Boolean.class, "&"));
        addOperation("|", new TypeOP(getOp(), Boolean.class, "|"));
        addOperation("!", new TypeOP(getOp(), Boolean.class, "!"));
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
            else if (p instanceof CharSequence
                && ((CharSequence) p).toString().matches(KEY_ANY + syntax(KEY_OPERATION) + KEY_ANY))
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

    @Override
    public T eval(CharSequence expression) {
        CharSequence ifCond = subElement(expression, null, KEY_THEN, false);
        if (!isEmpty(ifCond) && !ifCond.equals(expression)) {
            //perhaps extract the last expression before key_then
            CharSequence prefix = subElement(ifCond, null, syntax.get(KEY_BEGIN), true);
            ifCond = subElement(ifCond, syntax.get(KEY_BEGIN), syntax.get(KEY_END), true);
            if (prefix.equals(ifCond))
                prefix = syntax.get(KEY_EMPTY);
            //check and execute
            boolean ifTrue = this.op.eval(ifCond, (Map<CharSequence, Boolean>) values);
            return ifTrue
                ? eval(concat(prefix, subEnclosing(expression, KEY_THEN, KEY_ELSE)))
                : eval(concat(prefix, subEnclosing(expression, KEY_ELSE, null)));
        } else {
            return super.eval(expression);
        }
    }
}

/*
 * re-use boolean operations
 */
class TypeOP<T> extends CommonAction<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8140609432513549007L;
    SOperator<T> op;
    Class<T> type;
    String sop = "&";

    public TypeOP(SOperator<T> op, Class<T> type, String sop) {
        this.op = op;
        this.type = type;
        this.sop = sop;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T action() throws Exception {
        T o1 =
            (T) (type.isAssignableFrom(parameter[0].getClass()) ? (T) parameter[0] : op.converter
                .to((CharSequence) parameter[0]));
        T o2 =
            (T) (type.isAssignableFrom(parameter[1].getClass()) ? (T) parameter[1] : op.converter
                .to((CharSequence) parameter[1]));
        IAction<T> operation = (IAction<T>) op.operationDefs.get(sop);
        operation.setParameter(new Object[] { o1, o2 });
        return operation.activate();
    }
}
