package de.tsl2.nano.core.util;

import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.tsl2.nano.core.ManagedException;

/**
 * Utility-class for date and time operations. on default, it is not thread safe! call {@link #multithreaded} to be
 * thread safe.
 * 
 * @author ts 18.02.2009
 * @version $Revision$
 */
public final class DateUtil {
    /** the default date-time format */
    private static final DateFormat DEFAULT_DATETIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
        DateFormat.MEDIUM);
    /** the default date format */
    private static final DateFormat DEFAULT_DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM);
    /** the default time format */
    private static final DateFormat DEFAULT_TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.MEDIUM);
    /** sql date format (used by database queries). not assignable to a specific locale */
    private static final DateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat MINUTE_FORMAT = new SimpleDateFormat("mm:ss");

    private static final long MILLI_TO_MINUTES = 60 * 1000;
    private static final int HOUR_TO_MINUTES = 60;
    private static final int DAY_TO_HOUR = 24;

    public static final long T_MINUTE = MILLI_TO_MINUTES;
    public static final long T_HOUR = T_MINUTE * HOUR_TO_MINUTES;
    public static final long T_DAY = DAY_TO_HOUR * T_HOUR;

    public static final Date MIN_DATE = getDate(0, 0, 0);
    public static final Date MAX_DATE = getDate(9999, -1, -1);

    /** 4 quarters beginnings without year */
    public static final Date Q1 = getDate(9999, 1, 1);
    public static final Date Q2 = getDate(9999, 4, 1);
    public static final Date Q3 = getDate(9999, 7, 1);
    public static final Date Q4 = getDate(9999, 10, 1);

    static final Date[] QUARTERS = new Date[] { Q1, Q2, Q3, Q4 };

    /**
     * Default-Constructor
     */
    private DateUtil() {
        super();
    }

    /**
     * Convert a time period in milliseconds to seconds.
     * 
     * @param millis milliseconds
     * @return seconds
     */
    public static int millisToMinutes(long millis) {
        return (int) (millis / MILLI_TO_MINUTES);
    }

    /**
     * Convert a time period in seconds into milliseconds.
     * 
     * @param minutes minutes
     * @return milliseconds
     */
    public static final long minutesToMillis(int minutes) {
        return minutes * MILLI_TO_MINUTES;
    }

    public static final int diffHours(Date date1, Date date2) {
        return millisToMinutes(diffMillis(date1, date2)) / HOUR_TO_MINUTES;
    }

    /**
     * calculates the difference of 2 given dates in minutes (date2 - date1).
     * 
     * @param date1 date to be subtracted
     * @param date2 the "start date" (from which subtraction is done)
     * @return the diff in minutes
     */
    public static final int diffMinutes(Date date1, Date date2) {
        return millisToMinutes(diffMillis(date1, date2));
    }

    /**
     * calculates the difference of 2 given dates in milliseconds (date2 - date1).
     * 
     * @param date1 date to be subtracted
     * @param date2 the "start date" (from which subtraction is done)
     * @return the difference in milliseconds
     */
    public static long diffMillis(Date date1, Date date2) {
        final Calendar cal = getCalendar();
        cal.setTime(date1);
        final long long1 = cal.getTimeInMillis();// + cal.getTimeZone().getOffset(cal.getTimeInMillis());
        cal.setTime(date2);
        final long long2 = cal.getTimeInMillis();// + cal.getTimeZone().getOffset(cal.getTimeInMillis());
        return long2 - long1;
    }

    /**
     * Adds an amount of milliseconds to a given date
     * 
     * @param date date
     * @param millis millis to be added
     * @return the new date
     */
    public static Date addMillis(Date date, long millis) {
        final Calendar cal = getCalendar();
        cal.setTimeInMillis(date.getTime() + millis);
        return cal.getTime();
    }

    /**
     * Adds an amount of minutes to a given date
     * 
     * @param date date
     * @param minutes minutes to be added
     * @return the new date
     */
    public static Date addMinutes(Date date, int minutes) {
        final Calendar cal = getCalendar();
        cal.setTime(date);
        cal.add(MINUTE, minutes);
        return cal.getTime();

    }

    /**
     * Adds an amount of hours to a given date
     * 
     * @param date date
     * @param hours hours to be added
     * @return the new date
     */
    public static Date addHours(Date date, int hours) {
        final Calendar cal = getCalendar();
        cal.setTime(date);
        cal.add(Calendar.HOUR_OF_DAY, hours);
        return cal.getTime();

    }

    /**
     * Adds an amount of days to a given date
     * 
     * @param date date
     * @param days days to be added
     * @return the new date
     */
    public static Date addDays(Date date, int days) {
        final Calendar cal = getCalendar();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    /**
     * Adds an amount of months to a given date
     * 
     * @param date date
     * @param months months to be added
     * @return the new date
     */
    public static Date addMonths(Date date, int months) {
        final Calendar cal = getCalendar();
        cal.setTime(date);
        cal.add(Calendar.MONTH, months);
        return cal.getTime();
    }

    /**
     * Adds an amount of years to a given date
     * 
     * @param date date
     * @param months years to be added
     * @return the new date
     */
    public static Date addYears(Date date, int years) {
        final Calendar cal = getCalendar();
        cal.setTime(date);
        cal.add(Calendar.YEAR, years);
        return cal.getTime();
    }

    /**
     * concatenates the date fragment of a date-value and the time fragment of a date-value
     * 
     * @param date date to be concatenated
     * @param time time to be concatenated
     * @return the concatenated date
     */
    public static Date concatDateAndTime(Date date, Date time) {
        final Calendar cal = getCalendar();

        cal.setTime(date);
        final int year = cal.get(Calendar.YEAR);
        final int month = cal.get(Calendar.MONTH);
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        cal.setTime(time);
        final int hours = cal.get(Calendar.HOUR_OF_DAY);
        final int minutes = cal.get(Calendar.MINUTE);
        final int seconds = cal.get(Calendar.SECOND);

        cal.set(year, month, day, hours, minutes, seconds);
        return cal.getTime();
    }

    public static float seconds(long millis) {
        return millis / 1000f;
    }
    
    /**
     * see {@link #getTimeUnitInMinutes(TimeUnit, int)} * {@link #MILLI_TO_MINUTES}
     */
    public static long getTimeUnitInMillis(TimeUnit timeUnit, int timeUnitValue) {
        return getTimeUnitInMinutes(timeUnit, timeUnitValue) * MILLI_TO_MINUTES;
    }
    /**
     * Helper to convert from a {@link TimeUnit} based value to a minutes based value.
     * 
     * @param timeUnit the TimeUnit
     * @param timeUnitValue the value (in the given timeUnit)
     * @return the value converted to minutes
     */
    public static int getTimeUnitInMinutes(TimeUnit timeUnit, int timeUnitValue) {
        assert timeUnit != null;
        switch (timeUnit) {
        case DAYS:
            timeUnitValue *= DAY_TO_HOUR * HOUR_TO_MINUTES;
            break;
        case HOURS:
            timeUnitValue *= HOUR_TO_MINUTES;
            break;
        case MINUTES:
            // nothing to do
            break;
        default:
            throw new IllegalArgumentException("unknown timeUnit=" + timeUnit);
        }
        return timeUnitValue;
    }

    /**
     * evaluates the time from now to the next given {@link TimeUnit}. Example: now=20:15, timeUnit=TimeUnit.DAYS -->
     * result=03:45 in millis
     * 
     * @param timeUnit next start time
     * @return time period in millis to reach the next timeUnit
     */
    public static long getDelayToNextTimeUnit(TimeUnit timeUnit) {
        long now = System.currentTimeMillis();
        long mod;
        switch (timeUnit) {
        case DAYS:
            mod = DAY_TO_HOUR * HOUR_TO_MINUTES;
        case HOURS:
            mod = HOUR_TO_MINUTES;
            break;
        case MINUTES:
            mod = MILLI_TO_MINUTES;
            break;
        default:
            throw new IllegalArgumentException("unknown timeUnit=" + timeUnit);
        }
        return now %= mod;
    }

    /**
     * Get the start of a day (time 00:00:00.000) for the given date or the current date if no date is given.
     * 
     * @param date [OPTIONAL] a date for initalization
     * @return a date (current or from param "date") with time: 00:00:00.000)
     */
    public static Date getStartOfDay(Date date) {
        final Calendar startOfDay = getCalendar();
        if (date != null) {
            startOfDay.setTime(date);
        }
        return setTime(startOfDay, 0, 0, 0, 0).getTime();
    }

    /**
     * Get the end of a day (time 23:59:59.999) for the given date or the current date if no date is given.
     * 
     * @param date [OPTIONAL] a date for initalization
     * @return a date (current or from param "date") with time: 23:59:59.999)
     */
    public static Date getEndOfDay(Date date) {
        final Calendar endOfDay = getCalendar();
        if (date != null) {
            endOfDay.setTime(date);
        }
        setTime(endOfDay, 23, 59, 59, 999);
        return endOfDay.getTime();
    }

    /**
     * Get the start of a month for the given date or the current date if no date is given.
     * 
     * @param date [OPTIONAL] a date for initalization
     * @return a date (current or from param "date") with time: 00:00:00.000)
     */
    public static Date getStartOfMonth(Date date) {
        final Calendar startOfMonth = getCalendar();
        if (date != null) {
            startOfMonth.setTime(date);
        }
        startOfMonth.set(Calendar.DAY_OF_MONTH, startOfMonth.getActualMinimum(Calendar.DAY_OF_MONTH));
        return getStartOfDay(startOfMonth.getTime());
    }

    /**
     * Get the end of a month (time 23:59:59.999) for the given date or the current date if no date is given.
     * 
     * @param date [OPTIONAL] a date for initalization
     * @return a date (current or from param "date") with time: 23:59:59.999)
     */
    public static Date getEndOfMonth(Date date) {
        final Calendar endOfMonth = getCalendar();
        if (date != null) {
            endOfMonth.setTime(date);
        }
        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        return getEndOfDay(endOfMonth.getTime());
    }

    /**
     * Get the start of a year for the given date or the current date if no date is given.
     * 
     * @param date [OPTIONAL] a date for initalization
     * @return a date (current or from param "date") with time: 00:00:00.000)
     */
    public static Date getStartOfYear(Date date) {
        final Calendar startOfYear = getCalendar();
        if (date != null) {
            startOfYear.setTime(date);
        }
        startOfYear.set(Calendar.MONTH, startOfYear.getActualMinimum(Calendar.MONTH));
        return getStartOfMonth(startOfYear.getTime());
    }

    /**
     * Get the end of a year (time 23:59:59.999) for the given date or the current date if no date is given.
     * 
     * @param date [OPTIONAL] a date for initalization
     * @return a date (current or from param "date") with time: 23:59:59.999)
     */
    public static Date getEndOfYear(Date date) {
        final Calendar endOfYear = getCalendar();
        if (date != null) {
            endOfYear.setTime(date);
        }
        endOfYear.set(Calendar.MONTH, endOfYear.getActualMaximum(Calendar.MONTH));
        return getEndOfMonth(endOfYear.getTime());
    }

    /**
     * Formats a date to the default date-format
     * 
     * @param d the date
     * @return formatted date
     */
    public static String getFormattedDate(Date d) {
        String str = "";
        if (d != null) {
            str = DEFAULT_DATE_FORMAT.format(d);
        }

        return str;
    }

    /**
     * getFormattedTimeStamp
     * 
     * @return formatted actual timestamp
     */
    public static final String getFormattedTimeStamp() {
        return getFormattedDate(new Date(), "yyyy-MMM-dd_HHmmss_sss");
    }

    /**
     * Formats a date to the default time-format
     * 
     * @param d the date
     * @return formatted time
     */
    public static String getFormattedTime(Date d) {
        String str = "";
        if (d != null) {
            str = DEFAULT_TIME_FORMAT.format(d);
        }

        return str;
    }

    /**
     * Formats a time to the default time-format
     * 
     * @param d the time
     * @return formatted time
     */
    public static String getFormattedTime(long time) {
        return getFormattedTime(new Date(time));
    }

    public static String getFormattedMinutes(long time) {
        return MINUTE_FORMAT.format(new Date(time));
    }

    /**
     * Formats a date to the default date-time-format
     * 
     * @param d the date
     * @return formatted time
     */
    public static String getFormattedDateTime(Date d) {
        String str = "";
        if (d != null) {
            str = DEFAULT_DATETIME_FORMAT.format(d);
        }

        return str;
    }

    /**
     * Formats a date with a given pattern to the default date-time-format
     * 
     * @param d the date
     * @param pattern the pattern for the formatting
     * @return formatted time
     */
    public static String getFormattedDate(Date d, String pattern) {
        String str = "";
        if (d != null) {
            final SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            str = sdf.format(d);
        }

        return str;
    }

    /**
     * getDate
     * 
     * @param year year, if -1, the current maximum will be set
     * @param month month (1-12). if -1, the current maximum will be set
     * @param day day (1-31). if -1, the current maximum will be set for day and time.
     * @return date instance
     */
    public static Date getDate(int year, int month, int day) {
        final Calendar cal = getCalendar();
        cal.clear();

        if (year == -1) {
            year = cal.getActualMaximum(Calendar.YEAR);
        }
        cal.set(Calendar.YEAR, year);
        if (month == -1) {
            month = cal.getActualMaximum(Calendar.MONTH);
        } else {
            month -= 1;
        }
        cal.set(Calendar.MONTH, month);
        if (day == -1) {
            day = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            cal.set(Calendar.HOUR, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
            cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
            cal.set(Calendar.MILLISECOND, cal.getActualMaximum(Calendar.MILLISECOND));
        }
        cal.set(Calendar.DAY_OF_MONTH, day);
        return cal.getTime();
    }

    /**
     * delegates to {@link #getTime(int, int, int)} with second = 0.
     */
    public static Time getTime(int hour, int minute) {
        return getTime(hour, minute, 0, true);
    }

    /**
     * getTime
     * 
     * @param hour
     * @param minute
     * @param second
     * @return new time instance
     */
    public static Time getTime(int hour, int minute, int second, boolean UTC) {
        Calendar cal = getCalendar();
        cal.setTimeInMillis(hour * HOUR_TO_MINUTES * MILLI_TO_MINUTES + minute * MILLI_TO_MINUTES + 1000 * second);
        if (UTC) {
            //subtract the timezone - only if time > timezone-value
            long toffset = cal.getTimeZone().getOffset(cal.getTimeInMillis());
            return new Time(cal.getTimeInMillis() - toffset);
        } else {
            return new Time(cal.getTimeInMillis());
        }
    }

    /**
     * returns desired part of date
     * 
     * @param date date to evaluate
     * @param field field evaluate value for (e.g. {@linkplain Calendar#DAY_OF_MONTH}).
     * @return value of date field
     */
    public static int getFieldOfDate(Date date, int field) {
        final Calendar cal = getCalendar();
        cal.setTime(date);
        return cal.get(field);
    }

    /**
     * getYear
     * 
     * @param date date to eval
     * @return year of given date
     */
    public static final int getYear(Date date) {
        return getFieldOfDate(date, Calendar.YEAR);
    }

    /**
     * getMonth
     * 
     * @param date date to eval
     * @return month of given date
     */
    public static final int getMonth(Date date) {
        return getFieldOfDate(date, Calendar.MONTH);
    }

    /**
     * getDay
     * 
     * @param date date to eval
     * @return day of month of given date
     */
    public static final int getDay(Date date) {
        return getFieldOfDate(date, Calendar.DAY_OF_MONTH);
    }

    /**
     * Adds an amount of days, months etc. to a given date
     * 
     * @param date date
     * @param field field to change (e.g. {@linkplain Calendar#DAY_OF_MONTH})
     * @param amount number to be added
     * @return the new date
     */
    public static Date add(Date date, int field, int amount) {
        final Calendar cal = getCalendar();
        cal.setTime(date);
        cal.add(field, amount);
        return cal.getTime();
    }

    /**
     * changes the given field
     * 
     * @param date date
     * @param field field to change (e.g. {@linkplain Calendar#DAY_OF_MONTH})
     * @param value number to be set
     * @return the new date
     */
    public static Date change(Date date, int field, int value) {
        final Calendar cal = getCalendar();
        cal.setTime(date);
        cal.set(field, value);
        return cal.getTime();
    }

    /**
     * changes the given field to its actual maximum
     * 
     * @param date date
     * @param field field to change (e.g. {@linkplain Calendar#DAY_OF_MONTH})
     * @return the new date
     */
    public static Date setMaximum(Date date, int field) {
        final Calendar cal = getCalendar();
        cal.setTime(date);
        cal.set(field, cal.getActualMaximum(field));
        return cal.getTime();
    }

    /**
     * cleans time to be 00:00:00.000
     * 
     * @return actual date
     */
    public static Date getToday() {
        return clearTime(null);
    }

    /**
     * cleans time to be 00:00:00.000
     * 
     * @return tomorrow date
     */
    public static Date getTomorrow() {
        Date date = addDays(new Date(), 1);
        return setTime(date, 0, 0, 0, 0);
    }

    /**
     * cleans time to be 00:00:00.000. to have the latest time yesterday, use today and subtract one milisecond
     * 
     * @return yesterday date
     */
    public static Date getYesterday() {
        Date date = addDays(new Date(), -1);
        return setTime(date, 0, 0, 0, 0);
    }

    /**
     * getDate
     * 
     * @param formattedString formatted string
     * @return new date instance
     */
    public static Date getDate(String formattedString) {
        try {
            return DEFAULT_DATE_FORMAT.parse(formattedString);
        } catch (final ParseException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * does expect the given date string to be an sql date (yyyy-MM-dd, e.g. as result from database) - like the
     * java.sql.Date does.
     * 
     * @param formattedString formatted sql-date-string
     * @return new date instance
     */
    public static Date getDateSQL(String formattedString) {
        try {
            return SQL_DATE_FORMAT.parse(formattedString);
        } catch (final ParseException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * returns the same format as would be done by java.sql.Date
     * 
     * @param date (optional) to format as sql date
     * @return String with pattern yyyy-MM-dd or null if date is null
     */
    public static final String toSqlDateString(Date date) {
        return date != null ? SQL_DATE_FORMAT.format(date) : null;
    }

    /**
     * @return changed date
     * @see DateUtil#setDate(Date, int, int, int)
     * @see #setTime(Date, int, int, int, int)
     */
    public static Date setDateTime(Date source,
            int year,
            int month,
            int day,
            int hour,
            int minute,
            int second,
            int milli) {
        if (source == null) {
            final Calendar cal = getCalendar();
            source = cal.getTime();
        }
        return setTime(setDate(source, year, month, day), hour, minute, second, milli);
    }

    /**
     * setDate
     * 
     * @param source source date to change
     * @param year year to set or -1 leave it unchanged
     * @param month month (1-12) to set or -1 leave it unchanged
     * @param day day (1-31) to set or -1 leave it unchanged
     * @return changed date
     */
    public static Date setDate(Date source, int year, int month, int day) {
        final Calendar cal = getCalendar();
        cal.setTime(source);
        return setDate(cal, year, month, day).getTime();
    }

    public static Calendar setDate(Calendar cal, int year, int month, int day) {
        if (year != -1) {
            cal.set(Calendar.YEAR, year);
        }
        if (month != -1) {
            cal.set(Calendar.MONTH, month - 1);
        }
        if (day != -1) {
            cal.set(Calendar.DAY_OF_MONTH, day);
        }
        return cal;
    }

    /**
     * setTime
     * 
     * @param source date to change
     * @param hour hours to set or -1 leave it unchanged
     * @param minute minutes to set or -1 leave it unchanged
     * @param second seconds to set or -1 leave it unchanged
     * @param milli millis to set or -1 leave it unchanged
     * @return date with new time
     */
    public static Date setTime(Date source, int hour, int minute, int second, int milli) {
        final Calendar cal = getCalendar();
        if (source != null)
            cal.setTime(source);
        return setTime(cal, hour, minute, second, milli).getTime();
    }

    static Calendar setTime(Calendar cal, int hour, int minute, int second, int milli) {
        if (hour != -1) {
            cal.set(Calendar.HOUR_OF_DAY, hour);
        }
        if (minute != -1) {
            cal.set(Calendar.MINUTE, minute);
        }
        if (second != -1) {
            cal.set(Calendar.SECOND, second);
        }
        if (milli != -1) {
            cal.set(Calendar.MILLISECOND, milli);
        }
        return cal;
    }

    /*
     * Workdays, Holidays, ...
     */

    /**
     * isWorkDay
     * 
     * @param d day to investigate
     * @param holidays specific holidays
     * @param freeDays optional other days than the weekend which shouldn't be seen as workdays (e.g. the friday)
     * @return true, if given date is not a weekend or holiday
     */
    public static boolean isWorkDay(Date d, Collection<Date> holidays, int... freeDays) {
        return !isDayOfWeek(d, SATURDAY, SUNDAY) && !holidays.contains(d);
    }

    /**
     * isDayOfWeek
     * 
     * @param d day to check
     * @param weekDays expected weekdays
     * @return true, if given day is one of given weekdays
     */
    private static boolean isDayOfWeek(Date d, int... weekDays) {
        return isDayOfWeek(getCalendar(), weekDays);
    }

    /**
     * isDayOfWeek
     * 
     * @param d day to check
     * @param weekDays expected weekdays
     * @return true, if given day is one of given weekdays
     */
    private static boolean isDayOfWeek(Calendar cal, int... weekDays) {
        return Arrays.asList(weekDays).contains(cal.get(DAY_OF_WEEK));
    }

    /**
     * getNextWorkday
     * 
     * @param d date to start from
     * @param holidays specific holidays
     * @return next workday, that is not weekend or holiday
     */
    public static Date getNextWorkday(Date d, Collection<Date> holidays) {
        final Calendar cal = getCalendar();
        cal.setTime(d);

        while (true) {
            if (!isWorkDay(cal.getTime(), holidays)) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            } else {
                return cal.getTime();
            }
        }

    }

    /**
     * getWorkdayCount
     * 
     * @param begin day to start from
     * @param end day to end on
     * @param holidays specific holidays
     * @return count of workdays between begin and end.
     */
    public static int getWorkdayCount(Date begin, Date end, Collection<Date> holidays) {
        int c = 0;
        while (!(begin = getNextWorkday(begin, holidays)).after(end))
            c++;
        return c;
    }

    /**
     * checks, if the two periods intersect. use {@link #MIN_DATE} and {@link #MAX_DATE} for limits. if to1 is null, to1
     * will be {@link #MAX_DATE}. if to2 is null, to2 will be {@link #MAX_DATE}.
     * 
     * @param from1 first periods from
     * @param to1 (optional) first periods to
     * @param from2 second periods from
     * @param to2 (optional) second periods to.
     * @return true, if the two periods intersect.
     */
    public static final boolean intersect(Date from1, Date to1, Date from2, Date to2) {
        if (from1 == null) {
            from1 = MIN_DATE;
        }
        if (from2 == null) {
            from2 = MIN_DATE;
        }
        if (to1 == null) {
            to1 = MAX_DATE;
        }
        if (to2 == null) {
            to2 = MAX_DATE;
        }
        return new Period(from1, to1).intersects(from2, to2);
    }

    /**
     * UNTESTED YET checks, if the given dateToCheck is between periodFrom and periodTo.
     * 
     * @param periodFrom period start
     * @param periodTo period end
     * @param dateToCheck date to be checked for period
     * @return true, if period contains dateToCheck
     */
    public static final boolean includes(Date periodFrom, Date periodTo, Date dateToCheck) {
        return new Period(periodFrom, periodTo).contains(new Period(dateToCheck, dateToCheck));
    }

    /**
     * internal calendar instance to be used by all methods, resetting the calendars date/time and creating a new date
     * instance.
     * 
     * @return calendar without lenient calculation
     */
    private static final Calendar getCalendar() {
        Calendar cal = ConcurrentUtil.getCurrent(Calendar.class);
        if (cal == null) {
            cal = Calendar.getInstance();
            ConcurrentUtil.setCurrent(cal);
        }
        return cal;
    }

    /**
     * getTimeDiffUTC
     * 
     * @param endTime end time
     * @param startTime start time
     * @return date instance without GMT correction
     */
    public static final Date getTimeDiffUTC(long endTime, long startTime) {
        Calendar cal = Calendar.getInstance();
        cal.setLenient(false);
        cal.setTimeInMillis(endTime - startTime);
        cal.getTimeZone().setRawOffset(0);
        return cal.getTime();
    }

    public static final int getTimeZoneOffset(long timeInMillis) {
        Calendar cal = Calendar.getInstance();
        return cal.getTimeZone().getOffset(timeInMillis);
    }

    public static final Date clearTime(Date src) {
        return setTime(src, 0, 0, 0, 0);
    }

    public static final Date clearSeconds(Date src) {
        return setTime(src, -1, -1, 0, 0);
    }

    /**
     * sets all date parameters to 0.
     * 
     * @param cal calendar instance
     * @return the given calendar instance
     */
    protected static final Calendar clearDate(Calendar cal) {
        return setDate(cal, 0, 0, 0);
    }

    /**
     * sets all time parameters to the given ones.
     * 
     * @param cal calendar instance
     * @param hour
     * @param minute
     * @param second
     * @param millisecond
     * @return the given calendar instance
     */
    protected static final Calendar setSeconds(Calendar cal, int second, int millisecond) {
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millisecond);
        return cal;
    }

    /**
     * compares only the date parameters of the given dates (no time parameters)
     * 
     * @param first first to compare
     * @param second second to compare
     * @return true, if year, month and day are equal
     */
    public static final boolean equalsDate(Date first, Date second) {
        //IMPROVE: not performance optimized!
        Calendar fcal = getCalendar();
        fcal.setTime(first);
        setTime(fcal, 0, 0, 0, 0);

        Calendar scal = getCalendar();
        scal.setTime(second);
        setTime(scal, 0, 0, 0, 0);
        return fcal.equals(scal);
    }

    /**
     * delegates to {@link Calendar#DAY_OF_YEAR}
     * 
     * @param date date to look for
     * @return count of days in year of given date
     */
    public static final int getDaysOfYear(Date date) {
        Calendar fcal = getCalendar();
        fcal.setTime(date);
        return fcal.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * isPast
     * 
     * @param date date to evaluate
     * @return true, if date is before now
     */
    public static final boolean isPast(Date date) {
        return date.before(new Date());
    }

    /**
     * isFuture
     * 
     * @param date date to evaluate
     * @return true, if date is after now
     */
    public static final boolean isFuture(Date date) {
        return date.after(new Date());
    }

    /**
     * filters the millis from current system time. on current dates, it is possible to store that into an int.<br/>
     * use cases are to have a simple local unique number on int - for test purposes only!
     * 
     * @return current system time without millis - packed into an int
     */
    public static int currentTimeSeconds() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * combines the given year with the given quarter (one of {@link #Q1}, {@link #Q2}, {@link #Q3}, {@link #Q4}).
     * 
     * @param year year to set
     * @param quarter one of {@link #Q1}, {@link #Q2}, {@link #Q3}, {@link #Q4}
     * @return date with year and quarter
     */
    public static final Date getQuarter(int year, Date quarter) {
        return change(quarter, Calendar.YEAR, year);
    }

    /**
     * evaluates the quarter number of the given date
     * 
     * @param date date to analyze
     * @return a number between 1 and 4
     */
    public static final int getCurrentQuarter(Date date) {
        int i;
        int year = getYear(date);
        for (i = 0; i < QUARTERS.length - 1; i++) {
            if (includes(getQuarter(year, QUARTERS[i]),
                add(getQuarter(year, QUARTERS[i + 1]), Calendar.MILLISECOND, -1), date)) {
                break;
            }
        }
        return i + 1;
    }

    /**
     * evaluates the quarter number of the next quarter
     * 
     * @param date date to analyze
     * @return a number between 1 and 4
     */
    public static final int getNextQuarter(Date date) {
        int q = getCurrentQuarter(date);
        return q == QUARTERS.length ? 1 : q + 1;
    }

    /**
     * getQuarters
     * 
     * @param dates dates to evaluate quarters for
     * @return unique list of quarters, given by dates
     */
    public static final List<Date> getQuarters(Date... dates) {
        List<Date> quarters = new ListSet<Date>();
        for (int i = 0; i < dates.length; i++) {
            quarters.add(getQuarter(getYear(dates[i]), QUARTERS[getCurrentQuarter(dates[i]) - 1]));
        }
        return quarters;
    }

    /**
     * getWeekNumber
     * 
     * @param date date
     * @return week of year
     */
    public static final int getWeekOfYear(Date date) {
        return (getFieldOfDate(date, Calendar.DAY_OF_YEAR) / 7) + 1;
    }

    /**
     * evaluates the min of all given dates
     * 
     * @param dates dates to eval
     * @return min date
     */
    public static final Date min(Date... dates) {
        return NumberUtil.min(dates);
    }

    /**
     * evaluates the max of all given dates
     * 
     * @param dates dates to eval
     * @return max date
     */
    public static final Date max(Date... dates) {
        return NumberUtil.max(dates);
    }

    /**
     * cuts all millis from 1970 until the current year. this could be done trough using a date of 1970, too. usable to
     * assign a unique value to an int. but be careful: each year costs 365 * 24 * 3600 * 1000 milliseconds. after 3
     * weeks, the {@link Integer#MAX_VALUE} is reached.
     * 
     * @param timeInMillis
     * @return millis without year information
     */
    public static final int getMillisWithoutYear(long timeInMillis) {
        return (int) timeInMillis - (getYear(getToday()) * 365 * 24 * 3600 * 1000);
    }

    /**
     * cuts all parts that are seconds and smaller
     * 
     * @param timeInMillis
     * @return given time without seconds and milliseconds
     */
    public static final long cutSeconds(long timeInMillis) {
        return timeInMillis - (timeInMillis % MILLI_TO_MINUTES);
    }

    public static final long cutTime(long timeInMillis) {
        return timeInMillis - (timeInMillis % (DAY_TO_HOUR * HOUR_TO_MINUTES * MILLI_TO_MINUTES));
    }
}
