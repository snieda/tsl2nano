package tsl2.nano.cursus;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.repeat.ICommand;
import tsl2.nano.cursus.Processor.Id;

public class Consilium implements IConsilium, Comparable<Consilium>, Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	protected String author;
	protected Date created;
	protected Date changed;
	protected Timer timer;
	protected Priority priority;
	protected Status status;
	protected String seal;
	protected  Set<? extends ICommand<?>> exsecutios;
	transient Id trusted;

	
	public Consilium() {
	}

	public Consilium(String author, Timer timer, Priority priority, Exsecutio<?>...exsecutios) {
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
	public String toString() {
		return Util.toString(getClass(), exsecutios, timer, status);
	}

	@Override
	public int compareTo(Consilium o) {
		if (timer == null)
			return 1;
		if (o.timer == null)
			return -1;
		int c = timer.from.compareTo(o.timer.from);
		if (c == 0)
			c = priority.index.compareTo(o.priority.index);
		return c;
	}
	@Override
	protected Consilium clone() throws CloneNotSupportedException {
		return (Consilium) super.clone();
	}
	public Consilium createAutomated(Date from) {
		try {
			Consilium automated = clone();
			automated.timer = new Timer(from, null);
			automated.author = "AUTOMATED";
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
		return automated;
	}

	@Override
	public Status getStatus() {
		return status;
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
		return getExsecutios().stream().anyMatch(e -> e instanceof Exsecutio && ((Exsecutio)e).hasFixedContent());
	}
}