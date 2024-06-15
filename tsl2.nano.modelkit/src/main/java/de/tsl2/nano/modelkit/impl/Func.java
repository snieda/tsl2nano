/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.modelkit.impl;

import java.util.Objects;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Func<T, R> extends AIdentified {
    static {
        ModelKitLoader.registereElement(Func.class);
    }

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
        Objects.requireNonNull(func,
                () -> config.name + ": func lambda code unavailable (not registered!) on: " + toString());
    }

    public BiFunction<AIdentified, T, R> getFunction() {
        visited();
        return func;
    }

    public R eval(T par) {
        return eval(null, par);
    }

    public R eval(AIdentified caller, T par) {
        visited(par);
        return func.apply(caller, par);
    }
}
