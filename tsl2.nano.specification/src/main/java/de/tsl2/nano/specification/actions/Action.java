/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.03.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.specification.actions;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.MethodUtil;
import de.tsl2.nano.specification.AbstractRunnable;
import de.tsl2.nano.specification.ParType;
import de.tsl2.nano.specification.Pool;

/**
 * Action to be loaded by ActionPool and provided to SpecifiedAction.
 * To {@link #run(Map, Object...)} this action, you have (if its not static) to provide the "instance" object inside the given context
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Action<T> extends AbstractRunnable<T> {
    public  static final String KEY_CONTEXT_INSTANCE = "instance";

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
        createParameter(getMethod(declaringClass.getName() + "." + operation + "()"));
    }

    public Action(String method) {
        this(getMethod(method));
    }

    public Action(String name, String method, LinkedHashMap<String, ParType> parameter) {
    	this(name, method);
    	this.parameter = parameter;
    }
    
    public Action(String name, String method) {
        this(getMethod(method));
        this.name = name;
    }

    public Class<?> getDeclaringClass() {
        return declaringClass;
    }
    
    /**
     * createMethod
     * @param method
     * @return 
     */
    public static Method getMethod(String method) {
        method = !method.contains("(") ? method + "()" : method;
        return MethodUtil.fromGenericString(method);
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
    /** 
     * you have (if its not static) to provide the "instance" object and the method/operation/action arguments inside the given context.
     * the arguments may be given as sequence, or with keys like "arg1", "arg2" etc.
     */
    public T run(Map<String, Object> context, Object... extArgs) {
        Object instance = context.remove(KEY_CONTEXT_INSTANCE);
        if (parameter == null)
        	createParameter(getMethod(declaringClass.getName() + "." + getOperation()));
        return (T) BeanClass.getBeanClass(declaringClass).callMethod(instance, getOperation(),
            getParameterList().toArray(new Class[0]), checkedArguments(context, false).values()
                .toArray());
    }
    
    /** convenience to create and run a runnable e.g. to run a kind of a proxy */
    @SuppressWarnings("unchecked")
    public static <R> R defineAndRun(String name, Object instance, String actionMethodName, Class<R> resultType, Object... args) {
        Action<?> runnable;
        if (ENV.get(Pool.class).exists(name)) {
            runnable = ENV.get(Pool.class).get(name, Action.class);
        } else {
            runnable = (Action<?>) ENV.get(Pool.class).add(name, instance.getClass().getName() + "." + actionMethodName);
        }
        Map<String, Object> context = MapUtil.asArgMap("arg", 1, args);
        context.put(KEY_CONTEXT_INSTANCE, instance);
        return (R) runnable.run(context);
    }

}
