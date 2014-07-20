package celestibytes.pizzana.version;

public enum Channel
{
    RELEASE("stable"),
    BETA("beta"),
    ALPHA("alpha");

    public static final Channel DEFAULT = RELEASE;

    private final String key;
    private final String postfix;

    Channel(String key)
    {
        this.key = key;
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

            if (s.equals(RELEASE.getKey()) || s.equals(""))
            {
                channel = RELEASE;
            }
        }
        else
        {
            channel = RELEASE;
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

    public String getPostfix()
    {
        return postfix;
    }
}
