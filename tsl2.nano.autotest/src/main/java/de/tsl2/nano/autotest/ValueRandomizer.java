package de.tsl2.nano.autotest;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.tsl2.nano.autotest.creator.AFunctionCaller;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.util.AdapterProxy;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * For test purposes only!
 * <p/>
 * Simple logic to fill all types of fields of a given object with random
 * values. Together with a Bean class that provides all types of fields and
 * their getters and setters you are able to write generic framework unit tests.
 * <p/>
 * NOTE: Additionally you should test your logic on testsets (e.g. maps of
 * fields with boundary values) describing the boundary conditions. To be more
 * generic, you may use lots of runs on random values instead. This may be done
 * with {@link #provideRandomizedObjects(int, Class...)}
 * <p/>
 * Before creating any random number + value, the ValueRandomizer tries to read a file with
 * name <classname.simplename.lowercase + .set> e.g.: String -> string.set
 * If this file exists a random value of that will be used. The values can be separated by whitespaces or ';'.
 * If this file exists and has only one line with content like 'min<->max' then a minimum and maximum value
 * will be set for that type.
 * 
 * @author Thomas Schneider
 */
public class ValueRandomizer {
	private static final ValueSets valueSets = new ValueSets();

	private ValueRandomizer() {
	}

	public static Object fillRandom(Object obj) {
		return fillRandom(obj, false, 0);
	}
	
	public static Object fillRandom(Object obj, boolean zeroNumber, final int depth) {
		PrivateAccessor<?> acc = new PrivateAccessor<>(obj);
		Field[] fields = acc.findMembers(PrivateAccessor::notStaticAndNotFinal);
		Arrays.stream(fields).forEach(f -> acc.set(f.getName(), createRandomValue(f.getType(), zeroNumber, depth+1)));
		return obj;
	}

	public static Object createRandomProxy(Class<?> interfaze, boolean zeroNumber) {
		return createRandomProxy(interfaze, zeroNumber, 0);
	}
	public static Object createRandomProxy(Class<?> interfaze, boolean zeroNumber, final int depth) {
		Map<String, Class> types = AdapterProxy.getValueTypes(interfaze);
		Map<String, Object> values = new HashMap<>();
		types.forEach( (n, t) -> values.put(n, createRandomValue(t, zeroNumber, depth+1)));
		return AdapterProxy.create(interfaze, values);
	}
	
	protected static <V> V createRandomValue(Class<V> typeOf) {
		return createRandomValue(typeOf, false);
	}
	
	protected static <V> V createRandomValue(Class<V> typeOf, boolean zeroNumber) {
		return createRandomValue(typeOf, zeroNumber, 0);
	}
	@SuppressWarnings({ "unchecked" })
	protected static <V> V createRandomValue(Class<V> typeOf, boolean zeroNumber, int depth) {
		if (!Util.isEmpty(AFunctionCaller.def("use.valueset", ValueSets.DEFAULT)) && valueSets.hasValueSet(typeOf))
			return valueSets.fromValueSet(typeOf);
		Object n = zeroNumber && (typeOf.isPrimitive() || NumberUtil.isNumber(typeOf)) 
				&& (!PrimitiveUtil.isAssignableFrom(char.class, typeOf) || AFunctionCaller.def("allow.single.char.zero", false))
				&& (!PrimitiveUtil.isAssignableFrom(byte.class, typeOf)  || AFunctionCaller.def("allow.single.byte.zero", false))
				? 0d : createRandomNumber(typeOf);
		if (NumberUtil.isNumber(n))
			n = convert(n, typeOf, zeroNumber, depth);
		try {
			return ObjectUtil.wrap(n, typeOf);
		} catch (Exception e) {
			if (checkMaxDepth(depth) && ObjectUtil.isInstanceable(typeOf)) {
				return constructWithRandomParameters(typeOf, ++depth).instance;
			} else {
				ManagedException.forward(e);
				return null;
			}
		}
	}

	private static Object convert(Object n, Class typeOf, boolean zeroNumber, int depth) {
		if (BeanClass.hasConstructor(typeOf, long.class))
			n = ((Number) n).longValue(); // -> Date
		else if (typeOf.equals(Class.class)) {
			if (typeOf.isAnnotation())
				n = ATestAnnotation.class;
			else if (typeOf.isInterface())
				n = ITestInterface.class;
			else
				n = TypeBean.class; // TODO: create randomly
		} else if (typeOf.equals(ClassLoader.class))
			n = Thread.currentThread().getContextClassLoader(); // TODO: create randomly
		else if (Collection.class.isAssignableFrom(typeOf))
			n = new ListSet<>(n);
		else if (Properties.class.isAssignableFrom(typeOf))
			n = MapUtil.asProperties(n.toString(), n.toString());
		else if (Map.class.isAssignableFrom(typeOf))
			n = MapUtil.asMap(n, n);
		else if (ByteUtil.isByteStream(typeOf))
			n = ByteUtil.toByteStream(new byte[] {((Number) n).byteValue()}, typeOf);
		else if (typeOf.isInterface() && !ObjectUtil.isStandardInterface(typeOf) && checkMaxDepth(depth))
			n = createRandomProxy(typeOf, zeroNumber, ++depth);
		else if (typeOf.isArray()) {
			n = ObjectUtil.wrap(n, typeOf.getComponentType());
			n = typeOf.getComponentType().isPrimitive() ? MapUtil.asArray(typeOf.getComponentType(), n)
					: MapUtil.asArray(MapUtil.asMap(n, n), typeOf.getComponentType());
		} else if (typeOf.equals(Object.class) || !ObjectUtil.isStandardType(typeOf) && !Util.isFrameworkClass(typeOf))
			n = typeOf.getSimpleName() + "(" + ByteUtil.hashCode(n) + ")";
		return n;
	}

	protected static boolean checkMaxDepth(int depth) {
		return depth < AFunctionCaller.def("create.randdom.max.depth", 10);
	}

	public static <V> Construction<V> constructWithRandomParameters(Class<V> typeOf)  {
		return constructWithRandomParameters(typeOf, 0);
	}
	@SuppressWarnings({ "unchecked" })
	static <V> Construction<V> constructWithRandomParameters(Class<V> typeOf, int depth)  {
		try {
			Constructor<?> constructor;
			Object[] parameters;
			if (PrintWriter.class.isAssignableFrom(typeOf)) { // poor workaround to avoid PrintWriter writing to project directory (instead of target)
				constructor = (Constructor<?>) Util.trY( () -> PrintWriter.class.getConstructor(File.class));
				parameters = new Object[] {FileUtil.userDirFile(createRandomValue(String.class))};
			} else {
				constructor = getBestConstructor(typeOf);
				if (constructor == null)
					throw new RuntimeException(typeOf + " is not constructable!");
				if (constructor.getParameterCount() > 0 && !checkMaxDepth(depth))
					throw new IllegalStateException("max depth reached on recursion. there is a cycle in parameter instantiation: " + typeOf);
				parameters = provideRandomizedObjects(depth, 1, constructor.getParameterTypes());
			}
			return new Construction(constructor.newInstance(parameters), constructor, parameters);
		} catch (Exception e) {
			ManagedException.forward(e);
			return null;
		}
	}

	private static <T> Constructor<T> getBestConstructor(Class<T> typeOf) {
		if (BeanClass.hasDefaultConstructor(typeOf))
			return Util.trY( () -> typeOf.getConstructor(new Class[0]));
		Constructor<T>[] cs = (Constructor<T>[]) typeOf.getConstructors();
		for (int i = 0; i < cs.length; i++) {
			if (cs[i].getParameterTypes().length == 1)
				return cs[i];
		}
		return cs.length > 0 ? cs[0] : null;
	}

	protected static <V> Object createRandomNumber(Class<V> typeOf) {
		return createRandomNumber(typeOf, intervalOf(typeOf));
	}
	protected static <V> Object createRandomNumber(Class<V> typeOf, long interval) {
		if (NumberUtil.isNumber(typeOf)) // negative only on number types
			interval = interval * (Math.random() < 0.5 ? -1 : 1);
		Object n = Math.random() * interval;
		return n;
	}

	private static <V> long intervalOf(Class<V> typeOf) {
		return NumberUtil.isNumber(typeOf) && !Number.class.equals(typeOf)
				? BigDecimal.class.isAssignableFrom(typeOf) 
					? (long) Double.MAX_VALUE
					: ((Number) BeanClass.getStatic(PrimitiveUtil.getWrapper(typeOf), "MAX_VALUE")).longValue()
				: typeOf.isEnum() 
					? typeOf.getEnumConstants().length 
					: Date.class.isAssignableFrom(typeOf) 
						? DateUtil.MAX_DATE.getTime()
						: Byte.MAX_VALUE;
	}

	public static Object[] provideRandomizedObjects(int countPerType, Class... types) {
		return provideRandomizedObjects(0, countPerType, types);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object[] provideRandomizedObjects(int depth, int countPerType, Class... types) {
		boolean zero = countPerType == 0;
		countPerType = countPerType < 1 ? 1 : countPerType; 
		Object[] randomObjects = new Object[countPerType * types.length];
		for (int i = 0; i < countPerType; i++) {
			for (int j = 0; j < types.length; j++) {
				if (ObjectUtil.isStandardType(types[j]) || types[j].isEnum() || ByteUtil.isByteStream(types[j]) || Serializable.class.isAssignableFrom(types[j]))
					randomObjects[i+j] = createRandomValue(types[j], zero || respectZero(countPerType, i), depth);
				else if (types[j].isInterface())
					randomObjects[i+j] = createRandomProxy(types[j], zero, depth);
				else {
					Class type = types[j].equals(Object.class) ? TypeBean.class : types[j];
					randomObjects[i+j] = fillRandom(constructWithRandomParameters(type, ++depth).instance, zero || respectZero(countPerType, i), depth);
				}
			}
		}
		return randomObjects;
	}
	
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	public static Map<Class, Object[]> provideRandomizedObjectMap(int countPerType, Class... types) {
//		Map<Class, Object[]> all = new LinkedHashMap<>();
//		for (int i = 0; i < types.length; i++) {
//			Object[] randomObjects = new Object[countPerType];
//			for (int j = 0; j < countPerType; j++) {
//				if (ObjectUtil.isStandardType(types[i]) || types[i].isEnum())
//					randomObjects[j] = createRandomValue(types[i], respectZero(countPerType, j));
//				else
//					randomObjects[j] = fillRandom(BeanClass.createInstance(types[i]), respectZero(countPerType, j));
//			}
//			all.put(types[i], randomObjects);
//		}
//		return all;
//	}

	protected static boolean respectZero(int countPerType, int currentIndex) {
		return countPerType > 2 && currentIndex == 0;
	}
	
	public static final void reset() {
		valueSets.clear();
	}
}
class ValueSets extends HashMap<Class, String[]> {
	static final String DEFAULT = "default";

	<V> V fromValueSet(Class<V> typeOf) {
		if (!containsKey(typeOf) && (FileUtil.userDirFile(valueSetFilename(typeOf)).exists() || FileUtil.hasResource(valueSetFilename(typeOf)))) {
			String content = new String(FileUtil.getFileBytes(valueSetFilename(typeOf), null));
			content = content.replaceFirst("[#].*\n", "");
			String[] names = content.split("[\n]");
			put(typeOf, names);
		}
		String[] values = get(typeOf);
		if (values.length == 1) 
			return fromValueMinMax(values[0], typeOf);
		return ObjectUtil.wrap(values[(int)(Math.random() * values.length)], typeOf);
	}

	<V> V fromValueMinMax(String minmax, Class<V> typeOf) {
		String typ = StringUtil.substring(minmax, null, ":", 0, true);
		String min = StringUtil.substring(minmax, ":", "<->");
		String max = StringUtil.substring(minmax, "<->", null);
		double d = NumberUtil.random(Double.valueOf(min), Double.valueOf(max));
		return ObjectUtil.wrap(typ != null ? PrimitiveUtil.convert(d, PrimitiveUtil.getPrimitiveClass(typ)): d, typeOf);
	}

	boolean hasValueSet(Class typeOf) {
		return containsKey(typeOf) || FileUtil.userDirFile(valueSetFilename(typeOf)).exists() || FileUtil.hasResource(valueSetFilename(typeOf));
	}

	private static String valueSetFilename(Class typeOf) {
		String name = AFunctionCaller.def("use.valueset", DEFAULT);
		return (name.equals(DEFAULT) ? "" : name + "-") + typeOf.getSimpleName().toLowerCase() + ".set";
	}
}

@Retention(RUNTIME)
@Target({TYPE, METHOD, PARAMETER})
@interface ATestAnnotation {
	int nix();
}

interface ITestInterface {
	void nix();
}
