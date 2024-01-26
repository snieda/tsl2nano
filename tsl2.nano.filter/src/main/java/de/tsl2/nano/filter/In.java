package de.tsl2.nano.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class In<T> implements NamedValuePredicate<T> {
    String name;
    List<T> expectedOneOf;

    In() {
    }

    public In(String name, T... expectedOneOf) {
        this.name = name;
        this.expectedOneOf = Arrays.asList(expectedOneOf);
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the expectedOneOf
     */
    public List<T> getExpectedOneOf() {
        return expectedOneOf;
    }

    /**
     * @param expectedOneOf the expectedOneOf to set
     */
    public void setExpectedOneOf(List<T> expectedOneOf) {
        this.expectedOneOf = expectedOneOf;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) expectedOneOf.get(0).getClass();
    }

    @Override
    public boolean test(Value<T> t) {
        return t.getName().equals(name) && expectedOneOf.contains(t.getValue());
    }

    @Override
    public String toString() {
        return name + " in " + Objects.toString(expectedOneOf);
    }
}
