package de.tsl2.nano.autotest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.tsl2.nano.autotest.creator.AFunctionCaller;
import de.tsl2.nano.autotest.creator.AutoTest;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.util.FilePath;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.parser.JSon;
import de.tsl2.nano.core.util.parser.Struct;

/**
 * for each type of values, they can be stored as strings to be converted later - or they are deserialized  as real objects
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ValueSets extends HashMap<Class, List<?>> {
	
	private static final String MINMAX = "<->";
	static final String DEFAULT = "default";
	static AtomicInteger counter = new AtomicInteger(0);

	public static <V> void storeSet(Class<V> type, List<V> set) {
		Struct.serialize(set, FilePath.userDirPath(valueSetFilename(type)), JSon.class);
	}

	<V> V fromValueSet(Class<V> typeOf) {
		return (V) fromValueSet(Util.getSingleBaseType(typeOf), 0);
	}
	<V> V fromValueSet(Class<V> typeOf, int depth) {
		if (!containsKey(typeOf) && (FileUtil.userDirFile(valueSetFilename(typeOf)).exists() || FileUtil.hasResource(valueSetFilename(typeOf)))) {
			loadValueSet(typeOf);
		}
		List<?> values = get(typeOf);
		if (!isSerializedObjectSet(typeOf) && isMinMax((List<String>)values)) {
			return fromValueMinMax(((List<String>)values).get(0), typeOf);
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
		List<?> values;
		if (isSerializedObjectSet(typeOf)) {
			values = readObjectSet(file, typeOf);
		} else {
			values = readSimpleSet(file, typeOf, prefix);
		}

		put(typeOf, Collections.synchronizedList(values));
		System.out.print(values.size() + " OK!\n");
	}
	private <V> boolean isSerializedObjectSet(Class<V> typeOf) {
		return !Util.isSimpleType(typeOf) && !Util.isContainer(typeOf);
	}

	private <V> List<String> readSimpleSet(String file, Class<V> typeOf, String prefix) {
		String content = new String(FileUtil.getFileBytes(file, null));
		content = content.replaceAll("^\\s*[#].*\n", "");
		String[] names = content.split("[\n]");
		if (!Util.isEmpty(prefix))
			names = (String[]) Arrays.stream(names).map(n -> prefix + n).toArray(l -> new String[l]);
		return new ArrayList(Arrays.asList(names));

	}

	private <V> List<V> readObjectSet(String file, Class<V> typeOf) {
		Object objects = Struct.deserialize(FilePath.userDirPath(file), typeOf);
		return List.class.isAssignableFrom(objects.getClass()) ? (List<V>) objects : new ArrayList(Arrays.asList(objects));
	}

	private Object avoidCollision(Object result, Class typeOf, int depth) {
		if (!AFunctionCaller.def(AutoTest.VALUESET_AVOID_COLLISION, boolean.class) || PrimitiveUtil.isPrimitiveOrWrapper(typeOf))
			return result;
		// TODO: improve performance by avoiding reload
		List<?> valueSet = get(typeOf);
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
