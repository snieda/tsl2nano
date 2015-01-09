/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Scanner;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;

import de.tsl2.nano.bean.def.IConstraint;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.StringUtil;

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
    }

    public Action(Class<?> mainClass, String method, String... argumentNames) {
        this(mainClass, method, null, argumentNames);
    }
    
    public Action(Class<?> mainClass, String method, T defaultValue, String... argumentNames) {
        super(method, null, Type.Action, defaultValue, null);
        this.mainClass = mainClass;
        this.method = method;
        argNames = argumentNames;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    T run(Properties context) {
        Class[] cls = new Class[argNames.length];
        Properties p = new Properties();
        String v;
        for (int i = 0; i < argNames.length; i++) {
            v = context.getProperty(argNames[i]);
            if (v != null)
                p.put("arg" + (i+1), v);
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

    @SuppressWarnings("rawtypes")
    protected void initConstraints(IConstraint constraints) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
        try {
            LogFactory.setPrintToConsole(true);
            value = run(env);
            changed = true;
            //let us see the result
            new Scanner(in).nextLine();
        } finally {
            LogFactory.setPrintToConsole(false);
        }
        return caller;
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
