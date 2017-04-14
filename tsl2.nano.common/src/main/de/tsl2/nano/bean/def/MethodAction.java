/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 05.01.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.annotation.Action;
import de.tsl2.nano.bean.annotation.ConstraintValueSet;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.format.RegExpFormat;

/**
 * uses the {@link IAction} to call a {@link Method}. The method can be enriched through annotations {@link Action} and
 * {@link Constraint}.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class MethodAction<T> extends CommonAction<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8818848476107346589L;

    @Element
    Class declaringClass;
    @Attribute
    String methodName;
    @Element
    Class[] parameterTypes;
    transient Method method;
    transient Constraint[] constraints;

    static final String ACTION_PREFIX = "action";

//    /**
//     * constructor
//     */
//    public MethodAction() {
//        super();
//    }
//
    /**
     * constructor
     * 
     * @param method
     */
    public MethodAction(Method method) {
        super(createId(method), createName(method),
            Messages.getStringOpt(method.toGenericString()));
        setMethod(method);
        String imagePath = getId() + ".icon";
        if (Messages.hasKey(imagePath)) {
            setImagePath(Messages.getString(imagePath));
        } else {
            setImagePath("icons/go.png");
        }
    }

    void setMethod(Method method) {
        this.method = method;
        this.declaringClass = method.getDeclaringClass();
        this.methodName = method.getName();
        this.parameterTypes = method.getParameterTypes();
    }

    /**
     * creates a readable unique method string - cuts method name prefix {@link #ACTION_PREFIX}.
     * 
     * @param m method
     * @return method id
     */
    static final String createId(Method m) {
        final String cls = m.getDeclaringClass().getSimpleName().toLowerCase();
        final String name = m.getName().substring(ACTION_PREFIX.length());
        return cls + "." + name.toLowerCase();
    }

    private static String createName(Method method) {
        final String name = method.getName().substring(ACTION_PREFIX.length());
        return ENV.translate(name, true);
    }

    @Override
    public T action() throws Exception {
        Object[] args = Arrays.copyOfRange(getParameter(), 1, getParameter().length);
        return (T) method.invoke(getParameter()[0], args);
    }

    /**
     * reads {@link Method} annotations {@link Action} and {@link Constraint} to create method argument constraints
     * 
     * @return method argument constraints
     */
    public Constraint[] getConstraints() {
        if (constraints == null && method.isAnnotationPresent(Action.class)) {
            Action annAction = method.getAnnotation(Action.class);
            if (!annAction.name().isEmpty())
                shortDescription = ENV.translate(annAction.name(), true);
            Annotation[][] cana;
            if ((cana = method.getParameterAnnotations()) != null) {
                constraints = new Constraint[cana.length];
                //on each parameter, the constraint annotation has to be the first one
                de.tsl2.nano.bean.annotation.Constraint c;
                for (int i = 0; i < cana.length; i++) {
                    if (cana[i].length > 0) {
                        c = (de.tsl2.nano.bean.annotation.Constraint) cana[i][0];
                        //IMPROvE: allowed are only strings...
                        String[] allowed = ConstraintValueSet.preDefined(c.allowed());
                        constraints[i] =
                            new Constraint(c.type() != Object.class ? c.type() : getArgumentTypes()[i],
                                allowed.length > 0 ? allowed : null);
                        constraints[i].defaultValue = !c.defaultValue().isEmpty() ? c.defaultValue() : null;
                        constraints[i].format =
                            !c.pattern().isEmpty() ? c.length() > -1 ? new RegExpFormat(c.pattern(), c.length())
                                : new RegExpFormat(c.pattern(), null) : null;
                        constraints[i].length = c.length();
                        constraints[i].min = !c.min().isEmpty() ? c.min() : null;
                        constraints[i].max = !c.max().isEmpty() ? c.max() : null;
                    }
                }
            }
        }
        return constraints;
    }

    @Override
    public Class[] getArgumentTypes() {
        return method.getParameterTypes();
    }

    /**
     * evaluates unique method argument names either through its parameter types or {@link Action#argNames()}.
     * 
     * @return unique argument names
     */
    public String[] getArgumentNames() {
        Action ana;
        if (method.isAnnotationPresent(Action.class)
            && (ana = method.getAnnotation(Action.class)).argNames().length > 0) {
            return ana.argNames();
        } else {
            return parameters().getNames();
        }
    }

    /**
     * convenience for any parametrized action to evaluate its argument names
     * 
     * @param action any action
     * @return default argument names or - if annotations found - defined argument names
     */
    public static final String[] getArgumentNames(IAction action) {
        return action instanceof MethodAction ? ((MethodAction) action).getArgumentNames()
            : action instanceof CommonAction ? ((CommonAction)action).parameters().getNames() : getIdNames(action.getArgumentTypes());
    }

    /**
     * uses names of given argument classes. if not unique, adds a counter to be unique.
     * 
     * @param argumentTypes
     * @return unique argument names
     */
    public static final String[] getIdNames(Class[] argumentTypes) {
        ArrayList<String> argNames = new ArrayList<String>(argumentTypes.length);
        String name;
        int i = 0;
        for (Class cls : argumentTypes) {
            name = cls.getSimpleName();
            name = argNames.contains(name) ? name + ++i : name;
            argNames.add(name);
        }
        return argNames.toArray(new String[0]);
    }

    /**
     * materializes a {@link Method} through the given arguments.
     * 
     * @param declaringClass methods declaring class
     * @param name method name
     * @param pars method parameters
     * @return instance of {@link Method}
     */
    static Method getMethod(Class declaringClass, String name, Class... pars) {
        try {
            return declaringClass.getMethod(name, pars);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * creates a new bean through informations of argument names and constraints
     * 
     * @return
     */
    public BeanDefinition toBean() {
        Bean b = new Bean();
        b.setName(getShortDescription());
        String[] args = getArgumentNames();
        Constraint[] c = getConstraints();
        for (int i = 0; i < args.length; i++) {
            b.addAttribute(new BeanValue<T>(new ValueHolder(null), args[i], c != null && c.length > i ? c[i] : null));
        }
        b.addAction(this);
        b.setDefault(false);
        return b;
    }

    @Commit
    private void initDeserializing() {
        method = getMethod(declaringClass, methodName, parameterTypes);
    }
}
