/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 06.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util.parser;

import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.Util;

/**
 * Simple text parser as extension of generic parser {@link Parser}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class SParser extends Parser<CharSequence> {

    @Override
    public void replace(CharSequence src, CharSequence expression, CharSequence replace) {
        StringUtil.replace((StringBuilder) src, expression.toString(), replace.toString());
    }

    @Override
    public CharSequence extract(CharSequence source, CharSequence match, CharSequence replacement) {
        return StringUtil.extract(source, match.toString(), replacement != null ? replacement.toString() : null);
    }

    @Override
    public CharSequence subElement(CharSequence src, CharSequence begin, CharSequence end, boolean last) {
         return StringUtil.substring(src.toString(), Util.asString(begin), Util.asString(end), last);
    }

    @Override
    public CharSequence subEnclosing(CharSequence src, CharSequence begin, CharSequence end) {
        return StringUtil.subEnclosing(src.toString(), Util.asString(begin), Util.asString(end), true);
    }

    @Override
    public CharSequence concat(Object... input) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < input.length; i++) {
            s.append(input[i]);
        }
        return s.toString();
    }

    @Override
    public CharSequence wrap(CharSequence src) {
        return new StringBuilder(src);
    }

    public CharSequence unwrap(CharSequence src) {
        return src != null ? src.toString() : null;
    }
}
