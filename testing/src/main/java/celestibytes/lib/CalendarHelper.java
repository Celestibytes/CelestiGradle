/*
 * Copyright (C) 2014 Celestibytes
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package celestibytes.lib;

import java.util.Calendar;

/**
 * An {@link Object} that has {@link Calendar} related utilities.
 *
 * @author PizzAna
 *
 */
public final class CalendarHelper
{
    /**
     *
     * Constructs a new {@link CalendarHelper}.
     *
     */
    private CalendarHelper()
    {

    }

    /**
     * The current time.
     */
    private static Calendar curTime = Calendar.getInstance();

    /**
     * The specific event's starting time.
     */
    private static Calendar eventStart = Calendar.getInstance();

    /**
     * The specific event's ending time.
     */
    private static Calendar eventEnd = Calendar.getInstance();

    /**
     * Tells if it's an author's birthday.
     *
     * @return {@code true} if it's an author's birthday, otherwise
     *         {@code false}.
     */
    public static boolean isBirthday()
    {
        return isPizzAnaBirthday() || isOkkapelBirthday();
    }

    /**
     * Tells if it's an PizzAna's birthday.
     *
     * @return {@code true} if it's an PizzAna's birthday, otherwise
     *         {@code false}.
     */
    public static boolean isPizzAnaBirthday()
    {
        setDate(eventStart, Calendar.MAY, 3);
        setDate(eventEnd, Calendar.MAY, 5);

        curTime = Calendar.getInstance();

        return curTime.after(eventStart) && curTime.before(eventEnd);
    }

    /**
     * Tells if it's an Okkapel's birthday.
     *
     * @return {@code true} if it's an Okkapel's birthday, otherwise
     *         {@code false}.
     */
    public static boolean isOkkapelBirthday()
    {
        setDate(eventStart, Calendar.JULY, 7);
        setDate(eventEnd, Calendar.JULY, 9);

        curTime = Calendar.getInstance();

        return curTime.after(eventStart) && curTime.before(eventEnd);
    }

    /**
     * Tells if it's a Halloween.
     *
     * @return {@code true} if it's a Halloween, otherwise {@code false}.
     */
    public static boolean isHalloween()
    {
        setDate(eventStart, Calendar.OCTOBER, 30);
        setDate(eventEnd, Calendar.NOVEMBER, 2);

        curTime = Calendar.getInstance();

        return curTime.after(eventStart) && curTime.before(eventEnd);
    }

    /**
     * Tells if it's a Christmas.
     *
     * @return {@code true} if it's a Christmas, otherwise {@code false}.
     */
    public static boolean isChristmas()
    {
        setDate(eventStart, Calendar.DECEMBER, 24);
        setDate(eventEnd, Calendar.DECEMBER, 27);

        curTime = Calendar.getInstance();

        return curTime.after(eventStart) && curTime.before(eventEnd);
    }

    /**
     * Sets the given {@link Calendar}'s instance to a specific date.
     *
     * @param calendar
     *            the {@link Calendar}'s instance.
     * @param month
     *            the month.
     * @param date
     *            the day of the month.
     */
    private static void setDate(Calendar calendar, int month, int date)
    {
        calendar.clear();
        calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DATE, date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
