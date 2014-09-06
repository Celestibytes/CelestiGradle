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

package celestibytes.lib.derp;

/**
 * An {@link Exception} that should be thrown whenever derps happen.
 *
 * @author PizzAna
 *
 */
public class DerpException extends Exception
{
    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = 3418533804520873407L;

    /**
     * Constructs a new {@link DerpException}.
     */
    public DerpException()
    {
        super();
    }

    /**
     * Constructs a new {@link DerpException}.
     *
     * @param message
     *            the message.
     */
    public DerpException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new {@link DerpException}.
     *
     * @param message
     *            the message.
     * @param cause
     *            the cause.
     */
    public DerpException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructs a new {@link DerpException}.
     *
     * @param cause
     *            the cause.
     */
    public DerpException(Throwable cause)
    {
        super(cause);
    }
}
