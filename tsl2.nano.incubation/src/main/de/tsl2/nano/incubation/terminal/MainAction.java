package de.tsl2.nano.incubation.terminal;

import java.util.ArrayList;
import java.util.List;
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
        List<String> argList = new ArrayList<String>(argNames.length);
        String a;
        for (int i = 0; i < argNames.length; i++) {
            a = context.getProperty(argNames[i]);
            if (a != null)
                argList.add(a);
        }
        context.put("arg1", argList.toArray(new String[0]));
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
    
    @Override
    public String getDescription(boolean full) {
        //print the main help screen
        run(new Properties());
        return super.getDescription(full);
    }
}
