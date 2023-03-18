package de.tsl2.nano.modelkit;

public interface TriFunction<T, U, V, R> {
    R apply(T t, U u, V v);
}
