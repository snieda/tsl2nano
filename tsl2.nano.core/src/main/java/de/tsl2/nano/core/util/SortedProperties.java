package de.tsl2.nano.core.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class SortedProperties extends Properties {
	private static final long serialVersionUID = 1L;

	public SortedProperties() {
	}

	public SortedProperties(Properties defaults) {
		super(defaults);
	}

	@Override
	public synchronized Enumeration<Object> keys() {
		return Collections.enumeration(new TreeSet<>(keySet()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> stringPropertyNames() {
		return new TreeSet(keySet());
	}
}
