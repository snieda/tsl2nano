/*
 * 
 * 
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.core.util;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;

/**
 * String helper class
 * 
 * @author ts 21.11.2008
 * @version $Revision: 1.0 $
 * 
 */
@SuppressWarnings("rawtypes")
public class StringUtil {
    /** variable (like ant-variables) matching expression. e.g.: ${myvar} */
    public static final String VAR_REGEXP = "\\$\\{\\w+\\}";

    /**
     * @see #substring(String, String, String, int)
     */
    public static String substring(String data, String from, String to) {
        return substring(data, from, to, 0);
    }

    /**
     * Does the same as substring, but considers outer encapsulating. means, it finds first match for 'from' it searches
     * the last match for 'to'.
     * @param constrain if true and from and to are not null, returns null if from or to was not found.
     * @return outer enclosing match.
     */
    public static String subEnclosing(String data, String from, String to, boolean constrain) {
        if (from == null && to == null)
            return data;
        else if (from == null)
            return substring(data, from, to, true, true);
        else if (to == null)
            return substring(data, from, to, 0, true);
        else {
            int iFrom = data.indexOf(from);
            int iTo = data.lastIndexOf(to);
            if (constrain && iFrom == -1 || iTo == -1)
                return null;
            else
                return data.substring(iFrom != -1 ? iFrom + from.length() : 0, iTo != -1 ? iTo : data.length());
        }
    }

    /**
     * delegates to {@link #substring(String, String, String, boolean)} with constrain = false
     */
    public static String substring(String data, String from, String to, boolean last) {
        return substring(data, from, to, last, false);
    }
    
    /**
     * Does the same as extract, but doesn't consider encapsulating (no regexp!).
     * 
     * @param last whether it searches with lastIndexOf(to).
     * @return
     */
    public static String substring(String data, String from, String to, boolean last, boolean constrain) {
        int i = !last ? 0 : from != null ? data.lastIndexOf(from) : to != null ? data.lastIndexOf(to) : 0;
        if (i < 0 && constrain)
            return null;
        i = i == -1 ? 0 : i;
        if (from != null || i == 0)
            return substring(data, from, to, i, constrain);
        else
            // if 'last' is used for 'to', we return directly the substring
            return data.substring(0, i);
    }

    /**
     * delegates to {@link #substring(String, String, String, int, boolean)} with constrain = false
     */
    public static String substring(String data, String from, String to, int start) {
        return substring(data, from, to, start, false);
    }
    
    /**
     * extracts from from (exclusive) to to, beginning at index start. doesn't consider encapsulating (no regexp!).
     * 
     * @param data text
     * @param from start-string
     * @param to end-string
     * @param start index
     * @param constrain if true, null be returned if from or to couldn't be found
     * @return extracted substring
     */
    public static String substring(String data, String from, String to, int start, boolean constrain) {
        if (from == null) {
            final int i = data.indexOf(to, start);
            if (i < 0) {
                if (constrain)
                    return null;
                else
                    return data.substring(start);
            }
            return data.substring(start, i);
        } else if (to == null) {
            final int i = data.indexOf(from, start);
            if (i < 0) {
                if (constrain)
                    return null;
                else
                    return data.substring(start);
            }
            return data.substring(i + from.length());
        } else {
            int i = data.indexOf(from, start);
            if (i < 0) {
                if (constrain)
                    return null;
                from = "";//-->length = 0
                i = start;
            }
            int j = data.indexOf(to, i + from.length() + 1);
            if (j < 0) {
                if (constrain)
                    return null;
                j = data.length();
            }
            return data.substring(i + from.length(), j);
        }
    }

    /**
     * @see #substring(StringBuilder, String, String, int)
     */
    public static String substring(StringBuilder data, String from, String to) {
        return substring(data, from, to, 0);
    }

    /**
     * TODO: replace substring(String,...) extracts from from (exclusive) to to, beginning at index start. doesn't
     * consider encapsulating (no regexp!).
     * 
     * @param data text
     * @param from start-string
     * @param to end-string
     * @param start index
     * @return extracted substring
     */
    public static String substring(StringBuilder data, String from, String to, int start) {
        /*
         * TODO:
         * as it doesn't exist any sufficient interface for String, StringBuilder, StringBuffer,
         * we do the instance checking here - avoiding to implement the algorithm twice.
         */
        if (from == null) {
            final int i = data.indexOf(to, start);
            if (i < 0) {
                return data.substring(start);
            }
            return data.substring(start, i);
        } else if (to == null) {
            final int i = data.indexOf(from, start);
            if (i < 0) {
                return data.substring(start);
            }
            return data.substring(i + from.length());
        } else {
            int i = data.indexOf(from, start);
            if (i < 0) {
                from = "";//-->length = 0
                i = start;
            }
            int j = data.indexOf(to, i + from.length() + 1);
            if (j < 0) {
                j = data.length();
            }
            return data.substring(i + from.length(), j);
        }
    }

    /**
     * delegates to {@link #trim(StringBuilder, char)} walking through given characters in string (note: character-order
     * is important!)
     */
    public static String trim(String src, String charactersToTrim) {
        char[] carr = charactersToTrim.toCharArray();
        StringBuilder sb = new StringBuilder(src);
        for (int i = 0; i < carr.length; i++) {
            trim(sb, carr[i]);
        }
        return sb.toString();
    }

    /**
     * trims given character left/right for source string
     * 
     * @param src source to trim
     * @param c character to be trimmed
     */
    public static void trim(StringBuilder src, char c) {
        int i = 0;
        //from start
        while (i < src.length() && src.charAt(i) == c)
            src.deleteCharAt(i++);
        //at the end
        i = src.length();
        while (i > 0 && src.charAt(--i) == c)
            src.deleteCharAt(i);
    }

    /**
     * replaces all occurrences of expression in str with replacement. if nothing was found, nothing will be done.
     * 
     * @param str source
     * @param expression expression to find
     * @param replacement replacement
     */
    public static void replace(StringBuilder str, String expression, String replacement) {
        int i = str.indexOf(expression);
        if (i != -1) {
            str.replace(i, i + expression.length(), replacement);
        }
    }

    /**
     * calls toString() of obj object if not null, otherwise an empty string will be returned
     * 
     * @param obj object or null
     * @return toString() of obj or empty string
     */
    public static final String toString(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    /**
     * creates a string representation of the given object
     * 
     * @param o object to be presented
     * @param maxLength max length of string
     * @return string
     */
    public static String toString(Object o, int maxLength) {
//        assert maxLength > 4 : "maxLength shouldn't be smaller than 5!";
        if (maxLength < 4) {
            maxLength = Integer.MAX_VALUE;
        }
        String postfix = "...";
        String result;
        if (o != null && o.getClass().isArray()) {
            o = Util.asList(o);
        }
        if (o instanceof Map) {
            o = ((Map) o).entrySet();
        }
        if (o instanceof Collection<?>) {
            result = o.toString();
            postfix += "size=" + ((Collection<?>) o).size() + "]";
        } else {
            result = String.valueOf(o);
        }
        maxLength -= postfix.length();
        return result != null && result.length() > maxLength ? result.substring(0, maxLength) + postfix : result;
    }

    /**
     * creates a formatted string representation of the given object
     * 
     * @param o object to be presented
     * @param maxLineCount max line count
     * @return string
     */
    public static String toFormattedString(Object o, int maxLineCount) {
        return toFormattedString(o, maxLineCount, true);
    }

    /**
     * creates a formatted string representation of the given object
     * 
     * @param o object to be presented
     * @param maxLineCount max line count
     * @return string
     */
    public static String toFormattedString(Object o, int maxLineCount, boolean showLines) {
        maxLineCount = maxLineCount < 0 ? Integer.MAX_VALUE : maxLineCount;
        String result;
        if (o instanceof Map) {
            o = ((Map) o).entrySet();
        }
        if (o instanceof Collection) {
            o = ((Collection) o).toArray();
        }
        if (o instanceof String) {
            o = ((String) o).split("\r*\n", maxLineCount + 1);
        }
        if (o instanceof Object[]) {
            final Object[] array = (Object[]) o;
            final StringBuilder strBuilder = new StringBuilder(array.length * 50);
            final int c = array.length > maxLineCount ? maxLineCount : array.length;
            for (int i = 0; i < c; i++) {
                strBuilder.append((showLines ? " [" + i + "]: " : "") + array[i] + "\n");
            }
            if (maxLineCount < array.length) {
                strBuilder.append(MessageFormat.format(Messages.getString("tsl2nano.more.elements"),
                    new Object[] { array.length - maxLineCount }));
            }
            result = strBuilder.toString();
        } else {
            result = String.valueOf(o);
        }
        return result;
    }

    /**
     * fills the given origin string on the left or right side with the given fill-chars to have the given fix-length.
     * 
     * @param origin origin string
     * @param fixLength length of result
     * @param fillChar fill char. if -1, do only a cut to the max length!
     * @param rightFill whether to fill on left or right side
     * @return fix length string
     */
    public static String fixString(String origin, int fixLength, char fillChar, boolean rightFill) {
        final StringBuffer buf = new StringBuffer(fixLength);
        final int fillLength = fixLength - origin.length();
        if (fillLength <= 0) {
            final int shiftRight = rightFill ? 0 : fillLength;
            return origin.substring(0 + shiftRight, fixLength + shiftRight);
        } else if ((byte) fillChar == -1) {
            return fixLength >= origin.length() ? origin : origin.substring(0, fixLength);
        }
        final StringBuffer fillString = new StringBuffer(fillLength);
        for (int i = 0; i < fillLength; i++) {
            fillString.append(fillChar);
        }
        if (rightFill) {
            buf.append(origin);
            buf.append(fillString);
        } else {
            buf.append(fillString);
            buf.append(origin);
        }
        return buf.toString();
    }

    /**
     * text with questionmarks (?). The questionmarks will be replaced by the toString() representation of the given
     * objects.
     * 
     * @param text text to replace the '?'
     * @param objects insertables
     * @return text with insertions
     */
    public static String insertObjects(String text, Object[] objects) {
        final String q = "?";
        int i = 0;
        int n = 0;
        Object obj;
        String toString;
        final StringBuffer t = new StringBuffer(text);
        while ((i = t.indexOf(q, i)) != -1 && n < objects.length) {
            obj = objects[n++];
            //to avoid a null-result if obj.toString() returns null, we call it twice
            toString = String.valueOf(String.valueOf(obj));
            t.replace(i, i + 1, toString);
        }
        return t.toString();
    }

    /**
     * text variables (starting with ${ and ending with }), will be replaced by the given properties (Map<Obbject,
     * Object> because of {@link Properties}).
     * 
     * @param text text to replace variables
     * @param properties insertables
     * @return text with insertions
     */
    public static String insertProperties(String text, Map<? extends Object, Object> properties) {
        int i = 0;
        final StringBuffer t = new StringBuffer(text);
        final Set<? extends Object> keySet = properties.keySet();
        for (final Object name : keySet) {
            final String vname = "${" + name + "}";
            i = t.indexOf(vname);
            if (i == -1) {
                continue;
            }
            final Object value = properties.get(name);
            t.replace(i, i + vname.length(), String.valueOf(value));
        }
        return t.toString();
    }

    /**
     * delegates to {@link #extract(CharSequence, String, String)} with null replacement
     */
    public static String extract(CharSequence source, String regexp) {
        return extract(source, regexp, null);
    }

    /**
     * extract regular expression
     * 
     * @param source source string. if it is an instanceof StringBuilder, the replacement can work.
     * @param regexp pattern
     * @param replacement (optional) all occurrencies of regexp will be replaced in source (only if StringBuilder!).
     * @return part of source or empty string
     */
    public static String extract(CharSequence source, String regexp, String replacement) {
        final Pattern p = Pattern.compile(regexp);
        final Matcher m = p.matcher(source);
        if (m.find()) {
            String result = m.group();
            if (replacement != null) {
                if (source instanceof StringBuilder) {
                    StringBuilder sb = (StringBuilder) source;
                    replace(sb, result, replacement);
                }
//                if (source instanceof StringBuffer) {
//                    StringBuffer sb = (StringBuffer) source;
//                    sb.delete(0, sb.length());
//                    sb.append(m.replaceAll(replacement));
//                }
            }
            return result;
        } else {
            return "";
        }
    }

//    /**
//     * extends {@link String#split(String)} to return at least src.
//     * @param src source
//     * @param regexp separator expression
//     * @return splitted string, at least src
//     */
//    String[] split(Object src, String regexp) {
//        String s = asString(src);
//        String[] split = s.split(regexp);
//        if (split.length == 0)
//            return new String[]{s};
//        else
//            return split;
//    }

    /**
     * splits and formats an object.
     * 
     * <pre>
     * example:
     * source = (Long)0123456789
     * betweenFiller = " "
     * splitIndexes = 3,7
     * 
     * result:
     * 012 3456 789
     * 
     * </pre>
     * 
     * @param source object to split and format
     * @param betweenFiller space filler
     * @param splitIndexes split indexes
     * @return formatted string
     */
    public static final String split(Object source, String betweenFiller, int... splitIndexes) {
        final String s = source.toString();
        final StringBuffer buf = new StringBuffer(s.length() + betweenFiller.length() * splitIndexes.length);
        int lastIndex = 0;
        for (int i = 0; i < splitIndexes.length; i++) {
            buf.append(s.substring(lastIndex, splitIndexes[i]) + betweenFiller);
            lastIndex = splitIndexes[i];
        }
        if (lastIndex < s.length() - 1) {
            buf.append(s.substring(lastIndex));
        }
        return buf.toString();
    }

    /**
     * Splits the given string to an array of string. The string is split every time its length is bigger than he given
     * maximum length.
     * 
     * <pre>
     * example:
     * source = "this is a test string"
     * maxLength = 6
     * 
     * result:
     * ["this i", "s a te", "st str", "ing"]
     * </pre>
     * 
     * @param source object to split
     * @param maxLength the maximum length after the string has to be split
     * @return
     */
    public static final String[] split(String source, int maxLength) {
        int length = source.length() / maxLength;
        if (source.length() % maxLength > 0) {
            length++;
        }
        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            result[i] = source.substring(i * maxLength, source.length() <= (i + 1) * maxLength ? source.length()
                : (i + 1) * maxLength);
        }
        return result;
    }

    /**
     * concats the given names into one string separated by 'sep'. if a name is null, it will be ignored.
     * 
     * @param sep separator
     * @param names names to combine
     * @return all names (without nulls)
     */
    public static final String concat(char[] sep, Object... names) {
        final StringBuffer buf = new StringBuffer(names.length * 15);
        final String ssep = String.valueOf(sep);
        for (int i = 0; i < names.length; i++) {
            if (names[i] != null)
                buf.append(names[i] + ssep);
        }
        return buf.length() > sep.length ? buf.substring(0, buf.length() - sep.length) : buf.toString();
    }

    public static final String findRegExp(String text, String regex, int start) {
        final Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find(start) ? matcher.group() : null;
    }

    /**
     * firstToUpperCase
     * 
     * @param string to convert
     * @return converted string
     */
    public static final String toFirstUpper(String string) {
        return String.valueOf(string.charAt(0)).toUpperCase() + string.substring(1, string.length());
    }

    /**
     * replaceNulls
     * 
     * @param strs array to replace the empty objects to nulls or vice versa
     * @param useNull if true, all empty objects will be replaced by nulls. if false, all nulls will be replaced to
     *            empty strings.
     */
    public static final void replaceNulls(Object[] strs, boolean useNull) {
        for (int i = 0; i < strs.length; i++) {
            if (strs[i] == null && !useNull)
                strs[i] = "";
            else if (strs[i] != null && strs[i].toString().length() == 0 && useNull)
                strs[i] = null;
        }
    }

    /**
     * getCryptoHash
     * 
     * @param data
     * @return
     */
    public static final byte[] cryptoHash(String data) {
        try {
            return Util.cryptoHash(data.getBytes("UTF-8"));
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * converts the given bytes to a hex string
     * 
     * @param bytes bytes to convert
     * @return hex string
     */
    public static final String toHexString(byte[] bytes) {
        return new BigInteger(1, bytes).toString(16);
    }
}
