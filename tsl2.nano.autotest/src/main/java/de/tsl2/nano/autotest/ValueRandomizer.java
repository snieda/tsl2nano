package de.tsl2.nano.autotest;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import de.tsl2.nano.autotest.creator.AFunctionCaller;
import de.tsl2.nano.autotest.creator.AutoTest;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.util.AdapterProxy;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.DefaultFormat;
import de.tsl2.nano.core.util.DependencyInjector;
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
	private static DependencyInjector di = new DependencyInjector();

	private ValueRandomizer() {
	}

	public static Object fillRandom(Object obj) {
		return fillRandom(obj, false, 0);
	}
	
	public static <T> T fillRandom(T obj, boolean zeroNumber, final int depth) {
		if (!checkMaxDepth(depth + 1)) {
			return obj;
		}
		PrivateAccessor<?> acc = new PrivateAccessor<>(obj);
		Field[] fields = acc.findMembers(PrivateAccessor::notStaticAndNotFinal);
		Arrays.stream(fields)
				.filter(f -> acc.member(f.getName()) == null)
				.forEach(f -> Util.trY(() -> acc.set(f.getName(), createRandomValue(f.getType(), zeroNumber, depth)),
						false));
		return obj;
	}

	public static Object createRandomProxy(Class<?> interfaze, boolean zeroNumber) {
		return createRandomProxy(interfaze, zeroNumber, 0);
	}

	public static <V> V createRandomProxy(Class<V> interfaze, boolean zeroNumber, final int depth) {
		if (!checkMaxDepth(depth + 1)) {
			return null;
		}
		Map<String, Class> types = AdapterProxy.getValueTypes(interfaze);
		Map<String, Object> values = new HashMap<>();
		types.forEach(
				(n, t) -> values.put(n, t.isAssignableFrom(interfaze) || interfaze.isAssignableFrom(t) ? null
						: createRandomValue(t, zeroNumber, depth)));
		return AdapterProxy.create(interfaze, values);
	}
	
	protected static <V> V createRandomValue(Class<V> typeOf) {
		return createRandomValue(typeOf, false);
	}
	
	protected static <V> V createRandomValue(Class<V> typeOf, boolean zeroNumber) {
		return createRandomValue(typeOf, zeroNumber, 0);
	}
	protected static <V> V createRandomValue(Class<V> typeOf, boolean zeroNumber, int depth) {
		if (!checkMaxDepth(++depth)) {
			System.out.print(">");
			return null;
		}
		Object n;
		V value = null;
		try {
			if (!Util.isEmpty(AFunctionCaller.def(AutoTest.VALUESET_GROUP, ValueSets.DEFAULT))
					&& valueSets.hasValueSet(typeOf)) {
				n = valueSets.fromValueSet(typeOf);
			} else {
				n = zeroNumber && (typeOf.isPrimitive() || NumberUtil.isNumber(typeOf))
						&& (!PrimitiveUtil.isAssignableFrom(char.class, typeOf)
								|| AFunctionCaller.def(AutoTest.ALLOW_SINGLE_CHAR_ZERO, false))
						&& (!PrimitiveUtil.isAssignableFrom(byte.class, typeOf)
								|| AFunctionCaller.def(AutoTest.ALLOW_SINGLE_BYTE_ZERO, false))
										? 0d
										: createRandomNumber(typeOf);
			}
			// avoid writing files into the project folder (and not into target)
			if (File.class.isAssignableFrom(typeOf)
					|| Path.class.isAssignableFrom(typeOf)
					|| String.class.isAssignableFrom(typeOf)
					|| Closeable.class.isAssignableFrom(typeOf)) {
				n = FileUtil.userDirFile(StringUtil.toBase64(n)).getAbsolutePath();
			} else if (NumberUtil.isNumber(n)) {
				n = convert(n, typeOf, zeroNumber, depth);
			}
			value = ObjectUtil.wrap(n, typeOf);
		} catch (Exception e) {
			// here we try it without randomized values but directly creating a default instance
			if (ObjectUtil.isInstanceable(typeOf)) {
				try {
					value = constructWithRandomParameters(typeOf, zeroNumber, depth).instance;
				} catch (Exception ex) {
					return null;
				}
			} else if (typeOf.isInterface()) {
				value = createRandomProxy(typeOf, zeroNumber, depth);
			} else {
				// ManagedException.forward(e);
				// Ok, we can't create an object, so we test with null value
				return null;
			}
		}
		if (Boolean.getBoolean("tsl2nano.autotest.inject.beanattributes")) {
			try {
				di.inject(value);
				injectTestBeanAttributes(value, zeroNumber, depth);
			} catch (Exception e) {
				// Ok, we cannot inject - but the value is already created, so its not a problem....
			}
		}
		return value;
	}

	private static <V> void injectTestBeanAttributes(V value, boolean zeroNumber, int depth) {
		if (!checkMaxDepth(depth + 7) || !Boolean.getBoolean("tsl2nano.autotest.inject.beanattributes")) {
			return;
		}
		List<IAttribute> attrs = BeanClass.getBeanClass(value.getClass()).getAttributes(true);
		attrs.stream().filter(a -> a.getValue(value) == null)
				.forEach(a -> a.setValue(value, createRandomValue(a.getType(), zeroNumber, depth)));
	}

	private static Object convert(Object n, Class typeOf, boolean zeroNumber, int depth) {
		if (typeOf.isAssignableFrom(n.getClass())) {
			return n;
		}
		if (BeanClass.hasConstructor(typeOf, long.class))
			n = ((Number) n).longValue(); // -> Date
		else if (typeOf.equals(Class.class)) {
			if (typeOf.isAnnotation())
				n = ATestAnnotation.class;
			else if (typeOf.isInterface())
				n = ITestInterface.class;
			else
				n = TypeBean.class; // must be equal to the object creating (see below)
		} else if (URI.class.isAssignableFrom(typeOf)) {
			n = URI.create("http://localhost");
		} else if (URL.class.isAssignableFrom(typeOf)) {
			n = Util.trY(() -> URI.create("http://localhost").toURL());
		} else if (InetAddress.class.isAssignableFrom(typeOf)) {
			n = Util.trY(() -> InetAddress.getLocalHost());
		} else if (InetSocketAddress.class.isAssignableFrom(typeOf)) {
			n = Util.trY(() -> InetSocketAddress.createUnresolved("localhost", 0));
		} else if (typeOf.isEnum()) {
			if (n instanceof Number) {
				n = typeOf.getEnumConstants()[((Number) n).intValue()];
			} else {
				throw new IllegalArgumentException(typeOf + " -> " + n);
			}
		} else if (Format.class.isAssignableFrom(typeOf)) {
			n = new DefaultFormat();
		} else if (Method.class.isAssignableFrom(typeOf)) {
				n = Util.trY( () -> TypeBean.class.getMethod("getString"));
		} else if (Field.class.isAssignableFrom(typeOf)) {
				n = Util.trY( () -> TypeBean.class.getField("string"));
		} else if (typeOf.equals(ClassLoader.class))
			n = Thread.currentThread().getContextClassLoader(); // TODO: create randomly
		else if (Collection.class.isAssignableFrom(typeOf))
			n = new ListSet<>(n);
		else if (Properties.class.isAssignableFrom(typeOf))
			n = MapUtil.asProperties(StringUtil.toBase64(n).replace('=', 'X'), n.toString());
		else if (Map.class.isAssignableFrom(typeOf))
			n = MapUtil.asMap(StringUtil.toBase64(n).replace('=', 'X'), n);
		else if (ByteUtil.isByteStream(typeOf))
			n = ByteUtil.toByteStream(new byte[] {((Number) n).byteValue()}, typeOf);
		else if (typeOf.isInterface() && !ObjectUtil.isStandardInterface(typeOf) && checkMaxDepth(depth))
			n = createRandomProxy(typeOf, zeroNumber, ++depth);
		else if (typeOf.isArray()) {
			n = ObjectUtil.wrap(n, typeOf.getComponentType());
			n = typeOf.getComponentType().isPrimitive() ? MapUtil.asArray(typeOf.getComponentType(), n)
					: MapUtil.asArray(MapUtil.asMap(n, n), typeOf.getComponentType());
		} else if (typeOf.equals(Object.class)) {
			n = new TypeBean(n.toString()); //must be equals to the type of Class (see above)
		} else if (!ObjectUtil.isStandardType(typeOf) && !Util.isFrameworkClass(typeOf)) {
			n = typeOf.getSimpleName() + "(" + ByteUtil.hashCode(n) + ")";
		}
		return n;
	}

	static boolean checkMaxDepth(int depth) {
		return depth < AFunctionCaller.def(AutoTest.CREATE_RANDDOM_MAX_DEPTH, 10);
	}

	public static <V> Construction<V> constructWithRandomParameters(Class<V> typeOf)  {
		return constructWithRandomParameters(typeOf, false, 0);
	}
	@SuppressWarnings({ "unchecked" })
	static <V> Construction<V> constructWithRandomParameters(Class<V> typeOf, boolean zeroNumber, int depth) {
		try {
			Constructor<?> constructor;
			Object[] parameters;
			if (typeOf.isArray()) {
				Object array = Array.newInstance(typeOf.getComponentType(), 1);
				Array.set(array, 0, createRandomValue(typeOf.getComponentType(), zeroNumber, depth));
				return new Construction(array);
			} else if (hasFileConstructor(typeOf)) {
				constructor = (Constructor<?>) Util.trY( () -> typeOf.getConstructor(File.class));
				parameters = new Object[] { FileUtil.userDirFile(createRandomValue(String.class, zeroNumber, depth)) };
			} else {
				constructor = getBestConstructor(typeOf);
				if (constructor == null)
					throw new RuntimeException(typeOf + " is not constructable!");
				else if (constructor.getParameterCount() > 0 && !checkMaxDepth(depth))
					throw new IllegalStateException("max depth reached on recursion. there is a cycle in parameter instantiation: " + typeOf);
				parameters = provideRandomizedObjects(depth, 1, constructor.getParameterTypes());
			}
			constructor.setAccessible(true);
			V instance = (V) constructor.newInstance(parameters);
			try {
				if (Boolean.getBoolean("tsl2nano.autotest.inject.beanattributes")) {
					di.inject(instance);
				}
				if (constructor.getParameterCount() == 0
						&& Boolean.getBoolean(AutoTest.PREFIX_FUNCTIONTEST + "fillinstance"))
					instance = fillRandom(instance, zeroNumber, depth);
			} catch (Exception ex) {
				//Ok, we do not inject in cause of problems
			}
			return new Construction(instance, constructor, parameters);
		} catch (Exception e) {
			ManagedException.forward(e);
			return null;
		}
	}

	private static boolean hasFileConstructor(Class<?> typeOf) {
		String fileConstructorNames = System.getProperty(AutoTest.PREFIX_FUNCTIONTEST + "fileconstructor.classes", 
				FileWriter.class.getName() + ", " +
			PrintWriter.class.getName() + ", " + 
			PrintStream.class.getName());
		List<String> fileConstructorClasses = Arrays.asList(fileConstructorNames.split("\\s*[,;]\\s*"));
		return fileConstructorClasses.contains(typeOf.getName());
}

	private static <T> Constructor<T> getBestConstructor(Class<T> typeOf) {
		Constructor<T>[] cs = (Constructor<T>[]) typeOf.getDeclaredConstructors();
		BiFunction<Constructor, Integer, Boolean> lowerParLength = (c, i) -> 0 < c.getParameterTypes().length
				&& c.getParameterTypes().length < i;
		int bestLength = Integer.MAX_VALUE;
		int selection = -1;
		for (int i = 0; i < cs.length; i++) {
			if (lowerParLength.apply(cs[i], bestLength) && hasSimpleTypes(cs[i].getParameterTypes())) {
				selection = i;
				bestLength = cs[i].getParameterTypes().length;
			}
		}
		if (selection == -1) {
			for (int i = 0; i < cs.length; i++) {
				if (lowerParLength.apply(cs[i], bestLength)) {
					selection = i;
					bestLength = cs[i].getParameterTypes().length;
				}
			}
			if (selection == -1) {
				if (BeanClass.hasDefaultConstructor(typeOf)) {
					return Util.trY(() -> typeOf.getDeclaredConstructor(new Class[0]));
				}
			} else {
				selection = 0;
			}
		}
		return cs.length > selection ? cs[selection] : null;
	}

	private static boolean hasSimpleTypes(Class<?>[] parameterTypes) {
		return Arrays.stream(parameterTypes).allMatch(t -> Util.isSimpleType(t));
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
				randomObjects[i+j] = createRandomValue(types[j], zero || respectZero(countPerType, i), depth);
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
		di.reset();
	}

	public static DependencyInjector getDependencyInjector() {
		return di;
	}

	public static void setDependencyInjector(DependencyInjector di_) {
		di = di_;
	}
}
@SuppressWarnings({"rawtypes", "unchecked"})
class ValueSets extends HashMap<Class, List<String>> {
	
	private static final String MINMAX = "<->";
	static final String DEFAULT = "default";
	static AtomicInteger counter = new AtomicInteger(0);

	<V> V fromValueSet(Class<V> typeOf) {
		return (V) fromValueSet(Util.getSingleBaseType(typeOf), 0);
	}
	<V> V fromValueSet(Class<V> typeOf, int depth) {
		if (!containsKey(typeOf) && (FileUtil.userDirFile(valueSetFilename(typeOf)).exists() || FileUtil.hasResource(valueSetFilename(typeOf)))) {
			loadValueSet(typeOf);
		}
		List<String> values = get(typeOf);
		if (isMinMax(values)) {
			return fromValueMinMax(values.get(0), typeOf);
		} else {
			Object result = values.get((int)(Math.random() * values.size()));
			result = avoidCollision(result, typeOf, depth);
			return ObjectUtil.wrap(result, typeOf);
		}
	}
	private boolean isMinMax(List<String> values) {
		return values.size() == 1 && values.get(0).contains(MINMAX);
	}
	private <V> void loadValueSet(Class<V> typeOf) {
		loadValueSet(typeOf, null);
	}
	private <V> void loadValueSet(Class<V> typeOf, String prefix) {
		String file = valueSetFilename(typeOf);
		System.out.print("loading valueset '" + file + "' with prefix '" + prefix + "'...");
		String content = new String(FileUtil.getFileBytes(file, null));
		content = content.replaceFirst("[#].*\n", "");
		String[] names = content.split("[\n]");
		if (!Util.isEmpty(prefix))
			Arrays.stream(names).map(n -> prefix + n);
		put(typeOf, Collections.synchronizedList(new ArrayList(Arrays.asList(names))));
		System.out.print(names.length + " OK!\n");
	}

	private Object avoidCollision(Object result, Class typeOf, int depth) {
		if (!AFunctionCaller.def(AutoTest.VALUESET_AVOID_COLLISION, boolean.class) || PrimitiveUtil.isPrimitiveOrWrapper(typeOf))
			return result;
		// TODO: improve performance by avoiding reload
		List<String> valueSet = get(typeOf);
		valueSet.remove(result);
		if (Util.isEmpty(valueSet)) {
			remove(typeOf);
			loadValueSet(typeOf, "" + counter.addAndGet(1));
		}
		return result;
	}

	<V> V fromValueMinMax(String minmax, Class<V> typeOf) {
		String typ = StringUtil.substring(minmax, null, ":", 0, true);
		String min = StringUtil.substring(minmax, ":", MINMAX);
		String max = StringUtil.substring(minmax, MINMAX, null);
		double d = NumberUtil.random(Double.valueOf(min), Double.valueOf(max));
		return ObjectUtil.wrap(typ != null ? PrimitiveUtil.convert(d, PrimitiveUtil.getPrimitiveClass(typ)): d, typeOf);
	}

	boolean hasValueSet(Class typeOf) {
		Class t = Util.getSingleBaseType(typeOf);
		return containsKey(t) || FileUtil.userDirFile(valueSetFilename(t)).exists() || FileUtil.hasResource(valueSetFilename(t));
	}

	private static String valueSetFilename(Class typeOf) {
		String name = AFunctionCaller.def(AutoTest.VALUESET_GROUP, DEFAULT);
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
