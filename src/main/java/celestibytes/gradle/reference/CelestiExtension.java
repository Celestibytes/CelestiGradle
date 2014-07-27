package celestibytes.gradle.reference;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;

import java.util.ArrayList;
import java.util.List;

public class CelestiExtension
{
    private final Project project;

    private String theVersion;
    private String coreVersion;
    private String minecraftVersion;
    private String basePackage;
    private List<String> artifactsList = new ArrayList<String>();
    private Closure manifest = null;

    public CelestiExtension(Project project)
    {
        this.project = project;
    }
}
