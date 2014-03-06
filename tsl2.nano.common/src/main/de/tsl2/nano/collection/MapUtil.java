/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.collection;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.exception.ManagedException;
import de.tsl2.nano.util.StringUtil;

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

    /**
     * fills a map with all bean-attribute-names and their values
     * 
     * @param o bean
     * @param useClassPrefix if true, the class-name will be used as prefix for the key
     * @param onlySingleValues if true, collections will be ignored
     * @param filterAttributes attributes to be filtered (ignored)
     * @return see {@link Bean#toValueMap()}
     */
    public static Map<String, Object> toValueMap(Object o,
            String keyPrefix,
            boolean onlySingleValues,
            String... filterAttributes) {
        return BeanUtil.toValueMap(o, keyPrefix, onlySingleValues, filterAttributes);
    }

    /**
     * collects the given objects to keys and their values (defined by object order) and puts it into a new map.
     * 
     * @param keysAndValues keys and values to put into the result map
     * @return map containing the given keys and values.
     */
    public static Map asMap(Object... keysAndValues) {
        if (keysAndValues == null || keysAndValues.length % 2 == 1) {
            throw ManagedException.implementationError(
                "the 'keysAndValues' parameters must not be null and must contain pairs of keys and values!",
                keysAndValues);
        }
        final Map<Object, Object> map = new LinkedHashMap<Object, Object>();
        for (int i = 0; i < keysAndValues.length - 1; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
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
     * removeAll
     * 
     * @param src
     * @param toRemove
     * @return src
     */
    public static <K, V> Map<K, V> removeAll(Map<K, V> src, Map<K, V> toRemove) {
        //don't know how it is possible, but without a new set instance, we get a concurrentmodificationexception
        Set<K> keys = new LinkedHashSet<K>(toRemove.keySet());
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

    public static <T> Collection<NamedValue> asNamedCollection(Map<?, T> m) {
        LinkedList<NamedValue> list = new LinkedList<NamedValue>();
        NamedValue.putAll(m, list);
        return list;
    }

    /**
     * see {@link MapEntrySet}.
     * 
     * @param m map to wrap into a proxy to combine the both interfaces {@link List} and {@link Map}.
     * @return proxy implementing both interfaces through delegation.
     */
    public static <K, V> Set<Map.Entry<K, V>> asEntrySetExtender(final Map<K, V> m) {
//        /**
//         * Extender of a maps entry set - used by MapUtil to provide a combination of Collection and Map. workaround in cause of
//         * interface naming clash of {@link Collection} and {@link Map} (method remove(Object) with different return types).
//         * 
//         * @author Tom
//         * @version $Revision$
//         */
//        public interface EntrySetExtender<K, V, E> {
//            /**
//             * @return the map behind the entry set
//             */
//            Map<K, V> map();
//
//            /**
//             * overwrite the entryset add method that will throw an unsupported operation exception.
//             */
//            boolean add(E entry);
//            boolean addAll(Collection<? extends E> c);
//            /**
//             * creates a new entry using the {@link #map()}. the new entry should be returned.
//             */
//            Map.Entry<K, V> addEntry(K key, V value);
//        }
//        EntrySetExtender<K, V, ?> entrySetExtender = new EntrySetExtender<K, V, Object>() {
//            PrivateAccessor<Map<K, V>> mapAccessor = new PrivateAccessor<Map<K, V>>(m);
//
//            @Override
//            public Map<K, V> map() {
//                return m;
//            }
//
//            @Override
//            public boolean add(Object entry) {
//                Map.Entry<K, V> e = (Entry<K, V>) entry;
//                addEntry(e.getKey(), e.getValue());
//                return true;
//            }
//
//            @Override
//            public boolean addAll(Collection<? extends Object> c) {
//                for (Object object : c) {
//                    Map.Entry<K, V> e = (Entry<K, V>) object;
//                    m.put(e.getKey(), e.getValue());
//                }
//                return true;
//            }
//
//            @Override
//            public Entry<K, V> addEntry(K key, V value) {
//                m.put(key, value);
//                return mapAccessor.call("getEntry", Map.Entry.class, new Class[] { Object.class }, key);
//            }
//
//        };
        return new MapEntrySet(m);
//        return (Set<Entry<K, V>>) DelegatorProxy.delegator(new Class[] { EntrySetExtender.class, Set.class },
//            entrySetExtender, m.entrySet());
//        return (Set<Entry<K, V>>) MultipleInheritanceProxy.createMultipleInheritance(new Class[] { EntrySetExtender.class,
//            Set.class }, Arrays.asList(entrySetExtender, m.entrySet()), EntrySetExtender.class.getClassLoader());
    }

}
