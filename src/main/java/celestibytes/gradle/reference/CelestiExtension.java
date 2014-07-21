package celestibytes.gradle.reference;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.List;

public class CelestiExtension
{
    public String version = "null";
    public boolean core;
    public String coreVersion = "null";
    public String minecraftVersion = "null";
    public String basePackage = "null";
    public List<String> artifactsList = new ArrayList<String>();
    public Closure manifest = null;
    public boolean versionCheck = false;

    public CelestiExtension()
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

    // DON'T TOUCH ME, I'M HERE ONLY TO MAKE INTELLIJ IDEA HAPPY
    @SuppressWarnings("unused")
    private boolean isCore()
    {
        return core;
    }

    public boolean getCore()
    {
        return core;
    }

    public void setCore(boolean core)
    {
        this.core = core;
    }

    public String getCoreVersion()
    {
        return coreVersion;
    }

    public void setCoreVersion(String coreVersion)
    {
        this.coreVersion = coreVersion;
    }

    public String getMinecraftVersion()
    {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion)
    {
        this.minecraftVersion = minecraftVersion;
    }

    public String getBasePackage()
    {
        return basePackage;
    }

    public void setBasePackage(String basePackage)
    {
        this.basePackage = basePackage;
    }

    public List<String> getArtifactsList()
    {
        return artifactsList;
    }

    public void setArtifactsList(List<String> artifactsList)
    {
        this.artifactsList = artifactsList;
    }

    public void artifact(String in)
    {
        artifactsList.add(in);
    }

    public Closure getManifest()
    {
        return manifest;
    }

    public void setManifest(Closure manifest)
    {
        this.manifest = manifest;
    }

    // DON'T TOUCH ME, I'M HERE ONLY TO MAKE INTELLIJ IDEA HAPPY
    @SuppressWarnings("unused")
    private boolean isVersionCheck()
    {
        return versionCheck;
    }

    public boolean getVersionCheck()
    {
        return versionCheck;
    }

    public void setVersionCheck(boolean versionCheck)
    {
        this.versionCheck = versionCheck;
    }
}
