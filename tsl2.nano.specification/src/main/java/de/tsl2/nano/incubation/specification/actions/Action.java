/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.03.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.specification.actions;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.specification.AbstractRunnable;
import de.tsl2.nano.incubation.specification.ParType;

/**
 * Action to be loaded by ActionPool and provided to SpecifiedAction.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Action<T> extends AbstractRunnable<T> {
    @Attribute
    Class<?> declaringClass;
    /** serialVersionUID */
    private static final long serialVersionUID = -7644005869196795158L;

    /**
     * constructor
     */
    public Action() {
        super();
    }

    /**
     * constructor
     * 
     * @param operation
     * @param parameter
     */
    public Action(String name, Class<?> declaringClass, String operation, LinkedHashMap<String, ParType> parameter) {
        super(name, operation, parameter);
        this.declaringClass = declaringClass;
        createParameter(getMethod(declaringClass.getName() + "." + operation));
    }

    public Action(String method) {
        this(getMethod(method));
    }

    public Action(String name, String method) {
        this(getMethod(method));
        this.name = name;
    }

    /**
     * createMethod
     * @param method
     * @return 
     */
    public static Method getMethod(String method) {
        String clsName = StringUtil.substring(method, null, ".", true);
        String methodName = StringUtil.substring(method, ".", null, true);
        Class<?> cls = BeanClass.load(clsName);
        return PrivateAccessor.findMethod(cls, methodName, null).iterator().next();
    }
    
    /**
     * constructor
     * 
     * @param declaringClass
     */
    public Action(Method method) {
        super();
        this.declaringClass = method.getDeclaringClass();
        setOperation(method.getName());
        this.name = getOperation();
        init(method);
    }

	private void init(Method method) {
		createParameter(method);
        initDeserializing();
	}

    private void createParameter(Method method) {
        Class<?>[] types = method.getParameterTypes();
        parameter = new LinkedHashMap<String, ParType>();
        for (int i = 0; i < types.length; i++) {
            parameter.put("arg" + (i + 1), new ParType(types[i]));
        }
    }

    @Override
    public String prefix() {
    	return "!";
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public T run(Map<String, Object> context, Object... extArgs) {
        Object instance = context.remove("instance");
        if (parameter == null)
        	createParameter(getMethod(declaringClass.getName() + "." + getOperation()));
        return (T) BeanClass.getBeanClass(declaringClass).callMethod(instance, getOperation(),
            getParameterList().toArray(new Class[0]), checkedArguments(context, false).values()
                .toArray());
    }
}
