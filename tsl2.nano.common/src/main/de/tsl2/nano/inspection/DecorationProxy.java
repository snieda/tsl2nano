package de.tsl2.nano.inspection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import de.tsl2.nano.core.ManagedException;

class DecorationProxy<T extends Inspector> implements InvocationHandler {
    
    private List<T> implementations;

    public DecorationProxy(Class<T> interfaze) {
        implementations = Inspectors.getImplementations(interfaze);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object[] result = new Object[] {null}; //workaround on non final result used in inner class
        boolean decoratingChain = args.length == 1 && method.getReturnType().isAssignableFrom(args[0].getClass());
        Inspectors.log("starting " + implementations.size() + " inspections: " + method);
        implementations.stream().filter(h->h.isEnabled()).forEach(h->{
            try {
                result[0] = method.invoke(h, decoratingChain ? result[0] : args);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                ManagedException.forward(e);
            }
        });
        Inspectors.log(" inspections done");
        return result[0];
    }

}