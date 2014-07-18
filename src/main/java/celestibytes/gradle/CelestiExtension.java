package celestibytes.gradle;

public class CelestiExtension
{
    protected String version = "null";

    public CelestiExtension(CelestiGradlePlugin plugin)
    {
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }
}
