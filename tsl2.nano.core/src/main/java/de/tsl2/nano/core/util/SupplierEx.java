package de.tsl2.nano.core.util;

import java.util.function.Supplier;

@FunctionalInterface
public interface SupplierEx<T> extends Supplier<T> {

    T doGet() throws Exception;

    @Override
    default T get() {
        try {
            return doGet();
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }
}