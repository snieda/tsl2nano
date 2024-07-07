package de.tsl2.nano.core.util;

import java.lang.reflect.Method;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrivateAccessor;

@SuppressWarnings("rawtypes")
public class MethodUtil extends FieldUtil {

    public static final String REGEX_METHOD_EXPRESSION = "(?:\\w+\\.)*\\w+\\(.*\\)";
    public static final String REGEX_FULL_METHOD_EXPRESSION = ".*" + REGEX_METHOD_EXPRESSION + "(\\s+throws.+)?";

    public static Method fromGenericString(String genericMethodString) {
        String extracted = StringUtil.extract(genericMethodString, REGEX_METHOD_EXPRESSION);
        if (Util.isEmpty(extracted))
            throw new IllegalArgumentException("the expression \"" + genericMethodString
                    + "\" is not an exptected generic method string (regex:" + REGEX_METHOD_EXPRESSION + ")");
        String[] splitted = extracted.split("[(,)]");
        Class cls = BeanClass.load(StringUtil.substring(splitted[0], null, ".", true));
        String methodName = StringUtil.substring(splitted[0], ".", null, true);
        Class[] pars = new Class[splitted.length - 1];
        for (int i = 1; i < splitted.length; i++) {
            pars[i] = splitted[i].length() < 3 ? Object.class : BeanClass.load(splitted[i]);
        }
        return PrivateAccessor.findMethod(cls, methodName, null, pars).iterator().next();
    }

    public static Method getMethod(Class<?> cls, String name, Class[] par) {
        try {
            return cls.getDeclaredMethod(name, par);
        } catch (NoSuchMethodException e) {
            if (cls.getSuperclass() != null) {
                return getMethod(cls.getSuperclass(), name, par);
            }
            ManagedException.forward(e, false);
            return null;
        }
    }

}
