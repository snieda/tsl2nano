/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 02.03.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import java.util.LinkedHashMap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IConnector;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.incubation.specification.actions.ActionPool;

/**
 * action defined in action pool
 * 
 * @author Tom
 * @version $Revision$
 */
public class SpecifiedAction<RETURNTYPE> extends SecureAction<RETURNTYPE> implements IConnector<Object> {
    /** serialVersionUID */
    private static final long serialVersionUID = 6277309054654171553L;

    @Attribute
    String name;

    transient Object instance;

    /**
     * constructor
     */
    public SpecifiedAction() {
        super();
    }

    /**
     * constructor
     * 
     * @param name
     * @param instance
     */
    public SpecifiedAction(String name, Object instance) {
        super(name, Environment.translate(name, true));
        this.name = name;
        this.instance = instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RETURNTYPE action() throws Exception {
        Object result = Environment.get(ActionPool.class).get(name)
            .run(instance != null ? BeanUtil.toValueMap(instance) : new LinkedHashMap<String, Object>());
        if (BeanUtil.isStandardType(result)) {
            Message.send(Environment.translate("tsl2nano.result.information", false, getShortDescription(), result));
            return null;
        } else {
            return (RETURNTYPE) result;
        }
    }

    /**
     * setInstance
     * 
     * @param instance
     */
    @Override
    public Object connect(Object instance) {
        this.instance = instance;
        return this;
    }

    @Override
    public void disconnect(Object connectionEnd) {
        //nothing to clean..
    }
    
    @Commit
    private void initDesializing() {
        this.id = this.name;
        this.shortDescription = Environment.translate(this.name, true);
        this.longDescription = this.shortDescription;
    }
}
