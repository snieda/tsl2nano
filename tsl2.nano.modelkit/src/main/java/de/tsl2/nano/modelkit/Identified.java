package de.tsl2.nano.modelkit;

import java.util.Arrays;
import java.util.List;

/**
 * defines basics for name identifying.
 */
public interface Identified {
    String getName();

    default boolean equals0(Object o) {
        return getClass().equals(o.getClass()) && hashCode() == o.hashCode();
    }

    default int hashCode0() {
        return getName().hashCode();
    }

    static <I extends Identified> I get(List<I> list, String name) {

        // TODO: not performance optimized. use a hashmap instead...
        return list.stream()
            .filter(i -> i.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(name + " not found in list: " + Arrays.toString(list.toArray())));
    }

    void tagNames(String parent);
}
