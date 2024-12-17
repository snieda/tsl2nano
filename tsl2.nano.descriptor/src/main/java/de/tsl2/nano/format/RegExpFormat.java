/*
 * 
 * 
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.format;

import static de.tsl2.nano.core.util.FormatUtil.getCurrencyFormatNoFraction;
import static de.tsl2.nano.core.util.FormatUtil.getCurrencyFormatNoSymbol;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Complete;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.collection.ReferenceMap;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.RegExUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.currency.CurrencyUtil;
import de.tsl2.nano.util.operation.IConverter;

/**
 * provides standard formatting through regular expressions. usable to verify input in a text component. the
 * parseObject() isn't able to return the desired instance!
 * 
 * if you only want to format or parse standard numbers or dates, then use the standard formatters like NumberFormat and
 * DateFormat.
 * 
 * This formatter is intended to be used to constrain or verify user input trough an input mask.
 * 
 * four standard expressions are available:
 * <ul>
 * <li>standard text</li>
 * <li>numbers</li>
 * <li>dates</li>
 * <li>length</li>
 * </ul>
 * 
 * @author ts 18.09.2008
 * @version $Revision: 1.0 $
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RegExpFormat extends Format implements INumberFormatCheck {
    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(RegExpFormat.class);
    @Element(data = true, required=false)
    private String pattern;
    @Attribute
    private int regExpFlags;
    @Attribute
    private int maxCharacterCount = -1;
    private transient Pattern compiledPattern;
    /** needed to format objects */
    @Element(name = "parser", required = false)
    protected GenericParser<?> parser = null;

    /** default: the full regexp must be matched on format or parse! */
    @Attribute
    boolean fullMatch = true;
    /** on default, this class is not able to parse a string to an object */
    @Attribute
    boolean isAbleToParse = false;

    public static final char DECIMAL_SEPARATOR() {return new DecimalFormatSymbols(Locale.getDefault()).getDecimalSeparator();}
    public static final char GROUPING_SEPARATOR() {return new DecimalFormatSymbols(Locale.getDefault()).getGroupingSeparator();}
    /** characters only to format an expression (like german date '01.01.2001' the dots) */
    public static final String FORMAT_CHARACTERS = " .,;_/*#|-:";

    /** alphanumeric character with lowest ascii code (=32) */
    public static final char MIN_CHAR = ' ';
    /** alphanumeric character with highest ascii code (=126) */
    public static final char MAX_CHAR = '~';

    /**
     * this pattern is not only a number format, but a pattern from nothing up to the desired number. E.g.: -999,999 -->
     * you start with '-', than you type the numbers!
     */
    public static final String FORMAT_DECIMAL() {return  "[-][0-9]{0,2}[" + DECIMAL_SEPARATOR() + "][0-9]{0,2}";}

    /**
     * All single byte characters, for applications / RDBMS that do not support multibyte/unicode characters.<br>
     * Note that the Posix pattern matcher "\\p{ASCII}" only covers characters up to \x7F, so for example no German
     * "Umlaute".
     */
    public static final String PATTERN_SINGLE_BYTE = "[\\x00-\\xFF]";

    public static final String PATTERN_SINGLE_BYTE_SPACE = "[\\x00-\\xFF €]";

    // TODO how to get the date format from locale? 
    public static final String FORMAT_DATE_SQL = "[1-2]\\d\\d\\d\\-[0-1]\\d\\-[0-3]\\d";
    public static final String FORMAT_DATE_EN = "[1-2]\\d\\d\\d\\ [A-Z]{3} [0-3]\\d";
    public static final String FORMAT_DATE_DE = "[0-3]\\d\\.[0-1]\\d(\\.([1-2]\\d\\d\\d)?)?";
    public static final String FORMAT_TIME = "[0-2]\\d\\:[0-5]\\d(\\:[0-5]\\d)?";
    public static final String FORMAT_TIME_EN = "\\d?\\d\\:[0-5]\\d(\\:[0-5]\\d)?(\\s[AP]M)?";
    public static final String FORMAT_DATETIME = FORMAT_DATE_SQL + "([,;|\\/]?\\s" + FORMAT_TIME + ")?";
    public static final String FORMAT_DATETIME_DE = FORMAT_DATE_DE + "([,;|\\/]?\\s" + FORMAT_TIME + ")?";
    public static final String FORMAT_DATETIME_EN = FORMAT_DATE_EN + "([,;|\\/]?\\s" + FORMAT_TIME + ")?";
    public static final String FORMAT_NAME_ALPHA_DE = "[a-zA-ZäöüÄÖÜß]+";
    public static final String FORMAT_NAME_ALPHA_EXT_DE = FORMAT_NAME_ALPHA_DE + PATTERN_SINGLE_BYTE_SPACE;
    public static final String FORMAT_NAME_ALPHA = "[a-zA-Z]*";
    public static final String FORMAT_NAME_ALPHA_NUM = "[a-zA-Z0-9]*";
    public static final String FORMAT_NUMBER = "[0-9]*";

    @Deprecated //in versions before 2.4.8 this was the FORMAT_DATE_DE. some applications have presenters with this format
    private static final String DEPRECATED_FORMAT_DATE_DE = "[0-3]\\d\\.[0-1]\\d(\\.[1-2]\\d\\d\\d)?";

    private static final Map<String, String> systemInitMap = new Hashtable<String, String>();
    @ElementMap(attribute = true, inline = true, required = false, name = "initmask", key = "pattern")
    Map<String, String> initMap = null;
    static {
        try {
            /*
             * standard german and english date format mask
             */
            systemInitMap.put(FORMAT_DATE_DE, createDateMask(Locale.GERMANY));
            systemInitMap.put(DEPRECATED_FORMAT_DATE_DE, createDateMask(Locale.GERMANY));
            systemInitMap.put(FORMAT_DATE_SQL, createDateMask(Locale.ENGLISH));
            /*
             * on german dates with month-day 31, the standard-initmask '01.01.CURRENTYEAR' does not work.
             * example: input: 31.1 and mask: 01.01.2011 ==> 31.11.2011 ==> error
             */
            systemInitMap.put("31.1", DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMANY).format(getInitialDate_Nov()));
            /*
             * standard decimal number
             */
            systemInitMap.put(FORMAT_DECIMAL(), "00" + DECIMAL_SEPARATOR() + "00");
            /*
             * standard time
             */
            systemInitMap.put(FORMAT_TIME, "00:00:00");
            systemInitMap.put(FORMAT_TIME_EN, "0:00:00 AM");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String getDateFormatExpression() {
        return isGermanLocale() ? FORMAT_DATE_DE : RegExUtil.createSimpleRegEx(createDateMask());
    }

    static String getTimeFormatExpression() {
        return isGermanLocale() ? FORMAT_TIME : RegExUtil.createSimpleRegEx(createDateMask(DateFormat.getTimeInstance()));
    }

    static String getDateTimeFormatExpression() {
        return isGermanLocale() ? FORMAT_DATETIME_DE : RegExUtil.createSimpleRegEx(createDateMask(DateFormat.getDateTimeInstance()));
    }

    static boolean isGermanLocale() {
        return Locale.getDefault().equals(Locale.GERMANY);
    }
    static String createDateMask() {
        return createDateMask(Locale.getDefault());
    }

    static String createDateMask(Locale loc) {
        return createDateMask(DateFormat.getDateInstance(DateFormat.MEDIUM, loc));
    }

    static String createDateMask(DateFormat df) {
        String s = df.format(getInitialDate());
        // on some formats, single numbers on month or days are allowed
        s = s.replaceAll("(^|[^\\d])(\\d)([^\\d]|$)", "$10$2$3");
        return s;
    }

    static Date getInitialDate() {
//        return new Date();
        /*
         * current date may be a problem on months, having less than 31 days.
         * we give 01.01.CURRENT_YEAR
         */
        return DateUtil.getDate(DateUtil.getYear(new Date()), 1, 1);
    }

    static Date getInitialDate_Nov() {
//      return new Date();
        /*
         * current date may be a problem on months, having less than 31 days.
         * we give 01.10.CURRENT_YEAR
         */
        return DateUtil.getDate(DateUtil.getYear(new Date()), 10, 1);
    }

    /**
     * constructor to be de-serializable
     */
    public RegExpFormat() {
        super();
    }

    /**
     * simple constructor without length argument. only usable on simple patterns without length definitions (like
     * {0,5}).
     * 
     * @param pattern regexp without length def.
     */
    protected RegExpFormat(String pattern) {
        this(pattern, null, calcLength(pattern, null), 0);
    }

    /**
     * simple constructor without length argument. only usable on simple patterns without length definitions (like
     * {0,5}).
     * 
     * @param pattern regexp without length def.
     * @param init init string
     */
    public RegExpFormat(String pattern, String init) {
        this(pattern, init, calcLength(pattern, init), 0);
    }

    /**
     * Constructor
     * 
     * @param pattern regular expression to check
     * @param maxCharacterCount maximum allowed count of characters
     * @param regExpFlags additional Flags for the regular expression
     * @see Pattern Pattern for Flags description
     */
    public RegExpFormat(String pattern, int maxCharacterCount, int regExpFlags) {
        this(pattern, null, maxCharacterCount, regExpFlags);
    }

    /**
     * Constructor
     * 
     * @param pattern regular expression to check
     * @param maxCharacterCount maximum allowed count of characters
     * @see Pattern Pattern for Flags description
     */
    public RegExpFormat(String pattern, int maxCharacterCount) {
        this(pattern, null, maxCharacterCount, 0);
    }

    /**
     * Constructor
     * 
     * @param pattern regular expression to check
     * @param maxCharacterCount maximum allowed count of characters
     * @param regExpFlags additional Flags for the regular expression
     * @see Pattern Pattern for Flags description
     */
    public RegExpFormat(String pattern, String init, int maxCharacterCount, int regExpFlags) {
        this(pattern, init, maxCharacterCount, regExpFlags, (Format) null);
    }

    /**
     * Constructor
     * 
     * @param pattern regular expression to check
     * @param init default init string
     * @param maxCharacterCount maximum allowed count of characters
     * @param regExpFlags additional Flags for the regular expression
     * @param parser (optional) format instance to be set as default format and parser
     * @see Pattern Pattern for Flags description
     */
    public RegExpFormat(String pattern, String init, int maxCharacterCount, int regExpFlags, Format parser) {
        super();
        this.pattern = pattern;
        if (init != null) {
            initMap = new Hashtable<String, String>();
            initMap.putAll(systemInitMap);
            initMap.put(pattern, init);
        } else {//to avoid using more resources, we use the static map
            initMap = systemInitMap;
        }

        this.maxCharacterCount = maxCharacterCount;
        this.regExpFlags = regExpFlags;
        if (parser != null && !(parser instanceof RegExpFormat)) {
            isAbleToParse = true;
            this.parser = (GenericParser<?>) (parser instanceof GenericParser ? parser : new GenericParser(parser));
        }
    }

    /**
     * init
     * 
     * @param pattern regular expression to check
     * @param maxCharacterCount maximum allowed count of characters
     * @param regExpFlags additional Flags for the regular expression
     * @see Pattern Pattern for Flags description
     */
    protected void init(String pattern, String init, int maxCharacterCount, int regExpFlags) {
        this.pattern = pattern;
        if (init != null) {
            if (initMap == systemInitMap) {
                initMap = new HashMap<String, String>();
            }
            initMap.put(pattern, init);
        }
        this.maxCharacterCount = maxCharacterCount;
        this.regExpFlags = regExpFlags;
        compiledPattern = null;
    }

    /**
     * tries to calculate the length through the given pattern. if the pattern contains brackets, no length will be
     * calculated
     * 
     * @param pattern pattern to evaluate
     * @return pattern length, or throw exception, if containing brackets
     */
    private static int calcLength(String pattern, String init) {
        if (init != null && init.length() > 0) {
            return init.length();
        }
        if (pattern.indexOf('{') == -1 && pattern.indexOf('[') == -1 && pattern.indexOf('(') == -1) {
            return pattern.length();
        }
//        throw new ManagedException("tsl2nano.implementationerror", new Object[] { pattern,
//            "only simple patterns without length definitions (like {0,5}) are allowed!" });
        int length = ENV.get("field.pattern.regexp.default.length", 64);
        LOG.warn("can't calculate pattern lenght for " + pattern + " --> using default length of " + length);
        return length;
    }

    /**
     * directly call to format with fullMatch parameter
     * 
     * @param source object to format
     * @param fullMatch if true, the source must exactly match the format pattern
     * @return string representation of the given source
     */
    public String format(Object source, boolean fullMatch) {
        final boolean lastFullMatch = setFullMatch(fullMatch);
        final String result = format(source);
        setFullMatch(lastFullMatch);
        return result;
    }

    /**
     * converts the given object to a string, using the default formatter. Then the string will be formatted through the
     * pattern. set {@link #fullMatch} to false, if you only need a find - not an exact match.
     * 
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     */
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        String o;
        //first: do a standard formatting, before checking against the regular expression
        if (obj instanceof String) {
            o = (String) obj;
        } else {
            if (parser == null) {
                setDefaultFormatter(obj);
            }
            o = parser.format(obj);
        }
        if (compiledPattern == null) {
        	if (Util.isEmpty(pattern))
        		pattern = ".*";
            compiledPattern = Pattern.compile(pattern, regExpFlags);
        }
        //second: fill separation characters
        o = getTextFormatted(o);
        //third: check against the regular expression
        final Matcher matcher = compiledPattern.matcher(o);
        final boolean matches = fullMatch ? matcher.matches() : matcher.find();
        if (matches) {
            return toAppendTo.append(matcher.group());
        } else {
            throw new ManagedException("tsl2nano.regexpfailure", new Object[] { o, compiledPattern.pattern() });
        }
    }

    /**
     * The RegExpFormat doesn't know the object type - it will never do a true parsing! The method will return a
     * formatted string, like the format() does. If not the whole text matches the pattern, null will be returned. But you
     * have the possibility to set the {@link #fullMatch} to false, to only find (not match) the pattern.
     * 
     * @see java.text.Format#parseObject(java.lang.String, java.text.ParsePosition)
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        if (Util.isEmpty(source)) {
            // set the pos > 0, otherwise the calling method will throw a parseexception.
            pos.setIndex(1);
            return null;
        }
        final String init = getInitMask(source);
        // if pattern requires more characters, fill the source with a defined initialization.
        if (init != null && source.length() <= init.length()) {
            source = prefixInit(source, init);
            if (source.length() <= init.length())
                source = source + init.substring(source.length());
        }
        if (compiledPattern == null) {
            compiledPattern = Pattern.compile(pattern, regExpFlags);
        }
        final Matcher matcher = compiledPattern.matcher(source);
        final boolean matches = fullMatch ? matcher.matches() : matcher.find();
        // set the pos > 0, otherwise the calling method will throw a parseexception.
        pos.setIndex(source.length());

        //perhaps real parsing
        Object obj = null;
        if (matches) {
            try {
                obj = parser != null && isAbleToParse(parser) ? parser.parseObject(source + getParsingSuffix(parser,
                    source)) : matcher.group();
            } catch (final ParseException e) {
                //            ManagedException.forward(e);
                LOG.error(e);
            }
        } else {
            LOG.warn("text '" + source
                + "' doesn't match regular expression '"
                + pattern
                + "' (flags: "
                + regExpFlags
                + ", fullmatch: "
                + fullMatch
                + ", defaultFormatter: "
                + parser
                + ")");
        }
        return matches ? obj : null;
    }

    /**
     * @return Returns the maxCharacterCount of this formatter.
     */
    public int getMaxCharacterCount() {
        return maxCharacterCount;
    }

    /**
     * @return Returns the fullMatch.
     */
    public boolean isFullMatch() {
        return fullMatch;
    }

    /**
     * @param fullMatch The fullMatch to set.
     * @return lastFullMatch
     */
    public boolean setFullMatch(boolean fullMatch) {
        final boolean lastFullMatch = this.fullMatch;
        this.fullMatch = fullMatch;
        return lastFullMatch;
    }

    /**
     * @deprecated: use {@link #numberWithGrouping(int, int, boolean)}
     * @see #number(int, int, boolean)
     */
    protected static final String number(int dec, int fract) {
        return number(dec, fract, false);
    }

    /**
     * @deprecated: use {@link #numberWithGrouping(int, int, boolean)}
     * @param dec count of decimals
     * @param fract count of fracts
     * @param fixed if true, fixed characters will be used.
     * @return regular expression describing the given number.
     */
    protected static final String number(int dec, int fract, boolean fixed) {
        final String p = "([-])|([-]?[0-9]{1," + dec
            + "}"
            + (fract > 0 ? "([" + DECIMAL_SEPARATOR() + "]" + "[0-9]{0," + fract + "})?)" : ")");
        if (fixed) {
            systemInitMap.put(p, fixed('0', dec) + (fract > 0 ? DECIMAL_SEPARATOR() + fixed('0', fract) : ""));
        }
        return p;
    }

    /**
     * numbers with grouping characters
     * 
     * @param dec count of decimals
     * @param fract count of fracts
     * @param fixed if true, fixed characters will be used.
     * @return regular expression describing the given number.
     */
    protected static final String numberWithGrouping(int dec, int fract, boolean fixed) {
        final StringBuilder p = new StringBuilder("[-]?");
        /*
         * involve grouping separators (3-digits-blocks).
         * if text input doesn't contain fractions, fractions will be added automated,
         * so, the decimal part should not exceed length of dec.
         */
        dec -= fract;
        // a len of 0 will be changed to 1!
        if (dec <= 0)
            dec = 1;
        final int r = dec % 3;
        if (r > 0)
            p.append("\\d{1," + r + "}" + "[" + GROUPING_SEPARATOR() + "]?");
        final int c = (dec - r) / 3;
        p.append("(\\d{1,3}" + "[" + GROUPING_SEPARATOR() + "]?){0," + c + "}");
        p.append((fract > 0 ? "([" + DECIMAL_SEPARATOR() + "]" + "\\d{0," + fract + "})?" : ""));
        if (fixed) {
            systemInitMap.put(p.toString(), fixed('0', dec) + (fract > 0 ? DECIMAL_SEPARATOR() + fixed('0', fract) : ""));
        }
        return p.toString();
    }

    /**
     * @param count count of characters
     * @param alphaonly if true, only alpha numerics are allowed
     * @return regular expression describing the given text specifications
     */
    public static final String alphanum(int count, boolean alphaonly) {
        return (alphaonly ? "[a-zA-Z]" : PATTERN_SINGLE_BYTE_SPACE) + "{0," + count + "}";
    }

    /**
     * @param c characters to use
     * @param count count to duplicate the character c.
     * @return string with length count containing the characters c.
     */
    private static final String fixed(char c, int count) {
        final StringBuffer buf = new StringBuffer(count);
        for (int i = 0; i < count; i++) {
            buf.append(c);
        }
        return buf.toString();
    }

    /**
     * provides a pattern defining numeric blocks, separated by spaces. e.g.: '0123 4567 8901'. this is an example with
     * blockwidth=4 and mincount=maxcount=3.
     * 
     * @param blockwidth numeric block width
     * @param mincount minimum count of blocks
     * @param maxcount maximum count of blocks
     * @param allowRest if true, the last block may have less than blockwith numbers (e.g.: '0123 4567 89')
     * @return regular expression defining numeric blocks
     */
    public static final String patternNumericBlock(int blockwidth, int mincount, int maxcount, boolean allowRest) {
        String pattern = "([0-9]{blockwidth,blockwidth} ){minCount,maxCount}" + (allowRest ? "[0-9]{0,blockwidth}" : "");
        pattern = StringUtil.insertProperties(pattern,
            MapUtil.asMap("blockwidth", blockwidth, "mincount", mincount, "maxcount", maxcount));
        return pattern;
    }

    /**
     * creates a standard regular expression for a currency. the biggest currency will be 999,999,999.99
     * 
     * @return new formatter for a currency
     */
    public static RegExpFormat createCurrencyRegExp() {
        final int dec = 11, fract = 2;
        return new RegExpFormat(numberWithGrouping(dec, fract, false) + getCurrencyPostfix(), null, dec + fract + 3,//28082012ts: 1 -->3 to enable a number like 123456789 --> 123.456.789,00 €
            0,
            getCurrencyFormat());
    }

    /**
     * creates a standard regular expression for a currency without showing the currency symbol
     * 
     * @return new formatter for a currency
     */
    public static RegExpFormat createCurrencyNoSymbol() {
        final int dec = 11, fract = 2;
        return new RegExpFormat(numberWithGrouping(dec, fract, false) + "", null, dec + fract + 3,//28082012ts: 1 -->3 to enable a number like 123456789 --> 123.456.789,00 €
            0,
            getCurrencyFormatNoSymbol());
    }

    /**
     * creates a standard regular expression for a number, without position after decimal point.
     * 
     * @return new formatter for the given number
     */
    public static RegExpFormat createCurrencyNoFraction() {
        final int dec = 11, fract = 0;
        return new RegExpFormat(numberWithGrouping(dec - 2, fract, false) + getCurrencyPostfix(),
            null,
            dec + fract + 3,//28082012ts: 1 -->3 to enable a number like 123456789 --> 123.456.789,00 €
            0,
            getCurrencyFormatNoFraction());
    }

    /**
     * creates a regular expression for big currencies without decimal place. the biggest currency will be
     * 999,999,999,999,999
     * 
     * @return new formatter for a currency
     */
    public static RegExpFormat createBigCurrencyRegExp() {
        final int dec = 17, fract = 0;
        return new RegExpFormat(numberWithGrouping(dec - 2, fract, false) + getCurrencyPostfix(),
            null,
            dec + fract + 3,//28082012ts: 1 -->3 to enable a number like 123456789 --> 123.456.789,00 €
            0,
            getCurrencyFormat());
    }
    /**
     * creates a regular expresssion for the given type of number
     * 
     * @param numberType type of number (having scale and precision)
     * @return new formatter
     */
    public static RegExpFormat createNumberRegExp(BigDecimal decimal) {
        assert decimal != null : "decimal must not be null!";
        //the scale is the scaled fraction digit count
        final int dec = decimal.scale();
        //the precision is the unscaled fraction digit count
        final int fract = decimal.precision();
        return createNumberRegExp(10, dec + fract);
    }

    /**
     * delegates to {@link #createNumberRegExp(int, int, Class)}.
     */
    public static RegExpFormat createNumberRegExp(int dec, int fract) {
        return createNumberRegExp(dec, fract, null);
    }

    /**
     * creates a standard regular expression for a number.
     * 
     * @param dec count of decimals
     * @param fract count of fracts
     * @param type type of number (Integer, Double, BigDecimal, primitives..)
     * @return new formatter for the given number
     */
    public static RegExpFormat createNumberRegExp(int dec, int fract, Class<?> type) {
        return new RegExpFormat(numberWithGrouping(dec, fract, false),
            null,
            dec + fract + 1,
            0,
            new GenericParser(type, null, null, fract));
    }

    /**
     * Create a new alpha-numerical RegExpFormat.
     * 
     * @param count count of characters
     * @param alphaonly if true, only alpha numerics are allowed (but e.g. no spaces)
     * @return new formatter for the given text specifications
     */
    public static RegExpFormat createAlphaNumRegExp(int count, boolean alphaonly) {
        return new RegExpFormat(alphanum(count, alphaonly), count, count > 100 ? Pattern.MULTILINE : 0);
    }

    /**
     * createDateRegExp
     * 
     * @return regexp for a german date
     */
    public static RegExpFormat createDateRegExp() {
        RegExpFormat regExp = new RegExpFormat(getDateFormatExpression(),
            createDateMask(),
            10,
            0,
            new GenericParser(Date.class));
        return regExp;
    }

    /**
     * createDateRegExp
     * 
     * @return regexp for a german time
     */
    public static RegExpFormat createTimeRegExp() {
        RegExpFormat regExp = new RegExpFormat(getTimeFormatExpression(),
            workaroundOnUSTime(DateFormat.getTimeInstance().format(getInitialDate())),
            8,
            0,
            new GenericParser(Time.class));
        return regExp;
    }

    private static String workaroundOnUSTime(String format) {
        return format.startsWith("12:") ? "00" + format.substring(2) : format;
    }

    /**
     * createDateRegExp
     * 
     * @return regexp for a german date-time
     */
    public static RegExpFormat createDateTimeRegExp() {
        RegExpFormat regExp = new RegExpFormat(getDateTimeFormatExpression(), createDateMask(DateFormat.getDateTimeInstance()), 19, 0, new GenericParser(Timestamp.class));
        return regExp;
    }

    /**
     * Build a RegExpFormat for the given pattern. Note that the pattern MUST NOT contain length checks, this is added
     * during construction (a suffix "{minLength, maxLength}" is added to the given pattern).
     * 
     * @param pattern the pattern
     * @param init optional init pattern
     * @param minLength the minimum length
     * @param maxLength maximum field length
     * @param regExpFlags additional Flags for the regular expression
     * @return the formatter
     * @see Pattern Pattern for Flags description
     */
    public static RegExpFormat createPatternRegExp(String pattern,
            String init,
            int minLength,
            int maxLength,
            int regExpFlags) {
        assert minLength >= 0 : "minLength must be >= 0";
        assert maxLength >= minLength : "maxLength must be >= minLength";
        return new RegExpFormat(pattern + "{" + minLength + "," + maxLength + "}", init, maxLength, regExpFlags);
    }

    /**
     * Create a new RegExpFormat that only controls the length of the input.
     * 
     * @param minLength the minimum length
     * @param maxLength maximum field length
     * @param regExpFlags additional Flags for the regular expression
     * @return the formatter
     * @see Pattern Pattern for Flags description
     */
    public static RegExpFormat createLengthRegExp(int minLength, int maxLength, int regExpFlags) {
        assert minLength >= 0 : "minLength must be >= 0";
        assert maxLength >= minLength : "maxLength must be >= minLength";
        return new RegExpFormat(".{" + minLength + "," + maxLength + "}", maxLength, regExpFlags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[pattern: " + pattern + "]";
    }

    /**
     * getDefaultFormatter
     * 
     * @return
     */
    public Format getDefaultFormatter() {
        return parser;
    }

    /**
     * @param obj object to format with a new default formatter
     */
    protected void setDefaultFormatter(Object obj) {
        this.parser = new GenericParser(FormatUtil.getDefaultFormat(obj, false), obj.getClass());
    }

    /**
     * append all following format characters (like ./:) of inputmask to the end of source.
     * 
     * @param source source text
     * @param initmask initmask for source text
     * @return source and following format characters
     */
    public static final String getTextWithCaretJump(String source, String initmask) {
        final StringBuffer textWithCaretJump = new StringBuffer(source);
        final int i = getNextInputPosition(source.length(), initmask);
        if (source.length() < initmask.length() && i > source.length()) {
            textWithCaretJump.append(initmask.substring(source.length(), i));
        } else {
            LOG.warn("initmask '" + initmask + "' is to short for text '" + source + "'");
        }
        return textWithCaretJump.toString();
    }

    /**
     * tries to jump over all format characters (see initmask)
     * 
     * @param currentPosition current caret position
     * @return the next input position
     */
    public int getNextInputPosition(int currentPosition) {
        return getNextInputPosition(currentPosition, getInitMask());
    }

    /**
     * tries to jump over all format characters (see initmask)
     * 
     * @param currentPosition current caret position
     * @param initmask mask, holding the formatting characters
     * @return the next input position
     */
    private static int getNextInputPosition(int currentPosition, String initmask) {
        if (initmask != null) {
            while (currentPosition < initmask.length()) {
                if (!FORMAT_CHARACTERS.contains(initmask.substring(currentPosition, currentPosition + 1))) {
                    break;
                }
                currentPosition++;
            }
        }
        return currentPosition;
    }

    /**
     * getTextFormatted
     * 
     * @param newText new text
     * @return new text formatted with caret jumps
     */
    public String getTextFormatted(String newText) {
        final StringBuffer textWithCaretJump = new StringBuffer(getMaxCharacterCount());
        final String initmask = getInitMask();
        if (initmask != null) {
            newText = prefixInit(newText, initmask);
            int ti = 0;
            char c, mc;
            for (int i = 0; i < initmask.length(); i++) {
                c = newText.charAt(ti);
                mc = initmask.charAt(i);
                if (FORMAT_CHARACTERS.contains(String.valueOf(mc)) && (newText.length() <= ti || c != mc)) {
                    textWithCaretJump.append(mc);
                    continue;
                } else if (newText.length() <= ti) {
                    //no formatting char and source end reached
                    break;
                }

                textWithCaretJump.append(c);
                // forward initmask to format char of new text
                if (FORMAT_CHARACTERS.contains(String.valueOf(c)) ) {
                    int ii = initmask.indexOf(c, i);
                    if (ii != -1 && ii > i) {
                        i = ii - 1;
                    }
                }
                ti++;
            }
        } else {
            textWithCaretJump.append(newText);
        }
        return textWithCaretJump.toString();
    }

    private String prefixInit(String newText, String initmask) {
        if (newText.length() >= initmask.length())
            return newText;
        String s;
        for (int i = 0; i < initmask.length(); i++) {
            if (FORMAT_CHARACTERS.contains(s=String.valueOf(initmask.charAt(i)))) {
                int firstTextSep = newText.indexOf(s);
                if (firstTextSep == -1 || i <= firstTextSep)
                    return newText;
                return initmask.substring(0, i - firstTextSep) + newText;
            }
        }
        return newText;
    }

    /**
     * formatWithCaretJump
     * 
     * @param newText new text
     * @return new text formatted with caret jumps
     */
    public String formatWithCaretJump(String newText) {
        return format(getTextFormatted(newText), false);
    }

    /**
     * tries to evaluate, if the formatter is a simple one, or one, containing separation characters like in a date.
     * 
     * @return true, if initmask contains at least one format character defined in {@link #FORMAT_CHARACTERS}.
     */
    public boolean hasSeparationCharacter() {
        if (getInitMask() == null) {
            return false;
        }
        final char[] initMask = getInitMask().toCharArray();
        for (int i = 0; i < initMask.length; i++) {
            if (FORMAT_CHARACTERS.contains(String.valueOf(initMask[i]))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return Returns the pattern.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * setPattern
     * 
     * @param pattern The pattern to set.
     * @param init initializing (default) string
     * @param maxLength max char count
     * @param regExpFlags reg exp flags
     */
    public void setPattern(String pattern, String init, int maxLength, int regExpFlags) {
        init(pattern, init, maxLength, regExpFlags);
    }

    /**
     * @return Returns the regExpFlags.
     */
    public int getRegExpFlags() {
        return regExpFlags;
    }

    /**
     * getInitMask
     * 
     * @return init mask
     */
    public String getInitMask() {
        return getInitMask(null);
    }

    /**
     * is able to return a source specific init-mask. needed e.g. for dates. is called by parsing the source to an
     * object to test the correctness.
     * 
     * @param source (optional) source. if null, the init-mask stored for the pattern will be returned
     * @return init mask
     */
    protected String getInitMask(String source) {
    	assureInit();
        String mask = null;
        if (source != null) {
            mask = initMap.get(source);
        }
        return mask != null || initMap == null ? mask : initMap.get(pattern);
    }

    /**
     * @return Returns the isAbleToParse.
     */
    public boolean isAbleToParse() {
        return isAbleToParse;
    }

    /**
     * @param isAbleToParse The isAbleToParse to set.
     */
    public void setAbleToParse(boolean isAbleToParse) {
        this.isAbleToParse = isAbleToParse;
    }

    /**
     * if a format, like the currency (see {@link DecimalFormat#getNegativeSuffix()}, needs a number suffix, we have to
     * give it to the parsing process.
     * 
     * @param format format to ask
     * @return empty or filled suffix
     */
    protected static String getParsingSuffix(Format format, String txt) {
        if (format instanceof GenericParser) {
            format = ((GenericParser) format).getParsingFormat();
        }
        //TODO: its a workaround to handle the currency format. use the positive/negative prefix and suffix
        if (format instanceof DecimalFormat) {
            final StringBuffer s = new StringBuffer();
            final DecimalFormat decFormat = (DecimalFormat) format;
            final String prefix = decFormat.getNegativePrefix();
            if (txt.equals(prefix)) {
                s.append("0");
            }
            final String suffix = decFormat.getNegativeSuffix();
            if (!txt.contains(suffix)) {
                return s.append(suffix).toString();
            } else {
                return s.toString();
            }
        } else {
            return "";
        }
    }

    /**
     * isAbleToParse
     * 
     * @param format format
     * @return true, if format can create an object from string
     */
    public static boolean isAbleToParse(Format format) {
        return !(format instanceof RegExpFormat) || ((RegExpFormat) format).isAbleToParse();
    }

    /**
     * on default this method returns true, if the underlying {@link #parser} is a {@link NumberFormat}.
     * <p/>
     * override this method if you have a {@link #parser} that is not of type {@link NumberFormat} but you want a field
     * to displayed like a number (e.g. in a table-column as right-alignment).
     * 
     * @return true, if this format instance asserts to be a number format
     */
    @Override
    public boolean isNumber() {
        return parser.isNumber();
    }

    /**
     * convenience to check, if the given format is an instance of {@link RegExpFormat} and
     * {@link RegExpFormat#isNumber} returns true.
     * 
     * @param format format to evaluate
     * @return true, if format is - or contains - a number format
     */
    public static boolean isNumber(Format format) {
        return (format instanceof NumberFormat) || (format instanceof INumberFormatCheck && ((INumberFormatCheck) format).isNumber());
    }

    /**
     * getCurrencyPostfix
     * 
     * @return regexp for currency postfix
     */
    protected static final String getCurrencyPostfix() {
        return getCurrencyPostfix(NumberFormat.getCurrencyInstance().getCurrency().getSymbol());
    }

    /**
     * getCurrencyPostfix
     * 
     * @param locale currency locale
     * @return regexp for currency postfix
     */
    protected static final String getCurrencyPostfix(Locale locale) {
        return getCurrencyPostfix(NumberFormat.getCurrencyInstance(locale).getCurrency().getSymbol());
    }

    /**
     * getCurrencyPostfix
     * 
     * @param symbol currency symbol
     * @return regexp for currency postfix
     */
    protected static final String getCurrencyPostfix(String symbol) {
        return "([\u202F\u00A0\\s]?" + symbol + ")?"; // added unicode for 'NO-BREAK SPACE'
    }


    /**
     * currency with currency default precision (normally:2). object types must be {@link BigDecimal}!
     * 
     * @return standard currency regular expression for current locale
     */
    public static final Format getCurrencyFormat(Currency c) {
        return CurrencyUtil.getFormat(c.getCurrencyCode(), c.getDefaultFractionDigits());
    }

    /**
     * currency with currency default precision (normally:2). object types must be {@link BigDecimal}!
     * 
     * @return standard currency regular expression for current locale
     */
    public static final Format getCurrencyFormat() {
        Currency c = NumberFormat.getCurrencyInstance().getCurrency();
        return getCurrencyFormat(c);
    }

    /**
     * creates a {@link Format} that is able to format and parse the given type. useful on text-fields representing an
     * object.
     * <p/>
     * if you give a converter, the formatting/parsing will be done in two steps:<br>
     * - convert a string to the attributes type - search the right bean through this filled attribute
     * <p/>
     * Example:<br>
     * A person has an id of type long. the id will be shown with a mask, containing spaces. the formatted string '100
     * 000 00000' must be converted to a long 10000000000. through an example person with given id, a real database
     * object will be found.
     * <p/>
     * Example-Code:
     * 
     * <pre>
     *         //string to long converter
     *         IConverter converter = new IConverter() {
     *             Override
     *             public Object from(Object toValue) {
     *                 return SteuerakteUtil.getFormattedStnr(toValue);
     *             }
     * 
     *             Override
     *             public Object to(Object fromValue) {
     *                 return SteuerakteUtil.getFormattedStnrAsLong((String) fromValue);
     *             }
     *         };
     *         return RegularExpressionFormat.getParser(Person.class,
     *             PersonConst.ATTR_PERSID,
     *             "[0-9]{3,3} [0-9]{3,3} [0-9]{5,5}",
     *             "000 000 00000",
     *             converter);
     * </pre>
     * 
     * @param <TYPE> field object type
     * @param type field type
     * @param uniqueIdAttribute attribute name to resolve field object
     * @param pattern regular expression to constrain the object-to-string representation
     * @param initMask default representing string for empty objects. the string length will be used to check for
     *            completition.
     * @param converter (optional) converts a string to the attribute type (e.g.: the string '100 000 00000' will be
     *            converted to long 10000000000).
     * @param useCache if true, all loaded objects will be cached.
     * @return new {@link RegExpFormat}
     */
    public static <TYPE> RegExpFormat getParser(final Class<TYPE> type,
            final String uniqueIdAttribute,
            String pattern,
            final String initMask,
            final IConverter<String, Object> converter,
            final boolean useCache) {
        final RegExpFormat format = new RegExpFormat(pattern, initMask) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;
            final TYPE instance = BeanClass.getBeanClass(type).createInstance();
            final BeanAttribute attribute = BeanAttribute.getBeanAttribute(type, uniqueIdAttribute);
            final ReferenceMap cache = useCache ? new ReferenceMap() : null;

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                if (type.isAssignableFrom(obj.getClass())) {
                    Object strObj = converter != null ? converter.from(attribute.getValue(obj))
                        : attribute.getValue(obj);
                    return toAppendTo.append(strObj);
                } else {
                    // on verifyText, it will be a string
                    return super.format(obj, toAppendTo, pos);
                }
            }

            @Override
            public Object parseObject(String source) throws ParseException {
                final String formattedSource = (String) super.parseObject(source);
                if (formattedSource == null) {
                    return null;
                }
                Object convSource = converter != null ? converter.to(formattedSource) : formattedSource;
                attribute.setValue(instance, convSource);
                if (source.length() < initMask.length() || formattedSource.equals(getInitMask())) {
                    return instance;
                }
                if (useCache && cache.containsKey(convSource)) {
                    return cache.get(convSource);
                }
                Collection<TYPE> result = BeanContainer.instance().getBeansByExample(instance);
                if (result.size() > 0) {
                    Object resObject = result.iterator().next();
                    if (useCache) {
                        cache.put(convSource, resObject);
                    }
                    return resObject;
                }
                return null;
            }

            @Override
            public boolean isAbleToParse() {
                return true;
            }

        };
        format.setAbleToParse(true);
        return format;
    }

    /**
     * Extension for {@link Serializable}
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        initSerialization();
        out.defaultWriteObject();
    }

    @Persist
    private void initSerialization() {
        //remove copied entries of systeminitmap - if it is not the systemInitMap itself!
        if (initMap != null && initMap != systemInitMap) {
            MapUtil.removeAll(initMap, systemInitMap.keySet());
        } else {
            initMap = null;
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initDeserialization();
    }

    private final void assureInit() {
    	initDeserialization();
    }
    
    @Commit
    @Complete
    private void initDeserialization() {
        if (Util.isEmpty(initMap)) {
            initMap = systemInitMap;
        }
    }

}
