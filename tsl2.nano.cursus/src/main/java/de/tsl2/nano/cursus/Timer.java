package de.tsl2.nano.cursus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Holds a start-end period and is able to walk through this period on given time-steps (see {@link #runThrough(Date, Date)} - then it is a generator.
 * @author Tom
 */
public class Timer implements Serializable {
	private static final long serialVersionUID = 1L;
	protected Date from;
	protected Date until;
	/** a value given by the {@link Calendar}. E.g. Calendar.DAY_OF_MONTH. */
	protected int stepType;
	/** number of {@link #stepType} for each step. If 0, the timer is only a period, not a generator */
	protected int stepLength;
	/** explizit step exceptions on given {@link #stepType} to be ignored. 
	 * It is defined as ArrayList, as List or Set is not Serializable and cannot be handled by JPA! */
	protected ArrayList<Integer> stepExceptions;
	protected Set<Timer> subTimers; //TODO: implement sub-timers

	transient int currentSteps;
	transient Calendar current;

	public Timer() {
		this(null, null);
	}

	public Timer(Date from, Date until) {
		this(from, until, Calendar.DAY_OF_YEAR, 1);
	}

	public Timer(Date from, Date until, int stepType, int stepLength, Integer... stepExceptions) {
		this.from = Util.value(from, DateUtil.MIN_STD_DATE);
		this.until = Util.value(until, DateUtil.MAX_STD_DATE);
		this.stepType = stepType;
		if (stepLength <= 0) {
			throw new IllegalArgumentException("stepLength must be greater than 0");
		}
		this.stepLength = stepLength;
		this.stepExceptions = new ArrayList<>(Arrays.asList(stepExceptions));
		preparePeriod();
	}

	private void preparePeriod() {
		if (current == null) {
			from = Util.value(from, DateUtil.MIN_STD_DATE);
			until = Util.value(until, DateUtil.MAX_STD_DATE);
			current = Calendar.getInstance();
			reset();
		}
	}

	Date next() {
		do {
			current.add(stepType, stepLength);
			if (current.getTime().after(until))
				return null;
		} while (stepExceptions.contains(current.get(stepType)));
		currentSteps++;
		return current.getTime();
	}

	//	boolean expired(String cron) {
	//		return DateUtil.includes(from, current.getTime(), date);
	//	}
	/** @return true, if timers period includes the given date */
	boolean expired(Date date) {
		preparePeriod();
		return DateUtil.includes(from, current.getTime(), date);
	}

	boolean isPartOf(Date from, Date until) {
		preparePeriod();
		return DateUtil.contains(from, until, this.from, this.until);
	}

	List<Date> runThrough(Date from, Date until) {
		preparePeriod();
		LinkedList<Date> steps = new LinkedList<>();
		Date d;
		while ((d = next()) != null)
			if (DateUtil.includes(from, until, d)) //TODO: use from/until in next()
				steps.add(d);
		return steps;
	}

	void reset() {
		currentSteps = 0;
		current.setTime(stepLength > 0 || until == null ? from : until);
	}

	/**
	 * @return true, if a step-length was defined, to it can {@link #runThrough(Date, Date)}
	 */
	public boolean isGenerator() {
		return stepLength > 0;
	}

	/*
	 * TODO: implement cascading timers
	 */
	//	Timer each(Timer subTimer) {
	//		getSubTimers().add(subTimer);
	//		return this;
	//	}
	//	Set<Timer> getSubTimers() {
	//		if (subTimers == null)
	//			subTimers = new HashSet<>();
	//		return subTimers;
	//	}
	//	public void setSubTimers(Set<Timer> subTimers) {
	//		this.subTimers = subTimers;
	//	}

	@Override
	public String toString() {
		return Util.toString(getClass(), from, stepLength > 0 ? " [" + (stepLength * stepType) + "] " : "", until);
	}
}