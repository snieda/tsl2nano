/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * some utility methods for collections
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class CollectionUtil extends de.tsl2.nano.core.util.CollectionUtil {
    private static final Log LOG = LogFactory.getLog(CollectionUtil.class);

    /**
     * if both interfaces ({@link List} and {@link Set}) are needed for one instance, the given collection will be
     * wrapped into a {@link ListSet}.
     * 
     * @param <T> item type
     * @param listOrSet implementation of {@link List} or {@link Set} to be combined in a new instance of
     *            {@link ListSet}.
     * @return new instanceof {@link ListSet}.
     *         <p/>
     *         TODO: how to create generics expression to define Set or List like: <S extends Set<T>, L extends List<T>>
     *         with <S | L> listOrSet ?
     */
    public static final <T> ListSet<T> asListSet(Collection<T> listOrSet) {
        if (listOrSet instanceof ListSet) {
            return (ListSet<T>) listOrSet;
        } else {
            return new ListSet(listOrSet);
        }
    }

    /**
     * delegates to {@link #getTransformedCollection(Collection, String, Class)}.
     */
    public static <S, T> Collection<T> getTransformedCollection(Collection<S> toTransform, final String attributeName) {
        return (Collection<T>) getTransformedCollection(toTransform, attributeName, Object.class);
    }

    /**
     * transform a collection.
     * 
     * @param <S> real type of collection items
     * @param <T> transformed type of collection items
     * @param toTransform original collection
     * @param attributeName attribute name to use to get the transformed type
     * @return transformed collection
     */
    public static <S, T> Collection<T> getTransformedCollection(Collection<S> toTransform,
            final String attributeName,
            Class<T> transformedType) {
        final ITransformer<S, T> transformer = new ITransformer<S, T>() {
            BeanAttribute attribute = null;

            @Override
            public T transform(S arg0) {
                if (attribute == null) {
                    attribute = BeanAttribute.getBeanAttribute(arg0.getClass(), attributeName);
                }
                return (T) attribute.getValue(arg0);
            }
        };
        return getList(getTransforming(toTransform, transformer).iterator());
    }

    public static <S> Collection<String> toStringTransformed(Collection<S> toTransform) {
        final ITransformer<S, String> transformer = new ITransformer<S, String>() {
            @Override
            public String transform(S arg0) {
                return arg0 != null ? arg0.toString() : "";
            }
        };
        return getList(getTransforming(toTransform, transformer).iterator());
    }
    
    /**
     * @deprecated: use {@link #getFilteringBetween(Iterable, Comparable, Comparable)} or
     *              {@link #getFilteringBetween(Iterable, Object, Object, boolean)} instead. getFilteredCollection
     * 
     * @param src full collection
     * @param predicate filter
     * @return filtering collection
     */
    public static <T> Collection<T> getFilteredCollection(Iterable<T> src, IPredicate<T> predicate) {
        return getList(getFiltering(src, predicate).iterator());
    }

    /**
     * remove
     * 
     * @param <T> collection type
     * @param source source collection
     * @param attributeName attribute name of type
     * @param value value to search to be removed
     * @return removed item or null, if remove failed
     */
    public static final <T> T remove(Collection<T> source, Class<T> type, String attributeName, Object value) {
        final BeanAttribute attribute = BeanAttribute.getBeanAttribute(type, attributeName);
        for (final T t : source) {
            if (value.equals(attribute.getValue(t))) {
                if (source.remove(t)) {
                    return t;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * finds an entry through bean-reflection
     * 
     * @param <T> collection type
     * @param source source collection
     * @param attributeName attribute name of type
     * @param value value to search to be found
     * @return found item or null
     */
    public static final <T> T find(Collection<T> source, Class<T> type, String attributeName, Object value) {
        final BeanAttribute attribute = BeanAttribute.getBeanAttribute(type, attributeName);
        for (final T t : source) {
            if (value.equals(attribute.getValue(t))) {
                return t;
            }
        }
        return null;
    }

    /**
     * wrap source collection holding instances of type 'type' into a new collection - holding values given by
     * attributeName.
     * 
     * @param <T> source collection item type
     * @param source source collection
     * @param type source collection type
     * @param attributeName attribute of source collection item to get the value as wrapper stored in the new collection
     * @return new collection holding wrapped item instances
     */
    public static final <T> Collection<?> wrap(Collection<T> source, Class<T> type, String attributeName) {
        final Collection wrapCollection = new ArrayList(source.size());
        final BeanAttribute attribute = BeanAttribute.getBeanAttribute(type, attributeName);
        for (final T t : source) {
            wrapCollection.add(attribute.getValue(t));
        }
        return wrapCollection;
    }

    /**
     * iterates through next count elements and returns a new sublist containing them. useful for performance
     * optimizations sending statements to a server. if you don't need a transformer you may use List.sublist(..)
     * instead.
     * <p/>
     * Example:
     * 
     * <pre>
     * Collection<MyEntity> myEntities = null;
     * Iterator&lt;MySearchViewBean&gt; searchIt = akten.iterator();
     * ITransformer&lt;MySearchViewBean, Long&gt; toNumbers = new ITransformer&lt;MySearchViewBean, Long&gt;() {
     *     &#064;Override
     *     public Long transform(MySearchViewBean arg0) {
     *         return arg0.getSpecificNumber();
     *     }
     * };
     * for (Collection&lt;Long&gt; specNumbers = CollectionUtil.next(searchIt, blockSize, toNumbers); searchIt.hasNext(); specNumbers = CollectionUtil.next(searchIt,
     *     blockSize,
     *     toNumbers)) {
     *     myEntities = myService.getMyEntityBeans(specNumbers);
     *     ...
     * }
     * 
     * </pre>
     * 
     * @param <T> element type
     * @param elements iterator on each next call this iterator should be the identical instance!
     * @param count block size for. will be the size of the returned sub list
     * @param transformer (optional) to transform the items of elements to the type T to be filled into the new list.
     * @return sublist
     */
    public static final <S, T> Collection<T> next(Iterator<S> elements, int count, ITransformer<S, T> transformer) {
        ArrayList<T> sublist = new ArrayList<T>(count);
        for (int i = 0; i < count && elements.hasNext(); i++) {
            sublist.add(transformer != null ? transformer.transform(elements.next()) : (T) elements.next());
        }
        return sublist;
    }

    /**
     * delegates to {@link #getFilteringBetween(Iterable, Comparable, Comparable)}, copying the filtered list.
     */
    public static final <T extends Comparable<T>> Collection<T> getFilteredBetween(Collection<T> src,
            final T from,
            final T to) {
        return getList(getFilteringBetween(src, from, to).iterator());
    }

    /**
     * delegates to {@link #getFilteringBetween(Iterable, Object, Object, boolean)}, copying the filtered list.
     */
    public static final <T> Collection<T> getFilteredBetween(Collection<T> src,
            final T from,
            final T to,
            final boolean ignoreCase) {
        return getList(getFilteringBetween(src, from, to, ignoreCase).iterator());
    }

    /**
     * getFiltering
     * 
     * @param src collection to filter
     * @param filter filter
     * @return filtering collection
     */
    public static final <I extends Iterable<T>, T> I getFiltering(I src, IPredicate<T> filter) {
        return FilteringIterator.getFilteringIterable(src, filter);
    }

    /**
     * getFiltering
     * 
     * @param src map to filter
     * @param filter key filter
     * @return filtering map
     */
    public static final <I extends Map<S, T>, S, T extends Comparable<T>> I getFilteringMapKey(I src,
            IPredicate<T> filter) {
        return FilteringIterator.getFilteringMap(src, filter);
    }

    /**
     * filters the given collection.
     * <p/>
     * Attention: if 'expression' changes afterwards, the collection iterator may change, too! If you do not want that,
     * use {@link #getList(Iterator) to create a copy.
     * 
     * @param <T> collection item type
     * @param src collection to filter
     * @param expression regular expression to be used as filter. the toString() methods of objects will be used to
     *            match against.
     * @return filtered collection
     */
    public static final <I extends Iterable<T>, T> I getFiltering(I src,
            final StringBuilder expression) {
        return FilteringIterator.getFilteringIterable(src, new IPredicate<T>() {
            @Override
            public boolean eval(T arg0) {
                String arg = arg0 != null ? arg0.toString() : "";
                return arg.matches(expression.toString());
            }
        });
    }

    /**
     * filters the given collection.
     * <p/>
     * Attention: if 'from' or 'to' are mutables and change afterwards, the collection iterator may change, too! If you
     * do not want that, use {@link #getList(Iterator)} to create a copy.
     * 
     * @param <T> collection item type
     * @param src collection to filter
     * @param from minimum object
     * @param to maximum object
     * @return filtered collection
     */
    public static final <I extends Iterable<T>, T extends Comparable<T>> I getFilteringBetween(I src,
            final T from,
            final T to) {
        if (from == null && to == null) {
            return src;
        }
        final boolean useNull = from == null || to == null;
        return FilteringIterator.getFilteringIterable(src, new IPredicate<T>() {
            @Override
            public boolean eval(T arg) {
                return (arg == null && useNull)
                    || ((from == null || arg.compareTo(from) >= 0) && (to == null || arg.compareTo(to) <= 0));
            }
        });
    }

    /**
     * filters the given collection through its string representations.
     * <p/>
     * Attention: if 'from' or 'to' are mutables and change afterwards, the collection iterator may change, too! If you
     * do not want that, use {@link #getList(Iterator)} to create a copy.
     * 
     * @param <T> collection item type
     * @param src collection to filter
     * @param from minimum object
     * @param to maximum object
     * @return filtered collection
     */
    public static final <I extends Iterable<T>, T> I getFilteringBetween(I src,
            final T from,
            final T to,
            final boolean ignoreCase) {
        if (from == null && to == null) {
            return src;
        }
        return FilteringIterator.getFilteringIterable(src, new IPredicate<T>() {
            @Override
            public boolean eval(T arg0) {
                // to be able to reuse the predicate, we can't do the calculations outside (which would be better for the  performance)
                String sfrom =
                    from != null && !ObjectUtil.isEmpty(from) ? ignoreCase && from.toString() != null ? from.toString()
                        .toUpperCase()
                        : from.toString() : null;
                if (StringUtil.STR_ANY.equals(sfrom)) {
                    sfrom = null;
                }
                String sto =
                    to != null && !ObjectUtil.isEmpty(to) ? ignoreCase && to.toString() != null ? to.toString()
                        .toUpperCase()
                        : to.toString() : null;
                if (StringUtil.STR_ANY.equals(sto)) {
                    sto = null;
                }
                boolean useNull = from == null || to == null;

                String sarg = arg0 != null ? ignoreCase && arg0.toString() != null ? arg0.toString().toUpperCase()
                    : arg0.toString() : null;
                return (sarg == null && useNull)
                    || ((sfrom == null || sarg.compareTo(sfrom) >= 0) && (sto == null || sarg.compareTo(sto) <= 0));
            }
        });
    }

    /**
     * getTransforming
     * 
     * @param <I> iterable type
     * @param <T> iterable content type
     * @param src mostly a collection
     * @param transformer transformer
     * @return proxied iterable giving {@link TransformingIterator} as iterator.
     */
    public static final <I extends Iterable<T>, S, T> I getTransforming(Iterable<S> src,
            ITransformer<S, T> transformer) {
        return TransformingIterator.getTransformingIterable(src, transformer);
    }

    /**
     * combines transforming and filtering a collection
     * 
     * @param <I> iterable type
     * @param <T> iterable content type
     * @param src mostly a collection
     * @param transformer transformer
     * @param filter filter
     * @return filtered and transformed iterable
     */
    public static final <I extends Iterable<T>, S, T> I getTransforming(Iterable<S> src,
            ITransformer<S, T> transformer,
            IPredicate<T> filter) {
        return TransformingIterator.getTransformingIterable((Iterable<S>) getFiltering((Iterable<T>) src, filter),
            transformer);
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

    /**
     * if {@link #isContainer(Object)} returns true, obj is an array, collection or map. this method returns a
     * collection where obj is wrapped into.
     * 
     * @param obj
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Collection<?> getContainer(Object obj) {
        if (obj == null) {
            return null;
        }
        Class<?> cls = obj.getClass();
        if (cls.isArray()) {
            return asList(obj);
        } else if (Collection.class.isAssignableFrom(cls)) {
            return (Collection<?>) obj;
        } else if (Map.class.isAssignableFrom(cls)) {
            return CollectionUtil.asEntrySetExtender((Map) obj);
        } else {
            throw new ManagedException(obj + " is not a container!");
        }
    }

}
