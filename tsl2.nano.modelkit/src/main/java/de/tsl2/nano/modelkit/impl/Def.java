package de.tsl2.nano.modelkit.impl;

import java.util.Objects;

import lombok.Setter;

/**
 * base definitions (values, lists, etc.) hold by the configuration and provided to fact implementations.
 */
public class Def<T> extends AIdentified {

    static {
        ModelKitLoader.registereElement(Def.class);
    }

    @Setter
    T value;

    Def() {
    }

    public Def(String name, T value) {
        super(name);
        this.value = value;
    }

    @Override
    public void validate() {
        Objects.requireNonNull(value, () -> config.name + ": definition is null (not registered?): " + toString());
    }

    public T getValue() {
        visited();
        return value;
    }
}
