package de.tsl2.nano.incubation.terminal.item;

import java.util.LinkedHashMap;
import java.util.Map;

import de.tsl2.nano.core.util.StringUtil;

/**
 * only for testing purpose
 * @param <T>
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class Echo<T> extends de.tsl2.nano.incubation.specification.actions.Action<T> {

    public Echo() {
        super("echo-action", Echo.class, "nothing", new LinkedHashMap());
    }
    
    @Override
    public T run(Map<String, Object> context, Object... extArgs) {
        System.out.println("action(echo): " + StringUtil.toString(extArgs, 80));
        return super.run(context, extArgs);
    }
}