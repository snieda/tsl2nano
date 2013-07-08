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
 * standard converter to convert an object into a string and vice versa - using a given {@link Format}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class StrConverter<T> implements IConverter<T, String> {
    Format format;
    /**
     * constructor
     * @param format
     */
    public StrConverter(Format format) {
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
            ForwardedException.forward(e);
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
