package de.tsl2.nano.incubation.terminal.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.util.Util;

public class MainAction<T> extends Action<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 7657014766656806827L;

    transient IRunnable<T, Properties> runner;

    public MainAction() {
    }

    public MainAction(String name, String mainClass, String... argumentNames) {
        super(name, null, mainClass + ".main", argumentNames);
    }
    
    public MainAction(Class<?> mainClass, String... argumentNames) {
        this(mainClass.getSimpleName(), mainClass, argumentNames);
    }
    
    public MainAction(String name, Class<?> mainClass, String... argumentNames) {
        super(name, mainClass, "main", argumentNames);
    }

    /**
     * constructor
     * @param name
     * @param mainClass
     * @param method
     * @param defaultValue
     * @param argumentNames
     */
    protected MainAction(String name, Class<?> mainClass, String method, T defaultValue, String... argumentNames) {
        super(name, mainClass, method, defaultValue, argumentNames);
    }
    
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public T run(Properties context) {
        List<String> argList = createArguments();
        Object a;
        /*
         * a main method normally gets no null arguments. calling it inside a jvm, arguments may be null
         * and shouldn't be shift the next arguments. so, we collect all arguments inclusive null values,
         * removing the null values from the end!
         */
        for (int i = 0; i < argNames.length; i++) {
            a = context.get(argNames[i]);
            argList.add(Util.asString(a));
        }
        for (int i = argList.size() - 1; i >= 0; i--) {
            if (argList.get(i) == null) {
                argList.remove(i);
            } else {
                break;
            }
        }
        
        context.put("arg1", argList.toArray(new String[0]));
        if (runner == null) {
            try {
                runner = new de.tsl2.nano.incubation.specification.actions.Action(getMainClass().getMethod(method,
                    new Class[] { String[].class }));
            } catch (Exception e) {
                ManagedException.forward(e);
            }
        }
        return runner.run(context);
    }

    /**
     * createArguments
     * @return
     */
    protected ArrayList<String> createArguments() {
        return new ArrayList<String>(argNames.length);
    }
    
    @Override
    public String getDescription(Properties env, boolean full) {
        //print the main help screen
        run(new Properties());
        return super.getDescription(env, full);
    }

}
