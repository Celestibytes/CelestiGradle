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

/**
 * An {@link Object}
 *
 * @author PizzAna
 *
 */
public class Tuple<A, B>
{
    private A a;
    private B b;
    
    public Tuple(A a, B b)
    {
        this.a = a;
        this.b = b;
    }
    
    /**
     * @return the a
     */
    public A getA()
    {
        return a;
    }

    /**
     * @param a
     *            the a to set
     */
    public void setA(A a)
    {
        this.a = a;
    }

    /**
     * @return the b
     */
    public B getB()
    {
        return b;
    }

    /**
     * @param b
     *            the b to set
     */
    public void setB(B b)
    {
        this.b = b;
    }
}
