package de.tsl2.nano.util.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.Util;

/**
 * For test purposes only!
 * <p/>
 * Simple logic to fill all types of fields of a given object with random
 * values. Together with a Bean class that provides all types of fields and
 * their getters and setters you are able to write generic framework unit tests.
 * <p/>
 * NOTE: Additionally you should test your logic on testsets (e.g. maps of
 * fields with boudary values) describing the boundary conditions. To be more
 * generic, you may use lots of runs on random values instead. This may be done
 * with {@link #provideRandomizedObjects(int, Class...)}
 * 
 * @author Thomas Schneider
 */
public class ValueRandomizer {
	private ValueRandomizer() {
	}

	public static Object fillRandom(Object obj) {
		PrivateAccessor<?> acc = new PrivateAccessor<>(obj);
		Field[] fields = acc.findMembers(PrivateAccessor::notStaticAndNotFinal);
		Arrays.stream(fields).forEach(f -> acc.set(f.getName(), createRandomValue(f.getType())));
		return obj;
	}

	@SuppressWarnings({ "unchecked" })
	protected static <V> V createRandomValue(Class<V> typeOf) {
		long size = NumberUtil.isNumber(typeOf)
				? BigDecimal.class.isAssignableFrom(typeOf) ? (long) Double.MAX_VALUE
						: ((Number) BeanClass.getStatic(PrimitiveUtil.getWrapper(typeOf), "MAX_VALUE")).longValue()
				: typeOf.isEnum() ? typeOf.getEnumConstants().length : Byte.MAX_VALUE;
		Object n = Math.random() * size;
		if (BeanClass.hasConstructor(typeOf, long.class))
			n = ((Number) n).longValue(); // -> Date
		else if (typeOf.equals(Class.class))
			n = TypeBean.class; // TODO: create randomly
		else if (Map.class.isAssignableFrom(typeOf))
			n = MapUtil.asMap(n, n);
		else if (typeOf.isArray()) {
			n = PrimitiveUtil.convert(n, typeOf.getComponentType());
			n = typeOf.getComponentType().isPrimitive() ? MapUtil.asArray(typeOf.getComponentType(), n)
					: MapUtil.asArray(MapUtil.asMap(n, n), typeOf.getComponentType());
		} else if (typeOf.equals(Object.class) || !ObjectUtil.isStandardType(typeOf) && !Util.isFrameworkClass(typeOf))
			n = typeOf.getSimpleName() + "(" + ByteUtil.hashCode(n) + ")";
		return ObjectUtil.wrap(n, typeOf);
	}

	public static Object[] provideRandomizedObjectArray(int countPerType, Class type) {
		return provideRandomizedObjects(countPerType, type).get(type);
	}
	public static Object[] provideRandomizedObjectArray(Class... type) {
		Map<Class, Object[]> map = provideRandomizedObjects(1, type);
		return CollectionUtil.addAll(new ArrayList<>(), map.values().toArray()).toArray();
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<Class, Object[]> provideRandomizedObjects(int countPerType, Class... types) {
		Map<Class, Object[]> all = new HashMap<>();
		for (int i = 0; i < types.length; i++) {
			Object[] randomObjects = new Object[countPerType];
			for (int j = 0; j < countPerType; j++) {
				if (ObjectUtil.isStandardType(types[i]) || types[i].isEnum())
					randomObjects[j] = createRandomValue(types[i]);
				else
					randomObjects[j] = fillRandom(BeanClass.createInstance(types[i]));
			}
			all.put(types[i], randomObjects);
		}
		return all;
	}
}
