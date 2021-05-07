/*
 * File: $HeadUrl$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: 24.06.2009
 * 
 * Copyright: (c) Thomas Schneider 2009, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Currency;
import java.util.Date;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.log.LogFactory;
//import de.tsl2.nano.currency.CurrencyUtil;

/**
 * evaluates a {@link Format} for a given type or instance. used by validators, to check input.
 * 
 * @author TS
 */
@SuppressWarnings({"rawtypes", "serial"})
public class FormatUtil {
    private static final Log LOG = LogFactory.getLog(FormatUtil.class);

    /**
     * getDefaultFormat
     * 
     * @param instanceOrClass can be an instance or class
     * @param toObjectConverter true, if the format has to handle conversion from string to object.
     * @return specialized format for the given type or instance
     */
    @SuppressWarnings("unchecked")
    public static Format getDefaultFormat(Object instanceOrClass, boolean toObjectConverter) {
        Format f = null;
        if (instanceOrClass != null) {
            final Class type = instanceOrClass.getClass() == Class.class ? (Class) instanceOrClass
                : instanceOrClass.getClass();
            if (NumberUtil.isNumber(type)) {
                f = NumberFormat.getInstance();
                final DecimalFormat df = (DecimalFormat) f;
                /*
                 * floats will be used as double, having infinitesimal rounding problems.
                 * bigdecimals should already hold their precision!
                 */
                if (!float.class.isAssignableFrom(type) && !Float.class.isAssignableFrom(type)
                    && !BigDecimal.class.isAssignableFrom(type)) {
                    df.setMaximumFractionDigits(340);
                } else {
                    //on float we get roundingmode HALF_EVEN - why is that the default?
                    if (CompatibilityLayer.MIN_JDK16)
                     {
                        df.setRoundingMode(RoundingMode.HALF_UP);
//                    df.setMaximumFractionDigits(340);
                    }
                }
                df.setGroupingUsed(false);
                df.setParseBigDecimal(true);
                //the information of the bigdecimal will be used by the formatter!
//                if (instanceOrClass instanceof BigDecimal) {
//                    BigDecimal b = (BigDecimal) instanceOrClass;
//                    df.setMaximumFractionDigits(b.precision());
//                    if (b.scale() != 0) {
//                        df.setMaximumIntegerDigits(b.scale());
//                    }
//                }
                /*
                 * the numberformat (-->decimalformat) is only able to return bigdecimal,double or long,
                 * so we need a format that returns on parsing the right instance.
                 */
                f = new Format() {
                    /** serialVersionUID */
                    private static final long serialVersionUID = 4024031963278900048L;

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                        return obj != null ? toAppendTo.append(df.format(obj)) : toAppendTo;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public Object parseObject(String source, ParsePosition pos) {
                        if (source != null) {
                            /*
                             * Workaround: trying to start with a negative number, the '-' must be combined with at least one digit
                             * to be parse-able.
                             */
                            if (df.getNegativePrefix().equals(source)) {
                                source = source + "0";
                            }

                            pos.setIndex(source.length());
                            BigDecimal bigDecimal;
                            try {
                                bigDecimal = (BigDecimal) df.parse(source);
                            } catch (final ParseException e) {
                                LOG.warn("failed to parse BigDecimal from=" + source, e);
                                return null;
                            }
                            if (Short.class.isAssignableFrom(type) || short.class.isAssignableFrom(type)) {
                                return bigDecimal.shortValue();
                            } else if (Byte.class.isAssignableFrom(type) || byte.class.isAssignableFrom(type)) {
                                return bigDecimal.byteValue();
                            } else if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
                                return bigDecimal.intValue();
                            } else if (Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)) {
                                return bigDecimal.longValue();
                            } else if (Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)) {
                                return bigDecimal.floatValue();
                            } else if (Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
                                return bigDecimal.doubleValue();
                            } else {
                                return bigDecimal;
                            }
                        } else {
                            pos.setIndex(1);
                            return null;
                        }
                    }

                    @Override
                    public String toString() {
                        return df.toPattern();
                    }
                };
            } else if (Time.class.isAssignableFrom(type)) {
                f = getTimeFormat(DateFormat.getTimeInstance(), Time.class);
            } else if (Timestamp.class.isAssignableFrom(type)) {
                f = getTimeFormat(DateFormat.getDateTimeInstance(), Timestamp.class);
            } else if (Date.class.isAssignableFrom(type)) {
                f = getCheckedFormat(DateFormat.getDateInstance());
            } else if (String.class.isAssignableFrom(type)) {
                //the String-to-String converter is needed if empty strings are converted to nulls!
                f = new Format() {
                    @Override
                    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                        return toAppendTo.append(obj != null ? obj.toString() : "");
                    }

                    @Override
                    public Object parseObject(String source, ParsePosition pos) {
                        pos.setIndex(source != null && source.length() > 0 ? source.length() : 1);
                        return Util.isEmpty(source) ? null : source;
                    }
                    @Override
                    public String toString() {
                        return "*";
                    }
                };
            } else if (Boolean.class.isAssignableFrom(type)) {
                f = new Format() {
                    @Override
                    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                        return toAppendTo.append(((Boolean)obj).toString());
                    }
                    @Override
                    public Object parseObject(String source, ParsePosition pos) {
                        pos.setIndex(!Util.isEmpty(source) ? source.length() : 1);
                        return !Util.isEmpty(source) ? Boolean.parseBoolean(source) : null;
                    }
                };
            } else if (Enum.class.isAssignableFrom(type)) {
                f = new Format() {
                    @Override
                    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                        return toAppendTo.append(obj != null ? obj.toString() : "");
                    }

                    @Override
                    public Object parseObject(String source, ParsePosition pos) {
                        pos.setIndex(!Util.isEmpty(source) ? source.length() : 1);
                        return !Util.isEmpty(source) ? valueOf(type, source) : null;
                    }

                    private Object valueOf(Class<Enum> type, String source) {
                        Enum e = null;
                        try {
                            e = Enum.valueOf(type, source);
                        } catch (Exception ex) {
                            //if the enum type defines toString() methods for it's values, the source may 
                            //be this toString() presentation. to resolve it's real enum name we have to
                            //check all enum toStrings.
                            if (e == null) {
                                e = CollectionUtil.findEnum(type, source);
                            }
                        }
                        return e;
                    }

                };
            } else {
                f = new Format() {
                    @Override
                    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                        return toAppendTo.append(obj != null ? ENV.format(obj) : "");
                    }

                    @Override
                    public Object parseObject(String source, ParsePosition pos) {
                        pos.setIndex(!Util.isEmpty(source) ? source.length() : 1);
                        return !Util.isEmpty(source) ? ObjectUtil.wrap(source, type) : null;
                    }

                };
            }
        }
        return f;
    }


    /**
     * getInstanceFormat
     * @param format
     * @param instanceType type to return as instance - the constructor must have one wrapping parameter
     * @return new wrapped instance
     */
    protected static Format getTimeFormat(final Format format, final Class<?> instanceType) {
        return new Format() {
            Format mf = format;

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return mf.format(obj, toAppendTo, pos);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                Object parseObject = mf.parseObject(source, pos);
                return parseObject != null ? BeanClass.createInstance(instanceType, ((Date)parseObject).getTime()) : null;
            }

            @Override
            public String toString() {
                return mf.toString();
            }
        };
    }
    /**
     * @deprecated: use DateFormat.setLenient(false) instead
     * 
     *              wrappes a checking format above the given one - to check the result of parsing to be equal to the
     *              source.
     * 
     * @param format origin format
     * @return wrapped checking format
     */
    protected static Format getCheckedFormat(final Format format) {
        /*
         * not used - may be a problem in insertion mode. e.g. it would not be possible to change
         * the date '31.12.2010' to '31.11.2010'. the day must be edited first - that may be a
         * problem for the user.
         */
//        return format;
        return new Format() {
            Format mf = format;

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return mf.format(obj, toAppendTo, pos);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return checkParse(mf, null, pos, source);
            }

            @Override
            public String toString() {
                return mf.toString();
            }
        };
    }

    /**
     * parses the given source and checks the result through {@link #checkParse(Format, Object, ParsePosition, String)}
     * if the parsing result is an instance of one of the given classesToCheck.
     * 
     * @param format to be used for parsing and checking against parsed format
     * @param source source to be parsed
     * @param classesToCheck the check will only be done on instances of them
     * @return parsed object, null or throws any Exception if check failes
     */
    public static Object checkParse(Format format, String source, Class... classesToCheck) {
        ParsePosition pos = new ParsePosition(0);
        Object parseResult = format.parseObject(source, pos);
        if (parseResult != null) {
            boolean check = false;
            for (int i = 0; i < classesToCheck.length; i++) {
                if (parseResult.getClass().isAssignableFrom(classesToCheck[i])) {
                    check = true;
                    break;
                }
            }
            if (check) {
                return checkParse(format, parseResult, pos, source);
            }
        }
        return parseResult;
    }

    /**
     * use that, if you don't want to have a fixed {@link DateFormat#isLenient()} = false.
     * <p/>
     * e.g.: java accepts any date values - rolling months and days. we have to check value correctness - throwing
     * exceptions
     */
    protected static Object checkParse(Format format, Object parseResult, ParsePosition pos, String source) {
        parseResult = parseResult != null ? parseResult : format.parseObject(source, pos);
        if (source != null && !source.equals(format.format(parseResult))) {
            throw new ManagedException("tsl2nano.invalidvalue", new Object[] { source });
        }
        return parseResult;
    }

    /**
     * create a default format. if precision is 0 or type is null, null will be returned
     * <p/>
     * TODO: evaluate extended date! <br/>
     * TODO: use prefix!
     * 
     * @param precision fraction digits
     * @return number format or null
     */
    public static final Format getDefaultExtendedFormat(Class<?> type,
            String prefix,
            String postfix,
            int precision,
            int scale) {
        if (type == null) {
            return null;
        }
        final Format format;
        if (precision >= 0 && postfix != null) {
            format = getCurrencyFormat(postfix, precision);
        } else {
            //this definition MUST match the definition of FormatUtil.getDefaultFormat(..)
            format = FormatUtil.getDefaultFormat(type, true);
        }
        if (precision >= 0 && format instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) format;
            df.setMinimumFractionDigits(scale);
            df.setMaximumFractionDigits(precision);
//        df.setGroupingUsed(false);
//        if (BigDecimal.class.isAssignableFrom(type))
//            df.setParseBigDecimal(true);
        }
        return format;
    }

    /**
     * currency with currency default precision (normally:2). object types must be {@link BigDecimal}!
     * 
     * @return standard currency regular expression for current locale
     */
    public static final Format getCurrencyFormat() {
        Currency c = NumberFormat.getCurrencyInstance().getCurrency();
        return getCurrencyFormat(c.getCurrencyCode(), c.getDefaultFractionDigits());
    }

    /**
     * currency with precision 0. object types must be {@link BigDecimal}!
     * 
     * @return number format
     */
    public static final Format getCurrencyFormatNoFraction() {
        return getCurrencyFormat(NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode(), 0);
    }

    public static final Format getCurrencyFormatNoSymbol() {
        return getCurrencyFormat(null, 2);
    }

    /**
     * creates a NumberFormat for BigDecimals with currency code and fractionDigits. to get an historic currency, see
     * {@link CurrencyUtil}.
     * 
     * @param currencyCode currency code (see {@link Currency#getInstance(String)} and {@link http
     *            ://de.wikipedia.org/wiki/ISO_4217}.
     * @param fractionDigits number of fraction digits (precision)
     * @return new numberformat instance
     */
    public static final Format getCurrencyFormat(String currencyCode, int fractionDigits) {
        final DecimalFormat numberFormat = (DecimalFormat) (currencyCode != null ? NumberFormat.getCurrencyInstance()
            : NumberFormat.getInstance());
        if (currencyCode != null) {
            numberFormat.setCurrency(Currency.getInstance(currencyCode));
        }
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);
        numberFormat.setGroupingUsed(true);
        numberFormat.setParseBigDecimal(true);
        return numberFormat;
    }

    public static String format(Object obj) {
    	return getDefaultFormat(obj, false).format(obj);
    }
    public static <T> T parse(Class<T> type, String source) {
        try {
            return (T) getDefaultFormat(type, true).parseObject(source);
        } catch (ParseException e) {
            return (T) ManagedException.forward(e);
        }
    }

}
