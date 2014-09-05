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

package celestibytes.lib.version;

/**
 * An {@link IllegalArgumentException} that is thrown to indicate that the
 * application has attempted to convert a string to a {@link Version}, but that
 * the string does not have the appropriate format.
 *
 * @author PizzAna
 *
 */
public class VersionFormatException extends IllegalArgumentException
{
    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = 2290788731669472257L;
    
    /**
     *
     * Constructs a new {@link VersionFormatException} with no detail message.
     *
     */
    public VersionFormatException()
    {
        super();
    }
    
    /**
     *
     * Constructs a new {@link VersionFormatException} with the specified detail
     * message.
     *
     * @param s
     *            the detail message.
     */
    public VersionFormatException(String s)
    {
        super(s);
    }
}
