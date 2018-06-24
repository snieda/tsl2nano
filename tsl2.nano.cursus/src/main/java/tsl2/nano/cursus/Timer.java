package tsl2.nano.cursus;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;

import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Holds a start-end period and is able to walk through this period on given time-steps (see {@link #runThrough(Date, Date)} - then it is a generator.
 * @author Tom
 */
@Entity
class Timer implements Serializable {
	private static final long serialVersionUID = 1L;
	Date from;
	Date until;
	/** a value given by the {@link Calendar}. E.g. Calendar.DAY_OF_MONTH. */
	int stepType;
	/** number of {@link #stepType} for each step. If 0, the timer is only a period, not a generator */
	int stepLength;
	/** explizit step exceptions on given {@link #stepType} to be ignored. */
	List<Integer> stepExceptions;
	Set<Timer> subTimers; //TODO: implement sub-timers
	
	int currentSteps;
	transient Calendar current;

	
	public Timer() {
	}

	public Timer(Date from, Date until) {
		this(from, until, 0, 0);
	}
	
	public Timer(Date from, Date until, int stepType, int stepLength, Integer...stepExceptions) {
		this.from = from;
		this.until = until;
		this.stepType = stepType;
		this.stepLength = stepLength;
		this.stepExceptions = Arrays.asList(stepExceptions);
		current = Calendar.getInstance();
		reset();
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
		return DateUtil.includes(from, current.getTime(), date);
	}
	List<Date> runThrough(Date from, Date until) {
		LinkedList<Date> steps = new LinkedList<>();
		Date d;
		while((d = next()) != null)
			if (DateUtil.includes(from, until, d)) //TODO: use from/until in next()
				steps.add(d);
		return steps;
	}
	void reset() {
		currentSteps = 0;
		current.setTime(stepLength > 0 || until == null? from : until);
	}
	/**
	 * @return true, if a step-length was defined, to it can {@link #runThrough(Date, Date)}
	 */
	public boolean isGenerator() {
		return stepLength > 0;
	}
	Timer each(Timer subTimer) {
		getSubTimers().add(subTimer);
		return this;
	}
	Set<Timer> getSubTimers() {
		if (subTimers == null)
			subTimers = new HashSet<>();
		return subTimers;
	}
	@Override
	public String toString() {
		return Util.toString(getClass(), from, stepLength > 0 ? " [" + (stepLength * stepType) + "] " : "", until);
	}
}