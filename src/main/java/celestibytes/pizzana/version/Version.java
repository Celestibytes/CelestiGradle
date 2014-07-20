package celestibytes.pizzana.version;

public final class Version implements Comparable<Version>
{
    public final int major;
    public final int minor;
    public final int patch;
    private final Channel channel;
    public final int number;
    private String description = "";

    public Version(String major, String minor, String patch)
    {
        this(Integer.parseInt(major), Integer.parseInt(minor), Integer.parseInt(patch));
    }

    public Version(int major, int minor, int patch)
    {
        this(major, minor, patch, Channel.RELEASE, 0);
    }

    public Version(String major, String minor, String patch, String channel, String number)
    {
        this(Integer.parseInt(major), Integer.parseInt(minor), Integer.parseInt(patch), Channel.parseChannel(channel),
             Integer.parseInt(number));
    }

    public Version(int major, int minor, int patch, String channel, int number)
    {
        this(major, minor, patch, Channel.parseChannel(channel), number);
    }

    public Version(int major, int minor, int patch, Channel channel, int number)
    {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.channel = channel;
        this.number = number;
    }

    public static Version parse(String s)
    {
        String major = "";
        String minor = "";
        String patch = "";
        String channel = "";
        String number = "";
        char[] chars = s.toCharArray();
        boolean hyphen = false;
        int dots = 0;

        for (char c : chars)
        {
            if (c != '.')
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
                    patch = patch + c;
                }
                else if (dots == 3)
                {
                    number = number + c;
                }

                if (c == '-')
                {
                    hyphen = true;
                }

                if (hyphen)
                {
                    if (dots == 2)
                    {
                        channel = channel + c;
                    }
                }
            }
            else
            {
                dots++;
            }
        }

        return channel.equals("") ? new Version(major, minor, patch)
                : new Version(major, minor, patch, channel, number);
    }

    public boolean isStable()
    {
        return channel == Channel.RELEASE;
    }

    public boolean isBeta()
    {
        return channel == Channel.BETA;
    }

    public boolean isAlpha()
    {
        return channel == Channel.ALPHA;
    }

    public Channel getChannel()
    {
        return channel;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public String toString()
    {
        return isStable() ? major + "." + minor + "." + patch
                : major + "." + minor + "." + patch + channel.getPostfix() + "." + number;
    }

    /**
     * Compares this object with the specified object for order.  Returns a negative integer, zero, or a positive
     * integer as this object is less than, equal to, or greater than the specified object.
     * <p/>
     * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) == -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and
     * <tt>y</tt>.  (This implies that <tt>x.compareTo(y)</tt> must throw an exception iff <tt>y.compareTo(x)</tt>
     * throws an exception.)
     * <p/>
     * <p>The implementor must also ensure that the relation is transitive: <tt>(x.compareTo(y)&gt;0 &amp;&amp;
     * y.compareTo(z)&gt;0)</tt> implies <tt>x.compareTo(z)&gt;0</tt>.
     * <p/>
     * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt> implies that <tt>sgn(x.compareTo(z)) ==
     * sgn(y.compareTo(z))</tt>, for all <tt>z</tt>.
     * <p/>
     * <p>It is strongly recommended, but <i>not</i> strictly required that <tt>(x.compareTo(y)==0) ==
     * (x.equals(y))</tt>.  Generally speaking, any class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended language is "Note: this class has a natural
     * ordering that is inconsistent with equals."
     * <p/>
     * <p>In the foregoing description, the notation <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the
     * mathematical <i>signum</i> function, which is defined to return one of <tt>-1</tt>, <tt>0</tt>, or <tt>1</tt>
     * according to whether the value of <i>expression</i> is negative, zero or positive.
     *
     * @param anotherVersion the object to be compared.
     *
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     * the specified object.
     *
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it from being compared to this object.
     */
    @Override
    public int compareTo(Version anotherVersion)
    {
        if (major != anotherVersion.major)
        {
            return major < anotherVersion.major ? -1 : 1;
        }

        if (minor != anotherVersion.minor)
        {
            return minor < anotherVersion.minor ? -1 : 1;
        }

        if (patch != anotherVersion.patch)
        {
            return patch < anotherVersion.patch ? -1 : 1;
        }

        if (isStable() && !anotherVersion.isStable())
        {
            return 1;
        }

        if (isAlpha() && !anotherVersion.isAlpha())
        {
            return -1;
        }

        if (isBeta() && anotherVersion.isAlpha())
        {
            return 1;
        }

        if (isBeta() && !anotherVersion.isBeta())
        {
            return -1;
        }

        if (!isStable() && anotherVersion.isStable())
        {
            return -1;
        }

        if (channel == anotherVersion.channel)
        {
            if (number != anotherVersion.number)
            {
                return number < anotherVersion.number ? -1 : 1;
            }
        }

        return 0;
    }
}
