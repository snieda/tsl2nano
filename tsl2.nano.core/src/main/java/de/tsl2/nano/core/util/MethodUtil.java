/*
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrivateAccessor;

@SuppressWarnings("rawtypes")
public class MethodUtil extends FieldUtil {

    public static final String REGEX_METHOD_EXPRESSION = "(?:\\w+\\.)*\\w+(?:\\[\\])?\\(.*\\)";
    public static final String REGEX_FULL_METHOD_EXPRESSION = ".*" + REGEX_METHOD_EXPRESSION + "(\\s+throws.+)?";

    public static Method fromGenericString(String genericMethodString) {
        String extracted = StringUtil.extract(genericMethodString.trim(), REGEX_METHOD_EXPRESSION);
        if (Util.isEmpty(extracted))
            throw new IllegalArgumentException("the expression \"" + genericMethodString
                    + "\" is not an exptected generic method string (regex:" + REGEX_METHOD_EXPRESSION + ")");
        String[] splitted = extracted.split("[(,)]");
        Class cls = BeanClass.load(StringUtil.substring(splitted[0], null, ".", true));
        String methodName = StringUtil.substring(splitted[0], ".", null, true);
        Class[] pars = new Class[splitted.length - 1];
        for (int i = 1; i < splitted.length; i++) {
            pars[i - 1] = splitted[i].length() < 3 ? Object.class : BeanClass.load(splitted[i]);
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

    public static Class<?> getReturnTypeRespectingGeneric(Method m) {
        Class<?> gType = ObjectUtil.getGenericType(m.getReturnType());
        return gType != null && !gType.equals(Object.class) ? gType : m.getReturnType();
    }

    public static Class<?> getExplicitType(Method m) {
        Class<?> type = m.getReturnType();
        if (type.equals(Class.class)) {
            Class<?> gType = getGenericType(m, 0);
            return gType.equals(Object.class) ? type : gType;
        } else {
            return type;
        }
    }

    /**
     * tries to get the generic type. if not defined, Object.class will be returned
     * 
     * @return generic type of attribute, or Object.class
     */
    public static Class<?> getGenericType(Method method, int typePos) {
        Object genType = method.getGenericReturnType();
        if (genType instanceof ParameterizedType) {
            genType = ((ParameterizedType) genType).getActualTypeArguments()[typePos];
        }
        return genType instanceof Class ? (Class<?>) genType : Object.class;
    }

}
