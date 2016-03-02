package de.tsl2.nano.util.operation;

import java.io.Serializable;
import java.util.List;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.PrimitiveUtil;


/**
 * Simplest implementation of {@link IRange}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class Range<T> implements IRange<T>, Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = -8480523674748971225L;
    
    /** the range minimum value */
    protected T from;
    /** the range maximum value */
    protected T to;

    /**
     * constructor for deserialization
     */
    protected Range() {
    }
    /**
     * constructor
     * @param from
     * @param to
     */
    public Range(T from, T to) {
        super();
        this.from = from;
        this.to = to;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getFrom() {
        return from;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getTo() {
        return to;
    }

    
    /**
     * @param from The from to set.
     */
    public void setFrom(T from) {
        this.from = from;
    }
    /**
     * @param to The to to set.
     */
    public void setTo(T to) {
        this.to = to;
    }
    /**
     * evaluates all primitive attributes of the given instance type and sets minimum values for them
     * @param instance range instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setPrimitiveMinValues(Object instance) {
        BeanClass<? extends Object> cls = BeanClass.getBeanClass(instance.getClass());
        List<IAttribute> attributes = cls.getAttributes();
        for (IAttribute a : attributes) {
            if (a.getType().isPrimitive()) {
                a.setValue(instance, PrimitiveUtil.getMinimumValue(a.getType()));
            }
        }
    }

    /**
     * evaluates all primitive attributes of the given instance type and sets maximum values for them
     * @param instance range instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setPrimitiveMaxValues(Object instance) {
        BeanClass<? extends Object> cls = BeanClass.getBeanClass(instance.getClass());
        List<IAttribute> attributes = cls.getAttributes();
        for (IAttribute a : attributes) {
            if (a.getType().isPrimitive()) {
                a.setValue(instance, PrimitiveUtil.getMaximumValue(a.getType()));
            }
        }
    }
}