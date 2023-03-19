package de.tsl2.nano.modelkit;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

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
}
