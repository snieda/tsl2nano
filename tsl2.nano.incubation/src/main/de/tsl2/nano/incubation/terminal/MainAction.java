package de.tsl2.nano.incubation.terminal;

import java.util.Properties;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.execution.IRunnable;

public class MainAction<T> extends Action<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 7657014766656806827L;

    transient IRunnable<T, Properties> runner;

    public MainAction() {
    }

    public MainAction(Class<?> mainClass, String... argumentNames) {
        this(mainClass.getSimpleName(), mainClass, argumentNames);
    }
    
    public MainAction(String name, Class<?> mainClass, String... argumentNames) {
        super(mainClass, "main", argumentNames);
        this.name = name;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    T run(Properties context) {
        String[] args = new String[argNames.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = context.getProperty(argNames[i]);
        }
        context.put("arg1", args);
        if (runner == null) {
            try {
                runner = new de.tsl2.nano.incubation.specification.actions.Action(mainClass.getMethod("main",
                    new Class[] { String[].class }));
            } catch (Exception e) {
                ManagedException.forward(e);
            }
        }
        return runner.run(context);
    }
}
