package de.tsl2.nano.modelkit.impl;

import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Func<T, R> extends AbstractIdentified {

    @JsonIgnore
    private Function<T, R> func;

    Func() {
    }

    public Func(String name, Function<T, R> func) {
        super(name);
        this.func = func;
    }

    @Override
    public void validate() {
        Objects.requireNonNull(func, config.name + ": func lambda code unavailable (not registered!) on: " + toString());
    }

    public Function<T, R> getFunction() {
        visited();
        return func;
    }

    public R eval(T par) {
        visited();
        return func.apply(par);
    }
}
