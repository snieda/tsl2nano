package de.tsl2.nano.core.util;

import java.util.function.Supplier;

@FunctionalInterface
public interface SupplierExVoid<T> extends Supplier<T> {

    void doIt() throws Exception;

    @Override
    default T get() {
        try {
            doIt();
            return null;
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }
}