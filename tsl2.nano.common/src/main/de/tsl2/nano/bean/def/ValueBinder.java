/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 2, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;

import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.messaging.ChangeEvent;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.util.operation.IConverter;

/**
 * binds two values to be synchronized. not thread-safe.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ValueBinder<FIRST, SECOND> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -2284366281259367732L;
    
    IValueAccess<FIRST> firstValue;
    IValueAccess<SECOND> secondValue;
    transient IConverter<FIRST, SECOND> converter;
    transient boolean updating;

    public static final String ATTR_FIRSTVALUE = "firstValue";
    public static final String ATTR_SECONDVALUE = "secondValue";

    /**
     * constructor to be serializable
     */
    protected ValueBinder() {
        super();
    }

    /**
     * constructor
     * 
     * @param firstValue
     * @param secondValue
     * @param converter
     */
    public ValueBinder(IValueAccess<FIRST> firstValue,
            IValueAccess<SECOND> secondValue,
            IConverter<FIRST, SECOND> converter) {
        super();
        this.firstValue = firstValue;
        this.secondValue = secondValue;
        if (converter != null)
            this.converter = converter;
        else {
            this.converter = new IConverter<FIRST, SECOND>() {

                @Override
                public FIRST from(SECOND toValue) {
                    return (FIRST) toValue;
                }

                @Override
                public SECOND to(FIRST fromValue) {
                    return (SECOND) fromValue;
                }
            };
        }

        createBinding();
    }

    private void createBinding() {
        firstValue.changeHandler().addListener(new IListener<ChangeEvent>() {
            @Override
            public void handleEvent(ChangeEvent changeEvent) {
                if (!updating && changeEvent.hasChanged) {
                    try {
                        updating = true;
                        secondValue.setValue(converter.to((FIRST) changeEvent.newValue));
                    } finally {
                        updating = false;
                    }
                }
            }
        });
        secondValue.changeHandler().addListener(new IListener<ChangeEvent>() {
            @Override
            public void handleEvent(ChangeEvent changeEvent) {
                if (!updating && changeEvent.hasChanged) {
                    try {
                        updating = true;
                        firstValue.setValue(converter.from((SECOND) changeEvent.newValue));
                    } finally {
                        updating = false;
                    }
                }
            }
        });
    }

    public FIRST getFirstValue() {
        return firstValue.getValue();
    }

    public void setFirstValue(FIRST value) {
        firstValue.setValue(value);
    }

    public SECOND getSecondValue() {
        return secondValue.getValue();
    }

    public void setSecondValue(SECOND value) {
        secondValue.setValue(value);
    }
}
