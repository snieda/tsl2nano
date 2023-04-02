package de.tsl2.nano.modelkit;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(list, () -> "configuration error: no element with right type for '" + name + "' found");
        // IMPROVE: to optimize performance use a hashmap instead...
        //          using loop instead of stream to enhance performance...
        I e;
        for (int i = 0; i < list.size(); i++) {
            e = list.get(i);
            if (e.getName().equals(name)) {
                return e;
            }
        }
        throw new IllegalStateException(name + " not found in list: " + Arrays.toString(list.toArray()));
    }

    void tagNames(String parent);
}
