/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.cursus;

import java.io.Serializable;

import de.tsl2.nano.bean.PReference;

/**
 * References the real entity instance to change
 * @author Tom
 */
public class Res<O, V> extends PReference<O, V> implements Serializable {
	private static final long serialVersionUID = 1L;
	protected String type;
	protected Object objectid;

	public Res() {
	}

	public Res(O instance, String path) {
		super(instance, path);
	}

	public Res(Class<O> type, Object objectid, String path) {
		this(type.getName(), objectid, path);
	}

	public Res(String type, Object objectid, String path) {
		this.type = type;
		this.objectid = objectid;
		setDescription(createDescription(type, objectid, path));
	}

	public Object getObjectid() {
		return objectid;
	}

	//	@Override
	//	public String toString() {
	//		return Util.toString(getClass(), type, objectid, path);
	//	}
}