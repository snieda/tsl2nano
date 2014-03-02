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

import java.util.Map;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.BeanClass;
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
    public Action(String name, Class<?> declaringClass, String operation, Map<String, ParType> parameter) {
        super(name, operation, parameter);
        this.declaringClass = declaringClass;
    }

    /**
     * constructor
     * 
     * @param declaringClass
     */
    public Action(Class<?> declaringClass) {
        super();
        this.declaringClass = declaringClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T run(Map<String, Object> context, Object... extArgs) {
        Object instance = context.remove("instance");
        return (T) BeanClass.getBeanClass(declaringClass).callMethod(instance, operation,
            getParameterList().toArray(new Class[0]), checkedArguments(context, false).values()
                .toArray());
    }
}
