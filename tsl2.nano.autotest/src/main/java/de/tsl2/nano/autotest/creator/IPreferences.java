package de.tsl2.nano.autotest.creator;

import java.util.LinkedHashMap;
import java.util.Map;

import de.tsl2.nano.core.serialize.YamlUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * on construction. perhaps we should use class Properties. What's about the ValueSet implementation?
 * @author ts
 */
public interface IPreferences {
	static final String REGEX_UNMATCH = "XXXXXXXX";
	static Map<String, Object> STATS = new LinkedHashMap<>();
	static Map<String, Object> PREFS = new LinkedHashMap<>();

	String name();
	Object get();
	default String description() { return "";}
	boolean enabled();
	
	static <E extends Enum<E>> Object get(E pref) {
		return get(pref, Object.class);
	}
	static <T, E extends Enum<E>> T get(E pref, Class<T> type) {
		return (T) get(pref, ((IPreferences)pref).get());
	}
	static <T, E extends Enum<E>> T get(E pref, T defaultValue) {
		assert pref instanceof IPreferences;
		T v = (T) PREFS.get(sysname(pref));
		if (v == null)
			PREFS.put(sysname(pref), Util.get(STATS.get("prefix") + sysname(pref), defaultValue));		
		return (T) PREFS.get(sysname(pref));
	}
	static String sysname(Enum e) {
		return e.name().toLowerCase().replace('_' , '.');
	}
	static Object set(Enum pref, Object value) {
		return PREFS.put(sysname(pref), value);
	}
	static void init(Class<? extends IPreferences> impl, String prefix) {
		PREFS.putAll(load(impl));
		STATS.put("prefix", prefix);
	}
	static LinkedHashMap<String, Object> load(Class<? extends IPreferences> impl) {
		String fileName = getFileName(impl);
		return FileUtil.userDirFile(fileName).exists() ? YamlUtil.load(fileName, LinkedHashMap.class) : new LinkedHashMap<>();
	}
	
	static void save(Class impl) {
		YamlUtil.dump(PREFS, getFileName(impl));
	}
	public static void reset() {
		PREFS.clear();
	}
	static String getFileName(Class<? extends IPreferences> impl) {
		return impl.getSimpleName().toLowerCase() + ".yml";
	}
	static String printInfo(Class<? extends IPreferences> impl, String prefix, int tabCount) {
		String a = "\n" + StringUtil.fixString(tabCount, '\t');
		IPreferences[] prefs = impl.getEnumConstants();
		StringBuilder buf = new StringBuilder(a + impl.getSimpleName() + " (PREFIX: '" + prefix + "') started with:");
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
