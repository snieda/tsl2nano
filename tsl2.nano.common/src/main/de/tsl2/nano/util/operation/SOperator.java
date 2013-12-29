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

import java.util.Map;

import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.util.Util;
import de.tsl2.nano.util.parser.SParser;

/**
 * Base {@link Operator} for string expressions.<p/>
 * TODO: create GenericOperator reading reflective operations from xml.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public abstract class SOperator<T> extends Operator<CharSequence, T> {
    SParser parser = new SParser();
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
            "([a-zA-Z0-9.,]+)",
            KEY_EMPTY,
            ""/*,
            KEY_DEFAULT_OPERAND,
            "",
            KEY_DEFAULT_OPERATOR,
            ""*/);
    }

    protected void createTermSyntax() {
        if (syntax.get(KEY_HIGH_OPERATION) == null)
            syntax.put(KEY_HIGH_OPERATION, syntax.get(KEY_OPERATION));
        String term = "[^" + "\\" + syntax(KEY_END) + "\\" + syntax(KEY_BEGIN) + syntax(KEY_OPERATION).subSequence(1, syntax(KEY_OPERATION).length() - 2) + "]*" + syntax(KEY_OPERATION) + "\\s*" + syntax(KEY_OPERAND);
        syntax.put(KEY_TERM, term);
        syntax.put(KEY_TERM_ENCLOSED, "\\" + syntax(KEY_BEGIN) + "\\s*" + term + "\\s*" + "\\" + syntax(KEY_END));
    }

    @Override
    public void replace(CharSequence src, CharSequence expression, CharSequence replace) {
        parser.replace(src, expression, replace);
    }

    @Override
    public CharSequence extract(CharSequence source, CharSequence match, CharSequence replacement) {
        return parser.extract(source, match.toString(), replacement != null ? replacement.toString() : null);
    }

    @Override
    public CharSequence subElement(CharSequence src, CharSequence begin, CharSequence end, boolean last) {
        return parser.subElement(src.toString(), Util.asString(begin), Util.asString(end), last);
    }
    
    @Override
    public CharSequence subEnclosing(CharSequence src, CharSequence begin, CharSequence end) {
        return parser.subEnclosing(src.toString(), Util.asString(begin), Util.asString(end));
    }
    
    @Override
    public CharSequence concat(Object... input) {
        return parser.concat(input);
    }

    @Override
    public CharSequence wrap(CharSequence src) {
        return parser.wrap(src);
    }

    public CharSequence unwrap(CharSequence src) {
        return parser.unwrap(src);
    }
    
    @Override
    public CharSequence trim(CharSequence totrim) {
        return parser.trim(totrim);
    }
}
