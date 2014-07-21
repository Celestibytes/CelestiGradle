package celestibytes.gradle.reference;

import groovy.lang.Closure;
import org.gradle.api.ProjectConfigurationException;

import java.util.ArrayList;
import java.util.List;

public class CelestiExtension
{
    public String version;
    public boolean minecraftMod = false;
    public boolean coreDependant = false;
    public String coreVersion;
    public String minecraftVersion;
    public String basePackage;
    public List<String> artifactsList = new ArrayList<String>();
    public Closure manifest = null;
    public boolean versionCheckable = false;

    public CelestiExtension()
    {
    }

    public String getVersion()
    {
        if (version == null)
        {
            throw new ProjectConfigurationException("You must set version!", new NullPointerException());
        }

        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public boolean isMinecraftMod()
    {
        return minecraftMod;
    }

    public boolean getMinecraftMod()
    {
        return minecraftMod;
    }

    public void setMinecraftMod(boolean minecraftMod)
    {
        this.minecraftMod = minecraftMod;
    }

    public boolean isCoreDependant()
    {
        return coreDependant;
    }

    public boolean getCoreDependant()
    {
        return coreDependant;
    }

    public void setCoreDependant(boolean coreDependant)
    {
        this.coreDependant = coreDependant;
    }

    public String getCoreVersion()
    {
        if (coreVersion == null)
        {
            throw new ProjectConfigurationException("You must set coreVersion!", new NullPointerException());
        }

        return coreVersion;
    }

    public void setCoreVersion(String coreVersion)
    {
        this.coreVersion = coreVersion;
    }

    public String getMinecraftVersion()
    {
        if (minecraftVersion == null)
        {
            throw new ProjectConfigurationException("You must set minecraftVersion!", new NullPointerException());
        }

        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion)
    {
        this.minecraftVersion = minecraftVersion;
    }

    public String getBasePackage()
    {
        if (basePackage == null)
        {
            throw new ProjectConfigurationException("You must set basePackage!", new NullPointerException());
        }

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

    public boolean isVersionCheckable()
    {
        return versionCheckable;
    }

    public boolean getVersionCheckable()
    {
        return versionCheckable;
    }

    public void setVersionCheckable(boolean versionCheckable)
    {
        this.versionCheckable = versionCheckable;
    }
}
