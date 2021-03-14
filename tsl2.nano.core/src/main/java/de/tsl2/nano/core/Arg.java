package de.tsl2.nano.core;

import java.text.Format;
import java.text.ParseException;
import java.util.List;

/**
 * describtes an item of a main argument - used by Argumentator.
 */
public class Arg<T extends Comparable<T>> {
    String name;
    String description;
    boolean mandatory;
    Format format;
    T defaultValue;
    List<T> range;
    T value;
    String example;

    static final String KEY_DUTY = "(!)";

    public Arg(String name, boolean mandatory, Format format, List<T> range, T defaultValue, String description,
            String example) {
        this.name = name;
        this.mandatory = mandatory;
        this.format = format;
        this.defaultValue = defaultValue;
        this.description = description;
        this.example = example;
    }

    public Arg(String name, String description) {
        this.name = name;
        // TODO: evaluate other members
        this.mandatory = description.contains(KEY_DUTY);
        this.description = description;
    }

    public T getValue() {
        return value == null && defaultValue != null ? defaultValue : value;
    }

    public void setValue(T newValue) {
        this.value = newValue;
    }

    String range() {
        return range != null && range.size() > 1 ? "[" + range.get(0) + "..." + range.get(range.size() - 1) + "]" : "";
    }

    public T materialize(String value) {
        if (format != null)
            try {
                return (T) format.parseObject(value);
            } catch (ParseException e) {
                ManagedException.forward(e);
            }
        throw new IllegalStateException();
    }

    @Override
    public String toString() {
        return description + " (" + (mandatory ? "(*) " : " ") 
            + (range != null ? range + ", ": "")  
            + (format != null ? format + ", ": "")
            + (defaultValue != null ? "default: " + defaultValue + ")": "");
    }
}
