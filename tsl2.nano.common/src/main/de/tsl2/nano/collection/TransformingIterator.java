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
import java.util.Queue;

import de.tsl2.nano.util.bean.BeanClass;

/**
 * Runtime transforming Iterator. Wraps the Iterator of the given collection. {@link #remove()} is not supported! Please
 * see {@link #getTransformingIterable(Iterable, ITransformer)} to wrap your original collection into a filtered
 * collection.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class TransformingIterator<E> implements Iterator<E> {
    Iterable<E> parent;
    Queue<E> previewItems;
    Iterator<E> parentIt;
    ITransformer<?, E> transformer;
    int i = 0;

    /**
     * constructor
     * 
     * @param parent
     * @param transformer
     */
    public TransformingIterator(Iterable<E> parent, ITransformer<?, E> transformer) {
        super();
        this.parent = parent;
        this.transformer = transformer;
        this.parentIt = parent.iterator();
    }

    @Override
    public boolean hasNext() {
        return parentIt.hasNext();
    }

    @Override
    public E next() {
        return (E) transformer.transform(parentIt.next());
    }

    @Override
    public void remove() {
        parentIt.remove();
    }

    /**
     * creates a proxy using the given iterable instance. if an iterator is requested, the orginal iterator will be
     * wrapped into the {@link TransformingIterator}.
     * <p/>
     * As the original instance (the collection) will be preserved, all other methods like size(), contains() etc. will
     * work on the un-filtered content!
     * 
     * @param <I> iterable type - mostly at least a collection
     * @param <T> member type of the iterable
     * @param iterable iterable instance
     * @param transformer item filter/selector
     * @return proxy, providing the given iterable filtered through the given predicate.
     */
    @SuppressWarnings("unchecked")
    public static <I extends Iterable<T>, T> I getTransformingIterable(final I iterable,
            final ITransformer<?, T> transformer) {
        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new BeanClass(iterable.getClass()).getInterfaces(),
            new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Object result = method.invoke(iterable, args);
                    if (Iterator.class.isAssignableFrom(method.getReturnType()))
                        result = new TransformingIterator<T>(iterable, transformer);
                    return result;
                }
            });
    }
}
