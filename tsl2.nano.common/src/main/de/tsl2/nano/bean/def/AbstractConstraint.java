/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 28.08.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.text.Format;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.Util;

/**
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public abstract class AbstractConstraint<T> implements IConstraint<T>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -2721455376198486249L;
    private static final Log LOG = LogFactory.getLog(AbstractConstraint.class);

    @Override
    public void check(String name, T value) {
        IStatus status = checkStatus(name, value);
        if (status.error() != null)
            throw (RuntimeException) status.error();
    }

    /**
     * parse the given text - will not throw any exception.
     * 
     * @param text text to parse
     * @return object yield from parsing given text - or null on any error
     */
    protected T parse(String text) {
        try {
            return (T) (getFormat() != null ? (T) getFormat().parseObject(text) : getType()
                .isAssignableFrom(String.class) ? text : null);
        } catch (Exception e) {
            LOG.warn(e.toString());
            //do nothing - the null return value indicates the error
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus checkStatus(String name, T value) {
        IStatus status = IStatus.STATUS_OK;

        try {
            if (!isNullable() && value == null) {
                status = Status.illegalArgument(name, value, "not null");
            } else if (value != null) {
                Format format = getFormat();
                Comparable<T> min, max;
                int length;

                String fval =
                    value instanceof String ? (String) value : format != null ? format.format(value) : Util
                        .asString(value);
                if (!PrimitiveUtil.isAssignableFrom(getType(), value.getClass())) {
                    status = Status.illegalArgument(name, fval, getType());
                } else if (value instanceof String && parse(fval) == null) {
                    status = Status.illegalArgument(name, fval, "format '" + format + "'");
                } else if (!(value instanceof String) && fval == null) {
                    status = Status.illegalArgument(name, fval, "format '" + format + "'");
                } else if ((min = getMinimum()) != null && min.compareTo(value) > 0) {
                    status = Status.illegalArgument(name, fval, " greater than " + min);
                } else if ((max = getMaximum()) != null && max.compareTo(value) < 0) {
                    status = Status.illegalArgument(name, fval, " lower than " + max);
                } else if ((length = getLength()) > 0 && fval.length() > length) {
                    status = Status.illegalArgument(name, fval, " a maximum-length of " + length);
                }
            }
            //TODO: check numbers on scale and precision
        } catch (Exception ex) {
            status = Status.illegalArgument(name, value, ex);
        }
        if (!status.ok()) {
            LOG.warn(status);
        }
        return status;
    }

    @Override
    public String toString() {
        Comparable<T> min = getMinimum(), max = getMaximum();
        Collection<T> allowedValues = getAllowedValues();
        Class<T> type = getType();
        Format format = getFormat();
        
        Object _min = min != null ? min : (!Util.isEmpty(allowedValues) ? allowedValues.iterator().next() : null);
        Object _max =
            max != null ? max : (!Util.isEmpty(allowedValues) ? new LinkedList<T>(allowedValues).getLast() : null);
        return Util.toString(this.getClass(), type, "length: " + getLength(), "mandatory: " + !isNullable(), "\n\trange: " + _min
            + (allowedValues != null ? " ... " : " - ") + _max,
            "\n\tformat: " + format);
    }
}
