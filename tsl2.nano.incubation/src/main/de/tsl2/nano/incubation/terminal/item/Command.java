package de.tsl2.nano.incubation.terminal.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.simpleframework.xml.Element;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.SystemUtil;

/**
 * OS Shell Command
 * 
 * @param <T>
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Command<T> extends MainAction<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 7657014766656806827L;

    @Element
    String cmd;
    
    transient IRunnable<T, Properties> runner;

    public Command() {
    }

    public Command(String name, String command, String... argumentNames) {
        super(name, SystemUtil.class, "execute", null, argumentNames);
        this.cmd = command;
    }

    @Override
    protected ArrayList<String> createArguments() {
        ArrayList<String> args = super.createArguments();
        if (!Util.isEmpty(cmd)) {
            String cmdargs[] = cmd.split("\\s");
            for (int i = 0; i < cmdargs.length; i++) {
                args.add(cmdargs[i]);
            }
        }
        return args;
    }
}
