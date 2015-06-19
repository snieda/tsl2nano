/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.06.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.core.util;

/**
 * some helpers on regular expressions
 * 
 * @author Tom
 * @version $Revision$
 */
public class RegExUtil {

    /**
     * combines the given regular expressions with an or-condition.
     * 
     * @param terms regex
     * @return concatenation with or
     */
    public static String any(CharSequence... terms) {
        return createTerm("|", terms);
    }

    /**
     * combines the given regular expressions with an and-condition.
     * 
     * @param terms regex
     * @return concatenation with and
     */
    public static String all(CharSequence... terms) {
        return createTerm("&", terms);
    }

    /**
     * combines the given regular expressions with an or-condition.
     * 
     * @param concat concatention string like '|' or '&'.
     * @param terms regex
     * @return concatenation with or
     */
    protected static String createTerm(String concat, CharSequence... terms) {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        for (int i = 0; i < terms.length; i++) {
            buf.append(terms[i] + concat);
        }
        buf.replace(buf.length() - 1, buf.length(), ")");
        return buf.substring(0, buf.length() - 1);
    }
}
