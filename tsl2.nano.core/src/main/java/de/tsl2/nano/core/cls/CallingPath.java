package de.tsl2.nano.core.cls;

import java.util.LinkedHashMap;
import java.util.Map;

import de.tsl2.nano.core.util.StringUtil;

/**
 * tries to walk through a method call path. Example: myobject1.evalobject2(mynumber).evalobject3(mydate)
 */
public class CallingPath {
    Object rootObj;
    String path[];
    boolean usePrimitiveArgs = true;
    
    public CallingPath(Object obj, String[] path) {
        this.rootObj = obj;
        this.path = path;
    }
    public static Object eval(Object obj, String expression, Map<String, Object> args) {
        return new CallingPath(obj, expression.split("\\.")).getResult(args);
    }

    public Object getResult(Map<String, Object> args) {
        return getPartialResult(rootObj, 0, args);
    }

    protected Object getPartialResult(Object lastResult, int pathIndex, Map<String, Object> args) {
        Map partialArgs = new LinkedHashMap(args);
        String methodName = evalMethodArgs(path[pathIndex], partialArgs);
        lastResult = new BeanClass(lastResult.getClass()).callMethod(lastResult, methodName, null, usePrimitiveArgs, partialArgs.values().toArray());
        if (pathIndex < path.length - 1)
            lastResult = getPartialResult(lastResult, ++pathIndex, args);
        return lastResult;
    }

    private String evalMethodArgs(String expression, Map allArgs) {
        String methodName = StringUtil.substring(expression, null, "(");
        String argEx = StringUtil.substring(expression, "(", ")");
        String[] strArgs = argEx.split("\\,\\s*");
        Map newArgs = new LinkedHashMap<>();
        for(int i=0; i<strArgs.length; i++) {
            newArgs.put(strArgs[i], allArgs.get(strArgs[i]));
        }
        allArgs.clear();
        allArgs.putAll(newArgs);
        return methodName;
    }

    /**
     * @param usePrimitiveArgs the usePrimitiveArgs to set
     */
    public void setUsePrimitiveArgs(boolean usePrimitiveArgs) {
        this.usePrimitiveArgs = usePrimitiveArgs;
    }
}