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
import java.util.Properties;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;

import de.tsl2.nano.bean.def.IConstraint;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.terminal.IItem;
import de.tsl2.nano.incubation.terminal.Terminal;

/**
 * action to be used on {@link Terminal}s. no inline {@link Runnable} are supported as instances of this class must be
 * serializable! So we do that only through reflection.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Action<T> extends AItem<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 5286326570932592363L;

    public static final String KEY_ENV = "ENVIRONMENT";

    @Element
    Class<?> mainClass;
    @Element
    String method;
    @ElementArray
    String[] argNames;

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

    public Action(String name, Class<?> mainClass, String method, String... argumentNames) {
        this(name, mainClass, method, null, argumentNames);
    }

    public Action(Class<?> mainClass, String method, T defaultValue, String... argumentNames) {
        this(method, mainClass, method, defaultValue, argumentNames);
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
            if (KEY_ENV.equals(argNames[i])) {
                v = context;
            } else {
                v = context.get(argNames[i]);
            }
            if (v != null) {
                p.put("arg" + (i + 1), v);
            }
            cls[i] = v != null ? v.getClass() : String.class;
        }
        IRunnable<T, Properties> runner = null;
        try {
            runner = new de.tsl2.nano.incubation.specification.actions.Action(mainClass.getMethod(method, cls));
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return runner.run(p);
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
            ex.printStackTrace();
            return caller == this ? getParent().next(in, out, env) : this;
        } finally {
            nextLine(in, out);
            LogFactory.setPrintToConsole(false);
        }
        return caller == this ? getParent().next(in, out, env) : this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEditable() {
        return false;
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
