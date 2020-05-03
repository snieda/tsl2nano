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

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.Parameter;
import de.tsl2.nano.action.Parameters;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IConnector;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.specification.ParType;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.actions.Action;

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
        super(name, ENV.translate(name, true));
        this.name = name;
        this.instance = instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RETURNTYPE action() throws Exception {
        if (instance == null && !Util.isEmpty(getParameter()))
            instance = getParameter(0);
        //fill specified action parameters
        Set<String> argNames = getArgumentNames();
        LinkedHashMap<String, Object> pars = new LinkedHashMap<String, Object>();
        int i = 1;
        for (String argName : argNames) {
            if (getParameter().length <= i)
                break;
            pars.put(argName, getParameter(i++));
        }
        if (instance != null) {
            pars.putAll(BeanUtil.toValueMap(instance));
            pars.put("instance", instance);
        }
        Action<?> a = getActionRunner();
        Object result = a.run(pars);
        if (BeanUtil.isStandardType(result)) {
            Message.send(ENV.translate("tsl2nano.result.information", false, getShortDescription(), result));
            return (RETURNTYPE) result;
        } else {
            return (RETURNTYPE) result;
        }
    }

    /**
     * getActionRunner
     * @return
     */
    public Action<?> getActionRunner() {
        return ENV.get(Pool.class).get(name, Action.class);
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
    public boolean isConnected() {
    	return instance != null;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getArgumentTypes() {
        return getActionRunner().getParameterList().toArray(new Class[0]);
    }
    
    public Set<String> getArgumentNames() {
        return getActionRunner().getParameter() != null ?  getActionRunner().getParameter().keySet() : new HashSet<>();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Parameters parameters() {
        //to be compatible...
        if (parameter == null) {
            Map<String, ParType> parMap = getActionRunner().getParameter();
            parameter = new Parameters(parMap.size());
            Set<String> keys = parMap.keySet();
            for (String k : keys) {
                parameter.add(new Parameter(k, new Constraint<>(parMap.get(k).getType()), null));
            }
        }
        return parameter;
    }
    
    @Override
    public void disconnect(Object connectionEnd) {
        //nothing to clean..
    }

    @Override
    public String getImagePath() {
        String icon = super.getImagePath();
        //fallback icon
        if (icon == null || !new File(icon).exists())
            return "icons/images_all.png"; 
        return super.getImagePath();
    }
    
    @Commit
    private void initDesializing() {
        this.id = this.name;
        this.shortDescription = ENV.translate(this.name, true);
        this.longDescription = this.shortDescription;
    }
}
