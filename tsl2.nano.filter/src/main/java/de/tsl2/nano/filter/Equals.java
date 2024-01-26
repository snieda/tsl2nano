package de.tsl2.nano.filter;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Equals<T> implements NamedValuePredicate<T> {
    Value<T> expected;

    Equals() {
    }

    public Equals(Value<T> expected) {
        this.expected = expected;
    }

    @JsonIgnore
    @Override
    public String getName() {
        return expected.getName();
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) expected.getValue().getClass();
    }

    @Override
    public boolean test(Value<T> t) {
        return expected.equals(t);
    }

    @Override
    public String toString() {
        return expected != null ? expected.toString() : super.toString();
    }

    /**
     * @return the expected
     */
    @JsonSerialize(contentAs = Map.class)
    public Value<T> getExpected() {
        return expected;
    }

    /**
     * @param expected the expected to set
     */
    @JsonDeserialize(contentAs = Value.class)
    public void setExpected(Value<T> expected) {
        this.expected = expected;
    }
}
