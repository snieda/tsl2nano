package de.tsl2.nano.core.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class FieldUtil extends ByteUtil {
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object[] toObjectArray(final Object obj, String... attributes) {
		return foreach((f, values) -> values.add(getValue(obj, f)), new ArrayList(), obj, attributes).toArray();
	}

	public static String toString(final Object obj, String... attributes) {
		return foreach((f, b) -> b.append(f.getName() + "=" + getValue(obj, f) + ","), new StringBuilder(), obj, attributes).toString();
	}

	public static Map<String, Object> toMap(final Object obj, String... attributes) {
		return foreach((f, m) -> m.put(f.getName(), getValue(obj, f)), new LinkedHashMap<String, Object>(), obj, attributes);
	}

	public static Map<String, Object> fromMap(final Object obj, Map<String, Object> map) {
		return foreach((f, m) -> setValue(obj, f, m.get(f.getName())), map, obj);
	}

	public static Object getValue(Object obj, Field f) {
		try {
			return f.get(obj);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void setValue(Object obj, Field f, Object value) {
		try {
			f.set(obj, value);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <Container, Result> Container foreach(BiConsumer<Field, Container> callback, Container container,
			Object obj, String... attributes) {
		Class<? extends Object> cls = obj.getClass();
		if (attributes == null || attributes.length == 0)
			attributes = getFieldNames(cls);
		for (int i = 0; i < attributes.length; i++) {
			try {
				callback.accept(cls.getDeclaredField(attributes[i].toLowerCase()), container);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return container;
	}

	public static String[] getFieldNames(Class<? extends Object> cls) {
		Field[] fields = cls.getDeclaredFields();
		String[] result = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			result[i] = fields[i].getName();
		}
		return result;
	}
}