package de.tsl2.nano.modelkit.impl;

/**
 * base definitions (values, lists, etc.) hold by the configuration and provided to fact implementations.
 */
public class Definition<T> extends AbstractIdentified {
    T value;

    public Definition(String name, T value) {
        super(name);
        this.value = value;
    }

    @Override
    public void validate() {
        //nothing to do...
    }

    public T getValue() {
        visited();
        return value;
    }
}
