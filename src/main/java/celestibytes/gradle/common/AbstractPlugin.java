package celestibytes.gradle.common;

import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.delayed.DelayedFileTree;
import net.minecraftforge.gradle.delayed.DelayedString;

import celestibytes.gradle.reference.Reference;
import celestibytes.gradle.util.IResolver;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import java.util.HashMap;

public abstract class AbstractPlugin<E extends AbstractExtension> implements Plugin<Project>, IResolver<E>
{
    protected Project project;
    protected String projectName;

    @Override
    public final void apply(Project project)
    {
        this.project = project;
        projectName = delayedString("{PROJECT}").call();

        createExtension();

        project.allprojects(new Action<Project>()
        {
            @Override
            public void execute(Project project)
            {
                addMavenRepo(project, "cbs", Reference.MAVEN);
            }
        });

        apply();

        makeLifecycleTasks();
    }

    protected abstract void apply();

    private void makeLifecycleTasks()
    {
        DefaultTask release = makeTask("release", DefaultTask.class);
        release.setDescription("Wrapper task for building release-ready archives.");
        release.setGroup(Reference.NAME);
        release.dependsOn("uploadArchives");
    }

    @SuppressWarnings("unchecked")
    protected Class<E> getExtensionClass()
    {
        return (Class<E>) AbstractExtension.class;
    }

    @SuppressWarnings("unchecked")
    protected E getExtension()
    {
        return (E) project.getExtensions().getByName(Reference.EXTEN);
    }

    private void createExtension()
    {
        createExtension(Reference.EXTEN, getExtensionClass(), this);
    }

    protected void createExtension(String name, Class clazz)
    {
        createExtension(name, clazz, project);
    }

    protected void createExtension(String name, Class clazz, Object... params)
    {
        project.getExtensions().create(name, clazz, params);
    }

    public DefaultTask makeTask(String name)
    {
        return makeTask(name, DefaultTask.class);
    }

    public <T extends Task> T makeTask(String name, Class<T> type)
    {
        return makeTask(project, name, type);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Task> T makeTask(Project proj, String name, Class<T> type)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put("type", type);
        return (T) proj.task(map, name);
    }

    public void applyExternalPlugin(String plugin)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("plugin", plugin);
        project.apply(map);
    }

    public MavenArtifactRepository addMavenRepo(Project project, final String name, final String url)
    {
        return project.getRepositories().maven(new Action<MavenArtifactRepository>() {
            @Override
            public void execute(MavenArtifactRepository repo)
            {
                repo.setName(name);
                repo.setUrl(url);
            }
        });
    }

    public FlatDirectoryArtifactRepository addFlatRepo(Project project, final String name, final Object... dirs)
    {
        return project.getRepositories().flatDir(new Action<FlatDirectoryArtifactRepository>() {
            @Override
            public void execute(FlatDirectoryArtifactRepository repo)
            {
                repo.setName(name);
                repo.dirs(dirs);
            }
        });
    }

    protected DelayedString delayedString(String path)
    {
        return new DelayedString(project, path, this);
    }

    protected DelayedFile delayedFile(String path)
    {
        return new DelayedFile(project, path, this);
    }

    protected DelayedFileTree delayedFileTree(String path)
    {
        return new DelayedFileTree(project, path, this);
    }

    protected DelayedFileTree delayedZipTree(String path)
    {
        return new DelayedFileTree(project, path, true, this);
    }

    @Override
    public String resolve(String pattern, Project project, BaseExtension extension)
    {
        return resolve(pattern, project, getExtension());
    }

    @Override
    public String resolve(String pattern, Project project, E extension)
    {
        pattern = pattern.replace("{PATH}", project.getPath().replace('\\', '/'));
        pattern = pattern.replace("{CORE_VERSION}", extension.getVersion());
        pattern = pattern.replace("{CORE_NAME}", Reference.CORE_NAME);
        return pattern;
    }
}
