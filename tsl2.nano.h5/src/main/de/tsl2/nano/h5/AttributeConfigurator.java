/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 08.01.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import de.tsl2.nano.bean.BeanClass;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.IPresentableColumn;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.util.PrivateAccessor;
import de.tsl2.nano.util.Util;

/**
 * Provides a specific set of Attribute members to configure.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class AttributeConfigurator implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    AttributeDefinition<?> attr;
    PrivateAccessor<AttributeDefinition<?>> attrAccessor;

    public AttributeConfigurator() {
        this(BeanClass.createInstance(AttributeDefinition.class));
    }

    /**
     * constructor
     * 
     * @param attr
     */
    public AttributeConfigurator(AttributeDefinition<?> attr) {
        this.attr = attr;
        attrAccessor = new PrivateAccessor<AttributeDefinition<?>>(attr);
    }

    public String getName() {
        return attr.getName();
    }

    public String getDescription() {
        return attr.getDescription();
    }

    public void setDescription(String description) {
        attrAccessor.set("description", description);
    }

    public String getType() {
        return attr.getType().getName();
    }

    public BigDecimal getLength() {
        return attr.scale() != 0 ? new BigDecimal(new BigInteger(String.valueOf(attr.length())), attr.scale())
            : new BigDecimal(attr.length());
    }

    public void setLength(BigDecimal length) {
        if (length != null) {
            //TODO: define scale and prec
            float fract = length.floatValue() - length.intValue();
            int prec = length.precision();
            int scale = length.unscaledValue().intValue() / length.intValue();
            attrAccessor.set("length", length.intValue());
            attrAccessor.set("scale", scale);
            attrAccessor.set("precision", prec);
        } else {
            attrAccessor.set("length", IPresentable.UNDEFINED);
            attrAccessor.set("scale", IPresentable.UNDEFINED);
            attrAccessor.set("precision", IPresentable.UNDEFINED);
        }
    }

    public Serializable getMin() {
        return attr.getMininum() != null ? attr.getFormat().format(attr.getMininum()) : null;
    }

    public void setMin(Serializable min) {
        attrAccessor.set("min", min);
    }

    public Serializable getMax() {
        return attr.getMaxinum() != null ? attr.getFormat().format(attr.getMaxinum()) : null;
    }

    public void setMax(Serializable max) {
        attrAccessor.set("max", max);
    }

    public String getFormat() {
        Format f = attr.getFormat();
        if (f instanceof SimpleDateFormat)
            return ((SimpleDateFormat) f).toPattern();
        else if (f instanceof NumberFormat)
            return ((DecimalFormat) f).toPattern();
        else if (f instanceof RegExpFormat)
            return ((RegExpFormat) f).getPattern();
        else
            return f != null ? f.toString() : "";
    }

    public void setFormat(String format) {
        Format f = attr.getFormat();
        if (f instanceof SimpleDateFormat)
            ((SimpleDateFormat) f).applyPattern(format);
        else if (f instanceof NumberFormat)
            ((DecimalFormat) f).applyPattern(format);
        else
            ((RegExpFormat) f).setPattern(format, null, attr.getLength(), 0);
    }
    
    public IPresentable getPresentation() {
        return attr.getPresentation();
    }

    public void setPresentation(IPresentable p) {
        attrAccessor.set("presentable", p);
    }
    
    public IPresentableColumn getColumnDefinition() {
        return attr.getColumnDefinition();
    }

    public void setColumnDefinition(IPresentableColumn c) {
        attrAccessor.set("columnDefinition", c);
    }
    
    public boolean isNullable() {
        return attr.isNullable();
    }

    public boolean isDoValidation() {
        return attr.isDoValidation();
    }

    public boolean isComposition() {
        return attr.composition();
    }

    public IAttributeDefinition<?> unwrap() {
        return attr;
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), attr);
    }
}
