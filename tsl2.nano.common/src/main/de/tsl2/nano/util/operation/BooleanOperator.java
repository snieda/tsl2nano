/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 22.10.2013
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
import de.tsl2.nano.core.util.Util;

/**
 * Boolean Operator as a sample implementation of {@link Operator}. Is able to do boolean operations.
 * <p/>
 * TODO: performance enhancing pre-check for evaluation (e.g.: A | B ==> if A is true, B can be ignored!)<br/>
 * TODO: XOR ==> !(A&B)&(A|B), NOT ==> (!A), EQUALS ==> (A&B)|!(A&B) <br/>
 * TODO: Boolean with bits: TRUE=1, FALSE=0
 * 
 * @author Tom
 * @version $Revision$
 */
public class BooleanOperator extends SOperator<Boolean> {

    /**
     * constructor
     */
    public BooleanOperator() {
        super();
    }

    /**
     * constructor
     * 
     * @param values
     */
    public BooleanOperator(Map<CharSequence, Boolean> values) {
        super(CharSequence.class, createConverter(), values);
    }

    protected static IConverter<CharSequence, Boolean> createConverter() {
        @SuppressWarnings("serial")
        Format fmt = new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(((Boolean) obj).toString());
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                pos.setIndex(Util.isEmpty(source) ? 1 : source.length());
                return Boolean.valueOf(source);
            }

        };
        return new FromCharSequenceConverter<Boolean>(fmt);
    }

    /**
     * define all possible operations. see {@link #operationDefs}
     */
    @Override
    @SuppressWarnings("serial")
    protected void createOperations() {
        syntax.put(KEY_OPERATION, "[!&|=]");
//        syntax.put(KEY_DEFAULT_OPERAND, "false");
//        syntax.put(KEY_DEFAULT_OPERATOR, "|");
        operationDefs = new HashMap<CharSequence, IAction<Boolean>>();
        addOperation("&", new CommonAction<Boolean>() {
            @Override
            public Boolean action() throws Exception {
                return (Boolean) parameters().getValue(0) & (Boolean) parameters().getValue(1);
            }
        });
        addOperation("|", new CommonAction<Boolean>() {
            @Override
            public Boolean action() throws Exception {
                return (Boolean) parameters().getValue(0) | (Boolean) parameters().getValue(1);
            }
        });
        addOperation("!", new CommonAction<Boolean>() {
            @Override
            public Boolean action() throws Exception {
                return !(Boolean) parameters().getValue(1);
            }
        });
        addOperation("=", new CommonAction<Boolean>() {
            @Override
            public Boolean action() throws Exception {
                return Util.equals(parameter.getValues());
            }
        });
    }

}
