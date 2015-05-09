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

import java.math.BigDecimal;
import java.text.Format;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Complete;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.format.FormatUtil;
import de.tsl2.nano.format.GenericParser;

/**
 * Checks constraints of a given value
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class Constraint<T> extends AbstractConstraint<T> implements IConstraint<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -4402914326367553367L;
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

    public Constraint(Class<T> type, T... allowedValues) {
        this(type, Arrays.asList(allowedValues));
    }
    /**
     * tries to set values from given type and allowedValues. will set the length, scale and precision.
     * @param type allowed type
     * @param allowedValues allowed values. the first value will be used as default value.
     */
    public Constraint(Class<T> type, Collection<T> allowedValues) {
        super();
        setType(type);
        if (allowedValues.size() > 0) {
            setRange(allowedValues);
            setDefault(allowedValues.iterator().next());
            int length = -1;
            for (T v : allowedValues) {
                length = Math.max(length, v.toString().length());
            }
            setLength(length);
            
            if (BigDecimal.class.isAssignableFrom(type)) {
                int scale = -1, precision = -1;
                for (T v : allowedValues) {
                    BigDecimal b = (BigDecimal)v;
                    scale = Math.max(scale, b.scale());
                    precision = Math.max(precision, b.precision());
                }
                setScale(scale);
                setPrecision(precision);
            }
        }
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
            if (!s.ok()) {
                throw new ManagedException(s.message());
            }
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

    public static <T, G> Format createFormat(Class<T> type) {
        Format format = null;
        if (Collection.class.isAssignableFrom(type)) {
//            format = new CollectionExpressionTypeFormat<T>((Class<T>) generic);
        } else if (Map.class.isAssignableFrom(type)) {
//            format = new MapExpressionFormat<T>((Class<T>) generic);
        } else if (type.isEnum()) {
            format = FormatUtil.getDefaultFormat(type, true);
        } else if (BeanUtil.isStandardType(type)) {
            format = FormatUtil.getDefaultFormat(type, true);
            //not all types have default formats
            if (format == null) {
                format = new GenericParser<T>(type);
            }
        } else {
//                setFormat(new ValueExpressionTypeFormat<T>(type));
        }
        return format;
    }

    @Override
    public Class<T> getType() {
        if (type == null) {
            type = (Class<T>) Object.class;
        }
        return type;
    }

    /** define maximum length */
    @SuppressWarnings("rawtypes")
    @Override
    public <C extends IConstraint<T>> C setType(Class<T> type) {
        this.type = type;
        if (type.isEnum()) {
            setRange(CollectionUtil.getEnumValues((Class<Enum>) type));
        }
        return (C) this;
    }

    @Persist
    private void initSerialization() {
        //simple-xml has problems on deserializing anonymous classes
        if (format != null && format.getClass().isAnonymousClass()) {
            format = null;
        }
        //the enum values should not be persisted
        if (Enum.class.isAssignableFrom(getType())) {
            allowedValues = null;
        }
    }

    @Complete
    private void afterSerialization() {
        initDeserialization();
    }

    @Commit
    private void initDeserialization() {
        if (Enum.class.isAssignableFrom(getType())) {
            allowedValues = CollectionUtil.getEnumValues((Class<Enum>) getType());
        }
        if (format == null) {
            format = createFormat(getType());
        }
    }
}
