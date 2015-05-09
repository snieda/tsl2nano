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
import java.util.Queue;

import de.tsl2.nano.core.cls.BeanClass;

/**
 * Runtime transforming Iterator. Wraps the Iterator of the given collection. {@link #remove()} is not supported! Please
 * see {@link #getTransformingIterable(Iterable, ITransformer)} to wrap your original collection into a filtered
 * collection.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class TransformingIterator<S, T> implements Iterator<T> {
    Iterable<S> parent;
    Queue<S> previewItems;
    Iterator<S> parentIt;
    ITransformer<S, T> transformer;
    int i = 0;

    /**
     * constructor
     * 
     * @param parent
     * @param transformer
     */
    public TransformingIterator(Iterable<S> parent, ITransformer<S, T> transformer) {
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
    public T next() {
        return transformer.transform(parentIt.next());
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
    public static <I extends Iterable<T>, S, T> I getTransformingIterable(final Iterable<S> iterable,
            final ITransformer<S, T> transformer) {
        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            BeanClass.getBeanClass(iterable.getClass()).getInterfaces(),
            new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Object result = method.invoke(iterable, args);
                    if (Iterator.class.isAssignableFrom(method.getReturnType())) {
                        result = new TransformingIterator<S, T>(iterable, transformer);
                    }
                    return result;
                }
            });
    }
}
