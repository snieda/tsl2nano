package de.tsl2.nano.modelkit;

import java.util.List;
import java.util.stream.Collectors;

import de.tsl2.nano.modelkit.impl.Fact;

@SuppressWarnings("unchecked")
public interface Selectable<T> extends Configured {
    boolean canSelect(T current);

    default boolean canSelect(String factName, Object current) {
        return get(factName, Fact.class).ask(current);
    }

    default List<T> from(List<T> list) {
        return list.stream().filter(t -> canSelect(t)).collect(Collectors.toList());
    }
}
