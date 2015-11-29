/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.core.util;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

//import de.tsl2.nano.bean.BeanUtil;
//import de.tsl2.nano.bean.def.Bean;
//import de.tsl2.nano.collection.MapEntrySet;
//import de.tsl2.nano.collection.NamedValue;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;

/**
 * utilities for maps.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class MapUtil {
    /**
     * Creates a map combining the header with its data informations. IMPORTANT: The column array and the attributeNames
     * array must have the same size and right order!<br>
     * The attributeNames array may contain <code>null</code> entries. In this case the bean element itself is returned
     * as "attributeValue".
     * 
     * @param columns table column headers
     * @param attributeNames bean attribute names
     * @return map combining the columnnames with bean attribute names.
     */
    public static final Map<String, String> wrapColumnAttributes(String[] columns, String[] attributeNames) {
        final Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < attributeNames.length; i++) {
            map.put(columns[i], attributeNames[i]);
        }
        return map;
    }

//    /**
//     * fills a map with all bean-attribute-names and their values
//     * 
//     * @param o bean
//     * @param useClassPrefix if true, the class-name will be used as prefix for the key
//     * @param onlySingleValues if true, collections will be ignored
//     * @param filterAttributes attributes to be filtered (ignored)
//     * @return see {@link Bean#toValueMap()}
//     */
//    public static Map<String, Object> toValueMap(Object o,
//            String keyPrefix,
//            boolean onlySingleValues,
//            String... filterAttributes) {
//        return BeanUtil.toValueMap(o, keyPrefix, onlySingleValues, filterAttributes);
//    }
//
    public static Map asProperties(Object... keysAndValues) {
        return asMap(new Properties(), keysAndValues);
    }

    /**
     * collects the given objects to keys and their values (defined by object order) and puts it into a new map.
     * 
     * @param keysAndValues keys and values to put into the result map
     * @return map containing the given keys and values.
     */
    public static Map asMap(Object... keysAndValues) {
        return asMap(new LinkedHashMap<Object, Object>(), keysAndValues);
    }

    public static <M extends Map> M asMap(M instance, Object... keysAndValues) {
        if (keysAndValues == null || keysAndValues.length % 2 == 1) {
            throw ManagedException.implementationError(
                "the 'keysAndValues' parameters must not be null and must contain pairs of keys and values!",
                keysAndValues);
        }
        for (int i = 0; i < keysAndValues.length - 1; i += 2) {
            instance.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return instance;
    }

    /**
     * delegates to {@link #asArray(Map, Class)} evaluating the generic type of the given map
     */
    public static <K, V> V[] asArray(Map<K, V> map) {
        Class<V> type;
        try {
            Method m = Map.class.getMethod("values", new Class[0]);
            type = (Class<V>) BeanAttribute.getGenericType(m, 0);
        } catch (Exception e) {
            type = (Class<V>) Object.class;
        }
        return asArray(map, type);
    }

    /**
     * the contrary to {@link #asMap(Object...)}.
     * 
     * @param map map to be flatten into an object array
     * @return array holding key-values
     */
    public static <K, V, V1 extends V> V1[] asArray(Map<K, V> map, Class<V1> type) {
        V1[] result = (V1[]) Array.newInstance(type, map.size() * 2);
        Set<K> keySet = map.keySet();
        int i = 0;
        for (K key : keySet) {
            result[i++] = (V1) key;
            result[i++] = (V1) map.get(key);
        }
        return result;
    }

    /**
     * the contrary to {@link #asMap(Object...)}. keys should be strings, values will be converted (through
     * {@link #toString()} to strings.
     * 
     * @param map map to be flatten into an object array
     * @return array holding key-values
     */
    public static <K, V> String[] asStringArray(Map<K, V> map) {
        String[] result = (String[]) Array.newInstance(String.class, map.size() * 2);
        Set<K> keySet = map.keySet();
        int i = 0;
        Object v;
        for (K key : keySet) {
            result[i++] = (String) key;
            v = map.get(key);
            result[i++] = v != null ? v.toString() : null;
        }
        return result;
    }

    /**
     * remove all elements from src that are contained in toRemove
     * 
     * @param src to change
     * @param toRemove to be evaluated
     * @return changed src map
     */
    public static <K, V> Map<K, V> removeAll(Map<K, V> src, Collection<K> toRemove) {
        //don't know how it is possible, but without a new set instance, we get a concurrentmodificationexception
        Set<K> keys = new LinkedHashSet<K>(toRemove);
        for (K k : keys) {
            src.remove(k);
        }
        return src;
    }

    /**
     * retain all elements in src that are contained in toRetain. all other will be removed.
     * 
     * @param src to change
     * @param toRetain to be evaluated
     * @return changed src map
     */
    public static <K, V> Map<K, V> retainAll(Map<K, V> src, Collection<K> toRetain) {
        //don't know how it is possible, but without a new set instance, we get a concurrentmodificationexception
        Set<K> keys = new LinkedHashSet<K>(src.keySet());
        keys.removeAll(toRetain);
        for (K k : keys) {
            src.remove(k);
        }
        return src;
    }

    /**
     * toString represention of a map
     * 
     * @param map map
     * @return map as string
     */
    public static String toString(Map<?, ?> map) {
        return StringUtil.toFormattedString(map, Integer.MAX_VALUE);
    }

    /**
     * finds all values for a given key set
     * 
     * @param map source map
     * @param keyExpression regular expression to match a key
     * @return all values of keys that match the given key expression
     */
    public static <V> List<V> getValues(Map<?, V> map, String keyExpression) {
        Set<?> keySet = map.keySet();
        List<V> result = new LinkedList<V>();
        for (Object k : keySet) {
            if (k.toString().matches(keyExpression))
                result.add(map.get(k));
        }
        return result;
    }

    /**
     * delegates to {@link #copy(Map, int, int, Map)} creating a new destination map
     */
    public static <K, V> Map<K, V> copy(Map<K, V> src, int start, int end) {
        return copy(src, start, end, new HashMap<K, V>());
    }
    /**
     * copies the given block of elements from src to dest. be careful: the order of your src map may not be defined!
     * 
     * @param src source map
     * @param start first item to be copied
     * @param end last item to be copied
     * @param dest destination map
     * @return destination map
     */
    public static <K, V> Map<K, V> copy(Map<K, V> src, int start, int end, Map<K, V> dest) {
        int i = 0;
        for (Object k : src.keySet()) {
            if (i >= start && i < end)
                dest.put((K) k, src.get(k));
        }
        return dest;
    }
}
