package de.tsl2.nano.modelkit.impl;

import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * rules to check agreements. accessing definitions of its owning configuration. a name, starting with '!' will negate the rule
 * result.
 */
// IMPROVE: rename to Agreement or Truth or Rule?
public class Fact<T> extends AbstractIdentified {
    private static final String PREF_NEGATION = "!";
    @JsonIgnore
    BiFunction<ModelKit, T, Boolean> rule;
    private List<String> andFacts;

    public Fact(String name, BiFunction<ModelKit, T, Boolean> rule, String... andFacts) {
        super(name);
        this.rule = rule;
        this.andFacts = List.of(andFacts);
    }

    @Override
    public void validate() {
        checkExistence(Fact.class, andFacts);
    }

    public boolean ask(T question) {
        visited();
        return (rule == null || ifNegate(rule.apply(config, question)))
            && andFacts.stream().allMatch(n -> get(n, Fact.class).ask(question));
    }

    private boolean ifNegate(boolean result) {
        return negate() ? !result : result;
    }

    private boolean negate() {
        return negate(name);
    }

    private static boolean negate(String factName) {
        return factName.startsWith(PREF_NEGATION);
    }

    public Fact setNegate() {
        name = negate() ? name.substring(1) : PREF_NEGATION + name;
        return this;
    }

    public static final String not(String factName) {
        return negate(factName) ? factName.substring(1) : PREF_NEGATION + factName;
    }
}
