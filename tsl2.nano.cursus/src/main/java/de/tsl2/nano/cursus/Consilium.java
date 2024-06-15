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
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.cursus.Processor.Id;
import de.tsl2.nano.repeat.ICommand;

public class Consilium implements IConsilium, Comparable<Consilium>, Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	protected String author;
	protected Date created;
	protected Date changed;
	protected Timer timer;
	protected Priority priority;
	protected Status status;
	protected String seal;
	protected Set<? extends ICommand<?>> exsecutios;
	transient Id trusted;

	public Consilium() {
	}

	public Consilium(String author, Timer timer, Priority priority, ICommand<?>... exsecutios) {
		this.author = author;
		this.timer = timer;
		this.priority = priority;
		this.exsecutios = new ListSet<>(Arrays.asList(exsecutios));
		created = new Date();
		status = Status.INACTIVE;
		seal = createSeal();
	}

	private String createSeal() {
		return StringUtil.toHexString(ByteUtil.cryptoHash(ByteUtil.serialize(this)));
	}

	@Override
	public void refreshSeal(Id processor) {
		if (!processor.equals(trusted))
			throw new IllegalStateException("The processor " + processor + " is not trusted!");
		changed = new Date();
		seal = createSeal();
		trusted = null;
	}

	@Override
	public void checkValidity(Id processor) {
		if (timer == null)
			throw new IllegalStateException("timer must be filled!");
		//TODO: change hash-creation
		//		if (!createSeal().equals(seal))
		//			throw new IllegalStateException("consilium " + this + " seal is broken!");
		trusted = processor;
	}

	@Override
	public int compareTo(Consilium o) {
		if (this == o)
			return 0;
		if (timer == null)
			return 1;
		if (o.timer == null)
			return -1;
		int c = timer.from.compareTo(o.timer.from);
		if (c == 0)
			c = -1 * priority.index.compareTo(o.priority.index);
		if (c == 0)
			c = status != null ? status.compareTo(o.status) : 0;
		if (c == 0)
			c = getName().toString().compareTo(o.getName().toString());
		return c;
	}

	@Override
	protected Consilium clone() throws CloneNotSupportedException {
		return (Consilium) super.clone();
	}

	public Consilium createAutomated(Date from) {
		return clone(new Timer(from, null), "AUTOMATED");
	}

	public Consilium clone(Timer timer, String author) {
		try {
			Consilium automated = clone();
			automated.timer = timer;
			automated.author = author != null ? author : "cloned from: " + this.timer.toString();
			automated.created = new Date();
			return automated;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public Set<Consilium> createAutomated(Date from, Date until) {
		Set<Consilium> automated = new HashSet<>();
		for (Date d : timer.runThrough(from, until)) {
			automated.add(createAutomated(d));
		}
		Processor.log("preparing " + automated.size() + " automated consilii of type \n\t" + this
				+ "\n\tto processing instance");
		return automated;
	}

	@Override
	public Status getStatus() {
		if (status == null)
			status = Status.INACTIVE;
		return status;
	}

	@Override
	public void setStatus(Status newStatus) {
		this.status = newStatus;
	}

	@Override
	public Set<? extends ICommand<?>> getExsecutios() {
		return exsecutios;
	}

	@Override
	public Timer getTimer() {
		return timer;
	}

	@Override
	public boolean hasFixedContent() {
		return getExsecutios().stream().anyMatch(e -> e instanceof Exsecutio && ((Exsecutio) e).hasFixedContent());
	}

	@Override
	public boolean affects(Object id) {
		return getExsecutios().stream()
				.anyMatch(e -> e instanceof Exsecutio && id.equals(((Exsecutio) e).getMutatio().res.objectid));
	}

	@Override
	public Object getName() {
		return Util.isEmpty(exsecutios) ? toString() : exsecutios.iterator().next().getName();
	}

	@Override
	public String toString() {
		return Util.toString(getClass(), exsecutios, timer, status, changed);
	}
}