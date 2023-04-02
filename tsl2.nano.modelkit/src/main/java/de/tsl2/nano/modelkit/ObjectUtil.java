package de.tsl2.nano.modelkit;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ObjectUtil {
    private ObjectUtil() {
    }

    public static String toString(List<?> items, String... fieldNames) {
        StringBuilder buf = new StringBuilder();
        items.stream().forEach(i -> buf.append(toString(i, fieldNames) + "\n"));
        return buf.toString();
    }

    public static String toString(Object instance, String[] fieldNames) {
        return toString(instance, -1, fieldNames);
    }
    public static String toString(Object instance, int maxLength, String[] fieldNames) {
        StringBuilder buf = new StringBuilder();
        Arrays.stream(fieldNames).forEach(n -> buf.append(String.valueOf(getValue(instance, n)) + "\t"));
        return toLenString(buf, maxLength);
    }

    public static String toLenString(CharSequence str, int maxLength) {
        if (maxLength == -1) {
            return str.toString();
        }
        int len = Math.min(maxLength, str.length());
        return str.subSequence(0, len) + (len > str.length() ? "..." : "");
    }

    public static List<?> subList(List<?> src, int maxLength) {
        if (maxLength == -1) {
            return src;
        }
        return src.subList(0, Math.min(maxLength, src.size()));
    }
    public static Object getValue(Object instance, String fieldName) {
        return ExceptionHandler.trY(() -> {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        });
    }

    public static void setValue(Object obj, String propertyName, Object value) {
        Objects.requireNonNull(obj, () -> "object must not be null for property: " + propertyName);
        setValue(obj.getClass(), propertyName, obj, value);
    }

    public static void setValue(Class<?> type, String propertyName, Object obj, Object value) {
        ExceptionHandler.trY(() -> new PropertyDescriptor(propertyName, type).getWriteMethod().invoke(obj, value));
    }

    public static <T> void update(T target, T source) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(target.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            Object newValue;
            for (int i = 0; i < propertyDescriptors.length; i++) {
                if (propertyDescriptors[i].getReadMethod() == null || propertyDescriptors[i].getWriteMethod() == null) {
                    continue;
                }
                newValue = propertyDescriptors[i].getReadMethod().invoke(source);
                if (newValue != null) {
                    propertyDescriptors[i].getWriteMethod().invoke(target, newValue);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
