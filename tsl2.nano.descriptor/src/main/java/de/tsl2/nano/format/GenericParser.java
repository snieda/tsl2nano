/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 13, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.format;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.DefaultFormat;
import de.tsl2.nano.core.util.FormatUtil;

/**
 * is able to format the given type to a string and to parse a string into an
 * object of the given type. uses {@link DefaultFormat} to format to a string
 * and {@link FormatUtil} and {@link RegExpFormat} to parse a string to an
 * object. is able to be xml-serialized.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class GenericParser<T> extends DefaultFormat implements INumberFormatCheck {

    /** serialVersionUID */
    private static final long serialVersionUID = 5202034503591423763L;

    @Attribute(required = false)
    protected Class<T> parsingType;
    @Attribute(required = false)
    protected String parsingPattern;
    private transient Format parsingFormat;
    @Attribute(required = false)
    /** scale, usable for numbers */
    private int scale;
    /**
     * precision, usable for numbers as precision or on dates for short/medium/long
     * presentation
     */
    @Attribute(required = false)
    private int precision;
    /** prefix to start the format */
    @Attribute(required = false)
    private String prefix;
    /** postfix (at the moment only on currencies) to end the format */
    @Attribute(required = false)
    private String postfix;

    /**
     * constructor to be de-serializable
     */
    public GenericParser() {
    }

    public GenericParser(Format format) {
        this(format, null);
    }

    public GenericParser(Format format, Class<T> type) {
        this.parsingFormat = format;
        this.parsingType = type;
        if (isNumber()) {
            NumberFormat numberFormat = (NumberFormat) format;
            precision = numberFormat.getMaximumFractionDigits();
            scale = numberFormat.getMinimumFractionDigits();
            postfix = numberFormat.getCurrency().getCurrencyCode();
            if (type == null) {
                if (numberFormat.isParseIntegerOnly()) {
                    parsingType = (Class<T>) Integer.class;
                } else {
                    parsingType = (Class<T>) BigDecimal.class;
                }
            }
        } else if (format instanceof DateFormat) {
            if (type == null) {
                SimpleDateFormat sdf = (SimpleDateFormat) format;
                String sdfPattern = sdf.toPattern();
                if (sdfPattern.contains("y") && sdfPattern.contains("s")) {
                    parsingType = (Class<T>) Timestamp.class;
                } else if (sdfPattern.contains("s")) {
                    parsingType = (Class<T>) Time.class;
                } else {
                    parsingType = (Class<T>) Date.class;
                }
            }
        } else {
            if (type == null) {
                parsingType = (Class<T>) String.class;
            }
        }
        if (format instanceof RegExpFormat) {
            parsingPattern = ((RegExpFormat) format).getPattern();
        }
    }

    public GenericParser(Class<T> parsingType) {
        this(parsingType, null, null, -1);
    }

    /**
     * constructor
     * 
     * @param parsingType
     */
    public GenericParser(Class<T> parsingType, String prefix, String postfix, int precision) {
        super();
        this.parsingType = parsingType;
        this.prefix = prefix;
        this.postfix = postfix;
        this.precision = precision;
    }

    public T parse(String source) {
        try {
            return (T) parseObject(source);
        } catch (ParseException e) {
            return (T) ManagedException.forward(e);
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public T parseObject(String source, ParsePosition pos) {
        return (T) getParsingFormat().parseObject(source, pos);
    }

    protected Format getParsingFormat() {
        if (parsingFormat == null) {
            if (parsingPattern != null) {
                parsingFormat = new RegExpFormat(parsingPattern,
                    null,
                    0,
                    0,
                    FormatUtil.getDefaultExtendedFormat(parsingType, prefix, postfix, precision, scale));
            } else {
                parsingFormat = FormatUtil.getDefaultExtendedFormat(parsingType, prefix, postfix, precision, scale);
            }
        }
        return parsingFormat;
    }
    
    /**
     * isNumber
     * @return true, if internal formatter is a number format
     */
    @Override
    public boolean isNumber() {
        return getParsingFormat() instanceof NumberFormat;
    }

    /**
     * getPostfix
     * @return
     */
    public String getPostfix() {
        return Currency.getInstance(postfix).getSymbol();
    }
    
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        return getParsingFormat() != null && obj != null ? getParsingFormat().format(obj, toAppendTo, pos) : super.format(obj, toAppendTo, pos);
    }
}
