package de.tsl2.nano.core.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * simple base class to be used as generic data holder - not using bean
 * properties with explicit fields and their getters and setters but using a map
 * of enums and their values. default values are read through system properties
 * using enum names.
 * 
 * @param <E> enum type to be used as member names
 * @param <V> type of values
 * 
 * @author Thomas Schneider
 */
@SuppressWarnings("unchecked")
public class ValueSet<E extends Enum<E>, V> {
	private Class<E> names;
	private Map<E, V> values;

	public ValueSet(Class<E> enumClass, Map<E, V> values) {
		this.names = enumClass;
		this.values = values;
	}

	public ValueSet(Class<E> enumClass, V... defaultValues) {
		this.names = enumClass;
		assert defaultValues.length == 0 || names().length == defaultValues.length;
		values = new HashMap<>();
		for (int i = 0; i < defaultValues.length; i++) {
			values.put(enumClass.getEnumConstants()[i], defaultValues[i]);
		}
	}

	public E[] names() {
		return names.getEnumConstants();
	}

	public Stream<E> stream() {
		return Arrays.stream(names());
	}

	public V[] valuesOf(E... filter) {
		return (V[]) stream().filter(e -> Util.in(e, filter)).toArray();
	}

	public void on(E name, BiConsumer<E, V> doIt) {
		doIt.accept(name, get(name));
	}

	public void transform(E name, Function<V, V> transformer) {
		set(name, transformer.apply(get(name)));
	}

	public V get(E par) {
		return values.get(par);
	}

	public void set(E name, V value) {
		values.put(name, value);
	}

	public void set(Entry<E, V>... entries) {
		for (int i = 0; i < entries.length; i++) {
			set(entries[i].key, entries[i].value);
		}
	}

	protected static float def(Enum<?> p, float value) {
		return Util.get(p.name(), value);
	}

	public static Entry<Enum<?>, Object> e(Enum<?> e, Object v) {
		return new Entry<Enum<?>, Object>(e, v);
	}
}

class Entry<E, V> {
	E key;
	V value;

	Entry(E e, V v) {
		key = e;
		value = v;
	}
}
