package de.tsl2.nano.filter;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.tsl2.nano.core.log.LogFactory;

public interface NamedValuePredicate<T> extends ValueMapPredicate {

    String getName();

    @JsonIgnore
    Class<T> getType();

    @SuppressWarnings("rawtypes")
    default Value convert(Value value) {
        if (value.getValue() != null && !value.getValue().getClass().equals(getType())) {
            Class<? extends Object> valueType = value.getValue().getClass();
            if (Long.class.isAssignableFrom(valueType)
                    || Short.class.isAssignableFrom(valueType)
                    || Byte.class.isAssignableFrom(valueType)) {
                // standard deserializing will instantiate integers, but values may be long, short or bytes
                value.setValue(((Number) value.getValue()).intValue());
            }
        }
        return value;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    default boolean test(Map<String, Value<?>> t) {
        Value value = convert(t.get(getName()));
        boolean result = test((Value<T>) Objects.requireNonNull(value));
        if (LogFactory.isDebugLevel()) {
            LogFactory.log(result + " <== check expected {} with: value {}", toString(),
                    LogFactory.isEnabled(LogFactory.LOG_ALL) ? t : value);
        }
        return result;
    }

    boolean test(Value<T> value);
}
