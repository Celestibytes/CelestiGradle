package net.minecraftforge.gradle.user.cg;

import net.minecraftforge.gradle.common.BasePlugin;
import net.minecraftforge.gradle.user.cg.util.GrepJava;

import com.google.common.collect.Maps;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.Map;

public class BaublesPlugin implements Plugin<Project>
{
    public Project project;

    @Override
    public void apply(final Project project)
    {
        this.project = project;

        try
        {
            String baublesMc = GrepJava.getCWVersionFromJava(project, "BAUBLES_MC");
            String baubles = GrepJava.getCWVersionFromJava(project, "BAUBLES");
            String baublesFile = "Baubles-deobf-" + baublesMc + "-" + baubles + ".jar";
            String baublesRoot = GrepJava.getPropertyFromJava(project, "src/main/java/celestialwizardry/reference/Reference.java", "BAUBLES_ROOT");
            final String baublesUrl = baublesRoot + baublesFile;
            final String baublesDest = "libs/" + baublesFile;

            DefaultTask getBaubles = makeTask("getBaubles");
            getBaubles.dependsOn("extractUserDev");
            getBaubles.doLast(new Action<Task>()
            {
                @Override
                public void execute(Task task)
                {
                    Map<String, String> args = Maps.newHashMap();
                    args.put("dir", "libs/");
                    project.getAnt().invokeMethod("delete", args);
                    project.getAnt().invokeMethod("mkdir", args);

                    args = Maps.newHashMap();
                    args.put("src", baublesUrl);
                    args.put("dest", baublesDest);
                    project.getAnt().invokeMethod("get", args);
                }
            });

            project.getTasks().getByName("setupDevWorkspace").dependsOn(getBaubles);
            project.getTasks().getByName("setupDecompWorkspace").dependsOn(getBaubles);
            project.getTasks().getByName("setupCIWorkspace").dependsOn(getBaubles);
        }
        catch (Exception e)
        {
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
}
