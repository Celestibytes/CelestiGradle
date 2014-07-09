package celestibytes.gradle;

import net.minecraftforge.gradle.CopyInto;
import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.common.BasePlugin;
import net.minecraftforge.gradle.delayed.DelayedBase;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.delayed.DelayedFileTree;
import net.minecraftforge.gradle.delayed.DelayedString;
import net.minecraftforge.gradle.tasks.abstractutil.DownloadTask;
import net.minecraftforge.gradle.tasks.dev.ChangelogTask;

import celestibytes.gradle.reference.Reference;
import com.google.common.collect.Maps;
import groovy.lang.Closure;
import io.github.pizzana.util.GrepJava;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            makeCWPackageTasks();
            makeCWSignTask();
        }
    }

    public void makeCWPackageTasks()
    {
        String changelogFile = "{BUILD_DIR}/libs/" + project.getName() + "-" + project.getVersion() + "-changelog.txt";

        ChangelogTask changelog = makeTask("createChangelog", ChangelogTask.class);

        changelog.setServerRoot(delayedString("{JENKINS_SERVER}"));
        changelog.setJobName(delayedString("{JENKINS_JOB}"));
        changelog.setAuthName(delayedString("{JENKINS_AUTH_NAME}"));
        changelog.setAuthPassword(delayedString("{JENKINS_AUTH_PASSWORD}"));
        changelog.setTargetBuild(delayedString("{BUILD_NUM}"));
        changelog.setOutput(delayedFile(changelogFile));
        changelog.dependsOn("classes", "processResources");

        project.getTasks().getByName("build").dependsOn(changelog);

        Closure<Object> commonManifest = new Closure<Object>(project)
        {
            @Override
            public Object call()
            {
                Manifest manifest = (Manifest) getDelegate();
                manifest.getAttributes().put("FMLCorePlugin",
                                             delayedString("celestialwizardry.codechicken.core.launch.DepLoader")
                                                     .call());
                manifest.getAttributes().put("FMLCorePluginContainsFMLMod", delayedString("true").call());
                return null;
            }
        };

        Jar jarTask = (Jar) project.getTasks().getByName("jar");
        jarTask.manifest(commonManifest);
        jarTask.dependsOn(changelog);

        Jar javadoc = makeTask("javadocJar", Jar.class);
        javadoc.setClassifier("javadoc");
        javadoc.from(delayedFile("{BUILD_DIR}/docs/javadoc/"));
        javadoc.dependsOn(jarTask, "javadoc");
        javadoc.setExtension("jar");
        project.getArtifacts().add("archives", javadoc);

        Jar sources = makeTask("sourcesJar", Jar.class);
        sources.setClassifier("sources");
        sources.from(delayedFile(changelogFile));
        sources.from(delayedFile("LICENSE"));
        sources.from(delayedFile("build.gradle"));
        sources.from(delayedFile("settings.gradle"));
        sources.from(delayedFile("{BUILD_DIR}/sources/java/"));
        sources.from(delayedFile("{BUILD_DIR}/resources/main/"));
        sources.from(delayedFile("gradlew"));
        sources.from(delayedFile("gradlew.bat"));
        sources.from(delayedFile("gradle/wrapper"), new CopyInto("gradle/wrapper"));
        sources.dependsOn(jarTask);
        sources.setExtension("jar");
        project.getArtifacts().add("archives", sources);

        Jar api = makeTask("apiJar", Jar.class);
        api.setClassifier("api");
        api.from(delayedFile("{BUILD_DIR}/sources/java/celestialwizardry/api/"),
                 new CopyInto("celestialwizardry/api/"));
        api.from(delayedFile("{BUILD_DIR}/sources/java/celestialwizardry/crystal/api/"),
                 new CopyInto("celestialwizardry/crystal/api/"));
        api.dependsOn(jarTask);
        api.setExtension("jar");
        project.getArtifacts().add("archives", api);

        Jar deobf = makeTask("deobfJar", Jar.class);
        deobf.setClassifier("deobf");
        sources.from(delayedFile(changelogFile));
        sources.from(delayedFile("LICENSE"));
        deobf.from(delayedFile("{BUILD_DIR}/classes/main/"));
        deobf.from(delayedFile("{BUILD_DIR}/classes/api/"));
        deobf.manifest(commonManifest);
        deobf.dependsOn(jarTask);
        deobf.setExtension("jar");
        project.getArtifacts().add("archives", deobf);
    }

    public void makeCWSignTask()
    {
        final Jar jar = (Jar) project.getTasks().getByName("jar");
        final File jarPath = jar.getArchivePath();
        final File keystoreLocation = project.file(project.getProperties().get("keystore_location"));
        final String keystoreAlias = (String) project.getProperties().get("keystore_alias");
        final String keystorePassword = (String) project.getProperties().get("keystore_password");

        DefaultTask signJar = makeTask("signJar", DefaultTask.class);
        signJar.getInputs().file(jarPath);
        signJar.getInputs().file(keystoreLocation);
        signJar.getInputs().property("keystore_alias", keystoreAlias);
        signJar.getInputs().property("keystore_password", keystorePassword);
        signJar.getOutputs().file(jarPath);
        signJar.onlyIf(new Spec<Task>()
        {
            @Override
            public boolean isSatisfiedBy(Task task)
            {
                return !keystoreLocation.getPath().equals(".");
            }
        });
        signJar.doLast(new Action<Task>()
        {
            @Override
            public void execute(Task task)
            {
                Map<String, String> args = Maps.newHashMap();
                args.put("destDir", jar.getDestinationDir().getPath());
                args.put("jar", jarPath.getPath());
                args.put("keystore", keystoreLocation.getPath());
                args.put("alias", keystoreAlias);
                args.put("storepass", keystorePassword);
                project.getAnt().invokeMethod("signjar", args);
            }
        });
        signJar.dependsOn("build");

        project.getTasks().getByName("uploadArchives").dependsOn(signJar);
    }

    public void makeBaublesTask()
    {
        try
        {
            String baublesMc = GrepJava.getCWVersionFromJava(project, "BAUBLES_MC");
            String baubles = GrepJava.getCWVersionFromJava(project, "BAUBLES");
            String baublesFile = "Baubles-deobf-" + baublesMc + "-" + baubles + ".jar";
            String baublesRoot = GrepJava.getPropertyFromJava(project,
                                                              "src/main/java/celestialwizardry/reference/Reference" +
                                                                      ".java",
                                                              "BAUBLES_ROOT");
            String baublesUrl = baublesRoot + baublesFile;
            final String baublesDest = "libs/" + baublesFile;

            File[] files = delayedFile("libs").call().listFiles();
            List<File> baubs = new ArrayList<File>();
            boolean hasUpToDateBaubles = false;

            if (files != null)
            {
                for (File file : files)
                {
                    if (file.isFile())
                    {
                        if (file.getName().contains("Baubles"))
                        {
                            if (file.getName().equals(baublesFile))
                            {
                                hasUpToDateBaubles = true;
                            }
                            else
                            {
                                baubs.add(file);
                            }
                        }
                    }
                }
            }

            Delete cleanEveryBaubles = makeTask("cleanEveryBaubles", Delete.class);

            for (File file : baubs)
            {
                cleanEveryBaubles.delete(file);
            }

            if (hasUpToDateBaubles)
            {
                cleanEveryBaubles.delete(delayedFile(baublesDest).call());
            }

            cleanEveryBaubles.setDescription(
                    "Deletes all of the libraries containing \'Baubles\' in their name from the \'libs\' directory");
            cleanEveryBaubles.setGroup(Reference.NAME);

            Delete cleanBaubles = makeTask("cleanBaubles", Delete.class);

            for (File file : baubs)
            {
                cleanBaubles.delete(file);
            }

            cleanBaubles.setDescription(
                    "Deletes all of the libraries containing \'Baubles\' in their name from the \'libs\' directory " +
                            "(excluding the up-to-date one)");
            cleanBaubles.setGroup(Reference.NAME);

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
            cleanBaubles.setDescription("Downloads the correct version of Baubles");
            cleanBaubles.setGroup(Reference.NAME);
            getBaubles.dependsOn(cleanBaubles);

            project.getTasks().getByName("extractUserDev").dependsOn(getBaubles);
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
