/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 01.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Complete;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.Util;

/**
 * Checks constraints of a given value
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class Constraint<T> implements IConstraint<T>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -4402914326367553367L;
    private static final Log LOG = LogFactory.getLog(Constraint.class);
    @Attribute(required = false)
    Class<T> type;
    @Element(required = false)
    Comparable<T> min;
    @Element(required = false)
    Comparable<T> max;

    @Element(required = false)
    protected Format format;
    @Element(required = false)
    T defaultValue;

    @Attribute(required = false)
    private boolean nullable = true;
    @Attribute(required = false)
    private int length = UNDEFINED;
    @Element(required = false)
    private int scale = UNDEFINED;
    @Element(required = false)
    private int precision = UNDEFINED;

    @ElementList(inline = true, entry = "value", required = false)
    private transient Collection<T> allowedValues;

    /**
     * constructor
     */
    public Constraint() {
    }

    public Constraint(Class<T> type) {
        this(type, null, null);
    }

    public Constraint(T defaultValue) {
        this((Class<T>) defaultValue.getClass(), null, null);
        setDefault(defaultValue);
    }

    /**
     * constructor
     * 
     * @param type
     * @param min
     * @param max
     */
    public Constraint(Class<T> type, Comparable<T> min, Comparable<T> max) {
        super();
        setType(type);
        setRange(min, max);
    }

    /**
     * setBasicDef
     * 
     * @param length {@link #length()}
     * @param nullable {@link #nullable()}
     * @param format {@link #getPattern()}
     * @param defaultValue {@link #getDefault()}
     * @param description {@link #getDescription()}
     */
    @Override
    public <C extends IConstraint<T>> C setBasicDef(int length,
            boolean nullable,
            Format format,
            T defaultValue) {
        this.length = length;
        this.nullable = nullable;
        this.format = format;
        this.defaultValue = defaultValue;
        if (defaultValue != null) {
            IStatus s = checkStatus(getType() != null ? getType().getSimpleName() : "arg", defaultValue);
            if (!s.ok())
                throw new ManagedException(s.message());
        }
        return (C) this;
    }

    /**
     * setNumberDef
     * 
     * @param scale {@link #scale()}
     * @param precision {@link #precision()}
     */
    @Override
    public <C extends IConstraint<T>> C setNumberDef(int scale, int precision) {
        this.scale = scale;
        this.precision = precision;
        return (C) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends IConstraint<T>> C setRange(Comparable<T> min, Comparable<T> max) {
        this.min = min;
        this.max = max;
        return (C) this;
    }

    @Override
    public <C extends IConstraint<T>> C setRange(Collection<T> allowedValues) {
        this.allowedValues = allowedValues;
        return (C) this;
    }

    @Override
    public Collection<T> getAllowedValues() {
        return allowedValues;
    }

    @Override
    public Comparable<T> getMinimum() {
        return min;
    }

    @Override
    public Comparable<T> getMaximum() {
        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends IConstraint<T>> C setFormat(Format format) {
        this.format = format;
        return (C) this;
    }

    /**
     * @return Returns the scale.
     */
    @Override
    public int getScale() {
        return scale;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends IConstraint<T>> C setScale(int scale) {
        this.scale = scale;
        return (C) this;
    }

    /**
     * @return Returns the precision.
     */
    @Override
    public int getPrecision() {
        return precision;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends IConstraint<T>> C setPrecision(int precision) {
        this.precision = precision;
        return (C) this;
    }

    /**
     * @return Returns the nullable.
     */
    @Override
    public boolean isNullable() {
        return nullable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends IConstraint<T>> C setNullable(boolean nullable) {
        this.nullable = nullable;
        return (C) this;
    }

    /**
     * @return Returns the length.
     */
    @Override
    public int getLength() {
        return length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends IConstraint<T>> C setLength(int length) {
        this.length = length;
        return (C) this;
    }

    /**
     * @return Returns the defaultValue.
     */
    @Override
    public T getDefault() {
        return defaultValue;
    }

    /**
     * @param defaultValue The defaultValue to set.
     */
    @Override
    public <C extends IConstraint<T>> C setDefault(T defaultValue) {
        this.defaultValue = defaultValue;
        return (C) this;
    }

    @Override
    public Format getFormat() {
        return format;
    }

    @Override
    public Class<T> getType() {
        if (type == null)
            type = (Class<T>) Object.class;
        return type;
    }

    /** define maximum length */
    @SuppressWarnings("rawtypes")
    @Override
    public <C extends IConstraint<T>> C setType(Class<T> type) {
        this.type = type;
        if (type.isEnum())
            setRange(CollectionUtil.getEnumValues((Class<Enum>) type));
        return (C) this;
    }

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
                String fval =
                    value instanceof String ? (String) value : getFormat() != null ? getFormat().format(value) : Util
                        .asString(value);
                if (!PrimitiveUtil.isAssignableFrom(getType(), value.getClass())) {
                    status = Status.illegalArgument(name, fval, getType());
                } else if (value instanceof String && parse(fval) == null) {
                    status = Status.illegalArgument(name, fval, "format '" + format + "'");
                } else if (!(value instanceof String) && fval == null) {
                    status = Status.illegalArgument(name, fval, "format '" + format + "'");
                } else if (min != null && min.compareTo(value) > 0) {
                    status = Status.illegalArgument(name, fval, " greater than " + min);
                } else if (max != null && max.compareTo(value) < 0) {
                    status = Status.illegalArgument(name, fval, " lower than " + max);
                } else if (length > 0 && fval.length() > length) {
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

    @Persist
    private void initSerialization() {
        //simple-xml has problems on deserializing anonymous classes
        if (format != null && format.getClass().isAnonymousClass())
            format = null;
        //the enum values should not be persisted
        if (Enum.class.isAssignableFrom(getType()))
            allowedValues = null;
    }
    
    @Complete
    private void afterSerialization() {
        initDeserialization();
    }
    
    @Commit
    private void initDeserialization() {
        if (Enum.class.isAssignableFrom(getType()))
            allowedValues = CollectionUtil.getEnumValues((Class<Enum>) getType());
    }

    @Override
    public String toString() {
        Object _min = min != null ? min : (!Util.isEmpty(allowedValues) ? allowedValues.iterator().next() : null);
        Object _max =
            max != null ? max : (!Util.isEmpty(allowedValues) ? new LinkedList<T>(allowedValues).getLast() : null);
        return Util.toString(this.getClass(), type, "length: " + length, "mandatory: " + !nullable, "\n\trange: " + _min
            + (allowedValues != null ? " ... " : " - ") + _max,
            "\n\tformat: " + format);
    }
}
