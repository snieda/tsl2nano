package de.tsl2.nano.core.util;

import java.lang.reflect.Method;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrivateAccessor;

public class MethodUtil extends FieldUtil {

    private static final String REGEX_METHOD_EXPRESSION = "(?:\\w+\\.)*\\w+\\(.*\\)";

    public static Method fromGenericString(String genericMethodString) {
        String extracted = StringUtil.extract(genericMethodString, REGEX_METHOD_EXPRESSION);
        String[] splitted = extracted.split("[(,)]");
        Class cls = BeanClass.load(StringUtil.substring(splitted[0], null, ".", true));
        String methodName = StringUtil.substring(splitted[0], ".", null, true);
        Class[] pars = new Class[splitted.length - 1];
        for (int i = 1; i < splitted.length; i++) {
            pars[i] = splitted[i].length() < 3 ? Object.class : BeanClass.load(splitted[i]);
        }
        return PrivateAccessor.findMethod(cls, methodName, null, pars).iterator().next();
    }
}
