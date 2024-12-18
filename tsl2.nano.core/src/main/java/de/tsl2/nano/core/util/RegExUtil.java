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

import java.util.Scanner;
import java.util.function.Predicate;
import java.util.regex.Pattern;

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
        return buf.toString();
    }
    public static String createSimpleRegEx(String template) {
        return createSimpleRegEx(template, false);
    }

    /**
     * 
     * @param template example string to generate the regular expression for
     * @param softOnSeparation if true, before each separation character (non-letter, non-digit, non-space) the character before is optional.
     * 
     * @return generated simple regular expression
     */
    public static String createSimpleRegEx(String template, boolean softOnSeparation) {
        return template
            .replaceAll("([:.?,!*+\\-\\|$<>\\}\\{\\]\\[\\)\\(])", (softOnSeparation ? "?" : "") + "[$1]")
            .replaceAll("[a-zA-Z\\p{L}]", ".")
            .replaceAll("\\d", "\\\\d")
            .replaceAll("\\s", "\\\\s");
    }

	public static String createFromRegEx(String regex, int minLength, int maxLength, int maxIterations) {
		if (!regex.matches(".*\\(\\?[=<!].*")) {//lookahead or lookbehind
			StringBuilder buf = new StringBuilder();
			Pattern pattern = Pattern.compile("(\\\\[^dwWsS])|(.+?([*?+]|([{]\\d+[}]))|.+)");
			try (Scanner sc = new Scanner(regex)) {
				sc.findAll(pattern)
						.forEach(p -> buf.append(createFromRegExPart(p.group(), 1, maxLength, maxIterations)));
			}
			return buf.toString();
		} else {
			return createFromRegExPart(regex, minLength, maxLength, maxIterations);
		}
	}

	public static String createFromRegExPart(String regex, int minLength, int maxLength, int maxIterations) {
		regex = cleanUpUnClosedBrackets(regex);
		String slen = StringUtil.extract(regex, "(\\d+)\\}");
		minLength = (int) (slen.length() > 0 ? Integer.parseInt(slen) : minLength);
		Pattern pattern = Pattern.compile(regex);
		CharSequence init = regex.replaceAll("[^\\\\][\\\\*+?\\}\\{\\]\\[]", "");
				init = new StringBuilder(((String) init).replaceAll("[\\\\]([\\\\*+?\\}\\{\\]\\[])", "$1"));
		System.out
				.print(String.format("\tgenerating with (regex: '%s', init: %s, minlen: %d, maxlen: %d, maxiter: %d) ",
						regex, init,
						init.length(), maxLength, maxIterations));
		return generateString((StringBuilder) init, minLength, maxLength, maxIterations,
				s -> pattern.matcher(s).find(s.length() - 1) || pattern.matcher(s).lookingAt());
	}

	private static String cleanUpUnClosedBrackets(String regex) {
		long open = StringUtil.countChar(regex, '(');
		long close = StringUtil.countChar(regex, ')');
		if (open != close) {
			if (open > close) {
				regex = regex.replaceFirst("[(]", "");
			} else {
				int l = regex.lastIndexOf(")");
				StringBuilder buf = new StringBuilder(regex);
				buf.replace(l, l+1, "");
				regex = buf.toString();
			}
		} else if (open == 1 && close == 1 && regex.indexOf('(') > regex.indexOf(')')) {
			regex = regex.replaceAll("[()]", "");
		}
		return regex;
	}

	public static String generateString(StringBuilder init, int minLength, int maxLength, int maxIterations,
					Predicate<CharSequence> checker) {
		StringBuilder match = new StringBuilder(StringUtil.fixString(init != null ? init : " ", minLength));
		int i = 0;
		char c;
		boolean matched = false;
		while (!matched && i++ < maxIterations && match.length() <= maxLength) {
			if (!(matched = checker.test(match)))
				match.setLength(Math.max(minLength, match.length() - 1));
			else
				break;

			c = (char) (int) (32 + Math.random() * 95);
			if (c == 127)
				c++; // -> EUR
			if (minLength > 0) {
				match.setCharAt((int) (Math.random() * minLength), c);
			} else {
				match.append(c);
			}
		}
		// if (match.length() == 0 || !checker.test(match))
		// 	throw new IllegalStateException("couldn't generate a string for: " + checker);
		String s = match.toString();
		System.out.println(" -> '" + s + "' checker match: " + checker.test(s));
		return s;
	}
}
