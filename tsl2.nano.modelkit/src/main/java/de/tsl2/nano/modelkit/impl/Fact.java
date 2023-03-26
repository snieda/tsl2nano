package de.tsl2.nano.modelkit.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.tsl2.nano.modelkit.Identified;
import lombok.Getter;
import lombok.Setter;

/**
 * rules to check agreements. accessing definitions of its owning configuration. a name, starting with '!' will negate the rule
 * result.
 */
public class Fact<T> extends AIdentified {
    static final String PREF_NEGATION = "!";
    @JsonIgnore
    BiFunction<ModelKit, T, Boolean> rule;

	@Getter @Setter
    private List<String> andFacts;

    public Fact(String name, BiFunction<ModelKit, T, Boolean> rule, String... andFacts) {
        super(name);
        this.rule = rule;
        this.andFacts = Arrays.asList(andFacts);
    }

    @Override
    public void validate() {
        checkExistence(Fact.class, andFacts);
    }

    @Override
    public void tagNames(String parent) {
        super.tagNames(parent);
        tag(parent, andFacts);
    }

    public boolean ask(T question) {
        visited();
        List<Fact> facts = get(Fact.class);
        return (rule == null || ifNegate(rule.apply(config, question)))
            && andFacts.stream().allMatch(n -> Identified.get(facts, n).ask(question));
    }

    private boolean ifNegate(boolean result) {
        return negate() ? !result : result;
    }

    private boolean negate() {
        return negate(name);
    }

    static boolean negate(String factName) {
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
