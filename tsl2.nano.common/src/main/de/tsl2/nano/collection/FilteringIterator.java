/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 25, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.collection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import de.tsl2.nano.util.bean.BeanClass;

/**
 * Runtime filtering Iterator. Wraps the Iterator of the given collection. {@link #remove()} is not supported! Please
 * see {@link #getFilteringIterable(Iterable, IPredicate)} to wrap your original collection into a filtered collection.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class FilteringIterator<E> implements Iterator<E> {
    Iterable<E> parent;
    Queue<E> previewItems;
    Iterator<E> previewIt;
    IPredicate<E> predicate;
    int i = 0;

    /**
     * constructor
     * 
     * @param parent
     * @param predicate
     */
    public FilteringIterator(Iterable<E> parent, IPredicate<E> predicate) {
        super();
        this.parent = parent;
        this.predicate = predicate;
        this.previewIt = parent.iterator();
        this.previewItems = new LinkedList<E>();
    }

    @Override
    public boolean hasNext() {
        if (previewItems.size() > 0)
            return true;
        while (previewIt.hasNext()) {
            E n = previewIt.next();
            if (predicate.eval(n))
                previewItems.add(n);
        }
        return previewItems.size() > 0;
    }

    @Override
    public E next() {
        return previewItems.poll();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * creates a proxy using the given iterable instance. if an iterator is requested, the orginal iterator will be
     * wrapped into the {@link FilteringIterator}.
     * <p/>
     * As the original instance (the collection) will be preserved, all other methods like size(), contains() etc. will
     * work on the un-filtered content!
     * 
     * @param <I> iterable type - mostly at least a collection
     * @param <T> member type of the iterable
     * @param iterable iterable instance
     * @param predicate item filter/selector
     * @return proxy, providing the given iterable filtered through the given predicate.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <I extends Iterable<T>, T> I getFilteringIterable(final I iterable, final IPredicate<T> predicate) {
        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new BeanClass(iterable.getClass()).getInterfaces(), new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result = method.invoke(iterable, args);
                if (Iterator.class.isAssignableFrom(method.getReturnType()))
                    result = new FilteringIterator<T>(iterable, predicate);
                return result;
            }
        });
    }
    
    /**
     * creates a proxy using the given map instance. if a key iterator is requested, the orginal iterator will be
     * wrapped into the {@link FilteringIterator}.
     * <p/>
     * As the original instance will be preserved, all other methods like size(), contains() etc. will
     * work on the un-filtered content!
     * 
     * @param <I> iterable type - mostly at least a collection
     * @param <T> member type of the iterable
     * @param map iterable instance
     * @param predicate item filter/selector
     * @return proxy, providing the given iterable filtered through the given predicate.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <I extends Map<S, T>, S, T> I getFilteringMap(final I map, final IPredicate<T> predicate) {
        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new BeanClass(map.getClass()).getInterfaces(), new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result = method.invoke(map, args);
                if (method.getName().equals("keySet"))
                    result = new FilteringIterator<T>((Iterable<T>) map.keySet(), predicate);
                return result;
            }
        });
    }
}
