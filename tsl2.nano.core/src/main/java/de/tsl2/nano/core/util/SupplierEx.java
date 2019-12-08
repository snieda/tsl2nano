package de.tsl2.nano.core.util;

import java.util.function.Supplier;

import de.tsl2.nano.core.ManagedException;

@FunctionalInterface
public interface SupplierEx<T> extends Supplier<T> {

    T doGet() throws Exception;

    @Override
    default T get() {
        try {
            return doGet();
        } catch (Exception e) {
            return (T) ManagedException.forward(e);
        }
    }
}