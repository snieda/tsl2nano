package de.tsl2.nano.core;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import de.tsl2.nano.core.serialize.YamlUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Provides a simple mechanism to use enums as application properties - filled by defaults or from system.properties or from yaml-file.
 * Instead of using Strings as application property names, it provides type safe access through enums.<p/>
 * There is an order of filling the property values:
 * <pre>
 * * If a system property with name <your given prefix or class name + enum-name> was found, it will be used as value
 * * If a yaml file with the name of your class is found, all values found inside will be set
 * * If no system property and no yaml settings were found, the default, given by your enum class will be used.
 * </pre>
 * To use the base interfaces you have to implement your own Enum class.</p>
 * Example:
 * <pre>
	public enum MyAppPreferences implements IPreferences {
		PARALLEL, TIMEOUT(20), FILTER(".*");
	
		static {
			IPreferences.init(MyAppPreferences.class);
		}
		
		Object value;
		MyAppPreferences() {
			set(false);
		}
		MyAppPreferences(Object value) {
			set(value);
		}
		private void set(Object value) {
			this.value = value;
		}
		@Override
		public Object get() {
			return value;
		}
	}
 * </pre> 
 * 
 * To access the properties, call {@link #get(Enum)} or with type {@link #get(Enum, Class)}. To change a property call {@link #set(Enum, Object)}.<p/>
 *  
 * @author ts
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface IPreferences {
	public static final String KEY_PREFIX = "prefix";
	static final String REGEX_UNMATCH = "XXXXXXXX";
	static Map<String, Object> STATS = new LinkedHashMap<>();
	static Map<String, Object> PREFS = new LinkedHashMap<>();

	/** name of enum (is implemented by your enums super class */
	String name();
	/** get the default value of that preference represented by this enum */
	Object get();
	/** optional description of enum */
	default String description() { return "";}
	/** on enums with value of type boolean, returns that boolean value. otherwise false.*/
	default boolean enabled() {
		return get() instanceof Boolean && Boolean.TRUE.equals(get());
	}
	/** returns current system value of given preference */
	static <E extends Enum<E>> Object get(E pref) {
		return get(pref, Object.class);
	}
	/** returns current system typed value of given preference */
	static <T, E extends Enum<E>> T get(E pref, Class<T> type) {
		return (T) get(pref, ((IPreferences)pref).get());
	}
	/** returns current system value of given preference. if not set before, the given default value will be set and returned. */
	static <T, E extends Enum<E>> T get(E pref, T defaultValue) {
		assert pref instanceof IPreferences;
		T v = (T) PREFS.get(sysname(pref));
		if (v == null)
			PREFS.put(sysname(pref), Util.get(STATS.get(KEY_PREFIX) + sysname(pref), defaultValue));		
		return (T) PREFS.get(sysname(pref));
	}
	/** (internally use only) converts the enum name to a system property name */
	static String sysname(Enum e) {
		return e.name().toLowerCase().replace('_' , '.');
	}
	/** sets the current system value for that enum name */
	static Object set(Enum pref, Object value) {
		assert pref instanceof IPreferences && ((IPreferences)pref).get().getClass().isAssignableFrom(value.getClass());
		return PREFS.put(sysname(pref), value);
	}
	/** sets the current system values of type enum for given enum names */
	public static void set(boolean on, Enum...properties) {
		Arrays.stream(properties).forEach( e -> set(e, on));
	}
	/** to be called inside a static block by your preferences/enum implementation */
	static void init(Class<? extends IPreferences> impl) {
		init(impl, impl.getSimpleName());
	}
	/** to be called inside a static block by your preferences/enum implementation */
	static void init(Class<? extends IPreferences> impl, String prefix) {
		PREFS.putAll(load(impl));
		STATS.put(KEY_PREFIX, prefix);
	}
	/** (internally use only) tries to load values for given implementation through yaml file */
	static LinkedHashMap<String, Object> load(Class<? extends IPreferences> impl) {
		String fileName = getFileName(impl);
		return FileUtil.userDirFile(fileName).exists() ? YamlUtil.load(fileName, LinkedHashMap.class) : new LinkedHashMap<>();
	}
	/** dumps the current preferences to file (see #getFileName()) */
	static void save(Class impl) {
		YamlUtil.dump(PREFS, getFileName(impl));
	}
	/** removes all preference values. this is a reset to the default values of your enum implementation */
	public static void reset() {
		PREFS.clear();
	}
	/** filename as simple name of given implementation class with extension '.yml'. */
	static String getFileName(Class<? extends IPreferences> impl) {
		return impl.getSimpleName().toLowerCase() + ".yml";
	}
	/** convenience of call to {@link #printInfo(Class, int)} with tabCount=0 */
	static String printInfo(Class<? extends IPreferences> impl) {
		return printInfo(impl, 0);
	}
	/** evaluates a human readable string with all names and values of given implementation */
	static String printInfo(Class<? extends IPreferences> impl, int tabCount) {
		String a = "\n" + StringUtil.fixString(tabCount, '\t');
		IPreferences[] prefs = impl.getEnumConstants();
		StringBuilder buf = new StringBuilder(a + impl.getSimpleName() + " (PREFIX: '" + STATS.get(KEY_PREFIX) + "') started with:");
		int kwidth = Math.max(StringUtil.maxLength(prefs), StringUtil.maxLength("user.dir", "user.name"));
		buf.append(a + "\t" + StringUtil.fixString("user.dir", kwidth) + ": " + System.getProperty("user.dir"));
		buf.append(a + "\t" + StringUtil.fixString("user.name", kwidth) + ": " + System.getProperty("user.name"));
		for (int i = 0; i < prefs.length; i++) {
			buf.append(a + "\t" + StringUtil.fixString(sysname((Enum)prefs[i]), kwidth) + ": " + get((Enum)prefs[i]) + prefs[i].description());
		}
		buf.append("\n");
		save(impl);
		String p = "\n" + StringUtil.fixString(79, '=') + "\n";
		return p + buf.toString() + p;
	}
}
