package com.jdom.util

import java.text.SimpleDateFormat

/**
 * Created by djohnson on 11/15/14.
 */
class TimeUtil {

    static interface Clock {
        long currentTimeMillis()
    }

    static class WallClock implements Clock {
        @Override
        long currentTimeMillis() {
            return System.currentTimeMillis()
        }
    }

    static Clock clock = new WallClock()

    public static final long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
    public static final long MILLIS_PER_YEAR = MILLIS_PER_DAY * 365;

    static Calendar newCalendar() {
        Calendar cal = Calendar.getInstance()
        cal.setTimeInMillis(clock.currentTimeMillis())
        return cal
    }

    static Date newDate() {
        Date date = new Date(clock.currentTimeMillis())
        return date
    }
    static Date newDateAtStartOfDay(int year, int month, int day) {
        Calendar cal = calendarAtStartOfDay(Calendar.getInstance())
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)

        return cal.getTime()
    }

    static Date dateAtStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance()
        cal.setTime(date)

        return calendarAtStartOfDay(cal).getTime()
    }

    static Date oneDayLater(final Date date) {
        Calendar cal = Calendar.getInstance()
        cal.setTime(date)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.getTime()
    }

    static Calendar calendarAtStartOfDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal
    }

    static long currentTimeMillis() {
        return clock.currentTimeMillis()
    }

    static Date dateFromDashString(String string) {
        return new SimpleDateFormat('yyyy-MM-dd').parse(string)
    }

    static String dashString(Date date) {
        return new SimpleDateFormat('yyyy-MM-dd').format(date)
    }

    static Date zeroHoursAndBelow(Date date) {
        Calendar cal = Calendar.getInstance()
        cal.setTime(date)

        return zeroHoursAndBelow(cal).getTime()
    }

    static Calendar zeroHoursAndBelow(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal
    }
}
