/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.core.util;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;

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
     * for generic purpose. normally you would implement 'new Object[] {o1, o2, ...}.
     * 
     * @param objects objects to pack into a typed array.
     * @return new array holding the given objects.
     */
    public static final <T> T[] asArray(T... objects) {
        Class<T> type = null;
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] != null) {
                type = (Class<T>) objects[i].getClass();
            }
        }
        if (type == null) {
            type = (Class<T>) Object.class;
        }
        return (T[]) Array.newInstance(type, objects.length);
    }

    /**
     * wrap an array into a collection. works on Object[] using Arrays.asList() and on primitive arrays with a simple
     * loop.
     * 
     * @param array object that is an array
     * @return filled collection
     */
    public static Collection asList(Object array) {
        return Util.asList(array);
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
     * hasNoValues
     * 
     * @param c collection to check
     * @return true, if c is null or c.size() == 0 or all values are null.
     */
    public static boolean hasNoValues(Collection c) {
        if (c == null || c.size() == 0) {
            return true;
        }

        for (Object object : c) {
            if (object != null) {
                return false;
            }
        }
        return true;
    }

    public static <T> T[][] split(T[] origin, int itemCount) {
        int arrCount = (origin.length / itemCount);
        T[][] splitted = (T[][]) Array.newInstance(origin.getClass().getComponentType(), arrCount, 0);
        for (int i = 0; i < splitted.length; i++) {
            splitted[i] = copyOfRange(origin, i * itemCount, (i+1) * itemCount); 
        }
        return splitted;
    }
    
    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        return (T[]) copyOfRange(original, from, to, original.getClass());
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
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        T[] copy = ((Object) newType == (Object) Object[].class) ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    public static <T> T[] copy(T[] array) {
        return Arrays.copyOf(array, array.length);
    }

    /**
     * delegates to {@link #concat(Class, Object[]...)}.
     */
    public static <T> T[] concat(T[]... arrays) {
        return (T[]) concat(arrays[0].getClass(), arrays);
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
            if (arrays[i] != null)
                newLength += arrays[i].length;
        }
        return concatNew((T[]) Array.newInstance(newType.getComponentType(), newLength), arrays);
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
    public static <T, U> T[] concatNew(T[] newArray, U[]... arrays) {
        int dest = 0;
        for (int i = 0; i < arrays.length; i++) {
            if (arrays[i] != null) {
                System.arraycopy(arrays[i], 0, newArray, dest, arrays[i].length);
                dest += arrays[i].length;
            }
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
     * @param enumClass
     * @return list of enums
     */
    public static final <E extends Enum<E>> List<E> getEnumValues(Class<E> enumClass) {
        return Arrays.asList(enumClass.getEnumConstants());
    }

    /**
     * finds an enum value through a given enum.toString() value
     * 
     * @param enumClass enum
     * @param enumToString enums value toString()
     */
    public static final <E extends Enum<E>> E findEnum(Class<E> enumClass, String enumToString) {
        if (Util.isEmpty(enumToString)) {
            return null;
        }
        E[] enumConstants = enumClass.getEnumConstants();
        enumToString = enumToString.toLowerCase();
        for (E e : enumConstants) {
            if (e.toString().toLowerCase().equals(enumToString)) {
                return e;
            }
        }
        throw new IllegalArgumentException(enumToString + " can't be found as enum of " + enumClass);
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
     * swaps two elements in an array
     * 
     * @param array
     * @param indexSwap1
     * @param indexSwap2
     */
    public static final void swap(Object[] array, int indexSwap1, int indexSwap2) {
        Object t = array[indexSwap1];
        array[indexSwap1] = array[indexSwap2];
        array[indexSwap2] = t;
    }

    /**
     * loads a simple collection from file
     * 
     * @param file file to be loaded
     * @param delimiter (optional) split regular expression (e.g.:\\s)
     * @return loaded collection
     */
    public static Collection load(String file, String delimiter) {
        try {
            Scanner sc = new Scanner(new File(file));
            if (delimiter != null) {
                sc.useDelimiter(delimiter);
            }
            Collection c = new LinkedList<String>();
            while (sc.hasNext()) {
                c.add(sc.next());
            }
            sc.close();
            return c;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * writes the given collection as simple file. each collection item is divided by div.
     * 
     * @param file file to write
     * @param delimiter item divider
     * @param collection collection to save
     */
    public static void write(String file, String delimiter, Collection collection) {
        try {
            Writer w = new FileWriter(file);
            for (Object i : collection) {
                w.append(i + delimiter);
            }
            w.close();
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * walks through a given iterable until position (=index) and returns that value.
     * 
     * @param iterable source
     * @param position index or -1, if you want the last item
     * @return value at position
     */
    public static <T> T get(Iterable<T> iterable, int position) {
        if (iterable instanceof List) {
            return ((List<T>) iterable).get(position);
        } else {
            int i = 0;
            T item = null;
            for (T t : iterable) {
                if (i++ == position) {
                    return t;
                }
                item = t;
            }
            if (position == -1) {
                return item;
            }

            throw new IllegalArgumentException(
                i == 0 ? iterable + " is empty!" : "position must be between 0 and " + i);
        }
    }

    /**
     * removes all entries having a null or empty (empty string, empty collection) value.
     * 
     * @param map map
     * @return count of removed null-entries
     */
    public static int removeEmptyEntries(Map<String, Object> map) {
        Set<String> keys = map.keySet();
        int count = 0;
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            String k = (String) iterator.next();
            if (Util.isEmpty(map.get(k))) {
                iterator.remove();
                count++;
            }
        }
        return count;
    }

    /**
     * searches the given array for the given element through its equals method. more performance provides
     * Arrays.binarySearch(...), but with some constraints.
     * 
     * @param array to search through
     * @param element to be searched
     * @return true, if array contains element
     */
    public static final boolean contains(Object[] array, Object element) {
        return Arrays.asList(array).indexOf(element) != -1;
    }
    public static final int indexOf(Object[] array, Object element) {
        return Arrays.asList(array).indexOf(element);
    }
}
