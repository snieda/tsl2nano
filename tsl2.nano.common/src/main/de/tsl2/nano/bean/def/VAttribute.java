/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 22.02.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.lang.reflect.Method;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.core.cls.BeanAttribute;

/**
 * virtual attribute working on {@link IValueAccess#getValue()}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class VAttribute<T> extends BeanAttribute<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8046769241735874765L;

    static final Method METHOD_VALUEACCESS = getReadAccessMethod(IValueAccess.class, IValueAccess.ATTR_VALUE, true);

    @Attribute(required=false)
    String virtualName;

    /**
     * constructor
     */
    protected VAttribute() {
    }

    /**
     * constructor
     */
    public VAttribute(String virtualName) {
        super(METHOD_VALUEACCESS);
        this.virtualName = virtualName;
    }

    @Override
    public String getName() {
        return virtualName != null ? virtualName : super.getName();
    }
    
    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public T getValue(Object beanInstance) {
        if (beanInstance == null)
            return null;
        else if (!(beanInstance instanceof IValueAccess))
            throw new IllegalArgumentException("instance of virtual attribute " + this
                + " must be of type IValueAccess, but is: " + beanInstance);
        return super.getValue(beanInstance);
    }
    
    @Override
    public void setValue(Object beanInstance, Object value) {
        if (beanInstance == null) {
            //TODO: throw error or log
            return;
        }
        super.setValue(beanInstance, value);
    }
}
