/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 29, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.operation;

import java.text.Format;
import java.text.ParseException;

import de.tsl2.nano.core.ManagedException;

/**
 * standard converter to convert an object into a string and vice versa - using a given {@link Format}. Does exactly the
 * same as {@link FromCharSequenceConverter} but swaps the FROM, TO parameter.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ToStrConverter<T> implements IConverter<T, String> {
    Format format;
    /**
     * constructor
     * @param format
     */
    public ToStrConverter(Format format) {
        super();
        this.format = format;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T from(String toValue) {
        try {
            return (T) format.parseObject(toValue);
        } catch (ParseException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String to(T fromValue) {
        return format.format(fromValue);
    }
}
