package de.tsl2.nano.terminal.item;

import java.util.ArrayList;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.util.Util;

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
    
    public Command() {
    }

    public Command(String name, String command, String... argumentNames) {
        super(name, SystemUtil.class, "executeAndGetOutput", null, argumentNames);
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
    @Persist
    private void initSerialization() {
        if (mainClass==null)
            mainClass = SystemUtil.class;
        if (method==null)
            method="executeAndGetOutput";
    }
}
