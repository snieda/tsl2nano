package de.tsl2.nano.util;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.tsl2.nano.core.util.DateUtil;

/**
 * A period is defined of a startDate and an endDate.
 * 
 * @author ts 08.09.2008
 * @version $Revision$
 */
public class Period implements Comparable<Period>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -2334567121622219707L;
    private Date start;
    private Date end;

    /** Datetime format */
    public final static SimpleDateFormat DEFAULT_FORMAT_DE = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    /** Time format */
    public final static SimpleDateFormat DEFAULT_TIMEFORMAT_DE = new SimpleDateFormat("HH:mm");

    /**
     * constructor
     */
    Period() {
        super();
    }
    
    /**
     * Constructor.
     * 
     * @param start startDate
     * @param end endDate
     */
    public Period(Date start, Date end) {
        super();
        this.start = start;
        this.end = end;
    }

    /**
     * Determines the timespan of a given date as a period (e.g. 10.03.2009 00:00:00 - 10.03.2009 23:59:59.999)
     * 
     * @param date date date
     * @return day-span-period
     */
    public static Period getDaySpan(Date date) {
        return new Period(DateUtil.getStartOfDay(date), DateUtil.getEndOfDay(date));

    }

    /**
     * Determines the timespan of a given date as a period (e.g. 01.03.2009 00:00:00 - 31.03.2009 23:59:59.999)
     * 
     * @param date date date
     * @return month-span-period
     */
    public static Period getMonthSpan(Date date) {
        Date start = DateUtil.getStartOfDay(date);
        start = DateUtil.change(start, Calendar.DAY_OF_MONTH, 0);
        Date end = DateUtil.getEndOfDay(date);
        end = DateUtil.setMaximum(end, Calendar.DAY_OF_MONTH);
        return new Period(start, end);

    }

    /**
     * Determines the timespan of a given date as a period (e.g. 01.01.2009 00:00:00 - 31.12.2009 23:59:59.999)
     * 
     * @param date date date
     * @return year-span-period
     */
    public static Period getYearSpan(Date date) {
        Date start = DateUtil.getStartOfDay(date);
        start = DateUtil.change(start, Calendar.MONTH, 0);
        start = DateUtil.change(start, Calendar.DAY_OF_MONTH, 1);
        Date end = DateUtil.getEndOfDay(date);
        end = DateUtil.setMaximum(end, Calendar.MONTH);
        end = DateUtil.setMaximum(end, Calendar.DAY_OF_MONTH);
        return new Period(start, end);

    }

    /**
     * Getter getStart
     * 
     * @return Returns the start.
     */
    public Date getStart() {
        return start;
    }

    /**
     * Getter getEnd
     * 
     * @return Returns the end.
     */
    public Date getEnd() {
        return end;
    }

    /**
     * Setter start
     * 
     * @param start The start to set.
     */
    public void setStart(Date start) {
        this.start = start;
    }

    /**
     * Setter end
     * 
     * @param end The end to set.
     */
    public void setEnd(Date end) {
        this.end = end;
    }

    /**
     * Determines if a given period intersects the period.
     * 
     * @param period period
     * @return true if the given period intersects the period
     */
    public boolean intersects(Period period) {
        return intersects(period.getStart(), period.getEnd());
    }

    /**
     * Another variant of intersection check (save period construction).
     * 
     * @param start start date of the other period
     * @param end end date of the other period
     * @return true if there is an intersection
     */
    public boolean intersects(Date start, Date end) {
        if (getEnd().after(start) && getStart().before(end)) {
            return true;
        }
        return false;
    }

    /**
     * Get the intersection minutes with the given period.
     * 
     * @param period a Period
     * @return the intersection minutes or 0 if there is no intersection
     */
    public int getIntersectionMinutes(Period period) {
        return getIntersectionMinutes(period.getStart(), period.getEnd());
    }

    /**
     * Another variant of intersection calculation.
     * 
     * @param start start date of the other period
     * @param end end date of the other period
     * @return the intersection minutes or 0 if there is no intersection
     */
    public int getIntersectionMinutes(Date start, Date end) {
        if (!intersects(start, end)) {
            return 0;
        }
        final Date intersectStart = getStart().after(start) ? getStart() : start;
        final Date intersectEnd = getEnd().before(end) ? getEnd() : end;

        final int intersection = DateUtil.diffMinutes(intersectStart, intersectEnd);
        return intersection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return DEFAULT_FORMAT_DE.format(start) + " - " + DEFAULT_FORMAT_DE.format(end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Period o) {
        if (this.getStart().compareTo(o.getStart()) == 0) {
            return this.getEnd().compareTo(o.getEnd());
        } else {
            return this.getStart().compareTo(o.getStart());
        }

    }

    /**
     * Calculates the span between start- and enddate in milliseconds.
     * 
     * @return the span between start- and enddate in milliseconds.
     */
    public long getDelta() {
        return DateUtil.diffMillis(start, end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object period) {
        if (period == null) {
            return false;
        }
        if (!(period instanceof Period)) {
            return false;
        }
        return (compareTo((Period) period) == 0);
    }

    /**
     * Check if this period includes another one.
     * 
     * @param period the period that is checked as included
     * @return true if included
     */
    public boolean contains(Period period) {
        if (period == null) {
            return false;
        }
        final Date start = getStart() != null ? getStart() : new Date(Long.MIN_VALUE);
        final Date end = getEnd() != null ? getEnd() : DateUtil.MAX_DATE;
        final Date pStart = period.getStart() != null ? period.getStart() : new Date(Long.MIN_VALUE);
        final Date pEnd = period.getEnd() != null ? period.getEnd() : DateUtil.MAX_DATE;
        return !end.before(pStart) && !start.after(pEnd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((end == null) ? 0 : end.hashCode());
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        return result;
    }

    /**
     * Makes a copy of the period
     * 
     * @return the copy of the period
     */
    public Period copy() {
        Date startDate = null;
        Date endDate = null;
        if (getStart() != null) {
            startDate = new Date(getStart().getTime());
        }
        if (getEnd() != null) {
            endDate = new Date(getEnd().getTime());
        }
        return new Period(startDate, endDate);
    }
}
