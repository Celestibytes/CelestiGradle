package celestibytes.pizzana.version;

public enum UpdateChannel
{
    STABLE("stable"),
    LATEST("latest");

    public static final UpdateChannel DEFAULT = STABLE;

    private final String key;

    UpdateChannel(String key)
    {
        this.key = key;
    }

    public String getKey()
    {
        return key;
    }
}
