/*
 * Copyright © 2002-2009 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.collection;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanUtil;
import de.tsl2.nano.util.bean.def.Bean;

/**
 * utilities for maps.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
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
            throw FormattedException.implementationError("the 'keysAndValues' parameters must not be null and must contain pairs of keys and values!",
                keysAndValues);
        }
        final Map<Object, Object> map = new LinkedHashMap<Object, Object>();
        for (int i = 0; i < keysAndValues.length - 1; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    /**
     * the contrary to {@link #asMap(Object...)}.
     * @param map map to flatten into an object array
     * @return array holding key-values
     */
    public static Object[] asArray(Map<?, ?> map) {
        Object[] result = new Object[map.size() * 2];
        Set<?> keySet = map.keySet();
        int i=0;
        for (Object key : keySet) {
            result[i++] = key;
            result[i++] = map.get(key);
        }
        return result;
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
}
