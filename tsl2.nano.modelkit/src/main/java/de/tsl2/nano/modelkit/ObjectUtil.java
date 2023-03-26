package de.tsl2.nano.modelkit;

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
        StringBuilder buf = new StringBuilder();
        Arrays.stream(fieldNames).forEach(n -> buf.append(String.valueOf(getValue(instance, n)) + "\t"));
        return buf.toString();
    }

    public static Object getValue(Object instance, String fieldName) {
        return ExceptionHandler.trY(() -> {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        });
    }

    public static void setValue(Object obj, String propertyName, Object value) {
        Objects.requireNonNull(obj, "instance must not be null to call setter for '" + propertyName + "'");
        setValue(obj.getClass(), propertyName, obj, value);
    }

    public static void setValue(Class<?> type, String propertyName, Object obj, Object value) {
        ExceptionHandler.trY(() -> new PropertyDescriptor(propertyName, type).getWriteMethod().invoke(obj, value));
    }
}
