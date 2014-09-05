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
public class Triple<A, B, C>
{
    private A a;
    private B b;
    private C c;

    public Triple(A a, B b, C c)
    {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public A getA()
    {
        return this.a;
    }

    public B getB()
    {
        return this.b;
    }

    public C getC()
    {
        return this.c;
    }

    public void setA(A o)
    {
        this.a = o;
    }

    public void setB(B o)
    {
        this.b = o;
    }

    public void setC(C o)
    {
        this.c = o;
    }
}
