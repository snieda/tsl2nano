/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal.item;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;

import de.tsl2.nano.bean.def.IConstraint;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.terminal.IItem;
import de.tsl2.nano.incubation.terminal.SIShell;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * action to be used on {@link SIShell}s. no inline {@link Runnable} are supported as instances of this class must be
 * serializable! So we do that only through reflection.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Action<T> extends AItem<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 5286326570932592363L;

    public static final String KEY_ENV = "ENVIRONMENT";

    /** mainClass, if null, the {@link #method} must start with it's full class path */
    @Element(required = false)
    Class<?> mainClass;
    /** method. see {@link #mainClass} */
    @Element
    String method;
    @ElementArray
    String[] argNames;

    transient Object instance;

    /**
     * constructor
     */
    public Action() {
        super();
        type = Type.Action;
        prefix.setCharAt(PREFIX, '!');
    }

    public Action(Class<?> mainClass, String method, String... argumentNames) {
        this(mainClass, method, null, argumentNames);
    }

    public Action(Object instance, String method, String... argumentNames) {
        this(method, instance, method, null, argumentNames);
    }

    public Action(String name, Class<?> mainClass, String method, String... argumentNames) {
        this(name, mainClass, method, null, argumentNames);
    }

    public Action(Class<?> mainClass, String method, T defaultValue, String... argumentNames) {
        this(method, mainClass, method, defaultValue, argumentNames);
    }

    public Action(String name, Object instance, String method, T defaultValue, String... argumentNames) {
        this(name, instance.getClass(), method, defaultValue, argumentNames);
        this.instance = instance;
    }

    public Action(String name, Class<?> mainClass, String method, T defaultValue, String... argumentNames) {
        super(name, null, Type.Action, defaultValue, null);
        this.mainClass = mainClass;
        this.method = method;
        argNames = argumentNames;
        prefix.setCharAt(PREFIX, '!');
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    T run(Properties context) {
        Class[] cls = new Class[argNames.length];
        Properties p = new Properties();
        Object v;
        for (int i = 0; i < argNames.length; i++) {
            String argName = StringUtil.substring(argNames[i], ")", null);
            if (KEY_ENV.equals(argName)) {
                v = context;
            } else if (isReference(argName)) {
                v = resolveReference(argName);
            } else {
                v = context.get(argName);
            }
            //optional casting
            String c = StringUtil.substring(argNames[i], "(", ")", false, true);
            Class<?> cast = c != null ? BeanClass.createBeanClass(c).getClazz() : String.class;
            if (v != null) {
                p.put("arg" + (i + 1), v);
            }
            cls[i] = c != null ? cast : v != null ? BeanClass.getDefiningClass(v.getClass()) : cast;
        }
        if (instance != null)
            p.put("instance", instance);
        IRunnable<T, Properties> runner = null;
        try {
            Set<Method> methods = PrivateAccessor.findMethod(mainClass, method, null, cls);
            //if nothing found, throw a nosuchmethod exception
            Method m = methods.size() > 0 ? methods.iterator().next() : getMainClass().getMethod(method, cls);
            runner = new de.tsl2.nano.incubation.specification.actions.Action(m);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return runner.run(p);
    }

    /**
     * creates a reference name as concatenation of cls + field. result is:<br/>
     * <@>cls.getName()<:>field
     * 
     * @param cls class holding the field
     * @param field field name
     * @return string describing the field reference.
     */
    public static String createReferenceName(Class<?> cls, String field) {
        return "@" + cls.getName() + ":" + field;
    }

    private boolean isReference(String expression) {
        return expression.startsWith("@");
    }

    @SuppressWarnings("rawtypes")
    private Object resolveReference(String expression) {
        String description[] = expression.substring(1).split(":");
        BeanClass bc = BeanClass.createBeanClass(description[0]);
        return BeanClass.getStatic(bc.getClazz(), description[1]);
    }

    protected Class<?> getMainClass() {
        if (mainClass == null && method.contains(".")) {
            String mainClassName = StringUtil.substring(method, null, ".", true);
            mainClass = BeanClass.createBeanClass(mainClassName).getClazz();
            method = StringUtil.substring(method, ".", null, true);
        }
        return mainClass;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void initConstraints(IConstraint constraints) {
    }

    @Override
    public String ask(Properties env) {
        return "...starting action " + getName() + " ...";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
        try {
            LogFactory.setPrintToConsole(true);
            Properties p = new Properties();
            p.putAll(env);
            p.putAll(System.getProperties());
            value = run(p);
            changed = true;
            //let us see the result
        } catch (Exception ex) {
//            ex.printStackTrace();
            return getParent().next(in, out, env);
        } finally {
            LogFactory.setPrintToConsole(false);
            out.print("done: " + (value != null ? "[" + StringUtil.toString(value, 30) + "]" : "")
                + " ...please hit enter to return");
            nextLine(in, out);
        }
        return getParent().next(in, out, env);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEditable() {
        return false;
    }

    /**
     * setInstance
     * 
     * @param instance
     */
    public void setInstance(Object instance) {
        this.instance = instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        prefix.setCharAt(PREFIX, '!');
        return getPresentationPrefix() + name + ": "
            + (changed ? StringUtil.toString(value, 60) : "<not activated yet>") + "\n";
    }
}
