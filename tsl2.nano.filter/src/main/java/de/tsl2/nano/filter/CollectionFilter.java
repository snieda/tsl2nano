package de.tsl2.nano.filter;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * simple object fiter, providing only AND conditions of simple #AttributeQuery.<p/> please extend to fulfill completeness.
 */
public final class CollectionFilter<T> {

    private CollectionFilter() {
    }

    public static <T> Equals<T> eq(String name, T expectedValue) {
        return new Equals<>(new Value<>(name, expectedValue));
    }

    public static <T> In<T> in(String name, T... expectedValues) {
        return new In<>(name, expectedValues);
    }

    public static <T extends Comparable<? super T>> Between<T> between(String name, T min, T max) {
        return new Between<>(name, min, max);
    }

    public static <T> List<T> filter(Collection<T> items, List<ValueMapPredicate> filters) {
        return filter(items, filters.toArray(new ValueMapPredicate[0]));
    }

    public static <T> List<T> filter(Collection<T> items, ValueMapPredicate... filters) {
        if (items == null || items.isEmpty()) {
            return new LinkedList<T>();
        }
        if (filters == null || filters.length == 0) {
            return new ArrayList<>(items);
        }

        return items.stream()
                .map(i -> new GenericObject<>(i))
                .filter(g -> Arrays.stream(filters).allMatch(f -> f.test(g.values)))
                .map(g -> g.instance)
                .collect(Collectors.toList());
    }
}

@SuppressWarnings("rawtypes")
class GenericObject<T> {
    private static Map<Class, PropertyDescriptor[]> beanPropertyCache = new HashMap<>();
    T instance;
    Map<String, Value<?>> values;

    public GenericObject(T instance) {
        this.instance = instance;
        values = createValues(instance);
    }

    Value getNamedValue(String name) {
        return values.get(name);
    }

    private Map<String, Value<?>> createValues(T instance) {
        HashMap<String, Value<?>> map = new HashMap<>();
        PropertyDescriptor[] props = getBeanProperties(instance.getClass());
        Arrays.stream(props).forEach(p -> map.put(p.getName(), new Value<>(instance, p)));
        return map;
    }

    private static <T> PropertyDescriptor[] getBeanProperties(Class<?> type) {
        PropertyDescriptor[] props = beanPropertyCache.get(type);
        if (props == null) {
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(type);
                props = beanInfo.getPropertyDescriptors();

            } catch (IntrospectionException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return props;
    }

    static void clearCache() {
        beanPropertyCache.clear();
    }
}
