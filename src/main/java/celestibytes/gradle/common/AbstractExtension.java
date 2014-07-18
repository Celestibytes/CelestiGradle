package celestibytes.gradle.common;

import org.gradle.api.Project;

public class AbstractExtension
{
    protected Project project;
    protected String version = "null";

    public AbstractExtension(AbstractPlugin<? extends AbstractExtension> plugin)
    {
        project = plugin.project;
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
