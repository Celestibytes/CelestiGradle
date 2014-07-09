package celestibytes.gradle;

import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.common.BasePlugin;
import net.minecraftforge.gradle.delayed.DelayedBase;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.delayed.DelayedFileTree;
import net.minecraftforge.gradle.delayed.DelayedString;
import net.minecraftforge.gradle.tasks.abstractutil.DownloadTask;

import celestibytes.gradle.reference.Reference;
import io.github.pizzana.util.GrepJava;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.specs.Spec;

import java.io.IOException;
import java.util.HashMap;

// TODO Replace that BaseExtension
public final class CelestiGradle implements Plugin<Project>, DelayedBase.IDelayedResolver<BaseExtension>
{
    private Project project;
    // private String projectName;

    @Override
    public void apply(Project project)
    {
        this.project = project;
        // projectName = project.getName();
        String projectName = delayedString("{PROJECT}").call();

        applyExternalPlugin("forge");

        if (projectName.equalsIgnoreCase(Reference.CW_NAME.toLowerCase()))
        {
            makeBaublesTask();
        }
    }

    public void makeBaublesTask()
    {
        try
        {
            String baublesMc = GrepJava.getCWVersionFromJava(project, "BAUBLES_MC");
            String baubles = GrepJava.getCWVersionFromJava(project, "BAUBLES");
            String baublesFile = "Baubles-deobf-" + baublesMc + "-" + baubles + ".jar";
            String baublesRoot = GrepJava.getPropertyFromJava(project,
                                                              "src/main/java/celestialwizardry/reference/Reference.java",
                                                              "BAUBLES_ROOT");
            String baublesUrl = baublesRoot + baublesFile;
            final String baublesDest = "libs/" + baublesFile;

            DownloadTask getBaubles = makeTask("getBaubles", DownloadTask.class);
            getBaubles.setUrl(delayedString(baublesUrl));
            getBaubles.setOutput(delayedFile(baublesDest));

            // TODO If this doesn't work, use onlyIf
            getBaubles.getOutputs().upToDateWhen(new Spec<Task>()
            {
                @Override
                public boolean isSatisfiedBy(Task task)
                {
                    DelayedFile excepted = delayedFile(baublesDest);
                    return excepted.call().exists() && !excepted.call().isDirectory();
                }
            });

        }
        catch (IOException e)
        {
            project.getLogger().warn("Failed to get baubles properties");
            e.printStackTrace();
        }
    }

    public DefaultTask makeTask(String name)
    {
        return makeTask(name, DefaultTask.class);
    }

    public <T extends Task> T makeTask(String name, Class<T> type)
    {
        return BasePlugin.makeTask(project, name, type);
    }

    public void applyExternalPlugin(String plugin)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("plugin", plugin);
        project.apply(map);
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
        pattern = pattern.replace("{PATH}", project.getPath().replace('\\', '/'));
        return pattern;
    }
}
