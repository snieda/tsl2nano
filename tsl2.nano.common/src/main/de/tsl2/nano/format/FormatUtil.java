/*
 * File: $HeadUrl$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: 24.06.2009
 * 
 * Copyright: (c) Thomas Schneider 2009, all rights reserved
 */
package de.tsl2.nano.format;

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
import de.tsl2.nano.log.LogFactory;

import de.tsl2.nano.currency.CurrencyUtil;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.execution.CompatibilityLayer;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanClass;

/**
 * evaluates a {@link Format} for a given type or instance. used by validators, to check input.
 * 
 * @author TS
 */
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
                        df.setRoundingMode(RoundingMode.HALF_UP);
//                    df.setMaximumFractionDigits(340);
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
                            if (df.getNegativePrefix().equals(source))
                                source = source + "0";

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

                };
            } else if (Time.class.isAssignableFrom(type)) {
                f = DateFormat.getTimeInstance();
            } else if (Timestamp.class.isAssignableFrom(type)) {
                f = DateFormat.getDateTimeInstance();
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
                        return StringUtil.isEmpty(source) ? null : source;
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
                        pos.setIndex(!StringUtil.isEmpty(source) ? source.length() : 1);
                        return !StringUtil.isEmpty(source) ? Enum.valueOf(type, source) : null;
                    }

                };
            } else {
                f = new Format() {
                    @Override
                    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                        return toAppendTo.append(obj != null ? obj.toString() : "");
                    }

                    @Override
                    public Object parseObject(String source, ParsePosition pos) {
                        pos.setIndex(!StringUtil.isEmpty(source) ? source.length() : 1);
                        return !StringUtil.isEmpty(source) ? BeanClass.createInstance(type, source) : null;
                    }

                };
            }
        }
        return f;
    }

    /**
     * wrappes a checking format above the given one - to check the result of parsing to be equal to the source.
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
                Object jdkResult = mf.parseObject(source, pos);
                /*
                 * e.g.: java accepts any date values - rolling months and days.
                 * we have to check value correctness - throwing exceptions
                 */
                if (source != null && !source.equals(format(jdkResult)))
                    throw new FormattedException("tsl2nano.invalidvalue", new Object[] { source });
                return jdkResult;
            }

        };
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
    protected static final Format getDefaultExtendedFormat(Class<?> type, String prefix, String postfix, int precision) {
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
            df.setMinimumFractionDigits(0);
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
        if (currencyCode != null)
            numberFormat.setCurrency(Currency.getInstance(currencyCode));
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);
        numberFormat.setGroupingUsed(true);
        numberFormat.setParseBigDecimal(true);
        return numberFormat;
    }
}
