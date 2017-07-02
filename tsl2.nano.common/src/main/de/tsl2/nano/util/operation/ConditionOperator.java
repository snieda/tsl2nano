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
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Input: string holding boolean expressions. conditional execution
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "serial" })
public class ConditionOperator<T> extends SOperator<T> {
    transient private BooleanOperator op;

    public static final String KEY_THEN = "?";
    public static final String KEY_ELSE = ":";
    public static final String KEY_EQUALS = "=";

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
        if (op == null) {
            op = new BooleanOperator((Map<CharSequence, Boolean>) getValues());
        }
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
        syntax.put(KEY_OPERATION, "[!&|?:=]");
        operationDefs = new HashMap<CharSequence, IAction<T>>();
        //TODO: the following two operations will be overwritten through the TypeOp ones, so we should delete them
        addOperation(KEY_THEN, new CommonAction<T>() {
            @Override
            public T action() throws Exception {
                boolean result =
                    parameters().getValue(0) instanceof Boolean ? (Boolean) parameters().getValue(0) : op.eval((String) parameters().getValue(0));
                /*
                 * if expression is true, we start an action or simply return a stored value.
                 */
                return executeIf((T) parameters().getValue(1), result);
            }
        });
        addOperation(KEY_ELSE, new CommonAction<T>() {
            @Override
            public T action() throws Exception {
                boolean result =
                    parameters().getValue(0) instanceof Boolean ? (Boolean) parameters().getValue(0) : op.eval((String) parameters().getValue(0));
                /*
                 * if expression is false, we start an action or simply return a stored value.
                 */
                return executeIf((T) parameters().getValue(1), !result);
            }
        });
        addOperation(KEY_EQUALS, new CommonAction<T>() {
            @Override
            public T action() throws Exception {
                /*
                 * if both parameter are equal, return true
                 */
                return (T)Util.untyped(Util.equals(parameter.getValues()));
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
            if (p instanceof IAction) {
                result = ((IAction<T>) p).activate();
            } else if (p instanceof CharSequence
                && ((CharSequence) p).toString().matches(KEY_ANY + syntax(KEY_OPERATION) + KEY_ANY)) {
                result = op.eval((CharSequence) p);
            } else {
                result = p;
            }
            addValue(KEY_RESULT, (T) result);
        } else {
            //trick to overrule the compiler check
            result = Boolean.FALSE;
        }
        return (T) result;
    }

    @Override
    public T eval(CharSequence expression) {
        CharSequence ifCond = subElement(expression, null, KEY_THEN, false);
        if (!isEmpty(ifCond) && !ifCond.equals(expression.toString())) {
            //perhaps extract the last expression before key_then
            CharSequence prefix = subElement(ifCond, null, syntax.get(KEY_BEGIN), true);
            CharSequence ifCondSub = subElement(ifCond, syntax.get(KEY_BEGIN), syntax.get(KEY_END), true);
            if (prefix.equals(ifCondSub)) {
                prefix = syntax.get(KEY_EMPTY);
            }
            //check and execute
            boolean ifTrue = this.op.eval(ifCondSub, (Map<CharSequence, Boolean>) getValues());
            if (!ifTrue) {
                ifTrue = this.op.eval(ifCond, (Map<CharSequence, Boolean>) getValues());
            }
            return ifTrue
                ? eval(concat(prefix, subEnclosing(expression, KEY_THEN, KEY_ELSE)))
                : eval(concat(prefix, subEnclosing(expression, KEY_ELSE, null)));
        } else {
            return super.eval(precalc(expression));
        }
    }

    /**
     * may do some pre calculations. override this method to do them.
     * @param expression expression to be prepared
     * @return result of pre calculations
     */
    protected CharSequence precalc(CharSequence expression) {
        return expression;
    }
}

/*
 * re-use boolean operations
 */
class TypeOP<T> extends CommonAction<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8140609432513549007L;
    transient SOperator<T> op;
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
            parameters().getValue(0) != null && type.isAssignableFrom(parameters().getValue(0).getClass()) ? (T) parameters().getValue(0)
            : op.converter
                .to((CharSequence) parameters().getValue(0));
        T o2 =
            parameters().getValue(1) != null && type.isAssignableFrom(parameters().getValue(1).getClass()) ? (T) parameters().getValue(1)
            : op.converter
                .to((CharSequence) parameters().getValue(1));
        IAction<T> operation = op.operationDefs.get(sop);
        operation.setParameter(new Object[] { o1, o2 });
        return operation.activate();
    }
}
