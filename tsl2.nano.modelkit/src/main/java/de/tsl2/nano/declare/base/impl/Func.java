package de.tsl2.nano.modelkit.impl;

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Func<T, R> extends AbstractIdentified {

    @JsonIgnore
    private Function<T, R> func;

    public Func(String name, Function<T, R> func) {
        super(name);
        this.func = func;
    }

    @Override
    public void validate() {
        // nothing to do
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
