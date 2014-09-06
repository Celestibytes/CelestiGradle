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
 * An {@link Object} that contains common version information.
 *
 * @author PizzAna
 *
 */
public final class Version implements Comparable<Version>
{
    /**
     * The major version number.
     */
    public final int major;
    
    /**
     * The minor version number.
     */
    public final int minor;
    
    /**
     * The patch version number.
     */
    public final int patch;
    
    /**
     * The version qualifier.
     */
    public final String qualifier;
    
    /**
     * The version's build.
     */
    public final int build;
    
    /**
     *
     * Constructs a new {@link Version}.
     *
     * @param major
     *            the major version number.
     * @param minor
     *            the minor version number.
     * @param patch
     *            the patch version number.
     */
    public Version(String major, String minor, String patch)
    {
        this(Integer.parseInt(major), Integer.parseInt(minor), Integer.parseInt(patch));
    }
    
    /**
     *
     * Constructs a new {@link Version}.
     *
     * @param major
     *            the major version number.
     * @param minor
     *            the minor version number.
     * @param patch
     *            the patch version number.
     */
    public Version(int major, int minor, int patch)
    {
        this(major, minor, patch, null, 0);
    }
    
    /**
     *
     * Constructs a new {@link Version}.
     *
     * @param major
     *            the major version number.
     * @param minor
     *            the minor version number.
     * @param patch
     *            the patch version number.
     * @param qualifier
     *            the version qualifier.
     * @param build
     *            the version's build.
     */
    public Version(String major, String minor, String patch, String qualifier, String build)
    {
        this(Integer.parseInt(major), Integer.parseInt(minor), Integer.parseInt(patch), qualifier, Integer
                .parseInt(build));
    }
    
    /**
     *
     * Constructs a new {@link Version}.
     *
     * @param major
     *            the major version number.
     * @param minor
     *            the minor version number.
     * @param patch
     *            the patch version number.
     * @param qualifier
     *            the version qualifier.
     * @param build
     *            the version's build.
     */
    public Version(int major, int minor, int patch, String qualifier, int build)
    {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.qualifier = qualifier;
        this.build = build;
    }
    
    /**
     * Parses a {@link Version} from the given {@link String}.
     *
     * @param s
     *            the {@link String} representing the {@link Version}.
     * @return the {@link Version} represented by the {@link String}.
     * @throws VersionFormatException
     *             if the {@link String} does not contain a parsable
     *             {@link Version}.
     */
    public static Version parse(String s) throws VersionFormatException
    {
        if (s == null || s.equals(""))
        {
            throw new VersionFormatException("Version may not be null or empty");
        }
        
        String major = "";
        String minor = "";
        String patch = "";
        String qualifier = "";
        String build = "";
        char[] chars = s.toCharArray();
        boolean hyphen = false;
        int dots = 0;
        
        for (char c : chars)
        {
            if (c != '.')
            {
                if (c == '-')
                {
                    hyphen = true;
                }
                else
                {
                    if (dots == 0)
                    {
                        major = major + c;
                    }
                    else if (dots == 1)
                    {
                        minor = minor + c;
                    }
                    else if (dots == 2)
                    {
                        if (hyphen)
                        {
                            qualifier = qualifier + c;
                        }
                        else
                        {
                            patch = patch + c;
                        }
                    }
                    else if (dots == 3)
                    {
                        build = build + c;
                    }
                }
            }
            else
            {
                dots++;
            }
        }
        
        if (dots < 2 && qualifier.equalsIgnoreCase(""))
        {
            throw new VersionFormatException("Version is too short");
        }
        
        return qualifier.equals("") ? new Version(major, minor, patch) : new Version(major, minor, patch, qualifier,
                build);
    }
    
    /**
     * Tells if the {@link Version} is stable according to the data.
     *
     * @return {@code true} if the {@link Version} represents a stable release,
     *         otherwise {@code false}.
     */
    public boolean isStable()
    {
        return qualifier == null;
    }
    
    /**
     * Tells if the {@link Version} is beta according to the data.
     *
     * @return {@code true} if the {@link Version} represents a beta release,
     *         otherwise {@code false}.
     */
    public boolean isBeta()
    {
        return qualifier.equalsIgnoreCase("beta");
    }
    
    /**
     * Tells if the {@link Version} is alpha according to the data.
     *
     * @return {@code true} if the {@link Version} represents a alpha release,
     *         otherwise {@code false}.
     */
    public boolean isAlpha()
    {
        return qualifier.equalsIgnoreCase("alpha");
    }
    
    /**
     * Tells if the {@link Version} is snapshot according to the data.
     *
     * @return {@code true} if the {@link Version} represents a snapshot
     *         release, otherwise {@code false}.
     */
    public boolean isSnapshot()
    {
        return qualifier.equalsIgnoreCase("SNAPSHOT");
    }
    
    /**
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * <p/>
     * <p>
     * The implementor must ensure
     * <tt>sgn(x.compareTo(y)) == -sgn(y.compareTo(x))</tt> for all <tt>x</tt>
     * and <tt>y</tt>. (This implies that <tt>x.compareTo(y)</tt> must throw an
     * exception iff <tt>y.compareTo(x)</tt> throws an exception.)
     * <p/>
     * <p>
     * The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp;
     * y.compareTo(z)&gt;0)</tt> implies <tt>x.compareTo(z)&gt;0</tt>.
     * <p/>
     * <p>
     * Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) ==
     * sgn(y.compareTo(z))</tt>, for all <tt>z</tt>.
     * <p/>
     * <p>
     * It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) ==
     * (x.equals(y))</tt>. Generally speaking, any class that implements the
     * <tt>Comparable</tt> interface and violates this condition should clearly
     * indicate this fact. The recommended language is "Note: this class has a
     * natural ordering that is inconsistent with equals."
     * <p/>
     * <p>
     * In the foregoing description, the notation <tt>sgn(</tt><i>expression</i>
     * <tt>)</tt> designates the mathematical <i>signum</i> function, which is
     * defined to return one of <tt>-1</tt>, <tt>0</tt>, or <tt>1</tt> according
     * to whether the value of <i>expression</i> is negative, zero or positive.
     *
     * @param o
     *            the object to be compared.
     *
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     *
     * @throws NullPointerException
     *             if the specified object is null
     * @throws ClassCastException
     *             if the specified object's type prevents it from being
     *             compared to this object.
     */
    @Override
    public int compareTo(Version o)
    {
        if (o == null)
        {
            throw new NullPointerException();
        }
        
        if (major != o.major)
        {
            return major < o.major ? -1 : 1;
        }
        
        if (minor != o.minor)
        {
            return minor < o.minor ? -1 : 1;
        }
        
        if (patch != o.patch)
        {
            return patch < o.patch ? -1 : 1;
        }
        
        if (isStable() && !o.isStable())
        {
            return 1;
        }
        
        if (isSnapshot() && !o.isSnapshot())
        {
            return -1;
        }
        
        if (isAlpha() && o.isSnapshot())
        {
            return 1;
        }
        
        if (isAlpha() && !o.isAlpha())
        {
            return -1;
        }
        
        if (isBeta() && o.isSnapshot())
        {
            return 1;
        }
        
        if (isBeta() && o.isAlpha())
        {
            return 1;
        }
        
        if (isBeta() && !o.isBeta())
        {
            return -1;
        }
        
        if (!isStable() && o.isStable())
        {
            return -1;
        }
        
        if (qualifier.equals(o.qualifier))
        {
            if (build != o.build)
            {
                return build < o.build ? -1 : 1;
            }
        }
        
        return 0;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + build;
        result = prime * result + major;
        result = prime * result + minor;
        result = prime * result + patch;
        result = prime * result + (qualifier == null ? 0 : qualifier.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        
        if (obj == null)
        {
            return false;
        }
        
        if (!(obj instanceof Version))
        {
            return false;
        }
        
        Version other = (Version) obj;
        
        if (build != other.build)
        {
            return false;
        }
        
        if (major != other.major)
        {
            return false;
        }
        
        if (minor != other.minor)
        {
            return false;
        }
        
        if (patch != other.patch)
        {
            return false;
        }
        
        if (qualifier == null)
        {
            if (other.qualifier != null)
            {
                return false;
            }
        }
        else if (!qualifier.equals(other.qualifier))
        {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString()
    {
        return isStable() ? major + "." + minor + "." + patch : major + "." + minor + "." + patch + qualifier + "."
                + build;
    }
}
