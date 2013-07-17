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

import java.text.Format;
import java.text.ParsePosition;

/**
 * is able to format the given type to a string and to parse a string into an object of the given type. uses
 * {@link DefaultFormat} to format to a string and {@link FormatUtil} and {@link RegExpFormat} to parse a
 * string to an object.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class GenericParser<T> extends DefaultFormat {

    /** serialVersionUID */
    private static final long serialVersionUID = 5202034503591423763L;

    protected Class<T> parsingType;
    protected String parsingPattern;
    private Format parsingFormat;

    
    /**
     * constructor to be de-serializable
     */
    public GenericParser() {
        super();
    }

    /**
     * constructor
     * 
     * @param parsingType
     */
    public GenericParser(Class<T> parsingType) {
        super();
        this.parsingType = parsingType;
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
                    FormatUtil.getDefaultFormat(parsingType, true));
            } else {
                parsingFormat = FormatUtil.getDefaultFormat(parsingType, true);
            }
        }
        return parsingFormat;
    }
}
