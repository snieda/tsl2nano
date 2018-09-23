package de.tsl2.nano.h5.collector;

import java.util.Collection;
import java.util.Map;

import de.tsl2.nano.bean.def.BeanCollector;

public interface IItemProvider<T> {
    @SuppressWarnings("rawtypes")
    T createItem(T srcInstance, Map context);
    Collection<? extends T> createItems(T srcInstance, Map context);
    String getName();
    void check(BeanCollector<Collection<T>, T> caller);
}
