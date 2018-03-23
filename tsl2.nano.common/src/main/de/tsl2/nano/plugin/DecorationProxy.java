package de.tsl2.nano.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.StringUtil;

class DecorationProxy<T extends Plugin> implements InvocationHandler {
    
    private List<T> implementations;

    public DecorationProxy(Class<T> interfaze) {
        implementations = Plugins.getImplementations(interfaze);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean decoratingChain = args.length == 1 && args[0] != null && method.getReturnType().isAssignableFrom(args[0].getClass());
        Object[] result = new Object[] {decoratingChain ? args[0] : null}; //workaround on non final result used in inner class
        Plugins.log("starting " + implementations.size() + " inspections: " + method);
        implementations.stream().filter(h->h.isEnabled()).forEach(h->{
            try {
                result[0] = decoratingChain ? method.invoke(h, result[0]) : method.invoke(h, args);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                System.out.println("Error on: " + method + "(" + StringUtil.toString(decoratingChain ? result[0] : args, -1) + ")");
                ManagedException.forward(e);
            }
        });
        Plugins.log(" inspections done");
        return result[0];
    }

}