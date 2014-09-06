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

package celestibytes.lib.math;

import java.util.Random;

/**
 * An {@link Object} containing general math related utilities.
 *
 * @author PizzAna
 *
 */
public final class MathHelper
{
    /**
     * The instance of the {@link Random} class used in the {@link MathHelper}.
     */
    private static Random random = new Random();
    
    /**
     * Clamps an {@code int} between the given minimum and maximum values.
     *
     * @param value
     *            the {@code int}.
     * @param min
     *            the minimum value.
     * @param max
     *            the maximum value.
     * @return the clamped {@code int}.
     */
    public static int clampInt(int value, int min, int max)
    {
        return value < min ? min : value > max ? max : value;
    }
    
    /**
     * Clamps an {@code double} between the given minimum and maximum values.
     *
     * @param value
     *            the {@code double}.
     * @param min
     *            the minimum value.
     * @param max
     *            the maximum value.
     * @return the clamped {@code double}.
     */
    public static double clampDouble(double value, double min, double max)
    {
        return value < min ? min : value > max ? max : value;
    }
    
    /**
     * Clamps an {@code float} between the given minimum and maximum values.
     *
     * @param value
     *            the {@code float}.
     * @param min
     *            the minimum value.
     * @param max
     *            the maximum value.
     * @return the clamped {@code float}.
     */
    public static float clampFloat(float value, float min, float max)
    {
        return value < min ? min : value > max ? max : value;
    }
    
    /**
     * Clamps an {@code int} between {@code 0} and the given maximum value.
     *
     * @param value
     *            the {@code int}.
     * @param max
     *            the maximum value.
     * @return the clamped {@code int}.
     */
    public static int clampInt(int value, int max)
    {
        return clampInt(value, 0, max);
    }
    
    /**
     * Clamps an {@code double} between {@code 0.0D} and the given maximum
     * value.
     *
     * @param value
     *            the {@code double}.
     * @param max
     *            the maximum value.
     * @return the clamped {@code double}.
     */
    public static double clampDouble(double value, double max)
    {
        return clampDouble(value, 0.0D, max);
    }
    
    /**
     * Clamps an {@code float} between {@code 0.0F} and the given maximum value.
     *
     * @param value
     *            the {@code float}.
     * @param max
     *            the maximum value.
     * @return the clamped {@code float}.
     */
    public static float clampFloat(float value, float max)
    {
        return clampFloat(value, 0.0F, max);
    }
    
    /**
     * Gives the next random {@code int} between {@code 0} and the given maximum
     * value.
     *
     * @param last
     *            the last {@code int}.
     * @param max
     *            the maximum value.
     * @return the next random {@code int} that is different from the last.
     */
    public static int nextInt(int last, int max)
    {
        return nextInt(last, random, max);
    }

    /**
     * Gives the next random {@code int} between {@code 0} and the given maximum
     * value.
     *
     * @param last
     *            the last {@code int}.
     * @param random
     *            the {@link Random}.
     * @param max
     *            the maximum value.
     * @return the next random {@code int} that is different from the last.
     */
    public static int nextInt(int last, Random random, int max)
    {
        int i = random.nextInt(max);
        return i != last ? i : nextInt(last, random, max);
    }

    /**
     * Gives the next random {@code int}.
     *
     * @param last
     *            the last {@code int}.
     * @return the next random {@code int} that is different from the last.
     */
    public static int nextInt(int last)
    {
        return nextInt(last, random);
    }

    /**
     * Gives the next random {@code int}.
     *
     * @param last
     *            the last {@code int}.
     * @param random
     *            the {@link Random}.
     * @return the next random {@code int} that is different from the last.
     */
    public static int nextInt(int last, Random random)
    {
        int i = random.nextInt();
        return i != last ? i : nextInt(last, random);
    }
}
