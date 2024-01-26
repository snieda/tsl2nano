package de.tsl2.nano.filter;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface ValueMapPredicate extends Predicate<Map<String, Value<?>>> {
    @Override
    default ValueMapPredicate or(Predicate<? super Map<String, Value<?>>> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    @Override
    default ValueMapPredicate and(Predicate<? super Map<String, Value<?>>> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    @Override
    default ValueMapPredicate negate() {
        return (t) -> !test(t);
    }
}
