package de.tsl2.nano.modelkit.impl;

import java.util.Objects;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Func<T, R> extends AIdentified {

    @JsonIgnore
    private BiFunction<AIdentified, T, R> func;

    Func() {
    }

    public Func(String name, BiFunction<AIdentified, T, R> func) {
        super(name);
        this.func = func;
    }

    @Override
    public void validate() {
        Objects.requireNonNull(func, config.name + ": func lambda code unavailable (not registered!) on: " + toString());
    }

    public BiFunction<AIdentified, T, R> getFunction() {
        visited();
        return func;
    }

    public R eval(AIdentified caller, T par) {
        visited();
        return func.apply(caller, par);
    }
}
