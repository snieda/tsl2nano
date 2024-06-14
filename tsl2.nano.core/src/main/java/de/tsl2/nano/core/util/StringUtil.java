/*
 * 
 * 
 * Copyright ï¿½ 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.core.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tsl2.nano.autotest.creator.Expect;
import de.tsl2.nano.autotest.creator.Expectations;
import de.tsl2.nano.autotest.creator.InverseFunction;
import de.tsl2.nano.core.ITransformer;
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
    private static final int MAX_TRIES = 80;
	/** variable (like ant-variables) matching expression. e.g.: ${myvar} */
    public static final String VAR_REGEXP = "\\$\\{[\\w._-]+\\}";
    public static final String STR_ANY = "*";
    static String XTAG = "<[^>]*>";

    /**
     * @see #substring(String, String, String, int)
     */
    @Expectations({@Expect(when = {"something.. <content>..some other", "<", ">"}, then = "content")})
    public static String substring(String data, String from, String to) {
        return substring(data, from, to, 0);
    }

    /**
     * Does the same as substring, but considers outer encapsulating. means, it finds first match for 'from' it searches
     * the last match for 'to'.
     * 
     * @param constrain if true and from and to are not null, returns null if from or to was not found.
     * @return outer enclosing match.
     */
    // TODO: the expectations should have a then() = "<content>" (enclosing!) but doesn't!
    @Expectations({@Expect(when = {"something.. <content>..some other", "<", ">", "false"}, then = "content")})
    public static String subEnclosing(String data, String from, String to, boolean constrain) {
        if (from == null && to == null) {
            return constrain ? null : data;
        } else if (from == null) {
            return substring(data, from, to, true, true);
        } else if (to == null) {
            return substring(data, from, to, 0, true);
        } else {
            int iFrom = data.indexOf(from);
            int iTo = data.lastIndexOf(to);
            if (constrain && iFrom == -1 || iTo == -1) {
                return null;
            } else {
                return data.substring(iFrom != -1 ? iFrom + from.length() : 0, iTo != -1 ? iTo : data.length());
            }
        }
    }

    /** delegates to {@link #substring(String, String, String, int)} interpreting from and to as regurlar expressions. */
    public static String subRegex(String data, String from, String to, int start) {
    	return substring(data, 
    			from != null ? extract(data.substring(start), from) : null, 
    			to   != null ? extract(data.substring(start), to) : null, 
    					start);
    }

    /** delegates to {@link #substring(String, String, String, int)} interpreting fromRegex as regurlar expressions. */
    public static String subRegexFrom(String data, String fromRegex, String to, int start) {
    	return substring(data, 
    			fromRegex != null ? extract(data.substring(start), fromRegex) : null, to, start);
    }

    /** delegates to {@link #substring(String, String, String, int)} interpreting toRegex as regurlar expressions. */
    public static String subRegexTo(String data, String from, String toRegex, int start) {
    	return substring(data, 
    			from, toRegex != null ? extract(data.substring(start), toRegex) : null, start);
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
        if (i < 0 && constrain) {
            return null;
        }
        i = i == -1 ? 0 : i;
        if (from != null || i == 0) {
            return substring(data, from, to, i, constrain);
        } else {
            // if 'last' is used for 'to', we return directly the substring
            return data.substring(0, i);
        }
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
     * @param constrain if true, null will be returned if @from or @to couldn't be found
     * @return extracted substring
     */
    public static String substring(String data, String from, String to, int start, boolean constrain) {
        if (from == null && to == null) {
            return constrain ? null : data;
        }
        if (from == null) {
            final int i = data.indexOf(to, start);
            if (i < 0) {
                if (constrain) {
                    return null;
                } else {
                    return data.substring(start);
                }
            }
            return data.substring(start, i);
        } else if (to == null) {
            final int i = data.indexOf(from, start);
            if (i < 0) {
                if (constrain) {
                    return null;
                } else {
                    return data.substring(start);
                }
            }
            return data.substring(i + from.length());
        } else {
            int i = data.indexOf(from, start);
            if (i < 0) {
                if (constrain) {
                    return null;
                }
                from = "";//-->length = 0
                i = start;
            }
            int j = data.indexOf(to, i + (from.length() > 0 ? from.length() : 1));
            if (j < 0) {
                if (constrain) {
                    return null;
                }
                j = data.length();
            }
            return data.substring(i + from.length(), j);
        }
    }

    /**
     * @see #substring(StringBuilder, String, String, int)
     */
    @Expectations({@Expect(when = {"something.. <content>..some other", "<", ">"}, then = "content")})
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
    // TODO: not a real trim(): each character will be 'trimmed' only once!
    @Expectations({@Expect(when = {" .<content>. ", " .<>"}, then = "content")})
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
        while (i < src.length() && src.charAt(i) == c) {
            src.deleteCharAt(i++);
        }
        //at the end
        i = src.length();
        while (i > 0 && src.charAt(--i) == c) {
            src.deleteCharAt(i);
        }
    }

//    @Expectations({@Expect(when = {"something.. <content>..some other", "<content>", "[new]"}, then = "something.. [new]..some other")})
    public static void replace(StringBuilder str, String expression, String replacement) {
        replace(str, expression, replacement, 0);
    }

    /**
     * replaces all occurrences of expression in str with replacement. if nothing was found, nothing will be done.
     * 
     * @param str source
     * @param expression expression to find
     * @param replacement replacement
     */
    public static void replace(StringBuilder str, String expression, String replacement, int start) {
        int i = str.indexOf(expression, start);
        if (i != -1) {
            str.replace(i, i + expression.length(), replacement);
        }
    }

    public static String replaceAll(CharSequence src, String regex, ITransformer<String, String> transformer) {
        return replaceAll(src, regex, 0, transformer);
    }

    /**
     * replaces all matches of regex in source src calling the callback {@link ITransformer#transform(String)} of your
     * given transformer.
     * 
     * @param src source string
     * @param regex regular expression to be matched before replace
     * @param transformer callback to replace matches.
     * @return transformed string
     */
    public static String replaceAll(CharSequence src, String regex, int group, ITransformer<String, String> transformer) {
        Matcher matcher = Pattern.compile(regex).matcher(src);
        StringBuffer result = new StringBuffer(src.length());
        while (matcher.find()) {
            //while appendReplacement seems to block strings having special characters like '$', '{', the appending is done on #append()
            matcher.appendReplacement(result, "");
            result.append(transformer.transform(matcher.group(group)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * @param src StringBuffer in cause of Matcher only working on StringBuffer directly
     * @param regex regular expression
     * @param replacement replacement
     * @return count of findings
     */
    // TODO: wrong replacement, see @Expect -> otherther
//    @Expectations({@Expect(when = {"something.. <content>..some other", "<content>", "[new]"}, resultIndex = 0, then = "something.. [new]..some other")})
    public static int replaceAll(StringBuilder src, String regex, String replacement) {
    	int count = 0;
        Matcher matcher = Pattern.compile(regex).matcher(src);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, replacement);
            count++;
        }
        matcher.appendTail(sb);
        src.setLength(0);
        src.append(sb);
        return count;
    }
    
    public static String toStringCut(Object obj, int len) {
    	String s = String.valueOf(obj);
    	return s.length() > len ? s.substring(0, Math.min(s.length(), len)) : s;
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
    @Expectations({@Expect(when = {"something.. <content>..some other", "9"}, then = "someth...")})
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
    @Expectations({@Expect(when = {"something.. <content>..some other", "1"}, then = " [0]: something.. <content>..some other")})
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
    	return toFormattedString(o, maxLineCount, showLines, "\n");
    }
    public static String toFormattedString(Object o, int maxLineCount, boolean showLines, String sep) {
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
                strBuilder.append((showLines ? " [" + i + "]: " : "") + array[i] + (i<c-1 ? sep : ""));
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
     * delegates to {@link #fixString(String, int, char, boolean)}.
     */
    @Expectations({@Expect(when = {"10", "c"}, then = "cccccccccc")})
    public static String fixString(int fixLength, char fillChar) {
        return fixString("", fixLength, fillChar, true);
    }

    public static String fixString(Object origin, int fixLength) {
    	return fixString(origin.toString(), fixLength, ' ', true);
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
     * delegates to {@link #insertProperties(String, Map, String, String)} using standard ant syntax.
     */
    public static String insertProperties(String text, Map<? extends Object, Object> properties) {
        return insertProperties(text, properties, "${", "}");
    }

    /**
     * text variables (starting with ${ and ending with }), will be replaced by the given properties (Map<Obbject,
     * Object> because of {@link Properties}).
     * 
     * @param text text to replace variables
     * @param properties insertables
     * @return text with insertions
     */
    public static String insertProperties(String text,
            Map<? extends Object, Object> properties,
            String key_prefix,
            String key_postfix) {
        int i = 0;
        Object value;
        String vname;
        final StringBuffer t = new StringBuffer(text);
        final Set<? extends Object> keySet = properties.keySet();
        for (final Object name : keySet) {
            vname = key_prefix + name + key_postfix;
            while ((i = t.indexOf(vname)) != -1) {
                value = properties.get(name);
                t.replace(i, i + vname.length(), String.valueOf(value));
            }
        }
        return t.toString();
    }

    /** returns the index of the first occurrency of given regular expression after start index */
    public static int indexOf(String src, String regex, int start) {
    	String f = extract(src.substring(start), regex);
    	return src.indexOf(f, start);
    }
    
    /**
     * extracts all expressions found. On StringBuilder/StringBuffer the regexp was replaced with "".
     * 
     * @param source to be searched on
     * @param regexp to be found
     * @param groups group to use
     * @return all found regexp entries.
     */
    public static String[] extractAll(CharSequence source, String regexp, int... groups) {
        String e;
        List<String> all = new LinkedList<String>();
        int i = 0;
        while (!Util.isEmpty((e = extract(source, regexp, "", i, groups)))) {
            all.add(e);
            if (source instanceof String) //on StringBuilder the extracted string was replaced with ""
                i = ((String) source).indexOf(e, i) + e.length();
        }
        return all.toArray(new String[0]);
    }

    /**
     * delegates to {@link #extract(CharSequence, String, String)} with null replacement
     */
    public static String extract(CharSequence source, String regexp, int... groups) {
        return extract(source, regexp, null, 0, groups);
    }

    /**
     * delegates to {@link #extract(CharSequence, String, String, int, int...)}
     */
    @Expectations({@Expect(when = {"something.. <content>..some other", "[<].*[>]", "[\\1]"}, then = "<content>")})
    public static String extract(CharSequence source, String regexp, String replacement) {
        return extract(source, regexp, replacement, 0, 0);
    }

    /**
     * extract regular expression. if you use brackets, the last group (represented by brackets) will be extracted.
     * <p/>
     * Example 1:
     * 
     * <pre>
     * String url = &quot;jdbc:mysql://db4free.net:3306/0zeit&quot;;
     * String port = StringUtil.extract(url, &quot;[:](\\d+)[:/;]\\w+&quot;);
     * assertEquals(&quot;3306&quot;, port);
     * </pre>
     * 
     * @param source source string. if it is an instanceof StringBuilder, the replacement can work.
     * @param regexp pattern
     * @param replacement (optional) all occurrences of regexp will be replaced in source (only if source is of type
     *            StringBuilder!).
     * @param start start index to search and/or replace
     * @param groups (ignored, if source is a StringBuilder) group numbers to concat. if empty, the last group will be
     *            returned
     * @return part of source or empty string
     */
    public static String extract(CharSequence source, String regexp, String replacement, int start, int... groups) {
        final Pattern p = Pattern.compile(regexp);
        final Matcher m = p.matcher(source);
        if (m.find(start)) {
            String result;
            if (replacement != null) {
                if (source instanceof StringBuilder) {
                    result = m.group(m.groupCount());
                    //if no match was done on the last group, we use the whole match
                    if (result == null || m.groupCount() > 0)
                        result = m.group(0);
                    if (result != null) {
                        StringBuilder sb = (StringBuilder) source;
                        replace(sb, result, replacement, start);
                    }
                } else {
                    result = concatGroups(m, groups);
                }
//                if (source instanceof StringBuffer) {
//                    StringBuffer sb = (StringBuffer) source;
//                    sb.delete(0, sb.length());
//                    sb.append(m.replaceAll(replacement));
//                }
            } else {
                result = concatGroups(m, groups);
            }
            return result;
        } else {
            return "";
        }
    }

    private static final String concatGroups(Matcher m, int[] groups) {
        if (groups.length == 0)
            return m.group(m.groupCount());
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < groups.length; i++) {
            buf.append(m.group(groups[i]));
        }
        return buf.toString();
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
            if (s.length() < splitIndexes[i]) {
                break;
            }
            buf.append(s.substring(lastIndex, splitIndexes[i]) + betweenFiller);
            lastIndex = splitIndexes[i];
        }
        if (lastIndex < s.length() - 1) {
            buf.append(s.substring(lastIndex));
        }
        return buf.toString();
    }

    /** 
     * splits the given string in the order of the given separator/splitter (should be unique!) strings. 
     * Not performance optimized! <p/>
     * @param source text to be splitted
     * @param regex whether to use the extended regular expression mechanism, if a splitter contains a regex marker '^'
     * @param splitter short strings to split the source. if marked with '^' and regex==true, it will be interpreted as regular expression. if a part of a splitter is surrounded with '°', this part will be ignored.
     * @return splitted source or null, if no split found
     * @throws IllegalStateException if one split failed
     * <pre>
     * Example:
     * source: 21.06.: 07:30-17:00(0,5h)  9,0h TICKET-123 Analyse
     * regex: false
     * splitter: [: ,-,(,)  , , ]
     * 
     * will split the given string into a date, fromtime, totime, pause, duration, ticketname and description.
     * 
     * If you need more flexibility, in cause of different input formats, you may use a regular expression and/or ignore-definitions.
     * 
     * source: 21.06.: 07:30-17:00(0,5h)  9,0h TICKET-123 Analyse
     * regex: true
     * splitter: [:, -, (, ^\\)\\:? ^, °:°,  ]
     * 
     * there the text may have ':' optional on two splits.
     * 
     * </pre>
     */
    public static final String[] splitFix(String source, boolean regex, String...splitter) {
    	final String REG_MARKER = Util.get("tsl2nano.string.split.regex.marker", "^");
    	final String IGN_MARKER = Util.get("tsl2nano.string.split.regex.marker", "°");
    	String[] s = new String[splitter.length + 1];
    	String last = null, ll = null, split, ignore = null;
    	int pos=0, lastpos=-1;
    	boolean regexFrom = false, regexTo = false;
    	for (int i = 0; i < s.length; i++) {
			split = i < splitter.length ? splitter[i] : null;
			if (split != null) {
				regexTo = regex && (split.startsWith(REG_MARKER) || split.endsWith(REG_MARKER));
				if (regexTo)
					split = split.replace(REG_MARKER, "");
				ignore = StringUtil.substring(split, IGN_MARKER, IGN_MARKER, false, true);
				if (ignore != null) {
					split = split.replace(IGN_MARKER + ignore + IGN_MARKER, "");
				}
			}
			last = substringEx(source, ll, split, pos, regexFrom, regexTo);
			int t = 0;
			while (Util.isEmpty(last) && t++ < MAX_TRIES) { //end directly after begin -> search for the next occurrence
				last = substringEx(source, ll +=split, split, pos, regexFrom, regexTo);
			}
			if (t == MAX_TRIES)
				throw new IllegalStateException("split " + i + ":'" + split + "'not found!");
			s[i] = ignore == null ? last.trim() : last.trim().replace(ignore, "");
			ll = ignore == null ? last + split : last.replace(ignore, "") + split;
			pos = source.indexOf(last, pos);
			if (pos < 0)
				throw new IllegalStateException("'" + last + "' on split: '" + split + "' + not found in data:" + source );
			if (i == 1 && lastpos == pos)
				return null;
			regexFrom = regexTo;
			lastpos = pos;
		}
    	return s;
    }

	private static String substringEx(String source, String ll, String split, int pos, boolean regexFrom, boolean regexTo) {
		return regexFrom && regexTo 
				? subRegex(source, ll, split, pos)
				: regexFrom
					? subRegexFrom(source, ll, split, pos) 
					: regexTo
						? subRegexTo(source, ll, split, pos)
						: substring(source, ll, split, pos);
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

    /** @return splitted src with given regEx that is not found inside of doublequotes */
    public static final String[] splitOutsideOfQuotations(String src, String regEx) {
        return src.split(regEx + "(?=(?:(?:(?:[^\"\\]++|\\.)*+\"){2})*+(?:[^\"\\]++|\\.)*+$)");
    }

    /**@param txt source string to split
     * @param splitter character set. splits for any character in the character set 
     * @return splitted string - but respecting double-quotes and brackets */
    public static String[] splitUnnested(String txt, String splitter) {
        List<String> lsplit = new LinkedList<>();
        String open = "<{[(", close = ")]}>";
        int begin = 0;
        String s;
        boolean inQuotes = false;
        int brackets = 0;
        for (int i = 0; i < txt.length(); i++) {
            s = String.valueOf(txt.charAt(i));
            if (splitter.contains(s) && !inQuotes && brackets < 1) {
                lsplit.add(txt.substring(begin, i));
                begin = i + 1;
            } else {
                inQuotes = (s.equals("\"") && !inQuotes) || (!s.equals("\"") && inQuotes);
                brackets = open.contains(s) ? ++brackets : close.contains(s) && brackets > 0 ? --brackets : brackets;
            }
        }
        if (begin < txt.length())
            lsplit.add(txt.substring(begin));
        return lsplit.toArray(new String[0]);
    }

    public static final String[] splitWordBinding(String word) {
        return word.split("[-./]");
    }

    /**
     * splits a name with camel-case concatenation into an array of names.
     * <p/>
     * Attention: this will replace newlines witch spaces!
     * 
     * @param ccName text to split
     * @return split ccName
     */
    public static final String[] splitCamelCase(String ccName) {
        return spaceCamelCase(ccName).split("\\s");
    }

    /**
     * splits a name with camel-case concatenation into an array of names.
     * 
     * @param ccName text to split
     * @return split ccName
     */
    @Expectations({@Expect(when = {"spaceCamelCase"}, then = "space Camel Case")})
    public static final String spaceCamelCase(String ccName) {
        return ccName.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
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
            if (names[i] != null) {
                buf.append(names[i] + ssep);
            }
        }
        return buf.length() > sep.length ? buf.substring(0, buf.length() - sep.length) : buf.toString();
    }

    /**
     * concats the given names into one string, while each name is wrapped into 'wrap'.
     * {@link MessageFormat#format(String, Object...)} is used, so wrap should contain at least '{0}'.if a name is null,
     * it will be ignored.
     * 
     * @param wrap name wrapper. e.g.: '${{0}}'
     * @param names names to combine
     * @return all names (without nulls)
     */
    public static final String concatWrap(char[] wrap, Object... names) {
        final StringBuffer buf = new StringBuffer(names.length * 15);
        final String wwrap = String.valueOf(wrap);
        for (int i = 0; i < names.length; i++) {
            if (names[i] != null) {
                buf.append(MessageFormat.format(wwrap, names[i]));
            }
        }
        return buf.toString();
    }

    /**
     * delegates to {@link #format(String, int, String, String)}
     */
    @Expectations({@Expect(when = {"something.. <content>..some other", "9"}, then = "something\n.. <conte\nnt>..some\n other")})
    public static final String format(String text, int maxLineWidth) {
        return format(text, maxLineWidth, "\n");
    }

    /**
     * formats the given text into given line length
     * 
     * @param text text to format
     * @param maxLineWidth maximum line width
     */
    public static final String format(String text, int maxLineWidth, String CR) {
        String items[] = text.split(CR);
        ArrayList<String> lines = new ArrayList<String>();
        for (int i = 0; i < items.length; i++) {
            lines.addAll(Arrays.asList(StringUtil.split(items[i], maxLineWidth)));
        }
        return StringUtil.concat(CR.toCharArray(), lines.toArray());
    }

    /**
     * finds the regex in the given text, starting from start
     * @return returns the first matching substring or null
     */
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
     * firstToLowerCase
     * 
     * @param string to convert
     * @return converted string
     */
    public static final String toFirstLower(String string) {
        return String.valueOf(string.charAt(0)).toLowerCase() + string.substring(1, string.length());
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
            if (strs[i] == null && !useNull) {
                strs[i] = "";
            } else if (strs[i] != null && strs[i].toString() != null && strs[i].toString().length() == 0 && useNull) {
                strs[i] = null;
            }
        }
    }

    /**
     * delegates to {@link Util#cryptoHash(byte[])}
     */
    public static final byte[] cryptoHash(String data) {
        return Util.cryptoHash(data.getBytes());
    }

    /**
     * creates a hash for the given data. use {@link #toHexString(byte[])} to convert the result to a more readable
     * string.
     * 
     * @param data data to hash
     * @param algorithm one of MD2, MD5, SHA, SHA-1, SHA-256, SHA-384, SHA-512
     * @return hashed data encoded with UTF-8
     */
    public static final byte[] cryptoHash(String data, String algorithm) {
        try {
            return Util.cryptoHash(data.getBytes("UTF-8"), algorithm);
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

    /**
     * converts the given bytes to a decimal string
     * 
     * @param bytes bytes to convert
     * @return decimal string
     */
//    @InverseFunction(methodName = "toHexString", parameters = {byte[].class})
    public static String fromHexString(String hex) {
        return fromBaseString(hex, 16);
    }

    public static boolean isHexString(String txt) {
        char[] carray = txt.toCharArray();
        for (int i = 0; i < carray.length; i++) {
            if (Character.digit(carray[i], 16) == -1)
                return false;
        }
        return true;
    }
    
    public static String toBase64(Object txt) {
    	return toBase64(txt.toString().getBytes());
    }
    public static String toBase64(byte[] raw) {
        return Base64.getEncoder().encodeToString(raw);
    }
    public static String fromBase64(String encoded) {
        return new String(Base64.getDecoder().decode(encoded));
    }

    
    public static String fromBaseString(String hex, int base) {
        StringBuilder buf = new StringBuilder(hex.length());
        for (int i = 0; i < hex.length(); i += 2) {
            buf.append((char) Integer.parseInt(hex.substring(i, i + 2), base));
        }
        return buf.toString();
    }

    public static final String toDecString(byte[] bytes) {
        return new BigInteger(1, bytes).toString(10);
    }

    public static String fromDecString(String number) {
        return fromBaseString(number, 10);
    }
    
    /**
     * cuts the given string to have a maximum length of maxLength characters.
     * 
     * @param name source
     * @param maxLength max length
     * @return new cut string.
     */
    @SuppressWarnings("unchecked")
    public static <T extends CharSequence> T cut(T name, int maxLength) {
        return (T) (name.length() > maxLength ? name.subSequence(0, maxLength) : name);
    }

    /**
     * like an xml2text this method tries to remove all xml tags and returns the pure text content.
     * 
     * @param xmlContent xml string
     * @return pure text with carriage returns
     */
    public static String removeXMLTags(String xmlContent) {
        return xmlContent.replaceAll("[\n]?" + XTAG + "(\\w*)" + XTAG, "\n\1");
    }

    /**
     * convenience to put a text into an inputstream
     */
    public static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    @InverseFunction(methodName = "toInputStream", parameters = {String.class}, compareParameterIndex = 0)
	public static String fromInputStream(InputStream stream) {
		return fromInputStream(stream, "");
	}
	public static String fromInputStream(InputStream stream, String lineEnd) {
		return fromInputStream(stream, "", lineEnd);
	}
	
	/**
	 * @param stream input stream to follow and close
	 * @param lineStart starting sequence on each new line. use "" if not needed
	 * @param lineEnd ending sequence on each new line. use "" if not needed
	 * @return string, scanned from input
	 */
	public static String fromInputStream(InputStream stream, String lineStart, String lineEnd) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(stream);
			StringBuilder buf = new StringBuilder();
			while (scanner.hasNextLine()) {
				buf.append(lineStart + scanner.nextLine() + lineEnd);
			}
			return buf.toString();
		} finally {
			if (scanner != null)
				scanner.close();
		}
	}

    /**
     * search for the given filter. the filter will be divided to its characters. an 100 percent match is done, if all
     * characters where found as sequence.
     * 
     * @TODO: perhaps this should be moved to the FuzzyFinder class.
     * @param item holding the content to be matched through the given filter. if null, weight is 0.
     * @param filter stream of characters to be found in item. if null, weight is 1.
     * @return weight of match. 0: not all characters of the given filter were found. 1: all characters were found as
     *         sequence.
     */
    public static double fuzzyMatch(Object item, String filter) {
        if (filter == null)
            return 1;
        if (item == null)
            return 0;
        double weight = 1;
        String content = item.toString().toLowerCase();
        filter = filter.toLowerCase();
//        if (content.contains(filter))
//            return 1;
        int la = -1; //last index
        int lb; //new index
        for (int i = 0; i < filter.length(); i++) {
            lb = content.indexOf(filter.charAt(i), la + 1);
            if (lb < 0)
                return 0;
            weight /= la < 0 ? 1 : (lb - la);
            la = lb;
        }
        return weight;
    }

    /**
     * removes all characters like tabs and CRs.
     * 
     * @param src
     * @return
     */
    public static String removeFormatChars(String src) {
        if (src == null)
            return null;
        return src.replaceAll("[\t\r\n]+", "");
    }
    
    public static String printToString(Consumer<PrintWriter> c) {
    	StringWriter sw = new StringWriter();
    	PrintWriter pw = new PrintWriter(sw);
    	c.accept(pw);
    	return sw.toString();
    }
    public static int countFindings(String data, String search) {
    	int c = 0;
    	int i, last = 0;
    	int ll = search.length();
		do {
    		i = data.indexOf(search, last);
    		if (i == -1)
    			break;
    		last = i + ll;
    		++c;
    	} while (true);
		return c;
    }

    /**
     * 
     * @param txt text to convert to a valid name
     * @return valid java class or variable name with camel-case
     */
    public static String toValidName(String txt) {
        // return txt.replaceAll("[^\\w\\d]+([\\w\\d])", "\\U$1\\E");
        return StringUtil.replaceAll(txt, "[^\\w\\d]+([\\w\\d])", 1, s -> s.toUpperCase());
    }

	public static String[] trim(String[] args) {
		for (int i = 0; i < args.length; i++) {
			args[i] = args[i].trim();
		}
		return args;
	}

	public static String matchingOneOf(Object...matchers) {
		StringBuilder buf = new StringBuilder(".*(");
		for (int i = 0; i < matchers.length; i++) {
			buf.append(matchers[i] + (i < matchers.length - 1 ? "|" : ""));
		}
		return buf.append(").*").toString();
	}
	
	public static int maxLength(Object...objs) {
		int maxLength = 0, l;
		for (int i = 0; i < objs.length; i++) {
			maxLength = (l = objs.toString().length()) > maxLength ? l : maxLength;
		}
		return maxLength;
	}

    public static boolean isXml(String txt) {
        return txt != null && (txt.contains("</") || txt.contains("/>"));
    }
}
