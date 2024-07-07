package de.tsl2.nano.core.util;

import de.tsl2.nano.core.cls.IValueAccess;
import de.tsl2.nano.core.messaging.EventController;

public class ValueHolder<T> implements IValueAccess<T> {
	T value;

	ValueHolder() {
	}
	public ValueHolder(T value) {
		this.value = value;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public void setValue(T object) {
		this.value = object;
	}

	@Override
	public Class<T> getType() {
		return value != null ? (Class<T>) value.getClass() : null;
	}

	@Override
	public EventController changeHandler() {
		return null;
	}
	
}
