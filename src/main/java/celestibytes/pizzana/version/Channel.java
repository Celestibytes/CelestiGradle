package celestibytes.pizzana.version;

public enum Channel
{
    STABLE("stable"),
    BETA("beta", STABLE),
    ALPHA("alpha", BETA);

    public static final Channel DEFAULT = STABLE;

    private final String key;
    private final Channel next;
    private final String postfix;

    Channel(String key)
    {
        this(key, null);
    }

    Channel(String key, Channel next)
    {
        this.key = key;
        this.next = next;
        this.postfix = key.equals("stable") ? "" : "-" + key;
    }

    public static Channel parseChannel(String s)
    {
        Channel channel = DEFAULT;

        if (s != null)
        {
            if (s.equals(ALPHA.getKey()))
            {
                channel = ALPHA;
            }

            if (s.equals(BETA.getKey()))
            {
                channel = BETA;
            }

            if (s.equals(STABLE.getKey()) || s.equals(""))
            {
                channel = STABLE;
            }
        }
        else
        {
            channel = STABLE;
        }

        return channel;
    }

    @Deprecated
    public static Channel getChannelFromString(String s)
    {
        return parseChannel(s);
    }

    public String getKey()
    {
        return key;
    }

    public Channel getNext()
    {
        return next;
    }

    public String getPostfix()
    {
        return postfix;
    }
}
