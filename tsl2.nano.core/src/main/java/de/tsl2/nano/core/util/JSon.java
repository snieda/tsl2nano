package de.tsl2.nano.core.util;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.cls.PrivateAccessor;

public class JSon {
	// final String JSON_NOQUOT = "[{\\[]((\\s*\\w++\\s*)[:](\\s*[^,\\s]*?\\s*)[,]?)*\\}";
	private static final String JSON_EXPR = "[\"]?(?:[\\{\\[](?:(?:\\s*+[\"]?\\w++[\"]?\\s*)[:](?:\\s*+[\"]?[^\"]*+[\"]?\\s*+)[,]?)*+[\\}\\]])++[\"]?";

	private static final  Pattern JSON_PATTERN = Pattern.compile(JSON_EXPR, Pattern.MULTILINE);

	public static boolean isJSon(String txt) {
		return JSON_PATTERN.matcher(txt).find();
    	// return !txt.contains("\"")
    	// 		? txt.matches("[{\\[]((\\s*\\w++\\s*)[:](\\s*[^,\\s]*?\\s*)[,]?)*\\}")//txt.matches("[{](.*[:].*[,]?)+[}\\]]")
    	// 		: txt.matches("[\\\"]?([\\{\\[]((\\s*[\\\"]?\\w++[\\\"]?\\s*)[:](\\s*[\\\"]?[^\\\"]*[\\\"]?\\s*)[,]?)*[\\}\\]])+[\\\"]?");
    }

    public static String toJSon(Object obj) {
    	return toJSon(obj, new StringBuilder(), new LinkedList<>());
    }
    static String toJSon(Object obj, StringBuilder json, List tree) {
    	if (tree.contains(obj))
    		obj = "@" + tree.indexOf(obj);
    	if (Class.class.isAssignableFrom(obj.getClass())) {
    		return "{" + obj.getClass().getName() + "}";
    	} else if (Proxy.isProxyClass(obj.getClass()) && Proxy.getInvocationHandler(obj) instanceof AdapterProxy) {
    		return toMapJson(obj, json, tree, ((AdapterProxy)Proxy.getInvocationHandler(obj)).values());
    	} else if (ObjectUtil.isSingleValueType(obj.getClass())) {
    		if (ObjectUtil.isSimpleType(obj.getClass()))
    			return FormatUtil.format(obj);
	    	Map<String, Object> m = new HashMap<>();
	    	new PrivateAccessor<>(obj).setUseDefiningClass(true).forEachMember( (n, v) -> m.put(n, v));
	    	return toMapJson(obj, json, tree, m);
    	} else if (ByteUtil.isByteStream(obj.getClass())) {
    		tree.add(obj);
    		return ByteUtil.toString(obj);
    	} else if (!(obj instanceof Map)) {
    		tree.add(obj);
    		if (obj instanceof Collection)
    			obj = ((Collection)obj).toArray();
    		Object[] orr = ((Object[])obj);
    		Object[] arr = /*orr instanceof Object[] || orr instanceof String[] ? orr : */new String[orr.length];
    		for (int i = 0; i < arr.length; i++) {
				arr[i] = Util.isSimpleType(orr[i].getClass()) ? FormatUtil.format(orr[i]) : toJSon(orr[i], json, tree);
			}
    		return "{" + StringUtil.concatWrap("\"{0}\"".toCharArray(), arr).replace("\"\"", "\",\"") + "}";
    	} else
    		return toMapJson(obj, json, tree, (Map) obj);
    }

	private static String toMapJson(Object obj, StringBuilder json, List tree, Map<String, Object> m) {
		tree.add(obj);
		return toJSon(m, json, tree);
	}
    
    public static String toJSon(Map map) {
    	return toJSon(map, new StringBuilder(), new LinkedList<>());
    }
    static String toJSon(Map map, StringBuilder json, List tree) {
        //the quotations enable content with json keys like ',' and ':'
        Set keys = map.keySet();
        json.append("{");
        for (Object k : keys) {
            Object v = map.get(k);
            if (Util.isEmpty(v))
                continue;
            else if (tree.contains(v))
            	v = "@" + tree.indexOf(v);
            else if (v.getClass().isArray()) {
                tree.add(v);
            	if (v.getClass().getComponentType().isPrimitive())
            		v = PrimitiveUtil.toArrayString(v);
            	else
            		v = Arrays.toString((Object[])v);
            } else if (!Util.isInstanceable(v.getClass()) || json.length() > Runtime.getRuntime().freeMemory() / 16 ) //avoid OutOfMemoryError
        		v = v.toString();
            json.append("\"" + k + "\": \""); // we have to split the appending to have the strings in the right order!
            json.append(ENV.get("tsl2.json.recursive", false) ? toJSon(v, json, tree) : v);
            json.append("\",");
        }
        if (json.length() > 1 && json.charAt(json.length() - 1) == ',')
            json.deleteCharAt(json.length()-1);
        return json.append("}").toString();
    }
    
    // TODO: do recursive calls checking different types, use references with '@' like toJSon() does
    public static Map fromJSon(String json) {
    	if (!json.contains("\""))
    		return fromJSonNoQuotations(json);
        Map map = new LinkedHashMap<>();
        String[] split = json.substring(1, json.length() - 1).split("[\"]");
        for (int i = 0; i < split.length-3; i+=4) {
            map.put(split[i+1], split[i+3]);
        }
        return map;
    }

    private static Map fromJSonNoQuotations(String json) {
        Map map = new LinkedHashMap<>();
        String[] split = json.substring(1, json.length() - 1).split("[,]");
        String[] keyValue;
        for (int i = 0; i < split.length; i++) {
        	keyValue = split[i].split("\\s*:\\s*");
            map.put(keyValue[0], keyValue[1]);
        }
        return map;
	}

	public static <T> List<T> toList(Class<T> type, String json) {
		LinkedList<T> list = new LinkedList<>();
		String s;
		// json = StringUtil.subEnclosing(json, "{", "}", false);
		while ((s = StringUtil.subEnclosing(json, "{", "}", true)) != null) {
			list.add( (T) BeanClass.getBeanClass(type).fromValueMap(fromJSon(s)));
			json = StringUtil.substring(json, s, null);
		}
		return list;
	}
	public static <T> T toObject(Class<T> type, String json) {
		return (T) BeanClass.getBeanClass(type).fromValueMap(fromJSon(json));
	}
}
