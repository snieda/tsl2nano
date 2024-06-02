package de.tsl2.nano.core.util;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class JSon {

	/** this expression is only a basic pattern - if matching, it may be - nevertheless - invalid! */
	private static final String JSON_EXPR = "[\"]?(?:[\\{\\[](?:(?:\\s*+[\"]?\\w++[\"]?\\s*)[:,](?:\\s*+[\"]?[^\"]*+[\"]?\\s*+)[,]?)*+[\\}\\]])++[\"]?";

	private static final  Pattern JSON_PATTERN = Pattern.compile(JSON_EXPR, Pattern.MULTILINE);

	public static boolean isJSon(String txt) {
		return JSON_PATTERN.matcher(txt).find();
    }

    public static String toJSon(Object obj) {
		return toJSon(obj, new StringBuilder(), new TreeInfo()).toString();
    }

	static StringBuilder toJSon(final Object object, StringBuilder json, TreeInfo tree) {
		final Object obj = tree.contains(object) ? tree.getReferenceKey(object) : object;
		if (obj instanceof Class) {
			json.append("{" + ((Class) obj).getName() + "}");
		} else if (obj instanceof Method) {
			json.append("{" + ((Method) obj).toGenericString() + "}");
		} else if (Proxy.isProxyClass(obj.getClass()) && Proxy.getInvocationHandler(obj) instanceof AdapterProxy) {
			tree.addRef(obj, () -> toMapJson(obj, json, tree,
					((AdapterProxy) Proxy.getInvocationHandler(obj)).values()));
    	} else if (ObjectUtil.isSingleValueType(obj.getClass())) {
    		if (ObjectUtil.isSimpleType(obj.getClass()))
				json.append("\"" + FormatUtil.format(obj) + "\"");
			else
				tree.addRef(obj, () -> toMapJson(obj, json, tree, FieldUtil.toSerializingMap(obj)));
		} else if (obj.getClass().isArray() && obj.getClass().getComponentType().isPrimitive()) {
			tree.addRef(obj, () -> json.append(PrimitiveUtil.toArrayString(obj)));
    	} else if (ByteUtil.isByteStream(obj.getClass())) {
			tree.addRef(obj, () -> json.append(ByteUtil.toString(obj)));
    	} else if (!(obj instanceof Map)) {
			tree.addRef(obj, () -> toArrayJson(json, tree,
					obj instanceof Collection ? ((Collection) obj).toArray() : (Object[]) obj));
		} else
			tree.addRef(obj, () -> toMapJson(obj, json, tree, (Map) obj));
		return json;
	}

	private static void toArrayJson(StringBuilder json, TreeInfo tree, Object[] orr) {
		// Object[] arr = new String[orr.length];
		json.append("[");
		for (int i = 0; i < orr.length; i++) {
			if (orr[i] != null) {
				/*arr[i] = */if (Util.isSimpleType(orr[i].getClass()))
					json.append("\"" + FormatUtil.format(orr[i]) + "\"");
				else {
					toJSon(orr[i], json, tree);
				}
				json.append(",");
			}
		}
		json.deleteCharAt(json.length() - 1);
		json.append("]");
		// json.append("[" + StringUtil.concatWrap("\"{0}\"".toCharArray(), arr).replace("\"\"", "\",\"") + "]");
	}

	private static void toMapJson(Object obj, StringBuilder json, TreeInfo tree, Map<String, Object> m) {
		if (tree.hasMaxRecursionReached(obj)) {
			return;
		}
		toJSon(m, json, tree);
	}
    
    public static String toJSon(Map map) {
		return toJSon(map, new StringBuilder(), new TreeInfo()).toString();
    }

	static StringBuilder toJSon(Map map, StringBuilder json, TreeInfo tree) {
        //the quotations enable content with json keys like ',' and ':'
        Set keys = map.keySet();
        json.append("{");
        for (Object k : keys) {
			Object v = map.get(k);
			if (Util.isEmpty(v)) {
                continue;
			} else if (tree.contains(v)) {
				v = tree.getReferenceKey(v);
			}
			json.append("\"" + k + "\": "); // we have to split the appending to have the strings in the right order!
			toJSon(v, json, tree.increaseRecursion());
			tree.decreaseRecursion();
			json.append(",");
        }
        if (json.length() > 1 && json.charAt(json.length() - 1) == ',')
            json.deleteCharAt(json.length()-1);
		json.append("}").toString();
		return json;
    }

    public static Map fromJSon(String json) {
		return (Map) fromJSon_(json, new TreeInfo());
	}

	public static Object fromJSon_(String json, TreeInfo tree) {
    	if (!json.contains("\""))
    		return fromJSonNoQuotations(json);
		if (isJSonList(json))
			return fromJSonList(String.class, json, tree);
        Map map = new LinkedHashMap<>();
		String[] attrs = json.substring(1, json.length() - 1).split("[,]\\s*[\"](?=[^\\{\\[\\]]*[:,])");
		String[] kv = new String[2];
		for (int i = 0; i < attrs.length; i++) {
			// kv = attrs[i].split("(?:^[^:]+)([:])");
			kv[0] = trim(StringUtil.substring(attrs[i], null, ":"));
			kv[1] = trim(StringUtil.substring(attrs[i], ":", null));
			map.put(trim(kv[0]),
					isJSon(kv[1])
							? tree.addRef(fromJSon_(kv[1], tree))
							: tree.getReference(kv[1]));
        }
        return map;
    }

	static <T> List<T> fromJSonList(Class<T> type, String json, TreeInfo tree) {
		String[] attrs = json.substring(1, json.length() - 1).split("[,]\\s*[\"]?(?![^\\{\\[]*[:,])");
		List<T> list = new ArrayList<>(attrs.length);
		for (int i = 0; i < attrs.length; i++) {
			attrs[i] = trim(attrs[i]);
			list.add((T) (isJSon(attrs[i])
					? tree.addRef(fromJSon_(attrs[i], tree))
					: tree.getReference(attrs[i])));
		}
		return list;
	}

	static boolean isJSonList(String json) {
		return json.startsWith("[") && json.endsWith("]");
	}

	private static String trim(String s) {
		return StringUtil.trim(s, " \"");
	}

	@Deprecated // standard fromJSon should do the job now
	private static Map fromJSonNoQuotations(String json) {
        Map map = new LinkedHashMap<>();
        String[] split = json.substring(1, json.length() - 1).split("[,]");
        String[] keyValue;
        for (int i = 0; i < split.length; i++) {
        	keyValue = split[i].split("\\s*:\\s*");
			if (keyValue.length < 2)
				throw new IllegalArgumentException("json parsing error on: " + StringUtil.toFormattedString(keyValue, -1));
            map.put(keyValue[0], keyValue[1]);
        }
        return map;
	}

	public static <T> List<T> toList(Class<T> type, String json) {
		LinkedList<T> list = new LinkedList<>();
		String s;
		while ((s = StringUtil.subEnclosing(json, "{", "}", true)) != null) {
			list.add((T) toObject(type, s));
			json = StringUtil.substring(json, s, null);
		}
		return list;
	}
	public static <T> T toObject(Class<T> type, String json) {
		return (T) BeanClass.getBeanClass(type).fromValueMap(fromJSon(json));
	}
}

@SuppressWarnings({ "rawtypes", "unchecked" })
/**
 * try to reuse references and avoid endless recursive self-referencing loops
 */
class TreeInfo {
	private static final Log LOG = LogFactory.getLog(TreeInfo.class);
	/** current path */
	List path = new LinkedList<>();
	/** stored references */
	List refs = new LinkedList<>();
	int recursion;

	Object get(int index) {
		return refs.get(index);
	}

	boolean contains(Object obj) {
		return refs.contains(obj);
	}

	Object getReference(String value) {
		if (value.matches("@\\d+")) {
			String sindex = StringUtil.extract(value, "\\d+");
			return get(Integer.valueOf(sindex));
		} else {
			return value;
		}
	}

	Object addRef(Object value) {
		return addRef(value, null);
	}

	Object addRef(Object value, SupplierExVoid callback) {
		boolean parentReferenceAdded = avoidEndlessReferenceLoop(value);
		if (callback != null)
			callback.get();
		if (!parentReferenceAdded)
			refs.add(value);
		return value;
	}

	public Object getReferenceKey(Object object) {
		return "@" + refs.indexOf(object);
	}

	private boolean avoidEndlessReferenceLoop(Object value) {
		// in cause of endless self-references we store the object here too early (before creating the json for it)!
		if (path.contains(value)) {
			refs.add(value);
			return true;
		} else {
			path.add(value);
			return false;
		}
	}

	public TreeInfo increaseRecursion() {
		++recursion;
		return this;
	}

	public TreeInfo decreaseRecursion() {
		--recursion;
		return this;
	}

	public boolean hasMaxRecursionReached(Object obj) {
		int maxRecursion = Util.get("tsl2.json.maxRecursion", 20);
		if (recursion > maxRecursion || Util.isJavaInternal(obj.getClass())) {
			LOG.warn("ignoring creation of json on '" + obj.getClass() + "' (recursion: " + recursion + ")");
			return true;
		}
		return false;
	}

}