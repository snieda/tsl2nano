package de.tsl2.nano.filter;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "name", "descriptor", "value" })
public class Value<T> {
    String name;
    T value;
    Object parentInstance;
    PropertyDescriptor descriptor;

    Value() {
    }

    /**
     * @param name
     * @param value
     */
    public Value(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public Value(String name, Class<T> valueType) {
        this(name, (T) null);
    }

    public Value(Object parentInstance, PropertyDescriptor descriptor) {
        this.name = descriptor.getName();
        this.descriptor = descriptor;
        this.parentInstance = parentInstance;
    }

    @Override
    public boolean equals(Object obj) {
        Value o;
        return obj instanceof Value
                && (o = (Value) obj).name.equals(name)
                && (o.getValue() == getValue() || (o.getValue() != null && o.getValue().equals(value)));
    }

    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public Object getParent() {
        return parentInstance;
    }

    /**
     * @return the value
     */
    public T getValue() {
        if (value == null && descriptor != null) {
            try {
                value = (T) descriptor.getReadMethod().invoke(parentInstance);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return name + " = " + (value == null && descriptor != null ? "[refl(" + parentInstance + ")]" : value);
    }

    public void resetValue() {
        value = null;
    }
}
