/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Sep 16, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import java.util.Arrays;

import de.tsl2.nano.messaging.ChangeEvent;
import de.tsl2.nano.messaging.IListener;

/**
 * Wraps enum value names to boolean attributes (up to 10 values). May be used for field bindings.<br>
 * This is done in a static way to have the right bean access methods. The {@link IObservableValue} given on
 * construction will be bound to this wrapped value<br>
 * 
 * <pre>
 * Example:
 * enum values: man, woman, unknown
 * 
 * possibilities:
 * value=man     --> attribute 'first' is true, all others false.
 * value=woman   --> attribute 'second' is true all others false.
 * value=unknown --> attribute 'third' is true all others false.
 * 
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class OptionsWrapper<E> {
    /** original value to be bound to this wrapper instance */
    protected IValueAccess<E> ovalue;
    E[] enumerations;
    private boolean changingValue;

    public static final String[] ATTR_NAMES = { "first",
        "second",
        "third",
        "fourth",
        "fifth",
        "sixth",
        "seventh",
        "eighth",
        "ninth",
        "tenth" };

    /**
     * constructor
     * 
     * @param ovalue orginal value to be bound to this wrapper instance
     * @param enumType enum type
     */
    public OptionsWrapper(IValueAccess<E> ovalue, Class<E> enumType) {
        this(ovalue, (enumType != null ? enumType.getEnumConstants() : null));
    }

    /**
     * constructor
     * 
     * @param ovalue orginal value to be bound to this wrapper instance. the value is holding an enum
     * @param enumValues enumeration values
     */
    public OptionsWrapper(IValueAccess<E> ovalue, E[] enumValues) {
        super();
        assert enumValues.length <= ATTR_NAMES.length : "maximum count of "+ ATTR_NAMES.length + " enumeration values exceeded";
        this.enumerations = enumValues;
        this.ovalue = ovalue;
        /*
         * if the value was set from outside, we listen to it to refresh our wrapper and
         * perhaps other data-bound values
         */
        ovalue.changeHandler().addListener(new IListener<ChangeEvent>() {
            @SuppressWarnings("unchecked")
            @Override
            public void handleEvent(ChangeEvent event) {
                if (!changingValue) {
                    Object value = event.getSource();
                    BeanValue.getBeanValue(OptionsWrapper.this, ATTR_NAMES[indexOf((E) value)]).setValue(true);
                }
            }
        });
    }

    /**
     * overwrite this method on specializations
     * 
     * @return Returns the enum constants.
     */
    public E[] getEnumConstants() {
        return enumerations;
    }

    /**
     * overwrite this method on specializations
     * 
     * @param index enum value index
     * @return true, if value on index is set
     */
    protected boolean hasValue(int index) {
        return getEnumConstants()[index] != null && getEnumConstants()[index].equals(ovalue.getValue());
    }

    /**
     * setValue overwrite this method on specializations
     * 
     * @param index enum value index
     * @param value value to set
     */
    protected void setValue(int index, boolean value) {
        if (value) {
            changingValue = true;
            ovalue.setValue(getEnumConstants()[index]);
            changingValue = false;
        }
    }

    /**
     * getValue
     * 
     * @return current selected real value
     */
    public E getValue() {
        return (E) ovalue.getValue();
    }

    /**
     * indexOf
     * @param enumValue value
     * @return index of value in enum constant array
     */
    protected int indexOf(E enumValue) {
        return Arrays.binarySearch(getEnumConstants(), enumValue);
    }
    
    /**
     * setValue
     * 
     * @param enumValue value to set
     */
    public void setValue(E enumValue) {
        int i = indexOf(enumValue);
        setValue(i, true);
    }

    /**
     * isFirst
     * 
     * @return true, if the value equals the first enum value.
     */
    public boolean isFirst() {
        return hasValue(0);
    }

    /**
     * sets the first value to the first enum value.
     * 
     * @param newValue if true, the value will be set to the first enum value. otherwise nothing will be done.
     */
    public void setFirst(boolean newValue) {
        setValue(0, newValue);
    }

    /**
     * isEighth
     * 
     * @return true, if the value equals the Eighth enum value.
     */
    public boolean isEighth() {
        return hasValue(7);
    }

    /**
     * isFifth
     * 
     * @return true, if the value equals the Fifth enum value.
     */
    public boolean isFifth() {
        return hasValue(4);
    }

    /**
     * isFourth
     * 
     * @return true, if the value equals the Fourth enum value.
     */
    public boolean isFourth() {
        return hasValue(3);
    }

    /**
     * isNinth
     * 
     * @return true, if the value equals the Ninth enum value.
     */
    public boolean isNinth() {
        return hasValue(8);
    }

    /**
     * isSecond
     * 
     * @return true, if the value equals the Second enum value.
     */
    public boolean isSecond() {
        return hasValue(1);
    }

    /**
     * isSeventh
     * 
     * @return true, if the value equals the Seventh enum value.
     */
    public boolean isSeventh() {
        return hasValue(6);
    }

    /**
     * isSixth
     * 
     * @return true, if the value equals the Sixth enum value.
     */
    public boolean isSixth() {
        return hasValue(5);
    }

    /**
     * isTenth
     * 
     * @return true, if the value equals the Tenth enum value.
     */
    public boolean isTenth() {
        return hasValue(9);
    }

    /**
     * isThird
     * 
     * @return true, if the value equals the Third enum value.
     */
    public boolean isThird() {
        return hasValue(2);
    }

    /**
     * sets the Eighth value to the Eighth enum value.
     * 
     * @param newValue if true, the value will be set to the Eighth enum value. otherwise nothing will be done.
     */
    public void setEighth(boolean newValue) {
        setValue(7, newValue);
    }

    /**
     * sets the Fifth value to the Fifth enum value.
     * 
     * @param newValue if true, the value will be set to the Fifth enum value. otherwise nothing will be done.
     */
    public void setFifth(boolean newValue) {
        setValue(4, newValue);
    }

    /**
     * sets the Fourth value to the Fourth enum value.
     * 
     * @param newValue if true, the value will be set to the Fourth enum value. otherwise nothing will be done.
     */
    public void setFourth(boolean newValue) {
        setValue(3, newValue);
    }

    /**
     * sets the Ninth value to the Ninth enum value.
     * 
     * @param newValue if true, the value will be set to the Ninth enum value. otherwise nothing will be done.
     */
    public void setNinth(boolean newValue) {
        setValue(8, newValue);
    }

    /**
     * sets the Second value to the Second enum value.
     * 
     * @param newValue if true, the value will be set to the Second enum value. otherwise nothing will be done.
     */
    public void setSecond(boolean newValue) {
        setValue(1, newValue);
    }

    /**
     * sets the Seventh value to the Seventh enum value.
     * 
     * @param newValue if true, the value will be set to the Seventh enum value. otherwise nothing will be done.
     */
    public void setSeventh(boolean newValue) {
        setValue(6, newValue);
    }

    /**
     * sets the Sixth value to the Sixth enum value.
     * 
     * @param newValue if true, the value will be set to the Sixth enum value. otherwise nothing will be done.
     */
    public void setSixth(boolean newValue) {
        setValue(5, newValue);
    }

    /**
     * sets the Tenth value to the Tenth enum value.
     * 
     * @param newValue if true, the value will be set to the Tenth enum value. otherwise nothing will be done.
     */
    public void setTenth(boolean newValue) {
        setValue(9, newValue);
    }

    /**
     * sets the Third value to the Third enum value.
     * 
     * @param newValue if true, the value will be set to the Third enum value. otherwise nothing will be done.
     */
    public void setThird(boolean newValue) {
        setValue(2, newValue);
    }
}
