/*
 * Copyright © 2002-2009 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.collection;

import java.lang.reflect.Array;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import de.tsl2.nano.log.LogFactory;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.bean.BeanAttribute;

/**
 * some utility methods for collections
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class CollectionUtil {
    private static final Log LOG = LogFactory.getLog(CollectionUtil.class);

    /**
     * wrap an array into a collection. works on Object[] using Arrays.asList() and on primitive arrays with a simple
     * loop.
     * 
     * @param array object that is an array
     * @return filled collection
     */
    public static Collection asList(Object array) {
        assert array.getClass().isArray() : "array parameter must be an array!";
        if (array instanceof Object[]) {
            //the Arrays.asList() returns a fixed size list!
            return Arrays.asList((Object[]) array);
        }

        /*
         * on primitives, do it yourself
         * Arrays.asList() needs a special array cast
         */
        final int length = Array.getLength(array);
        final Collection c = new ArrayList(length);
        for (int i = 0; i < length; i++) {
            c.add(Array.get(array, i));
        }
        return c;
    }

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
        if (listOrSet instanceof ListSet)
            return (ListSet<T>) listOrSet;
        else
            return new ListSet(listOrSet);
    }

    /**
     * if list was yield by {@link Arrays#asList(Object...)}, the list is static and cannot be copied - then it will be
     * wrapped into a new {@link ArrayList}. to avoid casts and checks on a list, the argument and return value are a
     * {@link Collection}.
     * 
     * @param list list to check and perhaps wrap
     * @return same instance or wrapped into {@link ArrayList}
     */
    public static final Collection<?> asStandardArrayList(Collection<?> list) {
        //perhaps the selection is a static arraylist, yield from Arrays.asList(..)
        if (list.getClass().getName().equals("java.util.Arrays$ArrayList")) {
            list = new ArrayList(list);
        }
        return list;
    }

    /**
     * delegates to {@link #getTransformedCollection(Collection, String, Class)}.
     */
    public static <S, T> Collection<T> getTransformedCollection(Collection<S> toTransform, final String attributeName) {
        return (Collection<T>) getTransformedCollection(toTransform, attributeName, Object.class);
    }

    /**
     * Uses apache collection utils to transform a collection. see {@link CollectionUtils}.
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
     * find
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
     * hasNoValues
     * @param c collection to check
     * @return true, if c is null or c.size() == 0 or all values are null.
     */
    public static boolean hasNoValues(Collection c) {
        if (c == null || c.size() == 0)
            return true;
        
        for (Object object : c) {
            if (object != null)
                return false;
        }
        return true;
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
     * starts the given action for all items of the given collection - as first action parameter.
     * 
     * @param forCollection to iterate
     * @param doAction to do
     * @return iteration size
     */
    public static final int doFor(Collection<?> forCollection, IAction<?> doAction) {
        int count = 0;
        for (final Object item : forCollection) {
            doAction.setParameter(new Object[] { item });
            doAction.activate();
            count++;
        }
        return count;
    }

    /**
     * delegates to {@link #getSortedList(Collection, Comparator, String, boolean)}, using
     * {@link #getSimpleComparator(Format)}.
     */
    public static Collection<?> getSortedList(Collection<?> collection) {
        return getSortedList(collection,
            NumberUtil.getNumberAndStringComparator(new DefaultFormat()),
            collection.toString(),
            false);
    }

    /**
     * delegates to {@link #getSortedList(Collection, Comparator, String, boolean)}, using
     * {@link #getSimpleComparator(Format)}.
     */
    public static Collection<?> getSortedList(Collection<?> collection, Format formatter, String name) {
        return getSortedList(collection, NumberUtil.getNumberAndStringComparator(formatter), name, false);
    }

    /**
     * tries to sort the given collection through the given comparator. if the comparator or the collection is null,
     * nothing will be done!
     * 
     * @param collection collection to sort
     * @param comparator comparator
     * @param name (optional) name of collection (only for logging)
     * @param createSortedSet if true, a new sorted set will be created and returned - the original collection will not
     *            be sorted!
     * @return sorted list (new TreeSet or original collection)
     */
    public static <T> Collection<T> getSortedList(Collection<T> collection,
            Comparator<T> comparator,
            String name,
            boolean createSortedSet) {
        assert collection != null && comparator != null : "collection and comparator must not be null!";
        if (createSortedSet) {
            LOG.debug("sorting collection of '" + name
                + " in a new TreeSet instance (size:"
                + collection.size()
                + ", comparator:"
                + comparator);
            final SortedSet<T> sortedSet = new TreeSet<T>(comparator);
            sortedSet.addAll(collection);
            LOG.debug("sorting finished (" + name + ")");
            return sortedSet;
        } else {
            LOG.debug("sorting (one time!) collection of '" + name
                + " in the current instance (size:"
                + collection.size()
                + ", comparator:"
                + comparator);
            List<T> slist;
            if (collection instanceof List) {
                slist = (List<T>) collection;
            } else {
                slist = new ArrayList<T>(collection);
            }
            Collections.sort(slist, comparator);
            //avoid an instanceof call and use the direct reference ==> performance!
            if (slist != collection) {
                collection.clear();
                collection.addAll(slist);
            }
            LOG.debug("sorting finished (" + name + ")");
            //return the original instance
            return collection;
        }
    }

    /**
     * Simple copy of Arrays.copyOfRange() of jdk1.6 to avoid dependency to jdk1.6
     * <p/>
     * Copies the specified range of the specified array into a new array. The initial index of the range (<tt>from</tt>
     * ) must lie between zero and <tt>original.length</tt>, inclusive. The value at <tt>original[from]</tt> is placed
     * into the initial element of the copy (unless <tt>from == original.length</tt> or <tt>from == to</tt>). Values
     * from subsequent elements in the original array are placed into subsequent elements in the copy. The final index
     * of the range (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>, may be greater than
     * <tt>original.length</tt>, in which case <tt>null</tt> is placed in all elements of the copy whose index is
     * greater than or equal to <tt>original.length - from</tt>. The length of the returned array will be
     * <tt>to - from</tt>. The resulting array is of the class <tt>newType</tt>.
     * 
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive. (This index may lie outside the array.)
     * @param newType the class of the copy to be returned
     * @return a new array containing the specified range from the original array, truncated or padded with nulls to
     *         obtain the required length
     * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt> or <tt>from &gt; original.length()</tt>
     * @throws IllegalArgumentException if <tt>from &gt; to</tt>
     * @throws NullPointerException if <tt>original</tt> is null
     * @throws ArrayStoreException if an element copied from <tt>original</tt> is not of a runtime type that can be
     *             stored in an array of class <tt>newType</tt>.
     * @since 1.6
     */
    public static <T, U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        T[] copy = ((Object) newType == (Object) Object[].class) ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * concatenates the given arrays into a new array of type newType
     * 
     * @param <T>
     * @param newType new array type
     * @param arrays arrays to copy/concat
     * @return return concatenation of arrays
     */
    public static <T> T[] concat(Class<? extends T[]> newType, T[]... arrays) {
        int newLength = 0;
        for (int i = 0; i < arrays.length; i++) {
            newLength += arrays[i].length;
        }
        return concat((T[]) Array.newInstance(newType.getComponentType(), newLength), arrays);
    }

    /**
     * concatenates the given arrays into a new array of type newType
     * 
     * @param <T>
     * @param <U>
     * @param newArray new array
     * @param arrays arrays to copy/concatenate
     * @return return concatenation of arrays
     */
    public static <T, U> T[] concat(T[] newArray, U[]... arrays) {
        int dest = 0;
        for (int i = 0; i < arrays.length; i++) {
            System.arraycopy(arrays[i], 0, newArray, dest, arrays[i].length);
            dest += arrays[i].length;
        }
        return newArray;
    }

    /**
     * getList
     * 
     * @param <T> item type
     * @param iterator iterator to wrap into a new list
     * @return new filled list instance
     */
    public static final <T> List<T> getList(Iterator<T> iterator) {
        ArrayList<T> list = new ArrayList<T>();
        while (iterator.hasNext()) {
            list.add(iterator.next());

        }
        return list;
    }

    /**
     * combines all given arrays into one list
     * 
     * @param <T> type of list content
     * @param arrays holding all elements to be stored into one list
     * @return list holding all items of all given arrays
     */
    public static final <T> List<T> asListCombined(Object[]... arrays) {
        return addAll(new ArrayList<T>(), arrays);
    }

    /**
     * combines all given arrays into one list
     * 
     * @param <T> type of list content
     * @param collection instance to add all items of all arrays
     * @param arrays holding all elements to be stored into one list
     * @return list holding all items of all given arrays
     */
    public static final <C extends Collection<T>, T> C addAll(C collection, Object[]... arrays) {
        for (int i = 0; i < arrays.length; i++) {
            collection.addAll((Collection<? extends T>) Arrays.asList(arrays[i]));
        }
        return collection;
    }

    /**
     * @param enumClass
     * @return list of enums
     */
    public static final <E extends Enum<E>> List<E> getEnumValues(Class<E> enumClass) {
        return Arrays.asList(enumClass.getEnumConstants());
    }

    /**
     * converts the given array into a list containing the enum names - usefull e.g. on sql/ejb queries creating a ' in
     * ' clause.
     * 
     * @param enums enums to get the names from
     * @return list holding all given enum names
     */
    public static final List<String> getEnumNames(Enum[] enums) {
        List<String> strList = new ArrayList<String>(enums.length);
        for (int i = 0; i < enums.length; i++) {
            strList.add(enums[i].name());
        }
        return strList;
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
     * @param src collection to filter
     * @param filter filter
     * @return filtering collection
     */
    public static final <I extends Iterable<T>, T> I getFiltering(I src, IPredicate<T> filter) {
        return FilteringIterator.getFilteringIterable(src, filter);
    }

    /**
     * getFiltering
     * @param src map to filter
     * @param filter key filter
     * @return filtering map
     */
    public static final <I extends Map<S, T>, S, T extends Comparable<T>> I getFilteringMapKey(I src, IPredicate<T> filter) {
        return FilteringIterator.getFilteringMap(src, filter);
    }

    /**
     * filters the given collection.
     * <p/>
     * Attention: if 'expression' changes afterwards, the collection iterator may change, too! If you
     * do not want that, use {@link #getList(Iterator) to create a copy.
     * 
     * @param <T> collection item type
     * @param src collection to filter
     * @param expression regular expression to be used as filter. the toString() methods of objects will be used to match against.
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
        if (from == null && to == null)
            return src;
        final boolean useNull = from == null || to == null;
        return FilteringIterator.getFilteringIterable(src, new IPredicate<T>() {
            @Override
            public boolean eval(T arg) {
                return (arg == null && useNull) || ((from == null || arg.compareTo(from) >= 0) && (to == null || arg.compareTo(to) <= 0));
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
        if (from == null && to == null)
            return src;
        return FilteringIterator.getFilteringIterable(src, new IPredicate<T>() {
            @Override
            public boolean eval(T arg0) {
                // to be able to reuse the predicate, we can't do the calculations outside (which would be better for the  performance)
                String sfrom = from != null ? ignoreCase && from.toString() != null ? from.toString().toUpperCase()
                    : from.toString() : null;
                String sto = to != null ? ignoreCase && to.toString() != null ? to.toString().toUpperCase()
                    : to.toString() : null;
                boolean useNull = from == null || to == null;

                String sarg = arg0 != null ? ignoreCase && arg0.toString() != null ? arg0.toString().toUpperCase()
                    : arg0.toString() : null;
                return (sarg == null && useNull) || ((sfrom == null || sarg.compareTo(sfrom) >= 0) && (sto == null || sarg.compareTo(sto) <= 0));
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
    public static final <I extends Iterable<T>, S, T> I getTransforming(Iterable<S> src, ITransformer<S, T> transformer) {
        return TransformingIterator.getTransformingIterable(src, transformer);
    }
}
