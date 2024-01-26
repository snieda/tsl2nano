package de.tsl2.nano.filter;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class Between<T extends Comparable<? super T>> implements NamedValuePredicate<T> {
    String name;
    // @JsonTypeInfo(
    // use = JsonTypeInfo.Id.NAME,
    // include = JsonTypeInfo.As.PROPERTY,
    // property = "Type")
    // @JsonSubTypes({
    // @JsonSubTypes.Type(value = LocalDate.class, name = "date"),
    // @JsonSubTypes.Type(value = LocalTime.class, name = "time"),
    // @JsonSubTypes.Type(value = String.class, name = "string"),
    // @JsonSubTypes.Type(value = Double.class, name = "number")})
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    T min;

    // @JsonTypeInfo(
    // use = JsonTypeInfo.Id.NAME,
    // include = JsonTypeInfo.As.PROPERTY,
    // property = "Type")
    // @JsonSubTypes({
    // @JsonSubTypes.Type(value = LocalDate.class, name = "date"),
    // @JsonSubTypes.Type(value = LocalTime.class, name = "time"),
    // @JsonSubTypes.Type(value = String.class, name = "string"),
    // @JsonSubTypes.Type(value = Double.class, name = "number")})
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    T max;

    Between() {
    }

    Between(String name, T min, T max) {
        this.name = name;
        this.min = min;
        this.max = max;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the min
     */
    public T getMin() {
        return min;
    }

    /**
     * @param min the min to set
     */
    public void setMin(T min) {
        this.min = min;
    }

    /**
     * @return the max
     */
    public T getMax() {
        return max;
    }

    /**
     * @param max the max to set
     */
    public void setMax(T max) {
        this.max = max;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) (min != null ? min.getClass() : max.getClass());
    }

    @Override
    public boolean test(Value<T> t) {
        return t.getName().equals(name)
                && (min == null || t.getValue() == null || min.compareTo(t.getValue()) <= 0)
                && (max == null || t.getValue() == null || max.compareTo(t.getValue()) >= 0);
    }

    @Override
    public String toString() {
        return name + " between " + min + " and " + max;
    }
}
