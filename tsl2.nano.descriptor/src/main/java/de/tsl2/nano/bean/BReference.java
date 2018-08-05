/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 02.03.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.bean;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.cls.AReference;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;

/**
 * implements a detach/attach or de-/materialize or pointer/content algorithm for entities using the BeanContainer.
 * 
 * @author Tom
 * @version $Revision$
 */
public class BReference<O> extends AReference<Class<O>, O> {

    /**
     * constructor for deserialization
     */
    protected BReference() {
    }
    
    public BReference(O instance) {
        super(instance);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<O> type(String description) {
        return description != null ? BeanClass.load(StringUtil.substring(description, PREFIX_REFERENCE, PREFIX_ID)) : null;
    }
    
    @Override
    protected Object id(Class<O> type, String strId) {
        return BeanAttribute.wrap(strId, BeanContainer.getIdAttribute(type).getType());
    }

    
    public String name() {
        return StringUtil.substring(super.toString(), null, PREFIX_ID);
    }
    
    @Override
    protected Object getId(Object instance) {
        return BeanContainer.getIdAttribute(resolve()).getValue(instance);
    }

    @Override
    protected void checkDescription(String description) {
    	type(description);
    }
    
    @Override
    protected O materialize(String description) {
        if (!BeanContainer.isConnected())
            return null;
        Pointer tid = getTypeAndId(description);
        return BeanContainer.instance().getByID(tid.type, tid.id);
    }

    public Bean<O> bean() {
        return resolve() != null ? Bean.getBean(instance) : null;
    }
    
    @Override
    public String toString() {
        return instance != null ? Bean.getBean(instance).toString() : super.toString();
    }
}
