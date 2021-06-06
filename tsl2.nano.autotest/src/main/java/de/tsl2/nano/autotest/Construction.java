package de.tsl2.nano.autotest;

import java.lang.reflect.Constructor;

public class Construction<V> {
	public transient V instance;
	public Constructor<V> constructor;
	public Object[] parameter;
	
	public Construction(V instance) {
		super();
		this.instance = instance;
	}

	public Construction(V instance, Constructor<V> constructor, Object[] parameter) {
		this.instance = instance;
		this.constructor = constructor;
		this.parameter = parameter;
	}
}