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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.cls.BeanClass;

/**
 * Runtime filtering Iterator. Wraps the Iterator of the given collection. {@link #remove()} is not supported! Please
 * see {@link #getFilteringIterable(Iterable, IPredicate)} to wrap your original collection into a filtered collection.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class FilteringIterator<E> implements ListIterator<E> {
    Iterable<E> parent;
    Iterator<E> previewIt;
    IPredicate<E> predicate;
    int i = 0;
    boolean previewing;
    E item;
    int size = -1;

    /**
     * constructor
     * 
     * @param parent
     * @param predicate
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public FilteringIterator(Iterable<E> parent, IPredicate<E> predicate) {
        super();
        this.parent = parent;
        this.predicate = predicate;
        this.previewIt = parent instanceof List ? ((List) parent).listIterator() : parent.iterator();
    }

    @Override
    public boolean hasNext() {
        return preview() != null;
    }

    private E preview() {
        while (previewIt.hasNext()) {
            E n = previewIt.next();
            if (predicate.eval(n)) {
                previewing = true;
                i++;
                return (item = n);
            }
        }
        size = i;
        return null;
    }

    @Override
    public E next() {
        if (!previewing) {
            preview();
        }
        previewing = false;
        return item;
    }

    @Override
    public void remove() {
        size--;
        previewIt.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPrevious() {
        return backview() != null;
    }

    private E backview() {
        ListIterator<E> it = (ListIterator<E>) previewIt;
        while (it.hasPrevious()) {
            E n = it.previous();
            if (predicate.eval(n)) {
                previewing = true;
                i--;
                return (item = n);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E previous() {
        if (!previewing) {
            backview();
        }
        previewing = false;
        return item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextIndex() {
        preview();
        return i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int previousIndex() {
        backview();
        return i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(E e) {
        ((ListIterator<E>) previewIt).set(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(E e) {
        if (size > 0) {
            size++;
        }
        ((ListIterator<E>) previewIt).add(e);
    }

    /**
     * on first call, the size has to be evaluated
     * 
     * @return size of iterable
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected int size() {
        if (size < 0) {
            size = 0;
            Iterator iterator = new FilteringIterator(parent, predicate);
            while (iterator.hasNext()) {
                iterator.next();
                size++;
            }
        }
        return size;
    }

    /**
     * ONLY FOR FRAMEWORK-INTERNAL USE!
     * <p/>
     * if the given proxy has an invocation handler of type {@link FilteringIterator}, the internal used collection will
     * be returned.
     * 
     * @param proxy proxy having an invocation handler of type {@link FilteringIterator}.
     * @return internal collection
     */
    @SuppressWarnings("rawtypes")
    public static Iterable getIterable(Proxy proxy) {
        return ((IterableInvocationHandler)Proxy.getInvocationHandler(proxy)).getIterable();
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
    @SuppressWarnings("unchecked")
    public static <I extends Iterable<T>, T> I getFilteringIterable(final I iterable, final IPredicate<T> predicate) {
        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            BeanClass.getBeanClass(iterable.getClass()).getInterfaces(), new IterableInvocationHandler<I, T>(iterable,
                predicate));
    }

    /**
     * creates a proxy using the given map instance. if a key iterator is requested, the orginal iterator will be
     * wrapped into the {@link FilteringIterator}.
     * <p/>
     * As the original instance will be preserved, all other methods like size(), contains() etc. will work on the
     * un-filtered content!
     * 
     * @param <I> iterable type - mostly at least a collection
     * @param <T> member type of the iterable
     * @param map iterable instance
     * @param predicate item filter/selector
     * @return proxy, providing the given iterable filtered through the given predicate.
     */
    @SuppressWarnings({ "unchecked" })
    public static <I extends Map<S, T>, S, T> I getFilteringMap(final I map, final IPredicate<T> predicate) {
        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            BeanClass.getBeanClass(map.getClass()).getInterfaces(), new IterableInvocationHandler(map.keySet(),
                predicate)
            );
    }
}

/**
 * The invocation handler
 * @param <I>
 * @param <T>
 * @author Tom
 * @version $Revision$ 
 */
class IterableInvocationHandler<I extends Iterable<T>, T> implements InvocationHandler {
    I iterable;
    IPredicate<T> predicate;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return evalInvokation(iterable, predicate, method, args);
    }

    /**
     * constructor
     */
    public IterableInvocationHandler(final I iterable, final IPredicate<T> predicate) {
        super();
        this.iterable = iterable;
        this.predicate = predicate;
    }

    public I getIterable() {
        return iterable;
    }
    
    private static <I extends Iterable<T>, T> Object evalInvokation(final I iterable,
            final IPredicate<T> predicate,
            Method method,
            Object[] args) throws IllegalAccessException, InvocationTargetException {
        Object result;
        if (Iterator.class.isAssignableFrom(method.getReturnType())) {
            result = new FilteringIterator<T>(iterable, predicate);
        } else if (method.getName().equals("size")) {
            result = new FilteringIterator<T>(iterable, predicate).size();
        } else {
            result = method.invoke(iterable, args);
        }
        return result;
    }
}