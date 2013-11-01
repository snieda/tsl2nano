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
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.util.StringUtil;

/**
 * Base {@link Operator} for string expressions.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public abstract class SOperator<T> extends Operator<CharSequence, T> {

    /**
     * constructor
     */
    public SOperator() {
        super();
    }

    /**
     * constructor
     * 
     * @param inputClass
     * @param converter
     * @param values
     */
    public SOperator(Class<? extends CharSequence> inputClass,
            IConverter<CharSequence, T> converter,
            Map<CharSequence, T> values) {
        super(inputClass, converter, values);
    }

    /**
     * default implementation. please override
     * 
     * @return map containing needed {@link #syntax}. see {@link #syntax(String)}.
     */
    protected Map<String, CharSequence> createSyntax() {
        String open = "(", close = ")";
        return MapUtil.asMap(KEY_BEGIN,
            open,
            KEY_END,
            close,
            KEY_BETWEEN,
            ":",
            KEY_CONCAT,
            ";",
            KEY_OPERATION,
            "(.*)",
            KEY_OPERAND,
            "([a-zA-Z0-9]+)",
            KEY_EMPTY,
            ""/*,
            KEY_DEFAULT_OPERAND,
            "",
            KEY_DEFAULT_OPERATOR,
            ""*/);
    }

    protected void createTermSyntax() {
        String term = "[^" + "\\" + syntax(KEY_END) + "\\" + syntax(KEY_BEGIN) + syntax(KEY_OPERATION).subSequence(1, syntax(KEY_OPERATION).length() - 2) + "]*" + syntax(KEY_OPERATION) + "\\s*" + syntax(KEY_OPERAND);
        syntax.put(KEY_TERM, term);
        syntax.put(KEY_TERM_ENCLOSED, "\\" + syntax(KEY_BEGIN) + "\\s*" + term + "\\s*" + "\\" + syntax(KEY_END));
    }

    @Override
    protected void replace(CharSequence src, CharSequence expression, CharSequence replace) {
        StringUtil.replace((StringBuilder) src, expression.toString(), replace.toString());
    }

    @Override
    protected CharSequence extract(CharSequence source, CharSequence match, CharSequence replacement) {
        return StringUtil.extract(source, match.toString(), replacement != null ? replacement.toString() : null);
    }

    @Override
    protected CharSequence concat(Object... input) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < input.length; i++) {
            s.append(input[i]);
        }
        return s.toString();
    }

    @Override
    protected CharSequence wrap(CharSequence src) {
        return new StringBuilder(src);
    }

    protected CharSequence unwrap(CharSequence src) {
        return src != null ? src.toString() : null;
    }
}
