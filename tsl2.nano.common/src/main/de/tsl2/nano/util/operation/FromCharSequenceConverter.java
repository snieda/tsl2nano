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

import de.tsl2.nano.exception.ForwardedException;

/**
 * standard converter to convert an object into a string and vice versa - using a given {@link Format}. Does mostly the
 * same as {@link ToStrConverter} but swaps the FROM, TO parameter.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class FromCharSequenceConverter<T> implements IConverter<CharSequence, T> {
    Format format;

    /**
     * constructor
     * 
     * @param format
     */
    public FromCharSequenceConverter(Format format) {
        super();
        this.format = format;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence from(T toValue) {
        return format.format(toValue);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public T to(CharSequence fromValue) {
        try {
            return (T) format.parseObject(fromValue != null ? fromValue.toString() : null);
        } catch (ParseException e) {
            ForwardedException.forward(e);
            return null;
        }
    }
}
