package de.tsl2.nano.core.util;

import java.util.function.Supplier;

import de.tsl2.nano.core.ManagedException;

@FunctionalInterface
public interface SupplierExVoid<T> extends Supplier<T> {

    void doIt() throws Exception;

    @Override
    default T get() {
        try {
            doIt();
            return null;
        } catch (Exception e) {
            return (T) ManagedException.forward(e);
        }
    }
}